/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.components.chat

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Notification02
import me.rerere.rikkahub.data.ai.tools.local.KnockRequest

/**
 * AI 主动敲门弹窗（knock_user 工具的界面）。
 *
 * 这不是一个「等用户回答才能继续」的表单，而是 AI 自己发起的一次搭话：
 * 用户可以点按钮、可以划掉、也可以完全不理。剩余时间用一条细进度条示意，
 * 走完由工具侧超时收尾，模型会收到「用户没回应」。
 *
 * 进度条只跑一遍不循环，避免长时间挂在最上层持续重绘耗电。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KnockDialog(
    request: KnockRequest,
    assistantName: String,
    onChoose: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    // 剩余时间比例：1f -> 0f 单向推进，timeoutMs<=0 时不显示进度条
    var remainingFraction by remember(request.id) { mutableFloatStateOf(1f) }
    LaunchedEffect(request.id, request.timeoutMs) {
        if (request.timeoutMs <= 0L) return@LaunchedEffect
        val step = 200L
        var elapsed = 0L
        while (elapsed < request.timeoutMs) {
            delay(step)
            elapsed += step
            remainingFraction = (1f - elapsed.toFloat() / request.timeoutMs).coerceIn(0f, 1f)
        }
    }
    val animatedFraction by animateFloatAsState(
        targetValue = remainingFraction,
        animationSpec = tween(200),
        label = "knockRemaining",
    )

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
        ),
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = HugeIcons.Notification02,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = request.title.ifBlank { assistantName.ifBlank { "助手" } },
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Text(
                    text = request.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (request.timeoutMs > 0L) {
                    LinearProgressIndicator(
                        progress = { animatedFraction },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    request.buttons.forEach { label ->
                        TextButton(onClick = { onChoose(label) }) {
                            Text(text = label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}
