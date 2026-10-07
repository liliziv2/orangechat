package me.rerere.rikkahub.ui.theme.presets.custom

import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.theme.PresetTheme

/*
 * 雾庭 Mist Garden —— 底图玻璃主题（与海港 Harbor 同族，结构照 HarborTheme 走）。
 *
 * 用户 2026-10-07（批 82）重给的 Day / Night 各一套色卡。**核心规则只有一句**：
 *
 *     蓝灰做底，青绿做主色，灰绿做辅助，粉 / 黄只做点缀；
 *     禁止大面积绿色背景、绿色 Surface、绿色气泡。
 *
 * 具体落法：
 *   · 「底」= background / surface / surface* 全家 / 气泡、输入框 —— 一律取 **蓝灰**色卡值，
 *     一个绿色都不放。用户气泡 User #DCE8EC 本身就是蓝灰（不是绿）。
 *   · 「主色」= primary 取 **蓝青**（Day #5F8F9A / Night #79AEB7）—— 是蓝味的青，
 *     不是薄荷绿。发送按钮、选中态、强调文字读它。
 *   · 「辅助」= secondary 取 **灰绿**（Day #92A9A0 / Night #829F94）—— 低饱和、偏灰，
 *     只做次级强调。
 *   · 「点缀」= Accent 落 tertiary、Warm 黄**不落槽**。粉只在 tertiary / error 一处出现。
 *
 * ─────────────────────────────────────────────────────────────────────────
 * Day（11 色）
 * ─────────────────────────────────────────────────────────────────────────
 *   BG #F6F8FA · Surface #FFFFFF · Muted #E7EDF0 · Text #263238
 *   Primary #5F8F9A（蓝青） · Secondary #92A9A0（灰绿） · Accent #D3A0B0（灰粉）
 *   Warm #D8C77B（少量暖黄） · User #DCE8EC · AI #FFFFFF · Input #F8FAFB
 *
 *   底色是**中性偏冷的蓝灰**（BG #F6F8FA 几乎无色相，比批 79 的 #F6F9FE 更中性），
 *   Surface 纯白、AI 气泡纯白 —— 页面与 AI 气泡之间的层次靠 Muted 一档的 surface* 撑开，
 *   不靠给气泡上色。User 气泡 #DCE8EC 是淡蓝灰，与 AI 纯白形成「冷 / 白」两档。
 *
 *   锚点 -> 槽位：
 *     BG      #F6F8FA -> background
 *     Surface #FFFFFF -> surface / surfaceBright / surfaceContainerLowest /
 *                        surfaceContainerHigh（AI 气泡）/ onPrimary / onSecondary / onTertiary
 *     Muted   #E7EDF0 -> surfaceVariant / surfaceContainerHighest
 *     Text    #263238 -> onBackground / onSurface / onSecondaryContainer / 各 on*
 *     Primary #5F8F9A -> primary（蓝青，主强调）
 *     Secondary #92A9A0 -> secondary（灰绿，次强调）
 *     Accent  #D3A0B0 -> tertiary（灰粉，点缀）
 *     User    #DCE8EC -> secondaryContainer（用户气泡：淡蓝灰）
 *     Input   #F8FAFB -> surfaceContainer / surfaceContainerLow / surfaceContainerLowest 一族
 *
 * ─────────────────────────────────────────────────────────────────────────
 * Night（11 色）
 * ─────────────────────────────────────────────────────────────────────────
 *   BG #171C21 · Surface #222A2F · Muted #39444A · Text #E5ECEE
 *   Primary #79AEB7 · Secondary #829F94 · Accent #B97C8C
 *   Warm #D3C078（少量暖黄） · User #2A3B42 · AI #222B30 · Input #20292E
 *
 *   夜间**与日间同族（都是蓝灰），不靠色相翻转区分** —— 区分来自：
 *   ① 明度整体落到 L* 十几的黑蓝；② 底色从「近白」变成「深蓝灰」；
 *   ③ 主色由 #5F8F9A 提亮到 #79AEB7（暗底上要够亮才读得出「青」）。
 *   Surface #222A2F 是**冷蓝灰炭**（不是批 79 的暖炭 #2A2828）——夜间同样守「蓝灰做底」。
 *
 *   锚点 -> 槽位：
 *     BG      #171C21 -> background / onPrimary / onSecondary / onTertiary / onError
 *     Surface #222A2F -> surface / surfaceContainer
 *     AI      #222B30 -> surfaceContainerHigh（AI 气泡）
 *     User    #2A3B42 -> secondaryContainer（用户气泡：深蓝灰，比 AI 略冷）
 *     Input   #20292E -> surfaceContainerLow（GLASS 输入框）
 *     Muted   #39444A -> surfaceVariant / surfaceContainerHighest
 *     Text    #E5ECEE -> onBackground / onSurface / onSecondaryContainer / 各 on*
 *     Primary #79AEB7 -> primary · Secondary #829F94 -> secondary · Accent #B97C8C -> tertiary
 *
 *   ⚠️ Warm（Day #D8C77B / Night #D3C078）**没有落槽**，理由与青屿的 Moon Gold 一致：
 *   `lightColorScheme()` 一共 48 个参数，强调色家族只有 primary / secondary / tertiary
 *   三支，已被 Primary / Secondary / Accent 占满；容器家族在界面上各有语义（用户气泡 /
 *   AI 气泡 / 输入框），塞进去会改掉现有观感。暖黄在色卡里是「少量点缀」，
 *   强行占一个主槽就违背了「只做点缀」。将来真有组件需要「雾庭暖黄」，再从色卡值派生。
 *
 * 本主题在 GLASS_BACKGROUND_THEMES 里（Theme.kt），所以真实观感链路是三段：
 *   底图（harbor_chat_bg.webp，与 harbor / creamrose 同一张）
 *   → 上层 scrim（THEME_BACKGROUND_SCRIM["mistgarden"]，日间冷调近白 / 夜间蓝灰炭两点渐变）
 *   → 组件本身半透明 surface（界面 82% 之类的 interfaceSurfaceOpacity）
 * 页面根 background 的 alpha 被置 0，由 RouteActivity 绘制底图。
 *
 * ⚠️ 本主题**不在** THEME_THINKING_CONTAINER_THEMES 里（用户 2026-10-07 拍板）：
 * 思考卡与助手气泡共用 surfaceContainerHigh，不单独占 tertiaryContainer。
 */

