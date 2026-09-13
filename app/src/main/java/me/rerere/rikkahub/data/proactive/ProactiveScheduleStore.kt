/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 自主唤醒机制参考 rikkahub-Jude (https://github.com/Lin-chpin/rikkahub-Jude)，同为 AGPL v3
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.proactive

import android.content.Context
import androidx.core.content.edit
import me.rerere.rikkahub.utils.JsonInstant

/**
 * 自主唤醒计划的持久化。
 *
 * 单独一个 SharedPreferences，不混进 Settings：这是 AI 自己写的运行时状态，
 * 跟用户配置的生命周期不一样（清空计划不该动用户的设置，反之亦然）。
 * 按助手分键，避免多个助手互相覆盖对方的计划。
 */
class ProactiveScheduleStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun read(assistantId: String?): ProactiveSchedulePlan? {
        val raw = prefs.getString(planKey(assistantId), null) ?: return null
        // 解析失败当作没有计划：宁可丢一次唤醒，也不能让脏数据每次都抛异常
        return runCatching { JsonInstant.decodeFromString<ProactiveSchedulePlan>(raw) }.getOrNull()
    }

    fun replace(plan: ProactiveSchedulePlan) {
        val normalized = plan.copy(wakeAtMillis = plan.wakeAtMillis.distinct().sorted())
        prefs.edit {
            putString(planKey(plan.assistantId), JsonInstant.encodeToString(normalized))
        }
    }

    fun clear(assistantId: String?) {
        prefs.edit { remove(planKey(assistantId)) }
    }

    /** 下一次自主唤醒的时间戳；没有计划返回 null。 */
    fun nextWakeAtMillis(assistantId: String?): Long? =
        read(assistantId)?.wakeAtMillis?.minOrNull()

    /**
     * 消费已到期的唤醒点，并推进重复计划。
     *
     * @return true 表示这次触发命中了自主计划（调用方据此判断是不是该按计划发言）。
     *
     * 一次性计划的时间点用完就删掉整个计划；重复计划算出下一次。
     * 注意重复计划从 now 起算而不是从原定时间起算：设备睡了很久才醒来时，
     * 从原定时间累加会一次性补出很多个已经过去的时间点。
     */
    fun consumeDue(
        assistantId: String?,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean {
        val plan = read(assistantId) ?: return false
        if (plan.wakeAtMillis.none { it <= nowMillis }) return false

        val remaining = plan.wakeAtMillis.filter { it > nowMillis }
        val next = when {
            remaining.isNotEmpty() -> remaining
            plan.repeatIntervalMinutes != null && plan.repeatIntervalMinutes > 0L ->
                listOf(nowMillis + plan.repeatIntervalMinutes * 60_000L)
            plan.recurrence != null && plan.recurrenceTimeMinutes != null -> listOf(
                ProactiveSchedulePlanner.nextCalendarWakeAt(
                    recurrence = plan.recurrence,
                    timeMinutes = plan.recurrenceTimeMinutes,
                    nowMillis = nowMillis,
                )
            )
            else -> emptyList()
        }

        if (next.isEmpty()) {
            clear(assistantId)
        } else {
            replace(plan.copy(wakeAtMillis = next))
        }
        return true
    }

    private fun planKey(assistantId: String?): String =
        assistantId?.takeIf { it.isNotBlank() }?.let { "${PLAN_KEY}_$it" } ?: PLAN_KEY

    companion object {
        private const val PREFS_NAME = "proactive_schedule_prefs"
        private const val PLAN_KEY = "plan"
    }
}
