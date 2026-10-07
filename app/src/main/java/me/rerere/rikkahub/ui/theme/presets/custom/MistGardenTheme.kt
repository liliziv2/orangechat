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
 * 用户 2026-10-07（批 79）给的 Day / Night 各一套色卡，并要求：
 *   · Day / Night 必须明显区分；
 *   · Night 不是简单把 Day 压黑；
 *   · 低饱和为主，补色只做强调；
 *   · 不新增复杂渐变；
 *   · 不重新做成奶油玫瑰 / 极简白的变体。
 *
 * ─────────────────────────────────────────────────────────────────────────
 * Day：冷调雾感（7 色）
 * ─────────────────────────────────────────────────────────────────────────
 *   BG #F6F9FE · Surface #FFFFFF · Muted #E2ECF8
 *   Primary #68B8A0 · Blue #A5BED8 · Pink #D0AEC0 · Text #383838
 *
 *   这是一套**冷调近白**的底：底色带极轻的蓝雾（BG #F6F9FE），强调色是
 *   低饱和的雾青绿 / 雾蓝 / 雾紫粉三支，靠「冷 — 更冷 — 微暖」的色相差拉开，
 *   而不是靠明度堆叠（奶油玫瑰是暖白纸感、极简白是无彩 —— 雾庭刻意都不走这两条）。
 *
 *   锚点 -> 槽位：
 *     Primary #68B8A0 -> primary（雾青绿，主强调）
 *     Blue    #A5BED8 -> secondary（雾蓝）
 *     Pink    #D0AEC0 -> tertiary（雾紫粉）
 *     Muted   #E2ECF8 -> secondaryContainer（用户气泡：淡雾蓝）
 *     Surface #FFFFFF -> onPrimary / onSecondary / onTertiary / surface
 *     BG      #F6F9FE -> background
 *     Text    #383838 -> onBackground / onSurface / onSecondaryContainer / onTertiaryContainer
 *
 * ─────────────────────────────────────────────────────────────────────────
 * Night：暖调夜雾（8 色，Gold 不落槽）
 * ─────────────────────────────────────────────────────────────────────────
 *   BG #1A1C20 · Surface #2A2828 · Muted #4A4548
 *   Primary #68AFA8 · Green #8FAE9B · Gold #B99A68 · Pink #B8788E · Text #E0D8D0
 *
 *   夜间**不是**把日间压黑：日间是「冷调（蓝雾/近白/雾紫）」，夜间整体换到
 *   **暖调** —— 底色是带一丝暖灰的炭色（Surface #2A2828 而非中性深灰），
 *   文字是暖白 #E0D8D0，强调色换成青绿 / 苔绿 / 玫瑰三支。
 *   色相家族整体替换，这才是「明显区分」的真正来源。
 *
 *   锚点 -> 槽位（按聊天界面实际读哪个槽位对齐）：
 *     Primary #68AFA8 -> primary（青绿）
 *     Green   #8FAE9B -> secondary（苔绿）
 *     Pink    #B8788E -> tertiary（玫瑰）
 *     Muted   #4A4548 -> secondaryContainer（用户气泡）
 *     BG      #1A1C20 -> background / onPrimary / onSecondary / onTertiary
 *     Surface #2A2828 -> surface / surfaceContainer / onError / onErrorContainer
 *     Text    #E0D8D0 -> onBackground / onSurface / onSecondaryContainer / onTertiaryContainer
 *
 *   ⚠️ Gold #B99A68 **没有落槽**，理由与青屿的 Moon Gold 一致：
 *   `lightColorScheme()` 一共 48 个参数，强调色家族只有 primary / secondary / tertiary
 *   三支，已被 Primary / Green / Pink 占满；容器家族在界面上各有语义（用户气泡 /
 *   AI 气泡 / 输入框），塞进去会改掉现有观感。与其硬占一个槽位，不如留在色卡里 ——
 *   将来真有组件需要「雾庭金」，再从色卡值派生。
 *
 * 本主题在 GLASS_BACKGROUND_THEMES 里（Theme.kt），所以真实观感链路是三段：
 *   底图（harbor_chat_bg.webp，与 harbor / creamrose 同一张）
 *   → 上层 scrim（THEME_BACKGROUND_SCRIM["mistgarden"]，日间冷调近白 / 夜间炭色两点渐变）
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
 * 日间：冷调雾感。
 * 白 surface + 极轻蓝雾底 + 雾青绿/雾蓝/雾紫粉三支低饱和强调。
 */
