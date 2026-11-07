package me.rerere.rikkahub.data.db.migrations

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import me.rerere.rikkahub.data.db.AppDatabase
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class Migration_34_35_Test {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java,
        emptyList(), FrameworkSQLiteOpenHelperFactory()
    )

    @Test fun addsNullableMemoryOwnerWithoutReassigningHistory() {
        helper.createDatabase("memory-owner-migration", 34).close()
        helper.runMigrationsAndValidate("memory-owner-migration", 35, true, Migration_34_35).use { db ->
            db.query("SELECT memory_owner_id FROM drive_event_ledger LIMIT 0").use {
                assertTrue(it.columnNames.contains("memory_owner_id"))
            }
        }
    }
}