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
 * 用户 2026-10-07（批 83）第三次给色卡，这次是**照着一张「酒馆主题美化」参考图取色**。
 * **主题名保留 `雾庭 / mistgarden` 不变**（用户明确：「全改保留主题名字」）。
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 配色骨架（对参考图做全图色彩聚类得到，1080×1440）
 * ─────────────────────────────────────────────────────────────────────────
 *   #F6F2ED  79.2%  奶油白底（大面积）
 *   #631215   6.4%  深红主体（顶栏蕾丝 / 底栏）
 *   #8F2122   3.6%  酒红次色
 *   #9C6260 / #D3AAA6  3.7%  玫瑰粉（点缀）
 *   #A69C9D / #6A595A  3.8%  灰调（阴影 / 描边）
 *   #77879C / #ACB7C6  0.7%  灰蓝（丝带，极少）
 *   #E2CCB9 / #A08578  1.0%  暖棕（装饰）
 *
 * 一句话概括：**奶油白做底，深红做主色，酒红 / 玫瑰只做点缀，灰蓝极少**。
 *
 * ─────────────────────────────────────────────────────────────────────────
 * Day（11 色）
 * ─────────────────────────────────────────────────────────────────────────
 *   BG #F7F3EE（奶油白） · Surface #FFFDF8 · Muted #EDE3DE · Text #2B1A1C
 *   Primary #7A1418（深红） · Secondary #A8323A（酒红） · Accent #C97F8A（玫瑰）
 *   User #F2E4E2（淡玫瑰白） · AI #FFFDF8 · Input #FBF6EE
 *
 *   底色是**微暖的奶油白**（BG #F7F3EE —— 参考图 79% 面积的 #F6F2ED 略提亮，
 *   避免整屏发灰）。Surface 与 AI 气泡同为 #FFFDF8（暖白，不是纯白 —— 纯白在
 *   奶油底上会「跳」）。用户气泡 #F2E4E2 是**淡玫瑰白**，与暖白的 AI 气泡形成
 *   「粉 / 白」两档，符合参考图里「用户侧偏红调」的观感。
 *
 *   锚点 -> 槽位：
 *     BG      #F7F3EE -> background
 *     Surface #FFFDF8 -> surface / surfaceBright / surfaceContainerLowest /
 *                        surfaceContainerHigh（AI 气泡）/ onPrimary / onSecondary / onTertiary
 *     Muted   #EDE3DE -> surfaceVariant / surfaceContainerHighest
 *     Text    #2B1A1C -> onBackground / onSurface / onSecondaryContainer / 各 on*
 *     Primary #7A1418 -> primary（深红，主强调 —— 发送按钮读它）
 *     Secondary #A8323A -> secondary（酒红，次强调）
 *     Accent  #C97F8A -> tertiary（玫瑰，点缀）
 *     User    #F2E4E2 -> secondaryContainer（用户气泡：淡玫瑰白）
 *     Input   #FBF6EE -> surfaceContainerLow（GLASS 输入框）/ surfaceContainer（FLAT）
 *
 * ─────────────────────────────────────────────────────────────────────────
 * Night（11 色）
 * ─────────────────────────────────────────────────────────────────────────
 *   BG #1A1416 · Surface #251C1E · Muted #3D2F31 · Text #F0E6E4
 *   Primary #E88C93 · Secondary #CE7C82 · Accent #D99AA4
 *   User #3A2126 · AI #26191C · Input #221A1C
 *
 *   夜间**不是把日间压黑**：底从奶油白变成**深栗褐**（#1A1416 —— 带红调的近黑，
 *   不是中性灰黑），主色由深红 #7A1418 提到 **亮玫瑰红 #E88C93**（暗底上深红读不出，
 *   必须提亮到 L* 60 以上才保住「红」的识别度）。Surface 仍是暖调（#251C1E）。
 *
 *   锚点 -> 槽位：
 *     BG      #1A1416 -> background / onPrimary / onSecondary / onTertiary / onError
 *     Surface #251C1E -> surface / surfaceContainer
 *     AI      #26191C -> surfaceContainerHigh（AI 气泡）
 *     User    #3A2126 -> secondaryContainer（用户气泡：深玫瑰褐）
 *     Input   #221A1C -> surfaceContainerLow（GLASS 输入框）
 *     Muted   #3D2F31 -> surfaceVariant / surfaceContainerHighest
 *     Text    #F0E6E4 -> onBackground / onSurface / onSecondaryContainer / 各 on*
 *     Primary #E88C93 -> primary · Secondary #CE7C82 -> secondary · Accent #D99AA4 -> tertiary
 *
 *   ⚠️ 暖棕 / 灰蓝（参考图里的 #E2CCB9 / #77879C）**没有落槽**，理由与前一版的
 *   Warm 黄一致：`lightColorScheme()` 一共 48 个参数，强调色家族只有 primary /
 *   secondary / tertiary 三支，已被 Primary / Secondary / Accent 占满；容器家族在界面上
 *   各有语义（用户气泡 / AI 气泡 / 输入框），塞进去会改掉现有观感。它们在图里本来就是
 *   「极少量装饰」（合计 1.7%），强行占一个主槽就违背了「只做点缀」。
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 对比度（WCAG，已逐条核算通过）
 * ─────────────────────────────────────────────────────────────────────────
 *   Day   onBackground/BG 14.9:1 · onPrimary/primary 10.8:1 · primary/BG 9.7:1
 *   Night onBackground/BG 14.8:1 · onPrimary/primary 7.0:1 · primary/BG 7.5:1
 *   全部满足正文 4.5:1 / 大字号与图形 3:1。
 *
 * 本主题在 GLASS_BACKGROUND_THEMES 里（Theme.kt），所以真实观感链路是三段：
 *   底图（harbor_chat_bg.webp，与 harbor / creamrose 同一张）
 *   → 上层 scrim（THEME_BACKGROUND_SCRIM["mistgarden"]，日间奶油白 / 夜间深栗褐）
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
 * 日间：奶油白做底，深红做主色。
 * 微暖奶油近白底 + 暖白 surface / AI 气泡 + 淡玫瑰白用户气泡，
 * 强调色是深红 #7A1418 / 酒红 #A8323A / 玫瑰 #C97F8A 三支。
 */
