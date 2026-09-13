/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 语音条混排机制参考 rikkahub-Jude (https://github.com/Lin-chpin/rikkahub-Jude)，同为 AGPL v3
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.voice

import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart

/**
 * 语音条混排回复。
 *
 * 模型在一条回复里用行首标记划分段落，客户端把 VOICE 段合成成语音条、TEXT 段保留为普通文本，
 * 于是同一条 assistant 消息里可以既有文字又有语音条：
 *
 * ```
 * 【文本】先给你看个东西
 * 【语音条】然后这段用我的声音说给你听
 * ```
 *
 * 与上游 Jude 的实现不同，这里不引入独立的 annotation + 平行 UI：解析结果直接写回
 * [UIMessagePart]（VOICE 段变成 [UIMessagePart.VoiceMessage]，TEXT 段是 [UIMessagePart.Text]），
 * 因为橘瓣的 ChatMessage 本来就按 parts 顺序逐个渲染，语音条能直接复用已有的
 * VoiceMessageBubble（连圆角、配色、透明度都是现成的）。
 */
enum class ChatVoiceSegmentType {
    TEXT,
    VOICE,
}

data class ChatVoiceSegment(
    val type: ChatVoiceSegmentType,
    val text: String,
)

/** 语音条标记。全角方括号，模型不容易和 Markdown 混淆。 */
const val CHAT_VOICE_MARKER = "【语音条】"

/** 普通文本标记。 */
const val CHAT_TEXT_MARKER = "【文本】"

private val chatVoiceMarkerRegex = Regex("【(语音条|文本)】")

/**
 * 解析混排回复。
 *
 * 返回 null 表示这条回复里没有语音条，调用方应当按普通文本处理（不要改动消息）。
 * 只有出现过至少一个 [CHAT_VOICE_MARKER] 才算语音条回复 —— 模型只写 【文本】 时
 * 没有任何意义，不值得把消息重写一遍。
 */
fun parseChatVoiceReply(text: String): List<ChatVoiceSegment>? {
    val matches = chatVoiceMarkerRegex.findAll(text).toList()
    if (matches.none { it.groupValues[1] == "语音条" }) return null

    val segments = buildList {
        // 第一个标记之前的内容没有标记，按普通文本处理，避免模型写开场白时内容被吞掉
        val leading = text.substring(0, matches.first().range.first).trim()
        if (leading.isNotEmpty()) {
            add(ChatVoiceSegment(ChatVoiceSegmentType.TEXT, leading))
        }
        matches.forEachIndexed { index, match ->
            val contentStart = match.range.last + 1
            val contentEnd = matches.getOrNull(index + 1)?.range?.first ?: text.length
            val content = text.substring(contentStart, contentEnd).trim()
            if (content.isEmpty()) return@forEachIndexed
            add(
                ChatVoiceSegment(
                    type = if (match.groupValues[1] == "语音条") {
                        ChatVoiceSegmentType.VOICE
                    } else {
                        ChatVoiceSegmentType.TEXT
                    },
                    text = content,
                )
            )
        }
    }
    // 标记后面全是空内容时退化成"没有语音条"
    if (segments.none { it.type == ChatVoiceSegmentType.VOICE }) return null
    return segments
}

/**
 * 去掉所有标记，得到纯文本。
 *
 * 用在合成失败的兜底路径上：宁可把整段话当普通文本显示，也不能让用户看到裸的 【语音条】 标记。
 */
fun stripChatVoiceMarkers(text: String): String = text
    .replace(chatVoiceMarkerRegex, "")
    .lineSequence()
    .joinToString("\n") { it.trim() }
    .trim()

/** 这条消息的文本里是否带语音条标记（用于判断需不需要做合成）。 */
fun UIMessage.hasChatVoiceMarker(): Boolean = parts
    .filterIsInstance<UIMessagePart.Text>()
    .any { it.text.contains(CHAT_VOICE_MARKER) }
