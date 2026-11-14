package me.rerere.rikkahub.ui.theme.presets

import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.theme.PresetTheme

/**
 * 极简中性：纯白 / 近黑底，中性灰表面层级，细淡边框，低饱和蓝作强调色，
 * 少量琥珀作点缀色。
 *
 * 色号取自 RikkaHub 当前的 Minimal 色板，Light / Dark 各一套。只取色值 ——
 * 上游的主题代码结构与布局一律未复制，本文件仍走 OrangeChat 自己的
 * PresetTheme 架构（standardLight / standardDark）。
 *
 * 这是预设列表里的兜底项：主题 ID 失效时回落到 minimal。
 */
val MinimalThemePreset by lazy {
    PresetTheme(
        id = "minimal",
        name = { Text(stringResource(R.string.theme_name_minimal)) },
        standardLight = lightScheme,
        standardDark = darkScheme,
    )
}

// ── Light ──────────────────────────────────────────────────────────────
private val primaryLight = Color(0xFF204D91)
private val onPrimaryLight = Color(0xFFFFFFFF)
private val primaryContainerLight = Color(0xFFDCE6F5)
private val onPrimaryContainerLight = Color(0xFF123763)
private val secondaryLight = Color(0xFF5A6675)
private val onSecondaryLight = Color(0xFFFFFFFF)
private val secondaryContainerLight = Color(0xFFE5EBF2)
private val onSecondaryContainerLight = Color(0xFF33404F)
private val tertiaryLight = Color(0xFFB29A51)
private val onTertiaryLight = Color(0xFFFFFFFF)
private val tertiaryContainerLight = Color(0xFFF3EACB)
private val onTertiaryContainerLight = Color(0xFF6A5B22)
private val errorLight = Color(0xFFBA1A1A)
private val onErrorLight = Color(0xFFFFFFFF)
private val errorContainerLight = Color(0xFFFFDAD6)
private val onErrorContainerLight = Color(0xFF93000A)
private val backgroundLight = Color(0xFFFAF7EB)
private val onBackgroundLight = Color(0xFF243B60)
private val surfaceLight = Color(0xFFFAF7EB)
private val onSurfaceLight = Color(0xFF243B60)
private val surfaceVariantLight = Color(0xFFECE9DC)
private val onSurfaceVariantLight = Color(0xFF5F6570)
private val outlineLight = Color(0xFFC8C2AE)
private val outlineVariantLight = Color(0xFFE8E2D0)
private val scrimLight = Color(0xFF000000)
private val inverseSurfaceLight = Color(0xFF243B60)
private val inverseOnSurfaceLight = Color(0xFFFAF7EB)
private val inversePrimaryLight = Color(0xFFA8C7FA)
private val surfaceDimLight = Color(0xFFEFEBDB)
private val surfaceBrightLight = Color(0xFFFFFDF5)
private val surfaceContainerLowestLight = Color(0xFFFFFFFF)
private val surfaceContainerLowLight = Color(0xFFFFFDF5)
private val surfaceContainerLight = Color(0xFFF7F3E5)
private val surfaceContainerHighLight = Color(0xFFFFFDF5)
private val surfaceContainerHighestLight = Color(0xFFEDE9DA)

// ── Dark ───────────────────────────────────────────────────────────────
private val primaryDark = Color(0xFFA8C7FA)
private val onPrimaryDark = Color(0xFF0E2A4F)
private val primaryContainerDark = Color(0xFF2D405D)
private val onPrimaryContainerDark = Color(0xFFD8E4FA)
private val secondaryDark = Color(0xFFC0C6D0)
private val onSecondaryDark = Color(0xFF2A333F)
private val secondaryContainerDark = Color(0xFF2D405D)
private val onSecondaryContainerDark = Color(0xFFDCE3EC)
private val tertiaryDark = Color(0xFFC7B579)
private val onTertiaryDark = Color(0xFF3A2F0B)
private val tertiaryContainerDark = Color(0xFF544A24)
private val onTertiaryContainerDark = Color(0xFFF3EACB)
private val errorDark = Color(0xFFFFB4AB)
private val onErrorDark = Color(0xFF690005)
private val errorContainerDark = Color(0xFF93000A)
private val onErrorContainerDark = Color(0xFFFFDAD6)
private val backgroundDark = Color(0xFF151E2D)
private val onBackgroundDark = Color(0xFFEEEBDD)
private val surfaceDark = Color(0xFF151E2D)
private val onSurfaceDark = Color(0xFFEEEBDD)
private val surfaceVariantDark = Color(0xFF37414F)
private val onSurfaceVariantDark = Color(0xFFB9C2CE)
private val outlineDark = Color(0xFF5A6675)
private val outlineVariantDark = Color(0xFF33404F)
private val scrimDark = Color(0xFF000000)
private val inverseSurfaceDark = Color(0xFFEEEBDD)
private val inverseOnSurfaceDark = Color(0xFF1E2836)
private val inversePrimaryDark = Color(0xFF204D91)
private val surfaceDimDark = Color(0xFF131B28)
private val surfaceBrightDark = Color(0xFF2A3648)
private val surfaceContainerLowestDark = Color(0xFF101825)
private val surfaceContainerLowDark = Color(0xFF1A2433)
private val surfaceContainerDark = Color(0xFF1B2534)
private val surfaceContainerHighDark = Color(0xFF202C3D)
private val surfaceContainerHighestDark = Color(0xFF283444)

private val lightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

private val darkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)