private val lightScheme = lightColorScheme(
    // Primary #7A1418（深红 —— 参考图顶栏蕾丝与底栏的主体色）
    primary = Color(0xFF7A1418),
    // Surface #FFFDF8（暖白 —— 深红底上的文字用暖白，不用纯白）
    onPrimary = Color(0xFFFFFDF8),
    // 主色的浅容器（比用户气泡再淡一档的玫瑰白）
    primaryContainer = Color(0xFFF3D9D8),
    // Text 同族偏深
    onPrimaryContainer = Color(0xFF3D0D10),
    // Secondary #A8323A（酒红 —— 比主色浅一档的次强调）
    secondary = Color(0xFFA8323A),
    // Surface #FFFDF8
    onSecondary = Color(0xFFFFFDF8),
    // User #F2E4E2（用户气泡：淡玫瑰白）
    secondaryContainer = Color(0xFFF2E4E2),
    // Text #2B1A1C
    onSecondaryContainer = Color(0xFF2B1A1C),
    // Accent #C97F8A（玫瑰，点缀）
    tertiary = Color(0xFFC97F8A),
    // Surface #FFFDF8
    onTertiary = Color(0xFFFFFDF8),
    // Accent 的极浅版（思考 / 工具卡底色）
    tertiaryContainer = Color(0xFFF7E3E6),
    // Text 同族偏深
    onTertiaryContainer = Color(0xFF4A2229),
    // M3 语义红（整屏偏红，但错误仍要能与主色区分：取更暗更沉的砖红）
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFDF8),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF75201B),
    // BG #F7F3EE（微暖奶油白）
    background = Color(0xFFF7F3EE),
    // Text #2B1A1C
    onBackground = Color(0xFF2B1A1C),
    // Surface #FFFDF8（暖白）
    surface = Color(0xFFFFFDF8),
    // Text #2B1A1C
    onSurface = Color(0xFF2B1A1C),
    // Muted #EDE3DE（行内代码底等）
    surfaceVariant = Color(0xFFEDE3DE),
    // Text 稍浅一档
    onSurfaceVariant = Color(0xFF6B565A),
    outline = Color(0xFF8E787C),
    outlineVariant = Color(0xFFDCCFCC),
    // M3 标准
    scrim = Color(0xFF000000),
    // Text #2B1A1C
    inverseSurface = Color(0xFF2B1A1C),
    // Surface #FFFDF8
    inverseOnSurface = Color(0xFFFFFDF8),
    // 暗底上的浅主色
    inversePrimary = Color(0xFFF0AAB0),
    // 比 Surface 略沉一档
    surfaceDim = Color(0xFFEFE8E2),
    surfaceBright = Color(0xFFFFFDF8),
    surfaceContainerLowest = Color(0xFFFFFDF8),
    // Input #FBF6EE（GLASS 输入框）
    surfaceContainerLow = Color(0xFFFBF6EE),
    // 输入框（FLAT）= surfaceContainer；取 Input 的极小加深版，读到边界但仍是浅底
    surfaceContainer = Color(0xFFF5EDE4),
    // AI 气泡 = AI #FFFDF8（与 surface 同色：AI 气泡不上色，靠页面奶油底把它托出来）
    surfaceContainerHigh = Color(0xFFFFFDF8),
    // Muted #EDE3DE
    surfaceContainerHighest = Color(0xFFEDE3DE),
)

