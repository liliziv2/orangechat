package me.rerere.rikkahub.ui.theme.presets

import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.theme.PresetTheme

/**
 * 暖灯苔绿 Warm Lamp · Moss Green（第五个预设）。
 *
 * 日间：奶油纸底 + 苔藓绿主色 + 木棕正文，暖灯金只做少量点缀。
 * 夜间：深苔绿底 + 暖灯金强调，不用纯黑大面积铺底、不用荧光绿。
 *
 * 与其它预设一样走 OrangeChat 自己的 PresetTheme 架构（standardLight / standardDark），
 * 不复用上游主题代码结构。材质模式（FLAT/TRANSLUCENT/GLASS）与透明度滑块
 * 完全独立，本文件只提供配色。
 */
val WarmLampMossThemePreset by lazy {
    PresetTheme(
        id = "warmlampmoss",
        name = { Text(stringResource(R.string.theme_name_warmlampmoss)) },
        standardLight = warmLampLightScheme,
        standardDark = warmLampDarkScheme,
    )
}

// ── 日间 Warm Lamp · Moss Green ───────────────────────────────────────
// 规格色：页面背景 #F0E8D8、普通表面/输入框 #E2D7C3、助手气泡 #E9DDC7、
// 用户气泡 #D3D2B4、思维链 #D8D6C0、主色 #77764A、正文 #302C24、
// 全局文字 #3B3429、暖灯金 #D49A54。
private val mossPrimaryLight = Color(0xFF77764A)
private val mossOnPrimaryLight = Color(0xFFFFFFFF)
private val mossPrimaryContainerLight = Color(0xFFE9DDC7)
private val mossOnPrimaryContainerLight = Color(0xFF3B3429)
private val mossSecondaryLight = Color(0xFF8A8763)
private val mossOnSecondaryLight = Color(0xFFFFFFFF)
private val mossSecondaryContainerLight = Color(0xFFD8D6C0)
private val mossOnSecondaryContainerLight = Color(0xFF3B3429)
// 暖灯金：点缀色（选中态、少量图标、装饰）
private val mossTertiaryLight = Color(0xFFD49A54)
private val mossOnTertiaryLight = Color(0xFF3B3429)
// 思维链表面专用槽位（日间）。
//
// 任务单 §六 日间规格要求「思维链表面 #D8D6C0」。而这个主题在
// Theme.kt 的 THEME_THINKING_CONTAINER_THEMES 里，思维链卡片转读
// tertiaryContainer —— 也就是说 tertiaryContainer 的日间取值直接就是
// 思维链的显色，必须是 #D8D6C0，而不是之前的奶油金 #F2E2C8。
// 暖灯金继续只由 tertiary（#D49A54）承担小面积点缀。
private val mossTertiaryContainerLight = Color(0xFFD8D6C0)
private val mossOnTertiaryContainerLight = Color(0xFF5A4526)
private val mossErrorLight = Color(0xFF9B4A3C)
private val mossOnErrorLight = Color(0xFFFFFFFF)
private val mossErrorContainerLight = Color(0xFFF5DDD6)
private val mossOnErrorContainerLight = Color(0xFF5C2418)

private val mossBackgroundLight = Color(0xFFF0E8D8)
private val mossOnBackgroundLight = Color(0xFF3B3429)
private val mossSurfaceLight = Color(0xFFE2D7C3)
private val mossOnSurfaceLight = Color(0xFF302C24)
private val mossSurfaceVariantLight = Color(0xFFD8D6C0)
private val mossOnSurfaceVariantLight = Color(0xFF5C5748)
private val mossOutlineLight = Color(0xFF9C9481)
private val mossOutlineVariantLight = Color(0xFFC9C0AC)
private val mossScrimLight = Color(0xFF000000)
private val mossInverseSurfaceLight = Color(0xFF3B3429)
private val mossInverseOnSurfaceLight = Color(0xFFF0E8D8)
private val mossInversePrimaryLight = Color(0xFFD49A54)
private val mossSurfaceDimLight = Color(0xFFDDD4C2)
private val mossSurfaceBrightLight = Color(0xFFF7F1E4)
private val mossSurfaceContainerLowestLight = Color(0xFFFBF6EC)
private val mossSurfaceContainerLowLight = Color(0xFFF5EEE0)
private val mossSurfaceContainerLight = Color(0xFFE2D7C3)
private val mossSurfaceContainerHighLight = Color(0xFFE9DDC7)
private val mossSurfaceContainerHighestLight = Color(0xFFEFE6D3)

