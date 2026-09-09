/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.ai.tools

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import me.rerere.ai.core.InputSchema
import me.rerere.ai.core.Tool
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.model.AssistantMemory
import me.rerere.rikkahub.data.model.MemoryCategory
import me.rerere.rikkahub.utils.toLocalString
import java.time.LocalDate

fun buildMemoryTools(
    json: Json,
    onCreation: suspend (String, MemoryCategory, Int) -> AssistantMemory,
    onUpdate: suspend (Int, String, MemoryCategory?, Int?) -> AssistantMemory,
    onDelete: suspend (Int) -> Unit
): List<Tool> = listOf(
    Tool(
        name = "memory_tool",
        description = """
            The memory tool stores long-term information across conversations.
            Use `action` to control the operation: `create` (add), `edit` (update), `delete` (remove).
            - No relevant record: `create` + `content`
            - Existing relevant record: `edit` + `id` + `content`
            - Outdated/irrelevant record: `delete` + `id`
            Memories will automatically appear in the <memories> tag in later conversations.
            Do not store sensitive information (e.g., ethnicity, religion, sexual orientation, political views, sex life, criminal records).
            You may store: preferred name, preferences, plans, work-related notes, chat style preferences, first chat time, etc.
            Do not show memory content directly in the conversation unless the user explicitly asks.
            Today is ${LocalDate.now().toLocalString(true)}.
            Similar memories should be merged; prefer updating existing records.

            Optional `category` groups the record: `preference` (likes/dislikes/style),
            `fact` (stable facts about the user), `plan` (future intentions/schedule),
            `relation` (people around the user), `event` (something that happened),
            `general` (anything else). Defaults to `general`.
            Optional `priority` marks how important the record is: 0 normal, 1 important, 2 critical.
            Use 2 only for information that must never be forgotten (e.g., the user's preferred name,
            hard constraints, health-related restrictions). Defaults to 0.

            Examples:
            {"action":"create","content":"User prefers brief replies and is more active on weekends.","category":"preference"}
            {"action":"create","content":"User's preferred name is “A-Xing”.","category":"fact","priority":2}
            {"action":"edit","id":12,"content":"User’s preferred name updated to “A-Xing”, prefers Chinese replies.","priority":2}
            {"action":"delete","id":7}
        """.trimIndent(),
        parameters = {
            InputSchema.Obj(
                properties = buildJsonObject {
                    put("action", buildJsonObject {
                        put("type", "string")
                        put(
                            "enum",
                            buildJsonArray {
                                add("create")
                                add("edit")
                                add("delete")
                            }
                        )
                        put("description", "Operation to perform: create, edit, or delete")
                    })
                    put("id", buildJsonObject {
                        put("type", "integer")
                        put("description", "The id of the memory record (required for edit/delete)")
                    })
                    put("content", buildJsonObject {
                        put("type", "string")
                        put("description", "The content of the memory record (required for create/edit)")
                    })
                    put("category", buildJsonObject {
                        put("type", "string")
                        put(
                            "enum",
                            buildJsonArray {
                                add("general")
                                add("preference")
                                add("fact")
                                add("plan")
                                add("relation")
                                add("event")
                            }
                        )
                        put("description", "Optional category of the memory record, defaults to general")
                    })
                    put("priority", buildJsonObject {
                        put("type", "integer")
                        put("description", "Optional importance: 0 normal, 1 important, 2 critical. Defaults to 0")
                    })
                },
                required = listOf("action")
            )
        },
        execute = {
            val params = it.jsonObject
            val action = params["action"]?.jsonPrimitive?.contentOrNull ?: error("action is required")
            val categoryParam = params["category"]?.jsonPrimitive?.contentOrNull
                ?.let { MemoryCategory.fromSerialName(it) }
            val priorityParam = params["priority"]?.jsonPrimitive?.intOrNull?.coerceIn(0, 2)
            val payload = when (action) {
                "create" -> {
                    val content = params["content"]?.jsonPrimitive?.contentOrNull ?: error("content is required")
                    val memory = onCreation(
                        content,
                        categoryParam ?: MemoryCategory.GENERAL,
                        priorityParam ?: 0
                    )
                    json.encodeToJsonElement(AssistantMemory.serializer(), memory)
                }

                "edit" -> {
                    val id = params["id"]?.jsonPrimitive?.intOrNull ?: error("id is required")
                    val content = params["content"]?.jsonPrimitive?.contentOrNull ?: error("content is required")
                    val memory = onUpdate(id, content, categoryParam, priorityParam)
                    json.encodeToJsonElement(AssistantMemory.serializer(), memory)
                }

                "delete" -> {
                    val id = params["id"]?.jsonPrimitive?.intOrNull ?: error("id is required")
                    onDelete(id)
                    buildJsonObject {
                        put("success", true)
                        put("id", id)
                    }
                }

                else -> error("unknown action: $action, must be one of [create, edit, delete]")
            }
            listOf(UIMessagePart.Text(payload.toString()))
        }
    )
)
