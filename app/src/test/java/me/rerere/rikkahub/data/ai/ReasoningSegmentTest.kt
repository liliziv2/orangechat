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
import me.rerere.rikkahub.ui.components.message.MessagePartBlock
import me.rerere.rikkahub.ui.components.message.ThinkingStep
import me.rerere.rikkahub.ui.components.message.groupMessageParts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 「空思考节点」收尾回归测试。
 *
 * 现象：聊天里出现 `思考了 1.2 秒 / 文本"报" / 思考了 0.1 秒 / 正文` —— 中间那个思考节点
 * 没有任何 reasoning 内容，还把两侧正文劈成了两个气泡。
 *
 * 根因在累加器：供应商会发「只带元数据、没有正文」的思考事件（Anthropic 的
 * `signature_delta` 只带 `signature`，OpenAI Responses 的 `output_item.added` /
 * `output_item.done` 只带 `encrypted_content`）。旧实现把这种分片当成「一段新思考的开始」：
 * 它既建了一个空 part，又调 `closeText()` 把当前文本段切断。
 *
 * 本文件锁住修复后的不变量：
 * 1. 没有可见内容的思考分片**不开新段**、**不切断文本段**；
 * 2. 它的元数据并进已有的思考段（优先开着的那段，否则最近一个 Reasoning part）；
 * 3. 一段思考都没有时，这个签名没有可签的内容，丢弃；
 * 4. `groupMessageParts()` 不把没有可见内容的 Reasoning part 渲染成思考节点，并因此把
 *    两侧文本并回一个气泡（旧版本已经存进库的消息靠这条兜底）。
 *
 * 刻意放在 app 模块：CI 的硬门槛是 `:app:testDebugUnitTest`，`ai/src/test` 不执行。
 */
class ReasoningSegmentTest {
    private val model = Model(modelId = "test-model")

    private val signature: JsonObject = buildJsonObject { put("signature", "sig-1") }

