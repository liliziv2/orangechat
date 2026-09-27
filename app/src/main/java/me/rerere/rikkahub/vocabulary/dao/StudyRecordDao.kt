package me.rerere.rikkahub.vocabulary.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import me.rerere.rikkahub.vocabulary.entity.StudyRecordEntity

@Dao
interface StudyRecordDao {
    @Insert
    suspend fun insert(record: StudyRecordEntity): Long

    /**
     * 所有词库加起来今天过了多少个词。
     *
     * `card_id` 全局唯一，直接 COUNT DISTINCT 就行 —— 一张卡反复判也只算一个词。
     * 时间范围由调用方按**系统时区的当天 0 点**算好传进来（见 VocabularyRepository.startOfDay）。
     */
    @Query(
        "SELECT COUNT(DISTINCT card_id) FROM study_record " +
            "WHERE timestamp >= :startTime AND timestamp < :endTime"
    )
    suspend fun countWordsInRangeAll(startTime: Long, endTime: Long): Int
}
