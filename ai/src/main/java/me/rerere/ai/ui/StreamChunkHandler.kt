package me.rerere.ai.ui

import me.rerere.ai.provider.Model
import kotlin.time.Clock

/**
 * 把 Provider 流式产出的 [MessageChunk] 合并进消息列表末尾的助手消息。
 *
 * 为什么不能「只更新最后一个 part」：文本、思考、工具调用、图片可能交错到达。一旦用
 * 「最后一个 part 是不是同类型」来决定追加还是新建，交错处就会被劈成两个 part ——
 * 表现就是一段连续正文被切成两个气泡、中间还夹一个「思考了 0.0 秒」的空思考块。
 *
 * 所以这里采用显式分段：
 * - 每收到一个 part，先按类型关掉**其它类型**的段（文本段 / 思考段 / 图片段），
 *   同类型则继续沿用当前段；
 * - 段在 [UIMessage.parts] 里的下标记在 `xxxPartIndexes` 里，后续增量按**段 id** 定位，
 *   而不是靠「它在不在末尾」；
 * - 工具调用本来就带 `toolCallId`，直接按 id 定位，因此并行生成的多个工具调用不会串。
 *
 * 供应商的 [MessageChunk] 里没有段 id（同一段文本的所有增量共用响应 id），所以段 id 由本类
 * 自己发号：text-1 / reasoning-2 / image-3。
 *
 * 最关键的一条：**纯空白且无 metadata 的思考增量直接丢弃，而且不关段**。供应商偶尔会在正文
 * 中间回一个换行这样的空思考分片，它既没有可显示内容，一旦被当成「类型切换」处理就会把两侧
 * 文本劈成两个 part。丢弃它，两侧文本才能留在同一段里。
 *
 * [handle] 不修改传入的消息列表，返回包含更新后消息的新列表；列表末尾不是助手消息时会先补
 * 一条空的助手消息。
 *
 * 本类保存着**一条**响应流的合并状态，不是无状态转换器：每条并发响应流都要用独立实例，且事件
 * 必须按 Provider 产出的顺序交给同一个实例。一条流结束后调用一次 [finish] 收尾即可，不要复用
 * 到下一条流。
 */
class StreamChunkHandler(private val model: Model? = null) {
    // 值是对应段在助手消息 parts 列表里的下标。段只追加、不插入，所以下标不会失效。
    private val textPartIndexes = mutableMapOf<String, Int>()
    private val reasoningPartIndexes = mutableMapOf<String, Int>()
    private val imagePartIndexes = mutableMapOf<String, Int>()

    // 当前开着的段。为 null 表示该类型当前没有开着的段。
    private var textId: String? = null
    private var reasoningId: String? = null
    private var imageId: String? = null
    private var sequence = 0

    /**
     * 把一个 [chunk] 合并进消息列表末尾的助手消息，返回新的消息列表。
     *
     * @throws IllegalArgumentException 当 [messages] 为空时抛出
     */
    fun handle(messages: List<UIMessage>, chunk: MessageChunk): List<UIMessage> {
        require(messages.isNotEmpty()) {
            "messages must not be empty"
        }
        val choice = chunk.choices.getOrNull(0) ?: return messages
        val delta = choice.delta ?: choice.message ?: return messages
        val targetMessages = if (messages.last().role != delta.role) {
            messages + UIMessage(modelId = model?.id, role = delta.role, parts = emptyList())
        } else {
            messages
        }
        return targetMessages.dropLast(1) + append(targetMessages.last(), delta)
    }

    /**
     * 一条响应流结束时调用：结束还没关掉的思考段（否则 UI 会一直显示「思考中」），并释放本次流的
     * 段状态。
     */
    fun finish(messages: List<UIMessage>): List<UIMessage> {
        textId = null
        reasoningId = null
        imageId = null
        textPartIndexes.clear()
        reasoningPartIndexes.clear()
        imagePartIndexes.clear()
        if (messages.isEmpty()) return messages
        return messages.dropLast(1) + messages.last().finishReasoning()
    }

    private fun append(message: UIMessage, delta: UIMessage): UIMessage {
        val newParts = message.parts.toMutableList()
        delta.parts.forEach { deltaPart ->
            appendPart(newParts, deltaPart)
        }
        return message.copy(
            parts = newParts,
            annotations = delta.annotations.ifEmpty { message.annotations },
        )
    }

