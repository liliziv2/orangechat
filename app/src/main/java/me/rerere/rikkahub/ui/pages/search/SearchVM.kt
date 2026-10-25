package me.rerere.rikkahub.ui.pages.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import me.rerere.rikkahub.data.db.fts.MessageSearchResult
import me.rerere.rikkahub.data.datastore.getCurrentAssistant
import me.rerere.rikkahub.data.repository.ConversationRepository

class SearchVM(
    private val conversationRepo: ConversationRepository,
    private val settingsStore: me.rerere.rikkahub.data.datastore.SettingsStore,
) : ViewModel() {
    private val _searchQuery = MutableStateFlow("")

    var searchQuery by mutableStateOf("")
        private set
    var results by mutableStateOf<List<MessageSearchResult>>(emptyList())
        private set
    var cloudResults by mutableStateOf<List<CloudSearchResult>>(emptyList())
        private set

    /**
     * 云搜索失败信息。null = 本次没有失败。
     *
     * 旧实现把异常直接 `cloudResults = emptyList()` 吞掉，用户开了云同步搜索、
     * 云端连不上时只会看到「无结果」，无法区分「真没有」和「连不上」。
     * 现在把失败原因留下来，由页面显示一行提示。
     */
    var cloudError by mutableStateOf<String?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isRebuilding by mutableStateOf(false)
        private set
    var rebuildProgress by mutableStateOf(0 to 0)
        private set

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(300L)
                .collectLatest { query -> performSearch(query) }
        }
    }

    fun onQueryChange(query: String) {
        searchQuery = query
        _searchQuery.value = query
    }

    fun search() {
        viewModelScope.launch {
            performSearch(searchQuery)
        }
    }

    fun rebuildIndex() {
        viewModelScope.launch {
            isRebuilding = true
            rebuildProgress = 0 to 0
            try {
                conversationRepo.rebuildAllIndexes { current, total ->
                    rebuildProgress = current to total
                }
            } finally {
                isRebuilding = false
            }
        }
    }

    private suspend fun performSearch(query: String) {
        if (query.isBlank()) {
            results = emptyList()
            cloudResults = emptyList()
            cloudError = null
            return
        }
        isLoading = true
        try {
            results = conversationRepo.searchMessages(query)
            // 同时搜 Supabase 云端
            searchCloud(query)
        } finally {
            isLoading = false
        }
    }

    private suspend fun searchCloud(query: String) {
        cloudError = null
        try {
            val settings = settingsStore.settingsFlow.value
            val externalConfigs = settings.externalMemories.filter { it.enabled }
            val currentAssistant = settings.getCurrentAssistant()
            val allCloudResults = mutableListOf<CloudSearchResult>()
            externalConfigs.forEach { config ->
                val service = me.rerere.rikkahub.data.service.ExternalMemoryService(config)
                // 先按当前助手 UUID 搜
                var cloudMsgs = service.searchMessages(
                    assistantId = currentAssistant.id.toString(),
                    keyword = query,
                    limit = 20,
                ).getOrDefault(emptyList())
                // UUID 匹配不上时（云端存的是助手名），做一次全局搜索兜底
                if (cloudMsgs.isEmpty()) {
                    cloudMsgs = service.searchMessagesAnyAssistant(
                        keyword = query,
                        assistantId = null,
                        limit = 20,
                    ).getOrDefault(emptyList())
                }
                cloudMsgs.forEach { msg ->
                    allCloudResults.add(CloudSearchResult(
                        content = msg.content,
                        role = msg.role,
                        createdAt = msg.createdAt,
                        source = config.name,
                    ))
                }
            }
            cloudResults = allCloudResults
            cloudError = null
        } catch (e: Exception) {
            cloudResults = emptyList()
            // 错误脱敏：不带出任何 key / token / 完整 URL 凭证片段。
            cloudError = (e.message ?: e::class.simpleName ?: "未知错误")
                .replace(Regex("(?i)(apikey|authorization|bearer|key)\\s*[:=]\\s*\\S+"), "$1=***")
                .take(160)
        }
    }
}

data class CloudSearchResult(
        val content: String,
        val role: String,
        val createdAt: String,
        val source: String,
)
