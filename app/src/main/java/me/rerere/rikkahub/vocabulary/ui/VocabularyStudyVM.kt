package me.rerere.rikkahub.vocabulary.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import me.rerere.rikkahub.vocabulary.algorithm.StudyConstants
import me.rerere.rikkahub.vocabulary.data.VocabularyRepository
import me.rerere.rikkahub.vocabulary.entity.VocabularyEntity
import me.rerere.rikkahub.vocabulary.study.SessionCard
import me.rerere.rikkahub.vocabulary.study.SessionState
import me.rerere.rikkahub.vocabulary.study.StudyMode
import me.rerere.rikkahub.vocabulary.study.answer
import me.rerere.rikkahub.vocabulary.study.markMastered
import me.rerere.rikkahub.vocabulary.study.reveal

/** 一组过完之后的成绩单 */
data class StudySummary(
    val total: Int,
    val completed: Int,
    val parked: Int,
    val firstTry: Int,
    val repeatCount: Int,
    val mastered: Int,
    /** 这是这本书的第几次学习 */
    val times: Int,
)

/**
 * 学习页。
 *
 * ⚠️ 构造函数里**不要拿 Context / Application**（Koin 会崩）。vocabId 通过
 * Koin 的参数传进来（`koinViewModel(parameters = { parametersOf(vocabId) })`）。
 */
class VocabularyStudyVM(
    private val repository: VocabularyRepository,
    private val vocabId: Long,
) : ViewModel() {

    private val _book = MutableStateFlow<VocabularyEntity?>(null)
    val book: StateFlow<VocabularyEntity?> = _book.asStateFlow()

    private val _session = MutableStateFlow<SessionState?>(null)
    val session: StateFlow<SessionState?> = _session.asStateFlow()

    /**
     * 上次没学完的会话。非 null 时页面先弹「继续 / 重新开始」，
     * **不要直接把用户扔回学习页** —— 他可能已经忘了上次在干什么。
     */
    private val _resumable = MutableStateFlow<SessionState?>(null)
    val resumable: StateFlow<SessionState?> = _resumable.asStateFlow()

    private val _summary = MutableStateFlow<StudySummary?>(null)
    val summary: StateFlow<StudySummary?> = _summary.asStateFlow()

    private val _notice = MutableStateFlow<VocabularyNotice?>(null)
    val notice: StateFlow<VocabularyNotice?> = _notice.asStateFlow()

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    /**
     * 页面进入时调一次。**幂等** —— 转屏会重建 composition、再调一次，
     * 不能因此把这一组重开。
     *
     * @param review 从「复习」入口进来是 true。有没学完的会话时它优先：
     *               上次那组是什么模式就接着什么模式。
     */
    suspend fun enter(review: Boolean) {
        if (_ready.value) return
        _book.value = repository.book(vocabId)
        val resumable = repository.loadResumable()?.takeIf { it.book.id == vocabId }
        if (resumable != null) {
            _resumable.value = resumable.state
        } else {
            startSession(if (review) StudyMode.REVIEW else StudyMode.LEARN)
        }
        _ready.value = true
    }

    fun start(mode: StudyMode) {
        viewModelScope.launch { startSession(mode) }
    }

    fun continueSession() {
        val state = _resumable.value ?: return
        _resumable.value = null
        _session.value = state
    }

    fun restartSession() {
        viewModelScope.launch {
            _resumable.value = null
            repository.clearSession()
            startSession(StudyMode.LEARN)
        }
    }

    private suspend fun startSession(mode: StudyMode) {
        val cards = repository.loadCards(vocabId, mode, StudyConstants.DEFAULT_SESSION_SIZE)
        if (cards.isEmpty()) {
            _notice.value = if (mode == StudyMode.LEARN) {
                VocabularyNotice.NoNewWords
            } else {
                VocabularyNotice.NoDueWords
            }
            return
        }
        val state = SessionState(
            mode = mode,
            total = cards.size,
            queue = cards.map { SessionCard(it) },
            startedAt = System.currentTimeMillis(),
        )
        _session.value = state
        repository.persistSession(vocabId, state)
    }

    fun reveal() {
        _session.value = _session.value?.reveal()
    }

    /** 判一次卡。没翻开答案时直接忽略 */
    fun answer(answerType: Int) {
        val current = _session.value ?: return
        if (!current.revealed) return
        val card = current.current ?: return

        val result = current.answer(answerType)
        _session.value = result.state

        viewModelScope.launch {
            repository.answer(card.card, answerType, result.graduated)
            // ⚠️ finish() 必须排在 answer() **之后**、同一个协程里 ——
            // 它要读刚写进去的那一行（第几次学习）。抢在写库之前算会少最后一条。
            if (result.state.finished) {
                finish(result.state)
            } else {
                repository.persistSession(vocabId, result.state)
            }
        }
    }

    /** 标记「我本来就会」。不需要翻开答案 */
    fun markMastered() {
        val current = _session.value ?: return
        val card = current.current ?: return

        val advanced = current.markMastered()
        _session.value = advanced

        viewModelScope.launch {
            repository.markMastered(card.card)
            if (advanced.finished) {
                finish(advanced)
            } else {
                repository.persistSession(vocabId, advanced)
            }
        }
    }

    private suspend fun finish(state: SessionState) {
        repository.finishSession(
            vocabId = vocabId,
            startedAt = state.startedAt,
            total = state.total,
            completed = state.done.size,
            parked = state.parked.size,
        )
        repository.clearSession()
        _session.value = null
        _summary.value = StudySummary(
            total = state.total,
            completed = state.done.size,
            parked = state.parked.size,
            firstTry = state.done.count { it.attempts == 1 },
            repeatCount = state.done.count { it.attempts > 1 },
            mastered = state.mastered,
            times = repository.countSessions(vocabId),
        )
    }

    fun dismissSummary() {
        _summary.value = null
    }

    fun consumeNotice() {
        _notice.value = null
    }
}
