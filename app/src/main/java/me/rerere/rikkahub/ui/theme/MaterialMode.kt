/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.data.datastore.DisplayMaterialMode

// 全局材质边框。
//
// 这条边框被十几个设置页 / 扩展页 / 抽屉下拉共用（都走 materialModeBorderStroke()），
// 原来 alpha 0.18 让每个容器都描一圈可见的框 —— 叠加起来就是"整个 App 都像玻璃"
// 的主要来源之一。玻璃质感应该只属于消息气泡，普通页面容器不需要靠描边交代材质。
//
// 0.18 → 0.07：留一点材质暗示，但不再抢层级。气泡自己的边框（GLASS_BUBBLE_BORDER_ALPHA
// 等，在 ChatMessage.kt）不受这里影响。
private const val MATERIAL_BORDER_ALPHA = 0.07f

@Composable
fun materialModeBorderStroke(): BorderStroke? = if (LocalMaterialMode.current.hasMaterialBorder) {
    BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = MATERIAL_BORDER_ALPHA),
    )
} else {
    null
}

@Composable
fun Modifier.materialTopAppBarDivider(): Modifier {
    if (!LocalMaterialMode.current.hasMaterialBorder) return this

    // 同上：TopBar 底部那条线也是"全局玻璃感"的一部分，跟边框用同一个 alpha
    val dividerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = MATERIAL_BORDER_ALPHA)
    return drawWithContent {
        drawContent()
        val strokeWidth = 1.dp.toPx()
        val y = size.height - strokeWidth / 2f
        drawLine(
            color = dividerColor,
            start = Offset(0f, y),
            end = Offset(size.width, y),
            strokeWidth = strokeWidth,
        )
    }
}

private val DisplayMaterialMode.hasMaterialBorder: Boolean
    get() = this == DisplayMaterialMode.TRANSLUCENT || this == DisplayMaterialMode.GLASS