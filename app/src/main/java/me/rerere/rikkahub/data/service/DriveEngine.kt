/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.service

import kotlin.math.max
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sqrt

/**
 * 9 维驱动内核 —— 纯函数，无 IO，不碰 Room，不碰时钟。
 *
 * 从 Elektron 的 `memory/desire_engine.py`（4657 行）移植。**移植范围只有状态本身**：
 * 9 维数值怎么算、怎么随事件变化、怎么随时间回落。Elektron 挂在同一个文件上的
 * 念头池（flit / fixation / unsourced / rumination）、悲恸引擎、节律层、天气余波、
 * 弦音系统都**没有**搬过来 —— 那些各自依赖别的子系统，不属于 State 层。
 *
 * ## 这一层回答什么
 *
 * 「现在内部状态是什么、为什么变化了」。
 * 它**不**回答「该不该做点什么」—— 情绪只有唤醒权、没有行动决策权（Elektron
 * `behavior.py` 文件头那条原则）。本轮没有任何唤醒出口，阈值只做只读观测，
 * 见 [DriveBaseline] 的类注释。
 *
 * ## 两层归一：为什么不是直接看 drive 数值
 *
 * 9 维的静息水位不一样（`possessiveness` 0.08、`attachment` 0.30），直接比数值等于
 * 拿不同量纲的东西排序。所以先做一次**基线归一**：
 *
 * ```
 * drive_activation(key, v) = sqrt( clamp( (v - baseline) / (1 - baseline) ) )
 * effective(key, v, fat)    = clamp( drive_activation × (1 - clamp(fat)) )
 * ```
 *
 * 开方是 Elektron 试出来的：线性余量会让「明确的事件」看起来永远只有半睡半醒，
 * 开方把「普通」（约 0.15–0.40）和「决定性」（0.50+）分开，同时保住
 * 基线处为 0、满值处为 1 两个端点。
 *
 * 第二层是**每维独立疲劳**：全局 `fatigue` 按 [FATIGUE_SENSITIVITY] 分配到各维，
 * 得到 [computeLocalFatigue]。疲劳压的是**激活量**，不动静息基线 ——
 * 累的时候"不活跃"，不是"没感情"。
 *
 * ## 本轮明确没做
 *
 * - `possessiveness_channels` / `attachment_rebound` / `libido_pending` 三个通道的
 *   **列已经开出来、也做 JSON 往返**，但 tick 里不驱动它们（Elektron 用它们做
 *   领地基线漂移、缺席回弹、亲密中断蓄积）。这三套机制各自还要一堆配套常量，
 *   留给后续阶段；现在它们停在默认值上。
 * - `attachment` 的溢出释放（超过 0.80 转 libido）、缺席回弹（idle return）同理未接。
 * - 跨维耦合（[COUPLING]）、ESM 软互抑、逃逸阀**已接**，这三个是 tick 的正确性必需项。
 */
object DriveEngine {

    // ─── 9 维与基线 ──────────────────────────────────────────────────────────

    /** 9 个维度的固定顺序。顺序是契约：ledger、快照、展示都按它排。 */
    val DRIVE_KEYS: List<String> = listOf(
        "attachment",
        "libido",
        "possessiveness",
        "reflection",
        "stewardship",
        "curiosity",
        "social",
        "fatigue",
        "stress",
    )

    /**
     * 各维静息基线（日常水位，不是零点）。
     *
     * `possessiveness` 给 0.08 是刻意的低：它一冒头就该是「边界被碰了」，不该是常态。
     * 私人生活三维（curiosity / stewardship / reflection）基线压得低，是为了让
     * [PERSONAL_AMBIENT_EXCESS] 的日常活气底有抬升空间 —— 基线虚高会让它们
     * 永远贴着 0、好几天冒不了头。
     */
    val DRIVE_BASELINES: Map<String, Double> = linkedMapOf(
        "attachment" to 0.30,
        "libido" to 0.20,
        "possessiveness" to 0.08,
        "reflection" to 0.18,
        "stewardship" to 0.18,
        "curiosity" to 0.22,
        "social" to 0.25,
        "fatigue" to 0.10,
        "stress" to 0.15,
    )

    /** 维度别名。Elektron 早期用 `duty`，v2 之后统一成 `stewardship`。 */
    private val DRIVE_ALIASES: Map<String, String> = mapOf("duty" to "stewardship")

