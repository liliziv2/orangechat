package me.rerere.rikkahub.data.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [MemoryPromptBudget] 的回归测试。
 *
 * 锁的是两件容易被"顺手优化"掉的语义：
 * 1. 预算裁在**召回侧**，被裁掉的记忆不许算作被想起来一次（否则 activation 虚高）；
 * 2. 裁剪按传入顺序进行，调用方负责排序 —— 两侧必须用同一个顺序，裁剪才幂等。
 */
class MemoryPromptBudgetTest {

    private data class Row(val id: Int, val size: Int)

    private fun rows(vararg sizes: Int): List<Row> {
        var id = 1
        return sizes.map { Row(id++, it) }
    }

    @Test
    fun `everything fits when the budget is generous`() {
        val items = rows(100, 100, 100)
        val selected = MemoryPromptBudget.select(items, budget = 10_000) { it.size }
        assertEquals(items, selected)
    }

    @Test
    fun `selection stops at the first record that does not fit`() {
        // 开销是 size + 64：40→104、40→104、40→104。预算 250 只能装前两条，
        // 第三条进来会是 312。必须是 break 而不是 continue ——
        // 低优先级的排后面，跳过去再装后面那等于打乱了"谁先出局"的顺序。
        val items = rows(40, 40, 40)
        val selected = MemoryPromptBudget.select(items, budget = 250) { it.size + 64 }
        assertEquals(2, selected.size)
        assertEquals(listOf(1, 2), selected.map { it.id })
    }

    @Test
    fun `the first record is always kept even when it alone exceeds the budget`() {
        // 空记忆区比超预算更糟：模型会以为"没有任何记忆"。
        val selected = MemoryPromptBudget.select(rows(9_999), budget = 100) { it.size + 64 }
        assertEquals(1, selected.size)
    }

    @Test
    fun `empty input yields empty output`() {
        assertTrue(MemoryPromptBudget.select(emptyList<Row>(), budget = 100) { it.size }.isEmpty())
    }

    @Test
    fun `cost counts both tracks and the json shell`() {
        // 只算 content 的话，双轨一进 prompt 预算就会静默超出一大截。
        val onlyContent = MemoryPromptBudget.cost("abc", "", "")
        val withTracks = MemoryPromptBudget.cost("abc", "de", "fghi")
        assertEquals(3 + 64, onlyContent)
        assertEquals(withTracks - onlyContent, 6)
    }

    @Test
    fun `selecting twice with the same order is idempotent`() {
        // 读取侧裁一次、prompt 侧兜底再裁一次 —— 第二次如果还能裁掉东西，
        // 说明两侧口径不一致，那边被砍掉的记忆就白记了一次 activation。
        val items = rows(200, 200, 200, 200, 200)
        val cost = { row: Row -> row.size + 64 }
        val once = MemoryPromptBudget.select(items, budget = 700) { cost(it) }
        val twice = MemoryPromptBudget.select(once, budget = 700) { cost(it) }
        assertEquals(once, twice)
    }

    @Test
    fun `prompt order sorts by priority descending then id ascending`() {
        // 两个类型不同的记录（实体 / 领域模型）都走这一个函数，
        // 所以这里锁的是函数本身的行为，不依赖任何一方的字段名。
        data class Item(val id: Int, val priority: Int)

        val items = listOf(
            Item(3, 0),
            Item(1, 2),
            Item(2, 2),
            Item(4, 1),
        )
        val ordered = items.sortedWith(
            MemoryPromptBudget.promptOrder<Item>(priorityOf = { it.priority }, idOf = { it.id })
        )
        assertEquals(listOf(1, 2, 4, 3), ordered.map { it.id })
    }
}
