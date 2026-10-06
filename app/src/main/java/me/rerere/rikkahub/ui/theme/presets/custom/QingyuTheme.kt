package me.rerere.rikkahub.ui.theme.presets.custom

import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.theme.PresetTheme

/*
 * 青屿 Qingyu —— 底图玻璃主题（与海港 Harbor 同族）。结构照 HarborTheme 走，只换色板。
 *
 * 用户 2026-10-06（批 78）给了新的夜间 20 色 + 6 个带意象名的日间色，并拍板
 * 「整换：日间用夜游那套」。于是本主题两态各来自一处：
 *
 *   日间 = 已删除的「夜游」（NightSkyTheme，批 74 整删）那套浅色方案，逐槽照搬；
 *   夜间 = 用户本轮新给的 20 色（深青绿基底 + 珊瑚补色 + 月黄 + 靛蓝的冷暖对比）。
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 日间：夜游那套（6 个锚点）
 * ─────────────────────────────────────────────────────────────────────────
 *   灯带黄 #E6D67A · 玻璃绿 #1E4F49 · 街角黑 #071516
 *   纸白   #F1E7B0 · 砖墙红 #8D3F25 · 蓝黑影 #0B2C32
 *
 *   锚点 -> 槽位（与 NightSkyTheme.kt 的 lightScheme 逐槽一致）：
 *     玻璃绿 -> primary
 *     纸白   -> onPrimary / onSecondary / onTertiary / inverseOnSurface
 *     街角黑 -> onBackground / onSurface / onPrimaryContainer / onTertiaryContainer / inverseSurface
 *     砖墙红 -> secondary
 *     灯带黄 -> secondaryContainer（用户气泡）
 *     蓝黑影 -> onSecondaryContainer / onSurfaceVariant
 *     tertiary = #79610D（灯带黄的压暗版 —— 暖黄 L* 约 85，当不了小图标 / 徽章的前景）
 *     页面   = #E8E6DC（纸白软化：保 L* 与色相，只把 C* 压到日 5.0）
 *
 *   页面底由 RouteActivity 的底图 + scrim 决定（本主题在 GLASS_BACKGROUND_THEMES 里）。
 *   日间 scrim 同步换成夜游那套：ScrimColors 三个色号统一 #E8E6DC，实测合成
 *   L* 93.2 / C* 2.9（对照 奶油玫瑰 日 L* 93.6 / C* 2.8）。
 *
 *   ⚠️ 与批 77 的两处差异，是「整换」的直接结果，不是漏改：
 *     · error 由「珊瑚补色 #D97872」改回 M3 语义红 #BA1A1A（夜游浅色当时就是这个取舍）；
 *     · 日间不再出现珊瑚 / 靛蓝 —— 夜游那套是「纸感暖白 + 深绿 + 砖红 + 灯带黄」。
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 夜间：本轮新色卡 20 色
 * ─────────────────────────────────────────────────────────────────────────
 *   Background #0E2025 · Background 2 #132A30 · Surface #17353A · Surface Soft #1C3E43
 *   Text Primary #E8F1F0 · Text Secondary #A8BCBB · Text Muted #718989 · Divider #29464A
 *   Primary #69B7AE · Primary Light #91D1C9
 *   Contrast Rose #B96F7D · Contrast Rose Light #D79AA5
 *   Cool Blue #6F93C7 · Moon Gold #D8C77A
 *   User Bubble #29474A · AI Bubble #18353A · Input #163237
 *   Success #77C3A5 · Warning #D8BF70 · Error #C97982
 *
 *   夜间**不是**日间降亮度，而是一套独立的冷暖对比体系：深青绿底（Background / Primary）
 *   托住暖侧的珊瑚（Contrast Rose）与月黄（Moon Gold），靛蓝（Cool Blue）站冷侧作对比。
 *
 *   锚点 -> 槽位（按聊天界面「实际读哪个槽位」对齐，不是按槽位名字）：
 *     Background / Background 2 -> background / surface / surfaceVariant / surfaceDim
 *     Surface                   -> surfaceContainer / inverseOnSurface
 *     Surface Soft              -> surfaceBright / surfaceContainerHighest / primaryContainer
 *                                  / tertiaryContainer
 *     Input                     -> surfaceContainerLow
 *     AI Bubble                 -> surfaceContainerHigh
 *     User Bubble               -> secondaryContainer
 *     Text Primary              -> onBackground / onSurface / onPrimaryContainer
 *                                  / onSecondaryContainer / onTertiaryContainer / inverseSurface
 *     Text Secondary            -> onSurfaceVariant
 *     Text Muted                -> outline
 *     Divider                   -> outlineVariant
 *     Primary                   -> primary
 *     Primary Light             -> inversePrimary
 *     Cool Blue                 -> secondary
 *     Contrast Rose             -> tertiary
 *     Contrast Rose Light       -> errorContainer
 *     Error                     -> error
 *
 * ⚠️ Moon Gold / Success / Warning 三个色**没有落槽**，与批 77 同一条理由：
 * `lightColorScheme()` 一共 48 个参数（本仓 M3 1.5.0-alpha19 实读），强调色家族只有
 * primary / secondary / tertiary 三支，已被 Primary / Cool Blue / Contrast Rose 占满；
 * 容器家族在界面上各有各的语义（用户气泡 / AI 气泡 / 输入框），塞进去会改掉现有观感。
 * 与其硬占一个槽位，不如留在色卡里 —— 将来真有组件需要「月黄 / 成功 / 警告」，再从这三个值派生。
 *
 * 两点与 Harbor 一致的取舍：
 *   1) outlineVariant 取「分隔线叠在底色上」的合成值，读作一道极弱分隔，不是描边。
 *   2) 夜间的 error 家族与补色（Contrast Rose）同色系 —— 青屿的「错误」就是那支玫瑰红，
 *      而不是 M3 的语义红（日间仍跟夜游走 M3 红）。
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

/*
 * 日间 = 夜游（NightSkyTheme）那套浅色方案，逐槽照搬（用户 2026-10-06 拍板「整换」）。
 * 该主题已在批 74 整删，此处是它在青屿里的唯一存续；改动请对照 st/apply_b74.py 的
 * 删除记录与 audit/base/.../custom/NightSkyTheme.kt 的副本。
 */
