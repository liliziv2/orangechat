package me.rerere.rikkahub.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 驱动状态实体 —— **单行表**，主键恒为 [SINGLETON_ID]。
 *
 * 这是 Elektron State 层「当下状态」的落点，对应 `desire_engine.py` 的 `drive_state` 表。
 * 它和 [DriveEventEntity]（账本）是两件事：
 * 这里是「现在什么感觉」，账本是「为什么变成这样」。合在一张表里会让状态随账本行数增长，
 * 也会让"回看当时的状态"变得不可能。
 *
 * 为什么是单行而不是按助手一行：驱动状态描述的是**这个 App 里这个 agent 的当下**，
 * 不分助手。Elektron 那边也是单行表。要做多助手隔离时再加 `assistant_id` 列并改主键，
 * 现在不预留 —— 预留了反而会让"到底按什么隔离"这个问题一直悬着。
 *
 * ## 几个 JSON 列为什么不是拆成 9×N 列
 *
 * `drives_json` / `local_fatigue_json` 是定长 9 键的 map。拆成 18 列会让每次加一个维度
 * 都要一次迁移，而维度的定义（[me.rerere.rikkahub.data.service.DriveEngine.DRIVE_KEYS]）
 * 在代码里本来就是一处。JSON 列的代价是不能用 SQL 直接按某一维排序 —— 现在也不需要。
 */
@Entity(tableName = "drive_state")
data class DriveStateEntity(
    /** 单行表的主键，恒为 [SINGLETON_ID]。 */
    @PrimaryKey
    val id: Int = SINGLETON_ID,

    /** 9 维当前值（raw，0..1），JSON 对象。 */
    @ColumnInfo("drives_json")
    val drivesJson: String,

    /** 已推进的拍数。惰性推进时累加，用来判断"这个状态被维护过几次"。 */
    @ColumnInfo("tick_count")
    val tickCount: Int = 0,

    /** 上次推进的时刻（毫秒）。**惰性 tick 的唯一依据**，也是"更新时间"。 */
    @ColumnInfo("last_ts")
    val lastTs: Long,

    /** 上一拍的 9 维值。给耦合的 delta 模式用，不参与展示。 */
    @ColumnInfo("prev_drives_json")
    val prevDrivesJson: String,

    /** 每维独立疲劳（由全局 fatigue × 敏感度算出），JSON 对象。 */
    @ColumnInfo("local_fatigue_json")
    val localFatigueJson: String,

    /**
     * 当前状态快照（JSON）。
     *
     * 存的是**算出来的派生量**：9 维有效激活、9 维有效分、PA/NA、当前主导维。
     * 它是缓存不是第二真相源 —— 真相永远是 `drives_json` + `local_fatigue_json`，
     * 这一列只是让"重启后能直接看到当时的状态长什么样"而不必重新推一遍。
     */
    @ColumnInfo("snapshot_json")
    val snapshotJson: String,

    /** 逃逸阀的连续失衡计数。见 DriveEngine.applyEscapeValve。 */
    @ColumnInfo("escape_streak")
    val escapeStreak: Int = 0,

    /** 最近一次真实用户消息的时刻（毫秒）。缺席漂移按它算。 */
    @ColumnInfo("last_user_message_at")
    val lastUserMessageAt: Long = 0L,

    /** 团圆上扬量。展示层的一次性事件，不写回基线。 */
    @ColumnInfo("reunion_pa_boost")
    val reunionPaBoost: Double = 0.0,

    /**
     * 领地通道状态（JSON）。
     *
     * **列已开、做 JSON 往返，但本轮 tick 不驱动它** —— Elektron 用这个通道做
     * 领地基线的慢漂移与事件尖峰分离。那套机制要一整套配套常量，留给后续阶段。
     * 现在它停在默认值上，读出来和写回去是同一个对象。
     */
    @ColumnInfo("possessiveness_channels_json")
    val possessivenessChannelsJson: String,

    /** 缺席回弹状态（JSON）。同上：列已开、本轮不驱动。 */
    @ColumnInfo("attachment_rebound_json")
    val attachmentReboundJson: String,

    /** 亲密中断蓄积状态（JSON）。同上：列已开、本轮不驱动。 */
    @ColumnInfo("libido_pending_json")
    val libidoPendingJson: String,
) {
    companion object {
        /** 单行表固定的主键值。 */
        const val SINGLETON_ID = 1
    }
}
