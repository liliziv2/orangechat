package me.rerere.rikkahub.vocabulary.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.rerere.rikkahub.R
import me.rerere.rikkahub.vocabulary.algorithm.CsvError

/**
 * 一条给用户看的提示。
 *
 * ## 为什么不让 ViewModel 直接给字符串
 *
 * VM 拿不到 Context（往构造函数里塞 Context/Application，Koin 会抛
 * `InstanceCreationException`，App 直接进安全模式），所以在 VM 里拼字符串
 * 就只能写死中文 —— 这个 App 有六套语言，那等于新功能只有中文用户能用。
 *
 * 所以 VM 只说「发生了什么事」，文案在 [text] 里取资源。
 */
sealed interface VocabularyNotice {

    /** 导入成功 */
    data class Imported(val name: String, val count: Int) : VocabularyNotice

    /** 导入失败 */
    data class ImportFailed(val reason: CsvError, val arg: String? = null) : VocabularyNotice

    /** 「学新词」时这本已经没有新词了 */
    data object NoNewWords : VocabularyNotice

    /** 「复习」时没有到期的词 */
    data object NoDueWords : VocabularyNotice
}

/** 把提示翻成人话。只能在 Composable 里调（要读资源）。 */
@Composable
fun VocabularyNotice.text(): String = when (this) {
    is VocabularyNotice.Imported ->
        stringResource(R.string.vocabulary_notice_imported, name, count)

    is VocabularyNotice.ImportFailed -> when (reason) {
        CsvError.UNREADABLE_FILE -> stringResource(R.string.vocabulary_error_unreadable_file)
        CsvError.EMPTY_FILE -> stringResource(R.string.vocabulary_error_empty_file)
        CsvError.NO_WORD -> stringResource(R.string.vocabulary_error_no_word, arg.orEmpty())
        CsvError.MISSING_TRANSLATION -> stringResource(R.string.vocabulary_error_missing_translation)
        CsvError.NO_HEADER -> stringResource(R.string.vocabulary_error_no_header)
    }

    VocabularyNotice.NoNewWords -> stringResource(R.string.vocabulary_notice_no_new_words)
    VocabularyNotice.NoDueWords -> stringResource(R.string.vocabulary_notice_no_due_words)
}
