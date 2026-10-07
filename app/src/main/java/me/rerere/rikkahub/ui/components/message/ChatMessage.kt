package me.rerere.rikkahub.ui.components.message
 
import android.content.Intent
import android.media.MediaPlayer
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.net.toUri
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.rerere.ai.core.MessageRole
import me.rerere.ai.provider.Model
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.UIMessageAnnotation
import me.rerere.ai.ui.UIMessagePart
import me.rerere.ai.ui.isEmptyUIMessage
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.File02
import me.rerere.hugeicons.stroke.MusicNote03
import me.rerere.hugeicons.stroke.PlayCircle
import me.rerere.hugeicons.stroke.PauseCircle
import me.rerere.hugeicons.stroke.Video01
import me.rerere.rikkahub.R
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.AssistantAffectScope
import me.rerere.rikkahub.data.ai.pills.PillRegistry
import me.rerere.rikkahub.data.ai.pills.PillStore
import me.rerere.rikkahub.data.model.MessageNode
import me.rerere.rikkahub.data.model.replaceRegexes
import me.rerere.rikkahub.ui.components.richtext.MarkdownBlock
import me.rerere.rikkahub.ui.components.richtext.ZoomableAsyncImage
import me.rerere.rikkahub.ui.components.richtext.buildMarkdownPreviewHtml
import kotlin.math.max
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.rerere.tts.controller.extractAmplitudeEnvelope
import me.rerere.rikkahub.ui.components.ui.ChainOfThought
import me.rerere.rikkahub.ui.components.ui.Favicon
import me.rerere.rikkahub.ui.components.ui.VoiceWaveform
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.modifier.shimmer
import me.rerere.rikkahub.ui.components.ui.toComposeColor
import me.rerere.rikkahub.ui.context.LocalDisplaySettings
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.theme.LocalDarkMode
import me.rerere.rikkahub.ui.theme.LocalMaterialMode
import me.rerere.rikkahub.ui.theme.THEME_THINKING_CONTAINER_THEMES
import me.rerere.rikkahub.ui.theme.extendColors
import me.rerere.rikkahub.data.datastore.ChatAvatarMode
import me.rerere.rikkahub.data.datastore.ChatFontFamily
import me.rerere.rikkahub.data.datastore.DisplayMaterialMode
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import me.rerere.rikkahub.utils.JsonInstant
import me.rerere.rikkahub.utils.base64Encode
import me.rerere.rikkahub.utils.openUrl
import coil3.compose.AsyncImage
import me.rerere.rikkahub.utils.splitIntoBubbleSegments
import me.rerere.rikkahub.utils.urlDecode
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

/**
 * 气泡侧边头像的容器。
 * enabled=false 时原样透传内容，不额外包 Row，免得动到原版式的对齐与宽度。
 * enabled=true 时按「对方左、自己右」摆一枚头像，内容列吃掉剩下的宽度；
 * 头像与内容顶端对齐，长回复时头像不会飘到中间。
 */
