package me.rerere.rikkahub.data.datastore

import android.content.Context
import android.util.Log
import androidx.datastore.core.IOException
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.pebbletemplates.pebble.PebbleEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.rerere.ai.core.MessageRole
import me.rerere.ai.core.ReasoningLevel
import me.rerere.ai.provider.Model
import me.rerere.ai.provider.ProviderSetting
import me.rerere.asr.ASRProviderSetting
import me.rerere.rikkahub.AppScope
import me.rerere.rikkahub.data.datastore.migration.PreferenceStoreV1Migration
import me.rerere.rikkahub.data.datastore.migration.PreferenceStoreV2Migration
import me.rerere.rikkahub.data.datastore.migration.PreferenceStoreV3Migration
import me.rerere.rikkahub.data.ai.mcp.McpServerConfig
import me.rerere.rikkahub.data.ai.prompts.DEFAULT_COMPRESS_PROMPT
import me.rerere.rikkahub.data.ai.prompts.DEFAULT_OCR_PROMPT
import me.rerere.rikkahub.data.ai.prompts.DEFAULT_SUGGESTION_PROMPT
import me.rerere.rikkahub.data.ai.prompts.DEFAULT_TITLE_PROMPT
import me.rerere.rikkahub.data.ai.prompts.DEFAULT_TRANSLATION_PROMPT
import me.rerere.rikkahub.data.ai.prompts.LEARNING_MODE_PROMPT
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.Avatar
import me.rerere.rikkahub.data.model.ExternalMemory
import me.rerere.rikkahub.data.model.InjectionPosition
import me.rerere.rikkahub.data.model.Lorebook
import me.rerere.rikkahub.data.model.MiniApp
import me.rerere.rikkahub.data.model.PromptInjection
import me.rerere.rikkahub.data.model.QuickMessage
import me.rerere.rikkahub.data.model.Tag
import me.rerere.rikkahub.data.sync.s3.S3Config
import me.rerere.rikkahub.ui.theme.CustomTheme
import me.rerere.rikkahub.ui.theme.PresetThemes
import me.rerere.rikkahub.utils.JsonInstant
import me.rerere.search.SearchCommonOptions
import me.rerere.search.SearchServiceOptions
import me.rerere.tts.provider.TTSProviderSetting
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import kotlin.uuid.Uuid

private const val TAG = "PreferencesStore"

private val Context.settingsStore by preferencesDataStore(
    name = "settings",
    produceMigrations = { context ->
        listOf(
            PreferenceStoreV1Migration(),
            PreferenceStoreV2Migration(),
            PreferenceStoreV3Migration()
        )
    }
)

