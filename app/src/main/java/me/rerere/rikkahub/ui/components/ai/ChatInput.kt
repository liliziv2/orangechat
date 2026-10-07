package me.rerere.rikkahub.ui.components.ai

import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.content.MediaType
import androidx.compose.foundation.content.ReceiveContentListener
import androidx.compose.foundation.content.consume
import androidx.compose.foundation.content.contentReceiver
import androidx.compose.foundation.content.hasMediaType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.dokar.sonner.ToastType
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.Job
import me.rerere.ai.provider.ModelAbility
import me.rerere.ai.ui.UIMessagePart
import me.rerere.asr.ASRStatus
import me.rerere.common.android.appTempFolder
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Add01
import me.rerere.hugeicons.stroke.ArrowUp02
import me.rerere.hugeicons.stroke.Cancel01
import me.rerere.hugeicons.stroke.Voice
import me.rerere.hugeicons.stroke.Zap
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.ai.mcp.McpManager
import me.rerere.rikkahub.data.datastore.DisplayMaterialMode
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.getAssistantById
import me.rerere.rikkahub.data.datastore.getCurrentAssistant
import me.rerere.rikkahub.data.datastore.getCurrentChatModel
import me.rerere.rikkahub.data.datastore.getQuickMessagesOfAssistant
import me.rerere.rikkahub.data.files.FilesManager
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.Conversation
import me.rerere.rikkahub.data.model.QuickMessage
import me.rerere.rikkahub.service.VoiceCallService
import me.rerere.rikkahub.ui.components.ui.KeepScreenOn
import me.rerere.rikkahub.ui.components.ui.toComposeColor
import me.rerere.rikkahub.ui.components.ui.permission.PermissionCamera
import me.rerere.rikkahub.ui.components.ui.permission.PermissionManager
import me.rerere.rikkahub.ui.components.ui.permission.PermissionRecordAudio
import me.rerere.rikkahub.ui.components.ui.permission.rememberPermissionState
import me.rerere.rikkahub.ui.context.LocalASRState
import me.rerere.rikkahub.ui.context.LocalCurrentAssistant
import me.rerere.rikkahub.ui.context.LocalCurrentChatModel
import me.rerere.rikkahub.ui.context.LocalDisplaySettings
import me.rerere.rikkahub.ui.context.LocalProviders
import me.rerere.rikkahub.ui.context.LocalQuickMessages
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.ui.hooks.ChatInputState
import me.rerere.rikkahub.ui.theme.LocalMaterialMode
import me.rerere.rikkahub.ui.theme.materialModeBorderStroke
import me.rerere.rikkahub.ui.theme.popupContainerColor
import me.rerere.rikkahub.utils.SoundEffectPlayer
import org.koin.compose.koinInject
import java.io.File
import kotlin.time.Duration.Companion.seconds
import kotlin.uuid.Uuid

enum class ExpandState {
    Collapsed, Files,
}