    /**
     * 每维疲劳敏感度：全局 `fatigue` 对该维激活量的压制强度。
     *
     * `attachment` / `libido` 几乎不受疲劳影响（0.12 / 0.08）—— 累不等于不亲近。
     * `social` 0.78 是最高的一档：没电的时候最先关掉的就是「想往外说」。
     * `fatigue` 自身 0.0，否则会自己压自己，形成不收敛的正反馈。
     */
    val FATIGUE_SENSITIVITY: Map<String, Double> = linkedMapOf(
        "attachment" to 0.12,
        "libido" to 0.08,
        "possessiveness" to 0.14,
        "reflection" to 0.32,
        "stewardship" to 0.30,
        "curiosity" to 0.38,
        "social" to 0.78,
        "fatigue" to 0.0,
        "stress" to 0.30,
    )

    /** 私人生活主场三维。它们有独立的日常活气底，见 [PERSONAL_AMBIENT_EXCESS]。 */
    val PERSONAL_LIFE_DRIVES: Set<String> = setOf("curiosity", "stewardship", "reflection")

    /** 静息之上维持的「日常活气」超出量（raw − baseline 的目标底）。 */
    val PERSONAL_AMBIENT_EXCESS: Map<String, Double> = mapOf(
        "curiosity" to 0.14,
        "stewardship" to 0.12,
        "reflection" to 0.12,
    )

    /** 每 tick-equivalent 朝 ambient 底合拢的比例。 */
    const val PERSONAL_AMBIENT_LIFT = 0.38

    // ─── 时间模式与阻尼 ──────────────────────────────────────────────────────

    /**
     * 各维的时间模式。决定它回落多快 —— `fast_spike` 冲得快也退得快，
     * `cumulative` 只积不散（`fatigue` 就是这样：休息才是唯一的出口）。
     */
    val DRIVE_TIME_MODES: Map<String, String> = linkedMapOf(
        "attachment" to "slow",
        "libido" to "fast_spike",
        "possessiveness" to "fast_spike + slow",
        "reflection" to "medium",
        "stewardship" to "medium",
        "curiosity" to "medium",
        "social" to "medium",
        "fatigue" to "cumulative",
        "stress" to "fast_spike",
    )

    /** 基础阻尼率：每个 tick 朝基线合拢的比例。 */
    const val DAMPING = 0.02

    /** 时间模式对阻尼的倍率。 */
    val DRIVE_TIME_MODE_DAMPING: Map<String, Double> = mapOf(
        "fast_spike" to 1.75,
        "fast_spike + slow" to 0.60,
        "medium" to 1.00,
        "slow" to 0.35,
        "cumulative" to 0.10,
    )

    /** `attachment` 单独的阻尼倍率：略快于 slow，约一天量级把超出量吸一半。 */
    const val ATTACHMENT_DAMPING_MULT = 0.80

    /** 心跳墙钟间隔（毫秒）。用于把"过了多久"折算成 tick 倍率。 */
    const val DESIRE_TICK_MILLIS = 900_000L

    // ─── 跨维耦合 ────────────────────────────────────────────────────────────

    /** 耦合的作用方式：`level` 看源维高出基线多少，`delta` 看源维这一拍动了多少。 */
    enum class CouplingMode { LEVEL, DELTA }

    data class Coupling(
        val source: String,
        val target: String,
        val coefficient: Double,
        val mode: CouplingMode,
    )

    /**
     * 跨维耦合表。
     *
     * `delta` 模式刻意跟随源维的**实际移动量** —— 早期版本用固定跳变，
     * 结果 +0.001 和 +0.5 对下游打出同样的一击。
     */
    val COUPLING: List<Coupling> = listOf(
        Coupling("stress", "attachment", 0.04, CouplingMode.LEVEL),
        Coupling("stress", "curiosity", -0.03, CouplingMode.LEVEL),
        Coupling("attachment", "libido", 0.005, CouplingMode.LEVEL),
        Coupling("curiosity", "reflection", 0.04, CouplingMode.DELTA),
        Coupling("reflection", "social", 0.03, CouplingMode.DELTA),
        Coupling("fatigue", "stress", 0.03, CouplingMode.LEVEL),
        Coupling("reflection", "stress", 0.06, CouplingMode.DELTA),
    )

    // ─── ESM 软互抑 + 逃逸阀 ─────────────────────────────────────────────────

    /** 正向组 / 负向组。不是新状态，只是把现有 9 维按情绪极性分组。 */
    val POSITIVE_GROUP: List<String> =
        listOf("attachment", "libido", "curiosity", "social", "reflection", "stewardship")
    val NEGATIVE_GROUP: List<String> = listOf("stress", "fatigue", "possessiveness")

    /** 互抑系数。 */
    const val ESM_K = 0.3

