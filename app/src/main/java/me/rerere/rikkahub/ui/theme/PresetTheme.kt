/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import me.rerere.rikkahub.ui.theme.presets.ClaudeThemePreset
import me.rerere.rikkahub.ui.theme.presets.MinimalThemePreset
import me.rerere.rikkahub.ui.theme.presets.custom.CreamRoseThemePreset
import me.rerere.rikkahub.ui.theme.presets.custom.HarborThemePreset

data class PresetTheme(
    val id: String,
    val name: @Composable () -> Unit,
    val standardLight: ColorScheme,
    val standardDark: ColorScheme,
) {
    fun getColorScheme(dark: Boolean): ColorScheme {
        return if (dark) standardDark else standardLight
    }
}

// 四个预设。Minimal 放在首位 —— PreferencesStore 与 SettingVM 都用
// PresetThemes[0].id 作为「没存过主题 / 原主题已不存在」时的默认值，
// 它与下面 findPresetTheme 的兜底必须是同一个，否则两处口径会不一致。
val PresetThemes by lazy {
    listOf(
        MinimalThemePreset,
        HarborThemePreset,
        CreamRoseThemePreset,
        ClaudeThemePreset,
    )
}

fun findPresetTheme(id: String): PresetTheme {
    // 旧主题 ID 失效时回落到 minimal。
    return PresetThemes.find { it.id == id } ?: MinimalThemePreset
}
fun findThemeById(id: String, customThemes: List<CustomTheme>): PresetTheme? {
    PresetThemes.find { it.id == id }?.let { return it }
    val custom = customThemes.find { it.id == id } ?: return null
    return PresetTheme(
        id = custom.id,
        name = { androidx.compose.material3.Text(custom.name) },
        standardLight = custom.generateColorScheme(dark = false),
        standardDark = custom.generateColorScheme(dark = true),
    )
}