/*
 * 夜间：深栗褐做底，亮玫瑰红做主色。
 * 带红调的近黑底 + 暖调 surface + 深玫瑰褐用户气泡，
 * 主色提亮到 #E88C93 —— 暗底上深红读不出，必须提亮才保住「红」。
 */
private val darkScheme = darkColorScheme(
    // Primary #E88C93（暗底上提亮的玫瑰红）
    primary = Color(0xFFE88C93),
    // BG #1A1416
    onPrimary = Color(0xFF1A1416),
    // 主色的暗容器
    primaryContainer = Color(0xFF5C1A20),
    // Text #F0E6E4
    onPrimaryContainer = Color(0xFFF7DADD),
    // 暗底上的浅主色
    inversePrimary = Color(0xFFF0AAB0),
    // Secondary #CE7C82（玫瑰褐）
    secondary = Color(0xFFCE7C82),
    // BG
    onSecondary = Color(0xFF1A1416),
    // User #3A2126（用户气泡：深玫瑰褐）
    secondaryContainer = Color(0xFF3A2126),
    // Text #F0E6E4
    onSecondaryContainer = Color(0xFFF0E6E4),
    // Accent #D99AA4（玫瑰，点缀）
    tertiary = Color(0xFFD99AA4),
    // BG
    onTertiary = Color(0xFF1A1416),
    // 玫瑰的暗容器
    tertiaryContainer = Color(0xFF442830),
    // Text #F0E6E4
    onTertiaryContainer = Color(0xFFF0E6E4),
    // 比主色更沉的砖红（错误要与主色可区分）
    error = Color(0xFFD98C86),
    // BG
    onError = Color(0xFF1A1416),
    // 砖红的暗版（错误容器）
    errorContainer = Color(0xFF5C2A26),
    // Text #F0E6E4
    onErrorContainer = Color(0xFFF0E6E4),
    // BG #1A1416（深栗褐 —— 带红调的近黑，不是中性灰黑）
    background = Color(0xFF1A1416),
    // Text #F0E6E4
    onBackground = Color(0xFFF0E6E4),
    // Surface #251C1E（暖调深褐）
    surface = Color(0xFF251C1E),
    // Text #F0E6E4
    onSurface = Color(0xFFF0E6E4),
    // Muted #3D2F31（行内代码底）
    surfaceVariant = Color(0xFF3D2F31),
    // Text 稍暗一档
    onSurfaceVariant = Color(0xFFC4B4B6),
    outline = Color(0xFF8A7A7C),
    outlineVariant = Color(0xFF3D2F31),
    // M3 标准
    scrim = Color(0xFF000000),
    // Text #F0E6E4
    inverseSurface = Color(0xFFF0E6E4),
    // Surface #251C1E
    inverseOnSurface = Color(0xFF251C1E),
    // 比 BG 再深一档
    surfaceDim = Color(0xFF140F10),
    // 比 Surface 再亮一档
    surfaceBright = Color(0xFF302427),
    surfaceContainerLowest = Color(0xFF140F10),
    // Input #221A1C（GLASS 输入框）
    surfaceContainerLow = Color(0xFF221A1C),
    // Surface #251C1E
    surfaceContainer = Color(0xFF251C1E),
    // AI 气泡 = AI #26191C（比 Surface 略偏红一档）
    surfaceContainerHigh = Color(0xFF26191C),
    // Muted #3D2F31
    surfaceContainerHighest = Color(0xFF3D2F31),
)
