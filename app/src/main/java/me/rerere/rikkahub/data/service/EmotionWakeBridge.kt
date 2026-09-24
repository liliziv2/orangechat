package me.rerere.rikkahub.data.service

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import me.rerere.rikkahub.utils.JsonInstant

/**
 * 一条中性唤醒的载荷。
 *
 * ## "中性"是什么意思
 *
 * 这里**只有状态描述，没有动作映射**。字段里不会出现"去发消息""去问她"这类东西，
 * 也不会有任何"建议动作"。Elektron 的原则写在 `behavior.py` 的文件头：
 * 「情绪只有唤醒权，没有行动决策权。这里读取 desire 与潮汐，在情绪越过阈值或
 * 发生明显变化时写入一条中性的 emotion_wake，并即时唤醒 Claude。具体做什么、
 * 是否暂时不做，由醒来的 Claude 结合记忆和现实边界自行判断。」
 *
 * 所以 State 层能表达的极限是「现在是什么状态、为什么变了」；
 * "要不要做点什么"是 Agent 的事，不是 State 的事。
 */
@Serializable
data class EmotionWakePayload(
    val kind: String = ImpulsePolicy.KIND_EMOTION_WAKE,
    /** 固定 `emotion`。Elektron 的载荷里也是这么写的 —— 唤醒不是某一维的私事。 */
    val drive: String = DRIVE_EMOTION,
    /** 本次唤醒里最强的那条信号的值。 */
    val value: Double,
    /** 中性说明。见类注释。 */
    val desc: String,
    /** 触发这次唤醒的全部信号。 */
    val signals: List<EmotionWakeSignal> = emptyList(),
    /** `信号 id -> 值`，一眼能看出是哪几条一起越线的。 */
    val emotionSignature: Map<String, Double> = emptyMap(),
    /** 唤醒时刻的 9 维原始值快照。 */
    val emotionSnapshot: Map<String, Double> = emptyMap(),
    /** 当时有效激活最高的那一维。 */
    val dominant: String? = null,
    /** 参照的基线是哪一种（`same_time_yesterday` / `warmup_oldest`）。null 表示历史不够、只做了绝对阈值。 */
    val baselineKind: String? = null,
    /** 基线有多老（小时）。 */
    val baselineAgeHours: Double? = null,
    /** 生成时刻（毫秒）。 */
    val bornAt: Long,
) {
    companion object {
        /** 唤醒不属于任何单一维度，载荷里用一个固定值表示。 */
        const val DRIVE_EMOTION = "emotion"

        const val DESC =
            "内部状态跨过阈值或相对基线明显上涨。这只是告诉你现在的状态是什么、为什么变了；" +
                "要不要行动、做什么，由你自己结合记忆与现实决定 —— 情绪不指定行动。"

        /** 把载荷编码成入库的 JSON。 */
        fun encode(payload: EmotionWakePayload): String =
            JsonInstant.encodeToString(serializer(), payload)

        /** 从库里读回来。坏数据返回 null，调用方退回常规路径。 */
        fun decode(json: String?): EmotionWakePayload? {
            if (json.isNullOrBlank()) return null
            return runCatching { JsonInstant.decodeFromString(serializer(), json) }.getOrNull()
        }
    }
}

/** 一条越线信号。只有观测结果，没有动作。 */
@Serializable
data class EmotionWakeSignal(
    /** `absolute:curiosity` / `relative:social` —— 闩锁也按这个 key 记。 */
    val id: String,
    val drive: String,
    /** [DriveBaseline.TRIGGER_ABSOLUTE] 或 [DriveBaseline.TRIGGER_RELATIVE]。 */
    val trigger: String,
    val value: Double,
    val threshold: Double,
    val baseline: Double? = null,
    val rise: Double? = null,
)

