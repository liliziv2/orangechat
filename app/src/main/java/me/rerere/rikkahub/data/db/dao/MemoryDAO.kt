/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import me.rerere.rikkahub.data.db.entity.MemoryEntity

@Dao
interface MemoryDAO {
    @Query("SELECT * FROM memoryentity WHERE assistant_id = :assistantId")
    fun getMemoriesOfAssistantFlow(assistantId: String): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memoryentity WHERE assistant_id = :assistantId")
    suspend fun getMemoriesOfAssistant(assistantId: String): List<MemoryEntity>

    @Query("SELECT * FROM memoryentity")
    fun getAllMemoriesFlow(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM memoryentity")
    suspend fun getAllMemories(): List<MemoryEntity>

    @Query("SELECT * FROM memoryentity WHERE id = :id")
    suspend fun getMemoryById(id: Int): MemoryEntity?

    @Insert
    suspend fun insertMemory(memory: MemoryEntity): Long

    @Update
    suspend fun updateMemory(memory: MemoryEntity)

    @Query("DELETE FROM memoryentity WHERE id = :id")
    suspend fun deleteMemory(id: Int)

    @Query("DELETE FROM memoryentity WHERE assistant_id = :assistantId")
    suspend fun deleteMemoriesOfAssistant(assistantId: String)

    /**
     * 按分类查询某助手的记忆。
     */
    @Query("SELECT * FROM memoryentity WHERE assistant_id = :assistantId AND category = :category")
    fun getMemoriesOfCategoryFlow(assistantId: String, category: String): Flow<List<MemoryEntity>>

    /**
     * 按优先级降序、创建时间降序查询，用于注入 prompt 时优先给出关键记忆。
     */
    @Query(
        "SELECT * FROM memoryentity WHERE assistant_id = :assistantId " +
            "ORDER BY priority DESC, create_at DESC, id DESC"
    )
    suspend fun getMemoriesOfAssistantByPriority(assistantId: String): List<MemoryEntity>

    @Query("UPDATE memoryentity SET category = :category WHERE id = :id")
    suspend fun updateCategory(id: Int, category: String)

    @Query("UPDATE memoryentity SET priority = :priority WHERE id = :id")
    suspend fun updatePriority(id: Int, priority: Int)
}
