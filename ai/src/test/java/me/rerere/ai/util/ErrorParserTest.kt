package me.rerere.ai.util

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ErrorParserTest {

    // 用户实测报错: {"message":"true","type":"bad_response_status_code","param":"","code":"bad_response_status_code"}
    // message 字段是网关序列化成字符串的布尔标记, 不含任何可读信息,
    // 必须跳过它去取 type/code, 而不是把 "true" 当文案展示
    @Test
    fun `gateway wrapped string-true message falls back to type field`() {
        val body = "{\"message\":\"true\",\"type\":\"bad_response_status_code\",\"param\":\"\",\"code\":\"bad_response_status_code\"}"
        val e = Json.parseToJsonElement(body).parseErrorDetail()
        assertEquals("bad_response_status_code", e.message)
    }

    @Test
    fun `boolean error field is skipped`() {
        val e = Json.parseToJsonElement("{\"error\":true,\"message\":\"invalid api key\"}").parseErrorDetail()
        assertEquals("invalid api key", e.message)
    }

    @Test
    fun `bare boolean primitive reports unknown instead of literal`() {
        val e = Json.parseToJsonElement("true").parseErrorDetail()
        assertTrue(e.message!!.contains("Unknown error"))
        assertTrue(!e.message!!.equals("true"))
    }

    @Test
    fun `normal error message still parsed`() {
        val e = Json.parseToJsonElement("{\"error\":{\"message\":\"rate limited\",\"type\":\"rate_limit\"}}").parseErrorDetail()
        assertEquals("rate limited", e.message)
    }

    @Test
    fun `parseHttpErrorResponse keeps status code in message and field`() {
        val e = parseHttpErrorResponse(403, "{\"error\":{\"message\":\"quota exceeded\"}}")
        assertEquals(403, e.httpStatusCode)
        assertTrue(e.message!!.startsWith("HTTP 403"))
        assertTrue(e.message!!.contains("quota exceeded"))
    }

    @Test
    fun `parseHttpErrorResponse with empty body still shows status`() {
        val e = parseHttpErrorResponse(500, "")
        assertEquals(500, e.httpStatusCode)
        assertEquals("HTTP 500 (empty response body)", e.message)
    }

    @Test
    fun `parseHttpErrorResponse with non-json body keeps raw preview`() {
        val e = parseHttpErrorResponse(502, "<html>bad gateway</html>")
        assertEquals(502, e.httpStatusCode)
        assertTrue(e.message!!.contains("502"))
        assertTrue(e.message!!.contains("bad gateway"))
    }
}
