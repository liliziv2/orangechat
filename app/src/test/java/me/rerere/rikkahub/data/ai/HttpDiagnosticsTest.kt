package me.rerere.rikkahub.data.ai

import me.rerere.ai.util.HttpDiagnostics
import me.rerere.ai.util.joinUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 「服务器返回 HTML 而不是 JSON」这一类错误的回归测试。
 *
 * 背景：网关 / CDN（Cloudflare 等）拦截请求时会返回 HTML 挑战页。修复前，调用方会把这个
 * HTML 直接交给 kotlinx.serialization 解析，最终抛给用户的是 kotlinx 的原始解析异常
 * （Unexpected JSON token ... JSON input: DOCTYPE html），真实的状态码 / Content-Type /
 * 最终 URL 全部被掩盖。
 *
 * 刻意放在 app 模块而不是 ai 模块：CI 的硬门槛是 `:app:testDebugUnitTest`，
 * `ai/src/test` 下的测试根本不会被执行。
 */
class HttpDiagnosticsTest {

    private val cloudflarePage = "<!DOCTYPE html>\n" +
        "<html>\n" +
        "<head><title>Access denied | example.com used Cloudflare</title></head>\n" +
        "<body>error code: 1020</body>\n" +
        "</html>"

    @Test
    fun `http 200 with text html is detected as html`() {
        assertTrue(HttpDiagnostics.looksLikeHtml("text/html; charset=UTF-8", cloudflarePage))
    }

    @Test
    fun `html detection also works without content type`() {
        assertTrue(HttpDiagnostics.looksLikeHtml(null, cloudflarePage))
    }

    @Test
    fun `json response is not detected as html`() {
        assertFalse(HttpDiagnostics.looksLikeHtml("application/json", "{\"object\":\"list\"}"))
    }

    @Test
    fun `html error message keeps status code content type and final url`() {
        val ex = HttpDiagnostics.htmlResponseException(
            code = 200,
            finalUrl = "https://api.example.com/v1/chat/completions",
            contentType = "text/html; charset=UTF-8",
            body = cloudflarePage,
        )

        val message = ex.message.orEmpty()
        assertTrue("应明确说明返回的是 HTML", message.contains("服务器返回 HTML"))
        assertTrue("应带状态码", message.contains("200"))
        assertTrue("应带 Content-Type", message.contains("text/html"))
        assertTrue("应带最终 URL", message.contains("https://api.example.com/v1/chat/completions"))
        assertFalse("不能把 kotlinx 的原始解析异常当成最终错误", message.contains("Unexpected JSON token"))
    }

    @Test
    fun `error message does not leak credentials`() {
        val ex = HttpDiagnostics.htmlResponseException(
            code = 403,
            finalUrl = "https://api.example.com/v1/models",
            contentType = "text/html",
            body = cloudflarePage,
        )
        val message = ex.message.orEmpty()
        assertFalse(message.contains("Bearer"))
        assertFalse(message.contains("sk-"))
    }

    @Test
    fun `describe response contains all diagnostic fields`() {
        val text = HttpDiagnostics.describeHttpResponse(
            code = 200,
            requestUrl = "https://api.example.com/v1/chat/completions",
            finalUrl = "https://blocked.example.com/",
            contentType = "text/html; charset=UTF-8",
            contentLength = "1234",
        )
        assertTrue(text.contains("HTTP 200"))
        assertTrue(text.contains("request URL: https://api.example.com/v1/chat/completions"))
        assertTrue(text.contains("final URL (redirect): https://blocked.example.com/"))
        assertTrue(text.contains("Content-Type: text/html; charset=UTF-8"))
        assertTrue(text.contains("Content-Length: 1234"))
        assertFalse(text.contains("Bearer"))
    }

    @Test
    fun `join url avoids double slash and missing slash`() {
        assertEquals(
            "https://api.example.com/v1/chat/completions",
            joinUrl("https://api.example.com/v1/", "/chat/completions"),
        )
        assertEquals(
            "https://api.example.com/v1/models",
            joinUrl("https://api.example.com/v1", "models"),
        )
    }
}
