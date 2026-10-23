package me.rerere.rikkahub.ui.pages.chat

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.ui.platform.LocalContext
import me.rerere.rikkahub.data.model.Assistant
import androidx.compose.foundation.layout.Spacer
import me.rerere.ai.provider.ModelAbility
import me.rerere.ai.core.ReasoningLevel
import me.rerere.rikkahub.ui.components.ui.MaterialActionSurface
import me.rerere.rikkahub.data.datastore.DisplayMaterialMode
import me.rerere.rikkahub.ui.theme.LocalMaterialMode
import me.rerere.rikkahub.ui.components.ui.UIAvatar
import me.rerere.hugeicons.stroke.ArrowDown01
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.adaptive.currentWindowDpSize
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dokar.sonner.ToastType
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import me.rerere.ai.provider.Model
import me.rerere.ai.provider.ModelType
import me.rerere.ai.ui.UIMessagePart
import me.rerere.hugeicons.stroke.Earth
import me.rerere.hugeicons.stroke.Tick01
import me.rerere.hugeicons.stroke.Idea01
import me.rerere.hugeicons.stroke.ArrowLeftRight
import me.rerere.hugeicons.stroke.PencilEdit01
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Cancel01
import me.rerere.hugeicons.stroke.LeftToRightListBullet
import me.rerere.hugeicons.stroke.Menu03
import me.rerere.hugeicons.stroke.MoreVertical
import me.rerere.hugeicons.stroke.QuillWrite01
import me.rerere.hugeicons.stroke.Voice
import me.rerere.rikkahub.R
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.data.datastore.ChatAvatarMode
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.getAssistantById
import me.rerere.rikkahub.data.datastore.getCurrentAssistant
import me.rerere.rikkahub.data.datastore.getCurrentChatModel
import me.rerere.rikkahub.data.files.FilesManager
import me.rerere.rikkahub.data.model.Conversation
import me.rerere.rikkahub.service.ChatError
import me.rerere.rikkahub.service.VoiceCallService
import me.rerere.rikkahub.ui.components.ai.ChatInput
import me.rerere.rikkahub.ui.components.ai.ModelSelectorSheet
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.ui.context.Navigator
import me.rerere.rikkahub.ui.hooks.ChatInputState
import me.rerere.rikkahub.ui.hooks.EditStateContent
import me.rerere.rikkahub.ui.hooks.useEditState
import me.rerere.rikkahub.utils.base64Decode
import me.rerere.rikkahub.utils.navigateToChatPage
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf
import kotlin.uuid.Uuid

