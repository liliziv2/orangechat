package me.rerere.rikkahub.ui.components.message

import java.net.URI
import me.rerere.ai.ui.UIMessageAnnotation

/** Audio metadata is not a web reference. Count and render only usable web sources. */
internal fun visibleMessageCitations(
    annotations: List<UIMessageAnnotation>,
): List<UIMessageAnnotation.UrlCitation> = annotations
    .filterIsInstance<UIMessageAnnotation.UrlCitation>()
    .mapNotNull { citation ->
        val url = citation.url.trim()
        val uri = runCatching { URI(url) }.getOrNull() ?: return@mapNotNull null
        if ((!uri.scheme.equals("https", ignoreCase = true) &&
                !uri.scheme.equals("http", ignoreCase = true)) || uri.host.isNullOrBlank()) {
            return@mapNotNull null
        }
        citation.copy(url = url, title = citation.title.trim().ifBlank { url })
    }
