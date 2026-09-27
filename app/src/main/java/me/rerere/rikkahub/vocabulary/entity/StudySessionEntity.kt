package me.rerere.rikkahub.vocabulary.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 一次学习会话。一组过完写一条，用来记录历史。
 *
 * 注意这是**过完之后**的档案；学到一半的进度不在这里，而在
 * StudySessionStore（DataStore 里的 JSON，只存卡片 ID）。
 */
@Entity(
    tableName = "study_session",
    foreignKeys = [
        ForeignKey(
            entity = VocabularyEntity::class,
            parentColumns = ["id"],
            childColumns = ["vocab_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["vocab_id"]),
        Index(value = ["start_time"])
    ]
)
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "vocab_id")
    val vocabId: Long,
    @ColumnInfo(name = "start_time")
    val startTime: Long,
    @ColumnInfo(name = "end_time")
    val endTime: Long? = null,
    /** 这一组计划过多少词 */
    @ColumnInfo(name = "total_cards")
    val totalCards: Int = 0,
    /** 攒够次数出队的词数 */
    @ColumnInfo(name = "completed_count")
    val completedCount: Int = 0,
    /** 判太多次先放过的词数（下次还会出现） */
    @ColumnInfo(name = "parked_count")
    val parkedCount: Int = 0,
)