/**
 * 情绪唤醒桥。
 *
 * 对应 Elektron 的 `behavior/behavior.py`。它是 State 与 Behavior 之间**唯一的接缝**：
 *
 * ```
 * DriveStateService.crossings()          ← 只读观测（上一轮就做好、无副作用）
 *   → 闩锁 / 最小间隔 / 已经在队列里没？   ← 本类
 *   → 排一条中性 emotion_wake 进队列       ← ImpulseQueueService
 *   → 把到点时刻告诉现有闹钟               ← ProactiveMessageService.scheduleNext
 *   → 现有的 ProactiveMessageTriggerService 醒过来时把它领走
 * ```
 *
 * ## 这一层刻意不做什么
 *
 * - **不直接启动任何 Service**。桥只负责"发现变化、排进队列、把闹钟叫早一点"；
 *   真正把 Agent 叫起来的是现有的 `ProactiveMessageTriggerService`，
 *   它本来就是被现有闹钟链驱动的。这里没有第二套 scheduler / wake service。
 * - **不生成动作**。载荷是纯状态描述（见 [EmotionWakePayload]）。
 * - **不做语义分析**。越线判断全部来自 `DriveBaseline`，是数值比较，不是"读懂了什么"。
 *
 * ## 为什么需要闩锁 + 最小间隔 + 去重三道闸
 *
 * 任务书第 7 条：「不能因为某个 drive 长期保持高位而每次重复唤醒。」
 * 三道闸各自挡一种情况：
 *
 * | 闸 | 挡什么 |
 * |---|---|
 * | 闩锁 | 同一维持续高于阈值 → 只在**首次**越线时算一次，落回线下才重新武装 |
 * | 队列去重 | 上一次唤醒还没处理完 → 不再排新的 |
 * | 最小间隔 | 处理完没多久又越线 → 半小时内不再排 |
 */
