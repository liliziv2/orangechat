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
                    // 结果就是输入框读不出边界。上调一档到 surfaceContainerHigh：
                    // 边界出现，仍然是一块浅底，不是卡片。
                    DisplayMaterialMode.FOLLOW_THEME,
                    DisplayMaterialMode.FLAT -> MaterialTheme.colorScheme.surfaceContainerHigh
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
    // 容器形状两态共用同一个常量：固定 28dp 圆角。
    //
    // 单行（胶囊）态容器正好 56dp 高 —— 28dp 半径恰好等于半高，它**就是**一颗胶囊。
    // 多行（大卡片）态容器 106~162dp 高，同一个半径读作圆角卡片。
    // 所以「从胶囊切到大卡片」时圆角是连续的，不是换了一个组件。
    //
    // 不能用 `percent = 50`：那个半径恒等于半高，多行态下会变成半径 53~81dp 的半圆，
    // 底排的 + 与发送会被 `.clip(containerShape)` 啃掉。所以这里是一个纯常量。
    val containerShape = InputEditorShape
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
                    // 两态：**一行是细长胶囊，换行之后才展开成大卡片**。
                    //
                    // 判据三条取或：
                    // 1. `wrappedToSecondLine` —— 实测已经换过行，由正文 TextField 的
                    //    onTextLayout 把真实行数喂回来。这是唯一跟设备宽度、系统字体缩放
                    //    都无关的判据（字数只是估算，宽屏/小字下会误判）；
                    // 2. 正文里有换行符（用户主动按回车）；
                    // 3. 字数超过 SingleLineMaxChars —— 兜底，让「字数已经很多但刚好还没
                    //    触发换行」的那一帧立刻切过去，不用等一次布局。
                    //
                    // 第 1 条必须**锁存**：切到卡片态后正文区会变宽，同一段文字可能又只占
                    // 一行，行数判据就会把状态弹回胶囊态、再换行、再弹回去 —— 死循环。
                    // 所以只在「第一次换行」时置位，输入清空时才复位。
                    var wrappedToSecondLine by remember { mutableStateOf(false) }
                    val inputText = state.textContent.text
                    val isMultiLine = wrappedToSecondLine ||
                        inputText.contains('\n') ||
                        inputText.length > SingleLineMaxChars
                    LaunchedEffect(inputText.isEmpty()) {
                        if (inputText.isEmpty()) wrappedToSecondLine = false
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
                                modifier = Modifier.size(20.dp),
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
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                } else {
                                    Icon(
                                        imageVector = HugeIcons.Voice,
                                        contentDescription = "Voice",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
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
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(ActionButtonSize)
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
                                    modifier = Modifier.fillMaxSize(),
                                    shape = CircleShape,
                                    color = containerColor,
                                    content = {})
                                if (loading) {
                                    KeepScreenOn()
                                    Icon(
                                        imageVector = HugeIcons.Cancel01,
                                        contentDescription = stringResource(R.string.stop),
                                        tint = contentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Icon(
                                        imageVector = HugeIcons.ArrowUp02,
                                        contentDescription = stringResource(R.string.send),
                                        tint = contentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // 单行态 / 多行态共用同一个 Row，只换内边距与对齐 ——
                    // 结构不变，按钮的位置就不会「跳」。
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                // 多行态把左右内缩让给下面的操作行：正文这一行要贴到 16dp
                                // （M3 TextField 自带的 contentPadding）与图标对齐，
                                // 再加 8dp 就变成 24dp，两行左边缘对不齐。
                                start = if (isMultiLine) 0.dp else 8.dp,
                                end = if (isMultiLine) 0.dp else 8.dp,
                                // 多行态底排下方留 10dp：容器圆角固定 28dp，距边界 8dp 处
                                // 下边界被抬高 28 − √(28² − 20²) ≈ 8.4dp，10dp 留 1.6dp 余量。
                                // 单行态容器正好 56dp 高，按钮靠几何中线，不留内缩。
                                bottom = if (isMultiLine) 10.dp else 0.dp,
                            ),
                        verticalAlignment = if (isMultiLine) {
                            Alignment.Bottom
                        } else {
                            Alignment.CenterVertically
                        },
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
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

                            // 多行态：操作行落在正文**下方**（P1 结构）。
                            // 单行态不渲染这一行 —— 按钮与正文同排。
                            if (isMultiLine) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 8.dp, end = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
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
    size: Dp = ActionButtonSize,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // 功能按钮统一尺寸与形状。
    //
    // 之前模型/搜索/思考走各自的 IconButton(M3 最小触摸区 48dp)与 ToggleSurface
    // (内 padding 8dp + 24dp 图标盒 = 40dp),而 + 与语音是 32dp,一排里出现
    // 三种尺寸,读起来是散的。现在全部收到同一个尺寸。
    //
    // 操作行里同时有 + / 搜索 / 思考 / 语音 / 发送 五个按钮，统一走 CapsuleActionSize
    // (36dp)；发送按钮仍然用 ActionButtonSize(40dp)，它是整条容器唯一的实心强调色
    // 元素，大一点才立得住。形状一律正圆。
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
    // 容器形状由 ChatInput 顶层传进来（恒为固定 28dp 圆角；单行态它恰好等于半高，
    // 所以同一个常量在单行时是胶囊、多行时是圆角卡片）。
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
                // 绝对高度兜底：行数上限会随系统字体缩放漂移，这条不会。
                // 超出的文字由 BasicTextField 自己内部滚动，不撑外壳。
                .heightIn(max = InputMaxHeight)
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

/** 发送按钮尺寸(整条容器唯一的实心强调色元素)。 */
private val ActionButtonSize = 40.dp

/**
 * 操作行内联功能按钮的尺寸(+ / 语音 / 搜索 / 思考)。
 *
 * 比发送按钮小一档：操作行里同时有五个按钮，全部 40dp 会让这一排读不出主次，
 * 而发送是唯一的实心强调色元素，该由它立住。36dp 仍高于可用触摸区下限。
 */
private val CapsuleActionSize = 36.dp

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
 *   单行（胶囊）态 = 正文区 56dp ⇒ 56dp，半径 28dp 恰好是半高，就是一颗胶囊
 *   多行（大卡片）态 = 正文区 56~112 + 操作行 40 + 下内边距 10 ⇒ 106~162dp
 * 两态都由行数封顶，不会无限长。
 */
private const val InputMaxLines = 4
private val InputMaxHeight = 112.dp

/**
 * 单行（胶囊）态的字数上限 —— 超过它就切到多行（大卡片）态。
 *
 * 判据用**字数**而不是行数：`TextFieldState` 不暴露实际行数（批 17 的结论），只能估。
 * 12 是「胶囊那一行还读得顺」的边界：胶囊里正文两侧被 + 与功能组夹着，360dp 屏幕上
 * 正文可用宽度约 130dp（约 9 个中文字）。定得再高会出现「字已经写满还挤在胶囊里」；
 * 定得太低又会一打字就展开，违背「不要一开始就是大卡片」。
 */
private const val SingleLineMaxChars = 12

/**
 * 输入容器的形状 —— 固定 28dp 圆角。**两态共用这一个常量。**
 *
 * 单行（胶囊）态容器正好 56dp 高，28dp 半径恰好等于半高 ⇒ 它**就是**一颗标准胶囊。
 * 多行（大卡片）态容器 106~162dp 高，同一个半径读作圆角卡片。
 * 所以「从胶囊切到大卡片」时圆角是连续的，不会像换了一个组件。
 *
 * 不能用 `percent = 50`：那个半径恒等于半高，多行态下会变成半径 53~81dp 的半圆，
 * 底排的 + 与发送会被 `.clip(containerShape)` 啃掉，多行文字的首尾也会被吃掉。
 *
 * 底排按钮的底部内缩量就是从 28dp 这个半径反算的（见 ChatInput 操作行那段注释）：
 * 半径 28dp 时，距边界 8dp 处下边界抬高约 8.4dp，所以多行态留 10dp。
 */
private val InputEditorShape = RoundedCornerShape(28.dp)