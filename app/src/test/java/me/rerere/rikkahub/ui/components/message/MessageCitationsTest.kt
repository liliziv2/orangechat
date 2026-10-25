package me.rerere.rikkahub.ui.components.message

import me.rerere.ai.ui.UIMessageAnnotation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageCitationsTest {
    @Test fun audioMetadataDoesNotCreateReferences() {
        assertTrue(visibleMessageCitations(listOf(
            UIMessageAnnotation.ChatVoiceReply(emptyList()),
            UIMessageAnnotation.TtsAudio("text", "text", "file:///audio.wav", "wav"),
            UIMessageAnnotation.VoiceCallRecord("call", 10),
        )).isEmpty())
    }

    @Test fun unusableLinksDoNotCreatePlaceholders() {
        val annotations = listOf("", "  ", "not a url", "javascript:alert(1)", "https://")
            .map { UIMessageAnnotation.UrlCitation("title", it) }
        assertTrue(visibleMessageCitations(annotations).isEmpty())
    }

    @Test fun mixedAnnotationsCountOnlyRealSources() {
        val source = UIMessageAnnotation.UrlCitation("Source", "https://example.com/page")
        assertEquals(listOf(source), visibleMessageCitations(listOf(
            UIMessageAnnotation.ChatVoiceReply(emptyList()), source,
            UIMessageAnnotation.UrlCitation("", ""),
        )))
    }

    @Test fun missingTitleFallsBackToUrl() {
        val citation = visibleMessageCitations(listOf(
            UIMessageAnnotation.UrlCitation("  ", " https://example.com/page "),
        )).single()
        assertEquals("https://example.com/page", citation.title)
        assertEquals("https://example.com/page", citation.url)
    }
}
