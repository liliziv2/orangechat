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
 * ─────────────────────────────────────────────────────────────────────────
 * 材质关系（不是「配色换深」）
 * ─────────────────────────────────────────────────────────────────────────
 * 参照 Tidal Echo（tidal/index.html）实测的层间步长 —— 它是纯亮色主题，只借关系不借色：
 *
 *     页面 98.10 · composer 95.91(-2.20) · AI 气泡 94.08(-4.02) · 用户气泡 90.73(-7.37)
 *     AI↔用户 3.34 · 彩度 C*：页面 1.45 / 卡片 1.72 / AI 2.66 / composer 3.69 / 用户 5.08
 *
 * 结构：**页面在极端那一端**，其余全部朝反方向逐档走，相邻档只差 2~7 个 L*。
 *   日间：页面最亮（L* 96.8），气泡/输入/卡片全部压暗。
 *   夜间：页面最暗（L* 15.0），气泡/输入/卡片全部提亮 —— 镜像，不是重新设计。
 *
 * 三条硬约束（都来自用户原话，不是我的偏好）：
 *   1) 页面彩度 C* <= 2。超过就会被读成「整屏蓝色滤镜」——一整块色带盖在所有页面上。
 *      夜间页面取 #242628（L*15.0 / C*1.5）：一块软炭灰，不是近黑。
 *   2) 夜间页面不能是近黑。近黑底 + 提亮气泡 = 大面积实心深色卡片；
 *      而且近黑会把所有容器都逼成「从黑里挖出来的洞」。
 *      旧值 #090D14（L*3.58）就是这么翻车的。
 *   3) 不同组件靠**明度**区分，不靠色差。所以气泡的彩度都压在 Tidal Echo 那一档
 *      （AI 2.66 / 用户 5.08），而不是各自一块颜色。
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 批 27：夜间层级差放宽（日间不动）
 * ─────────────────────────────────────────────────────────────────────────
 * 用户原话：「明暗材质逻辑正确，不要重新设计。现在仅调整视觉对比度 …… 提高背景→AI 气泡
 * →用户气泡→输入框→模型 pill 之间的明度/透明度层级差 …… 不要通过加重阴影、粗描边或
 * 提高饱和度来增加对比。」
 *
 * 所以只动**合成后的 L***，彩度一律不高于批 26（逐槽位核对过），槽位映射、GLASS、
 * scrim、hairline 全部不动。层间步长从批 26 的「精确复刻 Tidal Echo」放宽到约 1.5 倍：
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
 * 为什么不能靠调 alpha：合成 L* 对 alpha 极不敏感。surfaceContainerHigh 在 0.82 -> 1.00
 * 之间，合成 L* 只从 19.0 走到 20.0。所以「拉开层级」只能改槽位色阶或换槽位。
 *
 * 硬约束（脚本 st/theme_contrast.py 逐条校验）：
 *   - Moodlet 徽章 surfaceVariant@0.45 叠在 AI 气泡上：ΔL* 4.00（×0.45 = 1.80 >= 1）
 *   - 嵌套卡必须比它所在的卡片亮：surfaceBright(5.00) > surfaceContainer(3.60)
 *   - 用户点名的相邻对全部 >= 2.4：页面↔pill 3.60 · pill↔输入框 2.40 · AI↔用户 5.10
 *   - 彩度逐槽位不高于批 26
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 槽位映射（按聊天界面「实际读哪个槽位」对齐，不是按槽位名字）
 * ─────────────────────────────────────────────────────────────────────────
 *   页面            -> background / surface
 *   AI 气泡         -> surfaceContainerHigh（默认 FLAT 模式下输入框也读它，同材质是有意的）
 *   模型 pill       -> surfaceContainer（**不能**和输入框共用 High，否则两块糊在一起）
 *   用户气泡        -> secondaryContainer
 *   思考卡          -> tertiaryContainer（本主题在 THEME_THINKING_CONTAINER_THEMES 里；
 *                      SearchPage 也拿它当命中高亮底，所以多留一档分离度 ΔL* 6.0）
 *   输入框(GLASS)   -> surfaceContainerLow
 *   卡片 / 顶栏     -> surfaceContainer
 *   列表项 / 嵌套卡 -> surfaceBright
 *   行内代码底      -> surfaceVariant（同时是 MoodletBadge 的填充来源）
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 数值怎么来的
 * ─────────────────────────────────────────────────────────────────────────
 * 用户看到的是**合成色**，不是这里写的名义色：surface 槽位会被 Theme.kt:139-153
 * 用 interfaceSurfaceOpacity（默认 0.82）覆盖 alpha，观感 = 名义色@0.82 over 页面。
 * 所以本文件里的每个色值都是「先定合成色的 L* / C*，再反解名义色」得到的
 * （nom = (composite - 0.18*page) / 0.82）。推导脚本：st/theme_gen.py / st/theme_final.py。
 * 下面注释里的 L* / C* 都是**合成后**的值。
 *
 * surfaceVariant 还要额外满足批 12 的坑：MoodletBadge 的填充是 surfaceVariant@0.45
 * 叠在 AI 气泡（surfaceContainerHigh）上，同色叠加会让填充 ΔL* 归零。
 * 这里与 AI 气泡的合成差 ΔL* 4.00，×0.45 = 1.80 >= 1，徽章填充可见。
 *
 * 本主题**不进** GLASS_BACKGROUND_THEMES（Theme.kt:46）：页面是实心色，不叠底图，
 * 所以没有「底图 + scrim」那一段合成，根 background 保持不透明。
 *
 * 描边档 outline / outlineVariant 全是低彩度冷灰，没有一根黑描边；
 * outlineVariant 离页面只有 ΔL* 7，读作一道极弱的分隔线而不是一条描边。
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
    // 用户侧语音条的前景读它，压在浅色的用户气泡上必须是深色
    onPrimaryContainer = Color(0xFF243346),
    secondary = Color(0xFF4E7B99),
    onSecondary = Color(0xFFFFFFFF),
    // 用户气泡：合成 L*89.4 / Δ页面 -7.37（Tidal Echo 参照 7.37）/ C*5.1（参照 5.08）
    secondaryContainer = Color(0xFFD4DDE7),
    onSecondaryContainer = Color(0xFF243346),
    tertiary = Color(0xFF8A7A45),
    onTertiary = Color(0xFFFFFFFF),
    // 思考卡：合成 L*90.8 / Δ页面 -6.00 / C*7.0（功能色，多留一档分离度）
    tertiaryContainer = Color(0xFFE9E0D0),
    onTertiaryContainer = Color(0xFF43391F),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    // 页面：L*96.8 / C*0.9 —— 全屏最亮的一端，其余每一层都从这里往下走
    background = Color(0xFFF4F6F7),
    onBackground = Color(0xFF243346),
    surface = Color(0xFFF4F6F7),
    onSurface = Color(0xFF243346),
    // 合成 L*88.8 / C*3.5。Moodlet 徽章填充读它，必须与 AI 气泡拉开（批 12 的坑）
    surfaceVariant = Color(0xFFD5DBE1),
    // 次级文字：正文色向背景混，合成对比度 4.80:1
    onSurfaceVariant = Color(0xFF626E7B),
    // 离页面 ΔL* 22，读作一条可见的分隔
    outline = Color(0xFFA6ABB3),
    // 极弱 hairline：离页面只有 ΔL* 7
    outlineVariant = Color(0xFFDADEE3),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF243346),
    inverseOnSurface = Color(0xFFF4F6F7),
    inversePrimary = Color(0xFF9CC9E5),
    surfaceDim = Color(0xFFF9F8F8),
    surfaceBright = Color(0xFFF9FCFF),
    surfaceContainerLowest = Color(0xFFFAFAFA),
    // GLASS 模式的输入框：合成 Δ页面 -2.20（Tidal Echo composer 参照 2.20）/ C*3.7
    surfaceContainerLow = Color(0xFFE8EFF6),
    // 卡片 + 顶栏：合成 Δ页面 -2.60（旧值 -3.65）
    surfaceContainer = Color(0xFFEAEDF0),
    // AI 气泡；FLAT 模式输入框也读它：合成 Δ页面 -4.02（参照 4.02）/ C*2.7
    surfaceContainerHigh = Color(0xFFE4E8ED),
    surfaceContainerHighest = Color(0xFFDCE1E6),
)

