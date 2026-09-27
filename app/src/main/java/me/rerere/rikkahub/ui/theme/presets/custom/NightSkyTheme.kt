package me.rerere.rikkahub.ui.theme.presets.custom

import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.theme.PresetTheme

/*
 * 星夜 Night Sky —— 冷蓝夜空，低饱和暖金点缀。
 * 一个主题两态，跟随橘瓣的深浅切换（ColorMode.SYSTEM/LIGHT/DARK）。
 *
 * 八个锚点色（用户给定），Day / Night 各自独立给出 —— 夜间不是由日间降亮度推出来的：
 *   Day   Background #F4F6F7 · Main Text #243346 · Global Text #243346
 *         User Bubble #D6E8F2 · AI Bubble #E7EDF1 · Thinking Bubble #F1E6C9
 *         Accent #6FAED4 · Input #DFE8EE
 *   Night Background #090D14 · Main Text #E8EDF2 · Global Text #E8EDF2
 *         User Bubble #20394D · AI Bubble #192535 · Thinking Bubble #3A3325
 *         Accent #82C4E8 · Input #1B293A
 *
 * 锚点落到 M3 槽位，按聊天界面「实际读哪个槽位」对齐，不是按槽位名字：
 *   Background      -> background / surface
 *   Main Text       -> onSurface / onBackground
 *   Accent          -> primary
 *   User Bubble     -> secondaryContainer
 *   AI Bubble       -> surfaceContainerHigh
 *   Thinking Bubble -> tertiaryContainer（本主题的思考卡片改读这一槽位）
 *   Input           -> surfaceContainerLow
 *
 * 色表没给、但槽位必须填的几档，按「同色相族内插值、只做明度阶差、不加彩度」补齐：
 *   onSurfaceVariant  次级文字（时间戳 / 说明）。正文色向背景混 30%：
 *                     Day #243346 -> #626E7B（与 Harbor 的 Soft Text #5E6B78 几乎同档）。
 *   surfaceVariant    行内代码底，同时是 Moodlet 徽章填充的来源。**这一档不能只看
 *                     好看不好看** —— 徽章填充是 surfaceVariant@0.45 叠在助手气泡
 *                     （surfaceContainerHigh）上，同色叠加会让填充 ΔL* 归零（批 12 的坑）。
 *                     所以取「比 AI 气泡深一档、名义 ΔL* ≥ 4.5」：
 *                     Day #D5DFE7（对 #E7EDF1 ΔL* 5.11）、Night #25313E（对 #192535 ΔL* 5.47）。
 *                     与 CreamRose 的 4.7 同量级。
 *   其余 surfaceContainer 系列、surfaceDim、surfaceBright 是同一色相上的明度阶梯，
 *   相邻两档只差 ΔL* 1~3 —— 橘瓣整套主题就活在这个尺度上，不是笔误。
 *
 * 三处刻意的取舍：
 *   1) 本主题**不进** GLASS_BACKGROUND_THEMES（见 Theme.kt）：色表给的是实心聊天背景，
 *      页面不需要「底图 + scrim」合成，根 background 保持不透明。
 *   2) onPrimaryContainer 取正文色，而不是「压在用户气泡上的浅色」：聊天界面里
 *      用户侧语音条的前景（播放键 / 已播波形 / 时长）读它，而它压在浅色的用户气泡上，
 *      必须是深色才看得见。
 *   3) 输入框与气泡靠轻微明度差区分，槽位名字的 Low/High 顺序在这一档上倒置，视觉上读不出：
 *      Day 的 surfaceContainerLow（输入框 #DFE8EE）比 surfaceContainerHigh
 *      （AI 气泡 #E7EDF1）深 ΔL* 1.9；Night 反过来，输入框 #1B293A 比 AI 气泡 #192535
 *      亮 ΔL* 1.8。同 CreamRose 的处理。
 *
 * 描边档 outline / outlineVariant 全是有彩度的冷灰，没有一根黑描边；
 * outlineVariant 取的是「发丝线叠在底色上」的合成值，读作一道极弱分隔而不是一条描边。
 */

val NightSkyThemePreset by lazy {
    PresetTheme(
        id = "nightsky",
        name = {
            Text(stringResource(id = R.string.theme_name_nightsky))
        },
        standardLight = lightScheme,
        standardDark = darkScheme,
    )
}