@Composable
private fun SideAvatarRow(
    enabled: Boolean,
    showAvatar: Boolean,
    mine: Boolean,
    role: MessageRole,
    model: Model?,
    assistant: Assistant?,
    loading: Boolean,
    content: @Composable () -> Unit,
) {
    if (!enabled) {
        content()
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        // 用户侧头像贴得更近（6dp）；助手侧维持 8dp。
        horizontalArrangement = Arrangement.spacedBy(if (mine) 6.dp else 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (!mine) {
            SideAvatarSlot(showAvatar, role, model, assistant, loading, mine = false)
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            content()
        }
        if (mine) {
            SideAvatarSlot(showAvatar, role, model, assistant, loading, mine = true)
        }
    }
}

/** 头像槽：关掉头像时仍占位，两侧气泡的起始线才不会左右错开。 */
@Composable
private fun SideAvatarSlot(
    showAvatar: Boolean,
    role: MessageRole,
    model: Model?,
    assistant: Assistant?,
    loading: Boolean,
    mine: Boolean,
) {
    // 用户侧头像顶对齐内容列顶端（= 附件/图片顶），不再下沉；
    // 助手侧维持原有的 2dp 下沉。
    val topPad = if (mine) 0.dp else 2.dp
    if (showAvatar) {
        ChatMessageSideAvatar(
            role = role,
            model = model,
            assistant = assistant,
            loading = loading,
            size = 32.dp,
            modifier = Modifier.padding(top = topPad),
        )
    } else {
        // 关掉头像不留空洞：自己是实心四角星，对方是描边菱形
        ChatMessageSideMark(
            mine = role == MessageRole.USER,
            size = 32.dp,
            modifier = Modifier.padding(top = topPad),
        )
    }
}

@Composable
fun ChatMessage(
    node: MessageNode,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    model: Model? = null,
    assistant: Assistant? = null,
    lastMessage: Boolean = false,
    onFork: () -> Unit,
    onRegenerate: () -> Unit,
    onEdit: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onUpdate: (MessageNode) -> Unit,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    onTranslate: ((UIMessage, Locale) -> Unit)? = null,
    onClearTranslation: (UIMessage) -> Unit = {},
    onToolApproval: ((toolCallId: String, approved: Boolean, reason: String) -> Unit)? = null,
    onToolAnswer: ((toolCallId: String, answer: String) -> Unit)? = null,
) {
    val message = node.messages[node.selectIndex]
    // 挂在这条消息上的药丸。PillStore 是进程内 StateFlow，界面直接读它画标记 ——
    // 药丸只作用一轮，用户得看得见「这条挂了什么」，而不是靠记忆猜。
    val pendingPills by PillStore.pending.collectAsState()
    val attachedPills = pendingPills[message.id].orEmpty()
    val settings = LocalDisplaySettings.current
    // 侧边头像版式：头像贴气泡边，署名行让位。用户侧还要服从「显示用户头像」开关。
    val sideAvatar = settings.chatAvatarMode == ChatAvatarMode.SIDE
    val showSideSlot = sideAvatar && when (message.role) {
        MessageRole.USER -> settings.showUserAvatar
        MessageRole.ASSISTANT -> settings.showModelIcon
        else -> false
    }
    val textStyle = LocalTextStyle.current.copy(
        fontSize = LocalTextStyle.current.fontSize * settings.fontSizeRatio,
        color = settings.chatTextColor?.let { it.toComposeColor() } ?: Color.Unspecified,
        lineHeight = LocalTextStyle.current.lineHeight * settings.fontSizeRatio,
        fontFamily = when (settings.chatFontFamily) {
            ChatFontFamily.DEFAULT -> FontFamily.Default
            ChatFontFamily.SERIF -> FontFamily.Serif
            ChatFontFamily.MONOSPACE -> FontFamily.Monospace
            ChatFontFamily.CUSTOM -> {
                val fontPath = settings.customFontPath
                if (fontPath.isNotBlank() && java.io.File(fontPath).exists()) {
                    FontFamily(Font(java.io.File(fontPath)))
                } else {
                    FontFamily.Default
                }
            }
        }
    )
    var showActionsSheet by remember { mutableStateOf(false) }
    var showSelectCopySheet by remember { mutableStateOf(false) }
    var showPillSheet by remember { mutableStateOf(false) }
    val navController = LocalNavController.current
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (message.role == MessageRole.USER) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (!message.parts.isEmptyUIMessage() && !sideAvatar) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            ) {
                ChatMessageAssistantAvatar(
                    message = message,
                    model = model,
                    assistant = assistant,
                    loading = loading,
                    modifier = Modifier.weight(1f)
                )
                ChatMessageUserAvatar(
                    message = message,
                    avatar = settings.userAvatar,
                    nickname = settings.userNickname,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        ProvideTextStyle(textStyle) {
            // SIDE 版式下内容缩进一列，旁边留出头像槽；
            // 用 Row + weight 而不是把头像塞进外层 Column，气泡才不会被压成一竖列汉字。
            SideAvatarRow(
                enabled = sideAvatar,
                showAvatar = showSideSlot,
                mine = message.role == MessageRole.USER,
                role = message.role,
                model = model,
                assistant = assistant,
                loading = loading,
            ) {
                MessagePartsBlock(
                    assistant = assistant,
                    role = message.role,
                    parts = message.parts,
                    annotations = message.annotations,
                    loading = loading,
                    model = model,
                    onToolApproval = onToolApproval,
                    onToolAnswer = onToolAnswer,
                    onUserMessageClick = if (message.role == MessageRole.USER) onEdit else null,
                    // 长按自己的消息 = 打开「更多」操作单；药丸入口在单子里（见下方 onPill）。
                    // 助手气泡不给这个回调，长按保持无反应。
                    onUserMessageLongClick = if (message.role == MessageRole.USER) {
                        { showActionsSheet = true }
                    } else {
                        null
                    },
                )
 
                message.translation?.let { translation ->
                    CollapsibleTranslationText(
                        content = translation,
                        onClickCitation = {}
                    )
                }
            }
        }
 
        // 已挂药丸标记。放在气泡下方、操作行上方，和 ChatMessageEditedFiles 的
        // 文件 chip 用同一套视觉（Surface + RoundedCornerShape(50) + labelSmall），
        // 不新造一种 chip。只在真的挂了药丸时出现，平时不占位。
        if (message.role == MessageRole.USER && attachedPills.isNotEmpty()) {
            Row(
                modifier = Modifier.padding(top = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                attachedPills.forEach { code ->
                    val pill = PillRegistry.byCode(code) ?: return@forEach
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                    ) {
                        Text(
                            text = pill.name,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }

        val showActions = if (lastMessage) {
            !loading
        } else {
            message.parts.isEmptyUIMessage().not()
        }
 
        AnimatedVisibility(
            visible = showActions,
            enter = slideInVertically { it / 2 } + fadeIn(),
            exit = slideOutVertically { it / 2 } + fadeOut()
        ) {
            Column(
                modifier = Modifier.animateContentSize()
            ) {
                ChatMessageActionButtons(
                    message = message,
                    onRegenerate = onRegenerate,
                    node = node,
                    onUpdate = onUpdate,
                    onOpenActionSheet = {
                        showActionsSheet = true
                    },
                    onTranslate = onTranslate,
                    onClearTranslation = onClearTranslation
                )
            }
        }
 
        ProvideTextStyle(textStyle) {
            ChatMessageNerdLine(message = message)
        }
    }
    if (showPillSheet) {
        PillPickerSheet(
            messageId = message.id,
            onDismissRequest = { showPillSheet = false },
        )
    }
    if (showActionsSheet) {
        ChatMessageActionsSheet(
            message = message,
            onEdit = onEdit,
            onDelete = onDelete,
            onShare = onShare,
            onFork = onFork,
            model = model,
            onSelectAndCopy = {
                showSelectCopySheet = true
            },
            // 药丸入口：只有自己的消息能挂药丸；助手消息传 null ⇒ 单子里不出现这一项。
            onPill = if (message.role == MessageRole.USER) {
                { showPillSheet = true }
            } else {
                null
            },
            isFavorite = isFavorite,
            onToggleFavorite = onToggleFavorite,
            onWebViewPreview = {
                val textContent = message.parts
                    .filterIsInstance<UIMessagePart.Text>()
                    .joinToString("\n\n") { it.text }
                    .trim()
                if (textContent.isNotBlank()) {
                    val htmlContent = buildMarkdownPreviewHtml(
                        context = context,
                        markdown = textContent,
                        colorScheme = colorScheme
                    )
                    navController.navigate(Screen.WebView(content = htmlContent.base64Encode()))
                }
            },
            onDismissRequest = {
                showActionsSheet = false
            }
        )
    }
 
    if (showSelectCopySheet) {
        ChatMessageCopySheet(
            message = message,
            onDismissRequest = {
                showSelectCopySheet = false
            }
        )
    }
}
 
@OptIn(FlowPreview::class)
@Composable
private fun MessagePartsBlock(
    assistant: Assistant?,
    role: MessageRole,
    model: Model?,
    parts: List<UIMessagePart>,
    annotations: List<UIMessageAnnotation>,
    loading: Boolean,
    onToolApproval: ((toolCallId: String, approved: Boolean, reason: String) -> Unit)? = null,
    onToolAnswer: ((toolCallId: String, answer: String) -> Unit)? = null,
    onUserMessageClick: (() -> Unit)? = null,
    onUserMessageLongClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
 
    // 消息输出HapticFeedback
    val hapticFeedback = LocalHapticFeedback.current
    val displaySettings = LocalDisplaySettings.current
    val bubbleAlpha = 1f - displaySettings.chatBubbleTransparency / 100f
    // 助手气泡描边: 仅当未使用自定义背景图/自定义气泡色时才画细描边, 避免破坏用户自定义外观
    val assistantBubbleOutlined = displaySettings.assistantBubbleImagePath.isBlank() &&
        displaySettings.assistantBubbleColor == null
    val partsState by rememberUpdatedState(parts)
 
    val handleClickCitation: (String) -> Unit = remember {
        handler@{ citationId ->
            partsState.forEach { part ->
                if (part is UIMessagePart.Tool && part.toolName == "search_web" && part.isExecuted) {
                    val outputText = part.output.filterIsInstance<UIMessagePart.Text>().joinToString("\n") { it.text }
                    val items =
                        runCatching { JsonInstant.parseToJsonElement(outputText).jsonObject["items"]?.jsonArray }.getOrNull()
                            ?: return@forEach
                    items.forEach { item ->
                        val id = item.jsonObject["id"]?.jsonPrimitive?.content ?: return@forEach
                        val url = item.jsonObject["url"]?.jsonPrimitive?.content ?: return@forEach
                        if (citationId == id) {
                            context.openUrl(url)
                            return@handler
                        }
                    }
                }
            }
        }
    }
    LaunchedEffect(displaySettings) {
        snapshotFlow { partsState }
            .debounce(50.milliseconds)
            .collect { parts ->
                if (parts.isNotEmpty() && loading && displaySettings.enableMessageGenerationHapticEffect) {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.KeyboardTap)
                }
            }
    }
 
    // Render parts in original order (group thinking/tool as chain-of-thought)
    val groupedParts = remember(parts) { parts.groupMessageParts() }
    // 用户消息：把非文本附件（图片/视频/音频/文档）提到文本之前 ——
    // 「有图片时图片显示在文字气泡上方」（2026-10-07 批 79 要求）。
    // 只在渲染层重排分组先后，不动 UIMessage.parts，也不动存储顺序；
    // 助手消息分区顺序原样保留（思考/工具链顺序敏感）。
    val orderedParts = remember(groupedParts, role) {
        if (role == MessageRole.USER) {
            val attachments = groupedParts.filter { b ->
                b is MessagePartBlock.ContentBlock && b.part !is UIMessagePart.Text
            }
            val texts = groupedParts.filter { b ->
                b !is MessagePartBlock.ContentBlock || b.part is UIMessagePart.Text
            }
            attachments + texts
        } else {
            groupedParts
        }
    }
    orderedParts.fastForEachIndexed { partIndex, block ->
        when (block) {
            is MessagePartBlock.ThinkingBlock -> {
                if (block.steps.isNotEmpty()) {
                    val isReasoningOnlyBlock = block.steps.fastAll { it is ThinkingStep.ReasoningStep }
                    ChainOfThought(
                        modifier = Modifier.animateContentSize(),
                        // 思考气泡的槽位按主题分：默认跟助手气泡共用 surfaceContainerHigh，
                        // 单独定义过思考气泡色的主题（见 Theme.kt）改读 tertiaryContainer。
                        cardColors = CardDefaults.cardColors(
                            containerColor = if (LocalSettings.current.themeId in THEME_THINKING_CONTAINER_THEMES) {
                                MaterialTheme.colorScheme.tertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                        ),
                        steps = block.steps,
                        collapsedAdaptiveWidth = isReasoningOnlyBlock,
                    ) { step ->
                        when (step) {
                            is ThinkingStep.ReasoningStep -> {
                                key(step.reasoning.createdAt) {
                                    ChatMessageReasoningStep(
                                        reasoning = step.reasoning,
                                        model = model,
                                        assistant = assistant,
                                        collapsedAdaptiveWidth = isReasoningOnlyBlock,
                                    )
                                }
                            }
 
                            is ThinkingStep.ToolStep -> {
                                key(step.tool.toolCallId.ifBlank { step.hashCode().toString() }) {
                                    ChatMessageToolStep(
                                        tool = step.tool,
                                        loading = loading && !step.tool.isExecuted,
                                        allParts = parts,
                                        onToolApproval = onToolApproval,
                                        onToolAnswer = onToolAnswer,
                                    )
                                }
                            }
                        }
                    }
                }
            }
 
            is MessagePartBlock.ContentBlock -> key(block.index) {
                when (val part = block.part) {
                    is UIMessagePart.Text -> {
                        // 从显示文本中移除[zip:...]标记
                        val displayText = remember(part.text) {
                            part.text.replace(Regex("\\[zip:[^\\]]+\\]", RegexOption.IGNORE_CASE), "")
                        }
                        
                        // 关掉「语音条旁保留文字」时，有语音条就整块不渲染，避免一条回复占两份屏幕。
                        // 判断放在 SelectionContainer 外面：留一个空容器的话，父 Column 的 spacedBy
                        // 仍会为它算一份间距，把语音条整体往下推歪。语音条仍在后面的 part 分支渲染。
                        val hideTextForVoice = role == MessageRole.ASSISTANT &&
                            !displaySettings.showTextWithVoiceMessage &&
                            parts.any { it is UIMessagePart.VoiceMessage }
                        if (!hideTextForVoice) {
                        SelectionContainer {
                            Column {
                                if (role == MessageRole.USER) {
                                    if (!displaySettings.showUserBubble) {
                                        MarkdownBlock(
                                            content = displayText.replaceRegexes(
                                                assistant = assistant,
                                                scope = AssistantAffectScope.USER,
                                                visual = true,
                                            ),
                                            onClickCitation = handleClickCitation,
                                            modifier = Modifier.animateContentSize(),
                                        )
                                    } else if (assistant?.splitUserBubbleByLine == true) {
                                        // 分气泡: 按用户输入的换行 (\n) 拆成多个独立气泡,
                                        // 拆分逻辑见 splitIntoBubbleSegments (会保护代码块/表格内部的换行)
                                        val bubbleSegments = remember(displayText) {
                                            displayText.splitIntoBubbleSegments()
                                        }
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(4.dp),
                                            horizontalAlignment = Alignment.End,
                                        ) {
                                            bubbleSegments.fastForEachIndexed { segIndex, segment ->
                                                key(segIndex) {
                                                    BubbleSurface(
                                                        imagePath = displaySettings.userBubbleImagePath,
                                                        cornerRadius = displaySettings.bubbleCornerRadius.dp,
                                                        color = displaySettings.userBubbleColor?.let { it.toComposeColor() } ?: MaterialTheme.colorScheme.secondaryContainer,
                                                        overlayEnabled = displaySettings.bubbleImageOverlayEnabled,
                                                        bubbleAlpha = bubbleAlpha,
                                                        isUser = true,
                                                        onClick = { onUserMessageClick?.invoke() },
                                                        onLongClick = onUserMessageLongClick,
                                                    ) {
                                                        MarkdownBlock(
                                                            content = segment.replaceRegexes(
                                                                assistant = assistant,
                                                                scope = AssistantAffectScope.USER,
                                                                visual = true,
                                                            ),
                                                            onClickCitation = handleClickCitation
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        BubbleSurface(
                                            imagePath = displaySettings.userBubbleImagePath,
                                            cornerRadius = displaySettings.bubbleCornerRadius.dp,
                                            color = displaySettings.userBubbleColor?.let { it.toComposeColor() } ?: MaterialTheme.colorScheme.secondaryContainer,
                                            overlayEnabled = displaySettings.bubbleImageOverlayEnabled,
                                            bubbleAlpha = bubbleAlpha,
                                            isUser = true,
                                            onClick = { onUserMessageClick?.invoke() },
                                            onLongClick = onUserMessageLongClick,
                                        ) {
                                            MarkdownBlock(
                                                content = displayText.replaceRegexes(
                                                    assistant = assistant,
                                                    scope = AssistantAffectScope.USER,
                                                    visual = true,
                                                ),
                                                onClickCitation = handleClickCitation
                                            )
                                        }
                                    }
                                } else if (assistant?.splitBubbleByLine == true) {
                                    // 分气泡: 按模型自己写的换行 (\n) 拆成多个独立气泡,
                                    // 拆分逻辑见 splitIntoBubbleSegments (会保护代码块/表格内部的换行)
                                    val bubbleSegments = remember(displayText) {
                                        displayText.splitIntoBubbleSegments()
                                    }
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        bubbleSegments.fastForEachIndexed { segIndex, segment ->
                                            key(segIndex) {
                                                if (displaySettings.showAssistantBubble) {
                                                    BubbleSurface(
                                                        imagePath = displaySettings.assistantBubbleImagePath,
                                                        cornerRadius = displaySettings.bubbleCornerRadius.dp,
                                                        color = displaySettings.assistantBubbleColor?.let { it.toComposeColor() } ?: MaterialTheme.colorScheme.surfaceContainerHigh,
                                                        overlayEnabled = displaySettings.bubbleImageOverlayEnabled,
                                                        bubbleAlpha = bubbleAlpha,
                                                        isUser = false,
                                                        outlined = assistantBubbleOutlined,
                                                    ) {
                                                        MarkdownBlock(
                                                            content = segment.replaceRegexes(
                                                                assistant = assistant,
                                                                scope = AssistantAffectScope.ASSISTANT,
                                                                visual = true,
                                                            ),
                                                            onClickCitation = handleClickCitation,
                                                        )
                                                    }
                                                } else {
                                                    MarkdownBlock(
                                                        content = segment.replaceRegexes(
                                                            assistant = assistant,
                                                            scope = AssistantAffectScope.ASSISTANT,
                                                            visual = true,
                                                        ),
                                                        onClickCitation = handleClickCitation,
                                                        modifier = Modifier
                                                            .animateContentSize()
                                                    )
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    if (displaySettings.showAssistantBubble) {
                                        BubbleSurface(
                                            imagePath = displaySettings.assistantBubbleImagePath,
                                            cornerRadius = displaySettings.bubbleCornerRadius.dp,
                                            color = displaySettings.assistantBubbleColor?.let { it.toComposeColor() } ?: MaterialTheme.colorScheme.surfaceContainerHigh,
                                            overlayEnabled = displaySettings.bubbleImageOverlayEnabled,
                                            bubbleAlpha = bubbleAlpha,
                                            isUser = false,
                                            outlined = assistantBubbleOutlined,
                                        ) {
                                            MarkdownBlock(
                                                content = displayText.replaceRegexes(
                                                    assistant = assistant,
                                                    scope = AssistantAffectScope.ASSISTANT,
                                                    visual = true,
                                                ),
                                                onClickCitation = handleClickCitation,
                                            )
                                        }
                                    } else {
                                        MarkdownBlock(
                                            content = displayText.replaceRegexes(
                                                assistant = assistant,
                                                scope = AssistantAffectScope.ASSISTANT,
                                                visual = true,
                                            ),
                                            onClickCitation = handleClickCitation,
                                            modifier = Modifier
                                                .animateContentSize()
                                        )
                                    }
                                }
                            }
                        }
                        }
                    }
 
                    is UIMessagePart.Video -> {
                        Surface(
                            tonalElevation = 2.dp,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                intent.data = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    part.url.toUri().toFile()
                                )
                                val chooserIndent = Intent.createChooser(intent, null)
                                context.startActivity(chooserIndent)
                            },
                            modifier = Modifier,
                            shape = RoundedCornerShape(8.dp),
                        ) {
                            Box(modifier = Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                                Icon(HugeIcons.Video01, null)
                            }
                        }
                    }
 
                    is UIMessagePart.Audio -> {
                        AudioPlayerBubble(url = part.url)
                    }
 
                    is UIMessagePart.VoiceMessage -> {
                        // 语音条跟同一条消息的文本气泡共用圆角、配色与透明度，
                        // 这样两种消息版式（署名行 / 气泡侧边）下它都落在气泡该在的位置上。
                        VoiceMessageBubble(
                            voiceMessage = part,
                            isUser = role == MessageRole.USER,
                            cornerRadius = displaySettings.bubbleCornerRadius.dp,
                            bubbleColor = if (role == MessageRole.USER) {
                                displaySettings.userBubbleColor?.let { it.toComposeColor() }
                                    ?: MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                displaySettings.assistantBubbleColor?.let { it.toComposeColor() }
                                    ?: MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            bubbleAlpha = bubbleAlpha,
                            outlined = role != MessageRole.USER && assistantBubbleOutlined,
                        )
                    }
 
                    is UIMessagePart.Image -> {
                        val isImageLoading =
                            part.url.isBlank() || part.url.matches(Regex("^data:image/[^;]*;base64,\\s*$"))
                        if (isImageLoading) {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .shimmer(isLoading = true)
                            )
                        } else {
                            ZoomableAsyncImage(
                                model = part.url,
                                contentDescription = null,
                                modifier = Modifier
                                    .clip(MaterialTheme.shapes.medium)
                                    .height(72.dp)
                            )
                        }
                    }
 
                    is UIMessagePart.Document -> {
                        Surface(
                            tonalElevation = 2.dp,
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW)
                                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                intent.data = FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    part.url.toUri().toFile()
                                )
                                val chooserIndent = Intent.createChooser(intent, null)
                                context.startActivity(chooserIndent)
                            },
                            modifier = Modifier,
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            ProvideTextStyle(MaterialTheme.typography.labelSmall) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    when (part.mime) {
                                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> {
                                            Icon(
                                                painter = painterResource(R.drawable.docx),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
 
                                        "application/pdf" -> {
                                            Icon(
                                                painter = painterResource(R.drawable.pdf),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
 
                                        else -> {
                                            Icon(
                                                imageVector = HugeIcons.File02,
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
 
                                    Text(
                                        text = part.fileName,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.widthIn(max = 200.dp)
                                    )
                                }
                            }
                        }
                    }
 
                    else -> {
                        // Skip unknown part types (e.g., deprecated ToolCall, ToolResult, Search)
                    }
                }
                // 用户消息：附件与**其后紧跟的文字**之间补足到 8dp ——
                // 外层容器统一 spacedBy(4dp)，这里再加 4dp。判据是「下一个 block 是文字」，
                // 这样附件与附件之间仍是 4dp，只有「附件 -> 文字」那一处变成 8dp。
                if (role == MessageRole.USER &&
                    block.part !is UIMessagePart.Text &&
                    orderedParts.getOrNull(partIndex + 1)?.let { next ->
                        next is MessagePartBlock.ContentBlock && next.part is UIMessagePart.Text
                    } == true
                ) {
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }
    }
 
    // Annotations (always rendered at the end)
    if (annotations.isNotEmpty()) {
        Column(
            modifier = Modifier.animateContentSize(),
        ) {
            var expand by remember { mutableStateOf(false) }
            if (expand) {
                ProvideTextStyle(
                    MaterialTheme.typography.labelMedium.copy(
                        color = MaterialTheme.extendColors.gray8.copy(alpha = 0.65f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .drawWithContent {
                                drawContent()
                                drawRoundRect(
                                    color = contentColor.copy(alpha = 0.2f),
                                    size = Size(width = 10f, height = size.height),
                                )
                            }
                            .padding(start = 16.dp)
                            .padding(4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        annotations.fastForEachIndexed { index, annotation ->
                            when (annotation) {
                                is UIMessageAnnotation.UrlCitation -> {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Favicon(annotation.url, modifier = Modifier.size(20.dp))
                                        Text(
                                            text = buildAnnotatedString {
                                                append("${index + 1}. ")
                                                withLink(LinkAnnotation.Url(annotation.url)) {
                                                    append(annotation.title.urlDecode())
                                                }
                                            }
                                        )
                                    }
                                }

                                // 其余注解不在这里渲染：语音条走 parts 里的 VoiceMessage，
                                // TTS 音频缓存和通话记录各有自己的展示位置。
                                else -> Unit
                            }
                        }
                    }
                }
            }
            TextButton(
                onClick = {
                    expand = !expand
                }
            ) {
                Text(stringResource(R.string.citations_count, annotations.size))
            }
        }
    }
 
    // 工作区文件 chip: assistant 消息下方展示被 workspace_write_file/
    // workspace_edit_file 写入/编辑的文件, 点击可导出/分享。
    // 仅在归属工作区的 assistant 消息中渲染, 不影响用户消息和其它布局。
    if (role == MessageRole.ASSISTANT) {
        EditedFilesList(parts = parts, assistant = assistant)
    }
}
 
/**
 * 消息气泡容器。
 *
 * @param isUser 是否为用户气泡（右对齐）。用户气泡右下角收窄，助手气泡左下角收窄，形成非对称造型。
 * @param outlined 是否绘制细描边。仅在助手气泡使用默认背景（无自定义背景图/自定义气泡色）时传 true。
 */
@Composable
private fun BubbleSurface(
    imagePath: String,
    cornerRadius: Dp,
    color: Color,
    overlayEnabled: Boolean,
    bubbleAlpha: Float,
    isUser: Boolean = false,
    outlined: Boolean = false,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    // 长按入口（批 63 起：打开「更多」操作单，药丸在单子里）。长按必须跟点击挂在**同一条**
    // modifier 链上 —— 外面再包一层 Box 用 combinedClickable 是不行的：内层 clickable
    // 会在 Main pass 先消费掉 down，外层拿到的是 consumed 事件，onLongClick 永不触发。
    //
    // 两个 local 而不是一个，因为前三个材质分支与默认分支原本的语义不同：
    // 前三个是「onClick 非空才有点击」，默认分支走 Surface 的 onClick 重载、那个重载
    // **总是**挂一层 clickable（onClick 为 null 时是空 lambda）。共用一个 local 会让
    // 助手气泡在默认材质下丢掉那层无操作 ripple，点击还会穿透出去。
    val clickHandler = onClick
    val bubbleClickModifier: Modifier = if (onLongClick != null) {
        Modifier.combinedClickable(onClick = clickHandler ?: {}, onLongClick = onLongClick)
    } else if (clickHandler != null) {
        Modifier.clickable(onClick = clickHandler)
    } else {
        Modifier
    }
    val surfaceClickModifier: Modifier = if (onLongClick != null) {
        Modifier.combinedClickable(onClick = clickHandler ?: {}, onLongClick = onLongClick)
    } else {
        Modifier.clickable(onClick = clickHandler ?: {})
    }
    val materialMode = LocalMaterialMode.current
    val effectiveAlpha = when (materialMode) {
        DisplayMaterialMode.TRANSLUCENT -> TRANSLUCENT_BUBBLE_BASE_ALPHA * bubbleAlpha
        DisplayMaterialMode.GLASS -> bubbleAlpha
        DisplayMaterialMode.FOLLOW_THEME,
        DisplayMaterialMode.FLAT -> bubbleAlpha
    }
    val glassBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = GLASS_BUBBLE_BORDER_ALPHA)
    val translucentBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = TRANSLUCENT_BUBBLE_BORDER_ALPHA)
    val glassFillModifier = Modifier.drawWithCache {
        val gradientCenter = Offset(size.width / 2f, size.height / 2f)
        val glassFillBrush = Brush.radialGradient(
            colorStops = arrayOf(
                // 0.82/0.80/0.56/0.40 -> 0.72/0.70/0.58/0.48。
                // 原来中心到边缘掉了 0.42，这个落差读起来是"一块有厚度的板" ——
                // 助手气泡的卡片感就出在这里。收窄之后是"一层薄玻璃"，透而不空。
                0f to color.copy(alpha = 0.72f * bubbleAlpha),
                0.55f to color.copy(alpha = 0.70f * bubbleAlpha),
                0.82f to color.copy(alpha = 0.58f * bubbleAlpha),
                1f to color.copy(alpha = 0.48f * bubbleAlpha),
            ),
            center = gradientCenter,
            radius = gradientCenter.getDistance(),
        )
        onDrawBehind {
            drawRect(glassFillBrush)
        }
    }
    // 静态玻璃高光：顶部泛白 / 底部反光 / 顶沿镜面线三处一起压下去。
    // 它们是"塑料包边"的直接来源 —— 叠在同一圈边缘上时，眼睛读到的是"包边"
    // 而不是"玻璃"。减 FX 就减在这里：不新增层，只把现有层的强度降下来。
    val glassHighlightModifier = Modifier.drawWithCache {
        val topHighlightDepth = 6.dp.toPx()
        val bottomHighlightDepth = 4.dp.toPx()
        val specularHighlightTop = 1.dp.toPx()
        val specularHighlightHeight = 1.dp.toPx()
        val glassTopHighlightBrush = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to Color.White.copy(alpha = 0.09f),
                0.5f to Color.White.copy(alpha = 0.03f),
                1f to Color.Transparent,
            ),
            endY = topHighlightDepth,
        )
        val glassBottomHighlightBrush = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                0.5f to Color.White.copy(alpha = 0.02f),
                1f to Color.White.copy(alpha = 0.045f),
            ),
            startY = size.height - bottomHighlightDepth,
            endY = size.height,
        )
        val glassSpecularHighlightBrush = Brush.linearGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                0.10f to Color.White.copy(alpha = 0.05f),
                0.28f to Color.White.copy(alpha = 0.15f),
                0.52f to Color.White.copy(alpha = 0.08f),
                0.78f to Color.White.copy(alpha = 0.025f),
                1f to Color.Transparent,
            ),
            start = Offset.Zero,
            end = Offset(size.width, 0f),
        )
        onDrawBehind {
            drawRect(
                brush = glassTopHighlightBrush,
                size = Size(size.width, minOf(size.height, topHighlightDepth)),
            )
            drawRect(
                brush = glassBottomHighlightBrush,
                topLeft = Offset(0f, maxOf(0f, size.height - bottomHighlightDepth)),
                size = Size(size.width, minOf(size.height, bottomHighlightDepth)),
            )
            drawRect(
                brush = glassSpecularHighlightBrush,
                topLeft = Offset(0f, specularHighlightTop),
                size = Size(
                    width = size.width,
                    height = minOf(specularHighlightHeight, size.height - specularHighlightTop),
                ),
            )
        }
    }
    // 昼夜状态：与应用实际 colorScheme 一致（SYSTEM→系统；LIGHT/DARK 手动覆盖）
    val isDarkTheme = LocalDarkMode.current
    // 顶沿内高光：紧贴上边缘的 1px 亮线，模拟玻璃板的厚度切面。
    // CSS 里就是 box-shadow 的 inset 0 1px 0 —— 它跟 border 的区别在于只有顶边一条，
    // 眼睛会把它读成"这块板有厚度"，而四边均匀的描边只会读成"一个框"。
    val glassInsetTopHighlightModifier = Modifier.drawWithCache {
        val lineHeight = 1.dp.toPx()
        val lineAlpha = if (isDarkTheme) 0.09f else 0.30f
        onDrawBehind {
            drawRect(
                color = Color.White.copy(alpha = lineAlpha),
                size = Size(size.width, minOf(lineHeight, size.height)),
            )
        }
    }
    // 气泡实际轮廓：玻璃/液态玻璃的贴边高光必须照它走，否则会在圆角处错位并被父级 clip 切掉一截。
    // 非对称造型：用户气泡右下角收窄、助手气泡左下角收窄，指向各自的头像一侧。
    // 圆角保留，但要封顶。
    //
    // 半径一旦够到气泡高度的一半，短消息整体就读成"胶囊"了 —— 那是输入框的语言，
    // 不是气泡的语言。单行气泡高约 36dp（半高 18dp），上限取 14dp 时边缘依然圆润，
    // 但始终留着一段直边，形状明确是"圆角矩形"。下限 6dp 兜住任何过小的设置值，
    // 避免退化成直角。
    //
    // 注意不要沿用设置里的字段名（bubbleCornerRadius 是 DisplaySettings 的属性），
    // 这里必须是一个独立的局部值。
    val clampedCornerRadius = cornerRadius.coerceIn(6.dp, 14.dp)
    val shape = if (isUser) {
        RoundedCornerShape(
            topStart = clampedCornerRadius,
            topEnd = clampedCornerRadius,
            bottomEnd = clampedCornerRadius * 0.25f,
            bottomStart = clampedCornerRadius,
        )
    } else {
        RoundedCornerShape(
            topStart = clampedCornerRadius,
            topEnd = clampedCornerRadius,
            bottomEnd = clampedCornerRadius,
            bottomStart = clampedCornerRadius * 0.25f,
        )
    }
    val bubbleLayoutDirection = LocalLayoutDirection.current
    // 玻璃气泡的投影。
    //
    // 不能用 Modifier.shadow：那条路必须配 clip=false（半透明气泡的阴影得落在自身之外），
    // 而 clip=false 时 elevation 阴影是按 graphicsLayer 的矩形边界渲染的，圆角外侧会
    // 漏出四个方形暗角。这里改成自己画：按真实轮廓生成 Path，用几层递减的半透明黑
    // 描边往外扩，得到一圈跟着圆角走的柔和投影。
    //
    // 两个关键约束，破一个就会出现"圆角气泡配方角投影"：
    // 1) 这个 modifier 必须挂在 animateContentSize() 之前。animateContentSize 内部是
    //    `clipToBounds() then 尺寸动画`，它会把之后所有绘制裁到矩形边界内。阴影是往
    //    轮廓外扩的，直边外侧那段正好贴着边界被裁掉，只剩"矩形角 ∩ 圆角外侧"的四小块
    //    残留 —— 看起来就是四个角各挂一块方形暗块。
    // 2) 描边只保留轮廓外侧那一半（clipPath + Difference 把气泡内部挖掉）。居中描边的
    //    内半段会压在半透明玻璃填充下面，把边缘一圈染深，那就是"发光塑料膜"的来源。
    val glassShadowModifier = { elevation: Dp ->
        Modifier.drawWithCache {
            val spread = elevation.toPx()
            // 6 层、每层极淡。阴影在这里只负责"让气泡脱离背景一点点"，
            // 不参与材质表达 —— 那件事交给玻璃面自己。
            val layers = 6
            val outline = shape.createOutline(
                size = size,
                layoutDirection = bubbleLayoutDirection,
                density = this@drawWithCache,
            )
            // 气泡自身轮廓：用来把内部挖掉，保证阴影不侵入填充
            val bubblePath = Path().apply { addOutline(outline) }
            // 阴影轮廓：整体下移一点，让光源看起来在上方，投影才有"浮起"的方向感
            val shadowPath = Path().apply {
                addOutline(outline)
                translate(Offset(0f, spread * 0.22f))
            }
            onDrawBehind {
                clipPath(path = bubblePath, clipOp = ClipOp.Difference) {
                    // 从外往内画：最外层最淡最宽，逐层收窄加深，叠出渐变的衰减
                    for (i in layers downTo 1) {
                        val fraction = i.toFloat() / layers
                        drawPath(
                            path = shadowPath,
                            color = Color.Black.copy(alpha = 0.014f * (1f - fraction) + 0.004f),
                            style = Stroke(width = spread * fraction * 2f),
                        )
                    }
                }
            }
        }
    }
    val hasImage = imagePath.isNotBlank() && java.io.File(imagePath).exists()
    if (materialMode == DisplayMaterialMode.GLASS) {
        Box(
            modifier = Modifier
                // 同液态玻璃：手绘圆角投影，且必须排在 animateContentSize 之前，
                // 否则外扩投影会被它的 clipToBounds 裁成四个方角。
                .then(glassShadowModifier(GLASS_SHADOW_ELEVATION))
                .animateContentSize()
                .clip(shape)
                .then(bubbleClickModifier)
                .border(1.dp, glassBorderColor, shape)
        ) {
            if (hasImage) {
                AsyncImage(
                    model = imagePath,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }
            if (!hasImage || overlayEnabled) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .then(glassFillModifier)
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(glassHighlightModifier)
            )
            // 顶沿内高光：同液态玻璃，压在所有层之上代表玻璃上切面
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(glassInsetTopHighlightModifier)
            )
            // 上下 8dp -> 6dp。单行气泡高度约 36dp，其中 16dp 是纵向留白，
            // 读起来就是"一块厚片"；收到 12dp 后短消息明显紧凑。
            // 左右保持 8dp 不动 —— 长消息的阅读宽度由它决定，收左右等于直接变窄。
            // 横向 8 -> 10：文字不再贴到气泡边缘，短句不再显得「被框住」。
            // 纵向 6 -> 6 保持不变：高度不动，避免影响既有版式。
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                content()
            }
        }
    } else if (hasImage) {
        Box(
            modifier = Modifier
                .animateContentSize()
                .clip(shape)
                .then(bubbleClickModifier)
                .then(
                    if (materialMode == DisplayMaterialMode.TRANSLUCENT) {
                        Modifier.border(1.dp, translucentBorderColor, shape)
                    } else {
                        Modifier
                    }
                )
        ) {
            AsyncImage(
                model = imagePath,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )
            if (overlayEnabled) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(color.copy(alpha = effectiveAlpha))
                )
            }
            // 上下 8dp -> 6dp。单行气泡高度约 36dp，其中 16dp 是纵向留白，
            // 读起来就是"一块厚片"；收到 12dp 后短消息明显紧凑。
            // 左右保持 8dp 不动 —— 长消息的阅读宽度由它决定，收左右等于直接变窄。
            // 横向 8 -> 10：文字不再贴到气泡边缘，短句不再显得「被框住」。
            // 纵向 6 -> 6 保持不变：高度不动，避免影响既有版式。
            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                content()
            }
        }
    } else {
        Surface(
            modifier = Modifier
                .animateContentSize()
                // 这个 minimumInteractiveComponentSize 原本由 Surface 的 onClick 重载
                // 自带，换成非点击重载后必须自己补回来，否则短气泡（「嗯」这种）
                // 的最小可点区域会从 48dp 缩到内容高度。
                .minimumInteractiveComponentSize(),
            shape = shape,
            // 用户气泡与助手气泡在这里吃同一个 PLAIN_BUBBLE_ALPHA，
            // 两种气泡的"底"由此对齐到同一套材质语言。
            color = color.copy(
                alpha = if (materialMode == DisplayMaterialMode.TRANSLUCENT) {
                    effectiveAlpha
                } else {
                    effectiveAlpha * PLAIN_BUBBLE_ALPHA
                },
            ),
            border = if (materialMode == DisplayMaterialMode.TRANSLUCENT) {
                BorderStroke(1.dp, translucentBorderColor)
            } else if (outlined) {
                // 助手气泡默认背景时的一道细描边。
                // 0.5 -> 0.18：这一圈正是"塑料包边"的来源，而且只有助手气泡有，
                // 于是又成了两种气泡材质不统一的地方。留一点点即可 —— 助手气泡的
                // 填充色本来就接近背景，需要一根发丝把它托起来；用户气泡的填充色
                // 已经自带对比，就不需要描边。描边与否取决于填充对比度，而不是发送方。
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.18f))
            } else {
                null
            },
        ) {
            // 点击 / 长按都挂在**这一层**，也就是 Surface 的 surface() 节点里面。
            // Surface 的 onClick 重载正是这么放的：clickable 在 surface() 之内，
            // 涟漪才画在气泡底色之上。换成把 clickable 放到 Surface 外面，涟漪会被
            // 底色盖住、等于没有反馈 —— 所以这里保留 Surface 只做底与描边，
            // 点击单独交给里面的 Box。
            Box(modifier = surfaceClickModifier) {
                // 上下 8dp -> 6dp。单行气泡高度约 36dp，其中 16dp 是纵向留白，
                // 读起来就是"一块厚片"；收到 12dp 后短消息明显紧凑。
                // 左右保持 8dp 不动 —— 长消息的阅读宽度由它决定，收左右等于直接变窄。
                // 横向 8 -> 10：文字不再贴到气泡边缘，短句不再显得「被框住」。
                // 纵向 6 -> 6 保持不变：高度不动，避免影响既有版式。
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    content()
                }
            }
        }
    }
}