@Composable
fun ChatPage(id: Uuid, text: String?, files: List<Uri>, nodeId: Uuid? = null, autoStartVoice: Boolean = false) {
    val vm: ChatVM = koinViewModel(
        parameters = {
            parametersOf(id.toString())
        }
    )
    val filesManager: FilesManager = koinInject()
    val navController = LocalNavController.current
    val scope = rememberCoroutineScope()

    val setting by vm.settings.collectAsStateWithLifecycle()
    val conversation by vm.conversation.collectAsStateWithLifecycle()
    val loadingJob by vm.conversationJob.collectAsStateWithLifecycle()
    val processingStatus by vm.processingStatus.collectAsStateWithLifecycle()
    val currentChatModel by vm.currentChatModel.collectAsStateWithLifecycle()
    val enableWebSearch by vm.enableWebSearch.collectAsStateWithLifecycle()
    val errors by vm.errors.collectAsStateWithLifecycle()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val hazeState = rememberHazeState()
    val softwareKeyboardController = LocalSoftwareKeyboardController.current

    // Handle back press when drawer is open
    BackHandler(enabled = drawerState.isOpen) {
        scope.launch {
            drawerState.close()
        }
    }

    // Hide keyboard when drawer is open
    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) {
            softwareKeyboardController?.hide()
        }
    }

    val windowAdaptiveInfo = currentWindowDpSize()
    val isBigScreen =
        windowAdaptiveInfo.width > windowAdaptiveInfo.height && windowAdaptiveInfo.width >= 1100.dp

    val inputState = vm.inputState

    // 初始化输入状态（处理传入的 files 和 text 参数）
    LaunchedEffect(files, text) {
        if (files.isNotEmpty()) {
            val localFiles = filesManager.createChatFilesByContents(files)
            val contentTypes = files.mapNotNull { file ->
                filesManager.getFileMimeType(file)
            }
            val parts = buildList {
                localFiles.forEachIndexed { index, file ->
                    val type = contentTypes.getOrNull(index)
                    if (type?.startsWith("image/") == true) {
                        add(UIMessagePart.Image(url = file.toString()))
                    } else if (type?.startsWith("video/") == true) {
                        add(UIMessagePart.Video(url = file.toString()))
                    } else if (type?.startsWith("audio/") == true) {
                        add(UIMessagePart.Audio(url = file.toString()))
                    }
                }
            }
            inputState.messageContent = parts
        }
        text?.base64Decode()?.let { decodedText ->
            if (decodedText.isNotEmpty()) {
                inputState.setMessageText(decodedText)
            }
        }
    }

    val chatListState = rememberLazyListState()
    LaunchedEffect(nodeId, conversation.messageNodes.size) {
        if (!vm.chatListInitialized && conversation.messageNodes.isNotEmpty()) {
            if (nodeId != null) {
                val index = conversation.messageNodes.indexOfFirst { it.id == nodeId }
                if (index >= 0) {
                    chatListState.scrollToItem(index)
                }
            } else {
                chatListState.requestScrollToItem(conversation.currentMessages.size + 5)
            }
            vm.chatListInitialized = true
        }
    }

    when {
        isBigScreen -> {
            PermanentNavigationDrawer(
                drawerContent = {
                    ChatDrawerContent(
                        navController = navController,
                        current = conversation,
                        vm = vm,
                        settings = setting,
                    )
                }
            ) {
                ChatPageContent(
                    inputState = inputState,
                    loadingJob = loadingJob,
                    processingStatus = processingStatus,
                    setting = setting,
                    conversation = conversation,
                    drawerState = drawerState,
                    navController = navController,
                    vm = vm,
                    chatListState = chatListState,
                    enableWebSearch = enableWebSearch,
                    currentChatModel = currentChatModel,
                    bigScreen = true,
                    autoStartVoice = autoStartVoice,
                    hazeState = hazeState,
                    errors = errors,
                    onDismissError = { vm.dismissError(it) },
                    onClearAllErrors = { vm.clearAllErrors() },
                )
            }
        }

        else -> {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ChatDrawerContent(
                        navController = navController,
                        current = conversation,
                        vm = vm,
                        settings = setting,
                    )
                }
            ) {
                ChatPageContent(
                    inputState = inputState,
                    loadingJob = loadingJob,
                    processingStatus = processingStatus,
                    setting = setting,
                    conversation = conversation,
                    drawerState = drawerState,
                    navController = navController,
                    vm = vm,
                    chatListState = chatListState,
                    enableWebSearch = enableWebSearch,
                    currentChatModel = currentChatModel,
                    bigScreen = false,
                    autoStartVoice = autoStartVoice,
                    hazeState = hazeState,
                    errors = errors,
                    onDismissError = { vm.dismissError(it) },
                    onClearAllErrors = { vm.clearAllErrors() },
                )
            }
            BackHandler(drawerState.isOpen) {
                scope.launch { drawerState.close() }
            }
        }
    }
}

