package me.rerere.rikkahub.vocabulary.di

import android.content.Context
import androidx.room.Room
import me.rerere.rikkahub.vocabulary.VocabularyDatabase
import me.rerere.rikkahub.vocabulary.data.VocabularyRepository
import me.rerere.rikkahub.vocabulary.study.StudySessionStore
import org.koin.dsl.module

/**
 * 背单词模块的依赖注册。**整块可拆** —— 拆的时候删掉 `vocabulary/` 目录，
 * 再把 RikkaHubApp 里 `modules(...)` 的这一项去掉即可。
 *
 * ⚠️ DAO 要**一个一个列出来**，别指望有什么批量注册的写法。漏了哪个，编译能过，
 * 但运行时 Koin 解析不到 → 进页面即崩（报 InstanceCreationException）。
 * 好消息是这个模块的注册全在这一个文件里，漏了也只看这一处。
 */
val vocabularyModule = module {

    // 独立的第二个 Room 实例，库文件叫 vocabulary.db，和主库 rika_hub 完全无关。
    // Koin 的 single 是懒汉式 —— 用户没进背单词页就不会建库。
    single {
        Room.databaseBuilder(
            get<Context>(),
            VocabularyDatabase::class.java,
            "vocabulary.db",
        ).build()
    }

    single { get<VocabularyDatabase>().vocabularyDao() }
    single { get<VocabularyDatabase>().vocabularyCardDao() }
    single { get<VocabularyDatabase>().studyRecordDao() }
    single { get<VocabularyDatabase>().studySessionDao() }

    single { StudySessionStore(context = get()) }

    single {
        VocabularyRepository(
            context = get(),
            vocabularyDao = get(),
            cardDao = get(),
            recordDao = get(),
            sessionDao = get(),
            sessionStore = get(),
        )
    }
}