private const val TRANSLUCENT_BUBBLE_BASE_ALPHA = 0.72f
// 两种材质的描边统一到 0.07。描边在这里只有一个职责：把气泡从同色背景上
// 轻轻托起来。0.12~0.24 那一档已经能被看成"一圈边"，而两种材质各自不同的
// 数值正是"用户/助手材质不统一"的来源之一。
private const val TRANSLUCENT_BUBBLE_BORDER_ALPHA = 0.07f
private const val GLASS_BUBBLE_BORDER_ALPHA = 0.07f

/**
 * 实心模式（FLAT / FOLLOW_THEME）下气泡填充的额外透度。
 *
 * 完全不透明会把气泡读成"一块贴在背景上的色卡" —— 用户气泡的"胶囊感"、
 * 助手气泡的"卡片感"都来自这里。0.92 让底下页面极淡地透过来一点，气泡于是
 * 变成"带材质色的玻璃片"，同时几乎不影响文字对比度。
 *
 * 只在实心模式下生效：TRANSLUCENT 的 effectiveAlpha 本身已经压到 0.72，
 * 再乘一次会叠成 0.66，那就不是"轻微半透明"而是发虚了。
 */
private const val PLAIN_BUBBLE_ALPHA = 0.92f

/**
 * GLASS 材质气泡的投影高度。
 *
 * 4dp -> 2dp，与液态玻璃取同一个值：用户气泡和助手气泡必须落在同一条
 * "离背景的距离"上，否则两种气泡的材质语言立刻又分家了。
 */
