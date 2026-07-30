/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.LocalOverscrollFactory
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import dynamiccolor.ColorSpecs
import dynamiccolor.DynamicScheme
import dynamiccolor.Variant
import hct.Hct
import kotlinx.serialization.Serializable
import me.rerere.material3.toColorScheme
import me.rerere.rikkahub.data.datastore.CustomThemeColors
import me.rerere.rikkahub.ui.components.ui.toComposeColor
import me.rerere.rikkahub.ui.hooks.rememberAmoledDarkMode
import palettes.TonalPalette
import me.rerere.rikkahub.ui.hooks.rememberColorMode
import me.rerere.rikkahub.ui.hooks.rememberUserSettingsState

private val ExtendLightColors = lightExtendColors()
private val ExtendDarkColors = darkExtendColors()
val LocalExtendColors = compositionLocalOf { ExtendLightColors }

val LocalDarkMode = compositionLocalOf { false }

private val AMOLED_DARK_BACKGROUND = Color(0xFF000000)

@Serializable
enum class ColorMode {
    SYSTEM,
    LIGHT
}

@Composable
fun RikkahubTheme(
    content: @Composable () -> Unit
) {
    val settings by rememberUserSettingsState()

    val colorMode by rememberColorMode()
    val darkTheme = when (colorMode) {
        ColorMode.SYSTEM -> false
        ColorMode.LIGHT -> false
    }
    val amoledDarkMode by rememberAmoledDarkMode()

    val colorScheme = when {
        settings.dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> {
            val themeId = if (darkTheme) (settings.darkThemeId ?: settings.themeId) else settings.themeId
            val theme = findThemeById(themeId, settings.customThemes)
                ?: findPresetTheme(themeId)
            val baseScheme = theme.getColorScheme(dark = darkTheme)
            val customColors = if (darkTheme) settings.nightCustomColors else settings.dayCustomColors
            if (customColors?.hasAny() == true) {
                applyCustomColors(baseScheme, customColors, darkTheme)
            } else {
                baseScheme
            }
        }
    }
    val colorSchemeConverted = remember(darkTheme, amoledDarkMode, colorScheme) {
        if (darkTheme && amoledDarkMode) {
            colorScheme.copy(
                background = AMOLED_DARK_BACKGROUND,
                surface = AMOLED_DARK_BACKGROUND,
            )
        } else {
            colorScheme
        }
    }
    val extendColors = if (darkTheme) ExtendDarkColors else ExtendLightColors

    // 颜色自定义覆盖
    val finalColorScheme = remember(
        colorSchemeConverted,
        settings.displaySetting.primaryColor,
        settings.displaySetting.globalTextColor,
        settings.themeId,
        settings.darkThemeId,
        settings.dayCustomColors,
        settings.nightCustomColors,
        darkTheme,
    ) {
        var scheme = colorSchemeConverted
        settings.displaySetting.primaryColor?.let { pc ->
            val primaryColor = pc.toComposeColor()
            val luminance = 0.299f * primaryColor.red + 0.587f * primaryColor.green + 0.114f * primaryColor.blue
            val onPrimary = if (luminance > 0.5f) Color.Black else Color.White
            scheme = scheme.copy(
                primary = primaryColor,
                onPrimary = onPrimary,
            )
        }
        settings.displaySetting.globalTextColor?.let { gtc ->
            val textColor = gtc.toComposeColor()
            scheme = scheme.copy(
                onBackground = textColor,
                onSurface = textColor,
                onSurfaceVariant = textColor,
            )
        }
        val effectiveThemeId = if (darkTheme) (settings.darkThemeId ?: settings.themeId) else settings.themeId
        if (effectiveThemeId == "pearltide") {
            // 珍珠潮汐主题专属:统一让默认读取这几个 token 的容器变透明/半透明,
            // 这样 Scaffold、Card、TopAppBar、ModalDrawerSheet、ModalBottomSheet 等
            // 全部自动生效,不需要逐个页面单独改。
            // 这个 if 分支只在 themeId 精确等于 "pearltide" 时才会执行,
            // 其余六个官方预设(id 分别是 ocean/sakura/spring/autumn/black/claude)
            // 以及用户自定义主题(id 是随机 UUID,不会等于这个字符串)完全不受影响。
            val glassAlpha = 0.6f
            scheme = scheme.copy(
                background = scheme.background.copy(alpha = 0f),
                surface = scheme.surface.copy(alpha = glassAlpha),
                surfaceBright = scheme.surfaceBright.copy(alpha = glassAlpha),
                surfaceDim = scheme.surfaceDim.copy(alpha = glassAlpha),
                surfaceContainerLowest = scheme.surfaceContainerLowest.copy(alpha = glassAlpha),
                surfaceContainerLow = scheme.surfaceContainerLow.copy(alpha = glassAlpha),
                surfaceContainer = scheme.surfaceContainer.copy(alpha = glassAlpha),
                surfaceContainerHigh = scheme.surfaceContainerHigh.copy(alpha = glassAlpha),
                surfaceContainerHighest = scheme.surfaceContainerHighest.copy(alpha = glassAlpha),
                surfaceVariant = scheme.surfaceVariant.copy(alpha = glassAlpha),
            )
        }
        scheme
    }

    // 更新状态栏图标颜色
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalDarkMode provides darkTheme,
        LocalExtendColors provides extendColors,
        LocalOverscrollFactory provides null
    ) {
        MaterialExpressiveTheme(
            colorScheme = finalColorScheme,
            typography = Typography,
            content = content,
            motionScheme = MotionScheme.expressive()
        )
    }
}

