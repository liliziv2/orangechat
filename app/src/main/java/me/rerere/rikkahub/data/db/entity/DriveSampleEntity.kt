package me.rerere.rikkahub.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 驱动历史采样 —— 基线的参照系。
 *
 * Elektron 把这份历史放在 `emotion-history.json` 里（一个滚动 30 小时的 JSON 数组）。
 * Android 侧放进 Room：不是为了"更规范"，而是因为**这份数据必须活过进程重启**——
 * 放在内存里的话，每次冷启动都会退回「历史不够、不做相对判断」，
 * 于是相对上涨这条判断线永远不会生效。
 *
 * 保留窗口是 30 小时（[me.rerere.rikkahub.data.service.DriveBaseline.HISTORY_RETENTION_HOURS]），
 * 比 24 小时长一截 —— 多出来的 6 小时是给「昨天这个点」找参照时的容错余量：
 * 如果只留 24 小时，用户今天比昨天晚两小时打开 App，「同时间点」就落空了。
 *
 * 写入时机与状态推进一致（每次 [me.rerere.rikkahub.data.service.DriveStateService.applyEvent]），
 * 超期行在同一事务里删掉。
 */
@Entity(
    tableName = "drive_sample",
    indices = [Index(value = ["ts"], name = "index_drive_sample_ts")],
)
data class DriveSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** 采样时刻（毫秒）。 */
    @ColumnInfo("ts")
    val ts: Long,

    /** 当时的 9 维值（raw），JSON 对象。 */
    @ColumnInfo("values_json")
    val valuesJson: String,
)
