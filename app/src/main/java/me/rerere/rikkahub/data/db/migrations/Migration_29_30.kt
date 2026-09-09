/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v29 -> v30: 记忆增加分类 / 优先级 / 自动记录标记 / 创建时间。
 *
 * - category: 记忆分类（general / preference / fact / plan / relation / event / auto）
 * - priority: 0 普通 / 1 重要 / 2 关键，注入 prompt 时按此降序
 * - auto_generated: 是否由 memory_tool 自动写入
 * - create_at: 创建时间（毫秒），旧数据为 0
 *
 * 旧数据全部落到 general / 0 / false / 0，语义等价于当前行为，不需要数据回填。
 */
object Migration_29_30 : Migration(29, 30) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE memoryentity ADD COLUMN category TEXT NOT NULL DEFAULT 'general'")
        db.execSQL("ALTER TABLE memoryentity ADD COLUMN priority INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE memoryentity ADD COLUMN auto_generated INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE memoryentity ADD COLUMN create_at INTEGER NOT NULL DEFAULT 0")
    }
}