val MistGardenThemePreset by lazy {
    PresetTheme(
        id = "mistgarden",
        name = {
            Text(stringResource(id = R.string.theme_name_mistgarden))
        },
        standardLight = lightScheme,
        standardDark = darkScheme,
    )
}

/*
 * 日间：蓝灰做底，青绿做主色。
 * 中性偏冷近白底 + 纯白 surface / AI 气泡 + 淡蓝灰用户气泡，
 * 强调色是蓝青 #5F8F9A / 灰绿 #92A9A0 / 灰粉 #D3A0B0 三支低饱和。
 */
private val lightScheme = lightColorScheme(
    // Primary #5F8F9A（蓝青 —— 是蓝味的青，不是薄荷绿）
    primary = Color(0xFF5F8F9A),
    // Surface #FFFFFF
    onPrimary = Color(0xFFFFFFFF),
    // 主色的浅容器（比用户气泡再淡一档的青灰）
    primaryContainer = Color(0xFFD6E6EA),
    // Text 同族偏深
    onPrimaryContainer = Color(0xFF20383D),
    // Secondary #92A9A0（灰绿 —— 低饱和偏灰，只做次强调）
    secondary = Color(0xFF92A9A0),
    // Surface #FFFFFF
    onSecondary = Color(0xFFFFFFFF),
    // User #DCE8EC（用户气泡：淡蓝灰）
    secondaryContainer = Color(0xFFDCE8EC),
    // Text #263238
    onSecondaryContainer = Color(0xFF263238),
    // Accent #D3A0B0（灰粉，点缀）
    tertiary = Color(0xFFD3A0B0),
    // Surface #FFFFFF
    onTertiary = Color(0xFFFFFFFF),
    // Accent 的极浅版（思考 / 工具卡底色）
    tertiaryContainer = Color(0xFFF2E3E9),
    // Text 同族偏暖深
    onTertiaryContainer = Color(0xFF4E2F3A),
    // M3 语义红（整屏低饱和，但错误必须可读）
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    // BG #F6F8FA（中性偏冷近白）
    background = Color(0xFFF6F8FA),
    // Text #263238
    onBackground = Color(0xFF263238),
    // Surface #FFFFFF
    surface = Color(0xFFFFFFFF),
    // Text #263238
    onSurface = Color(0xFF263238),
    // Muted #E7EDF0（行内代码底等）
    surfaceVariant = Color(0xFFE7EDF0),
    // Text 稍浅一档
    onSurfaceVariant = Color(0xFF55636A),
    outline = Color(0xFF8A969D),
    outlineVariant = Color(0xFFD3DDE2),
    // M3 标准
    scrim = Color(0xFF000000),
    // Text #263238
    inverseSurface = Color(0xFF263238),
    // Surface #FFFFFF
    inverseOnSurface = Color(0xFFFFFFFF),
    // 暗底上的浅主色
    inversePrimary = Color(0xFFA3CDD6),
    // 比 Surface 略沉一档
    surfaceDim = Color(0xFFEDF1F4),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    // Input #F8FAFB（GLASS 输入框）
    surfaceContainerLow = Color(0xFFF8FAFB),
    // 输入框（FLAT）= surfaceContainer；取 Input 的极小加深版，读到边界但仍是浅底
    surfaceContainer = Color(0xFFF2F5F7),
    // AI 气泡 = AI #FFFFFF（与 surface 同色：AI 气泡不上色，靠页面蓝灰底把它托出来）
    surfaceContainerHigh = Color(0xFFFFFFFF),
    // Muted #E7EDF0
    surfaceContainerHighest = Color(0xFFE7EDF0),
)