private val GLASS_SHADOW_ELEVATION = 2.dp
 
@Composable
@Suppress("UnusedCrossTarget")
internal fun AudioPlayerBubble(url: String) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var durationMs by remember { mutableIntStateOf(0) }
    var currentMs by remember { mutableIntStateOf(0) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPrepared by remember { mutableStateOf(false) }
 
    // Generate pseudo-random waveform bar heights (deterministic per url)
    val waveformBars = remember(url) {
        val rnd = java.util.Random(url.hashCode().toLong())
        List(40) { 0.15f + rnd.nextFloat() * 0.85f }
    }
 
    val progress = if (durationMs > 0) currentMs.toFloat() / durationMs else 0f
 
    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }
 
    // Progress ticker
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    currentMs = it.currentPosition
                }
            }
            kotlinx.coroutines.delay(50)
        }
    }
 
    // Animate waveform bars when playing
    val animatedBars = remember { mutableStateOf(waveformBars) }
    LaunchedEffect(isPlaying, progress) {
        if (isPlaying) {
            val rnd = java.util.Random()
            val newBars = waveformBars.mapIndexed { index, base ->
                val playedRatio = if (progress > 0f) index.toFloat() / waveformBars.size else 0f
                if (playedRatio <= progress) {
                    // Already played bars stay at original height
                    base
                } else {
                    // Upcoming bars get slight animation
                    base * (0.85f + rnd.nextFloat() * 0.3f)
                }
            }
            animatedBars.value = newBars
        } else {
            animatedBars.value = waveformBars
        }
    }
 
    val activeColor = MaterialTheme.colorScheme.primary
    val inactiveColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
 
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer)
            .padding(start = 4.dp, end = 10.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Play / Pause button
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .clickable {
                    if (isPlaying) {
                        mediaPlayer?.pause()
                        isPlaying = false
                    } else {
                        if (mediaPlayer == null || !isPrepared) {
                            val mp = MediaPlayer()
                            try {
                                val uri = android.net.Uri.parse(url)
                                mp.setDataSource(context, uri)
                                mp.prepare()
                                durationMs = mp.duration
                                mp.setOnCompletionListener {
                                    isPlaying = false
                                    currentMs = 0
                                }
                                mp.start()
                                isPlaying = true
                                isPrepared = true
                                mediaPlayer = mp
                            } catch (e: Exception) {
                                mp.release()
                            }
                        } else {
                            mediaPlayer?.start()
                            isPlaying = true
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isPlaying) HugeIcons.PauseCircle else HugeIcons.PlayCircle,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
 
        Spacer(modifier = Modifier.width(8.dp))
 
        // Waveform bars
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(28.dp)
                .clickable { /* click waveform to seek (optional future) */ }
        ) {
            val barCount = animatedBars.value.size
            val totalWidth = size.width
            val barWidth = 2.5f
            val gap = (totalWidth - barWidth * barCount) / (barCount - 1).coerceAtLeast(1)
            val playedBarCount = (progress * barCount).toInt()
 
            animatedBars.value.forEachIndexed { index, barRatio ->
                val barHeight = size.height * barRatio.coerceIn(0.15f, 1f)
                val x = index * (barWidth + gap)
                val y = (size.height - barHeight) / 2f
                drawRoundRect(
                    color = if (index < playedBarCount) activeColor else inactiveColor,
                    topLeft = androidx.compose.ui.geometry.Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f, 1.5f)
                )
            }
        }
 
        Spacer(modifier = Modifier.width(6.dp))
 
        // Duration text
        val displaySec = if (isPlaying || currentMs > 0) {
            val remaining = (durationMs - currentMs) / 1000
            remaining.coerceAtLeast(0)
        } else {
            durationMs / 1000
        }
        Text(
            text = String.format("%d:%02d", displaySec / 60, displaySec % 60),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontSize = 13.sp,
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End
        )
    }
}
 
