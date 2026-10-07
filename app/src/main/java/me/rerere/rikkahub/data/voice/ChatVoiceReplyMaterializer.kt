/*
 * 语音条混排机制参考 rikkahub-Jude (https://github.com/Lin-chpin/rikkahub-Jude)，同为 AGPL v3
 */

package me.rerere.rikkahub.data.voice

import android.util.Log
import kotlinx.coroutines.CancellationException
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessageAnnotation
import me.rerere.ai.ui.UIMessagePart
import me.rerere.ai.ui.ChatVoiceReplySegment
import me.rerere.ai.ui.ChatVoiceReplySegmentType

private const val TAG = "ChatVoiceReply"

/**
 * 把一条带语音条标记的回复落地成真正的 parts。
 *
 * VOICE 段逐段交给 [synthesize] 合成（一段一条语音条，"按完整段落生成和播放"），
 * TEXT 段原样保留为文本。合成结果按原顺序写回 parts，非文本 part（图片、工具调用等）
 * 留在末尾不动 —— 它们和语音条无关，丢掉会破坏工具调用链。
 *
 * 任何一段合成失败就整条退化成纯文本（[stripChatVoiceMarkers]）：只补一半语音条会让
 * 对话看起来断了一截，不如老实显示文字。返回 null 表示无需改动这条消息。
 */
suspend fun materializeChatVoiceReply(
    message: UIMessage,
    synthesize: suspend (String) -> UIMessagePart.VoiceMessage?,
): UIMessage? {
    val sourceText = message.parts
        .filterIsInstance<UIMessagePart.Text>()
        .joinToString("\n") { it.text }
    val segments = parseChatVoiceReply(sourceText) ?: return null

    val nonTextParts = message.parts.filter { it !is UIMessagePart.Text }

    val voiceParts = mutableListOf<UIMessagePart>()
    try {
        for (segment in segments) {
            when (segment.type) {
                ChatVoiceSegmentType.TEXT -> {
                    voiceParts.add(UIMessagePart.Text(segment.text))
                }

                ChatVoiceSegmentType.VOICE -> {
                    val voice = synthesize(segment.text)
                    if (voice == null) {
                        Log.w(TAG, "voice synthesis returned null, falling back to plain text")
                        return message.withPlainChatVoiceText(sourceText, nonTextParts)
                    }
                    voiceParts.add(voice)
                }
            }
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "voice synthesis failed, falling back to plain text", e)
        return message.withPlainChatVoiceText(sourceText, nonTextParts)
    }

    if (voiceParts.none { it is UIMessagePart.VoiceMessage }) {
        return message.withPlainChatVoiceText(sourceText, nonTextParts)
    }
    // 挂上混排标记：渲染侧靠它区分「混排」与「自动整条转语音」——两者的 parts
    // 形态都是 Text + VoiceMessage 并存，光看 parts 分不出来（见 ChatMessage 的
    // hideTextForVoice）。这个 annotation 类型本来就为混排设计（Message.kt），
    // 只是此前没人写入。兜底路径（上面的 withPlainChatVoiceText）刻意不挂：
    // 那是纯文本，不是混排。
    val voiceReplyAnnotation = UIMessageAnnotation.ChatVoiceReply(
        segments = segments.map { segment ->
            ChatVoiceReplySegment(
                type = if (segment.type == ChatVoiceSegmentType.VOICE) {
                    ChatVoiceReplySegmentType.VOICE
                } else {
                    ChatVoiceReplySegmentType.TEXT
                },
                text = segment.text,
            )
        }
    )
    return message.copy(
        parts = voiceParts + nonTextParts,
        annotations = message.annotations + voiceReplyAnnotation,
    )
}

/**
 * 兜底：去掉标记，保留一条纯文本 part。
 *
 * 即使内容为空也保留一条空文本，避免 parts 里只剩工具调用导致消息被当成空消息处理。
 */
private fun UIMessage.withPlainChatVoiceText(
    sourceText: String,
    nonTextParts: List<UIMessagePart>,
): UIMessage = copy(
    parts = listOf(UIMessagePart.Text(stripChatVoiceMarkers(sourceText))) + nonTextParts
)
