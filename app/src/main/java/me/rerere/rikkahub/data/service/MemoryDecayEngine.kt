/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.service

import kotlin.math.exp
import kotlin.math.pow

/**
 * 记忆衰减引擎 —— 纯函数，无 IO。
 *
 * 从 Elektron 的 `memory/decay_engine.py` 移植。它只回答一个问题：**这条记忆现在该排多前**。
 *
 * ## 一个刻意的取舍：衰减只排序，不归档
 *
 * Elektron 在 2026-08-25 拍过板：「不模仿人的遗忘。衰减只排序，不归档。」
 * 代码里 archive 分支被 `success = False` 硬关掉，理由记在 DECISIONS 里 ——
 * 模仿遗忘会让档案永久失去一部分，而这份档案的价值恰恰在于「什么都没丢」。
 *
 * 移植时保留了这个决定：本引擎不返回「该不该删/该不该归档」，只返回一个排序分。
 * 归档是显式动作（[MemoryBankService.archiveMemory]），不由衰减触发。
 * [THRESHOLD] 因此只作为「低分」的参考线存在，**不要拿它去接归档逻辑**。
 *
 * ## 公式
 *
 * ```
 * score = importance × activation_count^0.3 × e^(-λ·days) × combined_weight
 *         × resolved_factor × urgency_boost
 *
 * λ = 0.05
 * emotion_weight  = 1.0 + arousal × 0.8
 * freshness       = 1.0 + e^(-hours/36)
 * combined_weight = days <= 3 ? freshness×0.7 + emotion×0.3
 *                             : emotion×0.7 + freshness×0.3
 * resolved_factor = (resolved && digested) ? 0.02 : resolved ? 0.05 : 1.0
 * urgency_boost   = (arousal > 0.7 && !resolved) ? 1.5 : 1.0
 * ```
 *
 * 短期（≤3 天）时间权重主导、情感放大；长期（>3 天）情感权重主导、时间只提供底线。
 * 这对应 Elektron README 里那句「短期靠时间权重，长期靠情绪权重」。
 */
object MemoryDecayEngine {

    /** 衰减速率。越大遗忘越快。 */
    const val LAMBDA = 0.05

    /**
     * 归档阈值。
     *
     * **当前没有任何代码拿它归档** —— 保留它是为了和 Elektron 的配置项对齐，
     * 以及给展示层一个「这条已经沉下去了」的参考线。接归档之前先读类注释。
     */
    const val THRESHOLD = 0.3

    /** 情感权重 = EMOTION_BASE + arousal × AROUSAL_BOOST。 */
    const val EMOTION_BASE = 1.0
    const val AROUSAL_BOOST = 0.8

    /** 短期/长期的分界天数。 */
    const val SHORT_TERM_DAYS = 3.0

    /** 短期段里时间权重的占比。 */
    const val SHORT_TERM_TIME_SHARE = 0.7

    /** 新鲜度加成的时间常数（小时）：`1 + e^(-hours/36)`。 */
    const val FRESHNESS_TIME_CONSTANT_HOURS = 36.0

    /** 时间字段缺失时的兜底天数，对齐 Elektron 解析失败时的 30 天。 */
    const val FALLBACK_DAYS = 30.0

    /** 永不衰减的哨兵值：pinned / protected / permanent 桶。 */
    const val NEVER_DECAY_SCORE = 999.0

    /** 唤醒度超过这个门且未结案时，往前顶一档。 */
    const val URGENCY_AROUSAL_GATE = 0.7
    const val URGENCY_BOOST = 1.5

    /** 结案后的加速淡化系数。 */
    const val RESOLVED_DIGESTED_FACTOR = 0.02
    const val RESOLVED_FACTOR = 0.05

    /** 固化类型：永不衰减。 */
    const val TYPE_PERMANENT = "permanent"

    private const val MILLIS_PER_DAY = 86_400_000.0
    private const val HOURS_PER_DAY = 24.0

