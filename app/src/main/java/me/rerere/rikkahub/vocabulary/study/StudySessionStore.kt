package me.rerere.rikkahub.vocabulary.study

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.vocabularySessionStore by preferencesDataStore(name = "vocabulary_session")

/**
 * 学到一半的会话，落盘用的快照。
 *
 * 设计要点：**只存卡片 ID，不存卡片内容**。恢复时按 ID 重新查库 ——
 * 这样期间如果词库内容刷新过（改了释义、加了词性），恢复出来的是新内容；
 * 存整个卡片对象的话会让用户看到一份过期的数据。
 *
 * 队列用**平行数组**（ids / passes / attempts）而不是嵌套对象，
 * 因为 JSON 里这样最省事，而且下标天然对齐。
 */
@Serializable
data class PersistedSession(
    val vocabId: Long,
    val mode: String,
    val total: Int,

    val queueCardIds: List<Long>,
    val queuePasses: List<Int>,
    val queueAttempts: List<Int>,

    val doneCardIds: List<Long>,
    val doneAttempts: List<Int>,

    val parkedCardIds: List<Long>,
    val parkedAttempts: List<Int>,
    val parkedPasses: List<Int>,

    val mastered: Int = 0,
    val startedAt: Long,
    val savedAt: Long = System.currentTimeMillis(),
)

/**
 * 断点续学的持久化。**每判一次卡存一次** —— 用户可能随时退出、App 可能被系统杀掉。
 *
 * 单独一个 DataStore 文件（`vocabulary_session`），不并进 App 的设置存储：
 * 这一块要能整个拆掉。
 */
class StudySessionStore(private val context: Context) {

    private val json = Json {
        // 以后加字段时，老数据也能读进来
        ignoreUnknownKeys = true
    }

    private val key = stringPreferencesKey("persisted_session")

    suspend fun save(session: PersistedSession) {
        val text = json.encodeToString(PersistedSession.serializer(), session)
        context.vocabularySessionStore.edit { prefs ->
            prefs[key] = text
        }
    }

    suspend fun load(): PersistedSession? {
        val text = context.vocabularySessionStore.data.map { prefs -> prefs[key] }.first()
            ?: return null
        // 读不动就当作没有会话，不要让一条坏数据把学习页卡死
        return runCatching {
            json.decodeFromString(PersistedSession.serializer(), text)
        }.getOrNull()
    }

    suspend fun clear() {
        context.vocabularySessionStore.edit { prefs -> prefs.remove(key) }
    }
}
