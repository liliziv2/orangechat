package me.rerere.rikkahub.vocabulary.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import me.rerere.rikkahub.vocabulary.entity.VocabularyCardEntity

@Dao
interface VocabularyCardDao {
    @Insert
    suspend fun insert(card: VocabularyCardEntity): Long

    @Insert
    suspend fun insertAll(cards: List<VocabularyCardEntity>)

    @Update
    suspend fun update(card: VocabularyCardEntity)

    @Update
    suspend fun updateAll(cards: List<VocabularyCardEntity>)

    @Delete
    suspend fun delete(card: VocabularyCardEntity)

    // 顺序一律走 id（= 导入顺序 = 书本顺序）。
    // 不要用 created_at 排序：批量导入时所有卡写的是同一个时间戳，排序键全相等，
    // 实际顺序会变成靠 rowid 蒙对。
    @Query("SELECT * FROM vocabulary_card WHERE vocab_id = :vocabId ORDER BY id")
    fun getCardsByVocabId(vocabId: Long): Flow<List<VocabularyCardEntity>>

    @Query("SELECT * FROM vocabulary_card WHERE id = :cardId")
    suspend fun getCardById(cardId: Long): VocabularyCardEntity?

    /** 未学习：还没攒够次数出师的 */
    @Query("SELECT * FROM vocabulary_card WHERE vocab_id = :vocabId AND learned = 0 ORDER BY id LIMIT :limit")
    suspend fun getNewCards(vocabId: Long, limit: Int): List<VocabularyCardEntity>

    /** 待复习：学过的、且到日子了。毕业的词 next_review_date 是 null，天然被排除 */
    @Query(
        "SELECT * FROM vocabulary_card WHERE vocab_id = :vocabId AND learned = 1 " +
            "AND next_review_date IS NOT NULL AND next_review_date <= :currentTime " +
            "ORDER BY next_review_date, id LIMIT :limit"
    )
    suspend fun getReviewCards(
        vocabId: Long,
        currentTime: Long,
        limit: Int,
    ): List<VocabularyCardEntity>

    // ------------------------------------------------------------------ 统计

    @Query("SELECT COUNT(*) FROM vocabulary_card WHERE vocab_id = :vocabId")
    fun countAllCards(vocabId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM vocabulary_card WHERE vocab_id = :vocabId AND learned = 0")
    fun countNewCards(vocabId: Long): Flow<Int>

    /** 学习中 = 学过但还没毕业（毕业的词 next_review_date 被置空） */
    @Query("SELECT COUNT(*) FROM vocabulary_card WHERE vocab_id = :vocabId AND learned = 1 AND next_review_date IS NOT NULL")
    fun countStudiedCards(vocabId: Long): Flow<Int>

    /** 已掌握 = 间隔拉到顶再认对一次，退出复习循环 */
    @Query("SELECT COUNT(*) FROM vocabulary_card WHERE vocab_id = :vocabId AND learned = 1 AND next_review_date IS NULL")
    fun countMasteredCards(vocabId: Long): Flow<Int>

    /**
     * 所有词库的进度汇总，**一条查询搞定**（列表页每本书的进度用）。
     *
     * ⚠️ 不要拿它当 Flow 用：`due` 依赖传进来的 [now]，时间过去了 Room 不会自动重发，
     * 得外面定时重查（见 VocabularyVM 的轮询）。
     */
    @Query(
        """
        SELECT vocab_id AS vocabId,
               COUNT(*) AS total,
               COALESCE(SUM(CASE WHEN learned = 0 THEN 1 ELSE 0 END), 0) AS newCount,
               COALESCE(SUM(CASE WHEN learned = 1 AND next_review_date IS NOT NULL THEN 1 ELSE 0 END), 0) AS studying,
               COALESCE(SUM(CASE WHEN learned = 1 AND next_review_date IS NOT NULL AND next_review_date <= :now
                        THEN 1 ELSE 0 END), 0) AS due,
               COALESCE(SUM(CASE WHEN learned = 1 AND next_review_date IS NULL THEN 1 ELSE 0 END), 0) AS mastered
        FROM vocabulary_card
        GROUP BY vocab_id
        """
    )
    suspend fun statsByBook(now: Long): List<BookStatsRow>
}

/** [VocabularyCardDao.statsByBook] 的一行：一本词库的进度汇总 */
data class BookStatsRow(
    val vocabId: Long,
    val total: Int,
    val newCount: Int,
    val studying: Int,
    val due: Int,
    val mastered: Int,
)