    /** 负向超出量比正向高出这个值才算「明显失衡」。 */
    const val ESCAPE_VALVE_EXCESS_GAP = 0.15

    /** 连续这么多拍失衡才触发，防止单次评分误判。 */
    const val ESCAPE_VALVE_STREAK_TRIGGER = 3

    /** 触发后负向组超出基线的部分往回拉的比例。 */
    const val ESCAPE_VALVE_PULLBACK = 0.5

    // ─── 事件包（drive_event v2）────────────────────────────────────────────

    /** 事件 schema 版本，写进 ledger。 */
    const val DRIVE_EVENT_SCHEMA = "drive_event_v2"

    /** 低于这个 agency 的事件被压制（不是自己的意愿，不该动内部状态）。 */
    const val DRIVE_EVENT_AGENCY_GATE = 0.35

    /** 低于这个 confidence 的事件被压制。 */
    const val DRIVE_EVENT_CONFIDENCE_FLOOR = 0.20

    /** 次级驱动贡献的缩放。 */
    const val DRIVE_EVENT_SECONDARY_SCALE = 0.45

    /** 主驱动的聚焦增益：一个事件该有语义中心，不该把预算均摊到 9 维。 */
    const val DRIVE_EVENT_PRIMARY_FOCUS_GAIN = 1.35

    /** 主驱动特征值对增量的额外放大系数。 */
    const val DRIVE_EVENT_PRIMARY_FEATURE_GAIN = 0.35

    /** 次级驱动的入场门。 */
    const val DRIVE_EVENT_SECONDARY_GATE = 0.25

    /** 非主驱动特征值的入场门。 */
    const val DRIVE_EVENT_NONPRIMARY_FEATURE_GATE = 0.45

    /** 非主驱动贡献的最小可见增量，低于它不进 proposed（防"洒水"）。 */
    const val DRIVE_EVENT_NONPRIMARY_MIN_DELTA = 0.003

    /** `possessiveness` 的领地警报门。低于它整维不进 proposed。 */
    const val POSSESSIVENESS_TERRITORIAL_GATE = 0.55

    /** 各维的事件基础增量。 */
    val DRIVE_EVENT_BASE_DELTA: Map<String, Double> = linkedMapOf(
        "attachment" to 0.22,
        "libido" to 0.19,
        "possessiveness" to 0.21,
        "reflection" to 0.19,
        "stewardship" to 0.19,
        "curiosity" to 0.18,
        "social" to 0.18,
        "fatigue" to 0.18,
        "stress" to 0.19,
    )

    /** 事件来源权重。用户亲口说的话最重，外部环境最轻。 */
    val DRIVE_EVENT_SOURCE_WEIGHTS: Map<String, Double> = mapOf(
        "user_message" to 1.00,
        "speech_event" to 0.90,
        "feel" to 0.75,
        "memory" to 0.55,
        "touch" to 0.70,
        "external" to 0.45,
        "analyze_nocturne_entry" to 0.55,
        "dp_memory" to 0.55,
        "dialogue_residue" to 0.50,
        "legacy_feed" to 0.60,
        "manual" to 0.75,
    )

    /** 未登记来源的兜底权重。 */
    const val DRIVE_EVENT_DEFAULT_SOURCE_WEIGHT = 0.65

    /**
     * 事件包里的 9 个「大脑特征」→ drive 的映射。
     *
     * 三元组是（目标维、权重、入场门）。这是 Elektron 事件包 v2 的核心：
     * 分析器只填 9 个语义特征，由这张表决定它们各自推到哪一维，
     * 而不是让调用方直接给 9 维 delta（那样每个调用点都会自己发明一套量纲）。
     */
    data class BrainFeatureSpec(val drive: String, val weight: Double, val threshold: Double)

    val DRIVE_EVENT_BRAIN_FEATURES: Map<String, BrainFeatureSpec> = linkedMapOf(
        "closeness_pull" to BrainFeatureSpec("attachment", 0.55, 0.0),
        "body_heat" to BrainFeatureSpec("libido", 0.52, 0.0),
        "territorial_alarm" to BrainFeatureSpec("possessiveness", 0.70, POSSESSIVENESS_TERRITORIAL_GATE),
        "inward_pull" to BrainFeatureSpec("reflection", 0.50, 0.0),
        "house_need" to BrainFeatureSpec("stewardship", 0.55, 0.0),
        "novelty_pull" to BrainFeatureSpec("curiosity", 0.48, 0.0),
        "expression_pressure" to BrainFeatureSpec("social", 0.50, 0.0),
        "energy_cost" to BrainFeatureSpec("fatigue", 0.50, 0.0),
        "tension_load" to BrainFeatureSpec("stress", 0.55, 0.0),
    )

