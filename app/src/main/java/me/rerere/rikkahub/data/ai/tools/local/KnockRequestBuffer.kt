/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.ai.tools.local

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * AI 主动敲门弹窗 (knock_user) 的进程内请求总线.
 *
 * 与 ask_user 的区别: ask_user 走 HITL 审批流, 会话必须停在待答复状态直到用户点提交,
 * 没有超时概念; knock_user 是 AI 自己发起的一次「敲门」, 挂在界面最上层, 用户可以直接
 * 无视 —— 到点后工具自己以 [KnockOutcome.NoResponse] 收尾, 模型收到「用户没回应」再自己
 * 接着说点什么, 更像真人被无视了的反应.
 *
 * 因为 AI 与界面同进程, 不需要 mac_knock 那套 HTTP 桥 / MCP server / 反向隧道,
 * 一个 [CompletableDeferred] 就够: 工具侧 [request] 后挂起, UI 侧 [respond] / [expire] 回填.
 */
sealed class KnockOutcome {
    /** 用户点了某个按钮, [button] 是按钮原文 */
    data class Chosen(val button: String) : KnockOutcome()

    /** 用户划掉了弹窗 (明确不想理这次) */
    data object Dismissed : KnockOutcome()

    /** 超时没人理 */
    data object NoResponse : KnockOutcome()
}

/**
 * 一次待响应的敲门请求. [id] 用来让 UI 回填到正确的 deferred,
 * [timeoutMs] 为 0 表示不自动超时 (一直等到用户处理).
 */
data class KnockRequest(
    val id: String,
    val title: String,
    val message: String,
    val buttons: List<String>,
    val timeoutMs: Long,
    val conversationId: String?,
)

object KnockRequestBuffer {
    /** 按钮数量上限, 与 mac_knock 一致: 超过 3 个按钮的弹窗在手机上没法排 */
    const val MAX_BUTTONS = 3

    /** 超时上限 10 分钟; 再长的等待应该改用主动消息而不是占着一个生成回合 */
    const val MAX_TIMEOUT_SECONDS = 600L

    const val DEFAULT_TIMEOUT_SECONDS = 60L

    private val pending = ConcurrentHashMap<String, CompletableDeferred<KnockOutcome>>()

    private val _current = MutableStateFlow<KnockRequest?>(null)

    /** 当前需要显示的敲门弹窗, null 表示没有. 同一时刻只显示一个, 后来的覆盖前一个 */
    val current: StateFlow<KnockRequest?> = _current.asStateFlow()

    fun request(request: KnockRequest): CompletableDeferred<KnockOutcome> {
        val deferred = CompletableDeferred<KnockOutcome>()
        pending[request.id] = deferred
        // 上一个还没被处理就先让它以「没回应」收尾, 免得旧 deferred 永久挂着
        val previous = _current.value
        if (previous != null && previous.id != request.id) {
            pending.remove(previous.id)?.complete(KnockOutcome.NoResponse)
        }
        _current.value = request
        return deferred
    }

    fun respond(id: String, outcome: KnockOutcome) {
        pending.remove(id)?.complete(outcome)
        if (_current.value?.id == id) {
            _current.value = null
        }
    }

    /** 超时收尾: 工具侧 withTimeoutOrNull 到点后调用, 同时收掉界面上的弹窗 */
    fun expire(id: String) {
        respond(id, KnockOutcome.NoResponse)
    }
}
