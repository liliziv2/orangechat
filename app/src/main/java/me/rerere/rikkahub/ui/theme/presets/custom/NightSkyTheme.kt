package me.rerere.rikkahub.ui.theme.presets.custom

import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.theme.PresetTheme

/*
 * 夜游 Night Stroll —— 城市夜色的两态：日间是纸感暖白 + 深绿，夜间是蓝黑 + 玻璃绿。
 * 一个主题两态，跟随橘瓣的深浅切换（ColorMode.SYSTEM/LIGHT/DARK）。
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 色源：用户 2026-10-05 给出的 12 个锚点色
 * ─────────────────────────────────────────────────────────────────────────
 *   暖黄（灯光）  灯带黄 #E6D67A  |  月光黄 #F1D37A
 *   绿（玻璃）    玻璃绿 #1E4F49  |  夜玻璃绿 #82D0B4
 *   街角（最暗）  街角黑 #071516  |  夜街角 #10192D
 *   纸（最亮）    纸白   #F1E7B0  |  夜纸白 #E7EDF5
 *   砖红          砖墙红 #8D3F25  |  暗砖红 #B74A33
 *   蓝黑（影）    蓝黑影 #0B2C32  |  夜蓝黑 #1B2A42
 *
 * 「纸」与「街角」是同一对的两端：亮色模式下纸 = 页面、街角 = 文字；暗色模式反过来。
 * 名字描述的是**相对明度**，不是绝对角色（实测 日间纸白 L* 约 91 / 街角黑约 6，
 * 夜间夜街角约 10 / 夜纸白约 93）。
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 用户 2026-10-05 追加的两条约束
 * ─────────────────────────────────────────────────────────────────────────
 *   「配色更加护眼一点 但是不要拉低对比度」
 *   「主题参考 echo 那个仓库 底图跟我们原有的主题 奶油玫瑰跟港口一样」
 *
 * ── 护眼：只压大面积彩度，L* 一点不动 ──
 * 刺眼的根因不是亮度，是**大面积彩度**。12 个锚点直接铺成整屏时页面 C* 到了
 * 28.5（日）/ 15.1（夜），而参照系是 Echo 的 1.4~2.1（它连 accent 才 11.4）、
 * 仓库已有的港口 1.7~1.9 / 奶油玫瑰 0.8~3.6 —— 超了一个数量级，读作「整屏黄滤镜」
 * 和「整屏蓝紫滤镜」。
 *
 * 软化 = 保留 L* 与色相，只把 C* 压到目标（日 5.0 / 夜 3.5）；夜间同时把页面
 * L* 从 8.95 提到 12.0（近黑 + 亮字最伤眼）。**对比度由 L* 决定，所以压彩度
 * 完全不动对比度** —— 这是「护眼」与「不拉低对比度」能同时成立的原因。
 * 色彩全部留给小面积：用户气泡 / 主色 / 强调色 / 次级文字仍是锚点原色。
 *
 * ── 底图：与奶油玫瑰、港口共用同一张 harbor_chat_bg.webp ──
 * 本主题进了 GLASS_BACKGROUND_THEMES（Theme.kt），根 background 的 alpha 被置 0，
 * 页面真实颜色 = RouteActivity 的「底图 + 三段 scrim」合成。
 *
 * 底图极亮：863x1822，平均 RGB 240/235/232 = **L* 93.6**（暖白纸纹）。
 * ⇒ 硬约束 `合成 = a x scrim + (1-a) x 底图`：夜间要 L* 13.6 就得 a >= 0.975。
 * **「深色页面」与「壁纸清晰可见」数学上互斥**，夜间只能选前者。
 *
 * scrim 色号两个分支都取「页面软化色」本身，只靠 alpha 做斜坡，不靠换色号
 * （奶油玫瑰踩过的「整屏棕滤镜」）。日间特意**不用**纸白锚点 #F1E7B0 ——
 * 它是 C*=28.5 的暖黄，任何够用的 alpha 都会把整屏染成黄色（实测 C*=7.3，
 * 超奶油玫瑰 2.8 的 2.6 倍）；换成页面软化色 #E8E6DC 后 C*=2.9，与奶油玫瑰持平。
 * 像素级实测（st/bg_measure.py，逐行按 alpha 合成）：
 *
 *   日间  #E8E6DC @ 0.20/0.12/0.34 -> L* 93.2  C* 2.9
 *   夜间  #1E1F24 @ 0.975/0.982/0.990 -> L* 13.6  C* 3.5
 *   对照  奶油玫瑰 日 L* 93.6 C* 2.8 / 夜 L* 20.4 C* 0.9
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 写法沿用仓库已有的「港口」主题（HarborTheme.kt）
 * ─────────────────────────────────────────────────────────────────────────
 *   1) 先定命名锚点，再按「聊天界面实际读哪个槽位」落 M3，不按槽位名字对齐；
 *   2) 锚点没覆盖的槽位（surfaceBright / surfaceDim / surfaceContainerLowest /
 *      surfaceContainerHighest / inverse* / primaryContainer / on* 各色）在同族内推导；
 *   3) Dark 不是 Light 降亮度推出来的，是按同一套关系独立给一套；
 *   4) error 保持 M3 语义红，不跟着主题色走；
 *   5) outlineVariant 取「发丝线叠在底色上」的合成值，读作分隔而不是描边。
 *
 * ─────────────────────────────────────────────────────────────────────────
 * 槽位落点（按聊天界面「实际读哪个槽位」对齐）
 * ─────────────────────────────────────────────────────────────────────────
 *   页面                -> background / surface          （日 纸白·软化 / 夜 夜街角·软化）
 *   卡片 / 顶栏 / pill   -> surfaceContainer              （日 推导 / 夜 夜蓝黑·软化）
 *   GLASS 输入框        -> surfaceContainerLow           （推导）
 *   AI 气泡 / FLAT 输入框 -> surfaceContainerHigh         （推导）
 *   用户气泡            -> secondaryContainer            （日 灯带黄 / 夜 暗砖红）
 *   思考卡              -> tertiaryContainer             （推导，低饱和）
 *   主色                -> primary                       （玻璃绿 / 夜玻璃绿）
 *   强调（小图标 / 徽章）-> tertiary                      （日 推导 / 夜 月光黄）
 *   次级强调            -> secondary                     （砖墙红 / 暗砖红提亮版）
 *   正文 / 次级文字      -> onSurface / onSurfaceVariant  （日 街角黑 / 蓝黑影）
 *   描边                -> outline / outlineVariant      （推导）
 *
 * 三处「锚点原色不能直接落槽」的推导，都按 HarborTheme.kt 的同一条规矩办
 * （Dark 独立于 Light，锚点按实际读哪个槽位落，不按槽位名字对齐）：
 *
 *   1) 暖黄（灯带黄 / 月光黄）日间是**浅色**（L* 约 85），当不了 `tertiary` 的前景 ——
 *      那个槽位读的是小图标 / 进度 / 徽章，压在浅色页面上必须够深。
 *      ⇒ 日间：灯带黄落 `secondaryContainer`（用户气泡，浅底可用），
 *        `tertiary` 用它的压暗版 #79610D（同色相，L* 43，over 页面 4.8）；
 *        夜间：月光黄是亮色，直接落 `tertiary`。
 *   2) 夜间「暗砖红 #B74A33」L* 约 46，**当背景够、当前景不够** ——
 *      `secondary` 在本仓读的是 MoodletBadge 前景 / Mermaid，落在 AI 气泡上，
 *      原色只有 2.7:1。⇒ `secondary` 用同色相提亮版 #F47E62（L* 66，over AI 气泡 4.9），
 *      原色 #B74A33 落 `secondaryContainer`（用户气泡，白字 5.2:1）。
 *      （Harbor 的 Dark 也是这么办的：light `secondary` #5E6B78 -> dark #B4BEC6。）
 *   3) 大面积锚点（纸白 / 夜街角 / 夜蓝黑）**按护眼要求软化**：保留 L* 与色相，
 *      只把 C* 压到目标。页面与各表面用软化值，锚点原色仍留在小面积槽位上
 *      （纸白 -> onPrimary / onSecondary / onTertiary / inverseOnSurface；
 *       夜街角 -> inverseOnSurface；夜蓝黑 -> 由 surfaceContainer 的软化版承接）。
 *
 * 9 个 surface* 槽位仍会被 Theme.kt 用 interfaceSurfaceOpacity（默认 0.82）覆盖 alpha，
 * 合成值 = 名义色@0.82 over 页面。
 * ⚠️ 本主题**在** GLASS_BACKGROUND_THEMES 里（harbor / creamrose / nightsky 三个共用
 * 同一张 harbor_chat_bg.webp），background 的 alpha 被置 0，页面由底图 + scrim 决定。
 * 思考卡仍在 THEME_THINKING_CONTAINER_THEMES 里（tertiaryContainer 有配色），
 * 但**已移出 THEME_THINKING_SURFACE_THEMES** —— 不再额外画一层底色。
 *
 * 主题 id 仍是 `nightsky`（PreferencesStore 存的是 id，改了老用户会静默回落到 Minimal）；
 * 显示名走 R.string.theme_name_nightsky = 夜游 / Night Stroll。
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
    // 玻璃绿
    primary = Color(0xFF1E4F49),
    // 纸白
    onPrimary = Color(0xFFF1E7B0),
    primaryContainer = Color(0xFFD0D4D3),
    // 街角黑
    onPrimaryContainer = Color(0xFF071516),
    // 砖墙红
    secondary = Color(0xFF8D3F25),
    // 纸白
    onSecondary = Color(0xFFF1E7B0),
    // 灯带黄
    secondaryContainer = Color(0xFFE6D67A),
    // 蓝黑影
    onSecondaryContainer = Color(0xFF0B2C32),
    tertiary = Color(0xFF79610D),
    // 纸白
    onTertiary = Color(0xFFF1E7B0),
    tertiaryContainer = Color(0xFFE4E2D7),
    // 街角黑
    onTertiaryContainer = Color(0xFF071516),
    // M3 语义红
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    // 纸白·软化
    background = Color(0xFFE8E6DC),
    // 街角黑
    onBackground = Color(0xFF071516),
    // 纸白·软化
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
 * ── 批 71：Night 语义色重映射（用户 2026-10-06）──────────────────────────────
 * 「Night 不再沿用当前灰黑 Surface 的视觉关系，按『深靛蓝 + 蓝灰 + 暗砖红 +
 *   玻璃绿 + 月光黄』重新映射整个 Chat 页面，不改结构，只改语义色。背景图保持可见。」
 *
 * 诊断：原 surface 家族色相其实已经是 ~278~285°（蓝紫），但**彩度只有 2.3~4.7**
 * ⇒ 屏幕上读作灰黑。两个深色锚点 夜街角 #10192D(C* 15.08) / 夜蓝黑 #1B2A42(C* 17.21)
 * 是高彩度靛蓝。
 *
 * 改法：**保留每个槽位的 L*（层级关系一字不动）**，色相统一 280°，彩度按深浅重分配 ——
 *   深槽位（surfaceDim / Lowest / background / surface）→ 深靛蓝 C* 15.2 / 14.8
 *   中槽位（…ContainerLow → …ContainerHighest / Variant / Bright）→ 蓝灰 C* 12.8 → 7.4
 *   浅槽位（outline / primaryContainer）→ 蓝灰 C* 7.0
 * 强调色与文字色**一字未动**：primary #82D0B4（玻璃绿）· secondary #F47E62 /
 * secondaryContainer #B74A33（暗砖红）· tertiary #F1D37A（月光黄）· 夜纸白 #E7EDF5 ·
 * onSurfaceVariant #A9B8CC（本来就是蓝灰）· error 家族（M3 标准）。
 *
 * 生成器 st/theme_b71.py（含 L* / C* 实测与越界自检）；牙齿 st/teeth_b71.py。
 * ⚠️ 9 个 surface* 槽位仍被 Theme.kt 乘 interfaceSurfaceOpacity(0.82)，名义色 ≠ 屏幕色。
 */

