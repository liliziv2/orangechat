package me.rerere.rikkahub.data.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** Old ledger rows deliberately keep an unknown owner; no speculative reassignment. */
object Migration_34_35 : Migration(34, 35) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE drive_event_ledger ADD COLUMN memory_owner_id TEXT")
    }
}