class SettingsStore(
    context: Context,
    scope: AppScope,
) : KoinComponent {
    companion object {
        // 版本号
        val VERSION = intPreferencesKey("data_version")

        // UI设置
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val THEME_ID = stringPreferencesKey("theme_id")
        val CUSTOM_THEMES = stringPreferencesKey("custom_themes")
        val DISPLAY_SETTING = stringPreferencesKey("display_setting")
        val DEVELOPER_MODE = booleanPreferencesKey("developer_mode")

        // 免责声明与用户协议
        val DISCLAIMER_ACCEPTED = booleanPreferencesKey("disclaimer_accepted")
        val DISCLAIMER_ACCEPTED_AT = intPreferencesKey("disclaimer_accepted_at")

        // 模型选择
        val ENABLE_WEB_SEARCH = booleanPreferencesKey("enable_web_search")
        val FAVORITE_MODELS = stringPreferencesKey("favorite_models")
        val SELECT_MODEL = stringPreferencesKey("chat_model")
        val TITLE_MODEL = stringPreferencesKey("title_model")
        val TRANSLATE_MODEL = stringPreferencesKey("translate_model")
        val SUGGESTION_MODEL = stringPreferencesKey("suggestion_model")
        val IMAGE_GENERATION_MODEL = stringPreferencesKey("image_generation_model")
        val TITLE_PROMPT = stringPreferencesKey("title_prompt")
        val TRANSLATION_PROMPT = stringPreferencesKey("translation_prompt")
        val TRANSLATE_THINKING_BUDGET = intPreferencesKey("translate_thinking_budget")
        val SUGGESTION_PROMPT = stringPreferencesKey("suggestion_prompt")
        val OCR_MODEL = stringPreferencesKey("ocr_model")
        val OCR_PROMPT = stringPreferencesKey("ocr_prompt")
        val COMPRESS_MODEL = stringPreferencesKey("compress_model")
        val COMPRESS_PROMPT = stringPreferencesKey("compress_prompt")

        // 提供商
        val PROVIDERS = stringPreferencesKey("providers")

        // 助手
        val SELECT_ASSISTANT = stringPreferencesKey("select_assistant")
        val ASSISTANTS = stringPreferencesKey("assistants")
        val ASSISTANT_TAGS = stringPreferencesKey("assistant_tags")

        // 搜索
        val SEARCH_SERVICES = stringPreferencesKey("search_services")
        val SEARCH_COMMON = stringPreferencesKey("search_common")
        val SEARCH_SELECTED = intPreferencesKey("search_selected")

        // MCP
        val MCP_SERVERS = stringPreferencesKey("mcp_servers")

        // WebDAV
        val WEBDAV_CONFIG = stringPreferencesKey("webdav_config")

        // S3
        val S3_CONFIG = stringPreferencesKey("s3_config")

        // TTS
        val TTS_PROVIDERS = stringPreferencesKey("tts_providers")
        val SELECTED_TTS_PROVIDER = stringPreferencesKey("selected_tts_provider")

        // ASR
        val ASR_PROVIDERS = stringPreferencesKey("asr_providers")
        val SELECTED_ASR_PROVIDER = stringPreferencesKey("selected_asr_provider")

        // Web Server
        val WEB_SERVER_ENABLED = booleanPreferencesKey("web_server_enabled")
        val WEB_SERVER_PORT = intPreferencesKey("web_server_port")
        val WEB_SERVER_JWT_ENABLED = booleanPreferencesKey("web_server_jwt_enabled")
        val WEB_SERVER_ACCESS_PASSWORD = stringPreferencesKey("web_server_access_password")
        val WEB_SERVER_LOCALHOST_ONLY = booleanPreferencesKey("web_server_localhost_only")

        // 提示词注入
        val MODE_INJECTIONS = stringPreferencesKey("mode_injections")
        val LOREBOOKS = stringPreferencesKey("lorebooks")
        val QUICK_MESSAGES = stringPreferencesKey("quick_messages")

        // 备份提醒
        val BACKUP_REMINDER_CONFIG = stringPreferencesKey("backup_reminder_config")

        // 统计
        val LAUNCH_COUNT = intPreferencesKey("launch_count")

        // 赞助提醒
        val SPONSOR_ALERT_DISMISSED_AT = intPreferencesKey("sponsor_alert_dismissed_at")

        // 系统工具设置
        val SYSTEM_TOOLS_SETTING = stringPreferencesKey("system_tools_setting")

        // 主动消息设置
        val PROACTIVE_MESSAGE_SETTING = stringPreferencesKey("proactive_message_setting")

        // 保活服务设置
        val KEEP_ALIVE_ENABLED = booleanPreferencesKey("keep_alive_enabled")

        // 外部记忆库
        val EXTERNAL_MEMORIES = stringPreferencesKey("external_memories")

        // 微信 Bot (iLink 协议)
        val WECHAT_BOT_SETTING = stringPreferencesKey("wechat_bot_setting")

        // QQ Bot (API v2, WebSocket 网关)
        val QQ_BOT_SETTING = stringPreferencesKey("qq_bot_setting")

        // Mini App
        val MINI_APPS = stringPreferencesKey("mini_apps")

        // 强制确认所有工具调用
        val FORCE_CONFIRM_TOOL_CALLS = booleanPreferencesKey("force_confirm_tool_calls")

        // 后台触发工作流时拦截敏感工具（needsApproval=true）
        val WORKFLOW_HEADLESS_BLOCK_SENSITIVE = booleanPreferencesKey("workflow_headless_block_sensitive")

        // 自动批准所有工具调用（懒人模式）
        val AUTO_APPROVE_ALL_TOOLS = booleanPreferencesKey("auto_approve_all_tools")
    }

    private val dataStore = context.settingsStore

    // 用于检测 assistants 列表是否真正变化，避免无关设置写入触发 Pebble 模板缓存清空
    @Volatile
    private var lastAssistantsForCacheInvalidation: List<Assistant>? = null

    val settingsFlowRaw = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            Settings(
                disclaimerAccepted = preferences[DISCLAIMER_ACCEPTED] == true,
                disclaimerAcceptedAt = preferences[DISCLAIMER_ACCEPTED_AT] ?: 0,
                enableWebSearch = preferences[ENABLE_WEB_SEARCH] == true,
                favoriteModels = preferences[FAVORITE_MODELS]?.let {
                    JsonInstant.decodeFromString(it)
                } ?: emptyList(),
                chatModelId = preferences[SELECT_MODEL]?.let { Uuid.parse(it) }
                    ?: DEFAULT_AUTO_MODEL_ID,
                titleModelId = preferences[TITLE_MODEL]?.let { Uuid.parse(it) }
                    ?: DEFAULT_AUTO_MODEL_ID,
                translateModeId = preferences[TRANSLATE_MODEL]?.let { Uuid.parse(it) }
                    ?: DEFAULT_AUTO_MODEL_ID,
                suggestionModelId = preferences[SUGGESTION_MODEL]?.let { Uuid.parse(it) }
                    ?: DEFAULT_AUTO_MODEL_ID,
                imageGenerationModelId = preferences[IMAGE_GENERATION_MODEL]?.let { Uuid.parse(it) } ?: Uuid.random(),
                titlePrompt = preferences[TITLE_PROMPT] ?: DEFAULT_TITLE_PROMPT,
                translatePrompt = preferences[TRANSLATION_PROMPT] ?: DEFAULT_TRANSLATION_PROMPT,
                translateThinkingBudget = preferences[TRANSLATE_THINKING_BUDGET] ?: 0,
                suggestionPrompt = preferences[SUGGESTION_PROMPT] ?: DEFAULT_SUGGESTION_PROMPT,
                ocrModelId = preferences[OCR_MODEL]?.let { Uuid.parse(it) } ?: Uuid.random(),
                ocrPrompt = preferences[OCR_PROMPT] ?: DEFAULT_OCR_PROMPT,
                compressModelId = preferences[COMPRESS_MODEL]?.let { Uuid.parse(it) } ?: DEFAULT_AUTO_MODEL_ID,
                compressPrompt = preferences[COMPRESS_PROMPT] ?: DEFAULT_COMPRESS_PROMPT,
                assistantId = preferences[SELECT_ASSISTANT]?.let { Uuid.parse(it) }
                    ?: DEFAULT_ASSISTANT_ID,
                assistantTags = preferences[ASSISTANT_TAGS]?.let {
                    JsonInstant.decodeFromString(it)
                } ?: emptyList(),
                providers = JsonInstant.decodeFromString(preferences[PROVIDERS] ?: "[]"),
                assistants = JsonInstant.decodeFromString(preferences[ASSISTANTS] ?: "[]"),
                dynamicColor = preferences[DYNAMIC_COLOR] != false,
                themeId = preferences[THEME_ID] ?: PresetThemes[0].id,
                customThemes = preferences[CUSTOM_THEMES]?.let {
                    JsonInstant.decodeFromString(it)
                } ?: emptyList(),
                developerMode = preferences[DEVELOPER_MODE] == true,
                displaySetting = JsonInstant.decodeFromString(preferences[DISPLAY_SETTING] ?: "{}"),
                searchServices = preferences[SEARCH_SERVICES]?.let {
                    JsonInstant.decodeFromString(it)
                } ?: listOf(SearchServiceOptions.DEFAULT),
                searchCommonOptions = preferences[SEARCH_COMMON]?.let {
                    JsonInstant.decodeFromString(it)
                } ?: SearchCommonOptions(),
                searchServiceSelected = preferences[SEARCH_SELECTED] ?: 0,
                mcpServers = preferences[MCP_SERVERS]?.let {
                    JsonInstant.decodeFromString(it)
                } ?: emptyList(),
                webDavConfig = preferences[WEBDAV_CONFIG]?.let {
                    JsonInstant.decodeFromString(it)
                } ?: WebDavConfig(),
                s3Config = preferences[S3_CONFIG]?.let {
                    JsonInstant.decodeFromString(it)
                } ?: S3Config(),
                ttsProviders = preferences[TTS_PROVIDERS]?.let {
                    JsonInstant.decodeFromString(it)
                } ?: emptyList(),
                selectedTTSProviderId = preferences[SELECTED_TTS_PROVIDER]?.let { Uuid.parse(it) }
                    ?: DEFAULT_SYSTEM_TTS_ID,
                asrProviders = preferences[ASR_PROVIDERS]?.let {
                    JsonInstant.decodeFromString(it)
                } ?: emptyList(),
                selectedASRProviderId = preferences[SELECTED_ASR_PROVIDER]?.let { Uuid.parse(it) },
                modeInjections = preferences[MODE_INJECTIONS]?.let {
                    JsonInstant.decodeFromString(it)
                } ?: emptyList(),
                lorebooks = preferences[LOREBOOKS]?.let {
                    JsonInstant.decodeFromString(it)
                } ?: emptyList(),
                quickMessages = preferences[QUICK_MESSAGES]?.let {
                    JsonInstant.decodeFromString(it)
                } ?: emptyList(),
                webServerEnabled = preferences[WEB_SERVER_ENABLED] == true,
                webServerPort = preferences[WEB_SERVER_PORT] ?: 8080,
                webServerJwtEnabled = preferences[WEB_SERVER_JWT_ENABLED] == true,
                webServerAccessPassword = preferences[WEB_SERVER_ACCESS_PASSWORD] ?: "",
                webServerLocalhostOnly = preferences[WEB_SERVER_LOCALHOST_ONLY] == true,
                backupReminderConfig = preferences[BACKUP_REMINDER_CONFIG]?.let {
                    JsonInstant.decodeFromString(it)
                } ?: BackupReminderConfig(),
                launchCount = preferences[LAUNCH_COUNT] ?: 0,
                sponsorAlertDismissedAt = preferences[SPONSOR_ALERT_DISMISSED_AT] ?: 0,
                systemToolsSetting = preferences[SYSTEM_TOOLS_SETTING]?.let {
                    JsonInstant.decodeFromString(it)
                } ?: SystemToolsSetting(),
                proactiveMessageSetting = preferences[PROACTIVE_MESSAGE_SETTING]?.let {
                    JsonInstant.decodeFromString(it)
                } ?: ProactiveMessageSetting(),
                wechatBotSetting = preferences[WECHAT_BOT_SETTING]?.let {
                    JsonInstant.decodeFromString(it)
                } ?: WechatBotSetting(),
                qqBotSetting = preferences[QQ_BOT_SETTING]?.let {
                    JsonInstant.decodeFromString(it)
                } ?: QqBotSetting(),
                keepAliveEnabled = preferences[KEEP_ALIVE_ENABLED] == true,
                externalMemories = preferences[EXTERNAL_MEMORIES]?.let {
                    JsonInstant.decodeFromString(it)
                } ?: emptyList(),
                miniApps = preferences[MINI_APPS]?.let {
                    JsonInstant.decodeFromString(it)
                } ?: emptyList(),
                forceConfirmToolCalls = preferences[FORCE_CONFIRM_TOOL_CALLS] != false,
                workflowHeadlessBlockSensitive = preferences[WORKFLOW_HEADLESS_BLOCK_SENSITIVE] != false,
                autoApproveAllTools = preferences[AUTO_APPROVE_ALL_TOOLS] == true,
            )
        }
        .map {
            var providers = it.providers.ifEmpty { DEFAULT_PROVIDERS }.toMutableList()
            DEFAULT_PROVIDERS.forEach { defaultProvider ->
                if (providers.none { it.id == defaultProvider.id }) {
                    providers.add(defaultProvider.copyProvider())
                }
            }
            providers = providers.map { provider ->
                val defaultProvider = DEFAULT_PROVIDERS.find { it.id == provider.id }
                if (defaultProvider != null) {
                    provider.copyProvider(
                        builtIn = defaultProvider.builtIn,
                        description = defaultProvider.description,
                        shortDescription = defaultProvider.shortDescription,
                    )
                } else provider
            }.toMutableList()
            val assistants = it.assistants.ifEmpty { DEFAULT_ASSISTANTS }.toMutableList()
            DEFAULT_ASSISTANTS.forEach { defaultAssistant ->
                if (assistants.none { it.id == defaultAssistant.id }) {
                    assistants.add(defaultAssistant.copy())
                }
            }
            val ttsProviders = it.ttsProviders.ifEmpty { DEFAULT_TTS_PROVIDERS }.toMutableList()
            DEFAULT_TTS_PROVIDERS.forEach { defaultTTSProvider ->
                if (ttsProviders.none { provider -> provider.id == defaultTTSProvider.id }) {
                    ttsProviders.add(defaultTTSProvider.copyProvider())
                }
            }
            it.copy(
                providers = providers,
                assistants = assistants,
                ttsProviders = ttsProviders,
            )
        }
        .map { settings ->
            // 去重并清理无效引用
            val validMcpServerIds = settings.mcpServers.map { it.id }.toSet()
            val validModeInjectionIds = settings.modeInjections.map { it.id }.toSet()
            val validLorebookIds = settings.lorebooks.map { it.id }.toSet()
            val validQuickMessageIds = settings.quickMessages.map { it.id }.toSet()
            val asrProviders = settings.asrProviders.distinctBy { it.id }
            settings.copy(
                providers = settings.providers.distinctBy { it.id }.map { provider ->
                    when (provider) {
                        is ProviderSetting.OpenAI -> provider.copy(
                            models = provider.models.distinctBy { model -> model.id }
                        )

                        is ProviderSetting.Google -> provider.copy(
                            models = provider.models.distinctBy { model -> model.id }
                        )

                        is ProviderSetting.Claude -> provider.copy(
                            models = provider.models.distinctBy { model -> model.id }
                        )
                    }
                },
                assistants = settings.assistants.distinctBy { it.id }.map { assistant ->
                    assistant.copy(
                        // 过滤掉不存在的 MCP 服务器 ID
                        mcpServers = assistant.mcpServers.filter { serverId ->
                            serverId in validMcpServerIds
                        }.toSet(),
                        // 过滤掉不存在的模式注入 ID
                        modeInjectionIds = assistant.modeInjectionIds.filter { id ->
                            id in validModeInjectionIds
                        }.toSet(),
                        // 过滤掉不存在的 Lorebook ID
                        lorebookIds = assistant.lorebookIds.filter { id ->
                            id in validLorebookIds
                        }.toSet(),
                        // 过滤掉不存在的快捷消息 ID
                        quickMessageIds = assistant.quickMessageIds.filter { id ->
                            id in validQuickMessageIds
                        }.toSet()
                    )
                },
                ttsProviders = settings.ttsProviders.distinctBy { it.id },
                asrProviders = asrProviders,
                selectedASRProviderId = settings.selectedASRProviderId
                    ?.takeIf { id -> asrProviders.any { provider -> provider.id == id } }
                    ?: asrProviders.firstOrNull()?.id,
                favoriteModels = settings.favoriteModels.filter { uuid ->
                    settings.providers.flatMap { it.models }.any { it.id == uuid }
                },
                modeInjections = settings.modeInjections.distinctBy { it.id },
                lorebooks = settings.lorebooks.distinctBy { it.id },
                quickMessages = settings.quickMessages.distinctBy { it.id },
                miniApps = settings.miniApps.distinctBy { it.id },
            )
        }
        .onEach { settings ->
            // 只在助手列表变化时才清空 Pebble 模板缓存（assistant 的 messageTemplate 字段决定模板内容）
            // 避免无关设置变化（如显示设置、provider 设置等）触发不必要的模板重新编译
            if (settings.assistants != lastAssistantsForCacheInvalidation) {
                lastAssistantsForCacheInvalidation = settings.assistants
                get<PebbleEngine>().templateCache.invalidateAll()
            }
        }

    // 内存是唯一的事实来源：启动时从盘上读一次种子，之后不再把盘上的值镜像回内存。
    // 原实现把 dataStore.data 持续收集进 StateFlow，写盘后的磁盘回读比内存晚一拍，
    // 会盖掉这期间的新修改（快速输入/连续拖 slider 丢更新，上游 3462d9ee 的根因）。
    // settingsFlowRaw 本身保留：多处服务用 settingsFlowRaw.first() 取磁盘快照，行为不变。
    val settingsFlow = MutableStateFlow(Settings.dummy())

    // 设置加载完成标记：磁盘加载失败时 update(fn) 退回旧行为，不无限等待
    @Volatile
    private var settingsLoadFailed = false

    // 写盘串行 + 合并：连续修改只落盘最新一份；trySend 永不挂起，
    // 写入方常驻 AppScope，调用方协程被取消也不会丢掉已生效的更改。
    private val pendingWrites = Channel<Settings>(capacity = Channel.CONFLATED)

    // 序列化内存修改：并发 update 不再互相覆盖
    private val updateMutex = Mutex()

    init {
        scope.launch {
            runCatching { settingsFlowRaw.first() }
                .onSuccess { loaded ->
                    lastAssistantsForCacheInvalidation = loaded.assistants
                    settingsFlow.value = loaded
                }
                .onFailure { e ->
                    settingsLoadFailed = true
                    Log.e(TAG, "Failed to load settings from disk", e)
                }
        }
        // 常驻写盘方：消费合并后的最新设置并落盘。
        scope.launch {
            for (settings in pendingWrites) {
                try {
                    dataStore.edit { preferences -> persist(settings, preferences) }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to persist settings", e)
                }
            }
        }
    }

    private suspend fun awaitLoaded() {
        settingsFlow.filterNot { it.init }.first()
    }

    // 内存写入路径：原 settingsFlowRaw.onEach 里的 Pebble 缓存失效逻辑随镜像一起迁过来
    private fun updateMemory(settings: Settings) {
        // 只在助手列表变化时才清空 Pebble 模板缓存（assistant 的 messageTemplate 字段决定模板内容）
        // 避免无关设置变化（如显示设置、provider 设置等）触发不必要的模板重新编译
        if (settings.assistants != lastAssistantsForCacheInvalidation) {
            lastAssistantsForCacheInvalidation = settings.assistants
            get<PebbleEngine>().templateCache.invalidateAll()
        }
        settingsFlow.value = settings
    }

    private fun applyUpdate(settings: Settings) {
        if (settings.init) {
            Log.w(TAG, "Cannot update dummy settings")
            return
        }
        updateMemory(settings)
        pendingWrites.trySend(settings)
    }

    // 把 Settings 的全部字段编码成 (key, value) 对；value 为 null 表示该 key 应被移除。
    // 与旧 update() 的逐字段赋值一一对应，键集合与存储格式不变。
    private fun Settings.toPreferencePairs(): List<Pair<Preferences.Key<*>, Any?>> =
        listOf<Pair<Preferences.Key<*>, Any?>>(
        Pair(DYNAMIC_COLOR, dynamicColor),
        Pair(THEME_ID, themeId),
        Pair(CUSTOM_THEMES, JsonInstant.encodeToString(customThemes)),
        Pair(DEVELOPER_MODE, developerMode),
        Pair(DISPLAY_SETTING, JsonInstant.encodeToString(displaySetting)),
        Pair(DISCLAIMER_ACCEPTED, disclaimerAccepted),
        Pair(DISCLAIMER_ACCEPTED_AT, disclaimerAcceptedAt),
        Pair(ENABLE_WEB_SEARCH, enableWebSearch),
        Pair(FAVORITE_MODELS, JsonInstant.encodeToString(favoriteModels)),
        Pair(SELECT_MODEL, chatModelId.toString()),
        Pair(TITLE_MODEL, titleModelId.toString()),
        Pair(TRANSLATE_MODEL, translateModeId.toString()),
        Pair(SUGGESTION_MODEL, suggestionModelId.toString()),
        Pair(IMAGE_GENERATION_MODEL, imageGenerationModelId.toString()),
        Pair(TITLE_PROMPT, titlePrompt),
        Pair(TRANSLATION_PROMPT, translatePrompt),
        Pair(TRANSLATE_THINKING_BUDGET, translateThinkingBudget),
        Pair(SUGGESTION_PROMPT, suggestionPrompt),
        Pair(OCR_MODEL, ocrModelId.toString()),
        Pair(OCR_PROMPT, ocrPrompt),
        Pair(COMPRESS_MODEL, compressModelId.toString()),
        Pair(COMPRESS_PROMPT, compressPrompt),
        Pair(PROVIDERS, JsonInstant.encodeToString(providers)),
        Pair(ASSISTANTS, JsonInstant.encodeToString(assistants)),
        Pair(SELECT_ASSISTANT, assistantId.toString()),
        Pair(ASSISTANT_TAGS, JsonInstant.encodeToString(assistantTags)),
        Pair(SEARCH_SERVICES, JsonInstant.encodeToString(searchServices)),
        Pair(SEARCH_COMMON, JsonInstant.encodeToString(searchCommonOptions)),
        Pair(SEARCH_SELECTED, searchServiceSelected.coerceIn(0, searchServices.size - 1)),
        Pair(MCP_SERVERS, JsonInstant.encodeToString(mcpServers)),
        Pair(WEBDAV_CONFIG, JsonInstant.encodeToString(webDavConfig)),
        Pair(S3_CONFIG, JsonInstant.encodeToString(s3Config)),
        Pair(TTS_PROVIDERS, JsonInstant.encodeToString(ttsProviders)),
        Pair(SELECTED_TTS_PROVIDER, selectedTTSProviderId?.toString()),
        Pair(ASR_PROVIDERS, JsonInstant.encodeToString(asrProviders)),
        Pair(SELECTED_ASR_PROVIDER, selectedASRProviderId?.toString()),
        Pair(MODE_INJECTIONS, JsonInstant.encodeToString(modeInjections)),
        Pair(LOREBOOKS, JsonInstant.encodeToString(lorebooks)),
        Pair(QUICK_MESSAGES, JsonInstant.encodeToString(quickMessages)),
        Pair(WEB_SERVER_ENABLED, webServerEnabled),
        Pair(WEB_SERVER_PORT, webServerPort),
        Pair(WEB_SERVER_JWT_ENABLED, webServerJwtEnabled),
        Pair(WEB_SERVER_ACCESS_PASSWORD, webServerAccessPassword),
        Pair(WEB_SERVER_LOCALHOST_ONLY, webServerLocalhostOnly),
        Pair(BACKUP_REMINDER_CONFIG, JsonInstant.encodeToString(backupReminderConfig)),
        Pair(LAUNCH_COUNT, launchCount),
        Pair(SPONSOR_ALERT_DISMISSED_AT, sponsorAlertDismissedAt),
        Pair(SYSTEM_TOOLS_SETTING, JsonInstant.encodeToString(systemToolsSetting)),
        Pair(PROACTIVE_MESSAGE_SETTING, JsonInstant.encodeToString(proactiveMessageSetting)),
        Pair(WECHAT_BOT_SETTING, JsonInstant.encodeToString(wechatBotSetting)),
        Pair(QQ_BOT_SETTING, JsonInstant.encodeToString(qqBotSetting)),
        Pair(KEEP_ALIVE_ENABLED, keepAliveEnabled),
        Pair(EXTERNAL_MEMORIES, JsonInstant.encodeToString(externalMemories)),
        Pair(MINI_APPS, JsonInstant.encodeToString(miniApps)),
        Pair(FORCE_CONFIRM_TOOL_CALLS, forceConfirmToolCalls),
        Pair(WORKFLOW_HEADLESS_BLOCK_SENSITIVE, workflowHeadlessBlockSensitive),
        Pair(AUTO_APPROVE_ALL_TOOLS, autoApproveAllTools),
    )

    // 首写整份落盘；之后只写与上次落盘相比变化的字段。
    private fun persist(settings: Settings, preferences: MutablePreferences) {
        val current = settings.toPreferencePairs()
        val previous = lastPersisted
        if (previous == null) {
            for ((key, value) in current) write(preferences, key, value)
        } else {
            val previousByKey = previous.associateBy { it.first }
            for ((key, value) in current) {
                if (previousByKey[key]?.second != value) write(preferences, key, value)
            }
        }
        lastPersisted = current
    }

    // 只被常驻写盘协程调用，无需加锁。
    @Volatile
    private var lastPersisted: List<Pair<Preferences.Key<*>, Any?>>? = null

    private fun write(preferences: MutablePreferences, key: Preferences.Key<*>, value: Any?) {
        when (value) {
            null -> preferences.remove(key)
            is Boolean -> preferences[key as Preferences.Key<Boolean>] = value
            is Int -> preferences[key as Preferences.Key<Int>] = value
            is String -> preferences[key as Preferences.Key<String>] = value
            else -> error("Unsupported settings value type: ${value::class}")
        }
    }

    suspend fun update(settings: Settings) = updateMutex.withLock {
        applyUpdate(settings)
    }

    suspend fun update(fn: (Settings) -> Settings) = updateMutex.withLock {
        var current = settingsFlow.value
        // 冷启动设置尚未加载完成时，等待真实设置后再应用，不再作用在 dummy 上被直接丢弃
        if (current.init && !settingsLoadFailed) awaitLoaded()
        applyUpdate(fn(settingsFlow.value))
    }

    suspend fun updateAssistant(assistantId: Uuid) {
        // 并入统一写入路径：先改内存再落盘，不再绕过内存直接编辑 DataStore
        update { settings ->
            settings.copy(assistantId = assistantId)
        }
    }

    suspend fun updateAssistantModel(assistantId: Uuid, modelId: Uuid) {
        update { settings ->
            settings.copy(
                assistants = settings.assistants.map { assistant ->
                    if (assistant.id == assistantId) {
                        assistant.copy(chatModelId = modelId)
                    } else {
                        assistant
                    }
                }
            )
        }
    }

    suspend fun updateAssistantReasoningLevel(assistantId: Uuid, reasoningLevel: ReasoningLevel) {
        update { settings ->
            settings.copy(
                assistants = settings.assistants.map { assistant ->
                    if (assistant.id == assistantId) {
                        assistant.copy(reasoningLevel = reasoningLevel)
                    } else {
                        assistant
                    }
                }
            )
        }
    }

    suspend fun updateAssistantMcpServers(assistantId: Uuid, mcpServers: Set<Uuid>) {
        update { settings ->
            settings.copy(
                assistants = settings.assistants.map { assistant ->
                    if (assistant.id == assistantId) {
                        assistant.copy(mcpServers = mcpServers)
                    } else {
                        assistant
                    }
                }
            )
        }
    }

    suspend fun updateAssistantInjections(
        assistantId: Uuid,
        modeInjectionIds: Set<Uuid>,
        lorebookIds: Set<Uuid>,
        quickMessageIds: Set<Uuid> = emptySet(),
    ) {
        update { settings ->
            settings.copy(
                assistants = settings.assistants.map { assistant ->
                    if (assistant.id == assistantId) {
                        assistant.copy(
                            modeInjectionIds = modeInjectionIds,
                            lorebookIds = lorebookIds,
                            quickMessageIds = quickMessageIds,
                        )
                    } else {
                        assistant
                    }
                }
            )
        }
    }
}