private val darkScheme = darkColorScheme(
    // 夜玻璃绿
    primary = Color(0xFF82D0B4),
    onPrimary = Color(0xFF0B2A22),
    primaryContainer = Color(0xFF4B4F5A),
    onPrimaryContainer = Color(0xFFCFE9DC),
    // 暗砖红·提亮
    secondary = Color(0xFFF47E62),
    onSecondary = Color(0xFF1B0E08),
    // 暗砖红
    secondaryContainer = Color(0xFFB74A33),
    onSecondaryContainer = Color(0xFFFFFFFF),
    // 月光黄
    tertiary = Color(0xFFF1D37A),
    onTertiary = Color(0xFF2A2210),
    tertiaryContainer = Color(0xFF363A46),
    // 夜纸白
    onTertiaryContainer = Color(0xFFE7EDF5),
    // M3 语义红
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    // 深靛蓝（夜街角同族，批 71 提彩度）
    background = Color(0xFF151F33),
    // 夜纸白
    onBackground = Color(0xFFE7EDF5),
    // 深靛蓝（夜街角同族，批 71 提彩度）
    surface = Color(0xFF151F33),
    // 夜纸白
    onSurface = Color(0xFFE7EDF5),
    surfaceVariant = Color(0xFF363A46),
    onSurfaceVariant = Color(0xFFA9B8CC),
    outline = Color(0xFF808490),
    outlineVariant = Color(0xFF333743),
    // M3 标准
    scrim = Color(0xFF000000),
    // 夜纸白
    inverseSurface = Color(0xFFE7EDF5),
    // 夜街角
    inverseOnSurface = Color(0xFF10192D),
    // 玻璃绿
    inversePrimary = Color(0xFF1E4F49),
    surfaceDim = Color(0xFF0E192D),
    surfaceBright = Color(0xFF393D48),
    surfaceContainerLowest = Color(0xFF0E192D),
    surfaceContainerLow = Color(0xFF202739),
    // 蓝灰（夜蓝黑同族，批 71 提彩度）
    surfaceContainer = Color(0xFF2A303F),
    surfaceContainerHigh = Color(0xFF323642),
    surfaceContainerHighest = Color(0xFF393D48),
)
