/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.ai

import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import me.rerere.rikkahub.data.datastore.DisplaySetting
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.AssistantMemory
import me.rerere.rikkahub.data.repository.ConversationRepository
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

/**
 * 记忆注入的字符预算上限。
 *
 * 记忆是只增不减的：助手每聊几轮就可能记一条，半年后攒到几百条，
 * 而它们每一轮都要整份塞进 system prompt。没有上限的话既白烧 token，
 * 又会把真正的对话历史挤出上下文窗口 —— 而且挤掉的总是最近的对话，
 * 因为 system prompt 在最前面，不会被上游截断。
 *
 * 所以按优先级从高到低填，填不下就停，并在末尾如实说明省略了多少条。
 * 宁可让模型知道"还有一些记忆没给你"，也不要静默丢弃。
 */
private const val MEMORY_PROMPT_BUDGET_CHARS = 6000

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
        val sorted = memories.sortedWith(
            compareByDescending<AssistantMemory> { it.priority }
                .thenBy { it.id }
        )
        // 按优先级从高到低填进预算，超出即停：低优先级的记忆先被舍弃。
        val included = mutableListOf<AssistantMemory>()
        var usedChars = 0
        for (memory in sorted) {
            // +32 粗算这条记忆的 JSON 外壳（id / category / priority 三个字段与括号引号）
            val cost = memory.content.length + 32
            if (usedChars + cost > MEMORY_PROMPT_BUDGET_CHARS && included.isNotEmpty()) break
            included += memory
            usedChars += cost
        }
        val json = buildJsonArray {
            included.forEach { memory ->
                add(buildJsonObject {
                    put("id", memory.id)
                    put("content", memory.content)
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