@Serializable
data class Settings(
    @Transient
    val init: Boolean = false,
    val disclaimerAccepted: Boolean = false,
    val disclaimerAcceptedAt: Int = 0,
    val dynamicColor: Boolean = true,
    val themeId: String = PresetThemes[0].id,
    val customThemes: List<CustomTheme> = emptyList(),
    val developerMode: Boolean = false,
    val displaySetting: DisplaySetting = DisplaySetting(),
    val enableWebSearch: Boolean = false,
    val favoriteModels: List<Uuid> = emptyList(),
    val chatModelId: Uuid = Uuid.random(),
    val titleModelId: Uuid = Uuid.random(),
    val imageGenerationModelId: Uuid = Uuid.random(),
    val titlePrompt: String = DEFAULT_TITLE_PROMPT,
    val translateModeId: Uuid = Uuid.random(),
    val translatePrompt: String = DEFAULT_TRANSLATION_PROMPT,
    val translateThinkingBudget: Int = 0,
    val suggestionModelId: Uuid = Uuid.random(),
    val suggestionPrompt: String = DEFAULT_SUGGESTION_PROMPT,
    val ocrModelId: Uuid = Uuid.random(),
    val ocrPrompt: String = DEFAULT_OCR_PROMPT,
    val compressModelId: Uuid = Uuid.random(),
    val compressPrompt: String = DEFAULT_COMPRESS_PROMPT,
    val assistantId: Uuid = DEFAULT_ASSISTANT_ID,
    val providers: List<ProviderSetting> = DEFAULT_PROVIDERS,
    val assistants: List<Assistant> = DEFAULT_ASSISTANTS,
    val assistantTags: List<Tag> = emptyList(),
    val searchServices: List<SearchServiceOptions> = listOf(SearchServiceOptions.DEFAULT),
    val searchCommonOptions: SearchCommonOptions = SearchCommonOptions(),
    val searchServiceSelected: Int = 0,
    val mcpServers: List<McpServerConfig> = emptyList(),
    val webDavConfig: WebDavConfig = WebDavConfig(),
    val s3Config: S3Config = S3Config(),
    val ttsProviders: List<TTSProviderSetting> = DEFAULT_TTS_PROVIDERS,
    val selectedTTSProviderId: Uuid = DEFAULT_SYSTEM_TTS_ID,
    val asrProviders: List<ASRProviderSetting> = emptyList(),
    val selectedASRProviderId: Uuid? = null,
    val modeInjections: List<PromptInjection.ModeInjection> = DEFAULT_MODE_INJECTIONS,
    val lorebooks: List<Lorebook> = emptyList(),
    val quickMessages: List<QuickMessage> = emptyList(),
    val webServerEnabled: Boolean = false,
    val webServerPort: Int = 8080,
    val webServerJwtEnabled: Boolean = false,
    val webServerAccessPassword: String = "",
    val webServerLocalhostOnly: Boolean = false,
    val backupReminderConfig: BackupReminderConfig = BackupReminderConfig(),
    val launchCount: Int = 0,
    val sponsorAlertDismissedAt: Int = 0,
    val systemToolsSetting: SystemToolsSetting = SystemToolsSetting(),
    val proactiveMessageSetting: ProactiveMessageSetting = ProactiveMessageSetting(),
    val wechatBotSetting: WechatBotSetting = WechatBotSetting(),
    val qqBotSetting: QqBotSetting = QqBotSetting(),
    val keepAliveEnabled: Boolean = false,
    val externalMemories: List<ExternalMemory> = emptyList(),
    val miniApps: List<MiniApp> = emptyList(),
    val forceConfirmToolCalls: Boolean = true,
    val workflowHeadlessBlockSensitive: Boolean = true,
    val autoApproveAllTools: Boolean = false,
) {
    companion object {
        // 构造一个用于初始化的settings, 但它不能用于保存，防止使用初始值存储
        fun dummy() = Settings(init = true)
    }
}