private fun applyCustomColors(base: ColorScheme, colors: CustomThemeColors, dark: Boolean): ColorScheme {
    val primaryArgb = colors.primaryColorArgb
    val secondaryArgb = colors.secondaryColorArgb
    val tertiaryArgb = colors.tertiaryColorArgb
    if (primaryArgb == null && secondaryArgb == null && tertiaryArgb == null) return base
    val sourceHct = Hct.fromInt((primaryArgb ?: 0xFF6750A4L).toInt())
    val specVersion = DynamicScheme.DEFAULT_SPEC_VERSION
    val platform = DynamicScheme.DEFAULT_PLATFORM
    val contrastLevel = 0.0
    val colorSpec = ColorSpecs.get(specVersion)

    val primaryPalette = colorSpec.getPrimaryPalette(
        Variant.TONAL_SPOT, sourceHct, dark, platform, contrastLevel,
    )
    val secondaryPalette = if (secondaryArgb != null) {
        TonalPalette.fromInt(secondaryArgb.toInt())
    } else {
        colorSpec.getSecondaryPalette(
            Variant.TONAL_SPOT, sourceHct, dark, platform, contrastLevel,
        )
    }
    val tertiaryPalette = if (tertiaryArgb != null) {
        TonalPalette.fromInt(tertiaryArgb.toInt())
    } else {
        colorSpec.getTertiaryPalette(
            Variant.TONAL_SPOT, sourceHct, dark, platform, contrastLevel,
        )
    }

    val scheme = DynamicScheme(
        sourceHct, Variant.TONAL_SPOT, dark, contrastLevel, platform, specVersion,
        primaryPalette, secondaryPalette, tertiaryPalette,
        colorSpec.getNeutralPalette(Variant.TONAL_SPOT, sourceHct, dark, platform, contrastLevel),
        colorSpec.getNeutralVariantPalette(Variant.TONAL_SPOT, sourceHct, dark, platform, contrastLevel),
        colorSpec.getErrorPalette(Variant.TONAL_SPOT, sourceHct, dark, platform, contrastLevel),
    )
    return scheme.toColorScheme()
}

val MaterialTheme.extendColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendColors.current