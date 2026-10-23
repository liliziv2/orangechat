package me.rerere.rikkahub.ui.pages.chat

import androidx.activity.ComponentActivity
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DrawerDefaults
import me.rerere.rikkahub.ui.theme.materialModeBorderStroke
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Book01
import me.rerere.hugeicons.stroke.ChartColumn
import me.rerere.hugeicons.stroke.Delete01
import me.rerere.hugeicons.stroke.Folder01
import me.rerere.hugeicons.stroke.FolderAdd
import me.rerere.hugeicons.stroke.InLove
import me.rerere.hugeicons.stroke.MessageAdd01
import me.rerere.hugeicons.stroke.PencilEdit01
import me.rerere.hugeicons.stroke.Search01
import me.rerere.hugeicons.stroke.Settings03
import me.rerere.hugeicons.stroke.Sun03
import me.rerere.rikkahub.R
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.data.datastore.DisplayMaterialMode
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.getCurrentAssistant
import me.rerere.rikkahub.data.datastore.getCurrentChatModel
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.Avatar
import me.rerere.rikkahub.data.model.Conversation
import me.rerere.ai.core.MessageRole
import me.rerere.rikkahub.data.model.Folder
import me.rerere.rikkahub.data.repository.ConversationRepository
import me.rerere.rikkahub.ui.components.ui.BackupReminderCard
import me.rerere.rikkahub.ui.components.ui.Greeting
import me.rerere.rikkahub.ui.components.ui.UIAvatar
import me.rerere.rikkahub.ui.components.message.ChatMessageSideAvatar
import me.rerere.rikkahub.ui.components.ui.UpdateCard
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.ui.context.Navigator
import com.dokar.sonner.ToastType
import me.rerere.rikkahub.ui.hooks.EditStateContent
import me.rerere.rikkahub.ui.hooks.readBooleanPreference
import me.rerere.rikkahub.ui.hooks.rememberIsPlayStoreVersion
import me.rerere.rikkahub.ui.hooks.useEditState
import me.rerere.rikkahub.ui.modifier.onClick
import me.rerere.rikkahub.utils.navigateToChatPage
import me.rerere.rikkahub.utils.toDp
// 背单词（独立 vocabulary.db）。拆掉这个模块时，把这一行 import + 下面两处
// koinInject/collect + 那行入口 + DrawerVocabularyRow 一起删掉即可。
import me.rerere.rikkahub.vocabulary.data.VocabularyRepository
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject
import kotlin.uuid.Uuid

/** 纪念卡圆角：比 DrawerItemSurface(12dp) 大一档的对话面板圆角。 */
private val MemorialCardShape = RoundedCornerShape(22.dp)

