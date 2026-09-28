package me.rerere.rikkahub.data.ai

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import me.rerere.ai.core.MessageRole
import me.rerere.ai.provider.Model
import me.rerere.ai.ui.MessageChunk
import me.rerere.ai.ui.StreamChunkHandler
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessageChoice
import me.rerere.ai.ui.UIMessagePart
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * [StreamChunkHandler] 的合并回归测试。
 *
 * 三条验收目标都落在这里：
 * 1. 供应商在正文中间回一个纯空白 reasoning 分片时，「4k4」不能被劈成两个 Text part；
 * 2. 空白 reasoning 分片不能产出可渲染的思考块（否则 UI 显示「思考了 0.0 秒」）；
 * 3. 文本 / 思考 / 工具交错到达时，part 的顺序与归属不乱。
 *
 * 刻意放在 app 模块而不是 ai 模块：CI 的硬门槛是 `:app:testDebugUnitTest`，
 * `ai/src/test` 下的测试根本不会被执行。
 */
class StreamChunkMergeTest {
    private val model = Model(modelId = "test-model")

    private fun chunkOf(
        vararg parts: UIMessagePart,
        role: MessageRole = MessageRole.ASSISTANT,
    ): MessageChunk = MessageChunk(
        id = "resp-1",
        model = "test-model",
        choices = listOf(
            UIMessageChoice(
                index = 0,
                delta = UIMessage(role = role, parts = parts.toList()),
                message = null,
                finishReason = null,
            )
        ),
    )

    private fun text(value: String) = UIMessagePart.Text(value)

    private fun reasoning(value: String, metadata: JsonObject? = null) = UIMessagePart.Reasoning(
        reasoning = value,
        finishedAt = null,
        metadata = metadata,
    )

    private fun tool(id: String, name: String, input: String) = UIMessagePart.Tool(
        toolCallId = id,
        toolName = name,
        input = input,
        output = emptyList(),
    )

    @Test
    fun `text deltas merge into a single text part`() {
        val handler = StreamChunkHandler(model)
        var messages = listOf(UIMessage.user("hello"))

        messages = handler.handle(messages, chunkOf(text("hel")))
        messages = handler.handle(messages, chunkOf(text("lo")))
        messages = handler.finish(messages)

        assertEquals(2, messages.size)
        assertEquals(MessageRole.ASSISTANT, messages.last().role)
        assertEquals("hello", messages.last().toText())
        assertEquals(model.id, messages.last().modelId)
        assertEquals(1, messages.last().parts.size)
    }

    @Test
    fun `blank reasoning delta does not split surrounding text`() {
        val handler = StreamChunkHandler(model)
        var messages = listOf(UIMessage.user("hello"))

        messages = handler.handle(messages, chunkOf(text("4k")))
        messages = handler.handle(messages, chunkOf(reasoning("\n")))
        messages = handler.handle(messages, chunkOf(text("4")))
        messages = handler.finish(messages)

        val parts = messages.last().parts
        assertEquals(1, parts.size)
        assertEquals("4k4", (parts[0] as UIMessagePart.Text).text)
    }

    @Test
    fun `blank reasoning delta with metadata is kept for provider round trip`() {
        val handler = StreamChunkHandler(model)
        var messages = listOf(UIMessage.user("hello"))
        val signature = buildJsonObject { put("signature", "sig") }

        messages = handler.handle(messages, chunkOf(text("a")))
        messages = handler.handle(messages, chunkOf(reasoning("", signature)))
        messages = handler.handle(messages, chunkOf(text("b")))

        val parts = messages.last().parts
        assertEquals(3, parts.size)
        assertEquals("a", (parts[0] as UIMessagePart.Text).text)
        assertEquals(signature, (parts[1] as UIMessagePart.Reasoning).metadata)
        assertEquals("b", (parts[2] as UIMessagePart.Text).text)
    }

