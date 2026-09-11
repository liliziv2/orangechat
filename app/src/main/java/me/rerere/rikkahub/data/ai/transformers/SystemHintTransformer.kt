/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.ai.transformers

import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart

/**
 * 程序注入的"系统提示"消息的识别规则。
 *
 * 语音通话心跳、主动消息上下文这类消息是程序替用户开口的：为了复用同一条
 * [me.rerere.rikkahub.service.ChatService.sendMessage] 落库链路，它们被存成了 USER 消息。
 * 当轮它们是必要的输入，但一旦沉进历史就变成噪音：
 *
 * - 用户会在聊天记录里看到自己"说"过没说过的话；
 * - 模型会把方括号指令当成用户的说话风格来模仿；
 * - 它们照样占 contextMessageSize 额度、进收尾摘要，挤掉真实对话。
 *
 * 所以这里只提供判定，过滤动作放在各调用方，且都遵循同一条规则：
 * **保留消息列表的最后一条**（那正是本轮要模型响应的指令），更早的同类消息一律丢弃。
 *
 * 过滤点：
 * - [me.rerere.rikkahub.service.ChatService] 组装 generateText 的 messages 入参（正常聊天）；
 * - [me.rerere.rikkahub.data.service.ProactiveMessageService] 构建 historyMessages（主动消息）；
 * - [me.rerere.rikkahub.ui.pages.chat.ChatList] 的 displayNodes（UI 显示）；
 * - ChatService.closeoutConversationToMemory 的 transcript（收尾摘要）。
 *
 * 注意都要在按条数截断之前过滤 —— GenerationHandler 内部先 limitContext 再跑输入转换器，
 * 做成转换器就晚了。
 */
object SystemHintTransformer {
    /** 语音通话沉默心跳，见 VoiceCallService.sendHeartbeatPrompt */
    const val HINT_VOICE_CALL = "[通话提示]"

    /** 主动消息附带的上下文块，见 ProactiveMessageService */
    const val HINT_PROACTIVE_CONTEXT = "[主动消息上下文]"

    private val HINT_MARKERS = listOf(HINT_VOICE_CALL, HINT_PROACTIVE_CONTEXT)

    /**
     * 时间提醒标记，见 TimeReminderTransformer。它作为 USER 消息插入，只给模型看。
     */
    private const val HINT_TIME_REMINDER = "<time_reminder>"

    /**
     * 提示词注入（模式注入 / 世界书）泄漏进时间线时的识别特征。
     *
     * 这类内容本该只存在于单次请求的消息列表里，不落库。但历史上主动消息流程
     * 把转换器插入的注入消息错当成 AI 回复写进了会话（见 ProactiveMessageService），
     * 用户于是在聊天界面看到成段的行为约束条款。修复只能阻止新的写入，
     * 已经存进数据库的那些需要在展示和上下文里一并跳过。
     *
     * 判据取"全大写方括号标题 + 成段条款"这种模板化结构：注入块几乎都以
     * [BEHAVIOR GUARDRAILS] / [SYSTEM RULES] 这样的标题起头，后面跟着多行约束。
     * 额外要求标题必须在开头、且整段有一定长度，避免误伤用户自己打的 "[TODO] 记一下"。
     */
    private val INJECTION_HEADER_REGEX = Regex("""^\[[A-Z][A-Z0-9_ /\-]{3,}\]""")

    /** 低于这个长度的方括号开头消息当成用户正常输入，不隐藏 */
    private const val INJECTION_MIN_LENGTH = 80

    /**
     * 判断一条消息是否是程序注入的系统提示（而不是用户真的打了这些字）。
     */
    fun isSystemHint(message: UIMessage): Boolean {
        if (message.role != MessageRole.USER) return false
        val text = message.parts
            .filterIsInstance<UIMessagePart.Text>()
            .joinToString("\n") { it.text }
            .trim()
        if (text.isEmpty()) return false
        if (HINT_MARKERS.any { text.contains(it) }) return true
        if (text.startsWith(HINT_TIME_REMINDER)) return true
        return text.length >= INJECTION_MIN_LENGTH && INJECTION_HEADER_REGEX.containsMatchIn(text)
    }

    /**
     * 丢弃历史里的系统提示，保留最后一条消息。
     *
     * 列表长度 ≤1 时原样返回：唯一那条要么是真实用户消息，要么就是本轮的心跳指令，
     * 两种情况都不能删。
     */
    fun dropStaleHints(messages: List<UIMessage>): List<UIMessage> {
        val lastIndex = messages.lastIndex
        if (lastIndex < 1) return messages
        if (messages.subList(0, lastIndex).none { isSystemHint(it) }) return messages
        return messages.filterIndexed { index, message ->
            index == lastIndex || !isSystemHint(message)
        }
    }
}