@Composable
fun ChatDrawerContent(
    navController: Navigator,
    vm: ChatVM,
    settings: Settings,
    current: Conversation,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val toaster = LocalToaster.current
    val isPlayStore = rememberIsPlayStoreVersion()
    val repo = koinInject<ConversationRepository>()

    val activity = context as ComponentActivity
    val drawerVm: ChatDrawerVM = koinViewModel(viewModelStoreOwner = activity)

    val conversations = drawerVm.conversations.collectAsLazyPagingItems()
    val folders by drawerVm.folders.collectAsStateWithLifecycle()
    val selectedFolderId by drawerVm.selectedFolderId.collectAsStateWithLifecycle()
    val conversationListState = rememberLazyListState(
        initialFirstVisibleItemIndex = drawerVm.scrollIndex,
        initialFirstVisibleItemScrollOffset = drawerVm.scrollOffset,
    )

    LaunchedEffect(conversationListState) {
        snapshotFlow {
            conversationListState.firstVisibleItemIndex to
                conversationListState.firstVisibleItemScrollOffset
        }
            .distinctUntilChanged()
            .collectLatest { (index, offset) ->
                drawerVm.saveScrollPosition(index, offset)
            }
    }

    val conversationJobs by vm.conversationJobs.collectAsStateWithLifecycle(
        initialValue = emptyMap(),
    )

    // 背单词入口那行的状态。⚠️ 这里**不能**挂定时轮询 —— drawerContent 只是滑出屏幕、
    // 没退出 composition，轮询会一直空转。所以只订两个 Flow（见 VocabularyRepository.dueCount）。
    val vocabularyRepo = koinInject<VocabularyRepository>()
    val vocabularyBooks by vocabularyRepo.books.collectAsStateWithLifecycle(initialValue = emptyList())
    val vocabularyDue by vocabularyRepo.dueCount.collectAsStateWithLifecycle(initialValue = 0)

    // 昵称编辑状态
    val nicknameEditState = useEditState<String> { newNickname ->
        vm.updateSettings(
            settings.copy(
                displaySetting = settings.displaySetting.copy(
                    userNickname = newNickname
                )
            )
        )
    }

    // 移动对话状态
    var showMoveToAssistantSheet by remember { mutableStateOf(false) }
    var conversationToMove by remember { mutableStateOf<Conversation?>(null) }
    val bottomSheetState = rememberModalBottomSheetState()

    // 文件夹相关状态
    var showMoveToFolderSheet by remember { mutableStateOf(false) }
    var conversationToMoveFolder by remember { mutableStateOf<Conversation?>(null) }
    val folderSheetState = rememberModalBottomSheetState()
    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderToRename by remember { mutableStateOf<Folder?>(null) }
    var folderToDelete by remember { mutableStateOf<Folder?>(null) }

    val drawerSurfaceAlpha =
        (settings.displaySetting.drawerSurfaceOpacity / 100f).coerceIn(0.6f, 1f)
    val drawerShape = DrawerDefaults.shape
    val showDrawerBorder = when (settings.displaySetting.materialMode) {
        DisplayMaterialMode.TRANSLUCENT,
        DisplayMaterialMode.GLASS -> true

        DisplayMaterialMode.FLAT,
        DisplayMaterialMode.FOLLOW_THEME -> false
    }
    val drawerModifier = Modifier
        .width(300.dp)
        .then(
            if (showDrawerBorder) {
                Modifier.border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
                    shape = drawerShape,
                )
            } else {
                Modifier
            }
        )

    ModalDrawerSheet(
        modifier = drawerModifier,
        drawerShape = drawerShape,
        drawerContainerColor =
            // 侧栏与其余表面统一：把用户那个滑块当**倍率**乘在主题已合成的
            // surfaceContainer alpha 上，不再用 1.0 把它覆盖掉。
            //
            // 原来这里是 `.copy(alpha = drawerSurfaceAlpha)`，而 drawerSurfaceOpacity
            // 默认 100f ⇒ alpha 恒为 1.0，把 Theme.kt 合成的 82% 顶掉 ⇒ 侧栏成了全 App
            // 唯一一个不透明的表面（其余容器都读同一份 82% 的 surfaceContainer），
            // 观感上比别的面板重一档。倍率化之后 100% = 与其余表面同色同透明，往下调更透。
            //
            // ⚠️ 不改 PreferencesStore 的默认值 100f → 82f：Json.kt 的 JsonInstant 是
            // `encodeDefaults = true`，已装机用户的 preferences[DISPLAY_SETTING] 里一定存着
            // `"drawerSurfaceOpacity":100.0`，改默认值对他们完全无效。只有在合成层动手才
            // 真的改变行为。
            MaterialTheme.colorScheme.surfaceContainer.copy(
                alpha = MaterialTheme.colorScheme.surfaceContainer.alpha * drawerSurfaceAlpha
            ),
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // 侧边栏背景图（最底层）
            val drawerBgPath = settings.displaySetting.drawerBackgroundPath
            if (drawerBgPath.isNotEmpty()) {
                val bgFile = java.io.File(drawerBgPath)
                if (bgFile.exists()) {
                    val bgBitmap = remember(drawerBgPath) {
                        BitmapFactory.decodeFile(drawerBgPath)
                    }
                    if (bgBitmap != null) {
                        Image(
                            bitmap = bgBitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(0.15f),
                            contentScale = ContentScale.Crop,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
            if (settings.displaySetting.showUpdates && !isPlayStore) {
                UpdateCard(vm)
            }

            BackupReminderCard(
                settings = settings,
                onClick = { navController.navigate(Screen.Backup) },
            )

            // ============ 纪念卡（侧栏顶部唯一身份区）============
            // 只重做这一块：柔和水纹底 + 半透明面板 + 细边框圆角。
            // 材质完全跟主题（不新增配色体系），卡片内部收拢：
            // 双头像(渺渺左/栖右) + 装饰数字 + 动态问候 + 右上设置 + 卡片内搜索/背单词。
            // 卡片以下（分隔线/文件夹/会话列表/底部栏）保持回退版本原样。
            // 相识天数：侧栏最早一条对话日期 -> 今天，自动抓取、随数据变化。
            val memorialDays by remember {
                repo.getDaysSinceFirstConversation()
            }.collectAsStateWithLifecycle(initialValue = null)

            DrawerMemorialCard(
                settings = settings,
                daysCount = memorialDays,
                vocabularyBookCount = vocabularyBooks.size,
                vocabularyDueCount = vocabularyDue,
                onOpenSettings = { navController.navigate(Screen.Setting) },
                onAssistantClick = { navController.navigate(Screen.AssistantDetail(settings.assistantId.toString())) },
                onSearch = { navController.navigate(Screen.MessageSearch) },
                onVocabulary = { navController.navigate(Screen.VocabularyList) },
                onUpdateUserAvatar = { avatar ->
                    vm.updateSettings(
                        settings.copy(
                            displaySetting = settings.displaySetting.copy(userAvatar = avatar)
                        )
                    )
                },
            )
            // 分隔线            // 分隔线：把顶部的「纪念卡」和下面的「会话列表」分成两层。
            //
            // 侧栏已收成纯聊天导航：功能目录整块移除，只留「搜索聊天 + 会话列表 +
            // 新建 + 收藏 + 统计」。被移走的功能改由设置页进入，页面本身没有动：
            //   工作区   -> 设置 → 扩展 → 技能与扩展
            //   插件     -> 设置 → 扩展 → 插件管理
            //   主动消息 -> 设置 → 通用 → 主动消息
            // 图像生成 / AI 翻译 / Mini Apps / 记忆库 不再出现在侧栏。
            DrawerDivider()

            FolderBar(
                folders = folders,
                selectedFolderId = selectedFolderId,
                onSelect = { drawerVm.selectFolder(it) },
                onCreate = { showCreateFolderDialog = true },
                onRename = { folderToRename = it },
                onDelete = { folderToDelete = it },
            )

            ConversationList(
                current = current,
                conversations = conversations,
                conversationJobs = conversationJobs.keys,
                listState = conversationListState,
                drawerItemAlpha = settings.displaySetting.drawerItemAlpha,
                materialMode = settings.displaySetting.materialMode,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                onClick = {
                    navigateToChatPage(navController, it.id)
                },
                onRegenerateTitle = {
                    vm.generateTitle(it, true)
                },
                onDelete = {
                    vm.deleteConversation(it)
                    // Refresh the conversation list to immediately remove the deleted item
                    // This fixes the issue where deleted conversations sometimes remain visible
                    // until manually clicked (issue #747)
                    conversations.refresh()
                    if (it.id == current.id) {
                        navigateToChatPage(navController)
                    }
                },
                onPin = {
                    vm.updatePinnedStatus(it)
                },
                onMoveToAssistant = {
                    conversationToMove = it
                    showMoveToAssistantSheet = true
                },
                onMoveToFolder = {
                    conversationToMoveFolder = it
                    showMoveToFolderSheet = true
                }
            )

            // 底部：一个「新建聊天」+ 几个只留图标的常驻入口。
            //
            // 原来这里是一整块带描边的圆角栏，里面五个等宽的「图标 + 文字」按钮；
            // 它和上面那些带底色的条目上下叠在一起，正是侧栏读起来拥挤的主要来源。
            // 现在只留一个明确但不铺满的新建入口，其余退成无底色的小图标 ——
            // 仍然是一行，但读起来是「工具条」，不是第二条导航。
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                // 新建聊天：侧栏里唯一使用主题强调色的元素，明确但不大。
                // 新建逻辑与 AssistantPicker 里那一份保持一致（尊重「启动时新建对话」偏好）。
                Surface(
                    onClick = {
                        scope.launch {
                            val id = if (context.readBooleanPreference("create_new_conversation_on_start", true)) {
                                Uuid.random()
                            } else {
                                repo.getConversationsOfAssistant(settings.assistantId)
                                    .first()
                                    .firstOrNull()
                                    ?.id ?: Uuid.random()
                            }
                            navigateToChatPage(navigator = navController, chatId = id)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = HugeIcons.MessageAdd01,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.size(6.dp))
                        Text(
                            text = stringResource(R.string.chat_page_new_chat),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                // 底部只剩「收藏」「统计」两项。聊天历史入口已按要求移除；
                // 新建聊天 pill 是这一行里的主操作，保持不动。
                DrawerIconAction(
                    icon = HugeIcons.InLove,
                    contentDescription = stringResource(R.string.favorite_page_title),
                    onClick = { navController.navigate(Screen.Favorite) },
                )

                // 统计数据
                DrawerIconAction(
                    icon = HugeIcons.ChartColumn,
                    contentDescription = "统计数据",
                    onClick = { navController.navigate(Screen.Stats) },
                )
            }
        }
        }
    }

    // 昵称编辑对话框
    nicknameEditState.EditStateContent { nickname, onUpdate ->
        AlertDialog(
            onDismissRequest = {
                nicknameEditState.dismiss()
            },
            title = {
                Text(stringResource(R.string.chat_page_edit_nickname))
            },
            text = {
                OutlinedTextField(
                    value = nickname,
                    onValueChange = onUpdate,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.chat_page_nickname_placeholder)) }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        nicknameEditState.confirm()
                    }
                ) {
                    Text(stringResource(R.string.chat_page_save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        nicknameEditState.dismiss()
                    }
                ) {
                    Text(stringResource(R.string.chat_page_cancel))
                }
            }
        )
    }

    // 移动到文件夹 Bottom Sheet
    if (showMoveToFolderSheet) {
        val doMove: (Uuid?) -> Unit = { folderId ->
            conversationToMoveFolder?.let { conversation ->
                drawerVm.moveConversationToFolder(conversation.id, folderId)
                scope.launch {
                    folderSheetState.hide()
                    showMoveToFolderSheet = false
                    conversationToMoveFolder = null
                    conversations.refresh()
                }
            }
        }
        ModalBottomSheet(
            onDismissRequest = {
                showMoveToFolderSheet = false
                conversationToMoveFolder = null
            },
            sheetState = folderSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.chat_page_move_to_folder),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // 移出文件夹（未归类）
                Surface(
                    onClick = { doMove(null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = if (conversationToMoveFolder?.folderId == null) {
                        MaterialTheme.colorScheme.surfaceVariant
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(HugeIcons.Folder01, null)
                        Text(
                            text = stringResource(R.string.chat_page_remove_from_folder),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(folders) { folder ->
                        val isCurrent = folder.id == conversationToMoveFolder?.folderId
                        Surface(
                            onClick = { doMove(folder.id) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium,
                            color = if (isCurrent) {
                                MaterialTheme.colorScheme.surfaceVariant
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            tonalElevation = if (isCurrent) 2.dp else 0.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(HugeIcons.Folder01, null)
                                Text(
                                    text = folder.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // 新建文件夹对话框
    if (showCreateFolderDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateFolderDialog = false },
            title = { Text(stringResource(R.string.chat_page_create_folder)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    placeholder = { Text(stringResource(R.string.chat_page_folder_name)) }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        drawerVm.createFolder(name)
                        showCreateFolderDialog = false
                    },
                    enabled = name.isNotBlank()
                ) { Text(stringResource(R.string.chat_page_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showCreateFolderDialog = false }) {
                    Text(stringResource(R.string.chat_page_cancel))
                }
            }
        )
    }

    // 重命名文件夹对话框
    folderToRename?.let { folder ->
        var name by remember(folder.id) { mutableStateOf(folder.name) }
        AlertDialog(
            onDismissRequest = { folderToRename = null },
            title = { Text(stringResource(R.string.chat_page_rename_folder)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        drawerVm.renameFolder(folder.id, name)
                        folderToRename = null
                    },
                    enabled = name.isNotBlank()
                ) { Text(stringResource(R.string.chat_page_save)) }
            },
            dismissButton = {
                TextButton(onClick = { folderToRename = null }) {
                    Text(stringResource(R.string.chat_page_cancel))
                }
            }
        )
    }

    // 删除文件夹确认
    folderToDelete?.let { folder ->
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text(stringResource(R.string.chat_page_delete_folder)) },
            text = { Text(stringResource(R.string.chat_page_delete_folder_confirm, folder.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (drawerVm.deleteFolder(folder.id)) {
                            folderToDelete = null
                            conversations.refresh()
                        } else {
                            toaster.show(context.getString(R.string.chat_page_delete_folder_generating), type = ToastType.Warning)
                        }
                    }
                ) { Text(stringResource(R.string.chat_page_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) {
                    Text(stringResource(R.string.chat_page_cancel))
                }
            }
        )
    }

    // 移动到助手 Bottom Sheet
    if (showMoveToAssistantSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showMoveToAssistantSheet = false
                conversationToMove = null
            },
            sheetState = bottomSheetState
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.chat_page_move_to_assistant),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(settings.assistants) { assistant ->
                        AssistantItem(
                            assistant = assistant,
                            isCurrentAssistant = assistant.id == conversationToMove?.assistantId,
                            onClick = {
                                conversationToMove?.let { conversation ->
                                    vm.moveConversationToAssistant(conversation, assistant.id)
                                    scope.launch {
                                        bottomSheetState.hide()
                                        showMoveToAssistantSheet = false
                                        conversationToMove = null
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * 侧栏顶部「纪念卡」（按参考图 1000454428 定稿重做）：
 *   行1: 两枚分开圆头像（左渺渺/用户，右栖/助手），各自可点（换头像/助手设置），右上设置
 *   行2: 中间大装饰数字（相识天数，自动抓取）+ 头像下名字行 + ♡ DAYS TOGETHER ♡ 装饰
 *   行3: 动态问候语（早上好/中午好/下午好/晚上好/夜深了 + 主题色小太阳）
 *   行4: 细分隔线 + 「搜索聊天｜背单词」双列卡内入口
 * 卡片下方一概不动。配色全走主题，不新增配色体系。
 */
@Composable
private fun DrawerMemorialCard(
    settings: Settings,
    daysCount: Int?,
    vocabularyBookCount: Int,
    vocabularyDueCount: Int,
    onOpenSettings: () -> Unit,
    onAssistantClick: () -> Unit,
    onSearch: () -> Unit,
    onVocabulary: () -> Unit,
    onUpdateUserAvatar: (Avatar) -> Unit,
) {
    val containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
    val outlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val currentAssistant = settings.getCurrentAssistant()
    val assistantName = currentAssistant.name.ifBlank {
        stringResource(R.string.assistant_page_default_assistant)
    }
    val userName = settings.displaySetting.userNickname.ifBlank {
        stringResource(R.string.user_default_name)
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        // 水纹/花瓣背景：两团错位径向渐变，主题色派生，被半透明面板罩住只剩色晕。
        Box(
            modifier = Modifier
                .matchParentSize()
                .clip(MemorialCardShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                            Color.Transparent,
                        ),
                        center = Offset(x = 0.16f, y = 0.1f),
                        radius = 620f,
                    )
                )
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f),
                            Color.Transparent,
                        ),
                        center = Offset(x = 0.92f, y = 0.85f),
                        radius = 560f,
                    )
                ),
        )
        Surface(
            shape = MemorialCardShape,
            color = containerColor,
            border = BorderStroke(1.dp, outlineColor),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // ---- 行 1：双头像 + 中间装饰数字 + 右上设置 ----
                Box(modifier = Modifier.fillMaxWidth()) {
                    Icon(
                        imageVector = HugeIcons.Settings03,
                        contentDescription = stringResource(R.string.settings),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .clip(CircleShape)
                            .clickable(onClick = onOpenSettings)
                            .padding(6.dp)
                            .size(20.dp),
                    )
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 左：渺渺 = 用户。点击弹原有「更换头像」对话框。
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            UIAvatar(
                                name = settings.displaySetting.userNickname,
                                value = settings.displaySetting.userAvatar,
                                modifier = Modifier.size(52.dp),
                                onUpdate = onUpdateUserAvatar,
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = userName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(18.dp))
                        // 中：装饰数字 = 相识天数（最早对话 -> 今天，自动抓取）。
                        Text(
                            text = daysCount?.toString() ?: "",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                            modifier = Modifier.padding(bottom = 18.dp),
                        )
                        Spacer(Modifier.width(18.dp))
                        // 右：栖 = 助手。点击直达助手设置页。
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onAssistantClick),
                            ) {
                                ChatMessageSideAvatar(
                                    role = MessageRole.ASSISTANT,
                                    model = settings.getCurrentChatModel(),
                                    assistant = currentAssistant,
                                    size = 52.dp,
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = assistantName,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                // ---- 行 2：英文装饰文案 ♡ DAYS TOGETHER ♡ ----
                Text(
                    text = "♡  D A Y S  T O G E T H E R  ♡",
                    style = MaterialTheme.typography.labelSmall,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
                    modifier = Modifier.padding(top = 6.dp),
                )
                // ---- 行 3：动态问候语 + 主题色小太阳 ----
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Greeting(style = MaterialTheme.typography.titleMedium)
                    Icon(
                        imageVector = HugeIcons.Sun03,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                }
                // ---- 行 4：分隔线 + 双列卡内入口 ----
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 10.dp)
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)),
                )
                Row(modifier = Modifier.fillMaxWidth()) {
                    MemorialCardPill(
                        icon = HugeIcons.Search01,
                        label = stringResource(R.string.chat_page_search_chats),
                        onClick = onSearch,
                        modifier = Modifier.weight(1f),
                    )
                    MemorialCardPill(
                        icon = HugeIcons.Book01,
                        label = stringResource(R.string.vocabulary_page_title),
                        trailing = when {
                            vocabularyDueCount > 0 -> stringResource(R.string.vocabulary_drawer_due, vocabularyDueCount)
                            vocabularyBookCount > 0 -> stringResource(R.string.vocabulary_drawer_books, vocabularyBookCount)
                            else -> stringResource(R.string.vocabulary_drawer_import_hint)
                        },
                        onClick = onVocabulary,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** 纪念卡内部的小入口 pill：onSurface 6% 实底 + 主题文字色，居中排布。 */
@Composable
private fun MemorialCardPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (trailing != null) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = trailing,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerItemSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape,
    color: Color,
    drawerItemAlpha: Float,
    materialMode: DisplayMaterialMode,
    content: @Composable () -> Unit,
) {
    val contentColor = MaterialTheme.colorScheme.onSurface
    val backgroundColor = when (materialMode) {
        DisplayMaterialMode.FLAT,
        DisplayMaterialMode.FOLLOW_THEME -> color

        DisplayMaterialMode.TRANSLUCENT,
        DisplayMaterialMode.GLASS -> color.copy(alpha = drawerItemAlpha)
    }
    val borderColor = when (materialMode) {
        DisplayMaterialMode.TRANSLUCENT -> contentColor.copy(alpha = 0.05f * drawerItemAlpha)
        DisplayMaterialMode.GLASS -> contentColor.copy(alpha = 0.04f * drawerItemAlpha)
        DisplayMaterialMode.FLAT,
        DisplayMaterialMode.FOLLOW_THEME -> Color.Transparent
    }
    val materialModifier = when (materialMode) {
        DisplayMaterialMode.TRANSLUCENT,
        DisplayMaterialMode.GLASS -> Modifier.border(1.dp, borderColor, shape)

        DisplayMaterialMode.FLAT,
        DisplayMaterialMode.FOLLOW_THEME -> Modifier
    }

    Surface(
        onClick = onClick,
        modifier = modifier.then(materialModifier),
        shape = shape,
        color = backgroundColor,
        contentColor = contentColor,
    ) {
        Box {
            if (materialMode == DisplayMaterialMode.GLASS) {
                DrawerItemGlassLayers(
                    shape = shape,
                    drawerItemAlpha = drawerItemAlpha,
                )
            }
            content()
        }
    }
}

@Composable
private fun BoxScope.DrawerItemGlassLayers(
    shape: Shape,
    drawerItemAlpha: Float,
) {
    val colorScheme = MaterialTheme.colorScheme
    // 抽屉条目的玻璃三层整体压低。
    //
    // Material cleanup 的原则：玻璃是「消息气泡效果」，不是整个 App 的材质主题。
    // 这三层（斜向渐变 0.1/0.07、竖向渐变 0.13/0.035、渐变描边 0.2/0.045）
    // 叠在每一个抽屉条目和底栏按钮上，是「一排玻璃球」观感的直接来源。
    //
    // 全部约 ×0.3：保留一点材质暗示，不再让普通导航元件跟气泡抢材质表现。
    // 气泡自己的 glassShadowModifier / glassInsetTopHighlightModifier
    // 与 liquid glass 均不受影响。
    Box(
        modifier = Modifier
            .matchParentSize()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        colorScheme.onSurface.copy(alpha = 0.03f * drawerItemAlpha),
                        colorScheme.primary.copy(alpha = 0.02f * drawerItemAlpha),
                        Color.Transparent,
                    )
                )
            )
    )
    Box(
        modifier = Modifier
            .matchParentSize()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colorScheme.onSurface.copy(alpha = 0.04f * drawerItemAlpha),
                        colorScheme.onSurface.copy(alpha = 0.01f * drawerItemAlpha),
                        Color.Transparent,
                    )
                )
            )
    )
    Box(
        modifier = Modifier
            .matchParentSize()
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        colorScheme.onSurface.copy(alpha = 0.06f * drawerItemAlpha),
                        colorScheme.onSurface.copy(alpha = 0.015f * drawerItemAlpha),
                        Color.Transparent,
                    )
                ),
                shape = shape,
            )
    )
}