    private fun appendPart(parts: MutableList<UIMessagePart>, deltaPart: UIMessagePart) {
        when (deltaPart) {
            is UIMessagePart.Text -> {
                if (deltaPart.text.isEmpty()) return
                closeReasoning(parts)
                closeImage()
                val id = textId ?: nextId(SEGMENT_TEXT).also { textId = it }
                val index = textPartIndexes[id]
                val existing = index?.let { parts.getOrNull(it) as? UIMessagePart.Text }
                if (index != null && existing != null) {
                    parts[index] = existing.copy(
                        text = existing.text + deltaPart.text,
                        metadata = deltaPart.metadata ?: existing.metadata,
                    )
                } else {
                    textPartIndexes[id] = parts.size
                    parts.add(UIMessagePart.Text(deltaPart.text, deltaPart.metadata))
                }
            }

            is UIMessagePart.Reasoning -> {
                // 纯空白且无 metadata 的思考分片：既没有可显示内容，又会在正文中间插出一个
                // 0 秒的空思考块、把两侧文本劈成两个 part。直接丢弃，**且不关段**。
                // 带 metadata 的分片仍然保留：thought signature 之类的元数据要回传供应商。
                if (deltaPart.reasoning.isBlank() && deltaPart.metadata == null) return
                closeText()
                closeImage()
                val id = reasoningId ?: nextId(SEGMENT_REASONING).also { reasoningId = it }
                val index = reasoningPartIndexes[id]
                val existing = index?.let { parts.getOrNull(it) as? UIMessagePart.Reasoning }
                if (index != null && existing != null) {
                    parts[index] = existing.copy(
                        reasoning = existing.reasoning + deltaPart.reasoning,
                        metadata = deltaPart.metadata ?: existing.metadata,
                    )
                } else {
                    reasoningPartIndexes[id] = parts.size
                    parts.add(
                        UIMessagePart.Reasoning(
                            reasoning = deltaPart.reasoning,
                            createdAt = deltaPart.createdAt,
                            finishedAt = null,
                            metadata = deltaPart.metadata,
                        )
                    )
                }
            }

            is UIMessagePart.Tool -> {
                closeText()
                closeReasoning(parts)
                closeImage()
                if (deltaPart.toolCallId.isBlank()) {
                    // 还没有 id：并入最后一个工具 part（同一轮里工具名/参数被拆片发送的形态）。
                    val lastIndex = parts.indexOfLast { it is UIMessagePart.Tool }
                    if (lastIndex < 0) {
                        parts.add(deltaPart.copy())
                    } else {
                        parts[lastIndex] = (parts[lastIndex] as UIMessagePart.Tool).merge(deltaPart)
                    }
                } else {
                    // 有 id：按 toolCallId 定位。工具调用可以并行生成，位置不可靠。
                    val index = parts.indexOfFirst {
                        it is UIMessagePart.Tool && it.toolCallId == deltaPart.toolCallId
                    }
                    if (index < 0) {
                        parts.add(deltaPart.copy())
                    } else {
                        parts[index] = (parts[index] as UIMessagePart.Tool).merge(deltaPart)
                    }
                }
            }

            is UIMessagePart.Image -> {
                closeText()
                closeReasoning(parts)
                val id = imageId ?: nextId(SEGMENT_IMAGE).also { imageId = it }
                val index = imagePartIndexes[id]
                val existing = index?.let { parts.getOrNull(it) as? UIMessagePart.Image }
                if (index != null && existing != null) {
                    parts[index] = existing.copy(
                        url = existing.url + deltaPart.url,
                        metadata = deltaPart.metadata ?: existing.metadata,
                    )
                } else {
                    imagePartIndexes[id] = parts.size
                    // 首片补 data uri 前缀，后续分片追加到同一个 url（流式 base64）。
                    parts.add(
                        UIMessagePart.Image(
                            url = "data:image/png;base64,${deltaPart.url}",
                            metadata = deltaPart.metadata,
                        )
                    )
                }
            }

            else -> return
        }
    }

    /** 关掉当前文本段。文本没有 finishedAt，只需要让后续增量另开一段。 */
    private fun closeText() {
        textId?.let { textPartIndexes.remove(it) }
        textId = null
    }

    /** 关掉当前思考段，并把它的 finishedAt 补上（UI 靠它结束「思考中」）。 */
    private fun closeReasoning(parts: MutableList<UIMessagePart>) {
        val id = reasoningId ?: return
        reasoningId = null
        val index = reasoningPartIndexes.remove(id) ?: return
        val reasoning = parts.getOrNull(index) as? UIMessagePart.Reasoning ?: return
        if (reasoning.finishedAt == null) {
            parts[index] = reasoning.copy(finishedAt = Clock.System.now())
        }
    }

    private fun closeImage() {
        imageId?.let { imagePartIndexes.remove(it) }
        imageId = null
    }

    private fun nextId(kind: String) = "$kind-${++sequence}"

    private companion object {
        const val SEGMENT_TEXT = "text"
        const val SEGMENT_REASONING = "reasoning"
        const val SEGMENT_IMAGE = "image"
    }
}