@Composable
private fun ChatPageContent(
    modifier: Modifier = Modifier,
    inputState: ChatInputState,
    loadingJob: Job?,
    processingStatus: String? = null,
    setting: Settings,
    bigScreen: Boolean,
    conversation: Conversation,
    drawerState: DrawerState,
    navController: Navigator,
    vm: ChatVM,
    chatListState: LazyListState,
    enableWebSearch: Boolean,
    currentChatModel: Model?,
    autoStartVoice: Boolean = false,
    hazeState: HazeState,
    errors: List<ChatError>,
    onDismissError: (Uuid) -> Unit,
    onClearAllErrors: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    // 会话内助手配置写回: 按助手 id 更新 settings.assistants 并落盘 (思考强度等),
    // ChatBottomAssistantBar 与 ChatInput 共用同一来源。
    val onUpdateAssistant: (Assistant) -> Unit = { updatedAssistant ->
        vm.updateSettings(
            setting.copy(
                assistants = setting.assistants.map { a ->
                    if (a.id == updatedAssistant.id) updatedAssistant else a
                }
            )
        )
    }
    val toaster = LocalToaster.current
    var previewMode by rememberSaveable { mutableStateOf(false) }

    TTSAutoPlay(vm = vm, setting = setting, conversation = conversation)

    val closeoutDoneText = stringResource(R.string.chat_page_closeout_done)
    val closeoutEmptyText = stringResource(R.string.chat_page_closeout_empty)
    // 共享聊天背景 Painter（仅图片背景时非空；与 AssistantBackground 共用同一实例，不重复加载）
    val chatBackgroundPainter = rememberChatBackgroundPainter(setting)

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize(),
        ) {
        AssistantBackground(setting = setting, backgroundPainter = chatBackgroundPainter)
        Scaffold(
            topBar = {
                TopBar(
                    settings = setting,
                    conversation = conversation,
                    bigScreen = bigScreen,
                    drawerState = drawerState,
                    previewMode = previewMode,
                    onCloseout = {
                        vm.closeoutConversation { archived ->
                            toaster.show(
                                if (archived) closeoutDoneText else closeoutEmptyText,
                                type = if (archived) ToastType.Success else ToastType.Info,
                            )
                            navigateToChatPage(navController)
                        }
                    },
                    onClickMenu = {
                        previewMode = !previewMode
                    },
                    onUpdateTitle = {
                        vm.updateTitle(it)
                    },
                    onVoiceCall = {
                        val activeId = VoiceCallService.activeConversationId.value
                        when {
                            activeId == null -> navController.navigate(
                                Screen.VoiceCall(conversation.id.toString())
                            )
                            activeId == conversation.id.toString() -> navController.navigate(
                                Screen.VoiceCall(conversation.id.toString())
                            )
                            else -> {
                                toaster.show("当前有通话进行中，请先挂断", type = ToastType.Warning)
                            }
                        }
                    },
                    onUpdateChatModel = {
                        vm.setChatModel(assistant = setting.getCurrentAssistant(), model = it)
                    },
                )
            },
            bottomBar = {
                Column {
                    // 输入框上方: 会话助手选择入口 + 按需状态标记 (联网/Thinking)。
                    // 只显示**这个会话绑定的助手**, 不与全局默认助手混淆;
                    // 点击弹助手底部弹窗, 选中的写 conversation.assistantId。
                    ChatBottomAssistantBar(
                        settings = setting,
                        conversation = conversation,
                        onSwitchAssistant = { assistant ->
                            vm.switchConversationAssistant(conversation, assistant.id)
                        },
                        onUpdateAssistant = onUpdateAssistant,
                        modifier = Modifier.padding(horizontal = 12.dp),
                    )
                    // 助手选择胶囊与输入框拉开一档距离（+8dp，真机视觉再微调）。
                    Spacer(Modifier.height(8.dp))
                    ChatInput(
                        state = inputState,
                    loading = loadingJob != null,
                    settings = setting,
                    conversation = conversation,
                    mcpManager = vm.mcpManager,
                    hazeState = hazeState,
                    autoStartVoice = autoStartVoice,
                    onCancelClick = {
                        vm.stopGeneration()
                    },
                    enableSearch = enableWebSearch,
                    onToggleSearch = {
                        vm.updateSettings(setting.copy(enableWebSearch = !enableWebSearch))
                    },
                    reasoningLevel = setting.getCurrentAssistant().reasoningLevel,
                    onUpdateReasoningLevel = { level ->
                        onUpdateAssistant(setting.getCurrentAssistant().copy(reasoningLevel = level))
                    },
                    showReasoningButton = currentChatModel?.abilities?.contains(ModelAbility.REASONING) == true,
                    onSendClick = {
                        if (currentChatModel == null) {
                            toaster.show("请先选择模型", type = ToastType.Error)
                            return@ChatInput
                        }
                        if (inputState.isEditing()) {
                            vm.handleMessageEdit(
                                parts = inputState.getContents(),
                                messageId = inputState.editingMessage!!,
                            )
                        } else {
                            vm.handleMessageSend(inputState.getContents())
                            scope.launch {
                                chatListState.requestScrollToItem(conversation.currentMessages.size + 5)
                            }
                            val sendSound = setting.displaySetting.sendSoundPath
                            if (sendSound.isNotBlank() && java.io.File(sendSound).exists()) {
                                runCatching {
                                    android.media.MediaPlayer().apply {
                                        setDataSource(sendSound)
                                        setOnPreparedListener { it.start() }
                                        setOnCompletionListener { it.release() }
                                        setOnErrorListener { mp, _, _ -> mp.release(); true }
                                        prepareAsync()
                                    }
                                }
                            }
                        }
                        inputState.clearInput()
                    },
                    onVoiceMessage = { url, duration, transcript ->
                        if (currentChatModel == null) {
                            toaster.show("请先选择模型", type = ToastType.Error)
                            return@ChatInput
                        }
                        vm.handleMessageSend(
                            listOf(
                                UIMessagePart.VoiceMessage(
                                    url = url,
                                    duration = duration,
                                    transcript = transcript,
                                )
                            )
                        )
                        scope.launch {
                            chatListState.requestScrollToItem(conversation.currentMessages.size + 5)
                        }
                    },
                    onLongSendClick = {
                        if (inputState.isEditing()) {
                            vm.handleMessageEdit(
                                parts = inputState.getContents(),
                                messageId = inputState.editingMessage!!,
                            )
                        } else {
                            vm.handleMessageSend(content = inputState.getContents(), answer = false)
                            scope.launch {
                                chatListState.requestScrollToItem(conversation.currentMessages.size + 5)
                            }
                        }
                        inputState.clearInput()
                    },
                    onUpdateAssistant = onUpdateAssistant,
                    onUpdateSearchService = { index ->
                        vm.updateSettings(
                            setting.copy(
                                searchServiceSelected = index
                            )
                        )
                    },
                    onCompressContext = { additionalPrompt, targetTokens, keepRecentMessages ->
                        vm.handleCompressContext(additionalPrompt, targetTokens, keepRecentMessages)
                    },
                )
            }
        },
            containerColor = Color.Transparent,
        ) { innerPadding ->
            ChatList(
                innerPadding = innerPadding,
                conversation = conversation,
                state = chatListState,
                loading = loadingJob != null,
                processingStatus = processingStatus,
                previewMode = previewMode,
                settings = setting,
                hazeState = hazeState,
                errors = errors,
                onDismissError = onDismissError,
                onClearAllErrors = onClearAllErrors,
                onRegenerate = {
                    vm.regenerateAtMessage(it)
                },
                onEdit = {
                    inputState.editingMessage = it.id
                    inputState.setContents(it.parts)
                },
                onForkMessage = {
                    scope.launch {
                        val fork = vm.forkMessage(message = it)
                        navigateToChatPage(navController, chatId = fork.id)
                    }
                },
                onDelete = {
                    if (loadingJob != null) {
                        vm.showDeleteBlockedWhileGeneratingError()
                    } else {
                        vm.deleteMessage(it)
                    }
                },
                onUpdateMessage = { newNode ->
                    vm.updateConversation(
                        conversation.copy(
                            messageNodes = conversation.messageNodes.map { node ->
                                if (node.id == newNode.id) {
                                    newNode
                                } else {
                                    node
                                }
                            }
                        ))
                    vm.saveConversationAsync()
                },
                onClickSuggestion = { suggestion ->
                    inputState.editingMessage = null
                    inputState.setMessageText(suggestion)
                },
                onTranslate = { message, locale ->
                    vm.translateMessage(message, locale)
                },
                onClearTranslation = { message ->
                    vm.clearTranslationField(message.id)
                },
                onJumpToMessage = { index ->
                    previewMode = false
                    scope.launch {
                        chatListState.animateScrollToItem(index)
                    }
                },
                onToolApproval = { toolCallId, approved, reason ->
                    vm.handleToolApproval(toolCallId, approved, reason)
                },
                onToolAnswer = { toolCallId, answer ->
                    vm.handleToolAnswer(toolCallId, answer)
                },
                onToggleFavorite = { node ->
                    vm.toggleMessageFavorite(node)
                },
                onConversationSystemPromptChange = { newPrompt ->
                    vm.updateConversation(conversation.copy(customSystemPrompt = newPrompt))
                    vm.saveConversationAsync()
                },
            )
        }

        }

    }
}

