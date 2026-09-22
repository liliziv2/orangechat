/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.data.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v31 -> v32: 把旧的 `memoryentity` 一次性搬进 `memory_bank`，从此只有一个 Memory domain。
 *
 * ## 为什么要搬
 *
 * v31 之前仓里存在两套记忆：老的 `memoryentity`（单轨纯文本 + category/priority）和
 * 新的 `memory_bank`（Elektron Memory 层）。v31 只把新表的列开出来了，**写入侧一个调用点
 * 都没改**，于是新表一直是空的，真实在跑的还是老表 —— 也就是"新路径全死、旧路径全活"。
 * 这次收口把写入侧全部改走 `MemoryBankService`，老表随之停写；但老表里已有的记忆不能丢，
 * 所以在这里搬一次。
 *
 * 搬完之后 `memoryentity` **保留不动**（既不删表也不删数据），作为只读的历史备份存在。
 * 真要回退，数据还在。
 *
 * ## 字段映射，以及每一处为什么这么定
 *
 * | 旧 `memoryentity` | 新 `memory_bank` | 说明 |
 * |---|---|---|
 * | `content` | `content` | 原文即现场，原样搬 |
 * | `content` | `fact_track` | 老记录是单轨的，它记的本来就是"事实"这一面 |
 * | — | `feel_track` | **留 NULL**。老数据里没有情绪轨，不能编一条出来；`feel_track IS NULL` 正是"这一轨缺失"的既定表示法 |
 * | `assistant_id` | `assistant_id` | 直接搬 |
 * | `category` | `domain` | 老的分类枚举（preference/fact/plan/relation/event）在新模型里就是主题域。`general` 表示"没有特定域"，按新表的约定写成 NULL 而不是字符串 `general` |
 * | `priority` 0/1/2 | `importance` 5/7/10 | 老的是 0..2 三档，新的是 1..10。2 在老文档里是"绝不能忘"，对应新模型的满分 10 |
 * | `auto_generated` | `source_type` | 复用新表既有的 source_type 词表：1→`auto`，0→`manual` |
 * | `id` | `source_id` = `'memoryentity:'‖id` | 保留可追溯性，见下 |
 * | `create_at` | `created_at` / `source_ts` | 直接搬 |
 * | — | `last_active_at` | **必须等于 `created_at`**，见下 |
 * | — | `type` | 统一写 `legacy`，让"这批是搬过来的"在数据里就能看出来 |
 *
 * ### `last_active_at` 为什么要显式回填
 *
 * 这是 Migration_30_31 里已经吃过一次的坑（PITFALLS「搬家之后所有旧记忆的排序全乱了」）：
 * 只有 `last_active_at` 参与召回排序，把它留 0 会被 `MemoryDecayEngine.daysSince` 兜底成
 * 30 天，等于把全部旧记忆一次性判成"很久没动"；反过来如果给所有旧记忆写当前时间，
 * 它们又会集体顶到最前面。唯一正确的做法是 `last_active_at := created_at`。
 *
 * 老表里 `create_at` 可能是 0（更早的版本不记时间）。这批没法还原真实时间，
 * 用迁移执行的时刻顶上 —— 至少不会把 1970 年的记录当成"刚活跃"。
 *
 * ### 为什么不搬 `id`
 *
 * `memory_bank.id` 是自增主键，直接沿用老 id 会和表里已有的行撞主键。所以让 SQLite 自己分配，
 * 老 id 存进 `source_id`，照样能追回"这条是从哪来的"。
 *
 * ### 一个刻意的克制：`priority = 2` 不映射成 `pinned`
 *
 * 老文档说 priority 2 是"绝不能忘"，而新模型里 `pinned` 的语义正好是"不参与衰减、
 * importance 锁 10"，看起来该直接对应过去。这里没有这么做：`pinned` 会让
 * `MemoryDecayEngine` 直接返回 `NEVER_DECAY_SCORE = 999`，一旦用户攒了几条
 * priority 2，它们会永久霸占召回列表最前面，而且这是个迁移不该单方面做的排序行为改变。
 * 所以只映射 `importance`（2→10，已经是满分），钉选留给用户显式操作。
 */
object Migration_31_32 : Migration(31, 32) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            INSERT INTO memory_bank (
                content, type, conversation_id, assistant_id, role,
                created_at, date_group, vector_status, vector_retry_count, embedding,
                last_active_at, activation_count, valence, arousal, importance, domain,
                fact_track, feel_track, overlay_of,
                source_type, source_id, source_ts,
                resolved, digested, pinned, `protected`, decay_score, archived
            )
            SELECT
                content,
                'legacy',
                NULL,
                assistant_id,
                NULL,
                CASE WHEN create_at > 0 THEN create_at ELSE CAST(strftime('%s', 'now') AS INTEGER) * 1000 END,
                NULL,
                'skipped',
                0,
                NULL,
                CASE WHEN create_at > 0 THEN create_at ELSE CAST(strftime('%s', 'now') AS INTEGER) * 1000 END,
                0,
                0.5,
                0.3,
                CASE WHEN priority >= 2 THEN 10 WHEN priority = 1 THEN 7 ELSE 5 END,
                CASE WHEN category IS NULL OR category = '' OR category = 'general' THEN NULL ELSE category END,
                content,
                NULL,
                NULL,
                CASE WHEN auto_generated = 1 THEN 'auto' ELSE 'manual' END,
                'memoryentity:' || id,
                CASE WHEN create_at > 0 THEN create_at ELSE CAST(strftime('%s', 'now') AS INTEGER) * 1000 END,
                0,
                0,
                0,
                0,
                0,
                0
            FROM memoryentity
            """.trimIndent()
        )
    }
}
