/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.ai.transformers

import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemHintTransformerTest {

    private fun user(text: String) = UIMessage(
        role = MessageRole.USER,
        parts = listOf(UIMessagePart.Text(text)),
    )

    private fun assistant(text: String) = UIMessage(
        role = MessageRole.ASSISTANT,
        parts = listOf(UIMessagePart.Text(text)),
    )

    /** 截图里泄漏到聊天界面的那段注入条款 */
    private val guardrailBlock = """
        [BEHAVIOR GUARDRAILS]
        Maintain continuity with the current conversation state.
        Keep time, location, characters, and event progress consistent.
        Do not repeat completed actions or previously established information unnecessarily.
    """.trimIndent()

    @Test
    fun `voice call hint is a system hint`() {
        assertTrue(SystemHintTransformer.isSystemHint(user("${SystemHintTransformer.HINT_VOICE_CALL} 用户沉默了")))
    }

    @Test
    fun `proactive context hint is a system hint`() {
        assertTrue(
            SystemHintTransformer.isSystemHint(
                user("${SystemHintTransformer.HINT_PROACTIVE_CONTEXT}\n现在 23:40")
            )
        )
    }

    @Test
    fun `time reminder is a system hint`() {
        assertTrue(SystemHintTransformer.isSystemHint(user("<time_reminder>Current time: Monday</time_reminder>")))
    }

    @Test
    fun `leaked injection block is a system hint`() {
        assertTrue(SystemHintTransformer.isSystemHint(user(guardrailBlock)))
    }

    @Test
    fun `normal user message is not a system hint`() {
        assertFalse(SystemHintTransformer.isSystemHint(user("今天好累，晚安")))
    }

    @Test
    fun `short bracketed message from the user is kept`() {
        // 用户自己打的短消息不能被当成注入块隐藏
        assertFalse(SystemHintTransformer.isSystemHint(user("[TODO] 明天记得买牛奶")))
    }

    @Test
    fun `assistant message is never a system hint`() {
        // 只有 USER 角色才可能是"程序替用户发的"，AI 引用条款不该被隐藏
        assertFalse(SystemHintTransformer.isSystemHint(assistant(guardrailBlock)))
    }

    @Test
    fun `drop stale hints keeps the last message`() {
        val messages = listOf(
            user("${SystemHintTransformer.HINT_VOICE_CALL} 第一次心跳"),
            assistant("在的"),
            user("${SystemHintTransformer.HINT_VOICE_CALL} 第二次心跳"),
        )
        val result = SystemHintTransformer.dropStaleHints(messages)
        // 沉进历史的那条被丢掉，本轮要回答的最后一条保留
        assertEquals(2, result.size)
        assertEquals(messages[1], result[0])
        assertEquals(messages[2], result[1])
    }

    @Test
    fun `drop stale hints keeps a single hint`() {
        val messages = listOf(user("${SystemHintTransformer.HINT_VOICE_CALL} 唯一一条"))
        assertEquals(messages, SystemHintTransformer.dropStaleHints(messages))
    }

    @Test
    fun `drop stale hints returns the list unchanged when there is nothing to drop`() {
        val messages = listOf(user("你好"), assistant("嗨"))
        assertEquals(messages, SystemHintTransformer.dropStaleHints(messages))
    }
}
