/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v30 -> v31: 记忆库接入 Elektron Memory 层所需的字段。
 *
 * 从 Elektron 的 bucket 结构（Markdown frontmatter）搬过来，落到 `memory_bank` 表的列上。
 * 五组，对应五条机制：
 *
 * 1. **时间字段独立保存** —— `last_active_at` / `activation_count`。
 *    Elektron 的 bucket 有 `created` / `last_active` / `activation_count` 三个独立字段，
 *    其中只有 `last_active` 参与召回排序。PITFALLS 里「搬家之后所有旧记忆的排序全乱了」
 *    就是因为导入接口不给 `last_active`、内部一律写当前时间。所以这里**显式回填**
 *    `last_active_at := created_at`，而不是留 0（留 0 会被引擎兜底成 30 天，
 *    等于把全部旧记忆一次性判成"很久没动"）。
 *
 * 2. **事实轨 + 情绪轨** —— `fact_track` / `feel_track`。
 *    不是分两条记录，是同一条记忆里两样都得在（DECISIONS 2026-09-20：
 *    「分轨给了偷懒的口子」）。写入侧的强制校验在 MemoryBankService。
 *
 * 3. **overlay 不覆盖** —— `overlay_of`。
 *    旧记忆永远不改写，新理解追加，因为「当时理解错了」本身是记录的一部分。
 *    只存「这条在重新理解哪一条」这一个方向；反方向的「这条被谁取代了」由
 *    `WHERE overlay_of = :id` 查出来，不额外存一列。少一列就少一次两段写，
 *    链的完整性不依赖两次写之间的原子性。
 *
 * 4. **来源可追溯** —— `source_type` / `source_id` / `source_ts`。
 *
 * 5. **衰减与排序** —— `valence` / `arousal` / `importance` / `domain` /
 *    `resolved` / `digested` / `pinned` / `protected` / `decay_score` / `archived`。
 *
 * 旧数据的取值全部落在语义等价的默认值上，不需要人工回填。
 *
 * ## 为什么文本列是可空的
 *
 * `domain` / `fact_track` / `feel_track` / `source_type` / `source_id` 建成了可空列
 * 而不是 `TEXT NOT NULL DEFAULT ''`。两个原因：
 * 一是 SQLite 允许新增可空列而不带默认值，省掉一次 Room `defaultValue` 与
 * `PRAGMA table_info` 的字符串比对（空串默认值在本仓还没有先例，数值和
 * 非空字符串的写法见 Migration_29_30）；
 * 二是 `fact_track IS NULL` 天然表示"这一轨缺失"，比 `= ''` 更直白。
 * 读取侧统一把 null 当空串处理。
 *
 * ## 一个已知的缺口
 *
 * 旧行的 `valence=0.5` / `arousal=0.3` / `importance=5` 全是默认值，情感权重完全相同 ——
 * 这正是 Elektron DECISIONS「自动打标不许留空」里说的「在库里是死的」那种条目。
 * 修它需要一个自动打标器读内容判领域/情感价/唤起度，属于后续阶段，本迁移只负责把列开出来。
 */
object Migration_30_31 : Migration(30, 31) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // --- 1. 时间字段独立保存 ---
        db.execSQL("ALTER TABLE memory_bank ADD COLUMN last_active_at INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE memory_bank ADD COLUMN activation_count INTEGER NOT NULL DEFAULT 0")

        // --- 2. 双轨（可空：null 表示这一轨缺失，写入侧会拒绝）---
        db.execSQL("ALTER TABLE memory_bank ADD COLUMN fact_track TEXT")
        db.execSQL("ALTER TABLE memory_bank ADD COLUMN feel_track TEXT")

        // --- 3. overlay 链（可空：null 表示这是一条独立记忆，不是对旧记忆的重新理解）---
        db.execSQL("ALTER TABLE memory_bank ADD COLUMN overlay_of INTEGER")

        // --- 4. 来源可追溯 ---
        db.execSQL("ALTER TABLE memory_bank ADD COLUMN source_type TEXT")
        db.execSQL("ALTER TABLE memory_bank ADD COLUMN source_id TEXT")
        db.execSQL("ALTER TABLE memory_bank ADD COLUMN source_ts INTEGER NOT NULL DEFAULT 0")

        // --- 5. 衰减与排序 ---
        db.execSQL("ALTER TABLE memory_bank ADD COLUMN valence REAL NOT NULL DEFAULT 0.5")
        db.execSQL("ALTER TABLE memory_bank ADD COLUMN arousal REAL NOT NULL DEFAULT 0.3")
        db.execSQL("ALTER TABLE memory_bank ADD COLUMN importance INTEGER NOT NULL DEFAULT 5")
        db.execSQL("ALTER TABLE memory_bank ADD COLUMN domain TEXT")
        db.execSQL("ALTER TABLE memory_bank ADD COLUMN resolved INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE memory_bank ADD COLUMN digested INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE memory_bank ADD COLUMN pinned INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE memory_bank ADD COLUMN `protected` INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE memory_bank ADD COLUMN decay_score REAL NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE memory_bank ADD COLUMN archived INTEGER NOT NULL DEFAULT 0")

        // --- 回填：last_active_at := created_at ---
        // 只在确实没值的时候写，避免覆盖迁移期间已由新写入路径填好的行。
        db.execSQL("UPDATE memory_bank SET last_active_at = created_at WHERE last_active_at = 0")
    }
}
