package me.rerere.rikkahub.ui.theme.presets.custom

import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.theme.PresetTheme

/*
 * 星夜 Night Sky —— 冷蓝夜空，小面积暖金星光。
 * 一个主题两态，跟随橘瓣的深浅切换（ColorMode.SYSTEM/LIGHT/DARK）。
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 色源：用户 2026-10-05 给出的固定色卡（逐字落槽，不再自行「理解星夜」）
 * ─────────────────────────────────────────────────────────────────────────
 *   语义      Day        Night
 *   背景      #EAF4FF    #0B1220
 *   主表面    #F7FAFF    #131D2D
 *   次表面    #E2EEFA    #1A2739
 *   边框      #B8CCE3    #34445A
 *   主文字    #26364A    #EAF1FA
 *   次文字    #66788D    #9CAEC3
 *   主色      #4C78C8    #8EBBFF
 *   强调色    #E7B84F    #F0C85B
 *   用户气泡  #D9E9FA    #203653
 *   AI 气泡   #F3F7FC    #182536
 *
 * 三条硬约束（用户原话）：
 *   1) 蓝底、蓝灰面 —— 背景与表面全在蓝灰族里，靠明度分档。
 *   2) 暖黄只做小面积星光（= 上面的「强调色」）。
 *   3) 粉 / 樱花 / 和纸米色一律不要。
 *
 * ⚠️ 本文件旧版（批 24~28b）那套「参照 Tidal Echo、先定合成色 L* / C* 再反解名义色」
 *    的推导**全部作废** —— 用户拍板「旧的亮度约束作废，直接按新色卡走」。
 *    连同「夜间不能是近黑」「页面彩度 C* <= 2」等约束一起作废。
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 槽位映射（按聊天界面「实际读哪个槽位」对齐，不是按槽位名字）
 * ─────────────────────────────────────────────────────────────────────────
 *   页面                -> background / surface          （色卡：背景）
 *   卡片 / 顶栏 / pill   -> surfaceContainer              （色卡：主表面）
 *   GLASS 输入框        -> surfaceContainerLow           （色卡：次表面）
 *   AI 气泡 / FLAT 输入框 -> surfaceContainerHigh         （色卡：AI 气泡）
 *   用户气泡            -> secondaryContainer            （色卡：用户气泡）
 *   主色                -> primary                       （色卡：主色）
 *   强调（小图标 / 进度 / 徽章）-> tertiary                （色卡：强调色）
 *   正文 / 次级文字      -> onSurface / onSurfaceVariant  （色卡：主文字 / 次文字）
 *   描边                -> outline / outlineVariant      （色卡：边框）
 *
 * 色卡没列到的槽位（surfaceVariant / surfaceBright / surfaceDim /
 * surfaceContainerLowest / surfaceContainerHighest / inverse* / secondary /
 * primaryContainer / on* 各色）在**同一蓝灰色族**内推导，脚本 st/theme_b62.py。
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 🔴 tertiaryContainer：批 62 拍板 ③ —— 走低饱和蓝灰，不再承担暖色层
 * ─────────────────────────────────────────────────────────────────────────
 * 它在仓库里被 **22 处**读，其中大半是**容器底**（思考卡 ChainOfThought、McpPicker
 * 徽章、MemoryBankPage 的 StatCard / 记忆卡、药丸 pill、Tag、搜索命中高亮…）。
 * 旧版把「星夜唯一的暖色层」放在这里（#F1E6C9 / #3A3325，暖米色）；若换成色卡里的
 * 满饱和暖金，这些容器就会变成一块块金块 —— 与「暖黄只做小面积星光」冲突。
 *
 * 用户 2026-10-05 拍板：「tertiaryContainer 用于容器背景时改为低饱和蓝灰；
 * tertiary / tertiary tint 保留现有强调色语义」，并选定**只改槽位、不动调用点**。
 * ⇒ 本主题下 tertiaryContainer = 蓝灰；暖色只走 `tertiary`（= 色卡的强调色）。
 *    其余 4 个活主题的 tertiaryContainer 不受影响（槽位值写在本文件里）。
 *
 * 两处**文字底**（Markdown 引用角标 @0.2 / Tag 填充 @0.62）靠 onTertiaryContainer
 * 取色，这里取 = 主文字色，实测对比度 10.04:1（日）/ 14.16:1（夜）。
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 数值怎么落、以及三处已知的「色卡自带」现象
 * ─────────────────────────────────────────────────────────────────────────
 * 9 个 surface* 槽位仍会被 Theme.kt:156-166 用 interfaceSurfaceOpacity（默认 0.82）
 * 覆盖 alpha，观感 = 名义色@0.82 over 页面。本主题**不**在 GLASS_BACKGROUND_THEMES
 * 里，background 保持不透明。合成值与名义值的差 <= 约 2 个 L*，所以不做反解。
 *
 * 两处低于 WCAG AA 4.5:1 的对比是**色卡自带的**，本轮未改：
 *   - 日间 次文字 #66788D on 背景 #EAF4FF = 4.07:1
 *   - 日间 onPrimary #FFFFFF on 主色 #4C78C8 = 4.35:1（白已是可达上限）
 * 第三处是「日间 页面 ↔ AI 气泡」合成后只差 ΔL* 1.08（#EAF4FF vs #F3F7FC，几乎同色）
 * —— 助手气泡靠 ChatMessage.kt 的 outlineVariant@0.18 发丝描边区分，不靠填充。
 *
 * 思考卡：本主题仍在 THEME_THINKING_CONTAINER_THEMES + THEME_THINKING_SURFACE_THEMES
 * 里 ⇒ 它读 tertiaryContainer 并真的画背景，即「一张蓝灰卡」（批 62 后不再是暖色卡）。
 *
 * 描边档 outline / outlineVariant 全是低彩度冷蓝灰，没有一根黑描边。
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
    // 色卡「主色」
    primary = Color(0xFF4C78C8),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD6E4F8),
    // 用户侧语音条 / 工具条的前景读它，压在浅色的用户气泡上必须是深色
    onPrimaryContainer = Color(0xFF16325A),
    secondary = Color(0xFF5A7BA6),
    onSecondary = Color(0xFFFFFFFF),
    // 色卡「用户气泡」
    secondaryContainer = Color(0xFFD9E9FA),
    onSecondaryContainer = Color(0xFF1F3A5C),
    // 色卡「强调色」—— 星夜唯一的暖色，只做小面积（图标 / 进度 / 徽章）
    tertiary = Color(0xFFE7B84F),
    onTertiary = Color(0xFF3A2D08),
    // 批 62 拍板 ③：低饱和蓝灰（原暖米色 #F1E6C9 作废）。22 处容器底 / 文字底读它。
    tertiaryContainer = Color(0xFFD7E3F1),
    onTertiaryContainer = Color(0xFF26364A),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    // 色卡「背景」
    background = Color(0xFFEAF4FF),
    // 色卡「主文字」
    onBackground = Color(0xFF26364A),
    surface = Color(0xFFEAF4FF),
    onSurface = Color(0xFF26364A),
    // 行内代码底 / MoodletBadge 填充来源；与 AI 气泡合成差 ΔL* 4.8
    // （MoodletBadge 填充 = surfaceVariant@0.45 叠在 AI 气泡上，实测 ΔL* 2.54 >= 1）
    surfaceVariant = Color(0xFFDCE7F3),
    // 色卡「次文字」
    onSurfaceVariant = Color(0xFF66788D),
    // 色卡「边框」
    outline = Color(0xFFB8CCE3),
    // 更弱一档：助手气泡的发丝描边读它（ChatMessage.kt 里 @0.18）
    outlineVariant = Color(0xFFD5E2F0),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF26364A),
    inverseOnSurface = Color(0xFFEAF4FF),
    inversePrimary = Color(0xFFA8C6F0),
    surfaceDim = Color(0xFFE6EFF9),
    surfaceBright = Color(0xFFFAFCFF),
    surfaceContainerLowest = Color(0xFFFCFEFF),
    // 色卡「次表面」—— GLASS 模式输入框
    surfaceContainerLow = Color(0xFFE2EEFA),
    // 色卡「主表面」—— 卡片 / 顶栏 / 模型 pill
    surfaceContainer = Color(0xFFF7FAFF),
    // 色卡「AI 气泡」—— FLAT 模式输入框也读它
    surfaceContainerHigh = Color(0xFFF3F7FC),
    surfaceContainerHighest = Color(0xFFDCE8F5),
)

private val darkScheme = darkColorScheme(
    // 色卡「主色」
    primary = Color(0xFF8EBBFF),
    onPrimary = Color(0xFF0A1B33),
    primaryContainer = Color(0xFF23406B),
    // 用户侧语音条 / 工具条的前景读它，压在深色的用户气泡上必须是浅色
    onPrimaryContainer = Color(0xFFCFE0FA),
    secondary = Color(0xFF8AA9CF),
    onSecondary = Color(0xFF0A1B33),
    // 色卡「用户气泡」
    secondaryContainer = Color(0xFF203653),
    onSecondaryContainer = Color(0xFFDCE9FA),
    // 色卡「强调色」—— 星夜唯一的暖色，只做小面积（图标 / 进度 / 徽章）
    tertiary = Color(0xFFF0C85B),
    onTertiary = Color(0xFF241C05),
    // 批 62 拍板 ③：低饱和蓝灰（原暖褐 #3A3325 作废）。理由见文件头。
    tertiaryContainer = Color(0xFF1E2B3C),
    onTertiaryContainer = Color(0xFFEAF1FA),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    // 色卡「背景」—— 深海军蓝，不是近黑（旧值 #242628 一并作废）
    background = Color(0xFF0B1220),
    // 色卡「主文字」
    onBackground = Color(0xFFEAF1FA),
    surface = Color(0xFF0B1220),
    onSurface = Color(0xFFEAF1FA),
    // 行内代码底 / MoodletBadge 填充来源；与 AI 气泡合成差 ΔL* 5.5
    // （MoodletBadge 填充 = surfaceVariant@0.45 叠在 AI 气泡上，实测 ΔL* 3.75 >= 1）
    surfaceVariant = Color(0xFF243349),
    // 色卡「次文字」
    onSurfaceVariant = Color(0xFF9CAEC3),
    // 色卡「边框」
    outline = Color(0xFF34445A),
    // 更弱一档：助手气泡的发丝描边读它（ChatMessage.kt 里 @0.18）
    outlineVariant = Color(0xFF26364C),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFEAF1FA),
    inverseOnSurface = Color(0xFF0B1220),
    inversePrimary = Color(0xFF4C78C8),
    surfaceDim = Color(0xFF0E1727),
    surfaceBright = Color(0xFF1B2839),
    surfaceContainerLowest = Color(0xFF080E19),
    // 色卡「次表面」—— GLASS 模式输入框
    surfaceContainerLow = Color(0xFF1A2739),
    // 色卡「主表面」—— 卡片 / 顶栏 / 模型 pill
    surfaceContainer = Color(0xFF131D2D),
    // 色卡「AI 气泡」—— FLAT 模式输入框也读它
    surfaceContainerHigh = Color(0xFF182536),
    surfaceContainerHighest = Color(0xFF223148),
)
