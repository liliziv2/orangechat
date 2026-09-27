package me.rerere.rikkahub.vocabulary.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 答题记录。每判一次卡写一条。
 *
 * 一张卡可能有多条记录（组内反复判、之后复习再判），所以统计时要按 `card_id` 去重。
 *
 * ⚠️ 组内还没攒够次数时**只写这张表，不动卡片状态** —— 这样中途退出的话
 * 这个词仍然算「未学习」，下次还能拿到。
 */
@Entity(
    tableName = "study_record",
    foreignKeys = [
        ForeignKey(
            entity = VocabularyEntity::class,
            parentColumns = ["id"],
            childColumns = ["vocab_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = VocabularyCardEntity::class,
            parentColumns = ["id"],
            childColumns = ["card_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["vocab_id"]),
        Index(value = ["card_id"]),
        Index(value = ["timestamp"])
    ]
)
data class StudyRecordEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "vocab_id")
    val vocabId: Long,
    @ColumnInfo(name = "card_id")
    val cardId: Long,
    /** true = 认识，false = 模糊或忘记 */
    @ColumnInfo(name = "is_correct")
    val isCorrect: Boolean,
    /** 0 = 忘记，1 = 模糊，2 = 认识 */
    @ColumnInfo(name = "answer_type")
    val answerType: Int,
    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),
)
