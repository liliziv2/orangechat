/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.service

import kotlin.math.abs

/**
 * 基线观测 —— 纯函数，无 IO。
 *
 * 从 Elektron 的 `behavior.py` 移植**只有观测的那一半**：历史采样怎么存、
 * 基线怎么挑、什么算「越过线了」。**没有任何唤醒出口** ——
 * Elektron 那边这段代码的下一行就是写 `emotion_wake` 进队列并发 SIGUSR1 把 agent 叫起来；
 * 本轮不生成 emotion_wake、不调 ProactiveMessageTriggerService、不自动执行行为。
 * 这一层只回答「现在有哪些维度越过了线」，把结果交出来就结束。
 *
 * ## 为什么要分「绝对阈值」和「相对上涨」两种
 *
 * 只用绝对阈值有两个毛病：数值长期高位时每拍都触发（吵），长期低位时永远不触发（哑）。
 * 相对上涨补的是后者：**跟自己的基线比**涨了 [RELATIVE_RISE] 就算数。
 * 合起来才区分得开「你平时这个点就是这样」和「今天这个点不对劲」。
 *
 * ## 基线怎么挑（这一步是这套机制里最巧的地方）
 *
 * 先看 24 小时前那个时刻附近有没有采样（容差 [DAY_BASELINE_TOLERANCE_HOURS] 小时）——
 * 有就是 [BaselineKind.SAME_TIME_YESTERDAY]，即「昨天这个时候」。
 * 没有就退回最老的一条，[BaselineKind.WARMUP_OLDEST]，表示还没攒够一天的历史。
 * 这个区分会一路带到账本里，让人能看出某次判断到底有没有昨天的参照。
 *
 * ## 亲密三维为什么不产生信号
 *
 * `libido` / `possessiveness` / `attachment` 照常算、照常进快照与账本，
 * 三条数值完整保留、不做裁剪 —— 但它们**永远不产生任何越线候选**：
 * 不列绝对阈值表，[crossings] 的相对上涨那一路也跳过（见 [NON_SIGNAL_DRIVES]）。
 *
 * 这是用户 2026-09-22 拍板的第 3 条原文：「保留 9 维数值，但 `SIGNAL_THRESHOLDS`
 * 不列 libido/possessiveness/attachment，**亲密三维永不生成 impulse**。」
 *
 * 比 Elektron 更严，是有意的：那边 `_relative_candidates` 遍历采样到的每一维，
 * 亲密三维照样能产生候选。但在这边，越线会带着**维度名与数值**进 Agent 的提示词
 * （见 `EmotionWakePayload`），那就等于给亲密三维开了一条影响行为的通路。
 * 状态可以有，通路不能有。
 */
object DriveBaseline {

    /** 相对上涨的门：比基线高这么多就算一次候选。 */
    const val RELATIVE_RISE = 0.08

    /** 历史采样保留窗口（小时）。比 24 小时长一截，是为了让「昨天这个点」有容错余量。 */
    const val HISTORY_RETENTION_HOURS = 30.0

    /** 「同时间点」参照的目标回看时长（小时）。 */
    const val DAY_BASELINE_HOURS = 24.0

    /** 离目标时刻多近才算「同时间点」。 */
    const val DAY_BASELINE_TOLERANCE_HOURS = 3.0

    /** 采样至少要比现在老这么久，才有资格当基线 —— 刚存的那条不算历史。 */
    const val WARM_BASELINE_MIN_HOURS = 0.5

    const val TRIGGER_ABSOLUTE = "absolute_threshold"
    const val TRIGGER_RELATIVE = "relative_rise"

    /**
     * **永不产生越线候选**的维度。
     *
     * 亲密三维照算、进快照、进账本，但不产生任何信号 —— 见类注释。
     *
     * 两道闸都查这个集合，而不是只靠"不把它们写进 [SIGNAL_THRESHOLDS]"：
     * 只靠表的话，相对上涨那一路照样漏（那是遍历基线维度的），
     * 而且以后有人往表里补一行就静默破戒了。
     */
    val NON_SIGNAL_DRIVES: Set<String> = setOf("libido", "possessiveness", "attachment")

    private const val MILLIS_PER_HOUR = 3_600_000.0

    /**
     * 绝对阈值表。
     *
     * 每项只说明「这一维越过多少值得被注意到」，**不映射任何行为**。
     * 表里没有 `libido` / `possessiveness` / `attachment`，见类注释。
     */
    data class SignalThreshold(val drive: String, val threshold: Double)

