package me.rerere.rikkahub.data.service

/**
 * 对话事件源 —— 把一条对话消息折算成 [DriveEngine.DriveEvent]。
 *
 * ## 这里做什么、不做什么
 *
 * Elektron 的事件包是由一个**分析器**（LLM）填 `brain` 里那 9 个语义特征的：
 * 「这次对话里有多少靠近感」「有没有领地警报」…… 那套东西依赖模型调用与
 * 独立的分析管线，属于后续阶段。
 *
 * 本轮**不做语义分析**。这里只用**可观测的对话结构** —— 消息长度、谁在说话 ——
 * 折算出一组确定性的特征值。所以：
 *
 * - 它是**结构性事件**，不是「读懂了这条消息」；
 * - 同样的输入永远得到同样的输出（纯函数），不依赖网络、模型、时钟；
 * - 它给出的 drive 变化是**方向正确但幅度粗糙**的：一条长消息确实意味着更多的
 *   表达与更多的靠近，但它不区分"我今天很累"和"我今天很想你"。
 *
 * 这么做是刻意的：**宁可有据可查的粗糙，也不要凭空编出来的精确**。
 * 等分析器接上以后，`brain` 会由真实语义填，这个构造器退化成兜底路径。
 *
 * ## 归一化常数为什么是这些值
 *
 * [FULL_LENGTH_CHARS] 是「算作一次充分表达」的字符数，纯粹是拍的工程常数，
 * 不是从 Elektron 来的（那边没有对话长度这个特征）。它的作用只是把长度映射到
 * 0..1 —— 选 240 是因为中文一次长回复大约在这个量级。**要调就调这一个数**，
 * 下面所有系数都挂在它上面。
 */
object DialogueEventSource {

    /** 用户消息来源标识。权重 1.00，是 [DriveEngine.DRIVE_EVENT_SOURCE_WEIGHTS] 里最高的一档。 */
    const val SOURCE_USER_MESSAGE = "user_message"

    /** 事件标签，写进账本，用来回答「这条记录是哪来的」。 */
    const val LABEL_USER_MESSAGE = "user_message"

    /** 算作「一次充分表达」的字符数。见类注释。 */
    const val FULL_LENGTH_CHARS = 240.0

    /** 证据字段保留的最大字符数。账本是审计面，不该把整条长消息抄进去。 */
    const val EVIDENCE_MAX_CHARS = 200

    /**
     * 一条用户消息 → 一个事件包。
     *
     * 三个特征都只由长度驱动，且都留了非零下限 —— **一条消息的存在本身就是一次靠近**，
     * 哪怕它只有「嗯」一个字。这不是修辞：Elektron 把 `user_message` 的权重定成
     * 最高一档（1.00，高于外部环境事件），意思就是「她主动来说话」这件事本身最重。
     *
     * | 特征 | 含义 | 下限 → 上限 |
     * |---|---|---|
     * | `closeness_pull` | 靠近 | 0.35 → 0.60 |
     * | `expression_pressure` | 表达压力 | 0.30 → 0.80 |
     * | `energy_cost` | 交互消耗 | 0.20 → 0.55 |
     *
     * 主驱动取 `attachment` —— 因为 `closeness_pull` 是三个特征里语义中心，
     * 而它按 [DriveEngine.DRIVE_EVENT_BRAIN_FEATURES] 映射到 `attachment`。
     * 另外两个特征会作为次级贡献进入 `social` 与 `fatigue`（各自要过入场门）。
     *
     * @param text 用户消息的可见文本（已拼好多段 part）
     */
    fun userMessage(text: String): DriveEngine.DriveEvent {
        val trimmed = text.trim()
        val lengthFactor = DriveEngine.clamp(trimmed.length / FULL_LENGTH_CHARS)

        val closenessPull = DriveEngine.clamp(0.35 + 0.25 * lengthFactor)
        val expressionPressure = DriveEngine.clamp(0.30 + 0.50 * lengthFactor)
        val energyCost = DriveEngine.clamp(0.20 + 0.35 * lengthFactor)

        return DriveEngine.DriveEvent(
            source = SOURCE_USER_MESSAGE,
            eventLabel = LABEL_USER_MESSAGE,
            primaryDrive = "attachment",
            intensity = DriveEngine.clamp(0.35 + 0.40 * lengthFactor),
            // 结构性事件的置信度刻意保守：我们知道"有人在说话"，但不知道"这有多重要"。
            confidence = 0.65,
            agency = 0.75,
            brain = mapOf(
                "closeness_pull" to closenessPull,
                "expression_pressure" to expressionPressure,
                "energy_cost" to energyCost,
            ),
            evidence = listOf(buildEvidence(trimmed)),
        )
    }

    /** 账本里的证据：截断到 [EVIDENCE_MAX_CHARS]，空白消息给一个明确的占位。 */
    fun buildEvidence(text: String): String {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return "(empty)"
        return if (trimmed.length <= EVIDENCE_MAX_CHARS) {
            trimmed
        } else {
            trimmed.take(EVIDENCE_MAX_CHARS) + "…"
        }
    }
}