class EmotionWakeBridge(
    private val driveStateService: DriveStateService,
    private val impulseQueueService: ImpulseQueueService,
    private val store: EmotionWakeStore,
) {

    /** 一次评估的结果。全部是"为什么没醒"或"排了哪一条"，没有动作。 */
    sealed interface Outcome {
        /** 首次运行：只建立检查点与闩锁，不产生唤醒。 */
        data class Initialized(val latched: Int) : Outcome

        /** 没有新的越线（可能是持续高位，被闩锁挡住了）。 */
        data class NoCrossing(val active: Int, val newKeys: Int) : Outcome

        /** 队列里已经有一条没处理完的唤醒。 */
        data class AlreadyPending(val impulseId: Long, val status: String) : Outcome

        /** 上一条被延后，还没到重试时刻。 */
        data class Deferred(val retryAt: Long) : Outcome

        /** 距上一次唤醒不足最小间隔。 */
        data class MinGap(val ageHours: Double) : Outcome

        /** 上一次失败了，还在失败冷却里。 */
        data class FailedCooldown(val ageHours: Double) : Outcome

        /** 排进去了。 */
        data class Enqueued(
            val impulseId: Long,
            val dueAt: Long,
            val signals: List<EmotionWakeSignal>,
        ) : Outcome

        /** 有新的越线，但队列去重把它挡下了（并发下的兜底）。 */
        data class Duplicate(val impulseId: Long) : Outcome
    }

    /**
     * 评估一次。
     *
     * 调用点在"状态刚刚变化之后"（见 `ChatService.sendMessage`），
     * 所以不需要任何后台心跳 —— 没有状态变化就没有评估。
     */
    suspend fun evaluate(now: Long = System.currentTimeMillis()): Outcome =
        withContext(Dispatchers.IO) {
            // 先把上次留下的烂摊子收拾掉，再判断"队列里有没有活跃行"。
            //
            // **这一步不能省，省了会永久停摆**：进程在生成中途被杀时，那一行会停在
            // `claimed`。而 `peekDue` 只查 `status = 'pending'`，看不到它 ——
            // 于是 `ProactiveMessageTriggerService` 里 `claimDue` 那条分支永远不会进，
            // 挂在 `claimDue` 内部的恢复流程也就永远不会跑。结果就是：
            // 去重闸被一条永远不动的 `claimed` 行堵死，这个 kind 的唤醒再也不会产生。
            //
            // 恢复的三种情形（见 [ImpulseQueueService.recover]）：租约过期的 claimed
            // 退回 pending、到点的 deferred 放回 pending、重试耗尽的 pending 判 failed。
            // 都是"让队列自己回到一致状态"，不产生任何行为决策。
            val recovered = impulseQueueService.recover(now)
            if (recovered > 0) {
                logSafe("recover before evaluate: 修正 $recovered 条队列行")
            }

            val crossings = driveStateService.crossings(now)
            val candidates = crossings.associateBy { signalId(it) }
            val activeKeys = candidates.keys

            val state = store.read()

            // 闩锁维护：已经不在候选里的 key 要放掉，否则那一维"落回线下再涨上来"
            // 永远不算新的一次，唤醒就只会有一次。
            val latched = state.latched.filterKeys { it in activeKeys }

            if (!state.initialized) {
                // 首次运行：把当前所有候选全部闩上，然后**不产生任何唤醒**。
                // 这就是任务书第 8 条要的 checkpoint —— 不重放历史。
                store.writeLatched(
                    latched = activeKeys.associateWith { key ->
                        EmotionWakeStore.Latch(
                            value = candidates.getValue(key).value,
                            at = now,
                        )
                    },
                    initialized = true,
                )
                impulseQueueService.prune(now)
                logSafe("wake: none | reason: baseline initialized | latched: ${activeKeys.size}")
                return@withContext Outcome.Initialized(latched = activeKeys.size)
            }

            val newKeys = activeKeys.filter { it !in latched }
            val signals = newKeys.map { candidates.getValue(it).toSignal() }

            // 先把清过的闩锁写回：不放掉过期 key 的话，下一次评估会把它当成"已经闩住"。
            store.writeLatched(latched = latched, initialized = true)

            if (signals.isEmpty()) {
                impulseQueueService.prune(now)
                return@withContext Outcome.NoCrossing(
                    active = activeKeys.size,
                    newKeys = 0,
                )
            }

            // 闸一：队列里还有没处理完的，就不排新的。
            //
            // 用 firstActive 直接问库，不拉一批 recent 再在内存里筛：后者要么定一个
            // 拍脑袋的扫描上限（超了就漏判），要么把整表拉出来。走
            // `index_impulse_queue_kind_dedupe_key` 的前缀列，命中一条就返回。
            val pending = impulseQueueService.firstActive(ImpulsePolicy.KIND_EMOTION_WAKE)
            if (pending != null) {
                return@withContext Outcome.AlreadyPending(pending.id, pending.status)
            }

            // 闸二/闸三：上一条结果的延后与最小间隔。
            val last = impulseQueueService.lastOutcome(ImpulsePolicy.KIND_EMOTION_WAKE)
            if (last != null) {
                if (last.status == ImpulsePolicy.STATUS_DEFERRED && last.retryAt > now) {
                    return@withContext Outcome.Deferred(last.retryAt)
                }
                val ageHours = (now - last.finishedAt) / MILLIS_PER_HOUR
                if (last.finishedAt > 0L && ageHours < ABSOLUTE_MIN_GAP_HOURS) {
                    logSafe("wake: none | reason: minimum gap (%.2f h)".format(ageHours))
                    return@withContext Outcome.MinGap(ageHours)
                }
                if (
                    last.status == ImpulsePolicy.STATUS_FAILED &&
                    last.finishedAt > 0L &&
                    ageHours < FAILED_COOLDOWN_HOURS
                ) {
                    logSafe("wake: none | reason: failed wake cooldown (%.2f h)".format(ageHours))
                    return@withContext Outcome.FailedCooldown(ageHours)
                }
            }

            // 到这里才真的排。
            val view = driveStateService.snapshot()
            val baseline = driveStateService.baseline(now)
            val payload = EmotionWakePayload(
                value = signals.maxOf { it.value },
                desc = EmotionWakePayload.DESC,
                signals = signals,
                emotionSignature = signals.associate { it.id to it.value },
                emotionSnapshot = view?.drives ?: emptyMap(),
                dominant = view?.snapshot?.dominant,
                baselineKind = baseline?.kind?.name?.lowercase(),
                baselineAgeHours = baseline?.ageHours,
                bornAt = now,
            )
            val dueAt = now + ImpulsePolicy.MIN_LEAD_MILLIS

            val result = impulseQueueService.enqueue(
                kind = ImpulsePolicy.KIND_EMOTION_WAKE,
                dedupeKey = DEDUPE_KEY,
                payloadJson = EmotionWakePayload.encode(payload),
                dueAt = dueAt,
                source = SOURCE_DRIVE_CROSSING,
                now = now,
            )

            when (result) {
                is ImpulseQueueService.EnqueueResult.Duplicate ->
                    Outcome.Duplicate(result.existingId)

                is ImpulseQueueService.EnqueueResult.Enqueued -> {
                    // 排成功了才闩上：入队失败时保持未闩，下一轮还能再试。
                    store.writeLatched(
                        latched = latched + newKeys.associateWith { key ->
                            EmotionWakeStore.Latch(
                                value = candidates.getValue(key).value,
                                at = now,
                            )
                        },
                        initialized = true,
                    )
                    store.writeWake(now = now, dueAt = dueAt)
                    impulseQueueService.prune(now)
                    logSafe(
                        "wake: added ${result.id} | signals: " +
                            signals.joinToString(",") { it.id } +
                            " | due in ${ImpulsePolicy.MIN_LEAD_MILLIS / 1000}s"
                    )
                    Outcome.Enqueued(
                        impulseId = result.id,
                        dueAt = dueAt,
                        signals = signals,
                    )
                }
            }
        }

    /**
     * 队列里已经没有待处理的了，清掉给闹钟看的镜像。
     *
     * 不清的话 `scheduleNext` 会一直按一个早就过去的时刻把闹钟叫早，
     * 虽然 `nextDueAtMillis` 会滤掉过去的时刻，但镜像本身留着是误导。
     */
    suspend fun syncScheduleMirror(now: Long = System.currentTimeMillis()) {
        val next = impulseQueueService.nextPendingDueAt(now)
        if (next == null) {
            store.clearNextDue()
        } else {
            store.writeWake(now = store.read().lastWakeAt, dueAt = next)
        }
    }

    private fun signalId(crossing: DriveBaseline.Crossing): String = when (crossing.trigger) {
        DriveBaseline.TRIGGER_ABSOLUTE -> "absolute:${crossing.drive}"
        else -> "relative:${crossing.drive}"
    }

    /**
     * 把观测结果包成可入库的信号。
     *
     * 这一步是**纯搬运**：字段一一对应，不新增、不推断、不排序。
     * `DriveBaseline.Crossing` 是"算出来的越线"，`EmotionWakeSignal` 是"要写进载荷的越线"，
     * 两者刻意分开 —— 前者属于 State 层，后者属于 Behavior 层，桥只做转换。
     */
    private fun DriveBaseline.Crossing.toSignal(): EmotionWakeSignal = EmotionWakeSignal(
        id = signalId(this),
        drive = this.drive,
        trigger = this.trigger,
        value = this.value,
        threshold = this.threshold,
        baseline = this.baseline,
        rise = this.rise,
    )

    private fun logSafe(message: String) {
        runCatching { Log.i(TAG, message) }
    }

    companion object {
        private const val TAG = "EmotionWakeBridge"

        /** 来源标记，写进队列行。 */
        const val SOURCE_DRIVE_CROSSING = "drive_crossing"

        /**
         * 去重键。
         *
         * 是个常量而不是"按维度"：`behavior.py` 的去重是
         * `has_active_kind(WAKE_KIND)` —— 只要队列里还有一条没处理完的
         * emotion_wake，就不再排新的。唤醒不是某一维的私事。
         */
        const val DEDUPE_KEY = "emotion_wake"

        /** 最小间隔（小时）。Elektron 的 `ABSOLUTE_MIN_GAP_H = 0.5`。 */
        const val ABSOLUTE_MIN_GAP_HOURS = 0.5

        /** 失败冷却（小时）。Elektron 的 `if status == failed and age < 1`。 */
        const val FAILED_COOLDOWN_HOURS = 1.0

        private const val MILLIS_PER_HOUR = 3_600_000.0
    }
}
