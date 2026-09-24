package me.rerere.rikkahub.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 驱动事件账本 —— **只追加，不修改**。
 *
 * 对应 Elektron `desire_engine.py` 的 `drive_event_ledger`。它回答的是
 * 「状态为什么变成这样」，与 [DriveStateEntity]（现在是什么）刻意分开。
 *
 * ## 被压制的事件也记
 *
 * `suppressed = 1` 的行是**有效信息**，不是噪音：它说明「这个事件进来了，但没够格动状态」，
 * 并给出原因（`low agency` / `low confidence` / `no drive delta`）。
 * 只记成功的事件会让"为什么今天状态没动"变成无法回答的问题。
 *
 * ## 索引
 *
 * 两个：`ts`（按时间倒序翻最近的事件）和 `primary_drive`（按维度回看某个维的历史）。
 * 它们对应两类真实查询，不是照着 Elektron 抄的 —— 那边索引建在同样的两列上，
 * 因为需求本来就一样。
 */
@Entity(
    tableName = "drive_event_ledger",
    indices = [
        Index(value = ["ts"], name = "index_drive_event_ledger_ts"),
        Index(value = ["primary_drive"], name = "index_drive_event_ledger_primary_drive"),
    ],
)
data class DriveEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 事件发生的时刻（毫秒）。 */
    @ColumnInfo("ts")
    val ts: Long,

    /** 事件包 schema 版本，当前是 `drive_event_v2`。留它是为了以后换 schema 时能分辨老行。 */
    @ColumnInfo("schema_version")
    val schemaVersion: String,

    /** 来源：user_message / speech_event / memory / external ... 权重表见 DriveEngine。 */
    @ColumnInfo("source")
    val source: String?,

    /** 事件标签，给人看的短标识。 */
    @ColumnInfo("event_label")
    val eventLabel: String?,

    /** 主驱动。null 表示这个事件没有语义中心。 */
    @ColumnInfo("primary_drive")
    val primaryDrive: String?,

    /** 强度 0..1。 */
    @ColumnInfo("intensity")
    val intensity: Double?,

    /** 置信度 0..1。低于 DriveEngine.DRIVE_EVENT_CONFIDENCE_FLOOR 会被压制。 */
    @ColumnInfo("confidence")
    val confidence: Double?,

    /** 主动性 0..1。低于 DriveEngine.DRIVE_EVENT_AGENCY_GATE 会被压制。 */
    @ColumnInfo("agency")
    val agency: Double?,

    /** 是否被压制。被压制的事件 `applied_json` 为空。 */
    @ColumnInfo("suppressed")
    val suppressed: Boolean = false,

    /** 压制原因，或空串。 */
    @ColumnInfo("reason")
    val reason: String?,

    /** 逐维变化明细（JSON）：`{"attachment": {"delta":0.05,"before":0.3,"after":0.35}}`。 */
    @ColumnInfo("applied_json")
    val appliedJson: String?,

    /** 原始事件包的大脑特征（JSON）。留着才能回答"当时凭什么这么算"。 */
    @ColumnInfo("brain_json")
    val brainJson: String?,

    /** 原始证据（JSON 字符串数组）：消息片段等。 */
    @ColumnInfo("evidence_json")
    val evidenceJson: String?,
)
