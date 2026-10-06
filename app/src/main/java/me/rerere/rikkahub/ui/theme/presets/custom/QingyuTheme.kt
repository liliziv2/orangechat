package me.rerere.rikkahub.ui.theme.presets.custom

import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.theme.PresetTheme

/*
 * 青屿 Qingyu —— 冷青绿基底 + 珊瑚补色 + 月黄 + 靛蓝的冷暖对比。
 *
 * 与海港（Harbor）同族：同一种「底图玻璃」主题 —— 根背景由 RouteActivity 画底图，
 * 界面允许底色透出。结构照 HarborTheme 走，只换色板。
 *
 * 日间色卡：
 *   Background #F1F7F6 · Background 2 #E5F0EF · Surface #FFFFFF · Surface Soft #F6FAF9
 *   Text #17363A · Secondary #60787A · Muted #8EA3A4 · Divider #D4E2E1
 *   Teal #328C88 · Teal Light #75C5BB · Coral #D97872 · Coral Light #F0BCB5
 *   Indigo #7188D3 · Moon #DCC86E
 *   User #D7ECE9 · AI #FFFFFF · Input #F8FBFA
 *   Success #63B69A · Warning #D8B85F · Error #D97872
 *
 * 夜间色卡：
 *   Background #0B2428 · Background 2 #103238 · Surface #153B3E · Surface Soft #1A4446
 *   Text #E9F3F1 · Secondary #A8C2C0 · Muted #718D8C · Divider #29484A
 *   Teal #63BDAE · Teal Light #91D5C8 · Coral #E58A7B · Coral Light #F1B3A8
 *   Indigo #879BE0 · Moon #E2D07A
 *   User #254B4D · AI #16363A · Input #14373B
 *   Success #78C9A8 · Warning #E4C56F · Error #E57E78
 *
 * 夜间**不是**日间降亮度，而是一套独立的冷暖对比体系：深青绿底（Background / Teal）
 * 托住暖侧的珊瑚红（Coral）与月黄（Moon），靛蓝（Indigo）站冷侧作对比。
 *
 * 锚点落到 M3 槽位，按聊天界面「实际读哪个槽位」对齐，不是按槽位名字：
 *   Background / Background 2 -> background / surface / surfaceVariant / surfaceDim
 *   Surface                   -> surfaceBright / surfaceContainerLowest
 *   Surface Soft              -> surfaceContainer / primaryContainer
 *   Input                     -> surfaceContainerLow
 *   AI Bubble                 -> surfaceContainerHigh
 *   Text                      -> onBackground / onSurface
 *   Secondary                 -> onSurfaceVariant
 *   Muted                     -> outline
 *   Divider                   -> outlineVariant
 *   Teal                      -> primary
 *   Teal Light                -> inversePrimary
 *   Indigo                    -> secondary
 *   Coral                     -> tertiary
 *   Coral Light               -> tertiaryContainer / errorContainer
 *   User Bubble               -> secondaryContainer
 *   Error                     -> error
 *
 * ⚠️ Moon / Success / Warning 三个色**没有落槽**，这不是漏了：
 * `lightColorScheme()` 一共 48 个参数（本仓 M3 1.5.0-alpha19 实读），强调色家族只有
 * primary / secondary / tertiary 三支，已被 Teal / Indigo / Coral 占满；容器家族在界面上
 * 各有各的语义（用户气泡 / AI 气泡 / 输入框），塞进去会改掉现有观感。与其硬占一个槽位，
 * 不如留在色卡里 —— 将来真有组件需要「月黄 / 成功 / 警告」，再从这三个值派生。
 *
 * 两点与 Harbor 一致的取舍：
 *   1) outlineVariant 取「分隔线叠在底色上」的合成值，读作一道极弱分隔，不是描边。
 *   2) error 家族与补色（Coral）同值 —— 色卡里 Error 与 Coral 本就是同一个色，
 *      青屿的「错误」就是珊瑚红，而不是 M3 的语义红。
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
    // Teal #328C88
    primary = Color(0xFF328C88),
    onPrimary = Color(0xFFFFFFFF),
    // Surface Soft #F6FAF9
    primaryContainer = Color(0xFFF6FAF9),
    // Text #17363A
    onPrimaryContainer = Color(0xFF17363A),
    // Teal Light #75C5BB
    inversePrimary = Color(0xFF75C5BB),
    // Indigo #7188D3
    secondary = Color(0xFF7188D3),
    onSecondary = Color(0xFFFFFFFF),
    // User #D7ECE9
    secondaryContainer = Color(0xFFD7ECE9),
    onSecondaryContainer = Color(0xFF17363A),
    // Coral #D97872
    tertiary = Color(0xFFD97872),
    onTertiary = Color(0xFFFFFFFF),
    // Coral Light #F0BCB5
    tertiaryContainer = Color(0xFFF0BCB5),
    onTertiaryContainer = Color(0xFF17363A),
    // Error #D97872（色卡里与补色同值）
    error = Color(0xFFD97872),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF0BCB5),
    onErrorContainer = Color(0xFF17363A),
    // Background #F1F7F6
    background = Color(0xFFF1F7F6),
    onBackground = Color(0xFF17363A),
    surface = Color(0xFFF1F7F6),
    onSurface = Color(0xFF17363A),
    // Background 2 #E5F0EF
    surfaceVariant = Color(0xFFE5F0EF),
    // Secondary #60787A
    onSurfaceVariant = Color(0xFF60787A),
    // Muted #8EA3A4
    outline = Color(0xFF8EA3A4),
    // Divider #D4E2E1
    outlineVariant = Color(0xFFD4E2E1),
    scrim = Color(0xFF000000),
    // 反色取夜间 Surface / 夜间 Text
    inverseSurface = Color(0xFF153B3E),
    inverseOnSurface = Color(0xFFE9F3F1),
    // Background 2 #E5F0EF
    surfaceDim = Color(0xFFE5F0EF),
    // Surface #FFFFFF
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    // Input #F8FBFA
    surfaceContainerLow = Color(0xFFF8FBFA),
    // Surface Soft #F6FAF9
    surfaceContainer = Color(0xFFF6FAF9),
    // AI #FFFFFF
    surfaceContainerHigh = Color(0xFFFFFFFF),
    // Background 2 #E5F0EF
    surfaceContainerHighest = Color(0xFFE5F0EF),
)

private val darkScheme = darkColorScheme(
    // Teal #63BDAE
    primary = Color(0xFF63BDAE),
    // Background #0B2428
    onPrimary = Color(0xFF0B2428),
    // Surface Soft #1A4446
    primaryContainer = Color(0xFF1A4446),
    // Text #E9F3F1
    onPrimaryContainer = Color(0xFFE9F3F1),
    // Teal Light #91D5C8
    inversePrimary = Color(0xFF91D5C8),
    // Indigo #879BE0
    secondary = Color(0xFF879BE0),
    onSecondary = Color(0xFF0B2428),
    // User #254B4D
    secondaryContainer = Color(0xFF254B4D),
    onSecondaryContainer = Color(0xFFE9F3F1),
    // Coral #E58A7B
    tertiary = Color(0xFFE58A7B),
    onTertiary = Color(0xFF0B2428),
    // 色卡未给夜间深色珊瑚容器 ⇒ 取 Surface Soft（与 primaryContainer 一致）
    tertiaryContainer = Color(0xFF1A4446),
    onTertiaryContainer = Color(0xFFE9F3F1),
    // Error #E57E78
    error = Color(0xFFE57E78),
    onError = Color(0xFF0B2428),
    // Coral Light #F1B3A8
    errorContainer = Color(0xFFF1B3A8),
    onErrorContainer = Color(0xFF0B2428),
    // Background #0B2428
    background = Color(0xFF0B2428),
    onBackground = Color(0xFFE9F3F1),
    surface = Color(0xFF0B2428),
    onSurface = Color(0xFFE9F3F1),
    // Background 2 #103238
    surfaceVariant = Color(0xFF103238),
    // Secondary #A8C2C0
    onSurfaceVariant = Color(0xFFA8C2C0),
    // Muted #718D8C
    outline = Color(0xFF718D8C),
    // Divider #29484A
    outlineVariant = Color(0xFF29484A),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE9F3F1),
    // Surface #153B3E
    inverseOnSurface = Color(0xFF153B3E),
    surfaceDim = Color(0xFF0B2428),
    // Surface Soft #1A4446
    surfaceBright = Color(0xFF1A4446),
    // 比底再深一档
    surfaceContainerLowest = Color(0xFF081A1D),
    // Input #14373B
    surfaceContainerLow = Color(0xFF14373B),
    // Surface #153B3E
    surfaceContainer = Color(0xFF153B3E),
    // AI #16363A
    surfaceContainerHigh = Color(0xFF16363A),
    // Surface Soft #1A4446
    surfaceContainerHighest = Color(0xFF1A4446),
)
