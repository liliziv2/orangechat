package me.rerere.rikkahub.ui.theme.presets.custom

import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.theme.PresetTheme

/*
 * 奶油玫瑰 Cream Rose
 * 一个主题两态，跟随橘瓣的深浅切换（ColorMode.SYSTEM/LIGHT/DARK）。
 *
 * 层级参照 Tidal Echo（web/index.html）：底图是主体，UI 只是压在底图上的低不透明度、
 * 低饱和度色层。所以这里不引入任何黑色描边和重投影 —— 层次全部由「面色阶差」做出来：
 * 相邻两档只差几个 RGB 单位，读起来是同一张纸上的几层，而不是几块叠起来的板。
 *
 * 八个锚点色，Day / Night 各自独立给出（夜间不是由日间降亮度推出来的）：
 *   Day   Background #F5EEE9 · Main Text #332827 · Global Text #403332
 *         User Bubble #B86F79 · AI Bubble #F1E5DE · Thinking Bubble #E1D0C5
 *         Accent #A95362 · Input #EEE1DB
 *   Night Background #181416 · Main Text #EEE5E5 · Global Text #D8CCCE
 *         User Bubble #8E4C59 · AI Bubble #2D2527 · Thinking Bubble #3B2F30
 *         Accent #CF7D8B · Input #272022
 *
 * 锚点落到 M3 槽位，按聊天界面「实际读哪个槽位」对齐，不是按槽位名字：
 *   Background      -> background / surface
 *   Main Text       -> onSurface / onBackground
 *   Global Text     -> onSurfaceVariant
 *   Accent          -> primary
 *   User Bubble     -> secondaryContainer
 *   AI Bubble       -> surfaceContainerHigh
 *   Thinking Bubble -> tertiaryContainer（本主题的思考卡片改读这一槽位）
 *   Input           -> surfaceContainerLow
 *
 * 其余槽位在同一色相族内插值补齐，只做明度阶差、不加彩度：
 *   surfaceContainerLowest 是比 background 再亮一档的「纸面」，
 *   surfaceContainer / surfaceVariant 夹在 Input 与 AI Bubble 之间，
 *   surfaceContainerHighest / surfaceDim 是比思考气泡再深一档的底。
 *
 * 两处刻意的取舍（是色卡本身带来的，不是笔误）：
 *   1) Input #EEE1DB 比 AI Bubble #F1E5DE 略深，而代码把输入框读 surfaceContainerLow、
 *      把助手气泡读 surfaceContainerHigh，于是 Low 比 High 深了 3 个 RGB 单位 ——
 *      这正是「气泡与输入区靠轻微明度差区分」本身，视觉上读不出倒置。
 *   2) onSecondaryContainer 取正文色而不是「压在用户气泡上的浅色」：聊天界面里
 *      助手语音条的未播放波形用它，而它压在浅色的助手气泡上，必须是深色才看得见。
 *      （用户气泡里的正文走的是 onSurface / onBackground。）
 *
 * 描边档 outline / outlineVariant 全部是有彩度的暖灰，没有一根黑描边。
 */

val CreamRoseThemePreset by lazy {
    PresetTheme(
        id = "creamrose",
        name = {
            Text(stringResource(id = R.string.theme_name_creamrose))
        },
        standardLight = lightScheme,
        standardDark = darkScheme,
    )
}

private val lightScheme = lightColorScheme(
    // Accent #A95362
    primary = Color(0xFFA95362),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEBDAD8),
    onPrimaryContainer = Color(0xFF763A45),
    secondary = Color(0xFF7E5259),
    onSecondary = Color(0xFFFFFFFF),
    // User Bubble #B86F79
    secondaryContainer = Color(0xFFB86F79),
    onSecondaryContainer = Color(0xFF403332),
    tertiary = Color(0xFF8A6A5F),
    onTertiary = Color(0xFFFFFFFF),
    // Thinking Bubble #E1D0C5
    tertiaryContainer = Color(0xFFE1D0C5),
    onTertiaryContainer = Color(0xFF403332),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    // Background #F5EEE9
    background = Color(0xFFF5EEE9),
    // Main Text #332827
    onBackground = Color(0xFF332827),
    surface = Color(0xFFF5EEE9),
    onSurface = Color(0xFF332827),
    surfaceVariant = Color(0xFFEDE4DD),
    // Global Text #403332
    onSurfaceVariant = Color(0xFF403332),
    outline = Color(0xFFB9A79C),
    outlineVariant = Color(0xFFDCCCC2),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF332827),
    inverseOnSurface = Color(0xFFF5EEE9),
    inversePrimary = Color(0xFFD79AA4),
    surfaceDim = Color(0xFFD9C9BF),
    surfaceBright = Color(0xFFF7F2ED),
    surfaceContainerLowest = Color(0xFFF8F3EF),
    // Input #EEE1DB
    surfaceContainerLow = Color(0xFFEEE1DB),
    surfaceContainer = Color(0xFFEFE4DD),
    // AI Bubble #F1E5DE
    surfaceContainerHigh = Color(0xFFF1E5DE),
    surfaceContainerHighest = Color(0xFFDDCBC1),
)

private val darkScheme = darkColorScheme(
    // Accent #CF7D8B
    primary = Color(0xFFCF7D8B),
    onPrimary = Color(0xFF3A1A20),
    primaryContainer = Color(0xFF5A2F38),
    onPrimaryContainer = Color(0xFFF5DDE0),
    secondary = Color(0xFFC79AA2),
    onSecondary = Color(0xFF3A1A20),
    // User Bubble #8E4C59
    secondaryContainer = Color(0xFF8E4C59),
    onSecondaryContainer = Color(0xFFD8CCCE),
    tertiary = Color(0xFFC4A79C),
    onTertiary = Color(0xFF3A1A20),
    // Thinking Bubble #3B2F30
    tertiaryContainer = Color(0xFF3B2F30),
    onTertiaryContainer = Color(0xFFD8CCCE),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    // Background #181416
    background = Color(0xFF181416),
    // Main Text #EEE5E5
    onBackground = Color(0xFFEEE5E5),
    surface = Color(0xFF181416),
    onSurface = Color(0xFFEEE5E5),
    surfaceVariant = Color(0xFF3A2E30),
    // Global Text #D8CCCE
    onSurfaceVariant = Color(0xFFD8CCCE),
    outline = Color(0xFF7A686A),
    outlineVariant = Color(0xFF3F3335),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFEEE5E5),
    inverseOnSurface = Color(0xFF332827),
    inversePrimary = Color(0xFFA95362),
    surfaceDim = Color(0xFF141112),
    surfaceBright = Color(0xFF4B3D3F),
    surfaceContainerLowest = Color(0xFF100E0F),
    // Input #272022
    surfaceContainerLow = Color(0xFF272022),
    surfaceContainer = Color(0xFF2A2224),
    // AI Bubble #2D2527
    surfaceContainerHigh = Color(0xFF2D2527),
    surfaceContainerHighest = Color(0xFF45383A),
)