@Composable
fun ChatInput(
    state: ChatInputState,
    loading: Boolean,
    conversation: Conversation,
    settings: Settings,
    mcpManager: McpManager,
    hazeState: HazeState,
    enableSearch: Boolean,
    onToggleSearch: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onUpdateAssistant: (Assistant) -> Unit,
    onUpdateSearchService: (Int) -> Unit,
    onCompressContext: (additionalPrompt: String, targetTokens: Int, keepRecentMessages: Int) -> Job,
    onCancelClick: () -> Unit,
    onSendClick: () -> Unit,
    onLongSendClick: () -> Unit,
    onVoiceMessage: ((url: String, duration: Long, transcript: String) -> Unit)? = null,
    autoStartVoice: Boolean = false,
) {
    val toaster = LocalToaster.current
    val assistant = settings.getCurrentAssistant()
    // 输入提示要跟着「这个会话绑定的助手」走，而不是全局当前助手 —— 否则在
    // 会话里换助手、或从会话列表进来时，提示文案会跟实际对话对象对不上。
    // 助手名为空时退回默认助手名（assistant_page_default_assistant），
    // 不写死任何具体名字。
    val inputPlaceholder = stringResource(
        R.string.chat_input_placeholder,
        (settings.getAssistantById(conversation.assistantId) ?: assistant).name
            .ifBlank { stringResource(R.string.assistant_page_default_assistant) },
    )
    val hazeTintColor = MaterialTheme.colorScheme.surfaceContainerLow
    val materialMode = LocalMaterialMode.current
    val useRealtimeBlur = settings.displaySetting.enableBlurEffect &&
        materialMode == DisplayMaterialMode.GLASS &&
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val useMaterialBorder = materialMode == DisplayMaterialMode.TRANSLUCENT ||
        materialMode == DisplayMaterialMode.GLASS

    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    fun sendMessage() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        if (loading) onCancelClick() else onSendClick()
    }

    fun sendMessageWithoutAnswer() {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        if (loading) onCancelClick() else onLongSendClick()
    }

    var expand by remember { mutableStateOf(ExpandState.Collapsed) }
    var showInjectionSheet by remember { mutableStateOf(false) }
    var showCompressDialog by remember { mutableStateOf(false) }
    // 附件面板的 sheet 状态提到这里而不是写在 if 里面：条件式 remember 会在每次开关时
    // 重建状态，导致重新打开时残留上一次的展开/收起进度。skipPartiallyExpanded
    // 让它直接到全高，不做半展开中间态。
    val filesSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    fun dismissExpand() {
        expand = ExpandState.Collapsed
        showInjectionSheet = false
        showCompressDialog = false
    }

    fun expandToggle(type: ExpandState) {
        if (expand == type) {
            dismissExpand()
        } else {
            expand = type
        }
    }

    val context = LocalContext.current
    val filesManager: FilesManager = koinInject()
    val asr = LocalASRState.current
    val asrState by asr.state.collectAsState()
    val voiceCallActiveId by VoiceCallService.activeConversationId.collectAsStateWithLifecycle()
    val isVoiceCallActive = voiceCallActiveId != null
    val hapticFeedback = LocalHapticFeedback.current
    val soundEffectPlayer: SoundEffectPlayer = koinInject()
    LaunchedEffect(Unit) {
        soundEffectPlayer.preload(R.raw.asr_start, R.raw.asr_stop)
    }
    val asrPermission = rememberPermissionState(PermissionRecordAudio)
    PermissionManager(permissionState = asrPermission)

    // 相机权限也挂在这里，不能留在 FilesPicker 里。
    //
    // 附件面板现在是 ModalBottomSheet，它的内容跑在独立的 Dialog composition 中，
    // 而 rememberPermissionState 需要 ComponentActivity 的 composition（要靠
    // ActivityResultRegistry 注册 launcher）。放在面板里会直接抛
    // IllegalStateException，表现就是「点 + 打开面板立刻崩」。
    // 权限状态与说明弹窗都留在 Activity composition，面板只拿一个点击回调。
    val cameraPermission = rememberPermissionState(PermissionCamera)
    PermissionManager(permissionState = cameraPermission)
    var asrBaseText by remember { mutableStateOf("") }
    var voiceMessageMode by remember { mutableStateOf(false) }

    // Auto-start voice recording when entering from voice call notification
    LaunchedEffect(autoStartVoice) {
        if (autoStartVoice && asrState.status == ASRStatus.Idle && asrState.isAvailable) {
            if (asrPermission.allRequiredPermissionsGranted) {
                voiceMessageMode = true
                asr.start { }
            } else {
                asrPermission.requestPermissions()
            }
        }
    }

    LaunchedEffect(asrState.status) {
        when (asrState.status) {
            ASRStatus.Listening -> {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                soundEffectPlayer.play(R.raw.asr_start)
            }

            ASRStatus.Stopping -> {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
                soundEffectPlayer.play(R.raw.asr_stop)
            }

            else -> {}
        }
    }
    LaunchedEffect(asrState.errorMessage) {
        asrState.errorMessage?.takeIf { it.isNotBlank() }?.let { message ->
            toaster.show(message = message, type = ToastType.Error)
            voiceMessageMode = false
        }
    }

    // Handle voice message completion
    LaunchedEffect(asrState.audioFilePath, voiceMessageMode) {
        if (voiceMessageMode && asrState.audioFilePath != null && asrState.status == ASRStatus.Idle) {
            onVoiceMessage?.invoke(
                asrState.audioFilePath!!,
                asrState.durationMs,
                asrState.transcript
            )
            voiceMessageMode = false
        }
    }

    // Camera launcher
    var cameraOutputUri by remember { mutableStateOf<Uri?>(null) }
    var cameraOutputFile by remember { mutableStateOf<File?>(null) }
    val (_, launchCameraCrop) = useCropLauncher(
        onCroppedImageReady = { croppedUri ->
            state.addImages(filesManager.createChatFilesByContents(listOf(croppedUri)))
            dismissExpand()
        },
        onCleanup = {
            cameraOutputFile?.delete()
            cameraOutputFile = null
            cameraOutputUri = null
        }
    )
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { captureSuccessful ->
        if (captureSuccessful && cameraOutputUri != null) {
            if (settings.displaySetting.skipCropImage) {
                state.addImages(filesManager.createChatFilesByContents(listOf(cameraOutputUri!!)))
                cameraOutputFile?.delete()
                cameraOutputFile = null
                cameraOutputUri = null
                dismissExpand()
            } else {
                launchCameraCrop(cameraOutputUri!!)
            }
        } else {
            cameraOutputFile?.delete()
            cameraOutputFile = null
            cameraOutputUri = null
        }
    }
    val onLaunchCamera: () -> Unit = {
        cameraOutputFile = context.cacheDir.resolve("camera_${Uuid.random()}.jpg")
        cameraOutputUri = FileProvider.getUriForFile(
            context, "${context.packageName}.fileprovider", cameraOutputFile!!
        )
        cameraLauncher.launch(cameraOutputUri!!)
    }

    // Image picker launcher
    var preCropTempFile by remember { mutableStateOf<File?>(null) }
    val (_, launchImageCrop) = useCropLauncher(
        onCroppedImageReady = { croppedUri ->
            state.addImages(filesManager.createChatFilesByContents(listOf(croppedUri)))
            dismissExpand()
        },
        onCleanup = {
            preCropTempFile?.delete()
            preCropTempFile = null
        }
    )
    val imagePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { selectedUris ->
            if (selectedUris.isNotEmpty()) {
                Log.d("ImagePickButton", "Selected URIs: $selectedUris")
                if (settings.displaySetting.skipCropImage) {
                    state.addImages(filesManager.createChatFilesByContents(selectedUris))
                    dismissExpand()
                } else {
                    if (selectedUris.size == 1) {
                        val tempFile = File(context.appTempFolder, "pick_temp_${System.currentTimeMillis()}.jpg")
                        runCatching {
                            context.contentResolver.openInputStream(selectedUris.first())?.use { input ->
                                tempFile.outputStream().use { output -> input.copyTo(output) }
                            }
                            preCropTempFile = tempFile
                            launchImageCrop(tempFile.toUri())
                        }.onFailure {
                            Log.e("ImagePickButton", "Failed to copy image to temp, falling back", it)
                            launchImageCrop(selectedUris.first())
                        }
                    } else {
                        state.addImages(filesManager.createChatFilesByContents(selectedUris))
                        dismissExpand()
                    }
                }
            } else {
                Log.d("ImagePickButton", "No images selected")
            }
        }

    // Video picker launcher
    val videoPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { selectedUris ->
            if (selectedUris.isNotEmpty()) {
                state.addVideos(filesManager.createChatFilesByContents(selectedUris))
                dismissExpand()
            }
        }

    // Audio picker launcher
    val audioPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { selectedUris ->
            if (selectedUris.isNotEmpty()) {
                state.addAudios(filesManager.createChatFilesByContents(selectedUris))
                dismissExpand()
            }
        }

    // File picker launcher
    val filePickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNotEmpty()) {
                val allowedMimeTypes = setOf(
                    "text/plain", "text/html", "text/css", "text/javascript", "text/csv", "text/xml",
                    "application/json", "application/javascript", "application/pdf",
                    "application/msword",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/vnd.ms-excel",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "application/vnd.ms-powerpoint",
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    "application/epub+zip",
                    "application/zip", "application/x-zip-compressed"
                )

                val allDocuments = mutableListOf<UIMessagePart.Document>()
                val allImageUris = mutableListOf<Uri>()

                uris.forEach { uri ->
                    val fileName = filesManager.getFileNameFromUri(uri) ?: "file"
                    val mime = filesManager.getFileMimeType(uri) ?: "text/plain"
                    val isZip = mime == "application/zip" || mime == "application/x-zip-compressed" ||
                        fileName.endsWith(".zip", ignoreCase = true)

                    if (isZip) {
                        // Auto-extract ZIP and add internal files
                        val extracted = filesManager.extractZipToChatFiles(uri, fileName)
                        allDocuments.addAll(extracted.documents)
                        allImageUris.addAll(extracted.images)
                    } else {
                        val isAllowed = allowedMimeTypes.contains(mime) || mime.startsWith("text/") ||
                            mime == "application/vnd.openxmlformats-officedocument.wordprocessingml.document" ||
                            mime == "application/pdf" ||
                            fileName.endsWith(".txt", ignoreCase = true) ||
                            fileName.endsWith(".md", ignoreCase = true) ||
                            fileName.endsWith(".csv", ignoreCase = true) ||
                            fileName.endsWith(".json", ignoreCase = true) ||
                            fileName.endsWith(".js", ignoreCase = true) ||
                            fileName.endsWith(".jsx", ignoreCase = true) ||
                            fileName.endsWith(".mjs", ignoreCase = true) ||
                            fileName.endsWith(".cjs", ignoreCase = true) ||
                            fileName.endsWith(".html", ignoreCase = true) ||
                            fileName.endsWith(".css", ignoreCase = true) ||
                            fileName.endsWith(".vue", ignoreCase = true) ||
                            fileName.endsWith(".svelte", ignoreCase = true) ||
                            fileName.endsWith(".xml", ignoreCase = true) ||
                            fileName.endsWith(".py", ignoreCase = true) ||
                            fileName.endsWith(".rb", ignoreCase = true) ||
                            fileName.endsWith(".lua", ignoreCase = true) ||
                            fileName.endsWith(".sql", ignoreCase = true) ||
                            fileName.endsWith(".java", ignoreCase = true) ||
                            fileName.endsWith(".kt", ignoreCase = true) ||
                            fileName.endsWith(".ts", ignoreCase = true) ||
                            fileName.endsWith(".tsx", ignoreCase = true) ||
                            fileName.endsWith(".dart", ignoreCase = true) ||
                            fileName.endsWith(".php", ignoreCase = true) ||
                            fileName.endsWith(".swift", ignoreCase = true) ||
                            fileName.endsWith(".go", ignoreCase = true) ||
                            fileName.endsWith(".bat", ignoreCase = true) ||
                            fileName.endsWith(".cmd", ignoreCase = true) ||
                            fileName.endsWith(".ps1", ignoreCase = true) ||
                            fileName.endsWith(".psm1", ignoreCase = true) ||
                            fileName.endsWith(".sh", ignoreCase = true) ||
                            fileName.endsWith(".bash", ignoreCase = true) ||
                            fileName.endsWith(".zsh", ignoreCase = true) ||
                            fileName.endsWith(".fish", ignoreCase = true) ||
                            fileName.endsWith(".c", ignoreCase = true) ||
                            fileName.endsWith(".h", ignoreCase = true) ||
                            fileName.endsWith(".cpp", ignoreCase = true) ||
                            fileName.endsWith(".cc", ignoreCase = true) ||
                            fileName.endsWith(".cxx", ignoreCase = true) ||
                            fileName.endsWith(".hpp", ignoreCase = true) ||
                            fileName.endsWith(".hh", ignoreCase = true) ||
                            fileName.endsWith(".hxx", ignoreCase = true) ||
                            fileName.endsWith(".rs", ignoreCase = true) ||
                            fileName.endsWith(".cs", ignoreCase = true) ||
                            fileName.endsWith(".markdown", ignoreCase = true) ||
                            fileName.endsWith(".mdx", ignoreCase = true) ||
                            fileName.endsWith(".toml", ignoreCase = true) ||
                            fileName.endsWith(".ini", ignoreCase = true) ||
                            fileName.endsWith(".env", ignoreCase = true) ||
                            fileName.endsWith(".gradle", ignoreCase = true) ||
                            fileName.endsWith(".kts", ignoreCase = true) ||
                            fileName.endsWith(".properties", ignoreCase = true) ||
                            fileName.endsWith(".proto", ignoreCase = true) ||
                            fileName.endsWith(".graphql", ignoreCase = true) ||
                            fileName.endsWith(".gql", ignoreCase = true) ||
                            fileName.endsWith(".yml", ignoreCase = true) ||
                            fileName.endsWith(".yaml", ignoreCase = true)
                        if (isAllowed) {
                            val localUri = filesManager.createChatFilesByContents(listOf(uri))[0]
                            allDocuments.add(UIMessagePart.Document(url = localUri.toString(), fileName = fileName, mime = mime))
                        } else {
                            toaster.show(
                                context.getString(R.string.chat_input_unsupported_file_type, fileName),
                                type = ToastType.Error
                            )
                        }
                    }
                }
                if (allDocuments.isNotEmpty()) {
                    state.addFiles(allDocuments)
                }
                if (allImageUris.isNotEmpty()) {
                    state.addImages(allImageUris)
                }
                if (allDocuments.isNotEmpty() || allImageUris.isNotEmpty()) {
                    dismissExpand()
                }
            }
        }

    // Collapse when ime is visible
    val imeVisile = WindowInsets.isImeVisible
    LaunchedEffect(imeVisile, showInjectionSheet, showCompressDialog) {
        if (imeVisile && !showInjectionSheet && !showCompressDialog) {
            dismissExpand()
        }
    }

    // Load input background image
    val inputBgPath = settings.displaySetting.inputBackgroundPath
    val inputBgBitmap = remember(inputBgPath) {
        if (inputBgPath.isNotBlank() && File(inputBgPath).exists()) {
            android.graphics.BitmapFactory.decodeFile(inputBgPath)?.asImageBitmap()
        } else null
    }
    val inputContainerColor = when {
        inputBgBitmap != null || useRealtimeBlur -> Color.Transparent
        else -> {
            val baseColor = settings.displaySetting.inputFieldColor?.let { it.toComposeColor() }
                ?: when (materialMode) {
                    // 玻璃模式下"透"本身就是材质。底色调亮一档会把玻璃推回不透明的
                    // 塑料板，所以这两种模式继续用原来的 surfaceContainerLow。
                    DisplayMaterialMode.GLASS,
                    DisplayMaterialMode.TRANSLUCENT -> hazeTintColor
                    // 实心模式：输入框该是"安静的浅底"，但不能安静到跟页面背景分不开。
                    // surfaceContainerLow 与页面 surface 只差约 7 个 RGB 单位，
                    // 加上这里本来就没有描边（useMaterialBorder 只在玻璃模式为真），
                    // 结果就是输入框读不出边界。上调到 surfaceContainer：
                    // 边界出现，仍然是一块浅底，不是卡片。
                    //
                    // 批 73：由 surfaceContainerHigh 再降一档到 surfaceContainer ——
                    // surfaceContainerHigh 是 AI 气泡的槽位（ChatMessage 读它），两者同色
                    // ⇒「Surface」与「Bubble」层级重合、输入框读起来像一条贴底的气泡。
                    // 降一档后输入框与气泡拉开一档，同时仍高于页面 surface。
                    DisplayMaterialMode.FOLLOW_THEME,
                    DisplayMaterialMode.FLAT -> MaterialTheme.colorScheme.surfaceContainer
                }
            // 输入框不该是"玻璃板"。它是常驻控件，底下透出的页面纹理会跟文字抢读，
            // 参考正常聊天 App：输入区是一块安静的浅底，不参与材质表演。
            // 0.78/0.56 → 0.94/0.88，保留一点透，但文字对比度稳住。
            when (materialMode) {
                DisplayMaterialMode.TRANSLUCENT -> baseColor.copy(alpha = 0.94f)
                DisplayMaterialMode.GLASS -> baseColor.copy(alpha = 0.88f)
                DisplayMaterialMode.FOLLOW_THEME,
                DisplayMaterialMode.FLAT -> baseColor
            }
        }
    }
    // 两态判据：**只认真实换行**。
    //
    // 1. `wrappedToSecondLine` —— 实测已经换过行，由正文 TextField 的 onTextLayout
    //    把真实行数喂回来。这是唯一跟设备宽度、系统字体缩放都无关的判据
    //    （字数只是估算，宽屏/小字下会误判）。
    // 2. 正文里有换行符（用户主动按回车）。
    //
    // 第 1 条必须**锁存**：切到展开态后正文区会变宽，同一段文字可能又只占一行，
    // 行数判据就会把状态弹回胶囊态、再换行、再弹回去 —— 死循环。
    // 所以只在「第一次换行」时置位，输入清空时才复位。
    //
    // 提到 `containerShape` 之前是必须的：容器形状要按两态选（单行真胶囊 / 多行圆角卡）。
    var wrappedToSecondLine by remember { mutableStateOf(false) }
    val inputText = state.textContent.text
    val isMultiLine = wrappedToSecondLine || inputText.contains('\n')
    LaunchedEffect(inputText.isEmpty()) {
        if (inputText.isEmpty()) wrappedToSecondLine = false
    }

    // 容器形状**按态选**：
    //
    // - 单行（胶囊）态用 [InputCapsuleShape] = `percent 50`（半径恒等于半高）。
    //   胶囊的定义就是「两端半圆」，半径必须跟着高度走 —— 单行容器高
    //   = 48dp 按钮盒与正文区（1 行 52dp）取大者 ＋ 上下各 12dp 内边距 ≈ 76dp，
    //   半高 38dp 自动成为半径；字体放大把单行撑高时形状依旧成立。
    // - 多行（展开）态用 [InputEditorShape] = 28dp 固定圆角。容器 100dp+ 高，
    //   读作圆角矩形。
    //
    // ⚠️ 百分比**只给单行**：多行态容器 100dp+ 高，半径等于半高会变成半圆，
    // 底排的 + 与发送会被 `.clip(containerShape)` 啃掉。
    val containerShape = if (isMultiLine) InputEditorShape else InputCapsuleShape
    val inputContainerBorder = if (useMaterialBorder) {
        // 与 MaterialMode.kt 的全局边框同步降到 0.07
        BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
    } else {
        null
    }
    val useStaticGlass = materialMode == DisplayMaterialMode.GLASS &&
        inputBgBitmap == null && !useRealtimeBlur
    val glassSurfaceTint = MaterialTheme.colorScheme.surface
    val glassHighlight = MaterialTheme.colorScheme.onSurface
    val staticGlassModifier = if (useStaticGlass) {
        Modifier.drawWithCache {
            val topLayerHeight = minOf(size.height * 0.32f, 32.dp.toPx())
            val highlightInset = 20.dp.toPx()
            val highlightY = 0.75.dp.toPx()

            onDrawBehind {
                // 三层玻璃高光整体压低。原来斜向 0.07/0.025 + 竖向 0.04 + 顶线 0.14，
                // 叠在一起让输入框看着像一块打了光的亚克力板，浮在页面上方。
                // 现在只留很淡的一点顶部提亮，交代"这是个可输入的浅底"就够。
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            glassSurfaceTint.copy(alpha = 0.022f),
                            glassSurfaceTint.copy(alpha = 0.008f),
                            Color.Transparent,
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width * 0.62f, topLayerHeight),
                    )
                )
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            glassHighlight.copy(alpha = 0.012f),
                            Color.Transparent,
                        ),
                        endY = topLayerHeight,
                    )
                )
                drawLine(
                    color = glassHighlight.copy(alpha = 0.05f),
                    start = Offset(highlightInset, highlightY),
                    end = Offset(size.width - highlightInset, highlightY),
                    strokeWidth = 0.75.dp.toPx(),
                )
            }
        }
    } else {
        Modifier
    }

    Surface(
        color = Color.Transparent,
    ) {
        Column(
            modifier = modifier
                .imePadding()
                .navigationBarsPadding()
                // 输入区是一块独立的悬浮容器:与屏幕底部、左右边缘都留出空间,
                // 让它读作「浮在页面上的输入组件」,而不是贴底的一行工具栏。
                .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 附件预览与「编辑中」提示条留在输入容器之外（上方）。
            //
            // 容器的内部结构是固定的两行：上行正文、下行操作。缩略图与提示条是
            // 「这次要发什么 / 正在编辑哪条」的附加信息，不属于这两行里的任何一行，
            // 塞进去会把两行结构撑成三行、四行，容器形状也就不再稳定。
            if (state.messageContent.isNotEmpty()) {
                MediaFileInputRow(state = state)
            }

            if (state.isEditing()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = stringResource(R.string.editing))
                        Spacer(Modifier.weight(1f))
                        Icon(
                            imageVector = HugeIcons.Cancel01,
                            contentDescription = stringResource(R.string.cancel_edit),
                            modifier = Modifier.clickable { state.clearInput() }
                        )
                    }
                }
            }

            // Input area with optional background image
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(containerShape)
                    .then(
                        if (useRealtimeBlur) Modifier.hazeEffect(
                            state = hazeState,
                            style = HazeMaterials.ultraThin(containerColor = hazeTintColor)
                        )
                        else Modifier
                    ),
                shape = containerShape,
                tonalElevation = 0.dp,
                color = inputContainerColor,
                border = inputContainerBorder,
            ) {
                // Use Box so background image can match parent size
                Box(modifier = staticGlassModifier) {
                    // Background image inside input area (matches content size exactly)
                    if (inputBgBitmap != null) {
                        Image(
                            bitmap = inputBgBitmap,
                            contentDescription = null,
                            modifier = Modifier
                                .matchParentSize()
                                .clip(containerShape),
                            contentScale = ContentScale.Crop,
                            alpha = 1f,
                        )
                    }

                    // 「+」附件/扩展入口。两态共用同一份定义：位置不同（单行在正文左侧、
                    // 多行在操作行最左），但尺寸、图标、行为必须完全一致，不能各写一遍。
                    val attachButton: @Composable () -> Unit = {
                        ActionIconButton(
                            size = CapsuleActionSize,
                            onClick = {
                                expandToggle(ExpandState.Files)
                            }) {
                            Icon(
                                imageVector = if (expand == ExpandState.Files) HugeIcons.Cancel01 else HugeIcons.Add01,
                                contentDescription = stringResource(R.string.more_options),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(CapsuleActionIconSize),
                            )
                        }
                    }

                    // 操作行的右侧功能组（搜索 / 思考 / 语音 / 发送）。同样两态共用。
                    //
                    // 抽成一个 lambda 而不是复制两份：这四块带着 toaster、语音 ASR 状态机、
                    // 发送的三种行为，复制一遍就等于开了第二套并行实现。
                    //
                    // 声明成 RowScope 的扩展，而不是普通 lambda —— 这不是风格问题。
                    // 里面那个 AnimatedVisibility（发送按钮录音时淡出）在本项目可用的重载
                    // 都带 Row/Column 接收者；普通 lambda 体里没有隐式接收者，编译器挑中
                    // ColumnScope 版却拿不到接收者，直接报
                    // "cannot be called in this context with an implicit receiver"（批 27 CI 就挂在这）。
                    // 两处调用点都直接写在 Row 的 content 里，接收者天然可用。
                    val trailingActions: @Composable RowScope.() -> Unit = {
                        // 搜索
                        val enableSearchMsg = stringResource(R.string.web_search_enabled)
                        val disableSearchMsg = stringResource(R.string.web_search_disabled)
                        val chatModel = settings.getCurrentChatModel()
                        Box(
                            modifier = Modifier.size(CapsuleActionSize),
                            contentAlignment = Alignment.Center,
                        ) {
                            SearchPickerButton(
                                enableSearch = enableSearch,
                                settings = settings,
                                iconSize = CapsuleActionIconSize,
                                onToggleSearch = { enabled ->
                                    onToggleSearch(enabled)
                                    toaster.show(
                                        message = if (enabled) enableSearchMsg else disableSearchMsg,
                                        duration = 1.seconds,
                                        type = if (enabled) {
                                            ToastType.Success
                                        } else {
                                            ToastType.Normal
                                        }
                                    )
                                },
                                onUpdateSearchService = onUpdateSearchService,
                                model = chatModel,
                            )
                        }

                        // 思考/调整
                        val model = settings.getCurrentChatModel()
                        if (model?.abilities?.contains(ModelAbility.REASONING) == true) {
                            Box(
                                modifier = Modifier.size(CapsuleActionSize),
                                contentAlignment = Alignment.Center,
                            ) {
                                ReasoningButton(
                                    reasoningLevel = assistant.reasoningLevel,
                                    onUpdateReasoningLevel = {
                                        onUpdateAssistant(assistant.copy(reasoningLevel = it))
                                    },
                                    onlyIcon = true,
                                    iconSize = CapsuleActionIconSize,
                                )
                            }
                        }

                        // 语音：固定在发送左边。通话进行中禁用，避免两路麦克风冲突。
                        if ((asrState.isAvailable || asrState.isRecording) && !isVoiceCallActive) {
                            ActionIconButton(
                                size = CapsuleActionSize,
                                onClick = {
                                    when (asrState.status) {
                                        ASRStatus.Listening -> {
                                            asr.stop()
                                        }

                                        ASRStatus.Idle, ASRStatus.Error -> {
                                            if (!asrPermission.allRequiredPermissionsGranted) {
                                                asrPermission.requestPermissions()
                                            } else {
                                                voiceMessageMode = true
                                                asr.start { transcript ->
                                                    // Ignore transcript in voice message mode
                                                }
                                            }
                                        }

                                        ASRStatus.Connecting, ASRStatus.Stopping -> {}
                                    }
                                }
                            ) {
                                if (asrState.isRecording) {
                                    androidx.compose.material3.CircularProgressIndicator(
                                        modifier = Modifier.size(CapsuleActionIconSize),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                } else {
                                    Icon(
                                        imageVector = HugeIcons.Voice,
                                        contentDescription = "Voice",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(CapsuleActionIconSize)
                                    )
                                }
                            }
                        }

                        // 发送：整条容器唯一使用主题强调色的实心按钮，固定在操作行最右端。
                        // 空输入态降低透明度而不是换成灰色，保持可辨识但仍读作不可用。
                        // 点击发送 / 长按无回答 / 生成中转取消，三种行为与原来完全一致。
                        AnimatedVisibility(
                            visible = !asrState.isRecording,
                            enter = fadeIn() + scaleIn(),
                            exit = fadeOut() + scaleOut(),
                        ) {
                            // 点击区与实心圆同为 48dp：五个按钮共用同一条中心线、
                            // 同一档视觉尺度。实心圆等于点击区（不再小一档），
                            // 圆内的箭头图标仍是 24dp（CapsuleActionIconSize）。
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(CapsuleActionSize)
                                    .clip(CircleShape)
                                    .combinedClickable(
                                        enabled = loading || !state.isEmpty(),
                                        onClick = {
                                            dismissExpand()
                                            sendMessage()
                                        }, onLongClick = {
                                            dismissExpand()
                                            sendMessageWithoutAnswer()
                                        }
                                    )
                            ) {
                                val containerColor = when {
                                    loading -> MaterialTheme.colorScheme.errorContainer
                                    state.isEmpty() -> MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                                    else -> MaterialTheme.colorScheme.primary
                                }
                                val contentColor = when {
                                    loading -> MaterialTheme.colorScheme.onErrorContainer
                                    else -> MaterialTheme.colorScheme.onPrimary
                                }
                                Surface(
                                    modifier = Modifier.size(ActionButtonSize),
                                    shape = CircleShape,
                                    color = containerColor,
                                    content = {})
                                if (loading) {
                                    KeepScreenOn()
                                    Icon(
                                        imageVector = HugeIcons.Cancel01,
                                        contentDescription = stringResource(R.string.stop),
                                        tint = contentColor,
                                        modifier = Modifier.size(CapsuleActionIconSize)
                                    )
                                } else {
                                    Icon(
                                        imageVector = HugeIcons.ArrowUp02,
                                        contentDescription = stringResource(R.string.send),
                                        tint = contentColor,
                                        modifier = Modifier.size(CapsuleActionIconSize)
                                    )
                                }
                            }
                        }
                    }

                    // 单行态 / 多行态共用同一个 Row，只换对齐方式 ——
                    // 结构不变，按钮的位置就不会「跳」。
                    //
                    // 容器内加 **14dp** 内边距（[InputContainerPadding]）：这是「Send 贴近
                    // 胶囊边界」的根治。以前操作行直接贴容器边缘，48dp 的发送盒子里那圈
                    // 实心圆距右边界只剩约 4dp，又压在圆角上，读作溢出。
                    // 内缩 14dp 后，发送圆右缘到胶囊边界恒为 14dp（单行态下缘与上缘同档，
                    // 因为正文区已收到 48dp、与按钮盒齐平）—— 贴在右下角但不挤。
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(InputContainerPadding),
                        verticalAlignment = if (isMultiLine) {
                            Alignment.Bottom
                        } else {
                            Alignment.CenterVertically
                        },
                    ) {
                        // 单行态：「+」直接排在正文左边。
                        if (!isMultiLine) {
                            attachButton()
                        }

                        // 正文。**两态共用同一个调用点** —— 这是硬要求：
                        // 挪到另一个分支的调用点，Compose 会把它当成另一个 composable
                        // 销毁重建，输入焦点和光标位置一起丢，键盘会在切形态的瞬间收起。
                        Column(modifier = Modifier.weight(1f)) {
                            TextInputRow(
                                state = state,
                                onSendMessage = { sendMessage() },
                                placeholder = inputPlaceholder,
                                modifier = Modifier.fillMaxWidth(),
                                shape = containerShape,
                                singleLine = !isMultiLine,
                                onMeasureLines = { lines ->
                                    if (lines > 1) wrappedToSecondLine = true
                                },
                            )

                            // 单行态不渲染这一行 —— 按钮与正文同排（图中样式）。
                            // 换行之后操作行才落在正文**下方**（P1 结构），
                            // 固定在容器底部，不随正文首行位置上下跳。
                            //
                            // 行高由 48dp 的按钮盒子决定（= CapsuleActionSize），没有额外
                            // 内边距 —— 五个按钮的中心线因此始终重合在同一条水平线上。
                            if (isMultiLine) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    attachButton()
                                    Spacer(Modifier.weight(1f))
                                    trailingActions()
                                }
                            }
                        }

                        // 单行态：功能组与正文同排，贴在行尾。
                        if (!isMultiLine) {
                            trailingActions()
                        }
                    }
                }
            }

        }
    }

    // 附件/扩展面板：从「输入框下面的一个 Box」改成 ModalBottomSheet。
    //
    // 以前它和输入框是同一个 Column 里的两个大圆角容器：展开时把输入框整个往上顶，
    // 面板高度还被输入框挤着，内容只能在一个矮窗口里滚。改成 bottom sheet 之后
    // 输入框位置不动，面板从底部盖上来，能拿到整屏高度。
    //
    // 返回键交给 ModalBottomSheet 自己处理，原来那个 BackHandler 一并去掉。
    if (expand == ExpandState.Files) {
        ModalBottomSheet(
            onDismissRequest = { dismissExpand() },
            sheetState = filesSheetState,
            containerColor = if (useRealtimeBlur) {
                Color.Transparent
            } else {
                popupContainerColor(hazeTintColor)
            },
        ) {
            FilesPicker(
                conversation = conversation,
                state = state,
                assistant = assistant,
                mcpManager = mcpManager,
                onCompressContext = onCompressContext,
                onUpdateAssistant = onUpdateAssistant,
                showInjectionSheet = showInjectionSheet,
                onShowInjectionSheetChange = { showInjectionSheet = it },
                showCompressDialog = showCompressDialog,
                onShowCompressDialogChange = { showCompressDialog = it },
                onDismiss = { dismissExpand() },
                // 已授权就直接开相机，没授权先申请（说明弹窗由上面的 PermissionManager 负责）。
                // 这个判断必须留在这里：cameraPermission 只在 Activity composition 里存在。
                onTakePic = {
                    if (cameraPermission.allRequiredPermissionsGranted) {
                        onLaunchCamera()
                    } else {
                        cameraPermission.requestPermissions()
                    }
                },
                onPickImage = { imagePickerLauncher.launch("image/*") },
                onPickVideo = { videoPickerLauncher.launch("video/*") },
                onPickAudio = { audioPickerLauncher.launch("audio/*") },
                onPickFile = { filePickerLauncher.launch(arrayOf("*/*")) },
            )
        }
    }
}