private val lightScheme = lightColorScheme(
    // Primary #68B8A0（雾青绿）
    primary = Color(0xFF68B8A0),
    // Surface #FFFFFF
    onPrimary = Color(0xFFFFFFFF),
    // Muted #E2ECF8 的浅化版（主色的容器：比用户气泡再淡一档）
    primaryContainer = Color(0xFFDCEBF4),
    // Text #383838
    onPrimaryContainer = Color(0xFF1F4B41),
    // Blue #A5BED8（雾蓝）
    secondary = Color(0xFFA5BED8),
    // Surface #FFFFFF
    onSecondary = Color(0xFFFFFFFF),
    // Muted #E2ECF8（用户气泡：淡雾蓝）
    secondaryContainer = Color(0xFFE2ECF8),
    // Text #383838
    onSecondaryContainer = Color(0xFF383838),
    // Pink #D0AEC0（雾紫粉）
    tertiary = Color(0xFFD0AEC0),
    // Surface #FFFFFF
    onTertiary = Color(0xFFFFFFFF),
    // Pink 的极浅版（思考/工具卡底色，与用户气泡同族的微粉）
    tertiaryContainer = Color(0xFFF2E4EC),
    // Text #383838
    onTertiaryContainer = Color(0xFF5A3A4A),
    // M3 语义红（与整体低饱和调不冲突，保留可读性）
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    // BG #F6F9FE（冷调近白）
    background = Color(0xFFF6F9FE),
    // Text #383838
    onBackground = Color(0xFF383838),
    // Surface #FFFFFF
    surface = Color(0xFFFFFFFF),
    // Text #383838
    onSurface = Color(0xFF383838),
    // 比用户气泡再淡一档的雾灰（行内代码底等）
    surfaceVariant = Color(0xFFEAF0F7),
    // Text 稍浅一档
    onSurfaceVariant = Color(0xFF5A6168),
    outline = Color(0xFF8A9098),
    outlineVariant = Color(0xFFD6DEE6),
    // M3 标准
    scrim = Color(0xFF000000),
    // Text #383838
    inverseSurface = Color(0xFF383838),
    // Surface #FFFFFF
    inverseOnSurface = Color(0xFFFFFFFF),
    // 浅色的主色版本（暗底上的 primary）
    inversePrimary = Color(0xFFA8D8C8),
    surfaceDim = Color(0xFFEDF1F7),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    // 输入框（FLAT）= surfaceContainer；雾庭取极淡雾蓝
    surfaceContainerLow = Color(0xFFF3F7FC),
    surfaceContainer = Color(0xFFF0F5FB),
    // AI 气泡（比 Surface 略沉一档的雾灰白）
    surfaceContainerHigh = Color(0xFFEDF2F9),
    surfaceContainerHighest = Color(0xFFE7EDF5),
)

/*
 * 夜间：暖调夜雾。
 * 炭色底（带一丝暖）+ 暖白文字 + 青绿/苔绿/玫瑰三支强调。
 * ⚠️ 不是日间压黑：色相家族整体从「冷」换到「暖」。
 */
private val darkScheme = darkColorScheme(
    // Primary #68AFA8（青绿）
    primary = Color(0xFF68AFA8),
    // BG #1A1C20
    onPrimary = Color(0xFF1A1C20),
    // 主色的暗容器（比用户气泡再深一档的青绿炭）
    primaryContainer = Color(0xFF23383A),
    // Text #E0D8D0
    onPrimaryContainer = Color(0xFFE0D8D0),
    // 暗底上的浅主色
    inversePrimary = Color(0xFF8FD0C9),
    // Green #8FAE9B（苔绿）
    secondary = Color(0xFF8FAE9B),
    // BG
    onSecondary = Color(0xFF1A1C20),
    // Muted #4A4548（用户气泡：暖灰）
    secondaryContainer = Color(0xFF4A4548),
    // Text #E0D8D0
    onSecondaryContainer = Color(0xFFE0D8D0),
    // Pink #B8788E（玫瑰）
    tertiary = Color(0xFFB8788E),
    // BG
    onTertiary = Color(0xFF1A1C20),
    // 玫瑰的暗容器（比用户气泡再深一档）
    tertiaryContainer = Color(0xFF3C2C33),
    // Text #E0D8D0
    onTertiaryContainer = Color(0xFFE0D8D0),
    // 玫瑰同色系（与补色一族，不是 M3 语义红）
    error = Color(0xFFB8788E),
    // BG
    onError = Color(0xFF1A1C20),
    // 玫瑰的浅版（错误容器）
    errorContainer = Color(0xFF5A3B45),
    // Text #E0D8D0
    onErrorContainer = Color(0xFFE0D8D0),
    // BG #1A1C20
    background = Color(0xFF1A1C20),
    // Text #E0D8D0
    onBackground = Color(0xFFE0D8D0),
    // Surface #2A2828（带一丝暖的炭，不是中性深灰）
    surface = Color(0xFF2A2828),
    // Text #E0D8D0
    onSurface = Color(0xFFE0D8D0),
    // 比 Muted 再深一档（行内代码底）
    surfaceVariant = Color(0xFF3A3639),
    // Text 稍暗一档
    onSurfaceVariant = Color(0xFFB8B0A8),
    outline = Color(0xFF8A8286),
    outlineVariant = Color(0xFF3E3A3D),
    // M3 标准
    scrim = Color(0xFF000000),
    // Text #E0D8D0
    inverseSurface = Color(0xFFE0D8D0),
    // Surface #2A2828
    inverseOnSurface = Color(0xFF2A2828),
    // 比 BG 再深一档
    surfaceDim = Color(0xFF141618),
    // 比 Surface 再亮一档
    surfaceBright = Color(0xFF343232),
    surfaceContainerLowest = Color(0xFF141618),
    // 输入框（GLASS）= surfaceContainerLow
    surfaceContainerLow = Color(0xFF232122),
    // Surface #2A2828
    surfaceContainer = Color(0xFF2A2828),
    // AI 气泡（略深于 Surface 一档）
    surfaceContainerHigh = Color(0xFF312E2F),
    surfaceContainerHighest = Color(0xFF3A3639),
)