@Serializable
enum class ChatFontFamily {
    @SerialName("default")
    DEFAULT,
    @SerialName("serif")
    SERIF,
    @SerialName("monospace")
    MONOSPACE,
    @SerialName("custom")
    CUSTOM,
}

@Serializable
enum class DisplayMaterialMode {
    @SerialName("follow_theme")
    FOLLOW_THEME,
    @SerialName("flat")
    FLAT,
    @SerialName("translucent")
    TRANSLUCENT,
    @SerialName("glass")
    GLASS,
}

/**
 * 消息排版模式。
 * HEADER: 头像在消息上方的署名行里（原有版式）。
 * SIDE:   头像贴在气泡侧边——对方在左、自己在右，署名行不再占一行。
 */
@Serializable
enum class ChatAvatarMode {
    @SerialName("header")
    HEADER,
    @SerialName("side")
    SIDE,
}

@Serializable
data class DisplaySetting(
    val userAvatar: Avatar = Avatar.Dummy,
    val userNickname: String = "",
    val useAppIconStyleLoadingIndicator: Boolean = true,
    val showUserAvatar: Boolean = true,
    /** 消息排版模式：署名行头像 / 气泡侧边头像。默认走气泡侧边（QQ 式并排）。 */
    val chatAvatarMode: ChatAvatarMode = ChatAvatarMode.SIDE,
    /** 侧边头像模式下，在聊天页顶栏显示「对方 + 自己」的叠压双头像。 */
    val showTopBarDualAvatar: Boolean = true,
    val showAssistantBubble: Boolean = false,
    /** 用户消息是否显示气泡背景；关闭后会真正绕开 BubbleSurface。 */
    val showUserBubble: Boolean = true,
    val showModelIcon: Boolean = true,
    val showModelName: Boolean = true,
    val showDateBelowName: Boolean = false,
    val showTokenUsage: Boolean = true,
    val showThinkingContent: Boolean = true,
    val autoCloseThinking: Boolean = true,
    val showUpdates: Boolean = true,
    val showMessageJumper: Boolean = true,
    val messageJumperOnLeft: Boolean = false,
    val fontSizeRatio: Float = 1.0f,
    val enableMessageGenerationHapticEffect: Boolean = false,
    val skipCropImage: Boolean = false,
    val enableNotificationOnMessageGeneration: Boolean = false,
    val enableLiveUpdateNotification: Boolean = false,
    val codeBlockAutoWrap: Boolean = false,
    val codeBlockAutoCollapse: Boolean = false,
    val showLineNumbers: Boolean = false,
    val ttsOnlyReadQuoted: Boolean = false,
    /**
     * 只朗读文本里的英文部分，中文/日文等其它文字跳过。
     * 适合用英文音色听发音练习：中英混排时不会被中文读音打断。
     */
    val ttsEnglishOnly: Boolean = false,
    val autoPlayTTSAfterGeneration: Boolean = false,
    /**
     * 生成结束后自动把助手回复合成一条语音条插进消息里（可回放、随会话持久化）。
     * 与 autoPlayTTSAfterGeneration 的区别：那个只是念一遍，念完什么都不留。
     * 每条回复都要走一次 TTS 合成，按量计费的服务商会真扣钱，所以默认关。
     */
    val autoVoiceMessageAfterGeneration: Boolean = false,
    /**
     * 有语音条的助手消息是否仍显示文字。
     * 手动生成语音条时把文字收起是合理的（自己按的，知道内容）；
     * 但自动生成开着时每条都收起会导致整个会话只剩语音条、没法回看文字，所以默认显示。
     */
    val showTextWithVoiceMessage: Boolean = true,
    val pasteLongTextAsFile: Boolean = false,
    val pasteLongTextThreshold: Int = 1000,
    val sendOnEnter: Boolean = false,
    val enableAutoScroll: Boolean = true,
    val enableLatexRendering: Boolean = true,
    val enableBlurEffect: Boolean = false,
    val chatFontFamily: ChatFontFamily = ChatFontFamily.DEFAULT,
    val materialMode: DisplayMaterialMode = DisplayMaterialMode.FOLLOW_THEME,
    // null preserves the global material for existing configurations.
    val drawerMaterialMode: DisplayMaterialMode? = null,
    val interfaceSurfaceOpacity: Float = 82f,
    val popupSurfaceOpacity: Float = 90f,
    val enableVolumeKeyScroll: Boolean = false,
    val volumeKeyScrollRatio: Float = 1.0f,
    val chatBubbleTransparency: Float = 0f,
    val thinkingChainTransparency: Float = 0f,
    // 自定义字体
    val customFontPath: String = "",
    // 设置页自定义背景
    val settingsBackgroundPath: String = "",
    // 输入框自定义背景
    val inputBackgroundPath: String = "",
    // 头像框（QQ挂件风格）
    val userAvatarFramePath: String = "",
    val aiAvatarFramePath: String = "",
    val userAvatarFrameOffsetX: Float = 0f,
    val userAvatarFrameOffsetY: Float = 0f,
    val userAvatarFrameScale: Float = 1f,
    val aiAvatarFrameOffsetX: Float = 0f,
    val aiAvatarFrameOffsetY: Float = 0f,
    val aiAvatarFrameScale: Float = 1f,
    // 侧边栏背景
    val drawerBackgroundPath: String = "",
    // 侧边栏根面板不透明度（百分比）
    val drawerSurfaceOpacity: Float = 100f,
    // 侧边栏元素透明度
    val drawerItemAlpha: Float = 1f,
    // 颜色自定义
    val chatTextColor: Long? = null,
    val globalTextColor: Long? = null,
    val userBubbleColor: Long? = null,
    val assistantBubbleColor: Long? = null,
    val thinkingFontSizeRatio: Float = 1.0f,
    val thinkingBubbleColor: Long? = null,
    val chatBackgroundColor: Long? = null,
    val primaryColor: Long? = null,
    val inputFieldColor: Long? = null,
    // 气泡背景图 & 圆角
    val userBubbleImagePath: String = "",
    val assistantBubbleImagePath: String = "",
    val bubbleCornerRadius: Float = 16f,
    val bubbleImageOverlayEnabled: Boolean = false, // 关=纯图片, 开=图片+主题色遮罩
    // 发送音效（自定义 mp3 路径，空 = 不播放）
    val sendSoundPath: String = "",
    // 用户资料卡：会注入到 system prompt，让助手知道"你在跟谁说话"
    val userBio: String = "",
    val userPersona: String = "",
    val injectUserProfile: Boolean = true,
)