    // ─── attachment 盆地跳变 ────────────────────────────────────────────────

    /**
     * `attachment` 的盆地模型阈值。
     *
     * 它不像别的维那样线性涨：过 [ATTACHMENT_BASIN_THRESHOLD] 直接从下方跳到
     * [ATTACHMENT_BASIN_JUMP]。Elektron 的注释说这是"盆地模型"—— 依恋不是渐变，
     * 跨过一个点就换了一种状态。已经在盆地上方时不重复跳，继续走普通 pulse。
     */
    const val ATTACHMENT_BASIN_THRESHOLD = 0.68
    const val ATTACHMENT_BASIN_JUMP = 0.82

    // ─── 基础工具 ────────────────────────────────────────────────────────────

    /** 夹取到 [lo, hi]。NaN 归到 lo。 */
    fun clamp(v: Double, lo: Double = 0.0, hi: Double = 1.0): Double =
        if (v.isNaN()) lo else max(lo, minOf(hi, v))

    /** 保留 3 位小数（快照用）。 */
    fun round3(v: Double): Double = round(v * 1000.0) / 1000.0

    /** 保留 4 位小数（ledger 里的增量用）。 */
    fun round4(v: Double): Double = round(v * 10000.0) / 10000.0

    /** 维度名规范化。认不出来时返回 [fallback]。 */
    fun normalizeDriveKey(raw: String?, fallback: String? = null): String? {
        val value = raw?.trim()?.lowercase().orEmpty()
        val aliased = DRIVE_ALIASES[value] ?: value
        return if (aliased in DRIVE_KEYS) aliased else fallback
    }

    /**
     * 把任意（可能残缺、可能带脏值）的 9 维 map 补成完整合法的一份。
     *
     * 缺的维用基线补、认不出的 key 丢掉、非有限值丢掉 —— 这一层是**唯一**的
     * 入口校验点，后面所有函数都可以放心 `getValue`。
     */
    fun normalizeDriveValues(values: Map<String, Double>?): Map<String, Double> {
        val out = LinkedHashMap<String, Double>(DRIVE_BASELINES)
        values?.forEach { (rawKey, rawValue) ->
            val key = normalizeDriveKey(rawKey) ?: return@forEach
            if (rawValue.isFinite()) out[key] = clamp(rawValue)
        }
        return out
    }

    /** 全局疲劳 → 每维独立疲劳。 */
    fun computeLocalFatigue(globalFatigue: Double): Map<String, Double> {
        val out = LinkedHashMap<String, Double>(FATIGUE_SENSITIVITY.size)
        FATIGUE_SENSITIVITY.forEach { (key, sensitivity) ->
            out[key] = clamp(globalFatigue * sensitivity)
        }
        return out
    }

    // ─── 单维脉冲 ────────────────────────────────────────────────────────────

    /**
     * 越接近满值，同样的 delta 打得越浅。
     *
     * `gain = delta × sqrt(1 − current)`。没有这个的话，高位维会被无限推平。
     */
    fun pulseGain(current: Double, baseDelta: Double): Double =
        baseDelta * sqrt(max(0.0, 1.0 - current))

    /** 给一维打一次脉冲。返回新的 9 维 map（不改原 map）。 */
    fun pulseDrive(drives: Map<String, Double>, driveKey: String, delta: Double): Map<String, Double> {
        val key = normalizeDriveKey(driveKey) ?: return normalizeDriveValues(drives)
        val out = LinkedHashMap(normalizeDriveValues(drives))
        val current = out.getValue(key)
        out[key] = clamp(current + pulseGain(current, delta))
        return out
    }

    /** `attachment` 的非线性脉冲，见 [ATTACHMENT_BASIN_THRESHOLD]。 */
    fun pulseAttachmentNonlinear(drives: Map<String, Double>, delta: Double): Map<String, Double> {
        val out = LinkedHashMap(normalizeDriveValues(drives))
        val current = out.getValue("attachment")
        val gained = current + pulseGain(current, delta)
        val next = if (current < ATTACHMENT_BASIN_THRESHOLD && gained >= ATTACHMENT_BASIN_THRESHOLD) {
            max(gained, ATTACHMENT_BASIN_JUMP)
        } else {
            gained
        }
        out["attachment"] = clamp(next)
        return out
    }

    // ─── 双层归一 ────────────────────────────────────────────────────────────

