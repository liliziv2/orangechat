package me.rerere.rikkahub.vocabulary.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 单词卡片。一个词一行，归属某本词库。
 *
 * 状态由两列决定：
 * - [learned] —— 是不是「学过」（一组里攒够次数了）。只碰过一次不算
 * - [nextReviewDate] —— 下次该复习的时间。`learned=true` 且它是 null 表示已毕业
 *
 * ⚠️ `learned` / `mastery` / `nextReviewDate` **只在攒够次数出队时才写**。
 * 组内每判一次都写的话，用户学到一半退出，这个词的 `learned` 已经成 true，
 * 下次就再也拿不到第二、第三次了（见 VocabularyRepository.answer）。
 */
@Entity(
    tableName = "vocabulary_card",
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
        Index(value = ["next_review_date"])
    ]
)
data class VocabularyCardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "vocab_id")
    val vocabId: Long,
    @ColumnInfo(name = "word")
    val word: String,
    @ColumnInfo(name = "translation")
    val translation: String,
    @ColumnInfo(name = "ipa")
    val ipa: String = "",
    @ColumnInfo(name = "example")
    val example: String = "",
    /** 词性：名词/动词/形容词/副词/介词… */
    @ColumnInfo(name = "pos")
    val pos: String = "",
    /**
     * 单元/课次，原样从 CSV 存下来。
     * 本阶段 UI 不暴露「按课学习」，留着是为了导入不丢信息。
     */
    @ColumnInfo(name = "unit")
    val unit: String? = null,
    /** 掌握度档位，0-MAX_MASTERY。下标对应复习间隔表 */
    @ColumnInfo(name = "mastery")
    val mastery: Int = 0,
    /** 攒够次数、真正学过了；只碰过一次不算 */
    @ColumnInfo(name = "learned")
    val learned: Boolean = false,
    /** null 且 learned=false 表示从没学过；learned=true 时表示已毕业 */
    @ColumnInfo(name = "next_review_date")
    val nextReviewDate: Long? = null,
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),
)
