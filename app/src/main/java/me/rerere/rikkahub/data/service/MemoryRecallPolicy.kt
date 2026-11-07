package me.rerere.rikkahub.data.service

import me.rerere.rikkahub.data.db.entity.MemoryBankEntity

/** Pure recall rules; full-library pages are merged without truncating query tokens. */
internal object MemoryRecallPolicy {
    fun requireOwner(actual: String?, expected: String?) {
        require(expected == null || actual == expected) { "memory belongs to another library" }
    }

    fun mergeRelevant(
        previous: List<MemoryBankEntity>,
        page: List<MemoryBankEntity>,
        tokens: Set<String>,
        limit: Int,
    ): List<MemoryBankEntity> = (previous + page.filter {
        !it.archived && MemoryRelevance.overlap(tokens, it) > 0.0
    }).distinctBy { it.id }.sortedWith(
        compareByDescending<MemoryBankEntity> { it.decayScore }
            .thenByDescending { it.createdAt }.thenBy { it.id }
    ).take(limit)
}