    /** 基线归一 + 开方响应。基线处 0，满值 1。 */
    fun driveActivation(driveKey: String, value: Double): Double {
        val key = normalizeDriveKey(driveKey) ?: driveKey
        val baseline = DRIVE_BASELINES[key] ?: 0.0
        val headroom = max(1e-9, 1.0 - baseline)
        return sqrt(clamp((value - baseline) / headroom))
    }

    /** 疲劳压的是激活量，不动静息基线。 */
    fun effectiveDriveActivation(driveKey: String, value: Double, localFatigue: Double): Double =
        clamp(driveActivation(driveKey, value) * (1.0 - clamp(localFatigue)))

    /** 9 维有效激活快照（展示/账本用）。 */
    fun activationSnapshot(
        drives: Map<String, Double>,
        localFatigue: Map<String, Double>,
    ): Map<String, Double> {
        val normalized = normalizeDriveValues(drives)
        val out = LinkedHashMap<String, Double>(DRIVE_KEYS.size)
        DRIVE_KEYS.forEach { key ->
            out[key] = round3(
                effectiveDriveActivation(key, normalized.getValue(key), localFatigue[key] ?: 0.0)
            )
        }
        return out
    }

    /** 9 维有效分（raw × (1 − 局部疲劳)）。 */
    fun effectiveSnapshot(
        drives: Map<String, Double>,
        localFatigue: Map<String, Double>,
    ): Map<String, Double> {
        val normalized = normalizeDriveValues(drives)
        val out = LinkedHashMap<String, Double>(DRIVE_KEYS.size)
        DRIVE_KEYS.forEach { key ->
            out[key] = round3(normalized.getValue(key) * (1.0 - clamp(localFatigue[key] ?: 0.0)))
        }
        return out
    }

    /** PA / NA 展示坐标：正向组均值 / 负向组均值。不持久化，读时算。 */
    data class PaNa(val pa: Double, val na: Double)

    fun paNaSnapshot(drives: Map<String, Double>): PaNa {
        val normalized = normalizeDriveValues(drives)
        val pa = POSITIVE_GROUP.map { normalized.getValue(it) }.average()
        val na = NEGATIVE_GROUP.map { normalized.getValue(it) }.average()
        return PaNa(round3(pa), round3(na))
    }

    /** 当前激活最高的那一维。全为 0 时返回 null。 */
    fun dominantDrive(drives: Map<String, Double>, localFatigue: Map<String, Double>): String? {
        val activation = activationSnapshot(drives, localFatigue)
        val best = DRIVE_KEYS.maxByOrNull { activation.getValue(it) } ?: return null
        return if (activation.getValue(best) > 0.0) best else null
    }

    // ─── 组超出量 / 互抑 / 逃逸阀 ────────────────────────────────────────────

    /** 某分组里超出各自基线的部分的均值（不超出的算 0）。 */
    fun groupExcess(drives: Map<String, Double>, group: List<String>): Double {
        if (group.isEmpty()) return 0.0
        val total = group.sumOf { key ->
            max(0.0, drives.getValue(key) - (DRIVE_BASELINES[key] ?: 0.0))
        }
        return total / group.size
    }

    /**
     * ESM 软互抑：两组都在时互相压一点，压的是**超出基线的部分**，不动基线。
     *
     * 「甜蜜又心疼」—— 不是清零，也不会把任何一维压到基线以下。
     * 互抑前的 pos_excess 用于压负向组，避免处理顺序影响结果。
     */
    fun applyEsmInhibition(drives: Map<String, Double>): Map<String, Double> {
        val normalized = normalizeDriveValues(drives)
        val out = LinkedHashMap(normalized)
        val posExcess = groupExcess(normalized, POSITIVE_GROUP)
        val negExcess = groupExcess(normalized, NEGATIVE_GROUP)
        POSITIVE_GROUP.forEach { key ->
            val excess = normalized.getValue(key) - DRIVE_BASELINES.getValue(key)
            if (excess > 0) {
                out[key] = clamp(DRIVE_BASELINES.getValue(key) + excess * (1.0 - ESM_K * negExcess))
            }
        }
        NEGATIVE_GROUP.forEach { key ->
            val excess = normalized.getValue(key) - DRIVE_BASELINES.getValue(key)
            if (excess > 0) {
                out[key] = clamp(DRIVE_BASELINES.getValue(key) + excess * (1.0 - ESM_K * posExcess))
            }
        }
        return out
    }

    data class EscapeValveResult(val drives: Map<String, Double>, val streak: Int)

