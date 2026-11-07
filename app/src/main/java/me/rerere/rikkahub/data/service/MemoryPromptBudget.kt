package me.rerere.rikkahub.data.service

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import me.rerere.rikkahub.data.model.AssistantMemory
import me.rerere.rikkahub.utils.JsonInstantPretty

/** Shared hard record budget. UTF-8 bytes conservatively estimate token cost; not a model tokenizer. */
internal object MemoryPromptBudget {
    const val BUDGET_CHARS = 6000
    const val BUDGET_TOKEN_ESTIMATE = 6000

    fun record(memory: AssistantMemory) = buildJsonObject {
        put("id", memory.id)
        put("content", memory.content)
        put("fact_track", memory.factTrack)
        put("feel_track", memory.feelTrack)
        put("category", memory.category.serialName)
        put("priority", memory.priority)
    }

    fun cost(memory: AssistantMemory): Int {
        val encoded = JsonInstantPretty.encodeToString(record(memory))
        // Byte cost covers CJK, escaping and JSON fields. Reserve array/indent overhead per record.
        return maxOf(encoded.length, encoded.toByteArray(Charsets.UTF_8).size) + 32
    }

    fun cost(content: String, factTrack: String, feelTrack: String): Int =
        content.length + factTrack.length + feelTrack.length + 64

    fun <T> select(items: List<T>, budget: Int = BUDGET_TOKEN_ESTIMATE, costOf: (T) -> Int): List<T> {
        require(budget >= 0)
        val included = mutableListOf<T>()
        var remaining = budget
        for (item in items) {
            val cost = costOf(item)
            require(cost >= 0)
            if (cost > remaining) continue
            included += item
            remaining -= cost
        }
        return included
    }

    fun <T> promptOrder(priorityOf: (T) -> Int, idOf: (T) -> Int): Comparator<T> =
        compareByDescending<T> { priorityOf(it) }.thenBy { idOf(it) }
}