// 顶栏右侧两个入口（语音 / 更多）共用同一套图标规格。
//
// 之前两个 Icon 都吃各自的隐式默认值：绘制尺寸走 ImageVector 的固有 24dp、着色走
// TopAppBar 的 actionIconContentColor（onSurfaceVariant）。看着「统一」，但 HugeIcons 里
// MoreVertical 是三个实心点、Voice 是细线轮廓，同样 24dp 下三点的墨水覆盖率明显更高，
// 读起来比语音「重」一圈 —— 这就是三点显得突兀的来源。
// 所以把两者都显式收到 22dp：规格从「隐式默认」变成「同一套显式常量」，
// 同时压低顶栏右侧的存在感（用户本轮的主诉求是「上方偏重」）。
private val TopBarActionIconSize = 24.dp

/**
 * 顶栏右侧操作按钮的统一外壳: 圆形 + 细描边 + 半透明 surface 底。
 * 日间/夜间自动跟随主题 (surfaceContainerHighest alpha 0.5 / outlineVariant 描边),
 * 不写死颜色。图标尺寸/着色由 content 决定。
 */
/**
 * 输入框上方的会话助手条:
 * - 左: 当前会话助手 (头像+名+下拉箭头), 点击弹助手选择
 * - 助手来源 = conversation.assistantId (会话级), 不与全局默认混淆
 *
 * 清单 C: 联网/Thinking 已收回输入框内部, 助手条不再携带任何状态按钮。
 */
