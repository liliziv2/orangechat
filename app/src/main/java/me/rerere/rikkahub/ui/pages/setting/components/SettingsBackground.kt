package me.rerere.rikkahub.ui.pages.setting.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage
import java.io.File
import me.rerere.rikkahub.ui.context.LocalDisplaySettings
import me.rerere.rikkahub.ui.context.LocalSettingsBackgroundActive
import me.rerere.rikkahub.ui.theme.CustomColors

@Composable
fun SettingsBackground(content: @Composable () -> Unit) {
    val path = LocalDisplaySettings.current.settingsBackgroundPath
    val backgroundFile = remember(path) {
        path.takeIf { it.isNotBlank() }
            ?.let(::File)
            ?.takeIf { it.isFile }
    }

    if (backgroundFile == null) {
        content()
        return
    }

    val containerColor = CustomColors.topBarColors.containerColor
    var imageLoaded by remember(backgroundFile) { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AsyncImage(
            model = backgroundFile,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            onSuccess = { imageLoaded = true },
            onError = { imageLoaded = false },
        )
        if (imageLoaded) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(containerColor.copy(alpha = 0.36f))
            )
        }
        CompositionLocalProvider(LocalSettingsBackgroundActive provides true) {
            content()
        }
    }
}