    /**
     * 逃逸阀（红线条款）。
     *
     * 连续 [ESCAPE_VALVE_STREAK_TRIGGER] 拍「负向组明显高于正向组」→
     * 强制把负向组超出基线的部分拉回 [ESCAPE_VALVE_PULLBACK]。
     * 用 streak 计数而非单次判断，避免一次评分波动就触发；触发后 streak 清零。
     */
    fun applyEscapeValve(drives: Map<String, Double>, streak: Int): EscapeValveResult {
        val normalized = normalizeDriveValues(drives)
        val posExcess = groupExcess(normalized, POSITIVE_GROUP)
        val negExcess = groupExcess(normalized, NEGATIVE_GROUP)

        var nextStreak = if (negExcess - posExcess > ESCAPE_VALVE_EXCESS_GAP) streak + 1 else 0
        val out = LinkedHashMap(normalized)
        if (nextStreak >= ESCAPE_VALVE_STREAK_TRIGGER) {
            NEGATIVE_GROUP.forEach { key ->
                val excess = normalized.getValue(key) - DRIVE_BASELINES.getValue(key)
                if (excess > 0) {
                    out[key] = clamp(
                        DRIVE_BASELINES.getValue(key) + excess * (1.0 - ESCAPE_VALVE_PULLBACK)
                    )
                }
            }
            nextStreak = 0
        }
        return EscapeValveResult(out, nextStreak)
    }

    // ─── 时间推进 ────────────────────────────────────────────────────────────

    /** 某维每 tick 朝基线合拢的基础比例。 */
    fun driveDampingRate(driveKey: String): Double {
        if (driveKey == "attachment") return DAMPING * ATTACHMENT_DAMPING_MULT
        val mode = DRIVE_TIME_MODES[driveKey] ?: "medium"
        return DAMPING * (DRIVE_TIME_MODE_DAMPING[mode] ?: DRIVE_TIME_MODE_DAMPING.getValue("medium"))
    }

    data class TickResult(
        val drives: Map<String, Double>,
        val localFatigue: Map<String, Double>,
        val escapeStreak: Int,
    )

    /**
     * 推进一拍：漂移 → 耦合 → 阻尼 → 日常活气托底 → 互抑 → 逃逸阀。
     *
     * **没有后台心跳**。Elektron 那边是 900 秒一拍的常驻进程；Android 侧不新开
     * 定时器，改成**惰性推进**：每次真正要动状态时（[DriveStateService.applyEvent]），
     * 先按 `last_ts` 到现在的实际间隔补算若干拍。间隔越久，[tickScale] 越大，
     * 但被夹在 0.25–4.0 —— 关掉 App 一周再打开，状态是"回落得更多"，不是"崩一下"。
     *
     * @param elapsedMillis 距上次推进的真实毫秒数
     * @param idleSeconds   距上次**用户消息**的秒数（缺席时间，驱动私人生活漂移）
     */
    fun tickDrives(
        drives: Map<String, Double>,
        escapeStreak: Int,
        elapsedMillis: Long,
        idleSeconds: Double,
    ): TickResult {
        val current = normalizeDriveValues(drives)
        val next = LinkedHashMap(current)

        // 1) 缺席漂移：attachment 不靠 idle 偷偷涨（缺席形状交给 longing），
        //    私人生活三维在空窗里慢慢冒头。
        val idleHours = max(0.0, idleSeconds) / 3600.0
        next["curiosity"] = clamp(next.getValue("curiosity") + 0.014 * idleHours)
        next["stewardship"] = clamp(next.getValue("stewardship") + 0.010 * idleHours)
        next["reflection"] = clamp(next.getValue("reflection") + 0.010 * idleHours)
        next["stress"] = clamp(next.getValue("stress") - 0.001 * idleHours)
        next["fatigue"] = clamp(next.getValue("fatigue") + 0.001 * idleHours)

        // 2) 跨维耦合
        val tickScale = if (elapsedMillis > 0) {
            clamp(elapsedMillis.toDouble() / DESIRE_TICK_MILLIS.toDouble(), 0.25, 4.0)
        } else {
            1.0
        }
        COUPLING.forEach { coupling ->
            val delta = when (coupling.mode) {
                CouplingMode.LEVEL ->
                    coupling.coefficient *
                        (next.getValue(coupling.source) - DRIVE_BASELINES.getValue(coupling.source)) *
                        tickScale

                CouplingMode.DELTA ->
                    coupling.coefficient *
                        max(0.0, next.getValue(coupling.source) - current.getValue(coupling.source))
            }
            next[coupling.target] = clamp(next.getValue(coupling.target) + delta)
        }

        // 3) 朝基线阻尼
        DRIVE_KEYS.forEach { key ->
            val baseRate = driveDampingRate(key)
            val rate = 1.0 - (1.0 - baseRate).pow(tickScale)
            next[key] = clamp(
                next.getValue(key) + rate * (DRIVE_BASELINES.getValue(key) - next.getValue(key))
            )
        }

        // 4) 私人生活活气托底：阻尼后若掉到日常底以下，轻轻托回来
        val liftFraction = 1.0 - (1.0 - PERSONAL_AMBIENT_LIFT).pow(tickScale)
        PERSONAL_LIFE_DRIVES.forEach { key ->
            val floor = DRIVE_BASELINES.getValue(key) + (PERSONAL_AMBIENT_EXCESS[key] ?: 0.08)
            if (next.getValue(key) < floor) {
                next[key] = clamp(
                    next.getValue(key) + (floor - next.getValue(key)) * liftFraction
                )
            }
        }

        // 5) 互抑 + 逃逸阀
        val inhibited = applyEsmInhibition(next)
        val valve = applyEscapeValve(inhibited, escapeStreak)

        // 6) 局部疲劳跟着全局 fatigue 重算
        return TickResult(
            drives = valve.drives,
            localFatigue = computeLocalFatigue(valve.drives.getValue("fatigue")),
            escapeStreak = valve.streak,
        )
    }

