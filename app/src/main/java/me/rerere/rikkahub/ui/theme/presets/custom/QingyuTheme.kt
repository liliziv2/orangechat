package me.rerere.rikkahub.ui.theme.presets.custom

import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.theme.PresetTheme

/*
 * 青屿 Qingyu —— 冷青调、低饱和、一点月光。
 *
 * 与海港（Harbor）同族：同一种「底图玻璃」主题 —— 根背景由 RouteActivity 画底图，
 * 界面允许底色透出。结构照 HarborTheme 走，只换色板。
 *
 * 日间锚点：
 *   底 #EEF6F7 · 表面 #F9FCFC · 表面柔 #E4F0F1
 *   主文字 #19343B · 次级文字 #60777D · 最淡 #91A5AA · 分隔 #D2E1E3
 *   主色 #347F89 · 主色浅 #70B8BE · 青绿 #4FA89B · 绿 #79C3AE · 月 #D8C978
 *   用户气泡 #D4E9EA · AI 气泡 #F7FAFA · 输入区 #F4F9F9
 *
 * 夜间锚点：
 *   底 #0B2027 · 表面 #12303A · 表面柔 #173B44
 *   主文字 #E6F3F4 · 次级文字 #A3BEC2 · 最淡 #6F8D93 · 分隔 #294850
 *   主色 #71C6CD · 主色浅 #A0DEE0 · 青绿 #59B7A8 · 绿 #78C7AC · 月 #E1D17D
 *   用户气泡 #214B52 · AI 气泡 #16343D · 输入区 #13303A
 *
 * 锚点落到 M3 槽位，按聊天界面「实际读哪个槽位」对齐，不是按槽位名字：
 *   Background      -> background / surface
 *   Surface         -> surfaceBright / surfaceContainerLowest
 *   Surface Soft    -> surfaceVariant / surfaceContainerHighest
 *   Main Text       -> onSurface / onBackground
 *   Soft Text       -> onSurfaceVariant
 *   Faint Text      -> outline
 *   Divider         -> outlineVariant
 *   Primary         -> primary
 *   Primary Light   -> inversePrimary
 *   Teal            -> tertiary
 *   Green           -> tertiaryContainer（浅化后作容器底）
 *   AI Bubble       -> surfaceContainerHigh
 *   User Bubble     -> secondaryContainer
 *   Input           -> surfaceContainerLow
 *
 * 两点与 Harbor 一致的取舍：
 *   1) outlineVariant 取「分隔线叠在底色上」的合成值，读作一道极弱分隔，不是描边。
 *   2) error 保持 M3 的语义红，不跟着青绿走 —— 它是错误语义，不是装饰色。
 *
 * ⚠️ 「月 #D8C978 / #E1D17D」是这套色板里唯一的暖色，M3 的基础色方案里没有对应
 * 的语义槽（primary / secondary / tertiary 都被青绿系占着）。它只作为锚点记录在
 * 此，不落槽 —— 不为了「用上」而硬塞进某个没有载体的容器色。
 */

val QingyuThemePreset by lazy {
    PresetTheme(
        id = "qingyu",
        name = {
            Text(stringResource(id = R.string.theme_name_qingyu))
        },
        standardLight = lightScheme,
        standardDark = darkScheme,
    )
}

