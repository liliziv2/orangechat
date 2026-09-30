package me.rerere.rikkahub.data.ai.transformers

import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.ai.pills.Pill
import me.rerere.rikkahub.data.ai.pills.PillRegistry
import me.rerere.rikkahub.data.ai.pills.PillStore

/**
 * 把挂在这条消息上的药丸拼到它**最前面**，只改发给模型的那一份。
 *
 * 落在输入侧（而不是把文本写进历史）是关键：`GenerationHandler.generateInternal`
 * 里发给 provider 的是 `internalMessages`，而回写进会话的是另一份没有被输入转换过的
 * 列表（那里有一处 `var messages = messages` 遮蔽了参数），所以这里拼进去的文本
 * 不会落库 —— 「用完即焚」是结构上成立的，不靠事后去历史里删原文。
 */
object PillTransformer : InputMessageTransformer {
    override suspend fun transform(
        ctx: TransformerContext,
        messages: List<UIMessage>,
    ): List<UIMessage> {
        if (PillStore.pending.value.isEmpty()) return messages
        return messages.map { message ->
            val pills = PillRegistry.resolve(PillStore.attachedTo(message.id))
            if (pills.isEmpty()) message else message.withPillBlock(pills)
        }
    }
}

/** 注入块的标题。上游格式是 `[药丸 · 名]`，照抄，不另立一套标记。 */
private const val PILL_BLOCK_PREFIX = "[药丸 · "

private fun UIMessage.withPillBlock(pills: List<Pill>): UIMessage {
    val block = buildString {
        pills.forEach { pill ->
            append(PILL_BLOCK_PREFIX).append(pill.name).append("]\n")
            append(pill.text).append("\n\n")
        }
    }
    // 必须**拼进第一条 Text part**，不能另加一个 Text part。
    // OpenAI 兼容 provider 用 `isOnlyTextPart()` 决定走 `content` 字符串还是 content 数组：
    // 多一条 Text part 会把消息从「一句文本」变成「一段内容数组」，消息形状变了，
    // 踩到的面比收益大。没有 Text part（纯图片消息）时才退化成在最前面补一条。
    val index = parts.indexOfFirst { it is UIMessagePart.Text }
    val newParts = if (index >= 0) {
        val first = parts[index] as UIMessagePart.Text
        parts.toMutableList().also { it[index] = first.copy(text = block + first.text) }
    } else {
        listOf(UIMessagePart.Text(block)) + parts
    }
    return copy(parts = newParts)
}
