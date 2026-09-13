/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 语音条混排机制参考 rikkahub-Jude (https://github.com/Lin-chpin/rikkahub-Jude)，同为 AGPL v3
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.voice

import android.util.Log
import kotlinx.coroutines.CancellationException
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart

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
    return message.copy(parts = voiceParts + nonTextParts)
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
