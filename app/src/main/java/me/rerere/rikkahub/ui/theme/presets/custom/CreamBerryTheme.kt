package me.rerere.rikkahub.ui.theme.presets.custom

import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.theme.PresetTheme

/**
 * 奶油莓 Cream Berry
 *
 * 取色参照图：奶油黄底、浅蓝气泡、莓红强调。用量刻意不均 ——
 * 奶油黄承担大面积页面，浅蓝只落在自己的气泡上，莓红只给选中态、发送键这种小面积位置。
 *
 * 日夜是同一套关系镜像，不是两套配色：
 *   日间：页面最亮（奶油黄），卡片/输入框略提亮成暖白，气泡靠色相区分。
 *   夜间：页面压成暖深灰，卡片提亮一档，浅蓝褪成灰蓝，莓红提亮成莓粉，
 *         否则深底上原色会糊成一团看不出来。
 *
 * 文字在日间是深棕灰而不是纯黑 —— 奶油底配纯黑会显得脏，暖调文字更贴。
 */
val CreamBerryThemePreset by lazy {
    PresetTheme(
        id = "creamberry",
        name = { Text(stringResource(R.string.theme_name_creamberry)) },
        standardLight = creamBerryLight,
        standardDark = creamBerryDark,
    )
}

private val creamBerryLight = lightColorScheme(
    // 莓红：选中态、发送键
    primary = Color(0xFFD90A46),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9E0),
    onPrimaryContainer = Color(0xFF7A0025),
    // 浅蓝：自己的气泡底色
    secondary = Color(0xFF3C6B77),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCDEEF4),
    onSecondaryContainer = Color(0xFF1D4750),
    // 暖棕：轻点缀，不抢莓红
    tertiary = Color(0xFF8C6A3F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF7E3C8),
    onTertiaryContainer = Color(0xFF5A3E1C),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF93000A),
    // 奶油黄页面
    background = Color(0xFFFFF7DC),
    onBackground = Color(0xFF44383D),
    surface = Color(0xFFFFF7DC),
    onSurface = Color(0xFF44383D),
    surfaceVariant = Color(0xFFF2E7CB),
    onSurfaceVariant = Color(0xFF6F5F62),
    outline = Color(0xFFC9B996),
    outlineVariant = Color(0xFFEADFC2),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFF44383D),
    inverseOnSurface = Color(0xFFFFF7DC),
    inversePrimary = Color(0xFFFFB1C1),
    surfaceDim = Color(0xFFEFE3C4),
    surfaceBright = Color(0xFFFFFDF6),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    // 输入框
    surfaceContainerLow = Color(0xFFFFFCF1),
    // 卡片 / 顶栏
    surfaceContainer = Color(0xFFFFF9E8),
    // AI 气泡：比页面亮一档的暖白
    surfaceContainerHigh = Color(0xFFFFFBEE),
    surfaceContainerHighest = Color(0xFFF5EAD3),
)

private val creamBerryDark = darkColorScheme(
    // 莓粉：深底上用原莓红会发闷
    primary = Color(0xFFF27D9D),
    onPrimary = Color(0xFF5C0020),
    primaryContainer = Color(0xFF8E1136),
    onPrimaryContainer = Color(0xFFFFD9E0),
    secondary = Color(0xFFA7D3DE),
    onSecondary = Color(0xFF00363F),
    // 灰蓝：浅蓝在夜间收掉彩度
    secondaryContainer = Color(0xFF344B55),
    onSecondaryContainer = Color(0xFFCDEEF4),
    tertiary = Color(0xFFDCC39B),
    onTertiary = Color(0xFF3F2E14),
    tertiaryContainer = Color(0xFF57422A),
    onTertiaryContainer = Color(0xFFF7E3C8),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    // 暖深灰页面
    background = Color(0xFF242126),
    onBackground = Color(0xFFF3E9DC),
    surface = Color(0xFF242126),
    onSurface = Color(0xFFF3E9DC),
    surfaceVariant = Color(0xFF453D42),
    onSurfaceVariant = Color(0xFFD6C6C2),
    outline = Color(0xFF6E6166),
    outlineVariant = Color(0xFF443A3F),
    scrim = Color(0xFF000000),
    inverseSurface = Color(0xFFF3E9DC),
    inverseOnSurface = Color(0xFF332A2E),
    inversePrimary = Color(0xFFD90A46),
    surfaceDim = Color(0xFF1F1C21),
    surfaceBright = Color(0xFF3A343A),
    surfaceContainerLowest = Color(0xFF1E1B20),
    surfaceContainerLow = Color(0xFF2C2930),
    surfaceContainer = Color(0xFF2B272D),
    surfaceContainerHigh = Color(0xFF302B31),
    surfaceContainerHighest = Color(0xFF383139),
)