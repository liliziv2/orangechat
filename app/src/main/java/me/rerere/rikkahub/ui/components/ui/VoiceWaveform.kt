/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.components.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 语音波形。全局朗读条与消息气泡共用这一个实现。
 *
 * 之前两处各画一遍 Canvas，柱宽/间距/圆角都是各自写死的字面量，改一处另一处不动。
 *
 * @param amplitudes 0..1 的振幅包络。空列表时退回一条等高矮墙，不画随机假波形——
 *   假波形会让人以为读到了音频内容。
 * @param progress 已播放比例 0..1，决定染色分界。
 * @param onSeek 传入即可拖动/点击定位，参数是 0..1 的目标比例；传 null 表示该条不支持定位。
 */
@Composable
fun VoiceWaveform(
    amplitudes: List<Float>,
    progress: Float,
    playedColor: Color,
    unplayedColor: Color,
    modifier: Modifier = Modifier,
    barWidth: Dp = 2.5.dp,
    onSeek: ((Float) -> Unit)? = null,
) {
    val bars = if (amplitudes.isEmpty()) List(24) { 0.18f } else amplitudes
    val seekModifier = if (onSeek != null) {
        Modifier
            .pointerInput(bars.size) {
                detectTapGestures { offset ->
                    onSeek((offset.x / size.width.toFloat()).coerceIn(0f, 1f))
                }
            }
            .pointerInput(bars.size) {
                detectHorizontalDragGestures { change, _ ->
                    onSeek((change.position.x / size.width.toFloat()).coerceIn(0f, 1f))
                }
            }
    } else Modifier

    Canvas(modifier = modifier.then(seekModifier)) {
        val barWidthPx = barWidth.toPx()
        val count = bars.size
        val gap = (size.width - barWidthPx * count) / (count - 1).coerceAtLeast(1)
        // 用比例比较而不是取整算“已播柱数”，短波形上进度才不会一跳一大格。
        val playedRatio = progress.coerceIn(0f, 1f)
        bars.forEachIndexed { index, ratio ->
            val barHeight = size.height * ratio.coerceIn(0.12f, 1f)
            val x = index * (barWidthPx + gap)
            val y = (size.height - barHeight) / 2f
            val center = (index + 0.5f) / count
            drawRoundRect(
                color = if (center <= playedRatio) playedColor else unplayedColor,
                topLeft = Offset(x, y),
                size = Size(barWidthPx, barHeight),
                cornerRadius = CornerRadius(barWidthPx / 2f, barWidthPx / 2f),
            )
        }
    }
}