    // ─── 事件包 → drive 增量 ─────────────────────────────────────────────────

    /**
     * 一次事件包。
     *
     * [brain] 是 9 个语义特征（见 [DRIVE_EVENT_BRAIN_FEATURES]），不是 9 维 delta ——
     * 调用方不该自己发明量纲。缺的 key 视为 0。
     */
    data class DriveEvent(
        val source: String,
        val eventLabel: String,
        val primaryDrive: String? = null,
        val secondaryDrives: Map<String, Double> = emptyMap(),
        val intensity: Double = 0.5,
        val confidence: Double = 0.65,
        val agency: Double = 0.75,
        val brain: Map<String, Double> = emptyMap(),
        val evidence: List<String> = emptyList(),
    )

    /** 事件算出来的计划：要打哪些维、各打多少，以及是否被压制。 */
    data class EventPlan(
        val primaryDrive: String?,
        val proposed: Map<String, Double>,
        val suppressed: Boolean,
        val reason: String,
        val intensity: Double,
        val confidence: Double,
        val agency: Double,
        val sourceWeight: Double,
    )

    /** 一维实际发生的变化。 */
    data class DriveDelta(
        val delta: Double,
        val rawDelta: Double,
        val before: Double,
        val after: Double,
    )

    data class EventApplication(
        val applied: Map<String, DriveDelta>,
        val suppressed: Boolean,
        val reason: String,
    )

