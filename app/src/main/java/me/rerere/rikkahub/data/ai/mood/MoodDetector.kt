/*
 * Pelle d'Umore — Emotional Skin for AI Chat
 * CC BY 4.0 — Attribution required
 * Ported to Compose for OrangeChat
 *
 * MoodDetector — buffers the streaming token stream to detect <mood>…</mood>
 * tags that may arrive fragmented across chunk boundaries.
 *
 * Protocol (from PROTOCOL.md):
 *   - Tag format: <mood>value</mood>
 *   - Valid values: rage, rage2, desire, vuoto, moonlight, off
 *   - Invalid/unknown value → ignore silently, strip tag only
 *   - Tag opened but never closed by end-of-turn → drop silently
 *   - Emit mood event once per turn at point of resolution
 */

package me.rerere.rikkahub.data.ai.mood

private const val TAG = "MoodDetector"

/**
 * Simple stream processor that detects `<mood>…</mood>` tags
 * across arbitrary chunk boundaries.
 *
 * Usage:
 * ```kotlin
 * val detector = MoodDetector()
 * detector.push(chunk1)   // -> (cleanedText, moodEvent?)
 * detector.push(chunk2)   // -> (cleanedText, moodEvent?)
 * detector.endOfTurn()    // drop any unclosed tag residue
 * ```
 */
class MoodDetector {

    private val buffer = StringBuilder()

    /**
     * Push a chunk of streaming text. Returns:
     * - `cleanedText`: text with `<mood>…</mood>` stripped
     * - `moodEvent`: non-null exactly once when a complete mood tag resolves
     */
    fun push(chunk: String): Result {
        buffer.append(chunk)
        val result = processBuffer()
        return Result(result.first, result.second)
    }

    /**
     * Process the accumulated buffer looking for complete <mood> tags.
     * Any text before/after/between tags is kept; only the mood tag itself
     * is stripped.
     */
    private fun processBuffer(): Pair<String, MoodMode?> {
        val text = buffer.toString()
        val regex = Regex("<mood>\\s*(\\w+)\\s*</mood>", RegexOption.DOT_MATCHES_ALL)
        val match = regex.find(text)

        if (match != null) {
            val value = match.groupValues[1].trim().lowercase()
            val mode = MoodMode.fromTag(value)

            // Remove the matched tag from the buffer
            val cleaned = text.removeRange(match.range)
            buffer.clear()
            buffer.append(cleaned)

            return Pair(cleaned, mode)
        }

        // No complete tag found — check if we have a partial tag that
        // might complete in a future chunk. If so, don't flush text
        // that could be part of a pending tag.

        // If the buffer ends with a prefix of "<mood>..." or "</mood>",
        // hold it back. Otherwise flush everything.
        val incompleteTagPrefixes = listOf(
            "<", "<m", "<mo", "<moo", "<mood",
            "<", "</", "</m", "</mo", "</moo", "</mood"
        )
        val pendingPrefix = incompleteTagPrefixes.firstOrNull { text.endsWith(it) }

        return if (pendingPrefix != null) {
            // Hold the incomplete tag prefix back
            val flushLen = text.length - pendingPrefix.length
            val flushed = text.substring(0, flushLen)
            buffer.clear()
            buffer.append(pendingPrefix)
            Pair(flushed, null)
        } else {
            // No pending tag — flush everything
            buffer.clear()
            Pair(text, null)
        }
    }

    /**
     * Call at end of turn. Drops any unclosed `<mood>` silently.
     * Returns any remaining non-tag text that was being buffered.
     */
    fun endOfTurn(): String {
        val remaining = buffer.toString()
        buffer.clear()
        // If the residual looks like a partial mood tag, drop it silently
        val partialTag = Regex("<m?o?o?d?/?>?$")
        return if (remaining.contains("<mood") || remaining.contains("</mood")) {
            "" // drop silently — open tag never closed
        } else {
            remaining // non-tag residual that was held back
        }
    }

    data class Result(
        val cleanedText: String,
        val moodEvent: MoodMode?
    )
}