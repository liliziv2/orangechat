package me.rerere.rikkahub.data.ai.transformers

import kotlinx.datetime.LocalDateTime
import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Instant

/**
 * Reasoning 出口的「内部内容泄漏」回归测试。
 *
 * 背景：内部约束文本（system prompt、注入条款、合成指令）曾经顺着主动消息链路落库，
 * 以用户气泡的形式暴露出来。那条链路已经有 ProactiveMessageRequestAssemblyTest 兜住了；
 * 这里补的是另一个出口 —— **Reasoning**。`<think>` 拆分会把内容从「可见文本」搬到
 * 「Reasoning」，搬错方向就会让该看见的看不见、该藏起来的露出来。
 *
 * 之所以能直接测 [splitThinkTags]，是因为它被特意抽成了不依赖 TransformerContext 的纯函数：
 * TransformerContext 需要一个 android.content.Context，否则这个文件在纯 JVM 单测里跑不起来。
 */
class ThinkTagTransformerTest {

    private val closedAt = Instant.fromEpochSeconds(1_700_000_000L)
    private val finishAt = Instant.fromEpochSeconds(1_700_000_100L)

    private fun message(role: MessageRole, text: String) = UIMessage(
        role = role,
        parts = listOf(UIMessagePart.Text(text)),
        createdAt = LocalDateTime(2026, 2, 22, 10, 0, 0),
    )

    private fun UIMessage.reasoningPart() =
        parts.filterIsInstance<UIMessagePart.Reasoning>().singleOrNull()

    private fun UIMessage.visibleText() =
        parts.filterIsInstance<UIMessagePart.Text>().joinToString("\n") { it.text }

    // ---------- 拆分本身 ----------

    @Test
    fun `think block becomes reasoning and the visible text is left clean`() {
        val out = message(MessageRole.ASSISTANT, "<think>内部推理</think>你好")
            .splitThinkTags(closedAt, null)
        assertEquals("内部推理", out.reasoningPart()?.reasoning)
        assertEquals("你好", out.visibleText())
    }

    @Test
    fun `reasoning content never shows up in the visible text`() {
        val out = message(MessageRole.ASSISTANT, "<think>INTERNAL-ONLY</think>reply")
            .splitThinkTags(closedAt, null)
        assertFalse(out.visibleText(), out.visibleText().contains("INTERNAL-ONLY"))
    }

    @Test
    fun `visible text is not duplicated into reasoning`() {
        val out = message(MessageRole.ASSISTANT, "<think>r</think>VISIBLE")
            .splitThinkTags(closedAt, null)
        assertEquals("r", out.reasoningPart()?.reasoning)
    }

    @Test
    fun `plain assistant text is untouched`() {
        val original = message(MessageRole.ASSISTANT, "just text")
        val out = original.splitThinkTags(closedAt, null)
        assertNull(out.reasoningPart())
        assertEquals("just text", out.visibleText())
    }

    @Test
    fun `non text parts survive so the tool chain stays intact`() {
        val tool = UIMessagePart.Tool(toolCallId = "call-1", toolName = "search", input = "{}")
        val out = UIMessage(
            role = MessageRole.ASSISTANT,
            parts = listOf(UIMessagePart.Text("<think>r</think>t"), tool),
            createdAt = LocalDateTime(2026, 2, 22, 10, 0, 0),
        ).splitThinkTags(closedAt, null)
        assertTrue(out.parts.any { it is UIMessagePart.Tool })
    }

    // ---------- 关键不变量：只有 ASSISTANT 能变成 Reasoning ----------

    @Test
    fun `user message is never turned into reasoning`() {
        // 注入进来的内容是 USER 轮次。它绝不能借由 think 标记变成 Reasoning 被渲染出来。
        val out = message(MessageRole.USER, "<think>internal constraint</think>hi")
            .splitThinkTags(closedAt, null)
        assertNull(out.reasoningPart())
        assertEquals("<think>internal constraint</think>hi", out.visibleText())
    }

    @Test
    fun `system message is never turned into reasoning`() {
        val out = message(MessageRole.SYSTEM, "<think>internal constraint</think>")
            .splitThinkTags(closedAt, null)
        assertNull(out.reasoningPart())
    }

    @Test
    fun `tool message is never turned into reasoning`() {
        val out = message(MessageRole.TOOL, "<think>x</think>{}")
            .splitThinkTags(closedAt, null)
        assertNull(out.reasoningPart())
    }

    // ---------- finishedAt 语义（流式 vs 结束）----------

    @Test
    fun `closed tag counts as finished even mid stream`() {
        val out = message(MessageRole.ASSISTANT, "<think>done</think>text")
            .splitThinkTags(closedAt, null)
        assertEquals(closedAt, out.reasoningPart()?.finishedAt)
    }

    @Test
    fun `open tag stays unfinished mid stream but is finished at the end`() {
        val streaming = message(MessageRole.ASSISTANT, "<think>still going")
            .splitThinkTags(closedAt, null)
        assertNull(streaming.reasoningPart()?.finishedAt)

        val finished = message(MessageRole.ASSISTANT, "<think>still going")
            .splitThinkTags(finishAt, finishAt)
        assertEquals(finishAt, finished.reasoningPart()?.finishedAt)
    }
}
