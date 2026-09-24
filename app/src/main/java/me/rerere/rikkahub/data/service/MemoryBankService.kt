package me.rerere.rikkahub.data.service

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import me.rerere.rikkahub.data.db.dao.MemoryBankDAO
import me.rerere.rikkahub.data.db.entity.MemoryBankEntity
import me.rerere.rikkahub.data.model.AssistantMemory
import me.rerere.rikkahub.data.model.memoryCategoryFromDomain
import me.rerere.rikkahub.data.model.memoryImportanceToPriority
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.sqrt

private const val TAG = "MemoryBankService"

/**
 * 衰减分重算的时间窗。
 *
 * 得分随时间单调下降，不重算排序就停在写入那一刻；但每次召回都全表重算又太贵。
 * 半小时一次对"排序"这个用途足够 —— 记忆的排序不需要秒级新鲜。
 */
private const val DECAY_REFRESH_INTERVAL_MS = 30 * 60 * 1000L

/** 注入 prompt 时默认召回条数。 */
private const val PROMPT_RECALL_COUNT = 8

/** `source_type` 里表示"助手在对话中自行沉淀、未经用户确认"的取值。 */
private const val SOURCE_TYPE_AUTO = "auto"

class MemoryBankService(
    private val memoryBankDAO: MemoryBankDAO,
    private val okHttpClient: OkHttpClient,
    private val context: Context,
) {
    companion object {
        /**
         * 全局记忆库的 assistantId 哨兵。
         *
         * 字面量沿用原来的 `MemoryRepository.GLOBAL_MEMORY_ID`，**不能改** ——
         * 库里已有的全局记忆就是按这个字符串存的，换一个值等于让它们全部读不出来。
         */
        const val GLOBAL_MEMORY_ID = "__global__"
    }

    /** 上次衰减重算的时刻。最坏情况两个线程同时重算一次，结果一样，不值得为它加锁。 */
    @Volatile
    private var lastDecayRefreshAt: Long = 0L

    data class MemoryStats(
        val total: Int = 0,
        val messageCount: Int = 0,
        val summaryCount: Int = 0,
        val manualCount: Int = 0,
        val vectorizedCount: Int = 0,
        val pendingCount: Int = 0,
        val failedCount: Int = 0,
    )

    val recallCount: Int = 3

    suspend fun getAssistantIds(): List<String> = withContext(Dispatchers.IO) {
        memoryBankDAO.getDistinctAssistantIds()
    }

    suspend fun getStats(assistantId: String? = null): MemoryStats = withContext(Dispatchers.IO) {
        val total = if (assistantId != null) {
            memoryBankDAO.getCountByAssistant(assistantId)
        } else {
            memoryBankDAO.getTotalCount()
        }
        val messageCount = if (assistantId != null) {
            memoryBankDAO.getCountByAssistantAndType(assistantId, "message")
        } else {
            memoryBankDAO.getCountByType("message")
        }
        val summaryCount = memoryBankDAO.getSummaryCount()
        val manualCount = if (assistantId != null) {
            memoryBankDAO.getCountByAssistantAndType(assistantId, "manual")
        } else {
            memoryBankDAO.getCountByType("manual")
        }
        val vectorizedCount = if (assistantId != null) {
            0
        } else {
            memoryBankDAO.getCountByVectorStatus("done")
        }
        val pendingCount = if (assistantId != null) {
            0
        } else {
            memoryBankDAO.getCountByVectorStatus("pending")
        }
        val failedCount = if (assistantId != null) {
            0
        } else {
            memoryBankDAO.getCountByVectorStatus("failed")
        }
        MemoryStats(
            total = total,
            messageCount = messageCount,
            summaryCount = summaryCount,
            manualCount = manualCount,
            vectorizedCount = vectorizedCount,
            pendingCount = pendingCount,
            failedCount = failedCount
        )
    }

    suspend fun getTodayPhaseSummaries(assistantId: String? = null): List<MemoryBankEntity> = withContext(Dispatchers.IO) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (assistantId != null) {
            memoryBankDAO.getMemoriesByAssistantTypeAndDateGroup(assistantId, "phase_summary", today)
        } else {
            memoryBankDAO.getMemoriesByTypeAndDateGroup("phase_summary", today)
        }
    }

    suspend fun getDailySummaries(assistantId: String? = null): List<MemoryBankEntity> = withContext(Dispatchers.IO) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        if (assistantId != null) {
            memoryBankDAO.getMemoriesByAssistantTypeAndDateGroup(assistantId, "daily_summary", today)
        } else {
            memoryBankDAO.getMemoriesByTypeAndDateGroup("daily_summary", today)
        }
    }

    suspend fun searchMemories(
        keyword: String = "",
        type: String = "",
        limit: Int = 100,
        assistantId: String? = null
    ): List<MemoryBankEntity> = withContext(Dispatchers.IO) {
        if (keyword.isNotBlank() && type.isNotBlank()) {
            memoryBankDAO.searchMemoriesByKeywordAndType(keyword, type, limit)
        } else if (keyword.isNotBlank()) {
            memoryBankDAO.searchMemoriesByKeyword(keyword, limit)
        } else if (type.isNotBlank() && assistantId != null) {
            memoryBankDAO.getMemoriesByAssistantAndTypeLimit(assistantId, type, limit)
        } else if (type.isNotBlank()) {
            memoryBankDAO.getMemoriesByTypeLimit(type, limit)
        } else {
            memoryBankDAO.getRecentMemories(limit)
        }
    }

    suspend fun deleteMemory(id: Int) = withContext(Dispatchers.IO) {
        memoryBankDAO.deleteMemoryById(id)
    }

    suspend fun rebuildIndex() {
        // No-op: vector index removed
    }

    suspend fun processPendingVectors() {
        // No-op: vector processing removed
    }

    /**
     * 单轨写入。**这是全仓唯一允许产生"缺轨记忆"的入口**，别在别处再开一个。
     *
     * 调用方只有插件沙箱（`PluginSandbox` 的 `memory.save`），它给的就是一段文本：
     * 插件是外部生产者，手里没有情绪轨可给，硬要求双轨等于把已装的插件全部打断。
     * 所以这里如实接受单轨，并把 `feel_track` 留 NULL —— 与从 `memoryentity`
     * 迁过来的老记忆用同一种表示法（null = 这一轨缺失），不是假装它有感受。
     *
     * 除此之外**一切写入都必须走 [writeMemory]**：AI 的 `memory_tool`、记忆管理页、
     * 收尾摘要、日记，四处的现场都在，没有理由分轨。
     */
    suspend fun saveManualMemory(content: String): MemoryBankEntity = withContext(Dispatchers.IO) {
        // 时间字段必须显式写。默认值 0 会被 MemoryDecayEngine.daysSince 兜底成 30 天，
        // 等于刚写进去就被判成"很久没动" —— 这正是 PITFALLS 里「搬家之后所有旧记忆的
        // 排序全乱了」那次的根因，只是这次会发生在每一条新记忆上。
        val now = System.currentTimeMillis()
        val entity = MemoryBankEntity(
            content = content,
            type = "manual",
            createdAt = now,
            lastActiveAt = now,
            // 插件路径没有情绪轨，如实留空
            sourceType = "plugin",
            vectorStatus = "skipped",
        )
        val scored = entity.copy(decayScore = scoreOf(entity, now))
        val id = memoryBankDAO.insertMemory(scored).toInt()
        scored.copy(id = id)
    }

    suspend fun recallMemories(query: String, count: Int): List<MemoryBankEntity> = withContext(Dispatchers.IO) {
        if (query.isNotBlank()) {
            memoryBankDAO.searchMemoriesByKeyword(query, count)
        } else {
            memoryBankDAO.getRecentMemories(count)
        }
    }

    // ==================== Vector Recall ====================

    /**
     * 向量召回：从 Supabase 远端查 embedding 做余弦相似度
     * 本地不再存储 embedding，全部走 ExternalMemoryService
     */
    suspend fun vectorRecall(
        queryEmbedding: List<Float>,
        assistantId: String? = null,
        count: Int = recallCount
    ): List<MemoryBankEntity> = withContext(Dispatchers.IO) {
        // 本地 embedding 已清空，返回空列表，调用方应改用 ExternalMemoryService.vectorRecallSummaries
        emptyList()
    }

    // 收口时删掉的两个方法：saveAutoSummary / saveChatMessage。
    // 它们都是"存一段文本"的单轨写入，调用点已全部改走 writeMemory；
    // 留着它们等于给"缺轨记忆"留了另外两条来源，见 saveManualMemory 的注释。

    /**
     * 清空本地 embedding 数据并执行 VACUUM 回收磁盘空间
     * embedding 已迁移到 Supabase 远端存储，本地不再需要
     * @return 清空的 embedding 条数
     */
    suspend fun clearLocalEmbeddingsAndVacuum(): Int = withContext(Dispatchers.IO) {
        // 统计所有有 embedding 的记录（不管 vector_status 是什么）
        val beforeCount = memoryBankDAO.getCountByVectorStatus("done") +
            memoryBankDAO.getCountByVectorStatus("pending") +
            memoryBankDAO.getCountByVectorStatus("failed") +
            memoryBankDAO.getCountByVectorStatus("skipped")
        // 清空所有 embedding 字段
        memoryBankDAO.clearAllEmbeddings()
        // 把所有 vector_status 标记为 skipped（不再尝试向量化）
        memoryBankDAO.markAllVectorStatusSkipped()
        Log.i(TAG, "Cleared $beforeCount local embeddings (all statuses)")
        beforeCount
    }

    // ==================== Helper Methods ====================

    private fun parseEmbedding(embeddingJson: String?): List<Float>? {
        // 本地 embedding 已废弃，始终返回 null
        return null
    }

    private fun cosineSimilarity(a: List<Float>, b: List<Float>): Float {
        if (a.size != b.size || a.isEmpty()) return 0f
        var dot = 0f
        var normA = 0f
        var normB = 0f
        for (i in a.indices) {
            dot += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }
        return if (normA == 0f || normB == 0f) 0f else dot / (kotlin.math.sqrt(normA) * kotlin.math.sqrt(normB))
    }

    // ==================== Elektron Memory 层 ====================

    /**
     * 按 Elektron 写入规范存一条记忆。
     *
     * 与 [saveManualMemory] 的区别：那条是插件 API 的"存一段文本"，是**唯一**允许缺轨的
     * 入口；这条要求**双轨齐全**、带情感坐标与来源，并且写入时就算好衰减分。
     *
     * 双轨是硬要求，不是风格建议。Elektron 的 DECISIONS 记着这条是被纠正出来的：
     * 原先「事实一条、感受一条分开记」，结果记出来一堆只有事实的条目 ——
     * 事实好记（有客观依据），感受麻烦（要回到现场焐着），分轨等于给了偷懒的口子。
     */
    data class MemoryWriteRequest(
        /** 原始现场：原话、语气、当时发生了什么。 */
        val content: String,
        /** 事实轨：行为、时间线、承诺。 */
        val factTrack: String,
        /** 情绪轨：感受、温度、影响。 */
        val feelTrack: String,
        val type: String = "manual",
        val assistantId: String? = null,
        val conversationId: String? = null,
        val role: String? = null,
        val domain: List<String> = emptyList(),
        val valence: Float = 0.5f,
        val arousal: Float = 0.3f,
        val importance: Int = 5,
        val sourceType: String? = null,
        val sourceId: String? = null,
        val sourceTs: Long = 0L,
        /** 这条记忆在重新理解哪一条。非空表示这是一次 overlay 追加，旧记忆不动。 */
        val overlayOf: Int? = null,
        val pinned: Boolean = false,
        val isProtected: Boolean = false,
        /**
         * 显式指定创建时间，0 表示用当前时间。
         *
         * 导入/迁移路径必须传这个字段。PITFALLS 里「搬家之后所有旧记忆的排序全乱了」
         * 的根因就是导入接口不给时间字段、内部一律写当前时间。
         */
        val createdAt: Long = 0L,
        /**
         * 显式指定最后活跃时间，0 表示跟随 [createdAt]。
         *
         * 对齐 Elektron 的 `last_active := created` —— 导入时两者必须一致，
         * 否则几百条几十天前的记忆会顶着"刚刚活跃"的时间分排到最前面。
         */
        val lastActiveAt: Long = 0L,
    )

    /**
     * 写入一条记忆。三处硬约束，任一不满足直接抛：
     *
     * - [MemoryWriteRequest.content] 不能空 —— 结论可以后面再推，现场丢了就没了
     * - 事实轨与情绪轨都不能空 —— 缺一轨的记忆读回来只剩半截
     * - [MemoryWriteRequest.overlayOf] 指向的记忆必须存在 —— overlay 是追加，不是凭空引用
     */
    suspend fun writeMemory(request: MemoryWriteRequest): MemoryBankEntity = withContext(Dispatchers.IO) {
        require(request.content.isNotBlank()) {
            "memory content must not be blank: 现场丢了就补不回来了"
        }
        require(request.factTrack.isNotBlank()) {
            "fact track is required: 缺事实轨的记忆等于一条没有依据的印象"
        }
        require(request.feelTrack.isNotBlank()) {
            "feel track is required: 缺情绪轨的记忆读回来只剩结论，不知道那是什么感觉"
        }
        request.overlayOf?.let { parentId ->
            require(memoryBankDAO.getMemoryById(parentId) != null) {
                "overlay target #$parentId does not exist"
            }
        }

        val createdAt = request.createdAt.takeIf { it > 0L } ?: System.currentTimeMillis()
        val lastActiveAt = request.lastActiveAt.takeIf { it > 0L } ?: createdAt
        // 钉选/保护把 importance 锁到 10，与 Elektron 的 create() 一致
        val importance = if (request.pinned || request.isProtected) {
            10
        } else {
            request.importance.coerceIn(1, 10)
        }

        val draft = MemoryBankEntity(
            content = request.content,
            type = request.type,
            conversationId = request.conversationId,
            assistantId = request.assistantId,
            role = request.role,
            createdAt = createdAt,
            lastActiveAt = lastActiveAt,
            valence = request.valence.coerceIn(0f, 1f),
            arousal = request.arousal.coerceIn(0f, 1f),
            importance = importance,
            domain = request.domain.filter { it.isNotBlank() }.joinToString(",").ifBlank { null },
            factTrack = request.factTrack,
            feelTrack = request.feelTrack,
            overlayOf = request.overlayOf,
            sourceType = request.sourceType,
            sourceId = request.sourceId,
            sourceTs = request.sourceTs,
            pinned = request.pinned,
            isProtected = request.isProtected,
            // 本地不做向量化（见文件顶部与 vectorRecall 的说明）
            vectorStatus = "skipped",
        )
        // 写入时就把分算好，召回排序不依赖后台任务先跑过一轮
        val scored = draft.copy(decayScore = scoreOf(draft, lastActiveAt))
        val id = memoryBankDAO.insertMemory(scored).toInt()
        scored.copy(id = id)
    }

    /**
     * 重算所有可衰减记忆的 decay_score。
     *
     * 写入时已经算过一次，这个方法给"时间流逝"补账 —— 得分随时间单调下降，
     * 不重算的话排序会停在写入那一刻。
     *
     * **它只更新排序分，不做任何归档。** Elektron 在 2026-08-25 拍板
     * 「不模仿人的遗忘，衰减只排序」，归档必须由调用方显式触发（见 [archiveMemory]）。
     */
    suspend fun refreshDecayScores(now: Long = System.currentTimeMillis()): Int =
        withContext(Dispatchers.IO) {
            val candidates = memoryBankDAO.getDecayCandidates()
            if (candidates.isEmpty()) return@withContext 0
            memoryBankDAO.updateMemories(candidates.map { it.copy(decayScore = scoreOf(it, now)) })
            candidates.size
        }

    /**
     * 按衰减得分排序召回。
     *
     * 这是本阶段"结构化 + 原文检索"这一路：关键词命中走 LIKE，排序用 decay_score。
     * 本地没有向量索引，语义召回仍由远端承担。
     */
    suspend fun recallRanked(query: String, count: Int = recallCount): List<MemoryBankEntity> =
        withContext(Dispatchers.IO) {
            if (query.isBlank()) {
                memoryBankDAO.getMemoriesRanked(count)
            } else {
                memoryBankDAO.searchMemoriesByKeywordRanked(query, count)
            }
        }

    /**
     * 记一次召回：last_active 前移、activation_count +1、decay_score 重算。
     *
     * 召回本身就是"被想起来一次"，所以它必须反过来影响下一次的排序 ——
     * 否则一条被反复用到的记忆会照着自己的写入时间一路沉下去。
     */
    suspend fun touchMemory(id: Int, now: Long = System.currentTimeMillis()) =
        withContext(Dispatchers.IO) {
            memoryBankDAO.touchActivation(id, now)
            memoryBankDAO.getMemoryById(id)?.let { refreshed ->
                memoryBankDAO.updateDecayScore(id, scoreOf(refreshed, now))
            }
        }

    /**
     * 显式归档一条记忆。
     *
     * 这是**唯一**的归档入口，必须由调用方主动触发（UI 或档案室），
     * 不会因为 decay_score 低就自动发生。归档不删除：归档后的记忆
     * 仍然可以被 [getArchivedMemories] 读出来。
     */
    suspend fun archiveMemory(id: Int) = withContext(Dispatchers.IO) {
        memoryBankDAO.archiveMemoryById(id)
    }

    /** 档案室读入口：所有已归档记忆。 */
    suspend fun getArchivedMemories(): List<MemoryBankEntity> = withContext(Dispatchers.IO) {
        memoryBankDAO.getArchivedMemories()
    }

    /** overlay 反查：哪些新理解在重新理解这一条。 */
    suspend fun getOverlaysOf(id: Int): List<MemoryBankEntity> = withContext(Dispatchers.IO) {
        memoryBankDAO.getOverlaysOf(id)
    }

    /** 把一条记忆实体折算成 [DecayInput]。引擎不碰 Room，也不碰时钟。 */
    private fun scoreOf(entity: MemoryBankEntity, now: Long): Float =
        MemoryDecayEngine.score(
            DecayInput(
                type = entity.type,
                importance = entity.importance,
                activationCount = entity.activationCount,
                lastActiveAt = entity.lastActiveAt,
                arousal = entity.arousal,
                resolved = entity.resolved,
                digested = entity.digested,
                pinned = entity.pinned,
                isProtected = entity.isProtected,
                now = now,
            )
        ).toFloat()

    // ==================== 旧路径收口后补上的能力 ====================

    /**
     * 就地改一条记忆。
     *
     * 与 [writeMemory] 的区别只在"改哪一条"：writeMemory 是新增，这里是覆盖已有行。
     * 它承接 AI 的 `memory_tool` 的 `edit` 动作与记忆管理页的编辑 —— 这两处的语义都是
     * "这条记错了，改掉"，不是"我对它有了新理解"。
     *
     * 后者（新理解）在 Elektron 里走 overlay 追加（[MemoryWriteRequest.overlayOf]），
     * 旧记忆不动。那条路**现在还没有接**，原因是它需要读取侧能解析出"同一个话题里哪条才是
     * 最新理解"；解析没做之前开放写入，同一个话题会以新旧两版同时出现在 prompt 里。
     * 所以 [MemoryWriteRequest.overlayOf] 目前是一个已建好、未接线（也未校验）的能力。
     */
    suspend fun updateMemory(
        id: Int,
        content: String,
        factTrack: String,
        feelTrack: String,
        domain: List<String> = emptyList(),
        importance: Int = 5,
    ): MemoryBankEntity? = withContext(Dispatchers.IO) {
        val old = memoryBankDAO.getMemoryById(id) ?: return@withContext null
        // 名字不能也叫 importance：Kotlin 不允许函数体里用局部 val 覆盖同名参数，
        // 而且就算能写，下一行读的到底是哪个 importance 也没人说得清。
        val normalizedImportance = importance.coerceIn(1, 10)
        val merged = old.copy(
            content = content,
            factTrack = factTrack,
            feelTrack = feelTrack,
            domain = domain.filter { it.isNotBlank() }.joinToString(",").ifBlank { null },
            importance = normalizedImportance,
        )
        // 内容变了，排序分要跟着重算 —— 否则会出现"改了内容却还按旧分排序"
        val updated = merged.copy(decayScore = scoreOf(merged, System.currentTimeMillis()))
        memoryBankDAO.updateMemory(updated)
        updated
    }

    /** 删除某个助手的全部记忆。助手被移除时调用。 */
    suspend fun deleteMemoriesOfAssistant(assistantId: String) = withContext(Dispatchers.IO) {
        memoryBankDAO.deleteMemoriesByAssistant(assistantId)
    }

    /**
     * 某个助手的记忆流，供记忆管理页订阅。
     *
     * 排序刻意与召回一致（decay_score 优先），这样管理页看到的顺序和模型实际读到的顺序
     * 是同一个，不会出现"列表里排在前面、模型却读不到"的割裂。
     */
    fun getMemoriesOfAssistantFlow(assistantId: String): Flow<List<AssistantMemory>> =
        memoryBankDAO.getMemoriesByAssistantFlow(assistantId)
            .map { rows -> rows.map { it.toAssistantMemory() } }

    /**
     * 注入 prompt 用的召回。
     *
     * 这是 [recallRanked] 的"给模型看"版本：限定助手、只取前 [count] 条、转成
     * [AssistantMemory]。顺手补一次随时间流逝的衰减重算。
     */
    suspend fun recallForPrompt(
        assistantId: String?,
        count: Int = PROMPT_RECALL_COUNT,
    ): List<AssistantMemory> = withContext(Dispatchers.IO) {
        refreshDecayScoresIfStale()
        val rows = if (assistantId == null) {
            memoryBankDAO.getMemoriesRanked(count)
        } else {
            memoryBankDAO.getMemoriesByAssistantRanked(assistantId, count)
        }
        rows.map { it.toAssistantMemory() }
    }

    /**
     * 距上次重算超过 [DECAY_REFRESH_INTERVAL_MS] 才真的重算，返回本次重算的行数。
     *
     * [refreshDecayScores] 本身没有节流，直接在召回路径上调用会变成每次对话全表重算。
     */
    suspend fun refreshDecayScoresIfStale(now: Long = System.currentTimeMillis()): Int {
        if (now - lastDecayRefreshAt < DECAY_REFRESH_INTERVAL_MS) return 0
        lastDecayRefreshAt = now
        return refreshDecayScores(now)
    }
}

/**
 * `memory_bank` 的一行 → UI 与工具层共用的 [AssistantMemory]。
 *
 * 放在 service 包而不是 model 包，是为了让 `data/model` 不依赖 `data/db` ——
 * 模型层目前是纯数据，保持这样比较省心。
 *
 * 两处刻意的降级：
 * - `feelTrack` 为 null 时映射成空串。从 `memoryentity` 迁过来的老记忆就是这种情况，
 *   空串表示"这一轨缺失"，UI 上要能看出它和老的双轨记忆不一样。
 * - `importance` 折回 0..2 三档给 UI 展示，属于有损映射；需要精确值的调用方直接读实体。
 */
fun MemoryBankEntity.toAssistantMemory(): AssistantMemory = AssistantMemory(
    id = id,
    content = content,
    factTrack = factTrack.orEmpty(),
    feelTrack = feelTrack.orEmpty(),
    category = memoryCategoryFromDomain(domain),
    priority = memoryImportanceToPriority(importance),
    autoGenerated = sourceType == SOURCE_TYPE_AUTO,
)