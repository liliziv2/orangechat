package me.rerere.rikkahub.data.service

/**
 * 记忆注入的字符预算 —— 读取侧和 prompt 侧共用的**唯一一份**。
 *
 * 之前这个上限只写在 `buildMemoryPrompt` 里：召回侧先无条件 `touch` 命中的 8 条
 * （激活度 +1、`last_active_at` 前移），再由 prompt 侧按预算砍掉一部分。
 * 结果是被砍掉的那几条其实一条都没进模型上下文，却照样被算作"被想起来一次"，
 * activation 虚高、decay_score 也跟着偏高 —— 越是长记忆越容易触发，因为它们的
 * 成本大、更容易被砍。
 *
 * 所以预算必须前移到召回侧：**只有真正塞得进 prompt 的那几条才算被想起来**。
 * prompt 侧仍然调用同一个函数，是为了在其它调用方绕过召回直接喂记忆时兜底；
 * 两边排序口径相同，这里就是幂等的，不会二次裁剪。
 */
internal object MemoryPromptBudget {
    /**
     * 留给记忆的字符上限。
     *
     * 记忆是只增不减的：助手每聊几轮就可能记一条，半年后攒到几百条，
     * 而它们每一轮都要整份塞进 system prompt。没有上限既白烧 token，
     * 又会把真正的对话历史挤出上下文窗口 —— 挤掉的总是最近的对话，
     * 因为 system prompt 在最前面，不会被上游截断。
     */
    const val BUDGET_CHARS = 6000

    /**
     * 单条记录的 JSON 外壳开销（id / category / priority / 双轨键名与括号引号）粗算值。
     *
     * 正文必须按实际长度算，不能只算 `content`：双轨一进 prompt，
     * 只算正文的话预算会静默超出一大截。
     */
    private const val RECORD_OVERHEAD_CHARS = 64

    fun cost(content: String, factTrack: String, feelTrack: String): Int =
        content.length + factTrack.length + feelTrack.length + RECORD_OVERHEAD_CHARS

    /**
     * 按传入顺序填预算，填不下就停。
     *
     * 调用方必须先把列表排好 —— 这里的顺序就是"谁先被舍弃"的顺序。
     * 两侧统一用 priority 降序，所以低优先级的记忆先出局。
     *
     * 第一条无论如何都保留：宁可超一点预算，也不要出现"记忆区是空的"。
     */
    fun <T> select(
        items: List<T>,
        budget: Int = BUDGET_CHARS,
        costOf: (T) -> Int,
    ): List<T> {
        val included = mutableListOf<T>()
        var usedChars = 0
        for (item in items) {
            val cost = costOf(item)
            if (usedChars + cost > budget && included.isNotEmpty()) break
            included += item
            usedChars += cost
        }
        return included
    }

    /**
     * 注入顺序：priority 降序、id 升序。
     *
     * 两侧（召回侧 `MemoryBankService` 与 prompt 侧 `buildMemoryPrompt`）**必须**
     * 用这一个函数生成排序：顺序不一致时，召回侧裁掉的和 prompt 侧再裁掉的不是同一批，
     * 被 prompt 侧砍掉的记忆会白记一次 activation，前面那次裁剪也就白做了。
     *
     * 泛型而不是各自写 comparator，就是因为这两处的记录类型不同（实体 / 领域模型），
     * 各自手写一份迟早会漂移。
     */
    fun <T> promptOrder(priorityOf: (T) -> Int, idOf: (T) -> Int): Comparator<T> =
        compareByDescending<T> { priorityOf(it) }.thenBy { idOf(it) }
}