private val lightScheme = lightColorScheme(
    // 玻璃绿 #1E4F49
    primary = Color(0xFF1E4F49),
    // 纸白 #F1E7B0
    onPrimary = Color(0xFFF1E7B0),
    primaryContainer = Color(0xFFD0D4D3),
    // 街角黑 #071516
    onPrimaryContainer = Color(0xFF071516),
    // 砖墙红 #8D3F25
    secondary = Color(0xFF8D3F25),
    // 纸白
    onSecondary = Color(0xFFF1E7B0),
    // 灯带黄 #E6D67A（用户气泡）
    secondaryContainer = Color(0xFFE6D67A),
    // 蓝黑影 #0B2C32
    onSecondaryContainer = Color(0xFF0B2C32),
    // 灯带黄压暗版（暖黄 L* 约 85，当不了小图标前景）
    tertiary = Color(0xFF79610D),
    // 纸白
    onTertiary = Color(0xFFF1E7B0),
    tertiaryContainer = Color(0xFFE4E2D7),
    // 街角黑
    onTertiaryContainer = Color(0xFF071516),
    // M3 语义红（夜游浅色当时的取舍）
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    // 纸白软化 #E8E6DC
    background = Color(0xFFE8E6DC),
    // 街角黑
    onBackground = Color(0xFF071516),
    // 纸白软化
    surface = Color(0xFFE8E6DC),
    // 街角黑
    onSurface = Color(0xFF071516),
    surfaceVariant = Color(0xFFD9D6CA),
    // 蓝黑影
    onSurfaceVariant = Color(0xFF0B2C32),
    outline = Color(0xFF676660),
    outlineVariant = Color(0xFFCFCCC3),
    // M3 标准
    scrim = Color(0xFF000000),
    // 街角黑
    inverseSurface = Color(0xFF071516),
    // 纸白
    inverseOnSurface = Color(0xFFF1E7B0),
    inversePrimary = Color(0xFFA8C9B0),
    surfaceDim = Color(0xFFDCD9CE),
    surfaceBright = Color(0xFFF7F6F1),
    surfaceContainerLowest = Color(0xFFF8F7F2),
    surfaceContainerLow = Color(0xFFF0EEE6),
    surfaceContainer = Color(0xFFF2F0E9),
    surfaceContainerHigh = Color(0xFFEEECE3),
    surfaceContainerHighest = Color(0xFFE4E2D7),
)

