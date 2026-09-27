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
 * 层级参照 Tidal Echo（web/index.html）：底图是主体，UI 只是压在底图上的低不透明度、
 * 低饱和度色层。所以这里不引入任何黑色描边和重投影 —— 层次全部由「面色阶差」做出来：
 * 相邻两档只差几个 RGB 单位，读起来是同一张纸上的几层，而不是几块叠起来的板。
 *
 * 八个锚点色，Day / Night 各自独立给出（夜间不是由日间降亮度推出来的）：
 *   Day   Background #F5EEE9 · Main Text #332827 · Global Text #403332
 *         User Bubble #B86F79 · AI Bubble #F1E5DE · Thinking Bubble #E1D0C5
 *         Accent #A95362 · Input #EEE1DB
 *   Night Background #412D26 · Main Text #EEE5E5 · Global Text #D8CCCE
 *         User Bubble #623A41 · AI Bubble #4F362E · Thinking Bubble #553727
 *         Accent #CF7D8B · Input #4F362E（默认 FLAT 模式与 AI 气泡同槽位）
 *
 * 锚点落到 M3 槽位，按聊天界面「实际读哪个槽位」对齐，不是按槽位名字：
 *   Background      -> background / surface
 *   Main Text       -> onSurface / onBackground
 *   Global Text     -> onSurfaceVariant
 *   Accent          -> primary
 *   User Bubble     -> secondaryContainer
 *   AI Bubble       -> surfaceContainerHigh
 *   Thinking Bubble -> tertiaryContainer（本主题的思考卡片改读这一槽位）
 *   Input           -> surfaceContainerLow
 *
 * 其余槽位在同一色相族内插值补齐，只做明度阶差、不加彩度：
 *   surfaceContainerLowest 是比 background 再亮一档的「纸面」，
 *   surfaceContainer / surfaceVariant 夹在 Input 与 AI Bubble 之间，
 *   surfaceContainerHighest / surfaceDim 是比思考气泡再深一档的底。
 *
 * 两处刻意的取舍（是色卡本身带来的，不是笔误）：
 *   1) Input #EEE1DB 比 AI Bubble #F1E5DE 略深，而代码把输入框读 surfaceContainerLow、
 *      把助手气泡读 surfaceContainerHigh，于是 Low 比 High 深了 3 个 RGB 单位 ——
 *      这正是「气泡与输入区靠轻微明度差区分」本身，视觉上读不出倒置。
 *   2) onSecondaryContainer 取正文色而不是「压在用户气泡上的浅色」：聊天界面里
 *      助手语音条的未播放波形用它，而它压在浅色的助手气泡上，必须是深色才看得见。
 *      （用户气泡里的正文走的是 onSurface / onBackground。）
 *
 * 描边档 outline / outlineVariant 全部是有彩度的暖灰，没有一根黑描边。
 *
 * 夜间是「合成后」的色卡：本主题在 GLASS_BACKGROUND_THEMES 里（Theme.kt），
 * 页面真实颜色由 RouteActivity 的「底图 + 三段 scrim」合成，所有 surface 槽位
 * 又被 Theme.kt 乘上 interfaceSurfaceOpacity（默认 82%）。下面 Night 的色值是按
 * 这条链路反解出来的名义值：相邻两档合成后只差 ΔL* 2~4，与 Tidal Echo 的实测
 * 步长一致。数值推导见 st/crema_night.py。
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
    // 用户气泡 #623A41 —— 低饱和粉棕，与 AI 气泡同一材质，只差一档明度
    // （合成后 ΔL* 3.40，Tidal Echo 实测参照 3.34）。旧值 #8E4C59 是高饱和粉，
    // 合成后与页面差 24.5，是一块独立跳出来的色板，不是同一块材质。
    secondaryContainer = Color(0xFF623A41),
    onSecondaryContainer = Color(0xFFD8CCCE),
    tertiary = Color(0xFFC4A79C),
    onTertiary = Color(0xFF3A1A20),
    // Thinking #553727 —— 比 AI 气泡只亮 ΔL* 0.70（旧值差 5.11，读作一块突兀亮色块）
    tertiaryContainer = Color(0xFF553727),
    onTertiaryContainer = Color(0xFFD8CCCE),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    // 页面 #412D26（暖棕）。本主题根 background 的 alpha 被 Theme.kt 置 0，
    // 页面真实颜色由 RouteActivity 的 scrim 合成决定；这个值只在
    // 「助手自定义底图」那条路的渐变遮罩里用到（rememberChatBackgroundVisuals）。
    background = Color(0xFF412D26),
    onBackground = Color(0xFFEEE5E5),
    surface = Color(0xFF412D26),
    onSurface = Color(0xFFEEE5E5),
    // Moodlet 徽章填充读这一槽位（surfaceVariant@0.45 叠在助手气泡上），必须比
    // surfaceContainerHigh 明显亮：两档名义值差 ΔL* 4.7 -> 填充可见差 2.57。
    // 这是批 12 的坑（同色叠加会让填充 ΔL* 归零），定色时不能只看「好不好看」。
    surfaceVariant = Color(0xFF5D4037),
    onSurfaceVariant = Color(0xFFD8CCCE),
    outline = Color(0xFF6F4D41),
    outlineVariant = Color(0xFF6A493E),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFEEE5E5),
    inverseOnSurface = Color(0xFF332827),
    inversePrimary = Color(0xFFA95362),
    surfaceDim = Color(0xFF33231E),
    surfaceBright = Color(0xFF614339),
    surfaceContainerLowest = Color(0xFF372620),
    // GLASS 模式的输入框读这一槽位（合成后 ΔL* 2.20 于页面，Tidal Echo 参照 2.20）
    surfaceContainerLow = Color(0xFF49322B),
    surfaceContainer = Color(0xFF4B342C),
    // AI 气泡；默认（FLAT）模式下输入框也读这一槽位 —— 两者同材质是有意的，
    // 不是漏配。旧值 #2D2527 与页面合成差 13.7（顶部），气泡像贴在灰底上的黑卡。
    surfaceContainerHigh = Color(0xFF4F362E),
    surfaceContainerHighest = Color(0xFF583D34),
)
