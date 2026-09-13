/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.voice

import kotlinx.coroutines.runBlocking
import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatVoiceReplyTest {

    private fun assistant(text: String) = UIMessage(
        role = MessageRole.ASSISTANT,
        parts = listOf(UIMessagePart.Text(text)),
    )

    private fun fakeVoice(text: String) = UIMessagePart.VoiceMessage(
        url = "file:///voice/${text.hashCode()}.mp3",
        duration = 1000,
        transcript = text,
    )

    @Test
    fun `reply without markers is not a voice reply`() {
        assertNull(parseChatVoiceReply("今天天气不错"))
    }

    @Test
    fun `text marker alone is not a voice reply`() {
        // 只有 【文本】 时没必要重写消息
        assertNull(parseChatVoiceReply("【文本】只是普通文字"))
    }

    @Test
    fun `single voice segment is parsed`() {
        val segments = parseChatVoiceReply("【语音条】晚安，好好睡")
        assertEquals(1, segments?.size)
        assertEquals(ChatVoiceSegmentType.VOICE, segments!![0].type)
        assertEquals("晚安，好好睡", segments[0].text)
    }

    @Test
    fun `mixed segments keep their order`() {
        val segments = parseChatVoiceReply("【文本】先看这个\n【语音条】这段听我说\n【文本】最后补一句")
        assertEquals(3, segments?.size)
        assertEquals(ChatVoiceSegmentType.TEXT, segments!![0].type)
        assertEquals("先看这个", segments[0].text)
        assertEquals(ChatVoiceSegmentType.VOICE, segments[1].type)
        assertEquals("这段听我说", segments[1].text)
        assertEquals(ChatVoiceSegmentType.TEXT, segments[2].type)
        assertEquals("最后补一句", segments[2].text)
    }

    @Test
    fun `content before the first marker becomes a text segment`() {
        val segments = parseChatVoiceReply("开场白\n【语音条】正文")
        assertEquals(2, segments?.size)
        assertEquals(ChatVoiceSegmentType.TEXT, segments!![0].type)
        assertEquals("开场白", segments[0].text)
    }

    @Test
    fun `empty segments are dropped`() {
        val segments = parseChatVoiceReply("【文本】\n【语音条】有内容")
        assertEquals(1, segments?.size)
        assertEquals(ChatVoiceSegmentType.VOICE, segments!![0].type)
    }

    @Test
    fun `voice marker with no content is not a voice reply`() {
        assertNull(parseChatVoiceReply("【语音条】   "))
    }

    @Test
    fun `strip removes every marker`() {
        val stripped = stripChatVoiceMarkers("【文本】甲\n【语音条】乙")
        assertTrue(stripped, !stripped.contains("【"))
        assertEquals("甲\n乙", stripped)
    }

    @Test
    fun `has marker only looks at text parts`() {
        assertTrue(assistant("【语音条】喂").hasChatVoiceMarker())
        assertTrue(!assistant("普通文字").hasChatVoiceMarker())
    }

    @Test
    fun `materialize turns voice segments into voice parts`() {
        runBlocking {
            val message = assistant("【文本】看这里\n【语音条】听这段")
            val result = materializeChatVoiceReply(message) { fakeVoice(it) }
            assertEquals(2, result?.parts?.size)
            assertTrue(result!!.parts[0] is UIMessagePart.Text)
            assertEquals("看这里", (result.parts[0] as UIMessagePart.Text).text)
            assertTrue(result.parts[1] is UIMessagePart.VoiceMessage)
            assertEquals("听这段", (result.parts[1] as UIMessagePart.VoiceMessage).transcript)
        }
    }

    @Test
    fun `materialize returns null when there is nothing to do`() {
        runBlocking {
            val message = assistant("没有标记的普通回复")
            assertNull(materializeChatVoiceReply(message) { fakeVoice(it) })
        }
    }

    @Test
    fun `materialize falls back to plain text when synthesis fails`() {
        runBlocking {
            val message = assistant("【文本】甲\n【语音条】乙")
            val result = materializeChatVoiceReply(message) { null }
            // 合成不出来就整条退化成纯文本，且不留标记
            assertEquals(1, result?.parts?.size)
            val text = (result!!.parts[0] as UIMessagePart.Text).text
            assertTrue(text, !text.contains("【"))
            assertEquals("甲\n乙", text)
        }
    }

    @Test
    fun `materialize falls back to plain text when synthesis throws`() {
        runBlocking {
            val message = assistant("【语音条】乙")
            val result = materializeChatVoiceReply(message) { error("boom") }
            assertEquals(1, result?.parts?.size)
            assertEquals("乙", (result!!.parts[0] as UIMessagePart.Text).text)
        }
    }

    @Test
    fun `materialize keeps non-text parts`() {
        runBlocking {
            val tool = UIMessagePart.Tool(
                toolCallId = "call-1",
                toolName = "search",
                input = "{}",
            )
            val message = UIMessage(
                role = MessageRole.ASSISTANT,
                parts = listOf(UIMessagePart.Text("【语音条】听我说"), tool),
            )
            val result = materializeChatVoiceReply(message) { fakeVoice(it) }
            // 工具调用必须保留，否则工具链结构会坏掉
            assertEquals(2, result?.parts?.size)
            assertTrue(result!!.parts[0] is UIMessagePart.VoiceMessage)
            assertEquals(tool, result.parts[1])
        }
    }
}
