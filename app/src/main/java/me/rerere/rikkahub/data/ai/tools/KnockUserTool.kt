/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.ai.tools

import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import me.rerere.ai.core.InputSchema
import me.rerere.ai.core.Tool
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.ai.tools.local.KnockOutcome
import me.rerere.rikkahub.data.ai.tools.local.KnockRequest
import me.rerere.rikkahub.data.ai.tools.local.KnockRequestBuffer
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * AI 主动敲门弹窗. 与 ask_user 的分工:
 *
 * - ask_user: 任务卡在信息不足, 必须等用户填完才能继续, 没有超时, 卡片长在消息流里.
 * - knock_user: AI 自己想找用户说句话 (提醒/邀约/确认一下), 弹窗浮在界面最上层,
 *   用户可以完全无视; 超时后工具返回「用户没回应」, 模型继续自己的一轮发言.
 *
 * 交互形状借鉴 KKarsyline/mac_knock 的 CLI 语义 (title / message / 1-3 buttons / timeout),
 * 但实现完全是本进程内的 Compose 弹窗: 同进程不需要 HTTP 桥或 MCP server.
 */
@OptIn(ExperimentalUuidApi::class)
fun createKnockUserTool(conversationId: String?): Tool = Tool(
    name = "knock_user",
    description = """
        Knock on the user's screen with a small popup you initiate yourself, on top of whatever they are doing.
        Use it when you want to reach out first - a reminder, an invitation, a quick confirmation - not when you
        are blocked on missing information (use ask_user for that). Provide 1 to ${KnockRequestBuffer.MAX_BUTTONS}
        short button labels; the label the user taps is returned to you verbatim.
        The user is free to ignore the popup: after the timeout you get responded=false with reason "no_response",
        which literally means the user did not answer. When that happens do not silently stop - say something
        yourself, the way a person would react to being left on read. Keep it to one knock at a time and do not
        knock repeatedly after being ignored.
    """.trimIndent().replace("\n", " "),
    parameters = {
        InputSchema.Obj(
            properties = buildJsonObject {
                putJsonObject("title") {
                    put("type", "string")
                    put("description", "Short popup title, a few words.")
                }
                putJsonObject("message") {
                    put("type", "string")
                    put("description", "The message body shown to the user (required).")
                }
                putJsonObject("buttons") {
                    put("type", "array")
                    put(
                        "description",
                        "1 to ${KnockRequestBuffer.MAX_BUTTONS} short button labels. Defaults to a single OK button."
                    )
                    put("items", buildJsonObject { put("type", "string") })
                }
                putJsonObject("timeout") {
                    put("type", "integer")
                    put(
                        "description",
                        "Seconds to wait before giving up, 0 means wait indefinitely. " +
                            "Default ${KnockRequestBuffer.DEFAULT_TIMEOUT_SECONDS}, " +
                            "max ${KnockRequestBuffer.MAX_TIMEOUT_SECONDS}."
                    )
                }
            },
            required = listOf("message")
        )
    },
    execute = { args ->
        val params = args.jsonObject
        val message = params["message"]?.jsonPrimitive?.contentOrNull
        if (message.isNullOrBlank()) {
            return@Tool listOf(
                UIMessagePart.Text(
                    buildJsonObject {
                        put("success", false)
                        put("error", "Missing required parameter 'message'")
                    }.toString()
                )
            )
        }

        val title = params["title"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
        val buttons = params["buttons"]?.let { element ->
            runCatching {
                element.jsonArray.mapNotNull { it.jsonPrimitive.contentOrNull?.takeIf { s -> s.isNotBlank() } }
            }.getOrNull()
        }?.take(KnockRequestBuffer.MAX_BUTTONS)?.takeIf { it.isNotEmpty() } ?: listOf("好")

        val timeoutSeconds = (params["timeout"]?.jsonPrimitive?.longOrNull ?: KnockRequestBuffer.DEFAULT_TIMEOUT_SECONDS)
            .coerceIn(0L, KnockRequestBuffer.MAX_TIMEOUT_SECONDS)
        val timeoutMs = timeoutSeconds * 1000L

        val requestId = Uuid.random().toString()
        val deferred = KnockRequestBuffer.request(
            KnockRequest(
                id = requestId,
                title = title ?: "",
                message = message,
                buttons = buttons,
                timeoutMs = timeoutMs,
                conversationId = conversationId,
            )
        )

        val outcome = if (timeoutMs <= 0L) {
            deferred.await()
        } else {
            withTimeoutOrNull(timeoutMs) { deferred.await() } ?: run {
                KnockRequestBuffer.expire(requestId)
                KnockOutcome.NoResponse
            }
        }

        val payload = buildJsonObject {
            put("success", true)
            put("title", title ?: "")
            put("message", message)
            put("buttons", buildJsonArray { buttons.forEach { add(it) } })
            when (outcome) {
                is KnockOutcome.Chosen -> {
                    put("responded", true)
                    put("choice", outcome.button)
                }

                is KnockOutcome.Dismissed -> {
                    put("responded", false)
                    put("reason", "dismissed")
                    put(
                        "note",
                        "用户划掉了弹窗, 明确不想理这次. 别再敲一次, 可以自己接一句话收场."
                    )
                }

                is KnockOutcome.NoResponse -> {
                    put("responded", false)
                    put("reason", "no_response")
                    put("waited_seconds", timeoutSeconds)
                    put(
                        "note",
                        "用户没回应. 弹窗已自动收起, 不要重复敲门; 像被无视了的人那样自己接着说点什么."
                    )
                }
            }
        }

        listOf(UIMessagePart.Text(payload.toString()))
    }
)
