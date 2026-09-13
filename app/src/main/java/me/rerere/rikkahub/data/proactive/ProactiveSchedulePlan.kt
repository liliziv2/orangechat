/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 自主唤醒机制参考 rikkahub-Jude (https://github.com/Lin-chpin/rikkahub-Jude)，同为 AGPL v3
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.proactive

import kotlinx.serialization.Serializable
import java.time.Instant
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.random.Random

/**
 * AI 自己排定的唤醒计划。
 *
 * 与用户设置里的随机间隔（[me.rerere.rikkahub.data.datastore.ProactiveMessageSetting]）并存：
 * 调度时取两者中较早的那个，所以开了自主计划不会让常规主动消息停摆。
 *
 * [wakeAtMillis] 是绝对时间戳的列表（升序去重）。重复计划只保留下一次的时间，
 * 到期后由 [ProactiveScheduleStore.consumeDue] 算出再下一次。
 */
@Serializable
data class ProactiveSchedulePlan(
    val assistantId: String? = null,
    val wakeAtMillis: List<Long> = emptyList(),
    /** interval 重复模式的间隔分钟数；null 表示不是这种重复。 */
    val repeatIntervalMinutes: Long? = null,
    /** calendar 重复模式：daily / weekdays。 */
    val recurrence: String? = null,
    /** calendar 重复模式的当天时刻，以从 00:00 起的分钟数表示。 */
    val recurrenceTimeMinutes: Int? = null,
    val createdAtMillis: Long = 0L,
) {
    val isRecurring: Boolean get() = repeatIntervalMinutes != null || recurrence != null
}

/** 模型传进来的原始参数，尚未换算成时间点。 */
data class ProactiveScheduleRequest(
    val mode: String,
    val at: String? = null,
    val windowStart: String? = null,
    val windowEnd: String? = null,
    val intervalMinutes: Long? = null,
    val count: Int? = null,
    val triggerType: String = ProactiveSchedulePlanner.TRIGGER_ONCE,
    val recurrence: String? = null,
    val time: String? = null,
)

/**
 * 把模型给的参数换算成具体的未来唤醒时间。
 *
 * 全部用 require/error 抛异常而不是返回 null：工具执行框架会把异常信息回给模型，
 * 让它知道自己哪个参数写错了并重试，比静默失败有用。
 */
class ProactiveSchedulePlanner(
    private val random: Random = Random.Default,
) {
    fun create(
        request: ProactiveScheduleRequest,
        assistantId: String?,
        nowMillis: Long = System.currentTimeMillis(),
    ): ProactiveSchedulePlan {
        require(request.triggerType in setOf(TRIGGER_ONCE, TRIGGER_RECURRING)) {
            "trigger_type must be once or recurring"
        }
        val recurring = request.triggerType == TRIGGER_RECURRING
        val calendarTimeMinutes = if (request.mode == MODE_CALENDAR) {
            parseLocalTime(request.time).toSecondOfDay() / 60
        } else {
            null
        }

        val wakeTimes = when (request.mode) {
            MODE_AT -> {
                require(!recurring) { "recurring schedules need mode=interval or mode=calendar" }
                listOf(requireFuture(parseTimestamp(request.at), nowMillis))
            }

            MODE_INTERVAL -> createIntervalTimes(request, nowMillis, recurring)

            MODE_RANDOM_WINDOW -> {
                require(!recurring) { "random_window only works with trigger_type=once" }
                createRandomTimes(request, nowMillis)
            }

            MODE_CALENDAR -> {
                require(recurring) { "calendar schedules require trigger_type=recurring" }
                listOf(
                    nextCalendarWakeAt(
                        recurrence = request.recurrence,
                        timeMinutes = requireNotNull(calendarTimeMinutes),
                        nowMillis = nowMillis,
                    )
                )
            }

            else -> error("mode must be at, interval, random_window or calendar")
        }

        return ProactiveSchedulePlan(
            assistantId = assistantId,
            wakeAtMillis = wakeTimes.distinct().sorted(),
            repeatIntervalMinutes = if (request.mode == MODE_INTERVAL && recurring) {
                requireInterval(request.intervalMinutes)
            } else {
                null
            },
            recurrence = if (request.mode == MODE_CALENDAR) request.recurrence else null,
            recurrenceTimeMinutes = calendarTimeMinutes,
            createdAtMillis = nowMillis,
        )
    }

    private fun createIntervalTimes(
        request: ProactiveScheduleRequest,
        nowMillis: Long,
        recurring: Boolean,
    ): List<Long> {
        val interval = requireInterval(request.intervalMinutes)
        // 重复模式只排下一次，后续由 consumeDue 顺延，免得一次写进去几十个时间点
        if (recurring) return listOf(nowMillis + interval * 60_000L)
        val count = request.count ?: 1
        require(count in 1..MAX_COUNT) { "count must be between 1 and $MAX_COUNT" }
        return (1..count).map { nth -> nowMillis + interval * nth * 60_000L }
    }

    private fun createRandomTimes(
        request: ProactiveScheduleRequest,
        nowMillis: Long,
    ): List<Long> {
        val start = parseTimestamp(request.windowStart)
        val end = parseTimestamp(request.windowEnd)
        // 窗口起点可能已经过去（"今天下午"在下午说的），从现在往后算
        val effectiveStart = maxOf(start, nowMillis + MIN_LEAD_MILLIS)
        require(end > effectiveStart) { "window_end must be after both now and window_start" }
        val count = request.count ?: error("count is required for random_window")
        require(count in 1..MAX_COUNT) { "count must be between 1 and $MAX_COUNT" }
        // 窗口太窄时取不到 count 个不同的毫秒值，会死循环
        require(end - effectiveStart >= count) { "the random window is too small for count" }
        return buildSet {
            while (size < count) {
                add(random.nextLong(effectiveStart, end))
            }
        }.sorted()
    }

    private fun requireInterval(value: Long?): Long = requireNotNull(value) {
        "interval_minutes is required for mode=interval"
    }.also {
        require(it > 0L) { "interval_minutes must be greater than zero" }
    }

    private fun requireFuture(value: Long, nowMillis: Long): Long {
        require(value > nowMillis) { "at must be in the future" }
        return value
    }

    /** 接受 Instant / 带时区 / 不带时区三种写法，模型对格式的选择并不稳定。 */
    private fun parseTimestamp(value: String?): Long {
        val input = value?.trim().takeIf { !it.isNullOrEmpty() }
            ?: error("a timestamp is required")
        return runCatching { Instant.parse(input).toEpochMilli() }
            .recoverCatching { ZonedDateTime.parse(input).toInstant().toEpochMilli() }
            .recoverCatching {
                LocalDateTime.parse(input).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
            .getOrElse { error("invalid timestamp: $input") }
    }

    private fun parseLocalTime(value: String?): LocalTime {
        val input = value?.trim().takeIf { !it.isNullOrEmpty() }
            ?: error("time is required in HH:mm format")
        val parts = input.split(':')
        require(parts.size == 2) { "time must use HH:mm format" }
        val hour = parts[0].toIntOrNull() ?: error("time must use HH:mm format")
        val minute = parts[1].toIntOrNull() ?: error("time must use HH:mm format")
        require(hour in 0..23 && minute in 0..59) { "time must use HH:mm format" }
        return LocalTime.of(hour, minute)
    }

    companion object {
        const val MODE_AT = "at"
        const val MODE_INTERVAL = "interval"
        const val MODE_RANDOM_WINDOW = "random_window"
        const val MODE_CALENDAR = "calendar"
        const val TRIGGER_ONCE = "once"
        const val TRIGGER_RECURRING = "recurring"
        const val RECURRENCE_DAILY = "daily"
        const val RECURRENCE_WEEKDAYS = "weekdays"

        /** 一次最多排多少个时间点，防止模型写出 count=1000 把闹钟刷爆。 */
        const val MAX_COUNT = 24

        /**
         * 最小提前量。
         *
         * 不让 AI 排出"立刻触发"的计划：那会在用户还在打字时插一条主动消息，
         * 观感像是抢话。上限不限制 —— AI 说三天后就三天后。
         */
        const val MIN_LEAD_MILLIS = 60_000L

        /**
         * 算出下一个符合 recurrence 的时刻。
         *
         * 从今天开始往后找最多 8 天：daily 最多找 1 天，weekdays 遇到周末最多顺延到周一，
         * 8 天足够覆盖并且能在数据异常时跳出循环。
         */
        fun nextCalendarWakeAt(
            recurrence: String?,
            timeMinutes: Int,
            nowMillis: Long,
        ): Long {
            require(recurrence == RECURRENCE_DAILY || recurrence == RECURRENCE_WEEKDAYS) {
                "recurrence must be daily or weekdays"
            }
            require(timeMinutes in 0 until 24 * 60) { "time must be between 00:00 and 23:59" }
            val localTime = LocalTime.of(timeMinutes / 60, timeMinutes % 60)
            val zone = ZoneId.systemDefault()
            var date = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate()
            repeat(8) {
                val allowed = recurrence == RECURRENCE_DAILY || date.dayOfWeek.value in 1..5
                if (allowed) {
                    val candidate = ZonedDateTime.of(date, localTime, zone).toInstant().toEpochMilli()
                    if (candidate > nowMillis) return candidate
                }
                date = date.plusDays(1)
            }
            error("could not find the next calendar wake time")
        }
    }
}
