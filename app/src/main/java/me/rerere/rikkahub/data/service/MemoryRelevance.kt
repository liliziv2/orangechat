package me.rerere.rikkahub.data.service

import me.rerere.rikkahub.data.db.entity.MemoryBankEntity

/**
 * 召回相关性 —— 纯函数，无 IO。
 *
 * 本地 embedding 已经废弃（见 [MemoryBankService.vectorRecall]，它现在直接返回空列表），
 * 所以「当前消息 ↔ 记忆」的相关性只能靠原文匹配。这个对象只回答一个问题：
 * **这条记忆和这句话沾不沾边**。
 *
 * 刻意只输出一个 [0,1] 的重叠比例，**不输出加权总分**：importance / activation / 时间 /
 * 情绪已经全部在 [MemoryDecayEngine] 的 `decay_score` 里，这里再乘一套权重就是第二套
 * 并行排序框架。排序仍然只由 `decay_score` 负责 —— 相关性只当闸门。
 *
 * 与 `MemoryDecayEngine` 一样抽成独立文件，是为了让公式可被单测覆盖：
 * 这里不碰 Room，也不碰时钟。
 */
internal object MemoryRelevance {

    /** CJK 取 2-gram：单字 token（"的"、"了"）命中率太高、信息量太低。 */
    private const val CJK_NGRAM = 2

    /** 拉丁词短于这个长度不参与（"a"、"of" 同理）。 */
    private const val MIN_LATIN_WORD = 2

    /**
     * 把一段文本切成用于匹配的 token 集合。
     *
     * - CJK（汉字 / 假名）连续段取 2-gram：「国行还是水货」→ 国行 / 行还 / 还是 / 是水 / 水货
     * - 拉丁字母与数字连续段取小写整词，长度需 ≥ [MIN_LATIN_WORD]
     *
     * 返回**集合**而不是列表：同一个词在一句话里出现两次，不该让重叠比例超过 1。
     */
    fun tokenize(text: String): Set<String> {
        val tokens = mutableSetOf<String>()
        val latinRun = StringBuilder()

        fun flushLatinRun() {
            if (latinRun.length >= MIN_LATIN_WORD) tokens.add(latinRun.toString().lowercase())
            latinRun.setLength(0)
        }

        var index = 0
        while (index < text.length) {
            val ch = text[index]
            when {
                isCjk(ch) -> {
                    flushLatinRun()
                    var end = index
                    while (end < text.length && isCjk(text[end])) end++
                    val segment = text.substring(index, end)
                    if (segment.length < CJK_NGRAM) {
                        tokens.add(segment)
                    } else {
                        for (start in 0..segment.length - CJK_NGRAM) {
                            tokens.add(segment.substring(start, start + CJK_NGRAM))
                        }
                    }
                    index = end
                }

                ch.isLetterOrDigit() -> {
                    latinRun.append(ch)
                    index++
                }

                else -> {
                    flushLatinRun()
                    index++
                }
            }
        }
        flushLatinRun()
        return tokens
    }

    /**
     * 这条记忆覆盖了查询里多少东西，[0,1]。
     *
     * 分母是**查询**的 token 数，不是记忆的长度 —— 否则一条很长的记忆会因为长而更容易
     * 命中，那跟相关性没关系。
     *
     * 匹配用原文 `contains` 而不是「把记忆也切一遍再求交集」：记忆里的同一段文字可能被
     * 标点或换行断开（"国行，还是水货"），重新切会漏掉；而 `contains` 只看字符相邻，
     * 与查询侧 2-gram 的切法天然对齐。
     */
    fun overlap(queryTokens: Set<String>, memory: MemoryBankEntity): Double {
        if (queryTokens.isEmpty()) return 0.0
        val haystack = buildString {
            append(memory.content)
            append('\n')
            append(memory.factTrack.orEmpty())
            append('\n')
            append(memory.feelTrack.orEmpty())
        }.lowercase()
        if (haystack.isBlank()) return 0.0
        val hits = queryTokens.count { haystack.contains(it) }
        return hits.toDouble() / queryTokens.size
    }

    /** 参与 2-gram 的字符：汉字（含扩展 A / 兼容区）与日文假名。 */
    private fun isCjk(ch: Char): Boolean {
        val code = ch.code
        return code in 0x3040..0x30FF ||
            code in 0x3400..0x4DBF ||
            code in 0x4E00..0x9FFF ||
            code in 0xF900..0xFAFF
    }
}
