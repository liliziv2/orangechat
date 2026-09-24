package me.rerere.rikkahub.data.service

/**
 * 三段式记忆的格式约定与解析。
 *
 * 两条摘要路径（`ChatService.closeoutConversationToMemory` 与 `DiarySummaryService`）
 * 都要模型按同一套格式输出，解析也必须是同一份代码 —— 否则"格式说明改了一处、
 * 解析没跟上"会让记忆静默地写不进去，而且只在真机上才看得出来。
 *
 * 三段对应 [MemoryBankService.MemoryWriteRequest] 的 content / factTrack / feelTrack，
 * 缺任何一段都不构成一条合格的记忆。
 */

/** 三段的标签，顺序固定：现场 / 事实 / 感受。prompt 里也用它拼，别手写字面量。 */
internal val MEMORY_SECTION_LABELS = listOf("现场", "事实", "感受")

/** 一次解析出来的三段。 */
internal data class StructuredMemory(
    val scene: String,
    val fact: String,
    val feel: String,
)

/**
 * 拼出给模型的三段式格式说明。
 *
 * 标签从 [MEMORY_SECTION_LABELS] 取，保证 prompt 里写的和解析器认的是同一组词 ——
 * 改标签只改一处。各段的长度要求由调用方通过 `sceneHint` 传入，两条摘要路径的篇幅
 * 要求本来就不一样（收尾摘要短、日记长），这里不做统一。
 */
internal fun memorySectionFormatHint(sceneHint: String): String = buildString {
    appendLine("记忆由三部分组成，缺一不可。严格按下面的格式逐行输出，不要加别的标题或说明：")
    appendLine("${MEMORY_SECTION_LABELS[0]}：<当时发生了什么、说了什么、什么语气。$sceneHint>")
    appendLine("${MEMORY_SECTION_LABELS[1]}：<行为、时间线、承诺等有客观依据的部分>")
    appendLine("${MEMORY_SECTION_LABELS[2]}：<情绪、温度、这段交流带来的影响>")
}

/**
 * 解析"现场 / 事实 / 感受"三段式输出。
 *
 * 三段都齐才返回；缺任何一段都返回 null，由调用方决定不写这条记忆。宁可少一条记忆，
 * 也不要把半截记忆塞进库里 —— 缺轨的记忆读回来只剩结论，不知道那是什么感觉。
 *
 * 容错的地方只有两处：全角/半角冒号都认；标签后换行写正文也认（续行会并进当前段）。
 */
internal fun parseStructuredMemory(raw: String): StructuredMemory? {
    val sections = mutableMapOf<String, StringBuilder>()
    var current: String? = null
    for (line in raw.lines()) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) continue
        val label = MEMORY_SECTION_LABELS.firstOrNull {
            trimmed.startsWith("$it：") || trimmed.startsWith("$it:")
        }
        if (label != null) {
            current = label
            val body = trimmed.removePrefix("$label：").removePrefix("$label:").trim()
            if (body.isNotEmpty()) {
                sections.getOrPut(label) { StringBuilder() }.append(body)
            }
        } else {
            current?.let { sections.getOrPut(it) { StringBuilder() }.append(' ').append(trimmed) }
        }
    }
    val scene = sections[MEMORY_SECTION_LABELS[0]]?.toString()?.trim().orEmpty()
    val fact = sections[MEMORY_SECTION_LABELS[1]]?.toString()?.trim().orEmpty()
    val feel = sections[MEMORY_SECTION_LABELS[2]]?.toString()?.trim().orEmpty()
    if (scene.isEmpty() || fact.isEmpty() || feel.isEmpty()) return null
    return StructuredMemory(scene = scene, fact = fact, feel = feel)
}
