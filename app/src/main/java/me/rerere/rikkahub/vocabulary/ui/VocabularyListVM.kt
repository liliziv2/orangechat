package me.rerere.rikkahub.vocabulary.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import me.rerere.rikkahub.vocabulary.dao.BookStatsRow
import me.rerere.rikkahub.vocabulary.data.ImportResult
import me.rerere.rikkahub.vocabulary.data.VocabularyRepository
import me.rerere.rikkahub.vocabulary.entity.VocabularyEntity

/**
 * 词库列表页。
 *
 * ⚠️ 构造函数里**不要拿 Context / Application** —— Koin 会抛 InstanceCreationException，
 * App 直接进安全模式。要 Context 就在 Composable 里取 LocalContext。
 */
class VocabularyListVM(
    private val repository: VocabularyRepository,
) : ViewModel() {

    val books: StateFlow<List<VocabularyEntity>> = repository.books
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val deletedBooks: StateFlow<List<VocabularyEntity>> = repository.deletedBooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _stats = MutableStateFlow<Map<Long, BookStatsRow>>(emptyMap())
    val stats: StateFlow<Map<Long, BookStatsRow>> = _stats.asStateFlow()

    private val _todayCount = MutableStateFlow(0)
    val todayCount: StateFlow<Int> = _todayCount.asStateFlow()

    /** 一句给用户看的提示（导入结果之类），显示完由页面调 [consumeNotice] 清掉 */
    private val _notice = MutableStateFlow<VocabularyNotice?>(null)
    val notice: StateFlow<VocabularyNotice?> = _notice.asStateFlow()

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing.asStateFlow()

    // 用户选过但还没提交的文件。⚠️ 必须放这里，不能放 Composable 的 remember：
    // 切出去接个电话再回来，remember 里的 URI 就没了，导入按钮点了没反应还不报错。
    private val _pendingUri = MutableStateFlow<Uri?>(null)
    val pendingUri: StateFlow<Uri?> = _pendingUri.asStateFlow()

    private val _pendingName = MutableStateFlow("")
    val pendingName: StateFlow<String> = _pendingName.asStateFlow()

    /**
     * 重算进度。
     *
     * ⚠️ 这个**必须由页面定时调**（`statsByBook` 里的 `due` 依赖「现在几点」，
     * 时间过去了 Room 不会重发 Flow），但**不能放在 VM 的 init 里循环** ——
     * VM 活得比页面久，那样会在用户没看列表时也一直空转。
     */
    suspend fun refresh() {
        _stats.value = repository.statsByBook(System.currentTimeMillis())
        _todayCount.value = repository.wordsStudiedToday()
    }

    fun onFilePicked(uri: Uri, name: String) {
        _pendingUri.value = uri
        _pendingName.value = name
    }

    fun updatePendingName(name: String) {
        _pendingName.value = name
    }

    fun cancelImport() {
        _pendingUri.value = null
        _pendingName.value = ""
    }

    /**
     * 提交导入。
     *
     * @param fallbackName 用户没填名字时用的默认名。**必须由 UI 传进来** ——
     *                     VM 拿不到 Context，自己写死就只能是中文。
     */
    fun confirmImport(fallbackName: String) {
        val uri = _pendingUri.value ?: return
        if (_importing.value) return
        val name = _pendingName.value
        _importing.value = true
        viewModelScope.launch {
            when (val result = repository.importCsv(uri, name, fallbackName)) {
                is ImportResult.Success -> {
                    // 用真正落库的名字（用户填的可能是空白，那就用了默认名）
                    _notice.value = VocabularyNotice.Imported(
                        name = name.trim().ifBlank { fallbackName },
                        count = result.count,
                    )
                }

                is ImportResult.Failure -> {
                    _notice.value = VocabularyNotice.ImportFailed(result.reason, result.arg)
                }
            }
            _importing.value = false
            _pendingUri.value = null
            _pendingName.value = ""
            refresh()
        }
    }

    fun deleteBook(vocabId: Long) {
        viewModelScope.launch {
            repository.deleteBook(vocabId)
            refresh()
        }
    }

    fun restoreBook(vocabId: Long) {
        viewModelScope.launch {
            repository.restoreBook(vocabId)
            refresh()
        }
    }

    fun consumeNotice() {
        _notice.value = null
    }
}
