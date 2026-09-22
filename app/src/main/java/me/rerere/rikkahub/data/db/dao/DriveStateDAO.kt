/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import me.rerere.rikkahub.data.db.entity.DriveEventEntity
import me.rerere.rikkahub.data.db.entity.DriveSampleEntity
import me.rerere.rikkahub.data.db.entity.DriveStateEntity

/**
 * State 层的全部数据访问。
 *
 * 三张表放一个 DAO 而不是拆三个：它们的读写点完全重合（每次 [applyEvent] 都要
 * 读状态、写状态、追加账本、追加采样），拆开只会让调用方每次都同时持有三个接口。
 *
 * 注意这里**没有** updateState 之外的写路径：状态只有 [upsertState] 一个出口，
 * 账本只有 [insertEvent]（追加），采样只有 [insertSample] 与 [pruneSamples]。
 */
@Dao
interface DriveStateDAO {

    // ===== 当下状态（单行） =====

    /** 读单行状态。首次安装时返回 null，由 Service 侧建初始行。 */
    @Query("SELECT * FROM drive_state WHERE id = 1")
    suspend fun getState(): DriveStateEntity?

    /** 写回状态。单行表用 REPLACE，语义就是 upsert。 */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertState(state: DriveStateEntity)

    // ===== 事件账本（只追加） =====

    /** 追加一条账本记录，返回自增 id。**压制的事件也要调它。** */
    @Insert
    suspend fun insertEvent(event: DriveEventEntity): Long

    /** 最近的事件，新的在前。 */
    @Query("SELECT * FROM drive_event_ledger ORDER BY ts DESC, id DESC LIMIT :limit")
    suspend fun recentEvents(limit: Int): List<DriveEventEntity>

    /** 某个维度最近的事件。 */
    @Query("SELECT * FROM drive_event_ledger WHERE primary_drive = :drive ORDER BY ts DESC, id DESC LIMIT :limit")
    suspend fun recentEventsForDrive(drive: String, limit: Int): List<DriveEventEntity>

    /** 账本总条数。 */
    @Query("SELECT COUNT(*) FROM drive_event_ledger")
    suspend fun eventCount(): Int

    /**
     * 增量拉账本：`id > afterId`，老的在前。
     *
     * 关闭循环靠这个游标推进，而不是"每次读最近 N 条"—— 后者在账本增长后
     * 会漏掉中间的行，而漏掉的行为就再也不会被回写成记忆了。
     */
    @Query("SELECT * FROM drive_event_ledger WHERE id > :afterId ORDER BY id ASC LIMIT :limit")
    suspend fun ledgerAfter(afterId: Long, limit: Int): List<DriveEventEntity>

    /** 账本里最大的 id。首次运行建立检查点用它。空表返回 0。 */
    @Query("SELECT COALESCE(MAX(id), 0) FROM drive_event_ledger")
    suspend fun ledgerMaxId(): Long

    // ===== 历史采样（基线参照系） =====

    /** 追加一条采样，返回自增 id。 */
    @Insert
    suspend fun insertSample(sample: DriveSampleEntity): Long

    /** 取窗口内的采样，老的在前（[me.rerere.rikkahub.data.service.DriveBaseline.pickBaseline] 需要有序）。 */
    @Query("SELECT * FROM drive_sample WHERE ts >= :since ORDER BY ts ASC")
    suspend fun samplesSince(since: Long): List<DriveSampleEntity>

    /** 丢弃窗口外的采样。 */
    @Query("DELETE FROM drive_sample WHERE ts < :before")
    suspend fun pruneSamples(before: Long)

    /** 采样总条数。 */
    @Query("SELECT COUNT(*) FROM drive_sample")
    suspend fun sampleCount(): Int
}