@Composable
internal fun VoiceMessageBubble(
    voiceMessage: UIMessagePart.VoiceMessage,
    isUser: Boolean,
    cornerRadius: Dp = 16.dp,
    bubbleColor: Color? = null,
    bubbleAlpha: Float = 1f,
    outlined: Boolean = false,
) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var positionMs by remember(voiceMessage.url) { mutableIntStateOf(0) }

    // 解码取真实振幅包络。IO 上做，解不出来就留空，VoiceWaveform 会画等高矮墙。
    var amplitudes by remember(voiceMessage.url) { mutableStateOf<List<Float>>(emptyList()) }
    LaunchedEffect(voiceMessage.url) {
        val path = voiceMessage.url.toUri().path ?: return@LaunchedEffect
        amplitudes = withContext(Dispatchers.IO) {
            extractAmplitudeEnvelope(java.io.File(path), buckets = 28)
        }
    }

    val progress = if (voiceMessage.duration > 0) {
        positionMs.toFloat() / voiceMessage.duration
    } else 0f
 
    val durationSec = (voiceMessage.duration / 1000).coerceAtLeast(1)
 
    DisposableEffect(voiceMessage.url) {
        onDispose {
            mediaPlayer?.release()
            mediaPlayer = null
        }
    }
 
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            mediaPlayer?.let {
                if (it.isPlaying) {
                    // 原来这个循环只判完播，不推进度，所以波形永远不染色
                    positionMs = it.currentPosition
                } else {
                    isPlaying = false
                }
            }
            kotlinx.coroutines.delay(50)
        }
    }
 
    // 与文本气泡同一套非对称造型：用户右下角收窄、助手左下角收窄，指向各自头像一侧。
    // 圆角保留，但要封顶。
    //
    // 半径一旦够到气泡高度的一半，短消息整体就读成"胶囊"了 —— 那是输入框的语言，
    // 不是气泡的语言。单行气泡高约 36dp（半高 18dp），上限取 14dp 时边缘依然圆润，
    // 但始终留着一段直边，形状明确是"圆角矩形"。下限 6dp 兜住任何过小的设置值，
    // 避免退化成直角。
    //
    // 注意不要沿用设置里的字段名（bubbleCornerRadius 是 DisplaySettings 的属性），
    // 这里必须是一个独立的局部值。
    val clampedCornerRadius = cornerRadius.coerceIn(6.dp, 14.dp)
    val shape = if (isUser) {
        RoundedCornerShape(
            topStart = clampedCornerRadius,
            topEnd = clampedCornerRadius,
            bottomEnd = clampedCornerRadius * 0.25f,
            bottomStart = clampedCornerRadius,
        )
    } else {
        RoundedCornerShape(
            topStart = clampedCornerRadius,
            topEnd = clampedCornerRadius,
            bottomEnd = clampedCornerRadius,
            bottomStart = clampedCornerRadius * 0.25f,
        )
    }
    val resolvedColor = bubbleColor ?: if (isUser) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
    }
    // 半透明材质下跟文本气泡吃同一个基础透明度，否则同一条消息里语音条会比文字气泡明显更实。
    val voiceMaterialMode = LocalMaterialMode.current
    val effectiveAlpha = if (voiceMaterialMode == DisplayMaterialMode.TRANSLUCENT) {
        TRANSLUCENT_BUBBLE_BASE_ALPHA * bubbleAlpha
    } else {
        bubbleAlpha
    }
    Surface(
        shape = shape,
        color = resolvedColor.copy(alpha = resolvedColor.alpha * effectiveAlpha),
        border = when {
            voiceMaterialMode == DisplayMaterialMode.TRANSLUCENT ->
                BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = TRANSLUCENT_BUBBLE_BORDER_ALPHA))

            outlined -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            else -> null
        },
        onClick = {
            val existing = mediaPlayer
            when {
                // 暂停而不是 stop+reset：拖动定位后要能从原处接着放
                isPlaying -> {
                    existing?.pause()
                    isPlaying = false
                }
                existing != null -> {
                    existing.start()
                    isPlaying = true
                }
                else -> try {
                    val mp = MediaPlayer()
                    mp.setDataSource(voiceMessage.url)
                    mp.prepare()
                    mp.setOnCompletionListener {
                        isPlaying = false
                        positionMs = 0
                        it.seekTo(0)
                    }
                    mp.start()
                    isPlaying = true
                    mediaPlayer = mp
                } catch (e: Exception) {
                    // 文件可能已被清理
                }
            }
        },
    ) {
        // 语音条只有一行：播放键 + 波形 + 时长。原来外面还套了一层 Column 撑「显示文字」按钮，
        // 那个按钮让气泡凭空高出一行、也把语音条的重心压偏，一并去掉。
        //
        // 尺寸：播放键与波形都取 32dp、行内垂直居中，上下各留 12dp ⇒ 气泡高 56dp，
        // 比单行文本气泡（6 + 20 + 6 = 32dp）明显高一档 —— 语音条要一眼看出不是文字。
        // 三个元素同高且不随时长变，所以 10″ 和 120″ 的语音条高度、版式完全一致。
        //
        // barWidth 在调用点传 3dp（VoiceWaveform 的默认值 2.5dp 不动）—— 那个实现
        // 全局朗读条也在用，改默认值会连带把 TTSController 那条一起加粗。
        // 132dp 里塞 28 根柱时 gap ≈ 1.6dp，柱仍明显宽于缝，不会糊成一条实心块。
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = if (isPlaying) HugeIcons.PauseCircle else HugeIcons.PlayCircle,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            // 真波形：解码音频文件取振幅包络，点/拖可定位
            val playedColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.primary
            val unplayedColor = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.35f)
            VoiceWaveform(
                amplitudes = amplitudes,
                progress = progress,
                playedColor = playedColor,
                unplayedColor = unplayedColor,
                modifier = Modifier.width(132.dp).height(32.dp),
                barWidth = 3.dp,
                onSeek = { ratio ->
                    mediaPlayer?.let { mp ->
                        val target = (ratio * mp.duration).toInt().coerceAtLeast(0)
                        mp.seekTo(target)
                        positionMs = target
                    }
                },
            )
            // 播放中报剩余，停下报总长（跟 Operit 一致）
            val shownSec = if (isPlaying) {
                ((voiceMessage.duration - positionMs) / 1000).coerceAtLeast(0)
            } else durationSec
            Text(
                text = "${shownSec}″",
                style = MaterialTheme.typography.labelLarge,
                color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

