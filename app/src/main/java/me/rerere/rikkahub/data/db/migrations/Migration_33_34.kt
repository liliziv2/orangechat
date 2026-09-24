package me.rerere.rikkahub.data.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v33 -> v34: 开 Elektron **Behavior 层**的冲动队列。
 *
 * ## 为什么是纯建表
 *
 * `impulse_queue` 在 v33 之前不存在 —— 上一轮（State 层）刻意没有行为出口，
 * 队列是这一轮才出现的。没有历史数据要搬，所以只建表建索引，不做 `INSERT ... SELECT`。
 *
 * ## 为什么队列一定要落盘
 *
 * 验收里有一条"pending wake 不会因为进程重启直接丢失"。把队列放内存里，
 * 或者放一个"退出就清"的临时文件，都会让这条直接失败 —— 而且失败方式是静默的：
 * 用户看到的是"AI 忽然不主动了"，没有任何日志能指回来。
 *
 * 另一条"lease 到期可以恢复"依赖 [ImpulseEntity] 的 `lease_expires_at` 列：
 * 进程在生成过程中被杀，那一行会停在 `claimed`。没有租约的话它会永远卡住，
 * 这个 kind 的唤醒就再也不产生了（因为去重查的是"有没有活跃行"）。
 *
 * ## 列的顺序与 DDL 必须与 Room 生成的一致
 *
 * Room 的 schema 校验在**运行时**（`validateMigration`），CI 编译期测不出来。
 * 下面逐列照着实体声明写，包括：
 * - 非空列写 `NOT NULL`，可空列（`assistant_id` / `lease_owner` / `last_error` /
 *   `note` / `result_memory_id`）不写；
 * - 自增主键写 `INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL`；
 * - **一律不写 `DEFAULT`** —— 实体上没有 `defaultValue` 注解，SQL 里就不该有默认值。
 *
 * ## 索引名必须与 `@Index(name = ...)` 一致
 *
 * Room 的 schema 校验会比对索引集合。迁移里多建一个没在实体上声明的索引，
 * 或者少建一个声明过的，都会在打开数据库时抛 `IllegalStateException`。
 */
object Migration_33_34 : Migration(33, 34) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `impulse_queue` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `created_at` INTEGER NOT NULL,
                `due_at` INTEGER NOT NULL,
                `kind` TEXT NOT NULL,
                `status` TEXT NOT NULL,
                `source` TEXT NOT NULL,
                `dedupe_key` TEXT NOT NULL,
                `payload_json` TEXT NOT NULL,
                `assistant_id` TEXT,
                `claimed_at` INTEGER NOT NULL,
                `lease_owner` TEXT,
                `lease_expires_at` INTEGER NOT NULL,
                `attempts` INTEGER NOT NULL,
                `max_attempts` INTEGER NOT NULL,
                `retry_at` INTEGER NOT NULL,
                `last_error` TEXT,
                `note` TEXT,
                `result_memory_id` INTEGER,
                `finished_at` INTEGER NOT NULL,
                `schema_version` TEXT NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_impulse_queue_status_due_at` " +
                "ON `impulse_queue` (`status`, `due_at`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_impulse_queue_kind_dedupe_key` " +
                "ON `impulse_queue` (`kind`, `dedupe_key`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_impulse_queue_status_finished_at` " +
                "ON `impulse_queue` (`status`, `finished_at`)"
        )
    }
}
