package me.rerere.rikkahub.vocabulary.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import me.rerere.rikkahub.vocabulary.algorithm.CsvParser
import me.rerere.rikkahub.vocabulary.algorithm.CsvError
import me.rerere.rikkahub.vocabulary.algorithm.CsvRow
import me.rerere.rikkahub.vocabulary.algorithm.MasteryCalculator
import me.rerere.rikkahub.vocabulary.algorithm.ParseResult
import me.rerere.rikkahub.vocabulary.algorithm.StudyConstants
import me.rerere.rikkahub.vocabulary.dao.BookStatsRow
import me.rerere.rikkahub.vocabulary.dao.StudyRecordDao
import me.rerere.rikkahub.vocabulary.dao.StudySessionDao
import me.rerere.rikkahub.vocabulary.dao.VocabularyCardDao
import me.rerere.rikkahub.vocabulary.dao.VocabularyDao
import me.rerere.rikkahub.vocabulary.entity.StudyRecordEntity
import me.rerere.rikkahub.vocabulary.entity.StudySessionEntity
import me.rerere.rikkahub.vocabulary.entity.VocabularyCardEntity
import me.rerere.rikkahub.vocabulary.entity.VocabularyEntity
import me.rerere.rikkahub.vocabulary.study.PersistedSession
import me.rerere.rikkahub.vocabulary.study.SessionCard
import me.rerere.rikkahub.vocabulary.study.SessionState
import me.rerere.rikkahub.vocabulary.study.StudyMode
import me.rerere.rikkahub.vocabulary.study.StudySessionStore
import java.time.LocalDate
import java.time.ZoneId

/** 一本词库 + 它那份没学完的会话 */
data class ResumableSession(
    val book: VocabularyEntity,
    val state: SessionState,
)

/**
 * 背单词的数据层。把 DAO 的查询和 algorithm 的纯计算串成完整流程。
 *
 * 拿到 Context 是允许的 —— 它是单例，活到进程结束。**ViewModel 不要拿 Context**
 * （Koin 会抛 InstanceCreationException，App 直接进安全模式），要 Context 就在
 * Composable 里取 LocalContext 传进来。
 */
