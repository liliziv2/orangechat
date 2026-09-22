/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.db.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import me.rerere.rikkahub.data.db.entity.MemoryBankEntity


@Dao
interface MemoryBankDAO {
    // ===== MemoryBank CRUD =====

    @Insert
    suspend fun insertMemory(memory: MemoryBankEntity): Long

    @Update
    suspend fun updateMemory(memory: MemoryBankEntity)

    @Delete
    suspend fun deleteMemory(memory: MemoryBankEntity)

    @Query("DELETE FROM memory_bank WHERE id = :id")
    suspend fun deleteMemoryById(id: Int)

    @Query("SELECT * FROM memory_bank WHERE id = :id")
    suspend fun getMemoryById(id: Int): MemoryBankEntity?

    @Query("SELECT * FROM memory_bank ORDER BY created_at DESC")
    suspend fun getAllMemories(): List<MemoryBankEntity>

    @Query("SELECT * FROM memory_bank WHERE type = :type ORDER BY created_at DESC")
    suspend fun getMemoriesByType(type: String): List<MemoryBankEntity>

    @Query("SELECT * FROM memory_bank WHERE type = :type ORDER BY created_at DESC LIMIT :limit")
    suspend fun getMemoriesByTypeLimit(type: String, limit: Int): List<MemoryBankEntity>

    @Query("SELECT * FROM memory_bank WHERE assistant_id = :assistantId ORDER BY created_at DESC")
    suspend fun getMemoriesByAssistant(assistantId: String): List<MemoryBankEntity>

    /**
     * 某个助手的记忆流，供 UI 列表订阅。
     *
     * 排序与召回一致（decay_score 优先），这样管理页看到的顺序和模型实际读到的顺序是同一个，
     * 不会出现"列表里排在前面、模型却读不到"的割裂。
     */
    @Query("SELECT * FROM memory_bank WHERE assistant_id = :assistantId ORDER BY decay_score DESC, created_at DESC")
    fun getMemoriesByAssistantFlow(assistantId: String): Flow<List<MemoryBankEntity>>

    /** 删除某个助手的全部记忆。助手被删除时调用。 */
    @Query("DELETE FROM memory_bank WHERE assistant_id = :assistantId")
    suspend fun deleteMemoriesByAssistant(assistantId: String)

    @Query("SELECT * FROM memory_bank WHERE assistant_id = :assistantId AND type = :type ORDER BY created_at DESC LIMIT :limit")
    suspend fun getMemoriesByAssistantAndTypeLimit(assistantId: String, type: String, limit: Int): List<MemoryBankEntity>

    @Query("SELECT * FROM memory_bank WHERE assistant_id = :assistantId AND type = :type AND date_group = :dateGroup ORDER BY created_at DESC")
    suspend fun getMemoriesByAssistantTypeAndDateGroup(assistantId: String, type: String, dateGroup: String): List<MemoryBankEntity>

    @Query("SELECT * FROM memory_bank WHERE type = :type AND date_group = :dateGroup ORDER BY created_at DESC")
    suspend fun getMemoriesByTypeAndDateGroup(type: String, dateGroup: String): List<MemoryBankEntity>

    @Query("SELECT DISTINCT assistant_id FROM memory_bank WHERE assistant_id IS NOT NULL")
    suspend fun getDistinctAssistantIds(): List<String>

    @Query("SELECT * FROM memory_bank WHERE date_group = :dateGroup ORDER BY created_at DESC")
    suspend fun getMemoriesByDateGroup(dateGroup: String): List<MemoryBankEntity>

    @Query("SELECT * FROM memory_bank WHERE date_group = :dateGroup AND type = :type ORDER BY created_at DESC")
    suspend fun getMemoriesByDateGroupAndType(dateGroup: String, type: String): List<MemoryBankEntity>

    @Query("SELECT * FROM memory_bank WHERE vector_status = :status")
    suspend fun getMemoriesByVectorStatus(status: String): List<MemoryBankEntity>

    @Query("SELECT * FROM memory_bank WHERE vector_status = 'pending' AND vector_retry_count < :maxRetry ORDER BY created_at ASC LIMIT :limit")
    suspend fun getPendingVectorMemories(maxRetry: Int, limit: Int = 50): List<MemoryBankEntity>