@Composable
private fun ChatBottomAssistantBar(
    settings: Settings,
    conversation: Conversation,
    onSwitchAssistant: (Assistant) -> Unit,
    onUpdateAssistant: (Assistant) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    // 会话绑定助手 (找不到时回退全局当前助手)
    val currentAssistant = settings.getAssistantById(conversation.assistantId)
        ?: settings.getCurrentAssistant()
    val assistantName = currentAssistant.name.ifBlank {
        stringResource(R.string.assistant_page_default_assistant)
    }
    var showAssistantSheet by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // 助手入口: 与输入框统一轮廓/描边 —— 药丸形 (percent 50)，
        // 描边走 materialModeBorderStroke()（1dp、alpha 0.07，与输入框边框同源）。
        MaterialActionSurface(
            modifier = Modifier
                .clickable { showAssistantSheet = true },
            shape = RoundedCornerShape(percent = 50),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            ) {
            UIAvatar(
                name = assistantName,
                value = currentAssistant.avatar,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = assistantName,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
                Icon(
                    imageVector = HugeIcons.ArrowDown01,
                    contentDescription = "切换助手",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // 清单 C：联网/Thinking 不再放这条助手条上 ——
        // 两者已收回输入框内部 (搜索贴发送侧、思考贴 + 侧)，
        // 助手条只负责「这是谁」的身份标识，输入框上方不再叠一排按钮。
    }

    if (showAssistantSheet) {
        AssistantPickerBottomSheet(
            settings = settings,
            currentAssistantId = currentAssistant.id,
            onSelect = {
                showAssistantSheet = false
                onSwitchAssistant(it)
            },
            onDismiss = { showAssistantSheet = false },
        )
    }
}

/** 按需状态标记: 小圆角 chip, 半透明容器底, 主题色, 点击留给后续选择器。 */
@Composable
private fun AssistStateChip(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    MaterialActionSurface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** 助手选择底部弹窗: 列出全部助手, 选中项打勾, 选择写会话绑定。 */
@Composable
private fun AssistantPickerBottomSheet(
    settings: Settings,
    currentAssistantId: Uuid,
    onSelect: (Assistant) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.7f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "切换助手",
                style = MaterialTheme.typography.titleMedium,
            )
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(settings.assistants, key = { it.id }) { assistant ->
                    val name = assistant.name.ifBlank { stringResource(R.string.assistant_page_default_assistant) }
                    ListItem(
                        headlineContent = { Text(name) },
                        leadingContent = {
                            UIAvatar(name = name, value = assistant.avatar, modifier = Modifier.size(36.dp))
                        },
                        trailingContent = {
                            if (assistant.id == currentAssistantId) {
                                Icon(HugeIcons.Tick01, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        },
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onSelect(assistant) },
                    )
                }
            }
        }
    }
}

