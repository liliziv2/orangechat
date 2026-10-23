/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */
package me.rerere.rikkahub.ui.components.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.data.datastore.DisplayMaterialMode
import me.rerere.rikkahub.ui.theme.LocalMaterialMode

/**
 * 聊天页控件统一的材质外壳 —— 与消息气泡同一套材质规则
 * (LocalMaterialMode 同源, 不各写一套判断):
 *
 * - FLAT / FOLLOW_THEME: 轻底色 (surfaceContainerHigh 60%) + 主题 small 圆角,
 *   无描边 —— 平面材质下控件不再退化成裸图标/裸字, 点击热区清晰可辨;
 * - TRANSLUCENT: 半透明 surface 底 (alpha 0.55) + 发丝描边 (onSurface 0.07,
 *   与气泡 GLASS/TRANSLUCENT 边框 alpha 一致);
 * - GLASS: 支持 realtime blur 的场景由调用方叠加 hazeEffect (气泡同款参数),
 *   不支持时回退到与 TRANSLUCENT 相同的静态半透明底 —— 保证图标/文字
 *   在聊天背景上可读。
 *
 * 普通态即上述样式; 按下/选中反馈由 content 内层 (ToggleSurface/ReasoningButton
 * 自己的 checked 色) 提供, 外壳不重复。
 * 尺寸与内边距由调用方给 (顶栏 38dp 圆 / 助手条 chip / Thinking chip),
 * 外壳只负责材质, 不改热区。
 */
@Composable
fun MaterialActionSurface(
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable () -> Unit,
) {
    val mode = LocalMaterialMode.current
    val containerColor = when (mode) {
        DisplayMaterialMode.FLAT,
        DisplayMaterialMode.FOLLOW_THEME ->
            MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)
        DisplayMaterialMode.TRANSLUCENT ->
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
        DisplayMaterialMode.GLASS ->
            // 不支持实时模糊时调用方已有静态回退, 这里给一致的基础透明度
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.45f)
    }
    val borderColor = when (mode) {
        DisplayMaterialMode.TRANSLUCENT,
        DisplayMaterialMode.GLASS ->
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
        else -> null
    }
    Box(
        modifier = modifier
            .clip(shape)
            .background(containerColor)
            .then(
                if (borderColor != null) {
                    // 与输入框边框同规格（1dp / onSurface 0.07），轮廓质感一致。
                    Modifier.border(1.dp, borderColor, shape)
                } else {
                    Modifier
                }
            ),
        contentAlignment = contentAlignment,
    ) {
        content()
    }
}
