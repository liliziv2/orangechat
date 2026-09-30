package me.rerere.rikkahub.data.ai.pills

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.uuid.Uuid

/**
 * 待生效药丸的进程内暂存。
 *
 * 挂在**消息 id** 上，不挂在会话或助手上：药丸的语义就是「贴在某一条自己说的话上」，
 * 换一条消息就该重新挑。与 [me.rerere.rikkahub.data.ai.tools.local.KnockRequestBuffer]
 * 同一套路子 —— AI 与界面同进程，一个进程内 StateFlow 就够，不需要落库、不需要跨进程桥。
 *
 * 刻意**不持久化**：药丸只作用一轮，存进磁盘就会出现「上次挑的药这次还在」，
 * 那正是它要避免的东西。
 */
object PillStore {
    private val _pending = MutableStateFlow<Map<Uuid, List<String>>>(emptyMap())

    /** 哪条消息挂了哪几颗药丸。界面直接读它画标记。 */
    val pending: StateFlow<Map<Uuid, List<String>>> = _pending.asStateFlow()

    fun attachedTo(messageId: Uuid): List<String> = _pending.value[messageId].orEmpty()

    /** 挑中 / 取消一颗。取消到空时把这条消息整个摘掉，免得留一堆空列表。 */
    fun toggle(messageId: Uuid, code: String) {
        _pending.update { current ->
            val next = current[messageId].orEmpty().toMutableList()
            if (!next.remove(code)) {
                next.add(code)
            }
            if (next.isEmpty()) current - messageId else current + (messageId to next)
        }
    }

    fun clear(messageId: Uuid) {
        _pending.update { it - messageId }
    }

    /**
     * 阅后即焚：这些消息上的药丸已经在这一轮被读过了，无论生成成功还是失败都不该再影响
     * 下一轮。
     *
     * 注入发生在 [me.rerere.rikkahub.data.ai.transformers.PillTransformer] 里、只改
     * 发给模型的那一份副本，**不会落库**，所以这里只需要清暂存，不需要去历史里删原文。
     */
    fun consume(messageIds: Collection<Uuid>) {
        if (messageIds.isEmpty()) return
        _pending.update { current -> current.filterKeys { it !in messageIds } }
    }
}