/*
 * 夜间：与日间同族（蓝灰），靠明度与底色深浅区分，不做色相翻转。
 * 冷蓝灰炭底 + 亮蓝青主色 + 灰绿/玫瑰两支强调。
 */
private val darkScheme = darkColorScheme(
    // Primary #79AEB7（暗底上提亮的蓝青）
    primary = Color(0xFF79AEB7),
    // BG #171C21
    onPrimary = Color(0xFF171C21),
    // 主色的暗容器
    primaryContainer = Color(0xFF233940),
    // Text #E5ECEE
    onPrimaryContainer = Color(0xFFE5ECEE),
    // 暗底上的浅主色
    inversePrimary = Color(0xFF9FCAD2),
    // Secondary #829F94（灰绿）
    secondary = Color(0xFF829F94),
    // BG
    onSecondary = Color(0xFF171C21),
    // User #2A3B42（用户气泡：深蓝灰，比 AI 略冷）
    secondaryContainer = Color(0xFF2A3B42),
    // Text #E5ECEE
    onSecondaryContainer = Color(0xFFE5ECEE),
    // Accent #B97C8C（玫瑰，点缀）
    tertiary = Color(0xFFB97C8C),
    // BG
    onTertiary = Color(0xFF171C21),
    // 玫瑰的暗容器
    tertiaryContainer = Color(0xFF3A2830),
    // Text #E5ECEE
    onTertiaryContainer = Color(0xFFE5ECEE),
    // 玫瑰同色系（与补色一族，不是 M3 语义红）
    error = Color(0xFFB97C8C),
    // BG
    onError = Color(0xFF171C21),
    // 玫瑰的暗版（错误容器）
    errorContainer = Color(0xFF54333C),
    // Text #E5ECEE
    onErrorContainer = Color(0xFFE5ECEE),
    // BG #171C21
    background = Color(0xFF171C21),
    // Text #E5ECEE
    onBackground = Color(0xFFE5ECEE),
    // Surface #222A2F（冷蓝灰炭，不是暖炭）
    surface = Color(0xFF222A2F),
    // Text #E5ECEE
    onSurface = Color(0xFFE5ECEE),
    // Muted #39444A（行内代码底）
    surfaceVariant = Color(0xFF39444A),
    // Text 稍暗一档
    onSurfaceVariant = Color(0xFFB0BCC2),
    outline = Color(0xFF7E8B92),
    outlineVariant = Color(0xFF39444A),
    // M3 标准
    scrim = Color(0xFF000000),
    // Text #E5ECEE
    inverseSurface = Color(0xFFE5ECEE),
    // Surface #222A2F
    inverseOnSurface = Color(0xFF222A2F),
    // 比 BG 再深一档
    surfaceDim = Color(0xFF12161A),
    // 比 Surface 再亮一档
    surfaceBright = Color(0xFF2C353B),
    surfaceContainerLowest = Color(0xFF12161A),
    // Input #20292E（GLASS 输入框）
    surfaceContainerLow = Color(0xFF20292E),
    // Surface #222A2F
    surfaceContainer = Color(0xFF222A2F),
    // AI 气泡 = AI #222B30（比 Surface 略冷一档）
    surfaceContainerHigh = Color(0xFF222B30),
    // Muted #39444A
    surfaceContainerHighest = Color(0xFF39444A),
)
