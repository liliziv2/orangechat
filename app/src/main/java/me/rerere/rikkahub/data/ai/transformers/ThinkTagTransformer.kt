package me.rerere.rikkahub.data.ai.transformers

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import kotlin.time.Clock
import kotlin.time.Instant

private val THINKING_REGEX = Regex("<think>([\\s\\S]*?)(?:</think>|$)", RegexOption.DOT_MATCHES_ALL)
private val CLOSING_TAG_REGEX = Regex("</think>")

/**
 * 把一条消息里的 `<think>…</think>` 拆成 Reasoning + 可见文本。
 *
 * 抽成不依赖 [TransformerContext] 的纯函数是为了让它可测：[TransformerContext] 的构造函数
 * 要一个 `android.content.Context`，于是 `visualTransform` / `onGenerationFinish` 在纯 JVM
 * 单测里根本调不到，只能靠 Robolectric 或仪器化测试。而这条链路恰好是「内部内容跑到用户
 * 可见位置」的出口之一（Reasoning 是那几个出口里唯一还没有测试兜住的），值得有单测。
 *
 * 两个入口只差 finishedAt 怎么取：
 * - 流式过程中（[ThinkTagTransformer.visualTransform]）：闭合了才算写完，否则 null；
 * - 生成结束（[ThinkTagTransformer.onGenerationFinish]）：一律算写完。
 *
 * 不变量（见 ThinkTagTransformerTest）：
 * - 只处理 ASSISTANT 消息 —— 注入进来的 SYSTEM / USER 内容不可能变成 Reasoning；
 * - `<think>` 里的内容只进 Reasoning，可见文本里不留残渣；
 * - 可见文本也不会被复制进 Reasoning；
 * - 非文本 part（工具调用等）原样保留，不动工具链结构。
 */
internal fun UIMessage.splitThinkTags(
    closedReasoningFinishedAt: Instant,
    openReasoningFinishedAt: Instant?,
): UIMessage {
    if (role != MessageRole.ASSISTANT || !hasPart<UIMessagePart.Text>()) return this
    return copy(
        parts = parts.flatMap { part ->
            if (part is UIMessagePart.Text && THINKING_REGEX.containsMatchIn(part.text)) {
                val hasClosingTag = CLOSING_TAG_REGEX.containsMatchIn(part.text)
                listOf(
                    UIMessagePart.Reasoning(
                        reasoning = THINKING_REGEX.find(part.text)
                            ?.groupValues?.getOrNull(1)?.trim() ?: "",
                        createdAt = createdAt.toInstant(timeZone = TimeZone.currentSystemDefault()),
                        finishedAt = if (hasClosingTag) {
                            closedReasoningFinishedAt
                        } else {
                            openReasoningFinishedAt
                        },
                    ),
                    part.copy(text = part.text.replace(THINKING_REGEX, "")),
                )
            } else {
                listOf(part)
            }
        }
    )
}

// 部分供应商不会返回reasoning parts, 所以需要这个transformer
object ThinkTagTransformer : OutputMessageTransformer {
    override suspend fun visualTransform(
        ctx: TransformerContext,
        messages: List<UIMessage>,
    ): List<UIMessage> = messages.map { message ->
        message.splitThinkTags(
            closedReasoningFinishedAt = Clock.System.now(),
            openReasoningFinishedAt = null,
        )
    }

    override suspend fun onGenerationFinish(
        ctx: TransformerContext,
        messages: List<UIMessage>,
    ): List<UIMessage> {
        val now = Clock.System.now()
        return messages.map { message ->
            message.splitThinkTags(
                closedReasoningFinishedAt = now,
                openReasoningFinishedAt = now,
            )
        }
    }
}
