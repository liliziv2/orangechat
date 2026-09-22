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
import me.rerere.rikkahub.data.model.domainToken
import me.rerere.rikkahub.data.model.memoryPriorityToImportance
import me.rerere.rikkahub.data.service.MemoryBankService
import me.rerere.rikkahub.utils.toLocalString
import java.time.LocalDate

/** `source_type`：这条记忆是对话中的 `memory_tool` 写进来的。 */
const val MEMORY_SOURCE_CHAT_TOOL = "chat_tool"

/**
 * 把工具收到的一次写入转成 [MemoryBankService.MemoryWriteRequest]。
 *
 * 转换放在 tools 包而不是 service 包，是为了让 `data/service` 不反向依赖 `data/ai/tools`。
 */
fun MemoryDraft.toWriteRequest(
    assistantId: String?,
    sourceType: String = MEMORY_SOURCE_CHAT_TOOL,
): MemoryBankService.MemoryWriteRequest = MemoryBankService.MemoryWriteRequest(
    content = content,
    factTrack = factTrack,
    feelTrack = feelTrack,
    type = "message",
    assistantId = assistantId,
    domain = listOfNotNull(category.domainToken),
    importance = memoryPriorityToImportance(priority),
    sourceType = sourceType,
)

/**
 * 一次记忆写入要带的东西。
 *
 * 抽成数据类是因为双轨把参数从 3 个涨到 5 个，位置参数已经读不出谁是谁了。
 *
 * [factTrack] / [feelTrack] 是**硬要求**，不是风格建议。Elektron 的 DECISIONS 记着
 * 这条是被纠正出来的：原先"事实一条、感受一条分开记"，结果记出来一堆只有事实的条目 ——
 * 事实好记（有客观依据），感受麻烦（要回到现场焐着），分轨等于给了偷懒的口子。
 * 缺轨由 `MemoryBankService.writeMemory` 在写入侧拒绝。
 */
data class MemoryDraft(
    /** 原始现场：原话、语气、当时发生了什么。 */
    val content: String,
    /** 事实轨：行为、时间线、承诺。 */
    val factTrack: String,
    /** 情绪轨：感受、温度、影响。 */
    val feelTrack: String,
    val category: MemoryCategory = MemoryCategory.GENERAL,
    /** 0=普通，1=重要，2=关键。落到 memory_bank.importance 的 5/7/10。 */
    val priority: Int = 0,
)

fun buildMemoryTools(
    json: Json,
    onCreation: suspend (MemoryDraft) -> AssistantMemory,
    onUpdate: suspend (Int, MemoryDraft) -> AssistantMemory,
    onDelete: suspend (Int) -> Unit
): List<Tool> = listOf(
    Tool(
        name = "memory_tool",
        description = """
            The memory tool stores long-term information across conversations.
            Use `action` to control the operation: `create` (add), `edit` (update), `delete` (remove).
            - No relevant record: `create` + `content` + `fact_track` + `feel_track`
            - Existing relevant record: `edit` + `id` + `content` + `fact_track` + `feel_track`
            - Outdated/irrelevant record: `delete` + `id`
            Memories will automatically appear in the <memories> tag in later conversations.
            Do not store sensitive information (e.g., ethnicity, religion, sexual orientation, political views, sex life, criminal records).
            You may store: preferred name, preferences, plans, work-related notes, chat style preferences, first chat time, etc.
            Do not show memory content directly in the conversation unless the user explicitly asks.
            Today is ${LocalDate.now().toLocalString(true)}.
            Similar memories should be merged; prefer updating existing records.

            ## Every memory has three parts, and all three are required

            `content` is the raw scene: what was said, in what tone, what was going on.
            Keep it close to the original moment — once the scene is gone it cannot be reconstructed.

            `fact_track` is the factual side: what was done, the timeline, what was promised.
            `feel_track` is the affective side: how it felt, the warmth, what it changed.

            `fact_track` and `feel_track` are NOT two separate memories — they are two faces of
            the SAME memory. Writing only one of them is rejected, on purpose. The reason is
            recorded in the project's own decisions: when facts and feelings were allowed to be
            stored separately, what got written was almost always only the facts, because facts
            are easy (they have evidence) and feelings are not (they require going back to the
            moment). Do not treat this as a formatting preference. If you genuinely cannot name
            a feeling, say so in `feel_track` in plain words rather than leaving it empty.

            Optional `category` groups the record: `preference` (likes/dislikes/style),
            `fact` (stable facts about the user), `plan` (future intentions/schedule),
            `relation` (people around the user), `event` (something that happened),
            `general` (anything else). Defaults to `general`.
            Optional `priority` marks how important the record is: 0 normal, 1 important, 2 critical.
            Use 2 only for information that must never be forgotten (e.g., the user's preferred name,
            hard constraints, health-related restrictions). Defaults to 0.

            Examples:
            {"action":"create","content":"User asked me to keep replies short, and mentioned they usually write late at night.","fact_track":"User prefers brief replies; they are usually active late at night.","feel_track":"They seemed a little tired but relaxed, like someone winding down rather than someone in a hurry.","category":"preference"}
            {"action":"create","content":"User said their preferred name is “A-Xing” and asked me to use it from now on.","fact_track":"User's preferred name is “A-Xing”; use it in future replies.","feel_track":"They brought it up themselves and sounded pleased about it — this matters to them.","category":"fact","priority":2}
            {"action":"edit","id":12,"content":"User corrected their preferred name to “A-Xing” and added that they prefer Chinese replies.","fact_track":"Preferred name is “A-Xing”; replies should be in Chinese.","feel_track":"The correction was matter-of-fact, not annoyed — just making sure I had it right.","priority":2}
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
                        put("description", "The raw scene: what was said, in what tone, what was going on. Required for create/edit.")
                    })
                    put("fact_track", buildJsonObject {
                        put("type", "string")
                        put("description", "The factual side of the SAME memory: what was done, the timeline, what was promised. Required for create/edit, and must not be empty.")
                    })
                    put("feel_track", buildJsonObject {
                        put("type", "string")
                        put("description", "The affective side of the SAME memory: how it felt, the warmth, what it changed. Required for create/edit, and must not be empty.")
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

            // 三样都读出来再判空。双轨是硬要求，缺轨时抛出的信息要让模型看懂该补哪个字段，
            // 而不是笼统地回一句 "content is required"。
            fun requiredField(name: String): String {
                val value = params[name]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
                if (value.isEmpty()) {
                    error(
                        "$name is required and must not be empty. Every memory carries the raw " +
                            "scene (content), the factual side (fact_track) and the affective " +
                            "side (feel_track) together — they are two faces of the same memory, " +
                            "not two separate records."
                    )
                }
                return value
            }

            fun draftFromParams(): MemoryDraft = MemoryDraft(
                content = requiredField("content"),
                factTrack = requiredField("fact_track"),
                feelTrack = requiredField("feel_track"),
                category = categoryParam ?: MemoryCategory.GENERAL,
                priority = priorityParam ?: 0,
            )

            val payload = when (action) {
                "create" -> {
                    val memory = onCreation(draftFromParams())
                    json.encodeToJsonElement(AssistantMemory.serializer(), memory)
                }

                "edit" -> {
                    val id = params["id"]?.jsonPrimitive?.intOrNull ?: error("id is required")
                    val memory = onUpdate(id, draftFromParams())
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
