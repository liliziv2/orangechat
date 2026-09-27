package me.rerere.rikkahub.ui.components.message

import androidx.compose.ui.util.fastForEachIndexed
import me.rerere.ai.ui.UIMessagePart

/**
 * 思考步骤类型，用于分组 Reasoning 和 Tool
 */
sealed interface ThinkingStep {
    data class ReasoningStep(
        val reasoning: UIMessagePart.Reasoning,
    ) : ThinkingStep

    data class ToolStep(
        val tool: UIMessagePart.Tool,
    ) : ThinkingStep
}

/**
 * 消息部分块类型，用于保持渲染顺序
 */
sealed interface MessagePartBlock {
    data class ThinkingBlock(val steps: List<ThinkingStep>) : MessagePartBlock
    data class ContentBlock(val part: UIMessagePart, val index: Int) : MessagePartBlock
}

/**
 * 将 parts 分组成 ThinkingBlock 和 ContentBlock
 * 连续的 Reasoning 和 Tool 会被分组到一个 ThinkingBlock 中
 *
 * 另外两条规则，目的都是让「被空思考块劈开的连续文本」重新变成一个气泡：
 *
 * 1. 内容为空白（Reasoning.reasoning 为 `isBlank()`）的 Reasoning 分片不参与分组 ——
 *    它既没有可显示的内容，又会在下面把两侧的 Text 切成两个 ContentBlock；
 * 2. 分组结束后，相邻的两个 Text ContentBlock 并回一块。
 *
 * 背景：供应商偶尔会在正文中间回一个纯空白的 reasoning 分片（见
 * `UIMessage.appendDelta` 里的说明），流式累加时它会插进两个 Text part 之间，
 * 而 Text 只在「上一个 part 也是 Text」时才合并，于是「4k4」这种连续文本被永久
 * 劈成两个 part，渲染成两个气泡、中间还留一个「思考了 0.0 秒」的空思考块。
 *
 * 正常流式路径下相邻 Text 不会出现（累加器会把连续 Text delta 合并成同一个 part），
 * 所以规则 2 只在规则 1 生效时才有实际效果。
 */
fun List<UIMessagePart>.groupMessageParts(): List<MessagePartBlock> {
    val result = mutableListOf<MessagePartBlock>()
    var currentThinkingSteps = mutableListOf<ThinkingStep>()

    fun flushThinkingSteps() {
        if (currentThinkingSteps.isNotEmpty()) {
            result.add(MessagePartBlock.ThinkingBlock(currentThinkingSteps.toList()))
            currentThinkingSteps = mutableListOf()
        }
    }

    this.fastForEachIndexed { index, part ->
        when (part) {
            is UIMessagePart.Reasoning -> {
                // 空白分片直接跳过，且**不 flush** —— 两侧的 Text 才能在下面并回一块。
                if (part.reasoning.isNotBlank()) {
                    currentThinkingSteps.add(ThinkingStep.ReasoningStep(part))
                }
            }

            is UIMessagePart.Tool -> {
                currentThinkingSteps.add(ThinkingStep.ToolStep(part))
            }

            else -> {
                flushThinkingSteps()
                result.add(MessagePartBlock.ContentBlock(part, index))
            }
        }
    }
    flushThinkingSteps()

    // 相邻 Text 并回一块，保留前一块的 index（渲染侧拿它当 key）。
    val merged = mutableListOf<MessagePartBlock>()
    result.forEach { block ->
        val previous = merged.lastOrNull() as? MessagePartBlock.ContentBlock
        val current = block as? MessagePartBlock.ContentBlock
        val previousText = previous?.part as? UIMessagePart.Text
        val currentText = current?.part as? UIMessagePart.Text
        if (previous != null && previousText != null && currentText != null) {
            merged[merged.lastIndex] = previous.copy(
                part = previousText.copy(text = previousText.text + currentText.text)
            )
        } else {
            merged.add(block)
        }
    }
    return merged
}