    /**
     * 把事件包折算成「要打哪些维」。
     *
     * 三条路径汇进 proposed：主驱动、显式次级驱动、brain 特征。
     * 之后做两件事：
     * 1. **侧效应预算**：一次事件的语义中心不该被一堆小 delta 盖过 ——
     *    把所有非主维的合计压到主维的 65% 以内。
     * 2. **压制判定**：没主驱动、agency 太低、confidence 太低、或算出来一个增量都没有。
     *
     * 注意 `possessiveness` 有一条独立闸：领地警报低于 [POSSESSIVENESS_TERRITORIAL_GATE]
     * 时整维不进 proposed —— 占有欲的入场门比其他维高，这是刻意的。
     */
    fun planEvent(event: DriveEvent, drives: Map<String, Double>): EventPlan {
        val normalized = normalizeDriveValues(drives)
        val primary = normalizeDriveKey(event.primaryDrive)
        val intensity = clamp(event.intensity)
        val confidence = clamp(event.confidence)
        val agency = clamp(event.agency)
        val sourceWeight = DRIVE_EVENT_SOURCE_WEIGHTS[event.source] ?: DRIVE_EVENT_DEFAULT_SOURCE_WEIGHT

        val proposed = LinkedHashMap<String, Double>()
        val suppressedReasons = mutableListOf<String>()

        // 主驱动
        if (primary != null) {
            val featureStrength = DRIVE_EVENT_BRAIN_FEATURES
                .filterValues { it.drive == primary }
                .keys
                .maxOfOrNull { event.brain[it] ?: 0.0 }
                ?: 0.0
            proposed[primary] = DRIVE_EVENT_BASE_DELTA.getValue(primary) *
                intensity *
                confidence *
                sourceWeight *
                DRIVE_EVENT_PRIMARY_FOCUS_GAIN *
                (1.0 + DRIVE_EVENT_PRIMARY_FEATURE_GAIN * featureStrength)
        }

        // 显式次级驱动
        event.secondaryDrives.forEach { (rawKey, rawValue) ->
            val key = normalizeDriveKey(rawKey) ?: return@forEach
            if (key == primary) return@forEach
            val value = clamp(rawValue)
            if (value < DRIVE_EVENT_SECONDARY_GATE) return@forEach
            val contribution = DRIVE_EVENT_BASE_DELTA.getValue(key) *
                intensity *
                confidence *
                sourceWeight *
                value *
                DRIVE_EVENT_SECONDARY_SCALE
            if (contribution >= DRIVE_EVENT_NONPRIMARY_MIN_DELTA) {
                proposed[key] = (proposed[key] ?: 0.0) + contribution
            }
        }

        // brain 特征
        DRIVE_EVENT_BRAIN_FEATURES.forEach { (feature, spec) ->
            if (spec.drive == primary) return@forEach
            val value = event.brain[feature] ?: 0.0
            val gate = max(spec.threshold, DRIVE_EVENT_NONPRIMARY_FEATURE_GATE)
            if (value <= 0.0 || value < gate) return@forEach
            val contribution = DRIVE_EVENT_BASE_DELTA.getValue(spec.drive) *
                value *
                intensity *
                confidence *
                sourceWeight *
                spec.weight *
                0.65
            if (contribution >= DRIVE_EVENT_NONPRIMARY_MIN_DELTA) {
                proposed[spec.drive] = (proposed[spec.drive] ?: 0.0) + contribution
            }
        }

        // 侧效应预算
        if (primary != null && proposed.containsKey(primary)) {
            val sideKeys = proposed.keys.filter { it != primary }
            val sideTotal = sideKeys.sumOf { proposed.getValue(it) }
            val sideBudget = proposed.getValue(primary) * 0.65
            if (sideTotal > sideBudget && sideBudget > 0.0) {
                val scale = sideBudget / sideTotal
                sideKeys.forEach { proposed[it] = proposed.getValue(it) * scale }
            }
        }

        // possessiveness 独立闸
        val territorial = event.brain["territorial_alarm"] ?: 0.0
        if (proposed.containsKey("possessiveness") && territorial < POSSESSIVENESS_TERRITORIAL_GATE) {
            if (primary == "possessiveness") suppressedReasons += "territorial_alarm below gate"
            proposed.remove("possessiveness")
        }

        var suppressed = false
        var reason = ""
        if (primary == null && proposed.isEmpty()) {
            suppressed = true
            reason = "no primary drive"
        } else if (agency < DRIVE_EVENT_AGENCY_GATE) {
            suppressed = true
            reason = "low agency"
        } else if (confidence < DRIVE_EVENT_CONFIDENCE_FLOOR) {
            suppressed = true
            reason = "low confidence"
        } else if (proposed.isEmpty()) {
            suppressed = true
            reason = suppressedReasons.joinToString("; ").ifEmpty { "no drive delta" }
        }

        return EventPlan(
            primaryDrive = primary,
            proposed = proposed,
            suppressed = suppressed,
            reason = reason,
            intensity = intensity,
            confidence = confidence,
            agency = agency,
            sourceWeight = sourceWeight,
        )
    }

    /**
     * 执行计划：把 proposed 打进 9 维，返回新的 drive 与逐维变化明细。
     *
     * 压制时不打任何维，原样返回（`applied` 为空）。
     * `attachment` 走非线性盆地跳变，其余维走普通脉冲。
     */
    fun applyPlan(
        drives: Map<String, Double>,
        plan: EventPlan,
    ): Pair<Map<String, Double>, Map<String, DriveDelta>> {
        var working = normalizeDriveValues(drives)
        val applied = LinkedHashMap<String, DriveDelta>()
        if (plan.suppressed) return working to applied

        plan.proposed.forEach { (key, rawDelta) ->
            if (rawDelta <= 0.0) return@forEach
            val before = working.getValue(key)
            val next = if (key == "attachment") {
                pulseAttachmentNonlinear(working, rawDelta)
            } else {
                pulseDrive(working, key, rawDelta)
            }
            working = next
            val after = working.getValue(key)
            if (kotlin.math.abs(after - before) > 1e-6) {
                applied[key] = DriveDelta(
                    delta = round4(after - before),
                    rawDelta = round4(rawDelta),
                    before = round4(before),
                    after = round4(after),
                )
            }
        }
        return working to applied
    }
}