@Composable
private fun ActionIconButton(
    onClick: () -> Unit,
    size: Dp = CapsuleActionSize,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // 功能按钮统一尺寸与形状。
    //
    // 操作行里同时有 + / 搜索 / 思考 / 语音 / 发送 五个按钮，点击区统一走
    // CapsuleActionSize(48dp)；图标统一走 CapsuleActionIconSize(24dp)，
    // 由调用点画在盒子正中。发送按钮的实心圆是 ActionButtonSize(48dp)，
    // 与点击区同大 —— 五个按钮因此共用同一条中心线、同一档视觉尺度。
    // 盒子本身是透明的（color = Color.Transparent），只贡献留白，不画方块。
    // 形状一律正圆。
    Surface(
        onClick = onClick,
        modifier = modifier.size(size),
        shape = CircleShape,
        tonalElevation = 0.dp,
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
private fun TextInputRow(
    state: ChatInputState,
    onSendMessage: () -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    // 容器形状由 ChatInput 顶层按两态传进来（单行 = percent 50 真胶囊，
    // 多行 = 固定 28dp 圆角）。
    // 这里不能直接用 ChatInput 的局部 containerShape —— 那是另一个函数的作用域。
    shape: Shape,
    // 单行（胶囊）态下按回车要主动补换行（SingleLine 会把换行吃掉），所以两态仍然要区分。
    // **但它只影响按键行为，不再影响 TextField 自己的换行 / 增高**（见下面 lineLimits）。
    singleLine: Boolean,
    // 实测行数回传。两态判据拿它当「已经换过行」的依据，不再只靠字数猜。
    onMeasureLines: (Int) -> Unit = {},
) {
    val displaySettings = LocalDisplaySettings.current
    val filesManager: FilesManager = koinInject()
    val assistant = LocalCurrentAssistant.current
    val allQuickMessages = LocalQuickMessages.current
    val quickMessages = remember(allQuickMessages, assistant.quickMessageIds) {
        allQuickMessages.filter { it.id in assistant.quickMessageIds }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // 「编辑中」提示条留在容器之外（见 ChatInput 里那段注释）。这里只留输入框本身。
        val receiveContentListener = remember(
            displaySettings.pasteLongTextAsFile, displaySettings.pasteLongTextThreshold
        ) {
            ReceiveContentListener { transferableContent ->
                when {
                    transferableContent.hasMediaType(MediaType.Image) -> {
                        transferableContent.consume { item ->
                            val uri = item.uri
                            if (uri != null) {
                                state.addImages(
                                    filesManager.createChatFilesByContents(
                                        listOf(uri)
                                    )
                                )
                            }
                            uri != null
                        }
                    }

                    displaySettings.pasteLongTextAsFile && transferableContent.hasMediaType(MediaType.Text) -> {
                        transferableContent.consume { item ->
                            val text = item.text?.toString()
                            if (text != null && text.length > displaySettings.pasteLongTextThreshold) {
                                val document = filesManager.createChatTextFile(text)
                                state.addFiles(listOf(document))
                                true
                            } else {
                                false
                            }
                        }
                    }

                    else -> transferableContent
                }
            }
        }
        TextField(
            state = state.textContent,
            modifier = Modifier
                .fillMaxWidth()
                // 高度完全由内容决定：行数 × 行高 + contentPadding。
                //
                // ⚠️ 多行态必须显式给一个**非 0** 的下限，否则 M3 的 TextField 内部
                // 那层 `defaultMinSize(minHeight = TextFieldDefaults.MinHeight = 56.dp)`
                // 会生效（TextField.kt:306）。它是「传入约束 minHeight == 0 时才套自己
                // 的下限」（foundation Size.kt 的 UnspecifiedConstraintsNode），
                // 1dp 即可让它让位 —— 于是 1 行文字只占 52dp，不再被顶成 56dp 的空壳。
                // 单行（胶囊）态反过来要**故意**占满 56dp：容器圆角固定 28dp，
                // 56dp 高时 28dp 正好是半高，读作一颗标准胶囊。
                .heightIn(
                    min = if (singleLine) InputCapsuleHeight else InputMinHeight,
                    max = InputMaxHeight,
                )
                // 绝对高度兜底：行数上限会随系统字体缩放漂移，这条不会。
                // 超出的文字由 BasicTextField 自己内部滚动，不撑外壳。
                .contentReceiver(receiveContentListener),
            shape = shape,
            // 输入与占位文字降一级:输入框是常驻控件,文字不该跟消息正文同级抢读。
            textStyle = MaterialTheme.typography.bodyMedium,
            placeholder = {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            // 上限从 5 行收到 4 行：5 行时外壳约 136–166dp（随字体缩放），已经超出
            // 「最大 120–140dp」；4 行固定 116dp。超出的文字在编辑区内部滚动。
            //
            // **正文恒为 MultiLine，不再按两态在 SingleLine / MultiLine 之间切。**
            // 原来单行态走 SingleLine，代价是长句在框内**横向滚动**：光标移到句尾时
            // 前半句会被滚出可视区，容器也不会随内容真正增高 —— 那不是多行自适应，
            // 只是把一行塞进了一个看起来更高的壳里。现在换行与增高完全交给
            // MultiLine + maxHeightInLines，两态只决定按钮排在哪一行（见 ChatInput）。
            lineLimits = TextFieldLineLimits.MultiLine(maxHeightInLines = InputMaxLines),
            // 把实测行数喂回两态判据。字数只是估算，设备宽度和系统字体缩放一变就不准。
            onTextLayout = { getResult ->
                onMeasureLines(getResult()?.lineCount ?: 1)
            },
            keyboardOptions = KeyboardOptions(
                imeAction = if (displaySettings.sendOnEnter) ImeAction.Send else ImeAction.Default
            ),
            onKeyboardAction = {
                when {
                    displaySettings.sendOnEnter && !state.isEmpty() -> onSendMessage()
                    // 单行（胶囊）态下按回车 = 用户主动要写多行：补一个换行，
                    // 容器下一帧就按 isMultiLine 切成大卡片。
                    // 不补的话 SingleLine 会把换行吃掉，用户永远进不了多行态。
                    singleLine -> state.appendText("\n")
                }
            },
            // 单行（胶囊）态的上下内边距从 M3 默认的 16dp 收到 14dp：
            // 1 行正文（20dp 行高）因此占 20 + 14×2 = 48dp，与 48dp 的操作按钮盒齐平，
            // 文字不再「浮」在 52dp 的正文区中间。容器总高仍是
            // max(48, 48) + 14×2 = 76dp，落在目标区间内。
            // 多行态保持 M3 的默认值（左右上下各 16dp，= TextFieldDefaults
            // .contentPaddingWithoutLabel()）—— 不硬编码，避免随 M3 版本漂移。
            contentPadding = if (singleLine) {
                PaddingValues(top = 14.dp, bottom = 14.dp, start = 16.dp, end = 16.dp)
            } else {
                TextFieldDefaults.contentPaddingWithoutLabel()
            },
            colors = TextFieldDefaults.colors().copy(
                unfocusedIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
            ),
            leadingIcon = if (quickMessages.isNotEmpty()) {
                {
                    QuickMessageButton(quickMessages = quickMessages, state = state)
                }
            } else null,
        )
    }
}

@Composable
private fun QuickMessageButton(
    quickMessages: List<QuickMessage>,
    state: ChatInputState,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(
        onClick = {
            expanded = !expanded
        }) {
        Icon(HugeIcons.Zap, null)
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            border = materialModeBorderStroke(),
            modifier = Modifier
                .widthIn(min = 200.dp)
                .width(IntrinsicSize.Min)
        ) {
            quickMessages.forEach { quickMessage ->
                Surface(
                    onClick = {
                        state.appendText(quickMessage.content)
                        expanded = false
                    },
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp)
                    ) {
                        Text(
                            text = quickMessage.title,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = quickMessage.content,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 输入容器内部四周的内边距。
 *
 * **这是「Send 贴近/超出胶囊边界」的根治。** 以前操作行直接贴容器边缘，
 * 48dp 的发送盒子里那圈实心圆距右边界只剩约 4dp，又压在圆角上，读作溢出。
 * 内缩 12dp 后，发送圆右缘到胶囊边界是 12dp，下缘（单行态）约 14dp ——
 * 贴在右下角但不挤。横向量同时让「+」的图标中心与正文左边缘自然对齐。
 *
 * 单行胶囊态容器高＝内容高（48dp 按钮盒与 1 行正文 52dp 取大者）＋ 上下各 12dp
 * ⇒ 约 76dp，[InputCapsuleShape] 的 `percent 50` 自动落成半径 38dp ⇒ 标准胶囊。
 */
private val InputContainerPadding = 14.dp

/**
 * 发送按钮的实心圆直径（整条容器唯一的实心强调色元素）。
 *
 * 与点击区同为 48dp：五个按钮共用同一条中心线、同一档视觉尺度 ——
 * 实心圆不再比点击区小一档。圆内箭头图标仍是 24dp（CapsuleActionIconSize）。
 */
private val ActionButtonSize = 48.dp

/**
 * 操作行里**每一个**按钮的点击区尺寸(+ / 语音 / 搜索 / 思考 / 发送)。
 *
 * 48dp 是触摸目标的下限，五个按钮统一用它，所以操作行的高度恒为 48dp ——
 * 多行态的容器总高就是「正文区高度 + 48dp」，正文每多一行整体长一行。
 *
 * 按钮本身是透明的（[ActionIconButton] 的 Surface 不画底），48dp 的盒子只贡献
 * 「图标到容器边缘 / 图标到图标」的留白，不产生任何可见方块。图标在盒子里居中，
 * 五个按钮因此共用同一条中心线。盒子之间的间距留白交给容器内边距
 * （[InputContainerPadding]，12dp）—— 不再叠加 padding 或 spacedBy。
 *
 * 实心发送圆与它同大（见 [ActionButtonSize]，也是 48dp），居中其中。
 */
private val CapsuleActionSize = 48.dp

/**
 * 操作行内联功能按钮的**图标**尺寸(+ / 搜索 / 思考 / 语音 / 发送)。
 *
 * 五个按钮共用这一个档位 —— 不因为某个图标本身图形大小不同而再单独 scale()。
 * 搜索 / 思考组件内部还各带 8dp 内边距（24 + 8×2 = 40dp），所以它们的自然尺寸
 * 比 48dp 的盒子小，居中放置即可；其余按钮是 `Icon(size = 本值)` 直接画在 48dp
 * 盒子的正中。两条路径的**图标墨迹尺寸一致**。
 *
 * 档位取 24dp 的依据（对参考图做了像素测量，1080px / 360dp ⇒ 3 px/dp）：
 * - 参考图的 + 墨迹 18.0×18.0dp、笔画 2.00dp；本仓库同款图标实测墨迹/盒子比
 *   0.662、笔画/墨迹比 0.116，参考图是 0.111 —— 同一图标族等比放大。
 * - 参考图的发送箭头墨迹 13.3×15.3dp，反推盒子约 23dp。
 * 两个锚点分别指向 27dp 与 23dp，取 24dp 这一档：落在任务单给的 24–28dp 区间内，
 * 且保证发送箭头不会比其余图标更大。
 *
 * 组件默认值一个都不动（搜索 / 思考组件的 iconSize 默认仍是 24dp，其余调用点
 * 零影响），只有聊天输入框这一处显式传本值。
 */
private val CapsuleActionIconSize = 24.dp

/**
 * 正文输入区的行数上限与绝对高度上限（**只管多行态**）。
 *
 * 为什么两个都要，而不是只留一个：
 * - `maxHeightInLines` 决定「能看见几行」，但它的**实际高度会随系统字体缩放漂移** ——
 *   行高 = 字号 × 行距系数，字号放大 1.3 倍，同样 4 行就从约 112dp 变成约 136dp。
 * - 所以再压一条**绝对 dp 上限**兜底，让外壳高度与字体缩放无关。
 *
 * 这两个值一致（4 行 = 4 × 20dp 行高 + 32dp 内边距 = 112dp），所以默认字体下由谁生效都一样；
 * 字体放大之后由 [InputMaxHeight] 生效，多出来的文字在编辑区内部滚动，不撑外壳。
 *
 * 容器总高：
 *   单行（胶囊）态 = 内容高（正文区 [InputCapsuleHeight] 48dp 与 1 行 52dp 取大者）
 *                   ＋ 上下各 12dp 内边距 ⇒ 约 76dp（[InputCapsuleShape] 的 percent 50 保证是胶囊）
 *   多行（展开）态 = 正文区 52~112 + 操作行 48（[CapsuleActionSize]）＋ 上下各 12dp ⇒ 约 124~184dp
 * 两态都由行数封顶，不会无限长；多行态的正文区高度完全由内容决定，没有固定空壳。
 */
private const val InputMaxLines = 4
private val InputMaxHeight = 112.dp

/**
 * 正文区在**单行（胶囊）态**的下限高度。
 *
 * 取 48dp = 操作按钮盒尺寸（[CapsuleActionSize]）：正文区与按钮盒齐高，
 * 单行态容器高度因此只由「1 行正文的自然高度」与这 48dp 的较大者决定，
 * 不会出现固定高空壳。容器形状走 [InputCapsuleShape]（percent 50），
 * 无论最终多高都读作一颗标准胶囊。
 */
private val InputCapsuleHeight = 48.dp

/**
 * 正文区在**多行（大卡片）态**的下限高度。
 *
 * 取 1dp 而不是 0dp 是有原因的：M3 的 TextField 在 BasicTextField 外面套了一层
 * `defaultMinSize(minWidth = TextFieldDefaults.MinWidth, minHeight = TextFieldDefaults.MinHeight)`
 * （TextField.kt:306，MinHeight = 56.dp），而 `defaultMinSize` **只在传入约束的
 * minHeight == 0 时才套用自己的下限**（foundation Size.kt 的 UnspecifiedConstraintsNode）。
 * 给一个非 0 的下限即可让它让位，正文区高度于是完全由内容决定：
 * 1 行 = 20dp 行高 + 32dp contentPadding = 52dp，2 行 = 72dp，不再有 56dp 的固定空壳。
 */
private val InputMinHeight = 1.dp

/**
 * 单行（胶囊）态的容器形状 —— `percent = 50`，即半径恒等于半高。
 *
 * **单行态用百分比的语义才是对的**：胶囊的定义就是「两端是半圆」，半径必须跟着高度走。
 * 单行容器高 = 48dp 按钮盒（[InputCapsuleHeight]）与正文区（1 行 52dp）取大者 ＋
 * 上下各 12dp 内边距（[InputContainerPadding]）≈ 76dp，半高 38dp 自动成为半径；
 * 系统字体放大把单行撑高时，胶囊形状依旧成立，不会退化成圆角矩形。
 *
 * ⚠️ 这个百分比**只能给单行态用**。多行态必须用 [InputEditorShape] 的固定 28dp ——
 * 多行容器 100dp 以上，半径等于半高会变成半圆，底排的 + 与发送会被 `.clip()` 啃掉。
 */
private val InputCapsuleShape = RoundedCornerShape(percent = 50)

/**
 * 输入容器的形状 —— **多行（展开）态**专用，固定 28dp 圆角。
 *
 * 容器 100dp 以上高，28dp 读作圆角矩形。单行态不用它，见 [InputCapsuleShape]。
 * 不使用 `percent = 50`：那个半径恒等于半高，多行态下会变成半径 50~80dp 的半圆，
 * 底排的 + 与发送会被 `.clip(containerShape)` 啃掉。所以这里是一个纯常量。
 */
private val InputEditorShape = RoundedCornerShape(28.dp)