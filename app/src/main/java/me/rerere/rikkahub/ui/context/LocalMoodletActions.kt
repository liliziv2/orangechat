package me.rerere.rikkahub.ui.context

import androidx.compose.runtime.compositionLocalOf

/**
 * Actions for moodlet badge interactions (real favorite + triple-like bonus reply).
 */
data class MoodletActions(
    val isFavorited: suspend (moodKey: String) -> Boolean = { false },
    val setFavorited: suspend (moodKey: String, favorited: Boolean, label: String, reason: String) -> Unit = { _, _, _, _ -> },
    val onTripleLike: (moodKey: String, label: String, reason: String) -> Unit = { _, _, _ -> },
)

val LocalMoodletActions = compositionLocalOf { MoodletActions() }
