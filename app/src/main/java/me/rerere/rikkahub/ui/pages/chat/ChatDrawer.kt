package me.rerere.rikkahub.ui.pages.chat

import me.rerere.rikkahub.ui.theme.popupContainerColor
import androidx.activity.ComponentActivity
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
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
import me.rerere.rikkahub.ui.theme.GLASS_BACKGROUND_THEMES
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

/** 纪念卡圆角：比会话行(12dp) 大一档的对话面板圆角。 */
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

    // ---- 侧栏表面分层（材质系统重修 §五）----
    //
    // 历史问题：
    //   1) 容器 alpha = surfaceContainer.alpha(0.82) × 滑杆 ⇒ 60% 时只有 0.49，透出聊天正文；
    //   2) 另有一层「1 - 滑杆」的反向 scrim，与容器互补 ⇒ 总不透明度几乎恒定，滑杆看起来失效；
    //   3) 随后改成 0.90~0.98 的窄区间底板 ⇒ 滑杆依然几乎看不出变化（任务单点名）。
    //
    // 现在的分层（自下而上，全部画在 ModalDrawerSheet 内部）：
    //   L0 主题底（**不透明**）：带图主题 = 底图 Crop + 主题 scrim；普通主题 = 页面背景实色
    //      + 一层极淡的主题色渐变。它独自承担「挡住聊天正文」的职责。
    //   L1 表面染色层（alpha = 滑杆 × 0.70）：只调「表面感强弱」，即还能看到多少主题氛围。
    //   L2 用户自定义侧栏背景图（若设置了，alpha 0.15 保持克制）
    //   L3 纪念卡 / 文件夹 / 会话列表 / 底栏等内容
    //
    // 关键点：遮挡只由 L0 一层负责、调节只由 L1 一层负责，职责分离。
    // 旧实现把「遮挡」和「表面感」压在同两三层半透明底色上，alpha 叠起来接近 1.0，
    // 侧栏就变成一块实色板 —— 滑杆再怎么拉也看不出区别。
    // Keep the opaque backing, but expose more of the drawer's own background.
    // 60 / 80 / 100 map to tint alpha 0.05 / 0.35 / 0.65.
    val drawerSurfaceProgress = ((settings.displaySetting.drawerSurfaceOpacity - 60f) / 40f).coerceIn(0f, 1f)
    val drawerSurfaceLayerAlpha = 0.05f + drawerSurfaceProgress * 0.60f
    val drawerThemeId = settings.themeId
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
            // 容器只做极淡染色，且**不再与滑杆相乘**（避免同一数值多层重复相乘）。
            // 真正的遮挡由内部 L0/L1 层负责，表面感由 L3 层按滑杆连续调节。
            MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.30f),
        drawerContentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // ---- L0：主题底（**不透明**，是整个侧栏「不透字」的唯一保障）----
            //
            // 这里必须是不透明的：只要这一层实了，底下聊天正文就永远进不来，
            // 上面无论叠多少半透明层、滑杆拉到多低，都只会露出「主题氛围」，
            // 而不会露出文字。
            //
            // 带壁纸主题（港口/奶油玫瑰）：底图按侧栏区域 Crop 绘制，再叠一层
            // 主题 scrim 保证文字对比度；复用 RouteActivity 同一张资源，不新增图片。
            // 普通主题：用页面背景实色 + 一层极淡的主题色渐变，避免读成一块死板的纯色。
            if (drawerThemeId in GLASS_BACKGROUND_THEMES) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = R.drawable.harbor_chat_bg),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        // 1.0：底图本身要实，它承担遮挡职责。
                        alpha = 1f,
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.22f)),
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 1f))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                                    Color.Transparent,
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f),
                                )
                            )
                        ),
                )
            }
            // ---- L2：用户自定义侧栏背景图（可选，叠在主题底之上）----
            // 旧行为保留：用户设了图就显示，alpha 0.15 保持克制。
            // 侧边栏背景图
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

            // The only slider-controlled tint; backing already isolates chat text.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.surfaceContainer.copy(
                            alpha = drawerSurfaceLayerAlpha
                        )
                    ),
            )
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
            DrawerMemorialCard(
                settings = settings,
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
            // 分隔线：把顶部的「纪念卡」和下面的「会话列表」分成两层。
            //
            // 侧栏已收成纯聊天导航：功能目录整块移除，只留「搜索聊天 + 会话列表 +
            // 新建 + 收藏 + 统计」。被移走的功能改由设置页进入，页面本身没有动：
            //   工作区   -> 设置 → 扩展 → 技能与扩展
            //   插件     -> 设置 → 扩展 → 插件管理
            //   主动消息 -> 设置 → 通用 → 主动消息
            // 图像生成 / AI 翻译 / Mini Apps / 记忆库 不再出现在侧栏。
            DrawerDivider()

            // 侧栏最后修复：恢复聊天管理入口。
            //   无文件夹：[聊天][新建]
            //   有文件夹：[聊天][文件夹][新建]（文件夹入口在新建之前）
            // 具体文件夹走横向 chips（LazyRow 可滚动），不竖排。
            FolderBar(
                folders = folders,
                selectedFolderId = selectedFolderId,
                onSelect = { drawerVm.selectFolder(it) },
                onCreate = { showCreateFolderDialog = true },
                onNewChat = {
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
        containerColor = popupContainerColor(MaterialTheme.colorScheme.surfaceContainerLow),
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
        containerColor = popupContainerColor(MaterialTheme.colorScheme.surfaceContainerLow),
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

/** Existing content rearranged inside the memorial card only. */
@Composable
private fun DrawerMemorialCard(
    settings: Settings,
    onOpenSettings: () -> Unit,
    onAssistantClick: () -> Unit,
    onSearch: () -> Unit,
    onVocabulary: () -> Unit,
    onUpdateUserAvatar: (Avatar) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val currentAssistant = settings.getCurrentAssistant()
    val assistantName = currentAssistant.name.ifBlank {
        stringResource(R.string.assistant_page_default_assistant)
    }
    val userName = settings.displaySetting.userNickname.ifBlank {
        stringResource(R.string.user_default_name)
    }
    Surface(
        shape = MemorialCardShape,
        color = scheme.primaryContainer.copy(alpha = 0.24f),
        border = BorderStroke(1.dp, scheme.onSurface.copy(alpha = 0.08f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Greeting(modifier = Modifier.weight(1f, fill = false), style = MaterialTheme.typography.bodyMedium)
                    Icon(
                        imageVector = HugeIcons.Sun03,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                }
                IconButton(onClick = onOpenSettings, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = HugeIcons.Settings03,
                        contentDescription = stringResource(R.string.settings),
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
                    UIAvatar(
                        name = settings.displaySetting.userNickname,
                        value = settings.displaySetting.userAvatar,
                        modifier = Modifier.size(52.dp),
                        onUpdate = onUpdateUserAvatar,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(userName, style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Box(modifier = Modifier.weight(1f).padding(bottom = 16.dp), contentAlignment = Alignment.Center) {
                    FourPetalFlower(size = 40.dp, color = scheme.primary.copy(alpha = 0.55f))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(64.dp)) {
                    // Keep the image and centered hit target; no outer ring.
                    Box(
                        modifier = Modifier.size(54.dp).clip(CircleShape).clickable(onClick = onAssistantClick),
                        contentAlignment = Alignment.Center,
                    ) {
                        ChatMessageSideAvatar(
                            role = MessageRole.ASSISTANT,
                            model = settings.getCurrentChatModel(),
                            assistant = currentAssistant,
                            size = 48.dp,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(assistantName, style = MaterialTheme.typography.labelMedium,
                        color = scheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Text(
                text = "♡  S I D E  B Y  S I D E  ♡",
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.5.sp,
                color = scheme.primary.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 8.dp),
                maxLines = 1,
            )
            // Compact actions at the bottom right; no date or placeholder.
            Row(
                modifier = Modifier.align(Alignment.End).padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                MemorialCardPill(HugeIcons.Search01, stringResource(R.string.chat_page_search_chats), onSearch)
                MemorialCardPill(HugeIcons.Book01, stringResource(R.string.vocabulary_page_title), onVocabulary)
            }
        }
    }
}

@Composable
private fun MemorialCardPill(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f),
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.heightIn(min = 48.dp).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
        }
    }
}

/** Four identical round oval petals without pointed tails. */
@Composable
private fun FourPetalFlower(size: androidx.compose.ui.unit.Dp, color: Color) {
    Canvas(modifier = Modifier.size(size)) {
        val r = this.size.minDimension / 2f
        val c = center
        val stroke = Stroke(width = 1.dp.toPx())
        repeat(4) { index ->
            rotate(degrees = index * 90f, pivot = c) {
                drawOval(
                    color = color,
                    topLeft = Offset(c.x - r * 0.27f, c.y - r * 0.88f),
                    size = Size(r * 0.54f, r * 0.72f),
                    style = stroke,
                )
            }
        }
        drawCircle(color = color, radius = r * 0.065f, center = c)
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
    onNewChat: () -> Unit,
    onRename: (Folder) -> Unit,
    onDelete: (Folder) -> Unit,
) {
    // 侧栏最后修复：恢复聊天管理入口，条件式三段：
    //   无文件夹：[聊天] [新建]
    //   有文件夹：[聊天] [文件夹▾] [新建]  —— 点[文件夹▾]横向展开具体文件夹 chips
    // 具体文件夹横向滚动，不竖排；长按重命名/删除保留。
    var showFolderChips by remember { mutableStateOf(false) }
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // [聊天]：全部会话
        item {
            FolderChip(
                label = stringResource(R.string.chat_page_folder_default),
                selected = selectedFolderId == null,
                onClick = { onSelect(null) },
                onLongClick = {},
            )
        }
        // [文件夹▾]：只有存在用户文件夹时出现，紧邻[新建]之前
        if (folders.isNotEmpty()) {
            item {
                FolderChip(
                    label = stringResource(R.string.chat_page_folder_add),
                    icon = HugeIcons.Folder01,
                    selected = showFolderChips || selectedFolderId != null,
                    onClick = { showFolderChips = !showFolderChips },
                    onLongClick = {},
                )
            }
        }
        // [新建]：常驻，新建对话（与底栏同一逻辑）
        item {
            FolderChip(
                label = stringResource(R.string.chat_page_new_chat),
                icon = HugeIcons.MessageAdd01,
                selected = false,
                onClick = onNewChat,
                onLongClick = {},
            )
        }
        // 具体文件夹 chips：横向滚动，点[文件夹▾]展开；长按重命名/删除
        if (folders.isNotEmpty() && showFolderChips) {
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