@Serializable
data class WebDavConfig(
    val url: String = "",
    val username: String = "",
    val password: String = "",
    val path: String = "rikkahub_backups",
    val items: List<BackupItem> = listOf(
        BackupItem.DATABASE,
        BackupItem.FILES
    ),
) {
    @Serializable
    enum class BackupItem {
        DATABASE,
        FILES,
    }
}

@Serializable
data class BackupReminderConfig(
    val enabled: Boolean = false,
    val intervalDays: Int = 7,
    val lastBackupTime: Long = 0L,
)

fun Settings.isNotConfigured() = providers.all { it.models.isEmpty() }

fun Settings.findModelById(uuid: Uuid): Model? {
    return this.providers.findModelById(uuid)
}

fun List<ProviderSetting>.findModelById(uuid: Uuid): Model? {
    this.forEach { setting ->
        setting.models.forEach { model ->
            if (model.id == uuid) {
                return model
            }
        }
    }
    return null
}

fun Settings.getCurrentChatModel(): Model? {
    return findModelById(this.getCurrentAssistant().chatModelId ?: this.chatModelId)
}

fun Settings.getCurrentAssistant(): Assistant {
    return this.assistants.find { it.id == assistantId } ?: this.assistants.first()
}

fun Settings.getAssistantById(id: Uuid): Assistant? {
    return this.assistants.find { it.id == id }
}

