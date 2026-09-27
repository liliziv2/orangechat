package me.rerere.rikkahub.vocabulary.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 词库。一本书一行。
 *
 * 卡片、进度、答题记录都按 `vocab_id` 分开，所以多本词库并存不用额外加锁。
 *
 * 这一版只做「用户自己导 CSV」，没有随 App 发内置词库 —— 所以参考实现里的
 * `asset_path` / `content_version` / `on_homepage` 三个列都没建（那套是内置词库的
 * 导入判重与主页展示用的）。要加内置词库时再补列 + 迁移。
 */
@Entity(tableName = "vocabulary")
data class VocabularyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "name")
    val name: String,
    @ColumnInfo(name = "description")
    val description: String = "",
    /**
     * 这份 CSV 带不带 unit 列。留着是为了**导入不丢信息**：
     * 本阶段 UI 不暴露「按课学习」，但卡片表里的 unit 照原样存着。
     */
    @ColumnInfo(name = "has_unit")
    val hasUnit: Boolean = false,
    /**
     * 软删除时间。非 null = 躺在「已删除词库」里，单词和进度都还在，随时能恢复。
     */
    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
)
