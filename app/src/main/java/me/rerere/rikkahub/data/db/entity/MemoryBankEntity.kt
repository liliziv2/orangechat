package me.rerere.rikkahub.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 记忆库条目实体
 * 存储每条记忆的元数据和内容
 */
@Entity(tableName = "memory_bank")
data class MemoryBankEntity(
    @PrimaryKey(true)
    val id: Int = 0,
    
    /** 记忆内容 */
    @ColumnInfo("content")
    val content: String = "",
    
    /** 记忆类型: message, phase_summary, daily_summary, manual */
    @ColumnInfo("type")
    val type: String = "message",
    
    /** 关联的对话ID（可选） */
    @ColumnInfo("conversation_id")
    val conversationId: String? = null,
    
    /** 关联的助手ID */
    @ColumnInfo("assistant_id")
    val assistantId: String? = null,
    
    /** 消息角色: user, assistant */
    @ColumnInfo("role")
    val role: String? = null,
    
    /** 创建时间戳 */
    @ColumnInfo("created_at")
    val createdAt: Long = System.currentTimeMillis(),
    
    /** 所属日期（用于每日总结分组），格式 yyyy-MM-dd */
    @ColumnInfo("date_group")
    val dateGroup: String? = null,
    
    /** 向量化状态: pending, done, failed */
    @ColumnInfo("vector_status")
    val vectorStatus: String = "pending",
    
    /** 向量化重试次数 */
    @ColumnInfo("vector_retry_count")
    val vectorRetryCount: Int = 0,

    /** Embedding 向量（JSON 格式的浮点数组） */
    @ColumnInfo("embedding")
    val embedding: String? = null,

    // ==================== Elektron Memory 层字段（v31 起） ====================
    // 下面这组列把 Elektron 的 bucket 结构搬到了关系表上。每一列对应的机制
    // 见 MemoryDecayEngine / MemoryBankService / Migration_30_31，别只看字段名猜语义。

    /**
     * 最后活跃时间（毫秒）。
     *
     * **只有它参与召回排序**，不要和 [createdAt] 混用 —— PITFALLS 里
     * 「搬家之后所有旧记忆的排序全乱了」就是把两者当成一回事造成的。
     */
    @ColumnInfo("last_active_at", defaultValue = "0")
    val lastActiveAt: Long = 0L,

    /** 被召回/引用的次数，衰减公式里按 `activation_count^0.3` 计。 */
    @ColumnInfo("activation_count", defaultValue = "0")
    val activationCount: Int = 0,

    /** 情感价（Russell 环形模型）：0=负面，1=正面。 */
    @ColumnInfo("valence", defaultValue = "0.5")
    val valence: Float = 0.5f,

    /** 唤醒度：0=平静，1=激动。越高衰减越慢；>0.7 且未结案时排序额外顶一档。 */
    @ColumnInfo("arousal", defaultValue = "0.3")
    val arousal: Float = 0.3f,

    /** 重要度 1..10。与 MemoryEntity.priority 的 0..2 是两套语义，不互相转换。 */
    @ColumnInfo("importance", defaultValue = "5")
    val importance: Int = 5,

    /** 主题域，逗号分隔（Elektron 侧是数组，关系表里拍平）。null 视为空。 */
    @ColumnInfo("domain")
    val domain: String? = null,

    /**
     * 事实轨：行为、时间线、承诺。
     *
     * 与 [feelTrack] 是**同一条记忆的两面**，不是两条记录 —— 分开记会记出一堆
     * 只有事实的条目，因为事实好记、感受麻烦。写入侧的强制校验见
     * MemoryBankService.writeMemory，缺一轨会被拒。
     */
    @ColumnInfo("fact_track")
    val factTrack: String? = null,

    /** 情绪轨：感受、温度、影响。null 表示这一轨缺失。 */
    @ColumnInfo("feel_track")
    val feelTrack: String? = null,

    /**
     * 这条记忆在重新理解哪一条。null 表示它本身是独立记忆。
     *
     * 旧记忆永远不改写，新理解只追加 —— 「当时理解错了」本身也是记录的一部分。
     * 反方向的「这条被谁取代了」由 `WHERE overlay_of = :id` 查出来，不额外存列。
     */
    @ColumnInfo("overlay_of")
    val overlayOf: Int? = null,

    /** 来源类型：conversation / message / behavior / emotion / manual ... */
    @ColumnInfo("source_type")
    val sourceType: String? = null,

    /** 来源 id（对话 id / 消息 id / impulse id / ledger id）。 */
    @ColumnInfo("source_id")
    val sourceId: String? = null,

    /** 来源发生时间（毫秒）。 */
    @ColumnInfo("source_ts", defaultValue = "0")
    val sourceTs: Long = 0L,

    /** 已处理。与 [digested] 同时为真时衰减加速到 ×0.02。 */
    @ColumnInfo("resolved", defaultValue = "0")
    val resolved: Boolean = false,

    /** 已为它写过 feel。 */
    @ColumnInfo("digested", defaultValue = "0")
    val digested: Boolean = false,

    /** 钉选：不参与衰减，importance 锁 10。 */
    @ColumnInfo("pinned", defaultValue = "0")
    val pinned: Boolean = false,

    /**
     * 保护：与 [pinned] 行为一致。
     *
     * Elektron 分成两个字段，是因为那边的 pinned 还带一个"搬进 permanent 目录"的副作用；
     * 这边没有目录，两者语义重合，但保留两列是为了让"AI 钉的"和"用户保护的"仍然可区分。
     */
    @ColumnInfo("protected", defaultValue = "0")
    val isProtected: Boolean = false,

    /**
     * 缓存下来的衰减得分，**只用于排序**。
     *
     * 写入时算一次，MemoryBankService.refreshDecayScores() 负责随时间的批量重算。
     * 引擎明确不拿它做归档判断 —— 见 MemoryDecayEngine 的类注释。
     */
    @ColumnInfo("decay_score", defaultValue = "0")
    val decayScore: Float = 0f,

    /**
     * 已归档。
     *
     * 只由显式的归档动作置位，衰减永远不会自动归档（Elektron 2026-08-25 拍板）。
     * 归档不等于删除，归档后的记忆仍可被读出来。
     */
    @ColumnInfo("archived", defaultValue = "0")
    val archived: Boolean = false,
)



