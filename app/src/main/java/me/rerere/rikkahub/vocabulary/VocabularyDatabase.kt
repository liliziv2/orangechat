package me.rerere.rikkahub.vocabulary

import androidx.room.Database
import androidx.room.RoomDatabase
import me.rerere.rikkahub.vocabulary.dao.StudyRecordDao
import me.rerere.rikkahub.vocabulary.dao.StudySessionDao
import me.rerere.rikkahub.vocabulary.dao.VocabularyCardDao
import me.rerere.rikkahub.vocabulary.dao.VocabularyDao
import me.rerere.rikkahub.vocabulary.entity.StudyRecordEntity
import me.rerere.rikkahub.vocabulary.entity.StudySessionEntity
import me.rerere.rikkahub.vocabulary.entity.VocabularyCardEntity
import me.rerere.rikkahub.vocabulary.entity.VocabularyEntity

/**
 * 背单词自己的数据库，**独立于主库**（`rikka_hub`）。
 *
 * 为什么单开一个库而不是并进 AppDatabase：
 *
 * 1. 这一整块是可拆除的。主库不动，拆的时候删掉 `vocabulary/` 目录 + 一行 Koin 注册即可，
 *    不用回退版本号、不用碰迁移链。
 * 2. 主库现在停在 version 34、迁移链是「autoMigration 到 28→29 + 手工迁移到 33→34」混着走的，
 *    往里加表要动一个已经在用户设备上跑着数据的文件。新表虽然能自动迁移，但没必要担这个风险。
 * 3. 这一块和主库**没有任何跨库事务需求** —— 词库、卡片、答题记录、会话四张表互相自洽。
 *
 * ⚠️ 表结构将来要改时：`exportSchema = false` 意味着 Room 不会导出 schema，
 * 于是**没法用 AutoMigration**。改列之前先把 `exportSchema` 改回 true、把
 * `app/schemas/` 下的 json 提交进仓库（注意 `audit/changes.py` 目前不跟踪 .json，
 * 要手动补），再挂 `AutoMigration(from = 1, to = 2)`。纯新增表则不受影响。
 */
@Database(
    entities = [
        VocabularyEntity::class,
        VocabularyCardEntity::class,
        StudyRecordEntity::class,
        StudySessionEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class VocabularyDatabase : RoomDatabase() {
    abstract fun vocabularyDao(): VocabularyDao

    abstract fun vocabularyCardDao(): VocabularyCardDao

    abstract fun studyRecordDao(): StudyRecordDao

    abstract fun studySessionDao(): StudySessionDao
}
