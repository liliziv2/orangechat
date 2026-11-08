package me.rerere.rikkahub.data.service

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class ExternalMessageUploadTest {
    @Test
    fun `durable intent round trips without credentials or content loss`() {
        val upload = ExternalMessageUpload(
            configId = "config", url = "https://example.supabase.co", table = "chat_messages",
            messageId = "6cec10d1-3353-4559-a646-9b9b7d96e659", assistantId = "assistant",
            conversationId = "conversation", role = "user", content = "中文\n\"quoted\" 😀",
            createdAt = "2026-11-08T02:00:00Z",
        )
        val encoded = Json.encodeToString(upload)
        assertEquals(upload, Json.decodeFromString<ExternalMessageUpload>(encoded))
        assertFalse(encoded.contains("supabaseKey"))
        assertFalse(encoded.contains("Authorization"))
    }

    @Test
    fun `retry preserves original identity and timestamp`() {
        val upload = ExternalMessageUpload("config", "url", "table", "message", "a", "c", "assistant", "reply", "time")
        val recovered = Json.decodeFromString<ExternalMessageUpload>(Json.encodeToString(upload))
        assertEquals(upload.messageId, recovered.messageId)
        assertEquals(upload.createdAt, recovered.createdAt)
        assertNotEquals(upload.messageId, recovered.copy(messageId = "regenerated-message").messageId)
    }
}