@Composable
private fun DrawerSearchRow(
    onClick: () -> Unit,
    drawerItemAlpha: Float,
    materialMode: DisplayMaterialMode,
) {
    DrawerItemSurface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        drawerItemAlpha = drawerItemAlpha,
        materialMode = DisplayMaterialMode.FLAT,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = HugeIcons.Search01,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.chat_page_search_chats),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * 侧栏的「背单词」入口行。形状/尺寸/底色都跟 [DrawerSearchRow] 对齐 ——
 * 两者是同一层级的「去另一个地方」，视觉上不该分家。
 *
 * 右侧小字给的是**当下最该知道的那个数**：
 *   有到期的 → 「N 词待复习」
 *   没到期的 → 「N 个词库」
 *   一本都没有 → 「导入词库开始背词」
 */
@Composable
private fun DrawerVocabularyRow(
    bookCount: Int,
    dueCount: Int,
    onClick: () -> Unit,
    drawerItemAlpha: Float,
) {
    val trailing = when {
        dueCount > 0 -> stringResource(R.string.vocabulary_drawer_due, dueCount)
        bookCount > 0 -> stringResource(R.string.vocabulary_drawer_books, bookCount)
        else -> stringResource(R.string.vocabulary_drawer_import_hint)
    }

    DrawerItemSurface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
        drawerItemAlpha = drawerItemAlpha,
        materialMode = DisplayMaterialMode.FLAT,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = HugeIcons.Book01,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = stringResource(R.string.vocabulary_page_title),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 侧栏底部的无底色图标按钮（聊天历史 / 收藏 / 统计 / 菜单）。 */
@Composable
private fun DrawerIconAction(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(38.dp),
        shape = CircleShape,
        color = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** 一条极淡的分隔线：把「功能目录」和「会话列表」分成两层。 */
@Composable
private fun DrawerDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
    )
}

