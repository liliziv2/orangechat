/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.service

import android.content.Context
import androidx.core.content.edit
import kotlinx.serialization.Serializable
import me.rerere.rikkahub.utils.JsonInstant

/**
 * 唤醒桥的**运行时状态**：闩锁（latch）、初始化检查点、下一次唤醒时刻。
 *
 * 对应 Elektron 的 `behavior/emotion-trigger-state.json`。
 *
 * ## 为什么不放进 Room
 *
 * 这里存的是"桥自己的判断状态"，不是状态层的真相。它有两个特殊要求：
 *
 * 1. **`ProactiveMessageService.scheduleNext` 要同步读**。那个函数是普通函数
 *    （被 `BroadcastReceiver.onReceive`、`RikkoHubApp.onCreate` 这些非挂起上下文调用），
 *    没法等一个 suspend 的 Room 查询。SharedPreferences 是同步的，正好合适 ——
 *    `ProactiveScheduleStore` 出于同一个理由也是 SharedPreferences。
 * 2. 它是**可以丢的**。丢了的后果只是"可能多醒一次"或"这一次没提前醒"，
 *    不会让队列里的唤醒消失（那些在 `impulse_queue` 里，是真相）。
 *
 * ## 闩锁为什么不能省
 *
 * 没有闩锁的话，"某一维长期高于阈值"会变成**每一拍都唤醒一次** ——
 * 这正是任务书第 7 条点名要防的事。闩锁的语义是：
 * 「这一维越过这条线」这件事只算一次，直到它**落回线下**（被清出闩锁）之后
 * 再次越过，才算新的一次。`behavior.py` 的
 * `latched = {k: v for k, v in latched.items() if k in active_keys}` 就是在做这件事。
 */
class EmotionWakeStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** 一条闩锁记录：上次是哪一刻、当时的值是多少。 */
    @Serializable
    data class Latch(val value: Double, val at: Long)

    /** 桥的完整运行时状态。 */
    @Serializable
    data class State(
        /**
         * 有没有建立过检查点。
         *
         * 首次运行为 false —— 此时**只建立闩锁、不产生唤醒**。
         * 这就是任务书第 8 条"首次运行建立 checkpoint，不重放全部历史事件"：
         * 刚装上 App 时库里可能已经有几十条历史越线，全部当成"刚刚发生"会
         * 一次性排出一堆唤醒，那既没有意义也解释不通。
         */
        val initialized: Boolean = false,
        /** 当前闩住的候选。key 形如 `absolute:curiosity` / `relative:social`。 */
        val latched: Map<String, Latch> = emptyMap(),
        /** 最近一次真的排进队列的唤醒时刻（毫秒）。最小间隔判断用。 */
        val lastWakeAt: Long = 0L,
        /**
         * 最近一次入队的唤醒的到点时刻（毫秒）。
         *
         * 这是给**现有闹钟**看的镜像：`ProactiveMessageService.scheduleNext` 拿它
         * 跟常规随机间隔、AI 自主计划比早。队列里的 `due_at` 才是真相，这里只是
         * 一个同步可读的副本；对不上最多是"晚一轮醒"，不会丢。
         */
        val nextDueAt: Long = 0L,
    )

    fun read(): State {
        val raw = prefs.getString(KEY_STATE, null) ?: return State()
        // 解析失败当作全新状态：宁可丢一次闩锁（最多多醒一次），
        // 也不能让一段脏 JSON 每次评估都抛异常把唤醒桥整个卡死。
        return runCatching { JsonInstant.decodeFromString<State>(raw) }.getOrDefault(State())
    }

    fun write(state: State) {
        prefs.edit { putString(KEY_STATE, JsonInstant.encodeToString(state)) }
    }

    /** 把闩锁换成 [latched]，其余字段不动。 */
    fun writeLatched(latched: Map<String, Latch>, initialized: Boolean = true) {
        write(read().copy(initialized = initialized, latched = latched))
    }

    /** 记录一次入队：更新最小间隔锚点与给闹钟看的到点时刻。 */
    fun writeWake(now: Long, dueAt: Long) {
        write(read().copy(lastWakeAt = now, nextDueAt = dueAt))
    }

    /** 队列已经没有待处理的了，把镜像清掉，免得闹钟一直按一个过期的时刻提前醒。 */
    fun clearNextDue() {
        val current = read()
        if (current.nextDueAt == 0L) return
        write(current.copy(nextDueAt = 0L))
    }

    /**
     * 给现有闹钟用的到点时刻。没有待处理唤醒时返回 null。
     *
     * 已经过去的时间点**不返回** —— 返回一个过去的时刻会让 `scheduleNext`
     * 把闹钟排到"立刻"，那正是 `MIN_LEAD_MILLIS` 要避免的抢话。
     */
    fun nextDueAtMillis(now: Long = System.currentTimeMillis()): Long? =
        read().nextDueAt.takeIf { it > now }

    /** 清空（调试/重置用）。 */
    fun clear() {
        prefs.edit { remove(KEY_STATE) }
    }

    private companion object {
        const val PREFS_NAME = "emotion_wake_prefs"
        const val KEY_STATE = "state"
    }
}
