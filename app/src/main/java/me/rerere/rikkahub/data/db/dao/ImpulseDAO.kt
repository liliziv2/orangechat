package me.rerere.rikkahub.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import me.rerere.rikkahub.data.db.entity.ImpulseEntity

/**
 * 冲动队列的数据访问。
 *
 * 所有 `@Query` 都写成单行字面量，**不做字符串拼接** —— 拼接过的 SQL 在
 * 编译期校验不到，写错了要等运行时才炸。
 *
 * 状态迁移全部走 [update] 的读-改-写：队列的并发点在 `ImpulseQueueService` 里
 * 用一把 Mutex 收口，DAO 这边不重复做原子性保证。
 */
@Dao
interface ImpulseDAO {

    /** 入队，返回自增 id。 */
    @Insert
    suspend fun insert(impulse: ImpulseEntity): Long

    /** 整行写回。状态迁移的唯一出口。 */
    @Update
    suspend fun update(impulse: ImpulseEntity)

    /** 按 id 读一行。 */
    @Query("SELECT * FROM impulse_queue WHERE id = :id")
    suspend fun getById(id: Long): ImpulseEntity?

    /**
     * 同一 kind + dedupe_key 下还没结束的那一条。
     *
     * 活跃状态包含 `deferred` —— 被延后的唤醒仍然是"这件事还没处理完"，
     * 放它进来的话下一次越线会立刻再排一条，去重就白做了。
     */
    @Query(
        "SELECT * FROM impulse_queue WHERE kind = :kind AND dedupe_key = :dedupeKey " +
            "AND status IN ('pending', 'claimed', 'deferred') ORDER BY id DESC LIMIT 1"
    )
    suspend fun findActive(kind: String, dedupeKey: String): ImpulseEntity?

    /**
     * 某个 kind 下最早的一条活跃行。
     *
     * 活跃包含 `deferred` —— 被延后的唤醒仍然是"这件事还没处理完"，
     * 放它进来的话下一次越线会立刻再排一条，去重就白做了。
     */
    @Query(
        "SELECT * FROM impulse_queue WHERE kind = :kind " +
            "AND status IN ('pending', 'claimed', 'deferred') ORDER BY id ASC LIMIT 1"
    )
    suspend fun firstActive(kind: String): ImpulseEntity?

    /** 到点且可以认领的行，最早到点的在前。 */
    @Query(
        "SELECT * FROM impulse_queue WHERE status = 'pending' AND due_at <= :now " +
            "ORDER BY due_at ASC, id ASC LIMIT :limit"
    )
    suspend fun duePending(now: Long, limit: Int): List<ImpulseEntity>

    /** 租约已经过期的认领行 —— 崩溃恢复的对象。 */
    @Query(
        "SELECT * FROM impulse_queue WHERE status = 'claimed' " +
            "AND lease_expires_at > 0 AND lease_expires_at <= :now"
    )
    suspend fun expiredLeases(now: Long): List<ImpulseEntity>

    /** 排期已到的延后行。 */
    @Query(
        "SELECT * FROM impulse_queue WHERE status = 'deferred' " +
            "AND retry_at > 0 AND retry_at <= :now"
    )
    suspend fun dueDeferred(now: Long): List<ImpulseEntity>

    /** 重试预算已经耗尽的 pending 行，直接判失败。 */
    @Query("SELECT * FROM impulse_queue WHERE status = 'pending' AND attempts >= max_attempts")
    suspend fun exhaustedRetries(): List<ImpulseEntity>

    /** 最近一次已结束的结果（用于最小间隔判断）。 */
    @Query(
        "SELECT * FROM impulse_queue WHERE kind = :kind " +
            "AND status IN ('done', 'deferred', 'failed', 'ignored') ORDER BY id DESC LIMIT 1"
    )
    suspend fun lastOutcome(kind: String): ImpulseEntity?

    /** 最早一条还没到点的 pending 的 due_at。用于让现有闹钟提前醒。 */
    @Query("SELECT MIN(due_at) FROM impulse_queue WHERE status = 'pending' AND due_at > :now")
    suspend fun nextPendingDueAt(now: Long): Long?

    /** 按状态数数。 */
    @Query("SELECT COUNT(*) FROM impulse_queue WHERE status = :status")
    suspend fun countByStatus(status: String): Int

    /**
     * 某个 kind 下还有几条活跃行。
     *
     * 给"已经 active 的 emotion_wake 不会重复创建"这条用。走
     * `index_impulse_queue_kind_dedupe_key` 的前缀列，不用全表扫。
     */
    @Query(
        "SELECT COUNT(*) FROM impulse_queue WHERE kind = :kind " +
            "AND status IN ('pending', 'claimed', 'deferred')"
    )
    suspend fun countActive(kind: String): Int

    /** 最近若干行，新的在前。 */
    @Query("SELECT * FROM impulse_queue ORDER BY id DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<ImpulseEntity>

    /**
     * 已经做完但还没回写记忆的行 —— 关闭循环要捞的就是这些。
     *
     * 老的在前：先做的先回写，记忆的顺序跟行为发生的顺序一致。
     */
    @Query(
        "SELECT * FROM impulse_queue WHERE status = 'done' AND result_memory_id IS NULL " +
            "ORDER BY id ASC LIMIT :limit"
    )
    suspend fun doneWithoutMemory(limit: Int): List<ImpulseEntity>

    /** 清理过期的终态行。返回删掉几行。 */
    @Query(
        "DELETE FROM impulse_queue WHERE status IN ('done', 'ignored', 'failed') " +
            "AND finished_at > 0 AND finished_at < :before"
    )
    suspend fun pruneTerminal(before: Long): Int
}
