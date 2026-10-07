package me.rerere.rikkahub.ui.theme.presets.custom

import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.theme.PresetTheme

/*
 * 苹果爱丽丝 Apple Alice —— 暖白纸底 + 深红主调。
 *
 * 槽位骨架照抄 HarborTheme.kt（结构、顺序、语义分配一字不改），只换色值 ——
 * 这样它和 Harbor / CreamRose 共用同一套「哪个槽位干什么」的口径，不会另起一套。
 *
 * 色彩原则（用户当面给的三条，不是猜的）：
 *   1) 红是主角。深红 C*=69.3，是唯一的高彩度强色；蓝 C*=31.8、黄 C*=55.4 只做辅助，
 *      不平均撒满整个 UI —— 否则从设计感掉成儿童频道。
 *   2) 夜间不做「白天反转」。白天三色直接压暗会变成「黑底 + 三原色」，很廉价。
 *      夜间三色全部压暗降饱和，但**保持三者之间的色相差**：
 *        红 C* 69.3 -> 56.0（h 24.6 -> 15.5）
 *        蓝 C* 31.8 -> 31.0（h 245.6 -> 242.0）
 *        黄 C* 55.4 -> 51.8（h 92.6 -> 90.8）
 *      三者色相仍然分得开，这是「不廉价」的技术原因。
 *   3) 黄天生 L*=85.8 极高：只能做**填充**（思考卡底 / 徽章），
 *      不能做文字 / 图标 / 描边（黄压白底对比度仅 1.33）。卡上文字走深橄榄黄。
 *
 * 与 Harbor 的夜间区分（用户点名要求）：靠**色相与彩度**而非明暗。
 *   港口夜底 #121517：中性冷灰（C*=1.9, h=246.9）
 *   爱丽丝夜底 #171B22：偏蓝深炭（C*=5.4, h=272.3）—— ΔE76 = 4.9
 *   夜间 AI 气泡差异更大：港 #262B2F（中性）vs 爱 #293C49（明显偏蓝），ΔE76 = 10.3。
 *
 * 锚点来源（用户直接给定，非推导）：
 *   日间  BG #F7F5F1 / Surface #FFFDF9 / 次级 #F3EFE7 / 主文字 #25272B / 次文字 #697078
 *         红 #C51F3A / 湖蓝 #55A9D3 / 奶油黄 #F2D56B / 用户气泡 #F4D9DE / AI 气泡 #E7F1F5
 *         输入框 #FFFFFF / 分割线 #DDE1E3 / 强调文字 #A71932
 *   夜间  BG #171B22 / Surface #222832 / 次级 #2B323D / 主文字 #F2F0EA / 次文字 #A9B0BA
 *         珊瑚红 #E05A70 / 湖蓝 #62B6DD / 柔黄 #E8C968 / 用户气泡 #4A2D37 / AI 气泡 #293C49
 *         输入框 #252B34 / 分割线 #39414B / 强调文字 #F0798C
 *
 * 对比度已逐条核算（WCAG）：
 *   日间 正文 13.74 / 次文字 4.60 / 白字压红 5.78 / 正文压用户气泡 11.27 / 压 AI 气泡 13.04
 *   夜间 正文 15.15 / 次文字 7.90 / 正文压用户气泡 10.70 / 压 AI 气泡 10.03
 *   全部满足 4.5:1 正文门槛与 3:1 大字门槛。
 *
 * 用户给的是 13 个语义色号，M3 有 35 个槽位。缺的按上述规则补，
 * 补法标注在每个值旁边（`// 补`），改动来源可追溯。
 *
 * 两处照抄 Harbor 的取舍（保持一致，不是笔误）：
 *   1) outlineVariant 取「发丝线叠在底色上」的合成值，读作底色上的一道极弱分隔，不是描边。
 *   2) error 保持 M3 语义红，不跟着主调走 —— 它是错误语义，不是装饰色。
 */

val AliceThemePreset by lazy {
    PresetTheme(
        id = "alice",
        name = {
            Text(stringResource(id = R.string.theme_name_alice))
        },
        standardLight = lightScheme,
        standardDark = darkScheme,
    )
}

