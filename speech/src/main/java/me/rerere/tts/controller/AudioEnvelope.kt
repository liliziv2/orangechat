/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.tts.controller

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.log10
import kotlin.math.sqrt

private const val TAG = "AudioEnvelope"
private const val DEFAULT_BUCKETS = 32
private const val DECODE_TIMEOUT_US = 10_000L

/**
 * 把音频文件解码成振幅包络，用来画真实波形。
 *
 * 之前两处语音条都是拿固定种子生成假波形（全局条 Random(42)、气泡条 Random(url.hashCode)），
 * 所有语音长得一模一样。这里读真实 PCM，按时间等分成 [buckets] 段，每段取 RMS 再压到 0..1。
 *
 * 解码失败返回空列表，调用方自己决定退回什么（不抛异常，波形只是装饰，不该弄崩消息列表）。
 */
fun extractAmplitudeEnvelope(file: File, buckets: Int = DEFAULT_BUCKETS): List<Float> {
    if (!file.exists() || file.length() == 0L) return emptyList()
    return runCatching { decodeEnvelope(file, buckets) }
        .onFailure { Log.w(TAG, "envelope 提取失败: ${file.name}", it) }
        .getOrDefault(emptyList())
}

/**
 * 内存字节版。全局朗读条播的是 [me.rerere.tts.model.TTSResponse.audioData]，不落盘，
 * 走不了文件那条路，所以先写临时文件再解——MediaExtractor 不接受裸 ByteArray。
 */
fun extractAmplitudeEnvelope(
    audioData: ByteArray,
    cacheDir: File,
    buckets: Int = DEFAULT_BUCKETS,
): List<Float> {
    if (audioData.isEmpty()) return emptyList()
    val temp = File(cacheDir, "envelope-${System.nanoTime()}.tmp")
    return try {
        temp.writeBytes(audioData)
        extractAmplitudeEnvelope(temp, buckets)
    } catch (e: Exception) {
        Log.w(TAG, "envelope 提取失败（内存字节）", e)
        emptyList()
    } finally {
        temp.delete()
    }
}

private fun decodeEnvelope(file: File, buckets: Int): List<Float> {
    val extractor = MediaExtractor()
    extractor.setDataSource(file.absolutePath)
    try {
        val trackIndex = (0 until extractor.trackCount).firstOrNull { index ->
            extractor.getTrackFormat(index)
                .getString(MediaFormat.KEY_MIME)
                ?.startsWith("audio/") == true
        } ?: return emptyList()

        extractor.selectTrack(trackIndex)
        val format = extractor.getTrackFormat(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: return emptyList()
        val codec = MediaCodec.createDecoderByType(mime)
        codec.configure(format, null, null, 0)
        codec.start()
        try {
            return drainDecoder(extractor, codec, buckets)
        } finally {
            runCatching { codec.stop() }
            codec.release()
        }
    } finally {
        extractor.release()
    }
}

private fun drainDecoder(
    extractor: MediaExtractor,
    codec: MediaCodec,
    buckets: Int,
): List<Float> {
    // 先把整段的 RMS 平方和按桶累积，最后再开方，避免保留全量 PCM 占内存。
    val sums = DoubleArray(buckets)
    val counts = LongArray(buckets)
    val bufferInfo = MediaCodec.BufferInfo()
    var inputDone = false
    var outputDone = false
    var totalSamples = 0L
    // 时长未知时按已解出的样本数兜底分桶，所以先收集再定位。
    val pending = ArrayList<ShortArray>()

    while (!outputDone) {
        if (!inputDone) {
            val inputIndex = codec.dequeueInputBuffer(DECODE_TIMEOUT_US)
            if (inputIndex >= 0) {
                val inputBuffer = codec.getInputBuffer(inputIndex)
                val sampleSize = if (inputBuffer != null) {
                    extractor.readSampleData(inputBuffer, 0)
                } else -1
                if (sampleSize < 0) {
                    codec.queueInputBuffer(
                        inputIndex, 0, 0, 0,
                        MediaCodec.BUFFER_FLAG_END_OF_STREAM,
                    )
                    inputDone = true
                } else {
                    codec.queueInputBuffer(inputIndex, 0, sampleSize, extractor.sampleTime, 0)
                    extractor.advance()
                }
            }
        }

        val outputIndex = codec.dequeueOutputBuffer(bufferInfo, DECODE_TIMEOUT_US)
        if (outputIndex >= 0) {
            if (bufferInfo.size > 0) {
                val outputBuffer = codec.getOutputBuffer(outputIndex)
                if (outputBuffer != null) {
                    outputBuffer.position(bufferInfo.offset)
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size)
                    val shorts = outputBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                    val chunk = ShortArray(shorts.remaining())
                    shorts.get(chunk)
                    pending.add(chunk)
                    totalSamples += chunk.size
                }
            }
            codec.releaseOutputBuffer(outputIndex, false)
            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                outputDone = true
            }
        }
    }

    if (totalSamples == 0L) return emptyList()

    val perBucket = (totalSamples / buckets).coerceAtLeast(1L)
    var sampleIndex = 0L
    for (chunk in pending) {
        for (sample in chunk) {
            val bucket = ((sampleIndex / perBucket).toInt()).coerceAtMost(buckets - 1)
            val value = sample.toDouble()
            sums[bucket] += value * value
            counts[bucket]++
            sampleIndex++
        }
    }

    return (0 until buckets).map { i ->
        if (counts[i] == 0L) return@map 0f
        val rms = sqrt(sums[i] / counts[i])
        normalizeRms((rms / Short.MAX_VALUE).toFloat())
    }.let(::rescale)
}

/** 跟 ASR 侧 calculateRmsAmplitude 用同一套 -60dB~0dB 映射，两处波形手感一致。 */
private fun normalizeRms(linear: Float): Float {
    if (linear < 1e-6f) return 0f
    val db = 20f * log10(linear)
    return ((db + 60f) / 60f).coerceIn(0f, 1f)
}

/**
 * TTS 输出普遍动态范围窄，直接画会是一条几乎齐平的矮墙。
 * 按本段自身的最大值拉伸，让起伏看得出来。
 */
private fun rescale(values: List<Float>): List<Float> {
    val max = values.maxOrNull() ?: return values
    if (max <= 0.01f) return values.map { 0.15f }
    return values.map { (it / max).coerceIn(0.12f, 1f) }
}