    @Query("SELECT COUNT(*) FROM memory_bank WHERE type = 'message' AND created_at > :sinceTimestamp")
    suspend fun getMessageCountSince(sinceTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM memory_bank WHERE type = 'message'")
    suspend fun getTotalMessageCount(): Int

    @Query("SELECT COUNT(*) FROM memory_bank")
    suspend fun getTotalCount(): Int

    @Query("SELECT COUNT(*) FROM memory_bank WHERE type = :type")
    suspend fun getCountByType(type: String): Int

    @Query("SELECT COUNT(*) FROM memory_bank WHERE type = 'phase_summary' OR type = 'daily_summary'")
    suspend fun getSummaryCount(): Int

    @Query("SELECT COUNT(*) FROM memory_bank WHERE vector_status = :status")
    suspend fun getCountByVectorStatus(status: String): Int

    @Query("SELECT COUNT(*) FROM memory_bank WHERE assistant_id = :assistantId")
    suspend fun getCountByAssistant(assistantId: String): Int

    @Query("SELECT COUNT(*) FROM memory_bank WHERE assistant_id = :assistantId AND type = :type")
    suspend fun getCountByAssistantAndType(assistantId: String, type: String): Int

    @Query("SELECT * FROM memory_bank ORDER BY created_at DESC LIMIT :limit")
    suspend fun getRecentMemories(limit: Int): List<MemoryBankEntity>

    @Query("SELECT * FROM memory_bank WHERE content LIKE '%' || :keyword || '%' ORDER BY created_at DESC LIMIT :limit")
    suspend fun searchMemoriesByKeyword(keyword: String, limit: Int = 20): List<MemoryBankEntity>

    @Query("SELECT * FROM memory_bank WHERE content LIKE '%' || :keyword || '%' AND type = :type ORDER BY created_at DESC LIMIT :limit")
    suspend fun searchMemoriesByKeywordAndType(keyword: String, type: String, limit: Int = 20): List<MemoryBankEntity>

    @Query("SELECT DISTINCT date_group FROM memory_bank WHERE date_group IS NOT NULL ORDER BY date_group DESC LIMIT :limit")
    suspend fun getRecentDateGroups(limit: Int): List<String>

    @Query("UPDATE memory_bank SET vector_status = :status, vector_retry_count = :retryCount WHERE id = :id")
    suspend fun updateVectorStatus(id: Int, status: String, retryCount: Int)

    // ===== Vector recall queries =====

    /** 获取所有已向量化且 embedding 非空的记忆 */
    @Query("SELECT * FROM memory_bank WHERE vector_status = 'done' AND embedding IS NOT NULL AND embedding != '' ORDER BY created_at DESC")
    suspend fun getAllVectorizedMemories(): List<MemoryBankEntity>

    /** 获取指定助手已向量化且 embedding 非空的记忆 */
    @Query("SELECT * FROM memory_bank WHERE assistant_id = :assistantId AND vector_status = 'done' AND embedding IS NOT NULL AND embedding != '' ORDER BY created_at DESC")
    suspend fun getVectorizedMemoriesByAssistant(assistantId: String): List<MemoryBankEntity>

    /** 更新指定记录的 embedding 和 vector_status */
    @Query("UPDATE memory_bank SET embedding = NULL WHERE embedding IS NOT NULL")
    suspend fun clearAllEmbeddings()

    @Query("UPDATE memory_bank SET vector_status = 'skipped' WHERE vector_status != 'skipped'")
    suspend fun markAllVectorStatusSkipped()

    @Query("UPDATE memory_bank SET embedding = :embedding, vector_status = :status WHERE id = :id")
    suspend fun updateEmbedding(id: Int, embedding: String, status: String)

    // ===== Elektron Memory 层查询（v31 起） =====

    @Update
    suspend fun updateMemories(memories: List<MemoryBankEntity>)

    @Query("UPDATE memory_bank SET decay_score = :score WHERE id = :id")
    suspend fun updateDecayScore(id: Int, score: Float)

    /** 记一次召回：last_active 前移、activation_count +1。 */
    @Query("UPDATE memory_bank SET last_active_at = :ts, activation_count = activation_count + 1 WHERE id = :id")
    suspend fun touchActivation(id: Int, ts: Long)

    /** 显式归档。没有自动归档路径，见 MemoryDecayEngine 的类注释。 */
    @Query("UPDATE memory_bank SET archived = 1 WHERE id = :id")
    suspend fun archiveMemoryById(id: Int)

    @Query("SELECT * FROM memory_bank WHERE archived = 1 ORDER BY created_at DESC")
    suspend fun getArchivedMemories(): List<MemoryBankEntity>

    /** overlay 反查：哪些新理解是在重新理解这一条。 */
    @Query("SELECT * FROM memory_bank WHERE overlay_of = :id ORDER BY created_at ASC")
    suspend fun getOverlaysOf(id: Int): List<MemoryBankEntity>

    /** 需要重算衰减分的行：跳过固化、钉选、保护与已归档。 */
    @Query("SELECT * FROM memory_bank WHERE archived = 0 AND type != 'permanent' AND pinned = 0 AND `protected` = 0")
    suspend fun getDecayCandidates(): List<MemoryBankEntity>

    /** 关键词召回，按衰减得分排序。这是本阶段"结构化 + 原文检索"的检索面。 */
    @Query("SELECT * FROM memory_bank WHERE archived = 0 AND content LIKE '%' || :keyword || '%' ORDER BY decay_score DESC, created_at DESC LIMIT :limit")
    suspend fun searchMemoriesByKeywordRanked(keyword: String, limit: Int = 20): List<MemoryBankEntity>

    @Query("SELECT * FROM memory_bank WHERE archived = 0 ORDER BY decay_score DESC, created_at DESC LIMIT :limit")
    suspend fun getMemoriesRanked(limit: Int): List<MemoryBankEntity>

    /** 指定助手的召回排序，供注入 prompt 使用。 */
    @Query("SELECT * FROM memory_bank WHERE archived = 0 AND assistant_id = :assistantId ORDER BY decay_score DESC, created_at DESC LIMIT :limit")
    suspend fun getMemoriesByAssistantRanked(assistantId: String, limit: Int): List<MemoryBankEntity>
}