    /**
     * 计算一条记忆的当前活跃度得分。
     *
     * 得分越高 = 记忆越鲜活。调用方只应拿它排序，不应拿它做删除判断。
     */
    fun score(input: DecayInput): Double {
        // 钉选 / 保护 / 固化：永不衰减，importance 在写入时已被锁到 10
        if (input.pinned || input.isProtected) return NEVER_DECAY_SCORE
        if (input.type == TYPE_PERMANENT) return NEVER_DECAY_SCORE

        val importance = input.importance.coerceIn(1, 10)
        val activationCount = maxOf(1.0, input.activationCount.toDouble())
        val daysSince = daysSince(input.lastActiveAt, input.now)
        val arousal = input.arousal.coerceIn(0f, 1f).toDouble()

        val emotionWeight = EMOTION_BASE + arousal * AROUSAL_BOOST
        val freshnessWeight = freshnessWeight(daysSince)

        val combinedWeight = if (daysSince <= SHORT_TERM_DAYS) {
            freshnessWeight * SHORT_TERM_TIME_SHARE +
                emotionWeight * (1.0 - SHORT_TERM_TIME_SHARE)
        } else {
            emotionWeight * SHORT_TERM_TIME_SHARE +
                freshnessWeight * (1.0 - SHORT_TERM_TIME_SHARE)
        }

        val baseScore = importance *
            activationCount.pow(0.3) *
            exp(-LAMBDA * daysSince) *
            combinedWeight

        val factor = resolvedFactor(input.resolved, input.digested)
        val urgency = if (arousal > URGENCY_AROUSAL_GATE && !input.resolved) URGENCY_BOOST else 1.0

        return baseScore * factor * urgency
    }

    /**
     * 距上次活跃过了多少天。
     *
     * `lastActiveAt <= 0` 表示旧数据没记这个字段，兜底成 [FALLBACK_DAYS]，
     * 与 Elektron 解析失败时的行为一致 —— 宁可当成"很久没动"，
     * 也不要当成"刚刚活跃"（后者会让一批几十天前的旧记忆时间分集体拉满，
     * 正是 PITFALLS 里「搬家之后所有旧记忆的排序全乱了」那次的根因）。
     */
    fun daysSince(lastActiveAt: Long, now: Long): Double {
        if (lastActiveAt <= 0L) return FALLBACK_DAYS
        return maxOf(0.0, (now - lastActiveAt) / MILLIS_PER_DAY)
    }

    /**
     * 新鲜度加成：刚存入 ×2.0，约 25 小时半衰 → ×1.5，72 小时后趋近 ×1.0。
     */
    fun freshnessWeight(daysSince: Double): Double {
        val hours = daysSince * HOURS_PER_DAY
        return 1.0 + exp(-hours / FRESHNESS_TIME_CONSTANT_HOURS)
    }

    /**
     * 结案加速淡化：已处理 + 已写过 feel → ×0.02，仅已处理 → ×0.05。
     */
    fun resolvedFactor(resolved: Boolean, digested: Boolean): Double = when {
        resolved && digested -> RESOLVED_DIGESTED_FACTOR
        resolved -> RESOLVED_FACTOR
        else -> 1.0
    }

    /** 是否低于归档参考线。**不要拿它触发归档**，见类注释。 */
    fun isBelowThreshold(score: Double): Boolean = score < THRESHOLD
}

/**
 * 一次衰减计算的输入。全部来自 [me.rerere.rikkahub.data.db.entity.MemoryBankEntity]。
 *
 * 抽成独立数据类是为了让公式可被单测覆盖 —— 引擎不碰 Room，也不碰时钟。
 */
data class DecayInput(
    val type: String,
    val importance: Int,
    val activationCount: Int,
    val lastActiveAt: Long,
    val arousal: Float,
    val resolved: Boolean,
    val digested: Boolean,
    val pinned: Boolean,
    val isProtected: Boolean,
    val now: Long,
)
