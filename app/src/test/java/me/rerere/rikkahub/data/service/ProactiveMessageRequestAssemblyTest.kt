package me.rerere.rikkahub.data.service

import me.rerere.ai.core.MessageRole
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.ai.transformers.SystemHintTransformer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.uuid.Uuid

/**
 * 主动消息链路的「内部约束泄漏」回归测试。
 *
 * 历史漏洞：ProactiveMessageTriggerService 组装请求时直接取 transformedMessages.first()
 * 当指令，结果被提示词注入（[BEHAVIOR GUARDRAILS] 这类条款）顶替，注入内容随后顺着
 * streamMessages 被当成 AI 回复落库，最终以用户气泡的形式暴露在聊天界面。
 *
 * 这里锁住修复后的不变量：
 * 1. 组装请求：合成指令必须是最后一条 USER 轮次；注入内容只能以独立消息出现在它之前，
 *    不得被合并进指令；注入生成的 SYSTEM 内容只并入系统提示词，不参与对话轮次。
 * 2. 生成收尾：最终 AI 回复只能按「ASSISTANT 角色 + 排除本轮已有消息」选取，
 *    拿不到时返回 null——宁可不发，也不能捞历史旧回复或合成 USER 指令落库。
 */
class ProactiveMessageRequestAssemblyTest {

    /** 截图里泄漏到聊天界面的那段注入条款 */
    private val guardrailBlock = """
        [BEHAVIOR GUARDRAILS]
        Maintain continuity with the current conversation state.
        Keep time, location, characters, and event progress consistent.
        Do not repeat completed actions or previously established information unnecessarily.
    """.trimIndent()

    private val instruction = UIMessage(
        role = MessageRole.USER,
        parts = listOf(
            UIMessagePart.Text("请根据以上上下文决定是否发消息。没什么好说的就回复 [PASS] 即可，不要强行找话题。")
        ),
    )

    private fun user(id: Uuid, text: String) = UIMessage(
        id = id,
        role = MessageRole.USER,
        parts = listOf(UIMessagePart.Text(text)),
    )

    private fun assistant(id: Uuid, text: String) = UIMessage(
        id = id,
        role = MessageRole.ASSISTANT,
        parts = listOf(UIMessagePart.Text(text)),
    )

    // ---------- splitInjectedSystemAndUserTurns ----------

    @Test
    fun `injection keeps instruction last and separate`() {
        val injection = user(Uuid.random(), guardrailBlock)
        val transformed = listOf(injection, instruction)
        val (systemText, turns) = splitInjectedSystemAndUserTurns(transformed, instruction)
        // USER 角色的注入不并入系统提示词
        assertEquals("", systemText)
        assertEquals(2, turns.size)
        assertEquals(injection.id, turns[0].id)
        // 合成指令必须是最后一条
        assertEquals(instruction.id, turns.last().id)
        // 注入内容没有被合并进指令
        assertTrue(turns.last().toText().contains("请根据以上上下文"))
        assertFalse(turns.last().toText().contains("BEHAVIOR GUARDRAILS"))
    }

    @Test
    fun `injected system content merges into system prompt and stays out of turns`() {
        val transformed = listOf(UIMessage.system(guardrailBlock), instruction)
        val (systemText, turns) = splitInjectedSystemAndUserTurns(transformed, instruction)
        assertEquals(guardrailBlock, systemText)
        assertEquals(1, turns.size)
        assertEquals(instruction.id, turns[0].id)
    }

    @Test
    fun `assistant role injection stays its own turn before the instruction`() {
        val userInjection = user(Uuid.random(), "${SystemHintTransformer.HINT_PROACTIVE_CONTEXT}\n现在 23:40")
        val assistantInjection = assistant(Uuid.random(), guardrailBlock)
        val transformed = listOf(assistantInjection, userInjection, instruction)
        val (_, turns) = splitInjectedSystemAndUserTurns(transformed, instruction)
        assertEquals(3, turns.size)
        assertEquals(instruction.id, turns.last().id)
        assertEquals(assistantInjection.id, turns[0].id)
        assertEquals(userInjection.id, turns[1].id)
    }

    @Test
    fun `missing instruction falls back to appended last`() {
        val injection = user(Uuid.random(), guardrailBlock)
        // 防御路径：指令在转换后列表里找不到时，必须补到最后而不是丢掉
        val transformed = listOf(injection)
        val (_, turns) = splitInjectedSystemAndUserTurns(transformed, instruction)
        assertEquals(2, turns.size)
        assertEquals(instruction.id, turns.last().id)
    }

    @Test
    fun `instruction alone stays intact`() {
        val (_, turns) = splitInjectedSystemAndUserTurns(listOf(instruction), instruction)
        assertEquals(listOf(instruction), turns)
    }

    // ---------- pickNewAssistantMessage ----------

    @Test
    fun `picks the new assistant message not the tool result or history`() {
        val historyAssistant = assistant(Uuid.random(), "上一条旧回复")
        val initialIds = setOf(historyAssistant.id, Uuid.random())
        val newAssistant = assistant(Uuid.random(), "新的主动消息")
        val toolResult = UIMessage(
            id = Uuid.random(),
            role = MessageRole.TOOL,
            parts = listOf(UIMessagePart.Text("{}")),
        )
        val finalMessages = listOf(
            UIMessage.system("sys"),
            user(Uuid.random(), "你好"),
            historyAssistant,
            newAssistant,
            toolResult,
        )
        val picked = pickNewAssistantMessage(finalMessages, initialIds)
        assertEquals(newAssistant.id, picked?.id)
    }

    @Test
    fun `returns null when nothing new was generated`() {
        val historyAssistant = assistant(Uuid.random(), "上一条旧回复")
        val finalMessages = listOf(
            UIMessage.system("sys"),
            user(Uuid.random(), "你好"),
            historyAssistant,
        )
        // 这一轮没生成出任何东西时必须返回 null，不能捞历史旧回复当新消息
        assertNull(pickNewAssistantMessage(finalMessages, setOf(historyAssistant.id)))
    }

    @Test
    fun `never picks the synthetic user instruction`() {
        val finalMessages = listOf(UIMessage.system("sys"), instruction)
        // 合成 USER 指令绝不能被当成 AI 回复落库
        assertNull(pickNewAssistantMessage(finalMessages, emptySet()))
    }
}