private val darkScheme = darkColorScheme(
    // Accent #82C4E8
    primary = Color(0xFF82C4E8),
    onPrimary = Color(0xFF0A2030),
    primaryContainer = Color(0xFF1E3D52),
    onPrimaryContainer = Color(0xFFCFE6F6),
    secondary = Color(0xFFA9BECF),
    onSecondary = Color(0xFF12222F),
    // 用户气泡：合成 L*26.1 / Δ页面 +11.10（批 26 是 +7.37）/ C*4.8
    secondaryContainer = Color(0xFF3D444B),
    onSecondaryContainer = Color(0xFFE8EDF2),
    tertiary = Color(0xFFC4B78C),
    onTertiary = Color(0xFF2A2415),
    // 思考卡：合成 L*24.0 / Δ页面 +9.00（批 26 是 +6.00）/ C*7.0
    tertiaryContainer = Color(0xFF433D30),
    onTertiaryContainer = Color(0xFFEDE4CC),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    // 页面：L*15.0 / C*1.5 —— 软炭灰，不是近黑。
    // 旧值 #090D14（L*3.58）就是「大面积实心深色卡片」的根因。
    background = Color(0xFF242628),
    onBackground = Color(0xFFE8EDF2),
    surface = Color(0xFF242628),
    onSurface = Color(0xFFE8EDF2),
    // 合成 L*25.0 / C*3.5；与 AI 气泡差 ΔL* 4.00（×0.45 = 1.80，徽章填充可见）
    surfaceVariant = Color(0xFF3C4146),
    onSurfaceVariant = Color(0xFFA9B6C4),
    // 离页面 ΔL* 22
    outline = Color(0xFF5E6269),
    // 极弱 hairline：离页面只有 ΔL* 7
    outlineVariant = Color(0xFF36383C),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFE8EDF2),
    inverseOnSurface = Color(0xFF242628),
    inversePrimary = Color(0xFF2F6B92),
    surfaceDim = Color(0xFF212121),
    surfaceBright = Color(0xFF313335),
    surfaceContainerLowest = Color(0xFF20201F),
    // GLASS 模式的输入框：合成 Δ页面 +2.20（参照 2.20）/ C*3.5
    surfaceContainerLow = Color(0xFF272C31),
    // 卡片 + 顶栏 + 模型 pill：合成 Δ页面 +3.60（批 26 是 +2.40）
    surfaceContainer = Color(0xFF2D2F31),
    // AI 气泡；FLAT 模式输入框也读它：合成 Δ页面 +6.00（批 26 是 +4.02）/ C*2.4
    surfaceContainerHigh = Color(0xFF333639),
    surfaceContainerHighest = Color(0xFF32363B),
)
