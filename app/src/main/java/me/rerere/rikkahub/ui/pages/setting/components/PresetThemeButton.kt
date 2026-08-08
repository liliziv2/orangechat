/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.setting.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Tick01
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import me.rerere.rikkahub.ui.theme.LocalDarkMode
import me.rerere.rikkahub.ui.theme.PresetTheme
import me.rerere.rikkahub.ui.theme.PresetThemes

@Composable
fun PresetThemeButton(
    theme: PresetTheme,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val darkMode = LocalDarkMode.current
    val scheme = theme.getColorScheme(darkMode)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = LocalIndication.current,
                onClick = {
                    onClick()
                }
            )
            .padding(8.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            // 主题预览卡片：顶部色条 + 主色圆点
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(scheme.primaryContainer)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .background(scheme.primary)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(scheme.secondaryContainer)
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(scheme.tertiary)
                )
            )
            if (selected) {
                Icon(
                    HugeIcons.Tick01,
                    contentDescription = null,
                    tint = scheme.contentColorFor(scheme.onPrimary),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        ProvideTextStyle(
            value = MaterialTheme.typography.labelMedium.copy(
                color = scheme.primary,
                textAlign = TextAlign.Center,
            )
        ) {
            theme.name()
        }
    }
}

private const val THEME_GRID_COLUMNS = 4

@Composable
fun PresetThemeButtonGroup(
    themeId: String,
    modifier: Modifier = Modifier,
    onChangeTheme: (String) -> Unit,
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = THEME_GRID_COLUMNS,
    ) {
        PresetThemes.fastForEach { theme ->
            key(theme.id) {
                PresetThemeButton(
                    theme = theme,
                    selected = theme.id == themeId,
                    modifier = Modifier.weight(1f),
                    onClick = {
                        onChangeTheme(theme.id)
                    },
                )
            }
        }
        // 补齐最后一行的空位, 让每列宽度保持一致
        repeat((THEME_GRID_COLUMNS - PresetThemes.size % THEME_GRID_COLUMNS) % THEME_GRID_COLUMNS) {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PresetThemeButtonPreview() {
    var themeId by remember { mutableStateOf("ocean") }
    PresetThemeButtonGroup(
        themeId = themeId,
        onChangeTheme = { themeId = it }
    )
}