    @Test
    fun `reasoning segment is closed when the next type arrives`() {
        val handler = StreamChunkHandler(model)
        var messages = listOf(UIMessage.user("hello"))

        messages = handler.handle(messages, chunkOf(reasoning("嗯")))
        assertNull((messages.last().parts[0] as UIMessagePart.Reasoning).finishedAt)

        messages = handler.handle(messages, chunkOf(text("答案")))
        val part = messages.last().parts[0] as UIMessagePart.Reasoning
        assertEquals("嗯", part.reasoning)
        assertNotNull(part.finishedAt)
    }

    @Test
    fun `reasoning and text in the same chunk stay in semantic order`() {
        val handler = StreamChunkHandler(model)
        var messages = listOf(UIMessage.user("hello"))

        messages = handler.handle(messages, chunkOf(reasoning("想"), text("说")))

        val parts = messages.last().parts
        assertEquals(2, parts.size)
        assertEquals("想", (parts[0] as UIMessagePart.Reasoning).reasoning)
        assertNotNull((parts[0] as UIMessagePart.Reasoning).finishedAt)
        assertEquals("说", (parts[1] as UIMessagePart.Text).text)
    }

    @Test
    fun `interleaved reasoning and text keep separate segments`() {
        val handler = StreamChunkHandler(model)
        var messages = listOf(UIMessage.user("hello"))

        messages = handler.handle(messages, chunkOf(reasoning("A")))
        messages = handler.handle(messages, chunkOf(text("B")))
        messages = handler.handle(messages, chunkOf(reasoning("C")))

        val parts = messages.last().parts
        assertEquals(3, parts.size)
        assertEquals("A", (parts[0] as UIMessagePart.Reasoning).reasoning)
        assertEquals("B", (parts[1] as UIMessagePart.Text).text)
        assertEquals("C", (parts[2] as UIMessagePart.Reasoning).reasoning)
        assertNotNull((parts[0] as UIMessagePart.Reasoning).finishedAt)
        assertNull((parts[2] as UIMessagePart.Reasoning).finishedAt)
    }

    @Test
    fun `finish closes a reasoning segment left open by the stream end`() {
        val handler = StreamChunkHandler(model)
        var messages = listOf(UIMessage.user("hello"))

        messages = handler.handle(messages, chunkOf(reasoning("只想不说")))
        assertNull((messages.last().parts[0] as UIMessagePart.Reasoning).finishedAt)

        messages = handler.finish(messages)
        assertNotNull((messages.last().parts[0] as UIMessagePart.Reasoning).finishedAt)
    }

    @Test
    fun `parallel tool calls are located by toolCallId`() {
        val handler = StreamChunkHandler(model)
        var messages = listOf(UIMessage.user("hello"))

        messages = handler.handle(messages, chunkOf(tool("call-a", "weather", "{")))
        messages = handler.handle(messages, chunkOf(tool("call-b", "clock", "{")))
        messages = handler.handle(messages, chunkOf(tool("call-a", "", "\"city\":\"sh\"}")))
        messages = handler.handle(messages, chunkOf(tool("call-b", "", "\"tz\":\"+8\"}")))

        val tools = messages.last().parts.filterIsInstance<UIMessagePart.Tool>()
        assertEquals(2, tools.size)
        assertEquals("weather", tools[0].toolName)
        assertEquals("{\"city\":\"sh\"}", tools[0].input)
        assertEquals("clock", tools[1].toolName)
        assertEquals("{\"tz\":\"+8\"}", tools[1].input)
    }

    @Test
    fun `text after a tool call becomes its own segment`() {
        val handler = StreamChunkHandler(model)
        var messages = listOf(UIMessage.user("hello"))

        messages = handler.handle(messages, chunkOf(text("前")))
        messages = handler.handle(messages, chunkOf(tool("call-a", "weather", "{}")))
        messages = handler.handle(messages, chunkOf(text("后")))

        val parts = messages.last().parts
        assertEquals(3, parts.size)
        assertEquals("前", (parts[0] as UIMessagePart.Text).text)
        assertEquals("call-a", (parts[1] as UIMessagePart.Tool).toolCallId)
        assertEquals("后", (parts[2] as UIMessagePart.Text).text)
    }
}
