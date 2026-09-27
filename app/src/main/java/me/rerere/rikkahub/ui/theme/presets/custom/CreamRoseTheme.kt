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
 * ─────────────────────────────────────────────────────────────────────────
 * 材质关系（不是「配色换深」）
 * ─────────────────────────────────────────────────────────────────────────
 * 参照 Tidal Echo（tidal/index.html）实测的层间步长 —— 它是纯亮色主题，只借关系不借色：
 *
 *     页面 98.10 · composer 95.91(-2.20) · AI 气泡 94.08(-4.02) · 用户气泡 90.73(-7.37)
 *     AI↔用户 3.34 · 彩度 C*：页面 1.45 / 卡片 1.72 / AI 2.66 / composer 3.69 / 用户 5.08
 *
 * 结构：**页面在极端那一端**，其余全部朝反方向逐档走，相邻档只差 2~7 个 L*。
 *   日间：页面最亮，气泡/输入/卡片全部压暗。
 *   夜间：页面最暗，气泡/输入/卡片全部提亮 —— 镜像，不是重新设计。
 * 手法：气泡无描边（--bubble-*-line: transparent）、雾面、卡片 rgba(244,248,250,.42) ≈ 页面、
 *       hairline rgba(74,93,108,.20) 极弱。层次全部由「面色阶差」做出来，不引入黑描边和重投影。
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 夜间重做的三件事（批 26）
 * ─────────────────────────────────────────────────────────────────────────
 * 1) 根 scrim 从暖棕 #1D1411 换成中性 #171716。旧值页面彩度 C*=4.6 ——
 *    底图（暖白纸纹，L*=92.9）被 scrim 压到 L*20 之后，剩下那点暖调被放大成
 *    整屏一层棕色，这就是「整屏棕滤镜」。换成中性后页面 C*=1.0。
 *    **页面亮度不动**（仍是 L*20.5 / 24.0 / 16.0 三段），只把彩度收掉。
 * 2) 卡片离页面只留 ΔL* 2.4~3.2（旧值 surfaceBright #614339 离页面 8+，
 *    读作一块独立的大面积实心卡片）。边界改由 outlineVariant（ΔL* 7）交代。
 * 3) 气泡彩度按 Tidal Echo 尺度收：用户气泡 C* 19.0 -> 5.1、思考卡 C* 19.1 -> 7.0。
 *    组件之间靠**明度**区分（4.02 / 7.37 / 3.34），不靠色差。
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 批 27：夜间层级差放宽（日间不动，底图 / scrim / 磨砂 / hairline 全部不动）
 * ─────────────────────────────────────────────────────────────────────────
 * 用户原话：「明暗材质逻辑正确，不要重新设计。现在仅调整视觉对比度 …… 提高背景→AI 气泡
 * →用户气泡→输入框→模型 pill 之间的明度/透明度层级差 …… 不要通过加重阴影、粗描边或
 * 提高饱和度来增加对比。」
 *
 * 只动**合成后的 L***，彩度逐槽位不高于批 26，层间步长放宽到约 1.5 倍：
 *
 *     槽位                    批 26 Δ页面   批 27 Δ页面   读作
 *     surfaceContainerLow        2.20        2.20      会话列表项 / GLASS 输入框
 *     surfaceContainer           2.40        3.60      卡片 / 顶栏 / 模型 pill
 *     surfaceBright              3.20        5.00      嵌套卡 / 列表项
 *     surfaceContainerHigh       4.02        6.00      AI 气泡 / FLAT 输入框 / 附件 chip
 *     tertiaryContainer          6.00        9.00      思考卡
 *     secondaryContainer         7.37       11.10      用户气泡
 *     surfaceVariant             8.00       10.00      行内代码底 / Moodlet 徽章填充
 *
 * 模型 pill 从 surfaceContainerHigh 换到 **surfaceContainer**：它原来和输入框（FLAT 模式
 * 同样读 surfaceContainerHigh）同色，6dp 间距下两块表面糊在一起，分不出哪里是 pill、
 * 哪里是输入框。换槽位后 pill ↔ 输入框 ΔL* 2.40，边界读得出来。
 *
 * 为什么不能靠调 alpha：合成 L* 对 alpha 极不敏感（0.82 -> 1.00 只走 1 个 L*）。
 * 拉开层级只能改槽位色阶或换槽位。
 *
 * 本主题页面是三段渐变（L* 24.0 / 20.5 / 16.0），上表按**中段 20.5** 求解；
 * 另两段的 Δ 会各差 ±4，但**每一档之间的差**不随位置变 —— 层级关系仍然成立。
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 槽位映射（按聊天界面「实际读哪个槽位」对齐，不是按槽位名字）
 * ─────────────────────────────────────────────────────────────────────────
 *   页面            -> background / surface（本主题根 background 的 alpha 被置 0，
 *                      真实页面由 RouteActivity 的「底图 + 三段 scrim」合成）
 *   AI 气泡         -> surfaceContainerHigh（默认 FLAT 模式下输入框也读它，同材质是有意的）
 *   用户气泡        -> secondaryContainer
 *   思考卡          -> tertiaryContainer（本主题在 THEME_THINKING_CONTAINER_THEMES 里；
 *                      SearchPage 也拿它当命中高亮底，所以多留一档分离度 ΔL* 6.0）
 *   输入框(GLASS)   -> surfaceContainerLow
 *   卡片 / 顶栏     -> surfaceContainer
 *   模型 pill       -> surfaceContainer（**不能**和输入框共用 High，否则两块糊在一起）
 *   列表项 / 嵌套卡 -> surfaceBright
 *   行内代码底      -> surfaceVariant（同时是 MoodletBadge 的填充来源）
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 数值怎么来的
 * ─────────────────────────────────────────────────────────────────────────
 * 本主题在 GLASS_BACKGROUND_THEMES 里（Theme.kt:46），所以真实观感链路是三段：
 *     底图 -> 根 scrim 合成(页面) -> 再叠 surface@interfaceSurfaceOpacity(默认 0.82)
 * 用户看到的是**合成色**，不是这里写的名义色。下面每个色值都是「先定合成色的 L* / C*，
 * 再反解名义色」得到的（nom = (composite - 0.18*page) / 0.82）。
 * 推导脚本：st/theme_gen.py / st/theme_final.py（合成链路模型在 st/theme_night.py）。
 * 注释里的 L* / C* 都是**合成后**的值。
 *
 * surfaceVariant 还要额外满足批 12 的坑：MoodletBadge 的填充是 surfaceVariant@0.45
 * 叠在 AI 气泡（surfaceContainerHigh）上，同色叠加会让填充 ΔL* 归零。
 * 这里与 AI 气泡的合成差 ΔL* 4.00，×0.45 = 1.80 >= 1，徽章填充可见。
 *
 * 一处刻意的取舍（是色卡本身带来的，不是笔误）：
 *   onSecondaryContainer 取正文色而不是「压在用户气泡上的浅色」：聊天界面里
 *   助手语音条的未播放波形用它，而它压在浅色的助手气泡上，必须是深色才看得见。
 *   （用户气泡里的正文走的是 onSurface / onBackground。）
 *
 * 描边档 outline / outlineVariant 全是低彩度暖灰，没有一根黑描边。
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
    surfaceContainerHigh = Color(0xFFF8F3EF),
    surfaceContainerHighest = Color(0xFFDDCBC1),
)

