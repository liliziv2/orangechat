package me.rerere.rikkahub.vocabulary.study

import me.rerere.rikkahub.vocabulary.algorithm.StudyConstants
import me.rerere.rikkahub.vocabulary.entity.VocabularyCardEntity

/** 这一组是「学新词」还是「复习到期的词」 */
enum class StudyMode { LEARN, REVIEW }

/** 队列里的一张卡。除了卡片本身，还要记住这一组里它被认对了几次、被问了几次 */
data class SessionCard(
    val card: VocabularyCardEntity,
    val passes: Int = 0,
    val attempts: Int = 0,
)

/**
 * 一轮学习的完整状态。**纯数据 + 纯函数**，不碰数据库、不碰 Android。
 *
 * 队列规则（三条，都在 [answer] 里）：
 * - 认识 → 组内认对次数 +1，攒够 [SessionState.target] 次出队
 * - 模糊 → 次数**不变**，隔几个词再排回来
 * - 忘记 → 次数**清零**，隔几个词再排回来
 *
 * 「模糊」不加也不减：算通过太松（用户会一路模糊过去），算失败太重（其实记住了大半）。
 * 「忘记」清零而不是 -1：一组里认对 3 次的目的是让这个词短期内被反复激活，
 * 如果只减 1，「认识、认识、忘记、认识」也能凑够 3 次 —— 但中间那次是彻底想不起来。
 */
data class SessionState(
    val mode: StudyMode,
    /** 这一组计划过多少词（进度分母，开局定死） */
    val total: Int,
    /** 还没过的；first() 就是当前这张 */
    val queue: List<SessionCard>,
    /** 攒够次数出队的 */
    val done: List<SessionCard> = emptyList(),
    /** 判太多次先放过的，这一组不再出现 */
    val parked: List<SessionCard> = emptyList(),
    /** 这一组里标成「已掌握」的词数 */
    val mastered: Int = 0,
    /** 当前这张翻开答案了没有 */
    val revealed: Boolean = false,
    val startedAt: Long = 0L,
) {
    val current: SessionCard? get() = queue.firstOrNull()

    val finished: Boolean get() = queue.isEmpty()

    /** 这一组里要认对几次才算过 */
    val target: Int
        get() = if (mode == StudyMode.REVIEW) {
            StudyConstants.REVIEW_PASSES
        } else {
            StudyConstants.LEARN_PASSES
        }

    /** 已经处理掉多少张（进度分子） */
    val answeredCount: Int get() = done.size + parked.size + mastered
}

/** [SessionState.answer] 的结果。[graduated] 决定要不要往卡片表落状态 */
data class AnswerResult(
    val state: SessionState,
    /** 这张卡这一组里攒够次数了 —— 只有它为 true 时才写卡片状态 */
    val graduated: Boolean,
)

/** 翻开答案。没翻开之前不能判卡 */
fun SessionState.reveal(): SessionState = if (revealed) this else copy(revealed = true)

/**
 * 判一次卡，返回推进后的会话。
 *
 * 没翻开答案、或者队列已经空了，都原样返回（[AnswerResult.graduated] 为 false）。
 */
fun SessionState.answer(answerType: Int): AnswerResult {
    val card = current ?: return AnswerResult(this, false)
    if (!revealed) return AnswerResult(this, false)

    val passes = when (answerType) {
        StudyConstants.ANSWER_KNOW -> card.passes + 1
        StudyConstants.ANSWER_FUZZY -> card.passes
        else -> 0
    }
    val stepped = card.copy(passes = passes, attempts = card.attempts + 1)
    val rest = queue.drop(1)

    val graduated = passes >= target
    val giveUp = !graduated && stepped.attempts >= StudyConstants.MAX_ATTEMPTS_PER_CARD

    val advanced = when {
        // 出队
        graduated -> copy(queue = rest, done = done + stepped, revealed = false)

        // 先放过，这一组不再出现
        giveUp -> copy(queue = rest, parked = parked + stepped, revealed = false)

        // 隔几个词再排回来：不直接放队尾（要等几分钟，等于一次新鲜度测试），
        // 也不放队首（刚看过答案，通过了说明不了什么）。队列比 4 个还短就直接放队尾。
        else -> {
            val at = minOf(StudyConstants.REQUEUE_GAP, rest.size)
            copy(
                queue = rest.toMutableList().also { it.add(at, stepped) },
                revealed = false,
            )
        }
    }
    return AnswerResult(advanced, graduated)
}

/**
 * 标记「我本来就会」。
 *
 * **不需要翻开答案** —— 用户看到单词就知道自己会，凭什么要先翻。
 * 这一组里直接出队，也不进 `done`（它不算「学了」）。
 */
fun SessionState.markMastered(): SessionState {
    if (current == null) return this
    return copy(queue = queue.drop(1), mastered = mastered + 1, revealed = false)
}
