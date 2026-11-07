package me.rerere.rikkahub.data.service

import me.rerere.rikkahub.data.db.entity.MemoryBankEntity
import me.rerere.rikkahub.data.model.AssistantMemory
import org.junit.Assert.*
import org.junit.Test

class MemoryRecallPolicyTest {
    @Test fun `relevant row beyond first two hundred is found`() {
        val rows = (1..650).map { MemoryBankEntity(id = it, content = if (it == 649) "约好去看水母" else "工作记录", decayScore = (1000 - it).toFloat()) }
        var hits = emptyList<MemoryBankEntity>()
        rows.chunked(200).forEach { page ->
            hits = MemoryRecallPolicy.mergeRelevant(hits, page, MemoryRelevance.tokenize("水母"), 200)
        }
        assertEquals(listOf(649), hits.map { it.id })
    }
    @Test fun `query keyword beyond first eight tokens remains searchable`() {
        val tokens = MemoryRelevance.tokenize("今天有好多别的事情想说最后才说起水母")
        val hits = MemoryRecallPolicy.mergeRelevant(emptyList(), listOf(MemoryBankEntity(id = 1, content = "水母")), tokens, 200)
        assertEquals(1, hits.size)
    }
    @Test fun `archived rows excluded and decay order preserved`() {
        val page = listOf(MemoryBankEntity(id = 1, content = "水母", archived = true), MemoryBankEntity(id = 2, content = "水母", decayScore = 1f), MemoryBankEntity(id = 3, factTrack = "水母", decayScore = 2f))
        assertEquals(listOf(3, 2), MemoryRecallPolicy.mergeRelevant(emptyList(), page, setOf("水母"), 10).map { it.id })
    }
    @Test(expected = IllegalArgumentException::class)
    fun `cross assistant mutation rejected`() { MemoryRecallPolicy.requireOwner("a", "b") }
    @Test fun `same owner and explicit management access allowed`() {
        MemoryRecallPolicy.requireOwner("a", "a")
        MemoryRecallPolicy.requireOwner("a", null)
    }
    @Test fun `oversized memory skipped smaller memory survives`() {
        val memories = listOf(AssistantMemory(id = 1, content = "长".repeat(8000)), AssistantMemory(id = 2, content = "水母\n\"约定\""))
        val selected = MemoryPromptBudget.select(memories) { MemoryPromptBudget.cost(it) }
        assertEquals(listOf(2), selected.map { it.id })
        assertTrue(selected.sumOf { MemoryPromptBudget.cost(it) } <= MemoryPromptBudget.BUDGET_TOKEN_ESTIMATE)
        assertEquals(selected, MemoryPromptBudget.select(selected) { MemoryPromptBudget.cost(it) })
    }
}