fun Settings.getQuickMessagesOfAssistant(assistant: Assistant) =
    quickMessages.filter { it.id in assistant.quickMessageIds }

fun Settings.getSelectedTTSProvider(): TTSProviderSetting? {
    return selectedTTSProviderId?.let { id ->
        ttsProviders.find { it.id == id }
    } ?: ttsProviders.firstOrNull()
}

fun Settings.getSelectedASRProvider(): ASRProviderSetting? {
    return selectedASRProviderId?.let { id ->
        asrProviders.find { it.id == id }
    } ?: asrProviders.firstOrNull()
}

fun Model.findProvider(providers: List<ProviderSetting>, checkOverwrite: Boolean = true): ProviderSetting? {
    val provider = findModelProviderFromList(providers) ?: return null
    val providerOverwrite = this.providerOverwrite
    if (checkOverwrite && providerOverwrite != null) {
        return providerOverwrite.copyProvider(models = emptyList())
    }
    return provider
}

private fun Model.findModelProviderFromList(providers: List<ProviderSetting>): ProviderSetting? {
    providers.forEach { setting ->
        setting.models.forEach { model ->
            if (model.id == this.id) {
                return setting
            }
        }
    }
    return null
}

internal val DEFAULT_ASSISTANT_ID = Uuid.parse("0950e2dc-9bd5-4801-afa3-aa887aa36b4e")
internal val DEFAULT_ASSISTANTS = listOf(
    Assistant(
        id = DEFAULT_ASSISTANT_ID,
        name = "",
        systemPrompt = ""
    ),
    Assistant(
        id = Uuid.parse("3d47790c-c415-4b90-9388-751128adb0a0"),
        name = "",
        systemPrompt = """
            You are a helpful assistant, called {{char}}, based on model {{model_name}}.

            ## Info
            - Time: {{cur_datetime}}
            - Locale: {{locale}}
            - Timezone: {{timezone}}
            - Device Info: {{device_info}}
            - System Version: {{system_version}}
            - User Nickname: {{user}}

            ## Hint
            - If the user does not specify a language, reply in the user's primary language.
            - Remember to use Markdown syntax for formatting, and use latex for mathematical expressions.
        """.trimIndent()
    ),
)

val DEFAULT_SYSTEM_TTS_ID = Uuid.parse("026a01a2-c3a0-4fd5-8075-80e03bdef200")
private val DEFAULT_TTS_PROVIDERS = listOf(
    TTSProviderSetting.SystemTTS(
        id = DEFAULT_SYSTEM_TTS_ID,
        name = "",
    ),
    TTSProviderSetting.OpenAI(
        id = Uuid.parse("e36b22ef-ca82-40ab-9e70-60cad861911c"),
        name = "AiHubMix",
        baseUrl = "https://aihubmix.com/v1",
        model = "gpt-4o-mini-tts",
        voice = "alloy",
    )
)

internal val DEFAULT_ASSISTANTS_IDS = DEFAULT_ASSISTANTS.map { it.id }

val DEFAULT_MODE_INJECTIONS = listOf(
    PromptInjection.ModeInjection(
        id = Uuid.parse("b87eaf16-f5cd-4ac1-9e4f-b11ae3a61d74"),
        content = LEARNING_MODE_PROMPT,
        position = InjectionPosition.AFTER_SYSTEM_PROMPT,
        name = "Learning Mode"
    )
)