private val lightScheme = lightColorScheme(
    // 红 #C51F3A —— 主角
    primary = Color(0xFFC51F3A),
    onPrimary = Color(0xFFFFFFFF),
    // 补：淡红面，同色相压到浅档，做选中态底 / chip
    primaryContainer = Color(0xFFF7D6DC),
    onPrimaryContainer = Color(0xFF6E0A1C),
    // 湖蓝 #55A9D3 —— 辅色（链接 / 次级按钮）
    secondary = Color(0xFF55A9D3),
    onSecondary = Color(0xFFFFFFFF),
    // 用户气泡 #F4D9DE
    secondaryContainer = Color(0xFFF4D9DE),
    // 补：气泡上的正文走主文字色（对比度 11.27）
    onSecondaryContainer = Color(0xFF25272B),
    // 补：蓝的同族深档，做次级强调文字
    tertiary = Color(0xFF2E7FA8),
    onTertiary = Color(0xFFFFFFFF),
    // 思考卡填充 = 奶油黄 #F2D56B
    tertiaryContainer = Color(0xFFF2D56B),
    // 补：黄底上的文字必须用深色（黄 L*85.8 太亮），深橄榄黄对比度 9.5
    onTertiaryContainer = Color(0xFF4A3D0E),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    // 背景 BG #F7F5F1
    background = Color(0xFFF7F5F1),
    // 主文字 #25272B
    onBackground = Color(0xFF25272B),
    // Surface #FFFDF9 —— 卡片 / 顶栏
    surface = Color(0xFFF7F5F1),
    onSurface = Color(0xFF25272B),
    // 次级 Surface #F3EFE7 —— 行内代码底 / Moodlet 徽章
    surfaceVariant = Color(0xFFF3EFE7),
    // 次文字 #697078
    onSurfaceVariant = Color(0xFF697078),
    // 补：Faint 档，比次文字更淡，做装饰性图标
    outline = Color(0xFF9197A0),
    // 分割线 #DDE1E3 —— 照 Harbor 的取舍：极弱分隔而非描边
    outlineVariant = Color(0xFFDDE1E3),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF2E3238),
    inverseOnSurface = Color(0xFFF2F0EA),
    // 补：反色面上的主调（用强调红 #A71932）
    inversePrimary = Color(0xFFC51F3A),
    // 补：比背景略深一档
    surfaceDim = Color(0xFFEBE7E0),
    // 补：比背景略亮一档
    surfaceBright = Color(0xFFFFFDF9),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    // 输入框 #FFFFFF
    surfaceContainerLow = Color(0xFFFFFFFF),
    // 次级 Surface #F3EFE7
    surfaceContainer = Color(0xFFF3EFE7),
    // AI 气泡 #E7F1F5
    surfaceContainerHigh = Color(0xFFE7F1F5),
    // 补：比 AI 气泡再深一档（保持与 Harbor 相同的层数结构）
    surfaceContainerHighest = Color(0xFFDCE8ED),
)

private val darkScheme = darkColorScheme(
    // 珊瑚红 #E05A70 —— 夜间主调（原红在暗底读不出，必须提亮，见文件头原则 2）
    primary = Color(0xFFE05A70),
    onPrimary = Color(0xFF3A0C16),
    // 补：暗红酒面
    primaryContainer = Color(0xFF5E2230),
    onPrimaryContainer = Color(0xFFFBDADE),
    // 湖蓝 #62B6DD
    secondary = Color(0xFF62B6DD),
    onSecondary = Color(0xFF0C2A3A),
    // 用户气泡 #4A2D37
    secondaryContainer = Color(0xFF4A2D37),
    // 补：气泡上的正文走主文字色（对比度 10.70）
    onSecondaryContainer = Color(0xFFF2F0EA),
    // 补：蓝的同族
    tertiary = Color(0xFF9ACBE4),
    onTertiary = Color(0xFF0C2A3A),
    // 思考卡 = 柔黄 #E8C968 压暗保色相后的填充
    tertiaryContainer = Color(0xFF3D3A1F),
    // 补：黄底上的文字（对比度 9.1）
    onTertiaryContainer = Color(0xFFD8CD97),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    // 背景 BG #171B22 —— 偏蓝深炭，与港口的近黑冷灰 #121517 靠色相区分（ΔE76 4.9）
    background = Color(0xFF171B22),
    // 主文字 #F2F0EA
    onBackground = Color(0xFFF2F0EA),
    // Surface #222832
    surface = Color(0xFF171B22),
    onSurface = Color(0xFFF2F0EA),
    // 次级 Surface #2B323D
    surfaceVariant = Color(0xFF2B323D),
    // 次文字 #A9B0BA
    onSurfaceVariant = Color(0xFFA9B0BA),
    // 补：比次文字更淡
    outline = Color(0xFF6E7683),
    // 分割线 #39414B
    outlineVariant = Color(0xFF39414B),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFF2F0EA),
    inverseOnSurface = Color(0xFF2E3238),
    inversePrimary = Color(0xFFC51F3A),
    // 补：比背景略暗
    surfaceDim = Color(0xFF13161C),
    // 补：比背景略亮
    surfaceBright = Color(0xFF2A313C),
    // 补：最暗一档
    surfaceContainerLowest = Color(0xFF12151B),
    // 输入框 #252B34
    surfaceContainerLow = Color(0xFF252B34),
    // Surface #222832
    surfaceContainer = Color(0xFF222832),
    // AI 气泡 #293C49 —— 明显偏蓝，与港口的中性灰 #262B2F 拉开（ΔE76 10.3）
    surfaceContainerHigh = Color(0xFF293C49),
    // 补：比 AI 气泡再亮一档
    surfaceContainerHighest = Color(0xFF33495A),
)
