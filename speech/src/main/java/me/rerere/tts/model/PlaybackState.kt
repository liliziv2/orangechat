/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.tts.model

/**
 * 统一的播放状态（对外暴露给 app 侧使用）。
 */
enum class PlaybackStatus {
    Idle,
    Buffering,
    Playing,
    Paused,
    Ended,
    Error
}

data class PlaybackState(
    val status: PlaybackStatus = PlaybackStatus.Idle,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val speed: Float = 1.0f,
    /** 当前段的振幅包络（0..1）。空表示还没解出来，UI 该退回等高矮墙而不是画假波形。 */
    val amplitudes: List<Float> = emptyList(),
    val currentChunkIndex: Int = 0, // 1-based，与 currentChunk StateFlow 对齐
    val totalChunks: Int = 0,
    val errorMessage: String? = null
)


