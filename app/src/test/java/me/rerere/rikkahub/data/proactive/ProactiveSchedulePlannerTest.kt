/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.proactive

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.random.Random

class ProactiveSchedulePlannerTest {

    private val planner = ProactiveSchedulePlanner(Random(42))
    private val now = 1_700_000_000_000L

    @Test
    fun `mode at schedules one wake`() {
        val at = Instant.ofEpochMilli(now + 3_600_000L).toString()
        val plan = planner.create(
            ProactiveScheduleRequest(mode = "at", at = at),
            assistantId = "a1",
            nowMillis = now,
        )
        assertEquals(1, plan.wakeAtMillis.size)
        assertEquals(now + 3_600_000L, plan.wakeAtMillis[0])
        assertEquals("a1", plan.assistantId)
        assertTrue(!plan.isRecurring)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `mode at rejects a past timestamp`() {
        planner.create(
            ProactiveScheduleRequest(
                mode = "at",
                at = Instant.ofEpochMilli(now - 1000L).toString(),
            ),
            assistantId = null,
            nowMillis = now,
        )
    }

    @Test
    fun `one-off interval with count spaces the wakes out`() {
        val plan = planner.create(
            ProactiveScheduleRequest(mode = "interval", intervalMinutes = 10, count = 3),
            assistantId = null,
            nowMillis = now,
        )
        assertEquals(
            listOf(now + 600_000L, now + 1_200_000L, now + 1_800_000L),
            plan.wakeAtMillis,
        )
        // 一次性计划不该带重复参数
        assertNull(plan.repeatIntervalMinutes)
    }

    @Test
    fun `recurring interval only stores the next wake`() {
        val plan = planner.create(
            ProactiveScheduleRequest(
                mode = "interval",
                intervalMinutes = 45,
                triggerType = "recurring",
            ),
            assistantId = null,
            nowMillis = now,
        )
        assertEquals(listOf(now + 45 * 60_000L), plan.wakeAtMillis)
        assertEquals(45L, plan.repeatIntervalMinutes)
        assertTrue(plan.isRecurring)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `interval rejects a non-positive value`() {
        planner.create(
            ProactiveScheduleRequest(mode = "interval", intervalMinutes = 0),
            assistantId = null,
            nowMillis = now,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `count above the cap is rejected`() {
        planner.create(
            ProactiveScheduleRequest(
                mode = "interval",
                intervalMinutes = 5,
                count = ProactiveSchedulePlanner.MAX_COUNT + 1,
            ),
            assistantId = null,
            nowMillis = now,
        )
    }

    @Test
    fun `random window produces distinct sorted times inside the window`() {
        val start = Instant.ofEpochMilli(now + 3_600_000L).toString()
        val end = Instant.ofEpochMilli(now + 7_200_000L).toString()
        val plan = planner.create(
            ProactiveScheduleRequest(
                mode = "random_window",
                windowStart = start,
                windowEnd = end,
                count = 4,
            ),
            assistantId = null,
            nowMillis = now,
        )
        assertEquals(4, plan.wakeAtMillis.size)
        assertEquals(plan.wakeAtMillis.distinct(), plan.wakeAtMillis)
        assertEquals(plan.wakeAtMillis.sorted(), plan.wakeAtMillis)
        assertTrue(plan.wakeAtMillis.all { it in (now + 3_600_000L)..(now + 7_200_000L) })
    }

    @Test(expected = IllegalStateException::class)
    fun `random window requires count`() {
        // 窗口本身合法（远在最小提前量之外），失败必须是因为缺 count
        planner.create(
            ProactiveScheduleRequest(
                mode = "random_window",
                windowStart = Instant.ofEpochMilli(now + 3_600_000L).toString(),
                windowEnd = Instant.ofEpochMilli(now + 7_200_000L).toString(),
            ),
            assistantId = null,
            nowMillis = now,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `random window rejects an end before now`() {
        planner.create(
            ProactiveScheduleRequest(
                mode = "random_window",
                windowStart = Instant.ofEpochMilli(now - 100_000L).toString(),
                windowEnd = Instant.ofEpochMilli(now - 1000L).toString(),
                count = 1,
            ),
            assistantId = null,
            nowMillis = now,
        )
    }

    @Test
    fun `calendar daily picks the next occurrence`() {
        val plan = planner.create(
            ProactiveScheduleRequest(
                mode = "calendar",
                triggerType = "recurring",
                recurrence = "daily",
                time = "21:00",
            ),
            assistantId = null,
            nowMillis = now,
        )
        assertEquals("daily", plan.recurrence)
        assertEquals(21 * 60, plan.recurrenceTimeMinutes)
        assertEquals(1, plan.wakeAtMillis.size)
        assertTrue(plan.wakeAtMillis[0] > now)
        val zoned = Instant.ofEpochMilli(plan.wakeAtMillis[0]).atZone(ZoneId.systemDefault())
        assertEquals(LocalTime.of(21, 0), zoned.toLocalTime())
    }

    @Test(expected = IllegalArgumentException::class)
    fun `calendar requires the recurring trigger type`() {
        planner.create(
            ProactiveScheduleRequest(mode = "calendar", recurrence = "daily", time = "21:00"),
            assistantId = null,
            nowMillis = now,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `calendar rejects a malformed time`() {
        planner.create(
            ProactiveScheduleRequest(
                mode = "calendar",
                triggerType = "recurring",
                recurrence = "daily",
                time = "9pm",
            ),
            assistantId = null,
            nowMillis = now,
        )
    }

    @Test
    fun `weekdays recurrence never lands on a weekend`() {
        // 从一个周六出发，下一次必须是周一
        val saturday = ZonedDateTime.of(2026, 2, 21, 12, 0, 0, 0, ZoneId.systemDefault())
        assertEquals(6, saturday.dayOfWeek.value)
        val next = ProactiveSchedulePlanner.nextCalendarWakeAt(
            recurrence = "weekdays",
            timeMinutes = 9 * 60,
            nowMillis = saturday.toInstant().toEpochMilli(),
        )
        val nextDay = Instant.ofEpochMilli(next).atZone(ZoneId.systemDefault())
        assertTrue(nextDay.dayOfWeek.value.toString(), nextDay.dayOfWeek.value in 1..5)
    }

    @Test(expected = IllegalStateException::class)
    fun `unknown mode is rejected`() {
        planner.create(
            ProactiveScheduleRequest(mode = "whenever"),
            assistantId = null,
            nowMillis = now,
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `unknown trigger type is rejected`() {
        planner.create(
            ProactiveScheduleRequest(mode = "interval", intervalMinutes = 5, triggerType = "maybe"),
            assistantId = null,
            nowMillis = now,
        )
    }
}
