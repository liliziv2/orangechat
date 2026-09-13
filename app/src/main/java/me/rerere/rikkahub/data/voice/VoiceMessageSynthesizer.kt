/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.voice

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.utils.stripMarkdown
import me.rerere.tts.model.AudioFormat
import me.rerere.tts.model.TTSRequest
import me.rerere.tts.provider.TTSManager
import me.rerere.tts.provider.TTSProviderSetting
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

private const val TAG = "VoiceMessageSynthesizer"
private const val VOICE_MESSAGES_DIR = "voice_messages"

/**
 * 把文本合成成可持久化的语音条。
 *
 * 从 ui/hooks/TTS.kt 的 createVoiceMessage 抽出来，因为语音条混排要在 ChatService（非 UI 层）
 * 里做合成，而原来的实现绑在 Composable 的 CustomTtsState 上拿不到。
 *
 * 与实时朗读的区别：不进播放队列，完整收完音频后写入 files/voice_messages，
 * 之后重听直接播本地文件，不再请求 provider、不再消耗字数额度。
 */
class VoiceMessageSynthesizer(
    private val context: Context,
    private val ttsManager: TTSManager,
) {
    /**
     * @return 合成好的语音条 part；文本为空、没有可用 provider 或合成失败时返回 null。
     */
    suspend fun synthesize(
        text: String,
        provider: TTSProviderSetting,
    ): UIMessagePart.VoiceMessage? = withContext(Dispatchers.IO) {
        val processed = text.stripMarkdown()
        if (processed.isBlank()) return@withContext null

        val voiceDir = File(context.filesDir, VOICE_MESSAGES_DIR).apply { mkdirs() }
        // 先写临时文件，成功后再改名：避免半截音频被当成可播放的语音条
        val tempFile = File(voiceDir, ".${UUID.randomUUID()}.part")
        var extension = "mp3"
        try {
            FileOutputStream(tempFile).use { output ->
                ttsManager.generateSpeech(provider, TTSRequest(processed)).collect { chunk ->
                    extension = chunk.format.toFileExtension()
                    if (chunk.data.isNotEmpty()) output.write(chunk.data)
                }
            }
            if (tempFile.length() == 0L) {
                tempFile.delete()
                Log.w(TAG, "synthesize: provider returned empty audio")
                return@withContext null
            }

            val voiceFile = File(
                voiceDir,
                "voice-${System.currentTimeMillis()}-${UUID.randomUUID()}.$extension"
            )
            if (!tempFile.renameTo(voiceFile)) {
                tempFile.copyTo(voiceFile, overwrite = true)
                tempFile.delete()
            }

            UIMessagePart.VoiceMessage(
                url = Uri.fromFile(voiceFile).toString(),
                duration = voiceFile.readDurationMs(),
                transcript = processed,
            )
        } catch (e: Exception) {
            Log.e(TAG, "synthesize failed", e)
            tempFile.delete()
            null
        }
    }

    /** 时长只用于 UI 显示，取不到就当 0，不影响播放。 */
    private fun File.readDurationMs(): Long = runCatching {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, Uri.fromFile(this))
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
        } finally {
            retriever.release()
        }
    }.getOrDefault(0L)
}

internal fun AudioFormat.toFileExtension(): String = when (this) {
    AudioFormat.MP3 -> "mp3"
    AudioFormat.WAV -> "wav"
    AudioFormat.OGG -> "ogg"
    AudioFormat.AAC -> "aac"
    AudioFormat.OPUS -> "opus"
    AudioFormat.PCM -> "pcm"
}
