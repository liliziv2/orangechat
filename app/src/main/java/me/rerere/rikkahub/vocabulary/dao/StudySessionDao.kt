package me.rerere.rikkahub.vocabulary.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import me.rerere.rikkahub.vocabulary.entity.StudySessionEntity

@Dao
interface StudySessionDao {
    @Insert
    suspend fun insert(session: StudySessionEntity): Long

    /** 这本书一共学过几次（总结页显示「第 N 次」） */
    @Query("SELECT COUNT(*) FROM study_session WHERE vocab_id = :vocabId")
    suspend fun countSessions(vocabId: Long): Int
}