private val lightScheme = lightColorScheme(
    // Accent #6FAED4
    primary = Color(0xFF6FAED4),
    onPrimary = Color(0xFF0E2A3C),
    primaryContainer = Color(0xFFD8EAF6),
    // 用户侧语音条的前景读它，压在用户气泡上必须是深色
    onPrimaryContainer = Color(0xFF243346),
    secondary = Color(0xFF4E7B99),
    onSecondary = Color(0xFFFFFFFF),
    // User Bubble #D6E8F2
    secondaryContainer = Color(0xFFD6E8F2),
    onSecondaryContainer = Color(0xFF243346),
    tertiary = Color(0xFF8A7A45),
    onTertiary = Color(0xFFFFFFFF),
    // Thinking Bubble #F1E6C9
    tertiaryContainer = Color(0xFFF1E6C9),
    onTertiaryContainer = Color(0xFF43391F),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    // Background #F4F6F7
    background = Color(0xFFF4F6F7),
    // Main Text #243346
    onBackground = Color(0xFF243346),
    surface = Color(0xFFF4F6F7),
    onSurface = Color(0xFF243346),
    // Moodlet 徽章填充读这一槽位，必须比 surfaceContainerHigh 深 ΔL* ≥ 4.5（批 12 的坑）
    surfaceVariant = Color(0xFFD5DFE7),
    // Global Text 的次级档：正文色向背景混 30%
    onSurfaceVariant = Color(0xFF626E7B),
    outline = Color(0xFF9AA7B2),
    outlineVariant = Color(0xFFDCE3E8),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF243346),
    inverseOnSurface = Color(0xFFF4F6F7),
    inversePrimary = Color(0xFF9CC9E5),
    surfaceDim = Color(0xFFDDE4E9),
    surfaceBright = Color(0xFFFBFCFD),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    // Input #DFE8EE
    surfaceContainerLow = Color(0xFFDFE8EE),
    surfaceContainer = Color(0xFFE3EAEF),
    // AI Bubble #E7EDF1；默认（FLAT）模式下输入框也读这一槽位 —— 两者同材质是有意的
    surfaceContainerHigh = Color(0xFFE7EDF1),
    surfaceContainerHighest = Color(0xFFDBE4EA),
)

private val darkScheme = darkColorScheme(
    // Accent #82C4E8
    primary = Color(0xFF82C4E8),
    onPrimary = Color(0xFF0A2030),
    primaryContainer = Color(0xFF1E3D52),
    onPrimaryContainer = Color(0xFFCFE6F6),
    secondary = Color(0xFFA9BECF),
    onSecondary = Color(0xFF12222F),
    // User Bubble #20394D
    secondaryContainer = Color(0xFF20394D),
    onSecondaryContainer = Color(0xFFE8EDF2),
    tertiary = Color(0xFFC4B78C),
    onTertiary = Color(0xFF2A2415),
    // Thinking Bubble #3A3325
    tertiaryContainer = Color(0xFF3A3325),
    onTertiaryContainer = Color(0xFFEDE4CC),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    // Background #090D14
    background = Color(0xFF090D14),
    // Main Text #E8EDF2
    onBackground = Color(0xFFE8EDF2),
    surface = Color(0xFF090D14),
    onSurface = Color(0xFFE8EDF2),
    // 比 AI 气泡亮 ΔL* 5.47：徽章填充要在气泡上看得见（批 12 的坑）
    surfaceVariant = Color(0xFF25313E),
    onSurfaceVariant = Color(0xFFA9B6C4),
    outline = Color(0xFF55636F),
    // 极弱边框：只比底亮几个单位，读作分隔而不是描边
    outlineVariant = Color(0xFF232E3A),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE8EDF2),
    inverseOnSurface = Color(0xFF1B293A),
    inversePrimary = Color(0xFF2F6B92),
    surfaceDim = Color(0xFF070A10),
    surfaceBright = Color(0xFF2A3644),
    surfaceContainerLowest = Color(0xFF05080D),
    // Input #1B293A；GLASS 模式的输入框读这一槽位
    surfaceContainerLow = Color(0xFF1B293A),
    surfaceContainer = Color(0xFF1D2B3B),
    // AI Bubble #192535
    surfaceContainerHigh = Color(0xFF192535),
    surfaceContainerHighest = Color(0xFF2C3948),
)
