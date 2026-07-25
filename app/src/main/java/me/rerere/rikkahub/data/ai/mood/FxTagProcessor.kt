/*
 * Pelle d'Umore — Emotional Skin for AI Chat
 * CC BY 4.0 — Attribution required
 * Ported to Compose for OrangeChat
 *
 * FxTagProcessor — extracts inline effect tags [glow]…[/glow] etc.
 * from raw AI text BEFORE markdown parsing.
 *
 * Strategy (mirrors fx.js but for Compose):
 *   1) BEFORE markdown parsing → extractFxTags(): swap every [tag]…[/tag]
 *      for a PUA placeholder, stash the inner text.
 *   2) AFTER markdown → AnnotatedString building → injectFxSpans():
 *      when the AnnotatedString builder encounters a PUA placeholder,
 *      apply the corresponding SpanStyle.
 *
 * Supported tags:
 *   [glow]     soft accent halo — text shadow glow
 *   [big]      larger font (1.4em)
 *   [huge]     much larger font (1.8em)
 *   [whisper]  small and dim (0.85em, faded color)
 *   [red]      danger/warning color
 *   [shake]    trembling (draw offset animation)  — complex, see notes
 *   [blur]     blurred until tap to reveal     — needs interaction
 *   [glitch]   RGB-split character corruption  — complex rendering
 */

package me.rerere.rikkahub.data.ai.mood

/**
 * An extracted inline effect tag.
 */
data class FxTag(
    val name: String,       // e.g. "glow", "shake"
    val inner: String,      // the raw inner text (plain text, no markdown re-parse)
)

/**
 * Result of preprocessing: cleaned text with placeholders, plus tag metadata.
 */
data class FxExtractionResult(
    val text: String,
    val tags: List<FxTag>,
)

/**
 * Preprocesses raw AI text before markdown parsing.
 *
 * Call in the pipeline BEFORE markdown parsing:
 * ```
 * val preprocessed = FxTagProcessor.extract(rawText)
 * // pass preprocessed.text to markdown parser
 * // during AnnotatedString building, use preprocessed.tags to apply styles
 * ```
 */
object FxTagProcessor {

    /** PUA codepoints that survive markdown parsing as literal text */
    private const val PH_OPEN = '\uE010'
    private const val PH_CLOSE = '\uE011'

    /** Regex: [tag]inner[/tag] — 8 effects, lowercase, no nesting */
    private val FX_RE = Regex(
        """\[(glow|big|huge|whisper|red|shake|blur|glitch)](.*?)\[/\1]""",
        RegexOption.DOT_MATCHES_ALL
    )

    /**
     * Extract all inline FX tags from raw text, replacing them with
     * PUA placeholders that survive the markdown parser.
     */
    fun extract(text: String): FxExtractionResult {
        if (text.isBlank()) return FxExtractionResult(text, emptyList())

        val tags = mutableListOf<FxTag>()
        val result = text.replace(FX_RE) { match ->
            val name = match.groupValues[1]
            val inner = match.groupValues[2]
            val idx = tags.size
            tags.add(FxTag(name, inner))
            "$PH_OPEN${idx}$PH_CLOSE"
        }

        return FxExtractionResult(result, tags)
    }

    /**
     * Build a PUA placeholder string for the given tag index.
     * Used during AnnotatedString building to detect placeholders.
     */
    fun placeholderFor(index: Int): String = "$PH_OPEN$index$PH_CLOSE"
}