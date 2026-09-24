package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import me.rerere.rikkahub.ui.context.LocalSettingsBackgroundActive

@Composable
fun settingsScaffoldContainerColor(
    fallback: Color = MaterialTheme.colorScheme.background,
): Color = if (LocalSettingsBackgroundActive.current) Color.Transparent else fallback