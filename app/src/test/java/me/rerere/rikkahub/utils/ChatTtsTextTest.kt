/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.utils

import me.rerere.rikkahub.data.voice.formatCallDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatTtsTextTest {

    @Test
    fun `english only keeps latin text`() {
        assertEquals("hello world", "hello world".keepEnglishOnlyForTts())
    }

    @Test
    fun `english only drops chinese`() {
        assertEquals("world", "你好world".keepEnglishOnlyForTts())
    }

    @Test
    fun `english only does not glue words together`() {
        // 中间的中文被丢掉后，两个英文词之间必须留有空格
        assertEquals("hello world", "hello你好world".keepEnglishOnlyForTts())
    }

    @Test
    fun `english only collapses whitespace`() {
        assertEquals("a b", "a    b".keepEnglishOnlyForTts())
    }

    @Test
    fun `english only returns empty for pure chinese`() {
        assertEquals("", "你好世界".keepEnglishOnlyForTts())
    }

    @Test
    fun `english only returns empty when only punctuation remains`() {
        // 过滤完只剩标点没有朗读价值
        assertEquals("", "，。！".keepEnglishOnlyForTts())
        assertEquals("", "!!!".keepEnglishOnlyForTts())
    }

    @Test
    fun `english only keeps digits`() {
        assertEquals("2026 years", "2026 years 你好".keepEnglishOnlyForTts())
    }

    @Test
    fun `chat tts text strips markdown`() {
        val result = "**bold** text".toChatTtsText(
            ttsOnlyReadQuoted = false,
            ttsEnglishOnly = false,
        )
        assertTrue(result, !result.contains("*"))
    }

    @Test
    fun `chat tts text can read quoted only`() {
        val result = "他说“这句才要念”后面不念".toChatTtsText(
            ttsOnlyReadQuoted = true,
            ttsEnglishOnly = false,
        )
        assertEquals("这句才要念", result)
    }

    @Test
    fun `chat tts text falls back to full text when there is no quote`() {
        val result = "没有引号的一句话".toChatTtsText(
            ttsOnlyReadQuoted = true,
            ttsEnglishOnly = false,
        )
        assertEquals("没有引号的一句话", result)
    }

    @Test
    fun `chat tts text drops jump marker`() {
        val result = "正文内容\n[JUMP]".toChatTtsText(
            ttsOnlyReadQuoted = false,
            ttsEnglishOnly = false,
        )
        assertTrue(result, !result.contains("JUMP"))
    }

    @Test
    fun `chat tts text applies english filter last`() {
        // 先去 Markdown 再过滤英文，否则星号会被当成英文内容留下
        val result = "**hello** 你好".toChatTtsText(
            ttsOnlyReadQuoted = false,
            ttsEnglishOnly = true,
        )
        assertEquals("hello", result)
    }

    @Test
    fun `call duration under one hour omits the hour part`() {
        assertEquals("0:00", formatCallDuration(0))
        assertEquals("0:07", formatCallDuration(7))
        assertEquals("1:05", formatCallDuration(65))
        assertEquals("59:59", formatCallDuration(3599))
    }

    @Test
    fun `call duration over one hour shows the hour part`() {
        assertEquals("1:00:00", formatCallDuration(3600))
        assertEquals("2:03:04", formatCallDuration(7384))
    }

    @Test
    fun `call duration clamps negative input`() {
        assertEquals("0:00", formatCallDuration(-5))
    }
}