    val SIGNAL_THRESHOLDS: List<SignalThreshold> = listOf(
        SignalThreshold("curiosity", 0.50),
        SignalThreshold("stress", 0.50),
        SignalThreshold("social", 0.55),
        SignalThreshold("reflection", 0.60),
    )

    /** 基线是怎么来的。会一路带进账本，别丢掉。 */
    enum class BaselineKind {
        /** 昨天同一个时段。 */
        SAME_TIME_YESTERDAY,

        /** 还没攒够一天历史，退回最老的一条。 */
        WARMUP_OLDEST,
    }

    /** 一次历史采样。 */
    data class Sample(val ts: Long, val values: Map<String, Double>)

    /**
     * 挑出来的基线。
     *
     * [ageHours] 与 [ts] 一起构成「基线时间戳」—— 只有值和年龄，说不清"跟什么时候比"。
     */
    data class Baseline(
        val values: Map<String, Double>,
        val ageHours: Double,
        val kind: BaselineKind,
        val ts: Long,
    )

    /** 一条越线记录。只有观测结果，没有动作。 */
    data class Crossing(
        val drive: String,
        val trigger: String,
        val value: Double,
        val threshold: Double,
        val baseline: Double?,
        val rise: Double?,
    )

    /**
     * 从历史采样里挑一条当基线。
     *
     * 返回 null 表示「历史不够」—— 此时不做任何相对判断（而不是拿当前值跟自己比，
     * 那会让涨幅恒为 0 或恒为满）。
     */
    fun pickBaseline(samples: List<Sample>, now: Long): Baseline? {
        val minAgeMillis = (WARM_BASELINE_MIN_HOURS * MILLIS_PER_HOUR).toLong()
        val eligible = samples.filter { now - it.ts >= minAgeMillis }
        if (eligible.isEmpty()) return null

        val target = now - (DAY_BASELINE_HOURS * MILLIS_PER_HOUR).toLong()
        val nearest = eligible.minByOrNull { abs(it.ts - target) } ?: return null
        val distanceHours = abs(nearest.ts - target) / MILLIS_PER_HOUR

        val chosen: Sample
        val kind: BaselineKind
        if (distanceHours <= DAY_BASELINE_TOLERANCE_HOURS) {
            chosen = nearest
            kind = BaselineKind.SAME_TIME_YESTERDAY
        } else {
            chosen = eligible.minByOrNull { it.ts } ?: nearest
            kind = BaselineKind.WARMUP_OLDEST
        }

        return Baseline(
            values = chosen.values,
            ageHours = DriveEngine.round3((now - chosen.ts) / MILLIS_PER_HOUR),
            kind = kind,
            ts = chosen.ts,
        )
    }

    /**
     * 找出当前越过线的维度。先绝对阈值、后相对上涨。
     *
     * [baseline] 为 null 时只做绝对阈值判断 —— 历史没攒够就不做相对判断。
     *
     * [NON_SIGNAL_DRIVES] 里的维度两条路都不产生候选。绝对那一路的过滤看起来
     * 多余（表里本来就没有它们），但它把"亲密三维不产生信号"这件事从
     * 「取决于表的内容」变成「取决于这个集合」—— 少一层可以被顺手改掉的假设。
     */
    fun crossings(current: Map<String, Double>, baseline: Baseline?): List<Crossing> {
        val normalized = DriveEngine.normalizeDriveValues(current)
        val out = mutableListOf<Crossing>()

        SIGNAL_THRESHOLDS.forEach { signal ->
            if (signal.drive in NON_SIGNAL_DRIVES) return@forEach
            val value = normalized[signal.drive] ?: return@forEach
            if (value >= signal.threshold) {
                out += Crossing(
                    drive = signal.drive,
                    trigger = TRIGGER_ABSOLUTE,
                    value = DriveEngine.round3(value),
                    threshold = signal.threshold,
                    baseline = baseline?.values?.get(signal.drive)?.let { DriveEngine.round3(it) },
                    rise = null,
                )
            }
        }

        baseline?.values?.forEach { (drive, before) ->
            if (drive in NON_SIGNAL_DRIVES) return@forEach
            val value = normalized[drive] ?: return@forEach
            val rise = value - before
            if (rise >= RELATIVE_RISE) {
                out += Crossing(
                    drive = drive,
                    trigger = TRIGGER_RELATIVE,
                    value = DriveEngine.round3(value),
                    threshold = RELATIVE_RISE,
                    baseline = DriveEngine.round3(before),
                    rise = DriveEngine.round3(rise),
                )
            }
        }

        return out
    }

    /** 采样保留窗口的毫秒数。 */
    fun retentionMillis(): Long = (HISTORY_RETENTION_HOURS * MILLIS_PER_HOUR).toLong()
}