private val darkScheme = darkColorScheme(
    // Accent #CF7D8B —— 保留粉身份。只做强调色（发送键等），不参与气泡材质。
    primary = Color(0xFFCF7D8B),
    onPrimary = Color(0xFF3A1A20),
    primaryContainer = Color(0xFF5A2F38),
    onPrimaryContainer = Color(0xFFF5DDE0),
    secondary = Color(0xFFC79AA2),
    onSecondary = Color(0xFF3A1A20),
    // 用户气泡：合成 L*31.5 / Δ页面 +11.10（批 26 是 +7.37）/ C*4.8。
    // 旧值 #623A41 的 C* 是 19.0 —— 一块独立跳出来的玫瑰色板，不是同一块材质。
    secondaryContainer = Color(0xFF594D4E),
    onSecondaryContainer = Color(0xFFD8CCCE),
    tertiary = Color(0xFFC4A79C),
    onTertiary = Color(0xFF3A1A20),
    // 思考卡：合成 L*29.4 / Δ页面 +9.00（批 26 是 +6.00）/ C*7.0（旧值 C* 19.1）
    tertiaryContainer = Color(0xFF50493D),
    onTertiaryContainer = Color(0xFFD8CCCE),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    // 页面名义色 = 中性 scrim 合成值。本主题根 background 的 alpha 被 Theme.kt 置 0，
    // 页面真实颜色由 RouteActivity 的 scrim 合成决定；这个值在
    // 「助手自定义底图」那条路的渐变遮罩里用到（rememberChatBackgroundVisuals）。
    background = Color(0xFF323130),
    onBackground = Color(0xFFEEE5E5),
    surface = Color(0xFF323130),
    onSurface = Color(0xFFEEE5E5),
    // 合成 L*30.4 / C*3.5。Moodlet 徽章填充读它（surfaceVariant@0.45 叠在助手气泡上），
    // 必须与 surfaceContainerHigh 拉开：合成差 ΔL* 4.00 -> 填充可见差 1.80。
    // 这是批 12 的坑（同色叠加会让填充 ΔL* 归零），定色时不能只看「好不好看」。
    surfaceVariant = Color(0xFF534A4C),
    onSurfaceVariant = Color(0xFFD8CCCE),
    // 离页面 ΔL* 22，读作一条可见的分隔
    outline = Color(0xFF746E6D),
    // 极弱 hairline：离页面只有 ΔL* 7。卡片的边界靠它交代，不靠填充差。
    outlineVariant = Color(0xFF484343),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFEEE5E5),
    inverseOnSurface = Color(0xFF332827),
    inversePrimary = Color(0xFFA95362),
    surfaceDim = Color(0xFF2D2D2D),
    // 列表项 / 嵌套卡：合成 Δ页面 +5.00（批 26 是 +3.20）
    surfaceBright = Color(0xFF423E3F),
    surfaceContainerLowest = Color(0xFF2B2B2B),
    // GLASS 模式的输入框：合成 Δ页面 +2.20（Tidal Echo composer 参照 2.20）/ C*3.5
    surfaceContainerLow = Color(0xFF3E3537),
    // 卡片 + 顶栏 + 模型 pill：合成 Δ页面 +3.60（批 26 是 +2.40）
    surfaceContainer = Color(0xFF3E3A3B),
    // AI 气泡；默认（FLAT）模式下输入框也读这一槽位 —— 两者同材质是有意的。
    // 合成 Δ页面 +6.00（批 26 是 +4.02）/ C*2.4。
    surfaceContainerHigh = Color(0xFF464041),
    surfaceContainerHighest = Color(0xFF484042),
)
