package me.rerere.rikkahub.data.security

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SecurityAuditDao {
    @Insert
    suspend fun insert(entity: SecurityAuditEntity)

    @Query("SELECT * FROM security_audit_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecent(limit: Int): Flow<List<SecurityAuditEntity>>

    @Query("SELECT * FROM security_audit_logs ORDER BY timestamp DESC")
    suspend fun getAll(): List<SecurityAuditEntity>

    @Query("DELETE FROM security_audit_logs WHERE timestamp < :olderThanMs")
    suspend fun deleteOlderThan(olderThanMs: Long)

    @Query("DELETE FROM security_audit_logs")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM security_audit_logs")
    suspend fun count(): Int
}