private val lightScheme = lightColorScheme(
    // Primary #347F89
    primary = Color(0xFF347F89),
    onPrimary = Color(0xFFFFFFFF),
    // Surface Soft #E4F0F1
    primaryContainer = Color(0xFFE4F0F1),
    // Main Text #19343B
    onPrimaryContainer = Color(0xFF19343B),
    // Soft Text #60777D
    secondary = Color(0xFF60777D),
    onSecondary = Color(0xFFFFFFFF),
    // User Bubble #D4E9EA
    secondaryContainer = Color(0xFFD4E9EA),
    onSecondaryContainer = Color(0xFF19343B),
    // Teal #4FA89B
    tertiary = Color(0xFF4FA89B),
    onTertiary = Color(0xFFFFFFFF),
    // Green #79C3AE 浅化后作容器底
    tertiaryContainer = Color(0xFFDDEFE9),
    onTertiaryContainer = Color(0xFF19343B),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    // Background #EEF6F7
    background = Color(0xFFEEF6F7),
    onBackground = Color(0xFF19343B),
    surface = Color(0xFFEEF6F7),
    onSurface = Color(0xFF19343B),
    // Surface Soft #E4F0F1
    surfaceVariant = Color(0xFFE4F0F1),
    onSurfaceVariant = Color(0xFF60777D),
    // Faint #91A5AA
    outline = Color(0xFF91A5AA),
    // Divider #D2E1E3
    outlineVariant = Color(0xFFD2E1E3),
    scrim = Color(0xFF000000),
    // 反色取夜间表面柔 / 夜间主文字
    inverseSurface = Color(0xFF173B44),
    inverseOnSurface = Color(0xFFE6F3F4),
    // Primary Light #70B8BE
    inversePrimary = Color(0xFF70B8BE),
    surfaceDim = Color(0xFFE2EEF0),
    // Surface #F9FCFC
    surfaceBright = Color(0xFFF9FCFC),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    // Input #F4F9F9
    surfaceContainerLow = Color(0xFFF4F9F9),
    surfaceContainer = Color(0xFFEEF5F6),
    // AI Bubble #F7FAFA
    surfaceContainerHigh = Color(0xFFF7FAFA),
    surfaceContainerHighest = Color(0xFFE4F0F1),
)

private val darkScheme = darkColorScheme(
    // Primary #71C6CD
    primary = Color(0xFF71C6CD),
    // Background #0B2027
    onPrimary = Color(0xFF0B2027),
    // Surface Soft #173B44
    primaryContainer = Color(0xFF173B44),
    // Main Text #E6F3F4
    onPrimaryContainer = Color(0xFFE6F3F4),
    // Soft Text #A3BEC2
    secondary = Color(0xFFA3BEC2),
    onSecondary = Color(0xFF0B2027),
    // User Bubble #214B52
    secondaryContainer = Color(0xFF214B52),
    onSecondaryContainer = Color(0xFFE6F3F4),
    // Teal #59B7A8
    tertiary = Color(0xFF59B7A8),
    onTertiary = Color(0xFF0B2027),
    // Surface Soft #173B44
    tertiaryContainer = Color(0xFF173B44),
    onTertiaryContainer = Color(0xFFE6F3F4),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    // Background #0B2027
    background = Color(0xFF0B2027),
    onBackground = Color(0xFFE6F3F4),
    surface = Color(0xFF0B2027),
    onSurface = Color(0xFFE6F3F4),
    // Surface Soft #173B44
    surfaceVariant = Color(0xFF173B44),
    onSurfaceVariant = Color(0xFFA3BEC2),
    // Faint #6F8D93
    outline = Color(0xFF6F8D93),
    // Divider #294850
    outlineVariant = Color(0xFF294850),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE6F3F4),
    // Surface #12303A
    inverseOnSurface = Color(0xFF12303A),
    // 日间主色，作反色
    inversePrimary = Color(0xFF347F89),
    surfaceDim = Color(0xFF0B2027),
    // Surface Soft #173B44
    surfaceBright = Color(0xFF173B44),
    // 比底再深一档
    surfaceContainerLowest = Color(0xFF081920),
    // Input #13303A
    surfaceContainerLow = Color(0xFF13303A),
    // Surface #12303A
    surfaceContainer = Color(0xFF12303A),
    // AI Bubble #16343D
    surfaceContainerHigh = Color(0xFF16343D),
    // Surface Soft #173B44
    surfaceContainerHighest = Color(0xFF173B44),
)