// ── 夜间 Warm Lamp · Night ────────────────────────────────────────
// 暖炭灰 / 灰褐主调；苔绿只做少量强调，暖灯金负责点缀。
//
// 规格色 → token 映射（与项目实际读色点一致）：
//   页面背景   #242522 → background
//   普通表面   #302F2A → surface / surfaceContainer（输入框读 surfaceContainer）
//   助手气泡   #393831 → surfaceContainerHigh
//   用户气泡   #454638 → secondaryContainer
//   思维链表面 #34342F → tertiaryContainer（本主题已加入 THEME_THINKING_CONTAINER_THEMES）
//   主色       #A4A181 → primary
//   暖灯金     #D5B477 → tertiary
//   正文       #EAE5D9 → onSurface / onBackground
//   次级文字   #B8B2A2 → onSurfaceVariant
private val mossPrimaryDark = Color(0xFFA4A181)
private val mossOnPrimaryDark = Color(0xFF242522)
private val mossPrimaryContainerDark = Color(0xFF393831)
private val mossOnPrimaryContainerDark = Color(0xFFEAE5D9)
private val mossSecondaryDark = Color(0xFFB8B2A2)
private val mossOnSecondaryDark = Color(0xFF242522)
private val mossSecondaryContainerDark = Color(0xFF454638)
private val mossOnSecondaryContainerDark = Color(0xFFEAE5D9)
// 暖灯金：点缀色（选中态、少量图标、装饰）
private val mossTertiaryDark = Color(0xFFD5B477)
private val mossOnTertiaryDark = Color(0xFF242522)
// 思维链表面专用槽位
private val mossTertiaryContainerDark = Color(0xFF34342F)
private val mossOnTertiaryContainerDark = Color(0xFFEAE5D9)
private val mossErrorDark = Color(0xFFE0A392)
private val mossOnErrorDark = Color(0xFF3A1710)
private val mossErrorContainerDark = Color(0xFF5C2418)
private val mossOnErrorContainerDark = Color(0xFFF5DDD6)

private val mossBackgroundDark = Color(0xFF242522)
private val mossOnBackgroundDark = Color(0xFFEAE5D9)
private val mossSurfaceDark = Color(0xFF302F2A)
private val mossOnSurfaceDark = Color(0xFFEAE5D9)
private val mossSurfaceVariantDark = Color(0xFF454638)
private val mossOnSurfaceVariantDark = Color(0xFFB8B2A2)
private val mossOutlineDark = Color(0xFF6E6A5E)
private val mossOutlineVariantDark = Color(0xFF423F37)
private val mossScrimDark = Color(0xFF000000)
private val mossInverseSurfaceDark = Color(0xFFEAE5D9)
private val mossInverseOnSurfaceDark = Color(0xFF242522)
private val mossInversePrimaryDark = Color(0xFFD5B477)
private val mossSurfaceDimDark = Color(0xFF1D1E1B)
private val mossSurfaceBrightDark = Color(0xFF413F37)
private val mossSurfaceContainerLowestDark = Color(0xFF1D1E1B)
private val mossSurfaceContainerLowDark = Color(0xFF282824)
private val mossSurfaceContainerDark = Color(0xFF302F2A)
private val mossSurfaceContainerHighDark = Color(0xFF393831)
private val mossSurfaceContainerHighestDark = Color(0xFF413F37)

