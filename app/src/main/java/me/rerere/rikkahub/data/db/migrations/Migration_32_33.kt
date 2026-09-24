package me.rerere.rikkahub.data.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v32 -> v33: 开 Elektron **State 层**的三张表。
 *
 * ## 为什么是纯建表，没有数据搬运
 *
 * 这三张表在 v32 之前**根本不存在** —— State 层是这一轮新开的（全仓 grep
 * `DriveEngine` / `drive_state` / `ledger` 在 v32 时代命中 0）。没有历史数据要搬，
 * 所以这个迁移只做建表与建索引，不做任何 `INSERT ... SELECT`。
 *
 * 与 [Migration_31_32] 那种"把旧表搬进新表"的迁移不同，这里**没有回填问题**，
 * 也就不存在 `last_active_at` 那一类"回填错了会集体霸榜"的坑。
 *
 * ## 状态行由代码懒初始化，不在迁移里插
 *
 * `drive_state` 是单行表。这一行**不在这里插** —— 由
 * `DriveStateService` 首次读取时建（此时才知道初始 JSON 该长什么样）。
 * 理由：迁移脚本一旦把初始 JSON 字面量抄一遍，就多了一份**会漂移的副本**；
 * 维度常量在 `DriveEngine` 里，初始值也该从那里来。
 *
 * ## 三张表各是什么
 *
 * | 表 | 回答的问题 | 对应 Elektron |
 * |---|---|---|
 * | `drive_state` | 现在什么感觉（单行） | `drive_state` |
 * | `drive_event_ledger` | 为什么变成这样（只追加） | `drive_event_ledger` |
 * | `drive_sample` | 昨天这个时候是什么样 | `emotion-history.json` |
 *
 * 第三张是**刻意加进 Room 的**：Elektron 把历史采样放在一个 30 小时滚动的 JSON 文件里，
 * 放内存或放文件都不行 —— 前者活不过冷启动（于是"相对上涨"这条判断线永远不生效），
 * 后者的原子写要自己实现。放进 Room 天然拿到事务与持久化。
 *
 * ## 列的顺序与 DDL 必须与 Room 生成的一致
 *
 * Room 的 schema 校验在**运行时**（`validateMigration`），CI 编译期测不出来。
 * 所以下面的 DDL 是照着实体声明逐列写出来的，包括：
 * - 非空列写 `NOT NULL`，可空列不写；
 * - 自增主键写 `INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL`；
 * - 单行表主键写列级 `NOT NULL` + 表级 `PRIMARY KEY(...)`；
 * - **一律不写 `DEFAULT`** —— 实体上没有 `defaultValue` 注解，SQL 里就不该有默认值，
 *   两边不一致会被校验判成 schema 漂移（v31 那次就是栽在 defaultValue 的字面量对不上）。
 */
object Migration_32_33 : Migration(32, 33) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // 当下状态：单行表，主键恒为 1（见 DriveStateEntity.SINGLETON_ID）
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `drive_state` (
                `id` INTEGER NOT NULL,
                `drives_json` TEXT NOT NULL,
                `tick_count` INTEGER NOT NULL,
                `last_ts` INTEGER NOT NULL,
                `prev_drives_json` TEXT NOT NULL,
                `local_fatigue_json` TEXT NOT NULL,
                `snapshot_json` TEXT NOT NULL,
                `escape_streak` INTEGER NOT NULL,
                `last_user_message_at` INTEGER NOT NULL,
                `reunion_pa_boost` REAL NOT NULL,
                `possessiveness_channels_json` TEXT NOT NULL,
                `attachment_rebound_json` TEXT NOT NULL,
                `libido_pending_json` TEXT NOT NULL,
                PRIMARY KEY(`id`)
            )
            """.trimIndent()
        )

        // 事件账本：只追加。可空列（source / reason / *_json）不写 NOT NULL。
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `drive_event_ledger` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `ts` INTEGER NOT NULL,
                `schema_version` TEXT NOT NULL,
                `source` TEXT,
                `event_label` TEXT,
                `primary_drive` TEXT,
                `intensity` REAL,
                `confidence` REAL,
                `agency` REAL,
                `suppressed` INTEGER NOT NULL,
                `reason` TEXT,
                `applied_json` TEXT,
                `brain_json` TEXT,
                `evidence_json` TEXT
            )
            """.trimIndent()
        )

        // 历史采样：基线的参照系
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `drive_sample` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `ts` INTEGER NOT NULL,
                `values_json` TEXT NOT NULL
            )
            """.trimIndent()
        )

        // 索引名必须与 Room 的 @Index(name = ...) 一致
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_drive_event_ledger_ts` ON `drive_event_ledger` (`ts`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_drive_event_ledger_primary_drive` " +
                "ON `drive_event_ledger` (`primary_drive`)"
        )
        db.execSQL(
            "CREATE INDEX IF NOT EXISTS `index_drive_sample_ts` ON `drive_sample` (`ts`)"
        )
    }
}