@Composable
private fun RoundTonalAction(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // 末轮定稿（规格图）：顶栏三入口统一裸图标。
    // 无底板/无描边/无阴影，IconButton 自带 48dp 热区；
    // 图标 24dp、onSurfaceVariant 着色，与左侧菜单钮视觉重量一致。
    // 功能与排列不变。
    IconButton(onClick = onClick, modifier = modifier.size(48.dp)) {
        Box(modifier = Modifier.size(24.dp), contentAlignment = Alignment.Center) {
            content()
        }
    }
}

@Composable
private fun TopBar(
    settings: Settings,
    conversation: Conversation,
    drawerState: DrawerState,
    bigScreen: Boolean,
    previewMode: Boolean,
    onClickMenu: () -> Unit,
    onCloseout: () -> Unit,
    onUpdateTitle: (String) -> Unit,
    onVoiceCall: () -> Unit,
    onUpdateChatModel: (Model) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val toaster = LocalToaster.current
    // closeout 会写记忆并离开当前会话，属于有副作用的动作，所以先确认一次
    var showCloseoutDialog by remember { mutableStateOf(false) }
    val titleState = useEditState<String> {
        onUpdateTitle(it)
    }
    val editTitleWarning = stringResource(R.string.chat_page_edit_title_warning)
    val editTitleLabel = stringResource(R.string.chat_page_edit_title)
    val openTitleEdit: () -> Unit = {
        if (conversation.messageNodes.isNotEmpty()) {
            titleState.open(conversation.title)
        } else {
            toaster.show(editTitleWarning, type = ToastType.Warning)
        }
    }
    // 次要开关收进溢出菜单，图标行只保留抽屉/语音/更多
    var showOverflowMenu by remember { mutableStateOf(false) }
    // 模型选择弹窗：由顶栏第二行（助手名 · 当前模型）触发
    var showModelPicker by remember { mutableStateOf(false) }

    TopAppBar(
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
        navigationIcon = {
            // 只留侧边栏入口 (菜单)。返回键去掉: 返回 = 关会话/回列表,
            // 系统手势与侧栏都可达, 顶栏不需要第三个出口。
            if (!bigScreen) {
                IconButton(
                    onClick = {
                        scope.launch { drawerState.open() }
                    }
                ) {
                    Icon(HugeIcons.Menu03, "Messages")
                }
            }
            // 顶栏双头像移除: 与侧边栏顶部双头像重复 (本轮改到侧边栏),
            // 顶栏只保留标题与副标题。
        },
        title = {
            // handoff 新要求: 顶栏不显示「栖 · 模型名」也不显示会话标题,
            // 视觉上只有三个按钮 (菜单/语音/更多)。模型切换入更多菜单。
        },
        actions = {
            // 右侧操作: 圆形描边 + 半透明背景 (日/夜自动随主题 surface 系颜色),
            // 图标数量控制为 语音 + 更多 两颗。
            // 切换模型: 顶栏常驻按钮 (handoff 任务4最新节), 打开既有模型选择弹窗。
            // 语音电话移入更多菜单, 顶栏不再常驻语音。
            // 换模型: 双向切换箭头 (规格图), 替代易混的 Package01。
            RoundTonalAction(onClick = { showModelPicker = true }, contentDescription = "切换模型") {
                Icon(
                    imageVector = HugeIcons.ArrowLeftRight,
                    contentDescription = null,
                    modifier = Modifier.size(TopBarActionIconSize),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // 次要开关（如预览模式）收进溢出菜单，保持图标行只有语音/更多。
            // 「新建聊天」不再放在这里：侧栏已经有「新建」，同一个动作不需要两个入口。
            Box {
                RoundTonalAction(onClick = { showOverflowMenu = true }, contentDescription = "更多") {
                    Icon(
                        imageVector = HugeIcons.MoreVertical,
                        contentDescription = null,
                        modifier = Modifier.size(TopBarActionIconSize),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false },
                ) {
                    DropdownMenuItem(
                        text = {
                            Text(if (previewMode) "退出预览模式" else "预览模式")
                        },
                        leadingIcon = {
                            Icon(
                                if (previewMode) HugeIcons.Cancel01 else HugeIcons.LeftToRightListBullet,
                                contentDescription = null,
                            )
                        },
                        onClick = {
                            showOverflowMenu = false
                            onClickMenu()
                        }
                    )
                    // 收尾：总结成记忆后开新会话。空会话没什么可总结的，所以禁用。
                    DropdownMenuItem(
                        text = { Text("语音电话") },
                        leadingIcon = {
                            Icon(HugeIcons.Voice, contentDescription = null)
                        },
                        onClick = {
                            showOverflowMenu = false
                            onVoiceCall()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.chat_page_edit_title)) },
                        leadingIcon = {
                            Icon(HugeIcons.PencilEdit01, contentDescription = null)
                        },
                        onClick = {
                            showOverflowMenu = false
                            openTitleEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.chat_page_closeout)) },
                        leadingIcon = {
                            Icon(HugeIcons.QuillWrite01, contentDescription = null)
                        },
                        enabled = conversation.messageNodes.isNotEmpty(),
                        onClick = {
                            showOverflowMenu = false
                            showCloseoutDialog = true
                        }
                    )
                }
            }
        },
    )
    if (showCloseoutDialog) {
        AlertDialog(
            onDismissRequest = { showCloseoutDialog = false },
            title = { Text(stringResource(R.string.chat_page_closeout)) },
            text = {
                Text(stringResource(R.string.chat_page_closeout_desc))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCloseoutDialog = false
                        onCloseout()
                    }
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloseoutDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }
    if (showModelPicker) {
        ModelSelectorSheet(
            modelId = settings.getCurrentAssistant().chatModelId ?: settings.chatModelId,
            providers = settings.providers,
            type = ModelType.CHAT,
            onSelect = {
                onUpdateChatModel(it)
                showModelPicker = false
            },
            onDismiss = { showModelPicker = false },
        )
    }
    titleState.EditStateContent { title, onUpdate ->
        AlertDialog(
            onDismissRequest = {
                titleState.dismiss()
            },
            title = {
                Text(stringResource(R.string.chat_page_edit_title))
            },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = onUpdate,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        titleState.confirm()
                    }
                ) {
                    Text(stringResource(R.string.chat_page_save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        titleState.dismiss()
                    }
                ) {
                    Text(stringResource(R.string.chat_page_cancel))
                }
            }
        )
    }
}