class VocabularyRepository(
    private val context: Context,
    private val vocabularyDao: VocabularyDao,
    private val cardDao: VocabularyCardDao,
    private val recordDao: StudyRecordDao,
    private val sessionDao: StudySessionDao,
    private val sessionStore: StudySessionStore,
) {

    val books: Flow<List<VocabularyEntity>> = vocabularyDao.getVisibleVocabularies()

    val deletedBooks: Flow<List<VocabularyEntity>> = vocabularyDao.getDeletedVocabularies()

    /**
     * 所有词库合计的待复习数（侧栏入口那行的小字用）。
     *
     * ⚠️ 这是**唯一**一个允许挂 Flow 的汇总数字，因为它显示在常驻组合的侧栏里，
     * 不能在那儿挂轮询。列表页那套统计仍然是 suspend + 轮询，两者口径一致但用途不同。
     */
    val dueCount: Flow<Int> = cardDao.countDueAll(System.currentTimeMillis())

    suspend fun book(vocabId: Long): VocabularyEntity? = vocabularyDao.getVocabularyById(vocabId)

    /** 每本词库的进度汇总。⚠️ 里面的 `due` 依赖传进来的 now，**必须轮询重算**，不能只挂 Flow */
    suspend fun statsByBook(now: Long): Map<Long, BookStatsRow> =
        cardDao.statsByBook(now).associateBy { it.vocabId }

    /** 今天一共过了多少个词（跨词库，按卡片去重） */
    suspend fun wordsStudiedToday(): Int {
        val start = startOfDayMillis()
        return recordDao.countWordsInRangeAll(start, start + DAY_MILLIS)
    }

    suspend fun countSessions(vocabId: Long): Int = sessionDao.countSessions(vocabId)

    /** 取一组要过的卡。学新词按导入顺序，复习按到期时间 */
    suspend fun loadCards(vocabId: Long, mode: StudyMode, limit: Int): List<VocabularyCardEntity> =
        when (mode) {
            StudyMode.LEARN -> cardDao.getNewCards(vocabId, limit)
            StudyMode.REVIEW -> cardDao.getReviewCards(vocabId, System.currentTimeMillis(), limit)
        }

    // ------------------------------------------------------------------ 判卡

    /**
     * 判一次卡。
     *
     * **最容易写错的地方**：`graduated == false` 时只写答题记录，**绝不动卡片状态**。
     * 组内每判一次都写 `mastery` / `learned` 的话，用户学到一半退出，这个词已经成了
     * 「学过」，下次就再也拿不到第二、第三次 —— 一组的机制整个失效。
     *
     * @param graduated 这一组里攒够认对次数了吗
     */
    suspend fun answer(
        card: VocabularyCardEntity,
        answerType: Int,
        graduated: Boolean,
    ): VocabularyCardEntity {
        val now = System.currentTimeMillis()

        if (!graduated) {
            recordDao.insert(recordOf(card, answerType, now))
            return card
        }

        val outcome = MasteryCalculator.apply(card.mastery, answerType, now)
        val updated = card.copy(
            mastery = outcome.mastery,
            learned = true,
            nextReviewDate = outcome.nextReviewDate,
            updatedAt = now,
        )
        cardDao.update(updated)
        recordDao.insert(recordOf(card, answerType, now))
        return updated
    }

    /**
     * 标记「我本来就会」。
     *
     * **不写答题记录** —— 它是「我本来就会」，不是「我学了」。写了的话这个词会算进
     * 「今日已学」，指标就假了。
     */
    suspend fun markMastered(card: VocabularyCardEntity): VocabularyCardEntity {
        val outcome = MasteryCalculator.mastered()
        val updated = card.copy(
            mastery = outcome.mastery,
            learned = true,
            nextReviewDate = outcome.nextReviewDate,
            updatedAt = System.currentTimeMillis(),
        )
        cardDao.update(updated)
        return updated
    }

    private fun recordOf(card: VocabularyCardEntity, answerType: Int, now: Long) =
        StudyRecordEntity(
            vocabId = card.vocabId,
            cardId = card.id,
            isCorrect = answerType == StudyConstants.ANSWER_KNOW,
            answerType = answerType,
            timestamp = now,
        )

    // ------------------------------------------------------------------ 词库

    /**
     * 从用户选的文件导入一份 CSV。
     *
     * 失败时给的是**错误码**，不是文案 —— 这一层拿不到 Context，
     * 文案由 UI 层按 [ImportResult.Failure.reason] 取资源。
     *
     * @param name 用户填的词库名。留空则用 [fallbackName]（由 UI 传一个本地化的默认名）
     */
    suspend fun importCsv(uri: Uri, name: String, fallbackName: String): ImportResult =
        withContext(Dispatchers.IO) {
            val text = runCatching {
                context.contentResolver.openInputStream(uri)
                    ?.use { it.bufferedReader().readText() }
            }.getOrNull()

            if (text == null) return@withContext ImportResult.Failure(CsvError.UNREADABLE_FILE)

            when (val parsed = CsvParser.parse(text)) {
                is ParseResult.Failure -> ImportResult.Failure(parsed.reason, parsed.arg)
                is ParseResult.Success -> {
                    val id = insertBook(
                        name = name.trim().ifBlank { fallbackName },
                        // 简介留空：它是要落库的「用户数据」，写死中文就等于把语言钉死在
                        // 导入那一刻。卡片上已经有进度那行，词数不缺地方显示。
                        description = "",
                        hasUnit = parsed.hasUnit,
                        rows = parsed.rows,
                    )
                    ImportResult.Success(vocabId = id, count = parsed.rows.size)
                }
            }
        }

    private suspend fun insertBook(
        name: String,
        description: String,
        hasUnit: Boolean,
        rows: List<CsvRow>,
    ): Long {
        val now = System.currentTimeMillis()
        val id = vocabularyDao.insert(
            VocabularyEntity(
                name = name,
                description = description,
                hasUnit = hasUnit,
                createdAt = now,
                updatedAt = now,
            )
        )
        cardDao.insertAll(
            rows.map { row ->
                VocabularyCardEntity(
                    vocabId = id,
                    word = row["word"].orEmpty().trim(),
                    translation = row["translation"].orEmpty().trim(),
                    ipa = row["ipa"].orEmpty().trim(),
                    example = row["example"].orEmpty().trim(),
                    pos = row["pos"].orEmpty().trim(),
                    unit = row["unit"]?.trim()?.takeIf { it.isNotEmpty() },
                    createdAt = now,
                    updatedAt = now,
                )
            }
        )
        return id
    }

    /** 软删除：单词、进度、答题记录全留着，随时能连进度一起恢复 */
    suspend fun deleteBook(vocabId: Long) {
        val book = vocabularyDao.getVocabularyById(vocabId) ?: return
        val now = System.currentTimeMillis()
        vocabularyDao.update(book.copy(deletedAt = now, updatedAt = now))
    }

    /** 从「已删除词库」捡回来，进度接着用 */
    suspend fun restoreBook(vocabId: Long) {
        val book = vocabularyDao.getVocabularyById(vocabId) ?: return
        vocabularyDao.update(book.copy(deletedAt = null, updatedAt = System.currentTimeMillis()))
    }

    // ------------------------------------------------------------------ 会话

    /** 一组过完了，记一条档案 */
    suspend fun finishSession(
        vocabId: Long,
        startedAt: Long,
        total: Int,
        completed: Int,
        parked: Int,
    ) {
        sessionDao.insert(
            StudySessionEntity(
                vocabId = vocabId,
                startTime = startedAt,
                endTime = System.currentTimeMillis(),
                totalCards = total,
                completedCount = completed,
                parkedCount = parked,
            )
        )
    }

    /** 每判一次卡存一次 —— 用户可能随时退出、App 可能被系统杀掉 */
    suspend fun persistSession(vocabId: Long, state: SessionState) {
        sessionStore.save(state.toPersisted(vocabId))
    }

    suspend fun clearSession() {
        sessionStore.clear()
    }

    /**
     * 读回没学完的会话。
     *
     * 恢复时**按 ID 重新查库**（不存卡片内容），所以期间词库内容刷新过的话，
     * 恢复出来的是新内容。任何一张卡查不到（被删了）就整份作废 —— 宁可从这一组重来，
     * 也不要拼出一个缺词的队列。
     */
    suspend fun loadResumable(): ResumableSession? {
        val persisted = sessionStore.load() ?: return null
        val book = vocabularyDao.getVocabularyById(persisted.vocabId) ?: return null
        if (book.deletedAt != null) return null
        val state = persisted.toState() ?: return null
        return ResumableSession(book, state)
    }

    private suspend fun PersistedSession.toState(): SessionState? {
        val studyMode = runCatching { StudyMode.valueOf(mode) }.getOrNull() ?: return null

        val queue = ArrayList<SessionCard>(queueCardIds.size)
        queueCardIds.forEachIndexed { i, id ->
            val card = cardDao.getCardById(id) ?: return null
            queue += SessionCard(
                card = card,
                passes = queuePasses.getOrElse(i) { 0 },
                attempts = queueAttempts.getOrElse(i) { 0 },
            )
        }
        // 队列空了说明这一组其实已经过完，没有可续的
        if (queue.isEmpty()) return null

        val done = ArrayList<SessionCard>(doneCardIds.size)
        doneCardIds.forEachIndexed { i, id ->
            val card = cardDao.getCardById(id) ?: return null
            done += SessionCard(card = card, attempts = doneAttempts.getOrElse(i) { 0 })
        }

        val parked = ArrayList<SessionCard>(parkedCardIds.size)
        parkedCardIds.forEachIndexed { i, id ->
            val card = cardDao.getCardById(id) ?: return null
            parked += SessionCard(
                card = card,
                passes = parkedPasses.getOrElse(i) { 0 },
                attempts = parkedAttempts.getOrElse(i) { 0 },
            )
        }

        return SessionState(
            mode = studyMode,
            total = total,
            queue = queue,
            done = done,
            parked = parked,
            mastered = mastered,
            revealed = false,
            startedAt = startedAt,
        )
    }

    private fun SessionState.toPersisted(vocabId: Long) = PersistedSession(
        vocabId = vocabId,
        mode = mode.name,
        total = total,
        queueCardIds = queue.map { it.card.id },
        queuePasses = queue.map { it.passes },
        queueAttempts = queue.map { it.attempts },
        doneCardIds = done.map { it.card.id },
        doneAttempts = done.map { it.attempts },
        parkedCardIds = parked.map { it.card.id },
        parkedAttempts = parked.map { it.attempts },
        parkedPasses = parked.map { it.passes },
        mastered = mastered,
        startedAt = startedAt,
    )

    companion object {
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L

        /**
         * 今天的 0 点。
         *
         * ⚠️ 用「系统时区的当天 0 点」，不要用「当前时间减 24 小时」——
         * 后者会让用户凌晨 1 点看到的「今日已学」把昨天下午的算进来。
         */
        fun startOfDayMillis(): Long =
            LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}

/**
 * 一次导入的结果。
 *
 * 失败带的是**错误码**而不是文案：Repository 层没有 Context，
 * 在这里拼字符串就只能写死中文。文案归 UI 层。
 */
sealed interface ImportResult {
    data class Success(val vocabId: Long, val count: Int) : ImportResult

    /** [arg] 给带参数的文案用（目前是「按什么顺序排」要用到的列名提示） */
    data class Failure(val reason: CsvError, val arg: String? = null) : ImportResult
}