    private fun chunkOf(vararg parts: UIMessagePart): MessageChunk = MessageChunk(
        id = "resp-1",
        model = "test-model",
        choices = listOf(
            UIMessageChoice(
                index = 0,
                delta = UIMessage(role = MessageRole.ASSISTANT, parts = parts.toList()),
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

    private fun tool(id: String) = UIMessagePart.Tool(
        toolCallId = id,
        toolName = "search",
        input = "{}",
        output = emptyList(),
    )

    /** 把一串分片喂给累加器（一条响应流一个实例），返回助手消息最终的 parts。 */
    private fun run(vararg parts: UIMessagePart): List<UIMessagePart> {
        val handler = StreamChunkHandler(model)
        var messages: List<UIMessage> = listOf(UIMessage.user("hi"))
        parts.forEach { messages = handler.handle(messages, chunkOf(it)) }
        messages = handler.finish(messages)
        return messages.last().parts
    }

    private fun List<UIMessagePart>.thinkingBlocks() =
        groupMessageParts().filterIsInstance<MessagePartBlock.ThinkingBlock>()

    private fun List<UIMessagePart>.contentBlocks() =
        groupMessageParts().filterIsInstance<MessagePartBlock.ContentBlock>()

    // ---------- A 普通无 reasoning 回复 ----------

    @Test
    fun `A plain reply without reasoning has no reasoning part and no thinking node`() {
        val parts = run(text("你"), text("好"))
        assertEquals(1, parts.size)
        assertEquals("你好", (parts[0] as UIMessagePart.Text).text)
        assertEquals(0, parts.thinkingBlocks().size)
    }

    // ---------- B 有真实 reasoning 回复 ----------

    @Test
    fun `B real reasoning keeps its content and renders exactly one thinking node`() {
        val parts = run(reasoning("让我想想"), text("答案"))
        assertEquals(2, parts.size)
        assertEquals("让我想想", (parts[0] as UIMessagePart.Reasoning).reasoning)

        val thinking = parts.thinkingBlocks().single()
        assertEquals(1, thinking.steps.size)
        assertEquals(
            "让我想想",
            (thinking.steps[0] as ThinkingStep.ReasoningStep).reasoning.reasoning,
        )
    }

    // ---------- C reasoning → text ----------

    @Test
    fun `C reasoning then text closes the reasoning segment`() {
        val parts = run(reasoning("想"), text("答"))
        assertNotNull((parts[0] as UIMessagePart.Reasoning).finishedAt)
        assertEquals("答", (parts[1] as UIMessagePart.Text).text)
        assertEquals(1, parts.thinkingBlocks().size)
        assertEquals(1, parts.contentBlocks().size)
    }

    // ---------- D text → reasoning → text ----------

    @Test
    fun `D text then reasoning then text keeps three parts in original order`() {
        val parts = run(text("前"), reasoning("想"), text("后"))
        assertEquals(3, parts.size)
        assertEquals("前", (parts[0] as UIMessagePart.Text).text)
        assertEquals("想", (parts[1] as UIMessagePart.Reasoning).reasoning)
        assertEquals("后", (parts[2] as UIMessagePart.Text).text)
    }

    // ---------- E reasoning → tool → text ----------

    @Test
    fun `E reasoning then tool then text groups the tool into the same thinking block`() {
        val parts = run(reasoning("想"), tool("t1"), text("答"))
        val thinking = parts.thinkingBlocks().single()
        assertEquals(2, thinking.steps.size)
        assertTrue(thinking.steps[0] is ThinkingStep.ReasoningStep)
        assertTrue(thinking.steps[1] is ThinkingStep.ToolStep)

        val content = parts.contentBlocks().single()
        assertEquals("答", (content.part as UIMessagePart.Text).text)
    }

    // ---------- F reasoning 很短、只有一个 delta ----------

    @Test
    fun `F a single short reasoning delta is still a real thinking node`() {
        val parts = run(reasoning("嗯"))
        assertEquals(1, parts.size)
        assertEquals("嗯", (parts[0] as UIMessagePart.Reasoning).reasoning)
        assertNotNull((parts[0] as UIMessagePart.Reasoning).finishedAt)
        assertEquals(1, parts.thinkingBlocks().size)
    }

    // ---------- 核心回归：只带元数据、没有正文的思考分片 ----------

    @Test
    fun `metadata only reasoning delta does not create a part and does not split text`() {
        // 旧实现：text("报") + signature_delta + text("错…") → 三个 part，
        // UI 上就是「报 / 思考了 0.x 秒 / 错…」。
        val parts = run(text("报"), reasoning("", signature), text("错这种东西最气人"))
        assertEquals(1, parts.size)
        assertEquals("报错这种东西最气人", (parts[0] as UIMessagePart.Text).text)
        assertEquals(0, parts.thinkingBlocks().size)
    }

    @Test
    fun `metadata only reasoning delta attaches to the reasoning part it belongs to`() {
        // 真实形态：思考正文先到，签名在文本之后才到（网关重排 / 多思考块）。
        val parts = run(reasoning("让我想想"), text("报"), reasoning("", signature), text("错"))
        assertEquals(2, parts.size)

        val first = parts[0] as UIMessagePart.Reasoning
        assertEquals("让我想想", first.reasoning)
        assertEquals("sig-1", first.metadata?.get("signature")?.toString()?.trim('"'))
        assertEquals("报错", (parts[1] as UIMessagePart.Text).text)
        assertEquals(1, parts.thinkingBlocks().size)
    }

    @Test
    fun `metadata only reasoning delta keeps the metadata keys that were already there`() {
        val existing = buildJsonObject { put("signature", "old") }
        val extra = buildJsonObject { put("encrypted_content", "enc") }

        val parts = run(reasoning("想", existing), text("答"), reasoning("", extra))

        val metadata = (parts[0] as UIMessagePart.Reasoning).metadata
        assertEquals("old", metadata?.get("signature")?.toString()?.trim('"'))
        assertEquals("enc", metadata?.get("encrypted_content")?.toString()?.trim('"'))
    }

    @Test
    fun `blank reasoning delta without metadata is dropped entirely`() {
        val parts = run(text("4k"), reasoning("\n"), text("4"))
        assertEquals(1, parts.size)
        assertEquals("4k4", (parts[0] as UIMessagePart.Text).text)
    }

    // ---------- 历史消息兜底（旧版本已经存进库的消息） ----------

    @Test
    fun `groupMessageParts heals a persisted empty reasoning part`() {
        // 手工造出旧版本会存下来的形态：空 reasoning 插在两段文本之间。
        val stale = listOf(
            reasoning("想"),
            text("报"),
            reasoning("", signature),
            text("错这种东西最气人"),
        )
        assertEquals(1, stale.thinkingBlocks().size)
        val content = stale.contentBlocks().single()
        assertEquals("报错这种东西最气人", (content.part as UIMessagePart.Text).text)
    }

    @Test
    fun `zero width only reasoning is not rendered as a thinking node`() {
        val parts = listOf(reasoning("\u200B"))
        assertEquals(0, parts.thinkingBlocks().size)
    }

    // ---------- G 连续多次生成 ----------

    @Test
    fun `G a later generation does not add a second empty thinking node`() {
        var messages: List<UIMessage> = listOf(UIMessage.user("hi"))

        // 第一次生成：真思考 + 正文。
        val first = StreamChunkHandler(model)
        messages = first.handle(messages, chunkOf(reasoning("想")))
        messages = first.handle(messages, chunkOf(text("答")))
        messages = first.finish(messages)

        // 第二次生成（同一条助手消息复用）：只来了一个签名分片。
        val second = StreamChunkHandler(model)
        messages = second.handle(messages, chunkOf(reasoning("", signature)))
        messages = second.handle(messages, chunkOf(text("补")))
        messages = second.finish(messages)

        val parts = messages.last().parts
        // 只有第一段思考那一个 Reasoning part —— 第二次生成的那个签名分片不能凭空多出一个。
        assertEquals(1, parts.count { it is UIMessagePart.Reasoning })
        // 每步生成都用新的累加器实例，所以文本本来就是两个 part，靠 UI 并回一个气泡。
        assertEquals(listOf("答", "补"), parts.filterIsInstance<UIMessagePart.Text>().map { it.text })
        assertEquals(1, parts.thinkingBlocks().size)
        val content = parts.contentBlocks().single()
        assertEquals("答补", (content.part as UIMessagePart.Text).text)
    }
}