@Composable
private fun FolderBar(
    folders: List<Folder>,
    selectedFolderId: Uuid?,
    onSelect: (Uuid?) -> Unit,
    onCreate: () -> Unit,
    onRename: (Folder) -> Unit,
    onDelete: (Folder) -> Unit,
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        item {
            FolderChip(
                label = stringResource(R.string.chat_page_folder_default),
                selected = selectedFolderId == null,
                onClick = { onSelect(null) },
                onLongClick = {},
            )
        }
        items(folders) { folder ->
            var menuExpanded by remember { mutableStateOf(false) }
            Box {
                FolderChip(
                    label = folder.name,
                    icon = HugeIcons.Folder01,
                    selected = selectedFolderId == folder.id,
                    onClick = { onSelect(folder.id) },
                    onLongClick = { menuExpanded = true },
                )
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    border = materialModeBorderStroke(),
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.chat_page_rename)) },
                        leadingIcon = { Icon(HugeIcons.PencilEdit01, null) },
                        onClick = {
                            onRename(folder)
                            menuExpanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.chat_page_delete)) },
                        leadingIcon = { Icon(HugeIcons.Delete01, null) },
                        onClick = {
                            onDelete(folder)
                            menuExpanded = false
                        }
                    )
                }
            }
        }
        item {
            FolderChip(
                label = stringResource(R.string.chat_page_folder_add),
                icon = HugeIcons.FolderAdd,
                selected = false,
                onClick = onCreate,
                onLongClick = {},
            )
        }
    }
}

@Composable
private fun FolderChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    icon: ImageVector? = null,
) {
    Surface(
        shape = CircleShape,
        color = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        modifier = Modifier
            .clip(CircleShape)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (icon != null) {
                Icon(icon, null, modifier = Modifier.size(14.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AssistantItem(
    assistant: Assistant,
    isCurrentAssistant: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = if (isCurrentAssistant) {
            MaterialTheme.colorScheme.surfaceVariant
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = if (isCurrentAssistant) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            UIAvatar(
                name = assistant.name,
                value = assistant.avatar,
                onUpdate = {},
                modifier = Modifier.size(40.dp),
            )
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = assistant.name.ifBlank { stringResource(R.string.assistant_page_default_assistant) },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (isCurrentAssistant) {
                    Text(
                        text = stringResource(R.string.assistant_page_current_assistant),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
