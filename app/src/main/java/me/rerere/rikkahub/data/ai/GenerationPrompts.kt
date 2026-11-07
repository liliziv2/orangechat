package me.rerere.rikkahub.data.ai

import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import me.rerere.rikkahub.data.datastore.DisplaySetting
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.AssistantMemory
import me.rerere.rikkahub.data.repository.ConversationRepository
import me.rerere.rikkahub.data.service.MemoryPromptBudget
import me.rerere.rikkahub.utils.JsonInstantPretty
import me.rerere.rikkahub.utils.toLocalDate

/**
 * 用户资料卡注入。昵称 / 简介 / 人设都为空时返回空串，不占用 prompt。
 */
internal fun buildUserProfilePrompt(display: DisplaySetting): String {
    val nickname = display.userNickname.trim()
    val bio = display.userBio.trim()
    val persona = display.userPersona.trim()
    if (nickname.isEmpty() && bio.isEmpty() && persona.isEmpty()) return ""
    return buildString {
        appendLine()
        append("**User Profile**")
        appendLine()
        append("This is who you are talking to. Use it naturally; do not recite it back to the user.")
        appendLine()
        if (nickname.isNotEmpty()) {
            append("- Name / nickname: ")
            append(nickname)
            appendLine()
        }
        if (bio.isNotEmpty()) {
            append("- About the user: ")
            append(bio)
            appendLine()
        }
        if (persona.isNotEmpty()) {
            append("- The role the user plays in this conversation: ")
            append(persona)
            appendLine()
        }
    }
}

internal fun buildMemoryPrompt(memories: List<AssistantMemory>) =
    buildString {
        appendLine()
        append("**Memories**")
        appendLine()
        append("These are memories stored via the memory_tool that you can reference in future conversations.")
        appendLine()
        append("They are sorted by priority (critical first). `category` is a semantic grouping;")
        appendLine()
        append("`priority` is 0=normal, 1=important, 2=critical. Treat priority=2 as never-forget facts.")
        appendLine()
        append("Each record carries `fact_track` (what was done, the timeline, what was promised) and")
        appendLine()
        append("`feel_track` (how it felt, the warmth, what it changed) alongside the raw `content`.")
        appendLine()
        append("They are two faces of the same memory, not two records. When a track is empty the")
        appendLine()
        append("record predates the two-track rule or came from a plugin — do not invent it back.")
        appendLine()
        val sorted = memories.sortedWith(
            // 与召回侧同一个排序函数：两边顺序不一致时，那边裁掉的和这边再裁掉的
            // 不是同一批，被这边砍掉的记忆会白记一次 activation。
            MemoryPromptBudget.promptOrder<AssistantMemory>(
                priorityOf = { it.priority },
                idOf = { it.id },
            )
        )
        // 按优先级从高到低填进预算，超出即停。预算值、单条开销和选取规则都在
        // [MemoryPromptBudget]：读取侧已经按同一套规则裁过一次，这里兜底且幂等。
        val included = MemoryPromptBudget.select(sorted) { memory ->
            MemoryPromptBudget.cost(memory.content, memory.factTrack, memory.feelTrack)
        }
        val json = buildJsonArray {
            included.forEach { memory ->
                add(buildJsonObject {
                    put("id", memory.id)
                    put("content", memory.content)
                    put("fact_track", memory.factTrack)
                    put("feel_track", memory.feelTrack)
                    put("category", memory.category.serialName)
                    put("priority", memory.priority)
                })
            }
        }
        append(JsonInstantPretty.encodeToString(json))
        appendLine()
        val omitted = sorted.size - included.size
        if (omitted > 0) {
            append(
                "Note: $omitted lower-priority memories were omitted to stay within the context budget. " +
                    "Use the memory tool to look up or reorganize them if needed."
            )
            appendLine()
        }
    }

internal suspend fun buildRecentChatsPrompt(
    assistant: Assistant,
    conversationRepo: ConversationRepository
): String {
    val recentConversations = conversationRepo.getRecentConversations(
        assistantId = assistant.id,
        limit = 10,
    )
    if (recentConversations.isNotEmpty()) {
        return buildString {
            appendLine()
            append("**Recent Chats**")
            appendLine()
            append("These are some of the user's recent conversations. You can use them to understand user preferences:")
            appendLine()
            val json = buildJsonArray {
                recentConversations.forEach { conversation ->
                    add(buildJsonObject {
                        put("title", conversation.title)
                        put("last_chat", conversation.updateAt.toLocalDate())
                    })
                }
            }
            append(JsonInstantPretty.encodeToString(json))
            appendLine()
        }
    }
    return ""
}
