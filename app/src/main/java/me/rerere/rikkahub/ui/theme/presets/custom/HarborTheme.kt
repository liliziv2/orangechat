/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.theme.presets.custom

import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.theme.PresetTheme

/*
 * 海港 Harbor —— 中性冷调。
 *
 * 色彩关系取自 Tidal Echo 的 Harbor 配色与视觉逻辑：冷灰蓝、低饱和、
 * 轻表面层级、极弱边框、极弱阴影。只借色彩关系与层级思路，
 * 没有引入 Tidal Echo 的 HTML / CSS，也没有引入它的 Web UI。
 *
 * Light 锚点（Tidal Echo Harbor 定义）：
 *   底 #F4F2EF · 主文字 #36404B · 次级文字 #5E6B78 · 最淡 #9197A0
 *   AI 气泡 #EAE6E4 · 用户气泡 #E1E0E3 · 强调 #4A5D6C · 输入区 #EFEBE8
 *   发丝线 rgba(74,93,108,.20)
 *
 * 锚点落到 M3 槽位，按聊天界面「实际读哪个槽位」对齐，不是按槽位名字：
 *   Background      -> background / surface
 *   Main Text       -> onSurface / onBackground
 *   Soft Text       -> onSurfaceVariant
 *   Accent          -> primary
 *   AI Bubble       -> surfaceContainerHigh
 *   User Bubble     -> secondaryContainer
 *   Input           -> surfaceContainerLow
 *   Hairline        -> outlineVariant
 *
 * Dark 不是 Light 降亮度推出来的，是按同一套「冷灰蓝 + 低饱和」关系独立给的一套：
 * 底压到近黑冷调，表面层级只靠几个 RGB 单位的阶差拉开，边框压到几乎看不见。
 *
 * 两处刻意的取舍：
 *   1) outlineVariant 取的是「发丝线叠在底色上」的合成值，而不是一个中间灰 ——
 *      这样它读起来是底色上的一道极弱分隔，而不是一条描边。
 *   2) error 保持 M3 的语义红，不跟着冷灰蓝走 —— 它是错误语义，不是装饰色。
 */

val HarborThemePreset by lazy {
    PresetTheme(
        id = "harbor",
        name = {
            Text(stringResource(id = R.string.theme_name_harbor))
        },
        standardLight = lightScheme,
        standardDark = darkScheme,
    )
}

private val lightScheme = lightColorScheme(
    // Accent #4A5D6C
    primary = Color(0xFF4A5D6C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEAE6E4),
    // Main Text #36404B
    onPrimaryContainer = Color(0xFF36404B),
    // Soft Text #5E6B78
    secondary = Color(0xFF5E6B78),
    onSecondary = Color(0xFFFFFFFF),
    // User Bubble #E1E0E3
    secondaryContainer = Color(0xFFE1E0E3),
    onSecondaryContainer = Color(0xFF36404B),
    tertiary = Color(0xFF5F7385),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFE7EAEC),
    onTertiaryContainer = Color(0xFF36404B),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    // Background #F4F2EF
    background = Color(0xFFF4F2EF),
    onBackground = Color(0xFF36404B),
    surface = Color(0xFFF4F2EF),
    onSurface = Color(0xFF36404B),
    surfaceVariant = Color(0xFFEAE6E4),
    onSurfaceVariant = Color(0xFF5E6B78),
    // Faint #9197A0
    outline = Color(0xFF9197A0),
    // Hairline rgba(74,93,108,.20) over #F4F2EF
    outlineVariant = Color(0xFFD5D7D8),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF2C3134),
    inverseOnSurface = Color(0xFFF1F2F4),
    inversePrimary = Color(0xFF8A9DAD),
    surfaceDim = Color(0xFFE2E0DD),
    surfaceBright = Color(0xFFFAF9F7),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    // Input #EFEBE8
    surfaceContainerLow = Color(0xFFEFEBE8),
    surfaceContainer = Color(0xFFECE9E6),
    // AI Bubble #EAE6E4
    surfaceContainerHigh = Color(0xFFEAE6E4),
    surfaceContainerHighest = Color(0xFFE1DEDC),
)

private val darkScheme = darkColorScheme(
    primary = Color(0xFF8FA6B8),
    onPrimary = Color(0xFF1E2A34),
    primaryContainer = Color(0xFF33454F),
    onPrimaryContainer = Color(0xFFD3DCE2),
    secondary = Color(0xFFB4BEC6),
    onSecondary = Color(0xFF1E2A34),
    secondaryContainer = Color(0xFF3C4750),
    onSecondaryContainer = Color(0xFFD3DCE2),
    tertiary = Color(0xFFA9BAC7),
    onTertiary = Color(0xFF1E2A34),
    tertiaryContainer = Color(0xFF37454F),
    onTertiaryContainer = Color(0xFFD3DCE2),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF121517),
    onBackground = Color(0xFFE3E6E8),
    surface = Color(0xFF121517),
    onSurface = Color(0xFFE3E6E8),
    surfaceVariant = Color(0xFF2A3238),
    onSurfaceVariant = Color(0xFFC3CACE),
    outline = Color(0xFF8C959B),
    // 极弱边框：只比底亮几个单位，读作分隔而不是描边
    outlineVariant = Color(0xFF262D33),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE3E6E8),
    inverseOnSurface = Color(0xFF2C3134),
    inversePrimary = Color(0xFF4A5D6C),
    surfaceDim = Color(0xFF121517),
    surfaceBright = Color(0xFF343B41),
    surfaceContainerLowest = Color(0xFF0C0F11),
    surfaceContainerLow = Color(0xFF181C1F),
    surfaceContainer = Color(0xFF1D2124),
    surfaceContainerHigh = Color(0xFF262B2F),
    surfaceContainerHighest = Color(0xFF313840),
)
