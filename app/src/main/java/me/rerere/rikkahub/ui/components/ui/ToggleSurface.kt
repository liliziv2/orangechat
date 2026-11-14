package me.rerere.rikkahub.ui.components.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

@Composable
fun ToggleSurface(
    checked: Boolean,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(50),
    onClick: () -> Unit = {},
    // 内容对齐。默认 TopStart = 保持原行为（内容贴着起点排）。
    // 需要「图标在固定尺寸盒子里居中」的调用点才显式传 Center。
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable () -> Unit
) {
    val contentColor =
        if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        contentColor = contentColor,
        modifier = modifier,
        shape = shape,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        ProvideTextStyle(MaterialTheme.typography.labelLarge) {
            // Surface 会把最小约束传给子节点，Box 因此拿到按钮盒的完整尺寸，
            // contentAlignment 才能真正在盒子里居中，而不是围着内容自身打转。
            Box(contentAlignment = contentAlignment) {
                content()
            }
        }
    }
}
