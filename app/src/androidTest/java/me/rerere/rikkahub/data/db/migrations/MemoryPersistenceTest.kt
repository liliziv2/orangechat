package me.rerere.rikkahub.data.db.migrations

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import me.rerere.rikkahub.data.db.AppDatabase
import me.rerere.rikkahub.data.service.MemoryBankService
import okhttp3.OkHttpClient
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MemoryPersistenceTest {
    @Test fun recallOwnershipBudgetAndReplaySurviveReopen() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "memory-persistence-test"
        context.deleteDatabase(name)
        fun open() = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        var db = open()
        fun service() = MemoryBankService(db.memoryBankDao(), OkHttpClient(), context, db)
        try {
            var memory = service()
            val request = MemoryBankService.MemoryWriteRequest(
                content = "水母约定", factTrack = "约好看水母", feelTrack = "期待", assistantId = "a",
                sourceType = "behavior_impulse", sourceId = "impulse-test"
            )
            val saved = memory.writeMemory(request)
            assertEquals(saved.id, memory.writeMemory(request).id)
            try {
                memory.deleteMemory(saved.id, expectedAssistantId = "b")
                fail("cross-owner delete accepted")
            } catch (_: IllegalArgumentException) { }
            val oversized = memory.writeMemory(request.copy(content = "长".repeat(9000), sourceId = "oversized"))
            db.close()
            db = open()
            memory = service()
            assertEquals(saved.id, memory.writeMemory(request).id)
            val recalled = memory.recallForPrompt("a", "水母")
            assertTrue(recalled.any { it.id == saved.id })
            assertEquals(1, db.memoryBankDao().getMemoryById(saved.id)!!.activationCount)
            assertEquals(0, db.memoryBankDao().getMemoryById(oversized.id)!!.activationCount)
            assertTrue(memory.recallForPrompt("b", "水母").isEmpty())
        } finally {
            db.close()
            context.deleteDatabase(name)
        }
    }
}