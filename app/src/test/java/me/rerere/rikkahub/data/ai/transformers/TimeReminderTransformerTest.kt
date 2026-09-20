/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.ai.transformers

import kotlinx.datetime.LocalDateTime
import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimeReminderTransformerTest {

    private fun userMessage(text: String, createdAt: LocalDateTime) = UIMessage(
        role = MessageRole.USER,
        parts = listOf(UIMessagePart.Text(text)),
        createdAt = createdAt,
    )

    private fun getMessageText(msg: UIMessage): String =
        msg.parts.filterIsInstance<UIMessagePart.Text>().joinToString("") { it.text }

    // 契约说明：applyTimeReminder 会给**第一条** USER 消息也插一条 <time_reminder>
    // （gapSeconds = null，只报当前时间，不带 "since last message"）。
    // 目的是让模型从第一句起就知道现在几点，而不是只在间隔过大时才提醒。
    // 下面的断言按这个契约写；旧版本断言的是「首条不注入」，已经过期 ——
    // 于是每条用例都少算了一条，表现为 expected 比实际小 1。

    @Test
    fun `single message still gets a leading time reminder`() {
        val messages = listOf(userMessage("Hello", LocalDateTime(2026, 2, 22, 10, 0, 0)))
        val result = applyTimeReminder(messages)
        assertEquals(2, result.size)
        val leading = getMessageText(result[0])
        assertTrue(leading.contains("<time_reminder>"))
        // 首条之前没有上一条消息，不该出现间隔描述
        assertTrue(leading, !leading.contains("since last message"))
        assertEquals("Hello", getMessageText(result[1]))
    }

    @Test
    fun `gap less than 1 hour should not inject a gap reminder`() {
        val messages = listOf(
            userMessage("Hello", LocalDateTime(2026, 2, 22, 10, 0, 0)),
            userMessage("World", LocalDateTime(2026, 2, 22, 10, 30, 0)), // 30 分钟
        )
        val result = applyTimeReminder(messages)
        // 首条提醒 + 两条原文，中间不再插
        assertEquals(3, result.size)
        assertTrue(getMessageText(result[0]).contains("<time_reminder>"))
        assertEquals("Hello", getMessageText(result[1]))
        assertEquals("World", getMessageText(result[2]))
    }

    @Test
    fun `gap exactly 1 hour should not inject a gap reminder`() {
        val messages = listOf(
            userMessage("Hello", LocalDateTime(2026, 2, 22, 10, 0, 0)),
            userMessage("World", LocalDateTime(2026, 2, 22, 11, 0, 0)), // 恰好 1 小时
        )
        val result = applyTimeReminder(messages)
        assertEquals(3, result.size)
        assertEquals("World", getMessageText(result[2]))
    }

    @Test
    fun `gap more than 1 hour should inject time reminder before second message`() {
        val messages = listOf(
            userMessage("Hello", LocalDateTime(2026, 2, 22, 10, 0, 0)),
            userMessage("World", LocalDateTime(2026, 2, 22, 12, 0, 0)), // 2 小时
        )
        val result = applyTimeReminder(messages)
        // 首条提醒 + Hello + 间隔提醒 + World
        assertEquals(4, result.size)
        assertEquals("Hello", getMessageText(result[1]))
        // 间隔提醒插在第二条之前
        val injected = getMessageText(result[2])
        assertTrue(injected.contains("<time_reminder>"))
        assertTrue(injected.contains("since last message"))
        assertEquals("World", getMessageText(result[3]))
    }

    @Test
    fun `injected message should contain day of week and gap in hours`() {
        val messages = listOf(
            userMessage("Hello", LocalDateTime(2026, 2, 22, 10, 0, 0)),
            userMessage("World", LocalDateTime(2026, 2, 22, 12, 0, 0)), // 2 小时
        )
        val result = applyTimeReminder(messages)
        val injected = getMessageText(result[2])
        // 星期几和时间之间有逗号分隔
        assertTrue(injected.contains(","))
        assertTrue(injected.contains("2 h since last message"))
    }

    @Test
    fun `gap in days should format correctly`() {
        val messages = listOf(
            userMessage("Hello", LocalDateTime(2026, 2, 20, 10, 0, 0)),
            userMessage("World", LocalDateTime(2026, 2, 22, 10, 0, 0)), // 2 天
        )
        val result = applyTimeReminder(messages)
        val injected = getMessageText(result[2])
        assertTrue(injected.contains("2 d since last message"))
    }

    @Test
    fun `multiple large gaps should inject multiple reminders`() {
        val messages = listOf(
            userMessage("Msg 1", LocalDateTime(2026, 2, 20, 10, 0, 0)),
            userMessage("Msg 2", LocalDateTime(2026, 2, 21, 10, 0, 0)), // 1 天
            userMessage("Msg 3", LocalDateTime(2026, 2, 22, 10, 0, 0)), // 1 天
        )
        val result = applyTimeReminder(messages)
        // 3 条原文 + 首条提醒 + 2 条间隔提醒
        assertEquals(6, result.size)
        assertTrue(getMessageText(result[0]).contains("<time_reminder>"))
        assertEquals("Msg 1", getMessageText(result[1]))
        assertTrue(getMessageText(result[2]).contains("<time_reminder>"))
        assertEquals("Msg 2", getMessageText(result[3]))
        assertTrue(getMessageText(result[4]).contains("<time_reminder>"))
        assertEquals("Msg 3", getMessageText(result[5]))
    }

    @Test
    fun `empty messages should return empty`() {
        val result = applyTimeReminder(emptyList())
        assertEquals(0, result.size)
    }
}
