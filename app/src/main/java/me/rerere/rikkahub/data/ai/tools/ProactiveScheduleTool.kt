/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 自主唤醒机制参考 rikkahub-Jude (https://github.com/Lin-chpin/rikkahub-Jude)，同为 AGPL v3
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.ai.tools

import android.content.Context
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import me.rerere.ai.core.InputSchema
import me.rerere.ai.core.Tool
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.data.proactive.ProactiveScheduleRequest
import me.rerere.rikkahub.data.proactive.ProactiveSchedulePlanner
import me.rerere.rikkahub.data.proactive.ProactiveScheduleStore

const val PROACTIVE_SCHEDULE_TOOL_NAME = "schedule_wake_up"

/**
 * 让 AI 自己决定下次什么时候来找用户。
 *
 * 橘瓣本来的主动消息只按用户设的随机区间触发（"每 30 到 90 分钟随机一次"），
 * AI 没有表达"我明天早上叫你"的手段。这个工具补上那一半：AI 排定的时间点会和
 * 常规随机间隔一起参与调度，取较早的那个，所以两者并存互不干扰。
 *
 * @param assistantId 计划按助手隔离，null 时落在公共键上（不该发生，调用方总会传）
 * @param onScheduleChanged 计划变更后重排闹钟。传进来而不是直接调 ProactiveMessageService，
 *   避免 tools 包反向依赖 service 包。
 */
fun createProactiveScheduleTool(
    context: Context,
    assistantId: String?,
    onScheduleChanged: () -> Unit,
): Tool = Tool(
    name = PROACTIVE_SCHEDULE_TOOL_NAME,
    description = """
        Decide when you will reach out to the user next, on your own initiative.
        action=set replaces your current plan, action=status shows it, action=cancel clears it.
        With trigger_type=once: mode=at wakes you at one ISO-8601 timestamp; mode=interval with
        interval_minutes and count schedules that many one-off wakes spaced by that interval;
        mode=random_window with window_start, window_end and count picks that many random moments
        inside the window, for when you want to drop in unpredictably.
        With trigger_type=recurring: mode=interval repeats forever at interval_minutes, and
        mode=calendar with recurrence=daily or recurrence=weekdays plus a local time like 21:00
        fires at that clock time every day or every workday.
        Your plan runs alongside the user's own proactive-message interval; whichever comes first wins,
        so a plan of yours never stops the regular ones. A wake must be at least one minute away.
        Only one plan exists at a time and action=set replaces it, so include everything you want in
        one call. The user's proactive-message master switch still decides whether any of this runs.
    """.trimIndent().replace("\n", " "),
    needsApproval = false,
    parameters = {
        InputSchema.Obj(
            properties = buildJsonObject {
                putJsonObject("action") {
                    put("type", "string")
                    put("enum", buildJsonArray {
                        add("set")
                        add("status")
                        add("cancel")
                    })
                    put("description", "set replaces the plan, status inspects it, cancel clears it")
                }
                putJsonObject("mode") {
                    put("type", "string")
                    put("enum", buildJsonArray {
                        add(ProactiveSchedulePlanner.MODE_AT)
                        add(ProactiveSchedulePlanner.MODE_INTERVAL)
                        add(ProactiveSchedulePlanner.MODE_RANDOM_WINDOW)
                        add(ProactiveSchedulePlanner.MODE_CALENDAR)
                    })
                }
                putJsonObject("trigger_type") {
                    put("type", "string")
                    put("enum", buildJsonArray {
                        add(ProactiveSchedulePlanner.TRIGGER_ONCE)
                        add(ProactiveSchedulePlanner.TRIGGER_RECURRING)
                    })
                    put("description", "once or recurring; defaults to once")
                }
                putJsonObject("at") {
                    put("type", "string")
                    put("description", "Future ISO-8601 timestamp, for mode=at")
                }
                putJsonObject("window_start") {
                    put("type", "string")
                    put("description", "ISO-8601 window start, for mode=random_window")
                }
                putJsonObject("window_end") {
                    put("type", "string")
                    put("description", "ISO-8601 window end, for mode=random_window")
                }
                putJsonObject("interval_minutes") {
                    put("type", "integer")
                    put("description", "Positive number of minutes, for mode=interval")
                }
                putJsonObject("count") {
                    put("type", "integer")
                    put(
                        "description",
                        "How many wakes to schedule, at most ${ProactiveSchedulePlanner.MAX_COUNT}; required for random_window"
                    )
                }
                putJsonObject("recurrence") {
                    put("type", "string")
                    put("enum", buildJsonArray {
                        add(ProactiveSchedulePlanner.RECURRENCE_DAILY)
                        add(ProactiveSchedulePlanner.RECURRENCE_WEEKDAYS)
                    })
                    put("description", "For mode=calendar")
                }
                putJsonObject("time") {
                    put("type", "string")
                    put("description", "Local time as HH:mm, for mode=calendar")
                }
            },
            required = listOf("action"),
        )
    },
    execute = { arguments ->
        val args = arguments.jsonObject
        val store = ProactiveScheduleStore(context)
        val payload = when (args["action"]?.jsonPrimitive?.contentOrNull?.lowercase()) {
            "set" -> {
                val plan = ProactiveSchedulePlanner().create(
                    request = args.toScheduleRequest(),
                    assistantId = assistantId,
                )
                store.replace(plan)
                onScheduleChanged()
                buildJsonObject {
                    put("success", true)
                    put("replaced", true)
                    putPlanFields(store, assistantId)
                }
            }

            "cancel" -> {
                val had = store.read(assistantId) != null
                store.clear(assistantId)
                onScheduleChanged()
                buildJsonObject {
                    put("success", true)
                    put("cancelled", had)
                }
            }

            "status" -> buildJsonObject { putPlanFields(store, assistantId) }

            else -> error("action must be set, status or cancel")
        }
        listOf(UIMessagePart.Text(payload.toString()))
    },
)

private fun JsonObject.toScheduleRequest() = ProactiveScheduleRequest(
    mode = this["mode"]?.jsonPrimitive?.contentOrNull.orEmpty(),
    at = this["at"]?.jsonPrimitive?.contentOrNull,
    windowStart = this["window_start"]?.jsonPrimitive?.contentOrNull,
    windowEnd = this["window_end"]?.jsonPrimitive?.contentOrNull,
    intervalMinutes = this["interval_minutes"]?.jsonPrimitive?.longOrNull,
    count = this["count"]?.jsonPrimitive?.intOrNull,
    triggerType = this["trigger_type"]?.jsonPrimitive?.contentOrNull
        ?: ProactiveSchedulePlanner.TRIGGER_ONCE,
    recurrence = this["recurrence"]?.jsonPrimitive?.contentOrNull,
    time = this["time"]?.jsonPrimitive?.contentOrNull,
)

/** 把当前计划摊平进返回值，让模型知道自己排了什么。 */
private fun kotlinx.serialization.json.JsonObjectBuilder.putPlanFields(
    store: ProactiveScheduleStore,
    assistantId: String?,
) {
    val plan = store.read(assistantId)
    put("scheduled", plan != null)
    if (plan == null) return
    put(
        "trigger_type",
        if (plan.isRecurring) {
            ProactiveSchedulePlanner.TRIGGER_RECURRING
        } else {
            ProactiveSchedulePlanner.TRIGGER_ONCE
        }
    )
    put("next_wake_at_ms", plan.wakeAtMillis.minOrNull() ?: 0L)
    put("wake_times_ms", buildJsonArray { plan.wakeAtMillis.forEach { add(it) } })
    plan.repeatIntervalMinutes?.let { put("repeat_interval_minutes", it) }
    plan.recurrence?.let { put("recurrence", it) }
    plan.recurrenceTimeMinutes?.let { put("time_minutes", it) }
}