/*
 * 夜间 = 用户 2026-10-06 给的 20 色（深青绿基底 + 珊瑚补色 + 月黄 + 靛蓝）。
 * 与批 77 的结构一一对应，只换色值：强调色仍是 Primary / Cool Blue / Contrast Rose 三支。
 * ⚠️ User Bubble #29474A 与 Divider #29464A 只差一个字节，别抄串。
 */
private val darkScheme = darkColorScheme(
    // Primary #69B7AE
    primary = Color(0xFF69B7AE),
    // Background #0E2025
    onPrimary = Color(0xFF0E2025),
    // Surface Soft #1C3E43
    primaryContainer = Color(0xFF1C3E43),
    // Text Primary #E8F1F0
    onPrimaryContainer = Color(0xFFE8F1F0),
    // Primary Light #91D1C9
    inversePrimary = Color(0xFF91D1C9),
    // Cool Blue #6F93C7
    secondary = Color(0xFF6F93C7),
    // Background
    onSecondary = Color(0xFF0E2025),
    // User Bubble #29474A
    secondaryContainer = Color(0xFF29474A),
    // Text Primary
    onSecondaryContainer = Color(0xFFE8F1F0),
    // Contrast Rose #B96F7D
    tertiary = Color(0xFFB96F7D),
    // Background
    onTertiary = Color(0xFF0E2025),
    // 色卡未给夜间深色玫瑰容器 ⇒ 取 Surface Soft（与 primaryContainer 一致）
    tertiaryContainer = Color(0xFF1C3E43),
    // Text Primary
    onTertiaryContainer = Color(0xFFE8F1F0),
    // Error #C97982
    error = Color(0xFFC97982),
    // Background
    onError = Color(0xFF0E2025),
    // Contrast Rose Light #D79AA5
    errorContainer = Color(0xFFD79AA5),
    // Background
    onErrorContainer = Color(0xFF0E2025),
    // Background #0E2025
    background = Color(0xFF0E2025),
    // Text Primary
    onBackground = Color(0xFFE8F1F0),
    // Background
    surface = Color(0xFF0E2025),
    // Text Primary
    onSurface = Color(0xFFE8F1F0),
    // Background 2 #132A30
    surfaceVariant = Color(0xFF132A30),
    // Text Secondary #A8BCBB
    onSurfaceVariant = Color(0xFFA8BCBB),
    // Text Muted #718989
    outline = Color(0xFF718989),
    // Divider #29464A
    outlineVariant = Color(0xFF29464A),
    // M3 标准
    scrim = Color(0xFF000000),
    // Text Primary
    inverseSurface = Color(0xFFE8F1F0),
    // Surface #17353A
    inverseOnSurface = Color(0xFF17353A),
    // Background
    surfaceDim = Color(0xFF0E2025),
    // Surface Soft #1C3E43
    surfaceBright = Color(0xFF1C3E43),
    // 比 Background 再深一档
    surfaceContainerLowest = Color(0xFF0A191D),
    // Input #163237
    surfaceContainerLow = Color(0xFF163237),
    // Surface #17353A
    surfaceContainer = Color(0xFF17353A),
    // AI Bubble #18353A
    surfaceContainerHigh = Color(0xFF18353A),
    // Surface Soft #1C3E43
    surfaceContainerHighest = Color(0xFF1C3E43),
)