private val warmLampLightScheme = lightColorScheme(
    primary = mossPrimaryLight,
    onPrimary = mossOnPrimaryLight,
    primaryContainer = mossPrimaryContainerLight,
    onPrimaryContainer = mossOnPrimaryContainerLight,
    secondary = mossSecondaryLight,
    onSecondary = mossOnSecondaryLight,
    secondaryContainer = mossSecondaryContainerLight,
    onSecondaryContainer = mossOnSecondaryContainerLight,
    tertiary = mossTertiaryLight,
    onTertiary = mossOnTertiaryLight,
    tertiaryContainer = mossTertiaryContainerLight,
    onTertiaryContainer = mossOnTertiaryContainerLight,
    error = mossErrorLight,
    onError = mossOnErrorLight,
    errorContainer = mossErrorContainerLight,
    onErrorContainer = mossOnErrorContainerLight,
    background = mossBackgroundLight,
    onBackground = mossOnBackgroundLight,
    surface = mossSurfaceLight,
    onSurface = mossOnSurfaceLight,
    surfaceVariant = mossSurfaceVariantLight,
    onSurfaceVariant = mossOnSurfaceVariantLight,
    outline = mossOutlineLight,
    outlineVariant = mossOutlineVariantLight,
    scrim = mossScrimLight,
    inverseSurface = mossInverseSurfaceLight,
    inverseOnSurface = mossInverseOnSurfaceLight,
    inversePrimary = mossInversePrimaryLight,
    surfaceDim = mossSurfaceDimLight,
    surfaceBright = mossSurfaceBrightLight,
    surfaceContainerLowest = mossSurfaceContainerLowestLight,
    surfaceContainerLow = mossSurfaceContainerLowLight,
    surfaceContainer = mossSurfaceContainerLight,
    surfaceContainerHigh = mossSurfaceContainerHighLight,
    surfaceContainerHighest = mossSurfaceContainerHighestLight,
)

private val warmLampDarkScheme = darkColorScheme(
    primary = mossPrimaryDark,
    onPrimary = mossOnPrimaryDark,
    primaryContainer = mossPrimaryContainerDark,
    onPrimaryContainer = mossOnPrimaryContainerDark,
    secondary = mossSecondaryDark,
    onSecondary = mossOnSecondaryDark,
    secondaryContainer = mossSecondaryContainerDark,
    onSecondaryContainer = mossOnSecondaryContainerDark,
    tertiary = mossTertiaryDark,
    onTertiary = mossOnTertiaryDark,
    tertiaryContainer = mossTertiaryContainerDark,
    onTertiaryContainer = mossOnTertiaryContainerDark,
    error = mossErrorDark,
    onError = mossOnErrorDark,
    errorContainer = mossErrorContainerDark,
    onErrorContainer = mossOnErrorContainerDark,
    background = mossBackgroundDark,
    onBackground = mossOnBackgroundDark,
    surface = mossSurfaceDark,
    onSurface = mossOnSurfaceDark,
    surfaceVariant = mossSurfaceVariantDark,
    onSurfaceVariant = mossOnSurfaceVariantDark,
    outline = mossOutlineDark,
    outlineVariant = mossOutlineVariantDark,
    scrim = mossScrimDark,
    inverseSurface = mossInverseSurfaceDark,
    inverseOnSurface = mossInverseOnSurfaceDark,
    inversePrimary = mossInversePrimaryDark,
    surfaceDim = mossSurfaceDimDark,
    surfaceBright = mossSurfaceBrightDark,
    surfaceContainerLowest = mossSurfaceContainerLowestDark,
    surfaceContainerLow = mossSurfaceContainerLowDark,
    surfaceContainer = mossSurfaceContainerDark,
    surfaceContainerHigh = mossSurfaceContainerHighDark,
    surfaceContainerHighest = mossSurfaceContainerHighestDark,
)
