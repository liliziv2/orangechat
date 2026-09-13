/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.components.message
 
import android.content.Intent
import android.graphics.ColorMatrixColorFilter
import android.graphics.RenderEffect as AndroidRenderEffect
import android.graphics.Shader
import android.media.MediaPlayer
import android.os.Build
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachIndexed
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.core.net.toUri
import kotlinx.coroutines.FlowPreview
import kotlinx.datetime.toJavaLocalDateTime
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
import me.rerere.rikkahub.ui.theme.LocalDarkMode
import me.rerere.rikkahub.ui.theme.LocalMaterialMode
import me.rerere.rikkahub.ui.theme.extendColors
import me.rerere.rikkahub.data.datastore.ChatAvatarMode
import me.rerere.rikkahub.data.datastore.ChatFontFamily
import me.rerere.rikkahub.data.datastore.DisplayMaterialMode
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.foundation.Image
import me.rerere.rikkahub.utils.JsonInstant
import me.rerere.rikkahub.utils.toMessageTimeString
import me.rerere.rikkahub.utils.base64Encode
import me.rerere.rikkahub.utils.openUrl
import coil3.compose.AsyncImage
import me.rerere.rikkahub.data.ai.mood.FxTagProcessor
import me.rerere.rikkahub.data.ai.mood.MoodMode
import me.rerere.rikkahub.utils.splitIntoBubbleSegments
import me.rerere.rikkahub.utils.urlDecode
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

// ===== 普通气泡真实背景模糊（原型）=====
// 由 ChatPage 通过 CompositionLocal 下发的共享上下文：
// - 共享聊天背景 Painter（仅图片背景时非空）
// - 背景容器窗口原点与像素尺寸
// - 是否允许实时气泡模糊 + 模糊半径 px
internal data class LiveBubbleBlurContext(
    val imageBitmap: ImageBitmap? = null,
    val backgroundOriginInWindow: Offset = Offset.Unspecified,
    val backgroundSizePx: Size = Size.Unspecified,
    // 复现页面最终背景所需的视觉参数（与 AssistantBackground 共享同一套数值）
    val baseColor: Color = Color.Unspecified,
    val imageAlpha: Float = 1f,
    val gradientTopAlpha: Float = 0f,
    val gradientBottomAlpha: Float = 0f,
    val enabled: Boolean = false,
    val radiusPx: Float = 0f,
)

internal val LocalLiveBubbleBlur = staticCompositionLocalOf { LiveBubbleBlurContext() }

/**
 * 在气泡背景片段子层 DrawScope 中，按与 AssistantBackground 完全一致的合成顺序重绘背景片段：
 * 1. 页面基础底色；
 * 2. 按背景纸不透明度（imageAlpha）绘制的 Crop + Center 图片；
 * 3. 页面垂直渐变遮罩（以完整背景容器坐标为基准，气泡只裁取对应部分）。
 * 以上全部位于同一 graphicsLayer 内，统一接受 RenderEffect 模糊。
 */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLiveBackgroundFragment(
    imageBitmap: ImageBitmap?,
    backgroundOriginInWindow: Offset,
    backgroundSizePx: Size,
    layerOriginInWindow: Offset,
    baseColor: Color,
    imageAlpha: Float,
    gradientTopAlpha: Float,
    gradientBottomAlpha: Float,
) {
    if (layerOriginInWindow == Offset.Unspecified) return
    if (backgroundSizePx.width <= 0f || backgroundSizePx.height <= 0f) return

    val hasBaseColor = baseColor != Color.Unspecified

    // 1. 页面基础底色（页面最终背景的最底层；气泡在背景容器内，直接填满本层即可）
    if (hasBaseColor) {
        drawRect(color = baseColor)
    }

    // 2. 与页面一致的半透明背景图片（使用与 AssistantBackground 相同的 imageAlpha）
    if (imageBitmap != null && imageBitmap.width > 0 && imageBitmap.height > 0) {
        val safeAlpha = imageAlpha.coerceIn(0f, 1f)
        // Crop：源图覆盖完整背景容器所需的统一缩放
        val scale = max(
            backgroundSizePx.width / imageBitmap.width,
            backgroundSizePx.height / imageBitmap.height,
        )
        val drawW = imageBitmap.width * scale
        val drawH = imageBitmap.height * scale
        // Center 对齐后，源图左上角在背景容器中的偏移
        val imgOffInBgX = (backgroundSizePx.width - drawW) / 2f
        val imgOffInBgY = (backgroundSizePx.height - drawH) / 2f
        // 转换为气泡片段层局部坐标 = 背景容器坐标 - 本层窗口原点
        val offX = imgOffInBgX + backgroundOriginInWindow.x - layerOriginInWindow.x
        val offY = imgOffInBgY + backgroundOriginInWindow.y - layerOriginInWindow.y
        // drawImage 是 DrawScope 公开 API：src 取源图全图，dst 为 Crop 缩放后的
        // 目标矩形（尺寸=drawW×drawH，起点=背景容器坐标减去本层窗口原点），
        // 精确复现 ContentScale.Crop + Alignment.Center；alpha 与页面背景纸一致。
        drawImage(
            image = imageBitmap,
            srcOffset = IntOffset.Zero,
            srcSize = IntSize(imageBitmap.width, imageBitmap.height),
            dstOffset = IntOffset(offX.toInt(), offY.toInt()),
            dstSize = IntSize(drawW.toInt(), drawH.toInt()),
            alpha = safeAlpha,
        )
    }

    // 3. 页面垂直渐变遮罩：startY/endY 以完整背景容器为基准换算到气泡局部坐标，
    // 气泡只裁取完整页面渐变在当前位置对应的部分，不把渐变重新缩放进每个气泡。
    if (hasBaseColor && (gradientTopAlpha > 0f || gradientBottomAlpha > 0f)) {
        val gradTopY = backgroundOriginInWindow.y - layerOriginInWindow.y
        val gradBottomY = gradTopY + backgroundSizePx.height
        val gradientBrush = Brush.verticalGradient(
            colors = listOf(
                baseColor.copy(alpha = gradientTopAlpha.coerceIn(0f, 1f)),
                baseColor.copy(alpha = gradientBottomAlpha.coerceIn(0f, 1f)),
            ),
            startY = gradTopY,
            endY = gradBottomY,
        )
        drawRect(brush = gradientBrush)
    }
}

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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (!mine) {
            SideAvatarSlot(showAvatar, role, model, assistant, loading)
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            content()
        }
        if (mine) {
            SideAvatarSlot(showAvatar, role, model, assistant, loading)
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
) {
    if (showAvatar) {
        ChatMessageSideAvatar(
            role = role,
            model = model,
            assistant = assistant,
            loading = loading,
            size = 32.dp,
            modifier = Modifier.padding(top = 2.dp),
        )
    } else {
        // 关掉头像不留空洞：自己是实心四角星，对方是描边菱形
        ChatMessageSideMark(
            mine = role == MessageRole.USER,
            size = 32.dp,
            modifier = Modifier.padding(top = 2.dp),
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
                    showMessageTime = settings.showDateTimeInMessage,
                    messageTime = message.createdAt.toJavaLocalDateTime().toMessageTimeString(),
                    parts = message.parts,
                    annotations = message.annotations,
                    loading = loading,
                    model = model,
                    onToolApproval = onToolApproval,
                    onToolAnswer = onToolAnswer,
                    onUserMessageClick = if (message.role == MessageRole.USER) onEdit else null,
                )
 
                message.translation?.let { translation ->
                    CollapsibleTranslationText(
                        content = translation,
                        onClickCitation = {}
                    )
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
    showMessageTime: Boolean,
    messageTime: String,
    model: Model?,
    parts: List<UIMessagePart>,
    annotations: List<UIMessageAnnotation>,
    loading: Boolean,
    onToolApproval: ((toolCallId: String, approved: Boolean, reason: String) -> Unit)? = null,
    onToolAnswer: ((toolCallId: String, answer: String) -> Unit)? = null,
    onUserMessageClick: (() -> Unit)? = null,
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
    groupedParts.fastForEach { block ->
        when (block) {
            is MessagePartBlock.ThinkingBlock -> {
                if (block.steps.isNotEmpty()) {
                    val isReasoningOnlyBlock = block.steps.fastAll { it is ThinkingStep.ReasoningStep }
                    ChainOfThought(
                        modifier = Modifier.animateContentSize(),
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
                                                        liquidGlassBubbles = displaySettings.liquidGlassBubbles,
                                                        messageTimeText = if (showMessageTime) messageTime else null,
                                                        isUser = true,
                                                        onClick = { onUserMessageClick?.invoke() },
                                                        enableLiveBubbleBlur = true,
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
                                            liquidGlassBubbles = displaySettings.liquidGlassBubbles,
                                            messageTimeText = if (showMessageTime) messageTime else null,
                                            isUser = true,
                                            onClick = { onUserMessageClick?.invoke() },
                                            enableLiveBubbleBlur = true,
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
                                                        liquidGlassBubbles = displaySettings.liquidGlassBubbles,
                                                        messageTimeText = if (showMessageTime) messageTime else null,
                                                        enableLiveBubbleBlur = true,
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
                                            liquidGlassBubbles = displaySettings.liquidGlassBubbles,
                                            messageTimeText = if (showMessageTime) messageTime else null,
                                            enableLiveBubbleBlur = true,
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
    liquidGlassBubbles: Boolean = false,
    /** 非空时在气泡右下角显示该时间；由「消息内显示日期时间」开关控制。 */
    messageTimeText: String? = null,
    isUser: Boolean = false,
    outlined: Boolean = false,
    onClick: (() -> Unit)? = null,
    // 本轮原型：用户与助手的普通文本气泡传 true（最终由 LiveBubbleBlurContext 与 final 条件决定）
    enableLiveBubbleBlur: Boolean = false,
    content: @Composable () -> Unit,
) {
    val materialMode = LocalMaterialMode.current
    val liveContext = LocalLiveBubbleBlur.current
    // FLAT + 液态玻璃开关 = iOS Liquid Glass 气泡
    // GLASS = 透明玻璃气泡；两者都需要实时背景模糊
    val wantsLiveBlurMode =
        materialMode == DisplayMaterialMode.GLASS ||
            (materialMode == DisplayMaterialMode.FLAT && liquidGlassBubbles)
    val liveEnabled =
        enableLiveBubbleBlur &&
            liveContext.enabled &&
            liveContext.imageBitmap != null &&
            wantsLiveBlurMode &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            liveContext.radiusPx > 0f
    val cachedBlurEffect = remember(liveEnabled, liveContext.radiusPx) {
        if (liveEnabled) {
            // 模糊之后再提一点饱和度。纯高斯模糊会把背景的颜色摊平成一片灰，
            // 真实玻璃透过来的颜色反而更浓；加一级颜色矩阵能把气泡从"磨砂塑料"
            // 拉回"玻璃"。这是 CSS 里 backdrop-filter: blur() saturate() 的常见搭配，
            // Android 侧用 RenderEffect 链式组合实现。
            val blur = AndroidRenderEffect.createBlurEffect(
                liveContext.radiusPx,
                liveContext.radiusPx,
                Shader.TileMode.CLAMP,
            )
            AndroidRenderEffect.createColorFilterEffect(
                ColorMatrixColorFilter(saturationColorMatrix(LIQUID_GLASS_SATURATION)),
                blur,
            ).asComposeRenderEffect()
        } else {
            null
        }
    }
    var liveLayerOriginInWindow by remember { mutableStateOf(Offset.Unspecified) }
    // 最终条件：仅此条件为 true 时才追加背景片段层（当前已验证 FINAL=1）
    val finalLiveBubbleBlurEnabled = liveEnabled && cachedBlurEffect != null
    // 注意：本体不包含 fillMaxSize/fillMaxWidth 等参与父级测量的尺寸 modifier，
    // 尺寸由调用点的 matchParentSize() 决定（只覆盖父 Box 已有尺寸，不参与父级测量）。
    val liveFragmentModifier = if (finalLiveBubbleBlurEnabled) {
        Modifier
            .onGloballyPositioned { coordinates ->
                liveLayerOriginInWindow = coordinates.positionInWindow()
            }
            .graphicsLayer {
                renderEffect = cachedBlurEffect
            }
            .drawBehind {
                // 页面背景完整合成作为该 graphicsLayer 节点的直接内容：
                // 底色 → 半透明图片 → 页面渐变遮罩，统一接受 RenderEffect 模糊
                drawLiveBackgroundFragment(
                    imageBitmap = liveContext.imageBitmap,
                    backgroundOriginInWindow = liveContext.backgroundOriginInWindow,
                    backgroundSizePx = liveContext.backgroundSizePx,
                    layerOriginInWindow = liveLayerOriginInWindow,
                    baseColor = liveContext.baseColor,
                    imageAlpha = liveContext.imageAlpha,
                    gradientTopAlpha = liveContext.gradientTopAlpha,
                    gradientBottomAlpha = liveContext.gradientBottomAlpha,
                )
            }
    } else {
        Modifier
    }
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
                0f to color.copy(alpha = 0.82f * bubbleAlpha),
                0.55f to color.copy(alpha = 0.80f * bubbleAlpha),
                0.82f to color.copy(alpha = 0.56f * bubbleAlpha),
                1f to color.copy(alpha = 0.40f * bubbleAlpha),
            ),
            center = gradientCenter,
            radius = gradientCenter.getDistance(),
        )
        onDrawBehind {
            drawRect(glassFillBrush)
        }
    }
    // 液态玻璃（FLAT + 开关）填充：均匀半透明底色，靠实时模糊 + 边缘高光出质感
    val liquidGlassFillModifier = Modifier.drawWithCache {
        val base = color.copy(alpha = LIQUID_GLASS_FILL_ALPHA * bubbleAlpha)
        onDrawBehind { drawRect(color = base) }
    }
    val glassHighlightModifier = Modifier.drawWithCache {
        val topHighlightDepth = 6.dp.toPx()
        val bottomHighlightDepth = 4.dp.toPx()
        val specularHighlightTop = 1.dp.toPx()
        val specularHighlightHeight = 1.dp.toPx()
        val glassTopHighlightBrush = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to Color.White.copy(alpha = 0.22f),
                0.5f to Color.White.copy(alpha = 0.08f),
                1f to Color.Transparent,
            ),
            endY = topHighlightDepth,
        )
        val glassBottomHighlightBrush = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                0.5f to Color.White.copy(alpha = 0.05f),
                1f to Color.White.copy(alpha = 0.12f),
            ),
            startY = size.height - bottomHighlightDepth,
            endY = size.height,
        )
        val glassSpecularHighlightBrush = Brush.linearGradient(
            colorStops = arrayOf(
                0f to Color.Transparent,
                0.10f to Color.White.copy(alpha = 0.14f),
                0.28f to Color.White.copy(alpha = 0.42f),
                0.52f to Color.White.copy(alpha = 0.24f),
                0.78f to Color.White.copy(alpha = 0.08f),
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
    // 实时模糊气泡专用：白色径向渐变高光遮罩（左上亮、向右下衰减；昼夜参数不同）
    val liveBubbleRadialHighlightModifier = Modifier.drawWithCache {
        val highlightCenter = if (isDarkTheme) {
            Offset(size.width * 0.20f, size.height * 0.15f)
        } else {
            Offset(size.width * 0.18f, size.height * 0.12f)
        }
        val highlightRadius = maxOf(size.width, size.height) * 0.95f
        val highlightBrush = Brush.radialGradient(
            colorStops = if (isDarkTheme) {
                arrayOf(
                    0f to Color.White.copy(alpha = 0.06f),
                    0.45f to Color.White.copy(alpha = 0.02f),
                    1f to Color.Transparent,
                )
            } else {
                arrayOf(
                    0f to Color.White.copy(alpha = 0.18f),
                    0.42f to Color.White.copy(alpha = 0.07f),
                    1f to Color.Transparent,
                )
            },
            center = highlightCenter,
            radius = highlightRadius,
        )
        onDrawBehind {
            drawRect(brush = highlightBrush)
        }
    }
    // 实时模糊气泡专用（仅夜间）：深色方向性识读遮罩，防止亮色背景导致气泡泛白、白字不可读
    val liveBubbleNightReadabilityModifier = Modifier.drawWithCache {
        val readabilityBrush = Brush.linearGradient(
            colorStops = arrayOf(
                0f to Color.Black.copy(alpha = 0.06f),
                0.55f to Color.Black.copy(alpha = 0.09f),
                1f to Color.Black.copy(alpha = 0.12f),
            ),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        )
        onDrawBehind {
            drawRect(brush = readabilityBrush)
        }
    }
    // 气泡实际轮廓：玻璃/液态玻璃的贴边高光必须照它走，否则会在圆角处错位并被父级 clip 切掉一截。
    // 非对称造型：用户气泡右下角收窄、助手气泡左下角收窄，指向各自的头像一侧。
    val shape = if (isUser) {
        RoundedCornerShape(
            topStart = cornerRadius,
            topEnd = cornerRadius,
            bottomEnd = cornerRadius * 0.25f,
            bottomStart = cornerRadius,
        )
    } else {
        RoundedCornerShape(
            topStart = cornerRadius,
            topEnd = cornerRadius,
            bottomEnd = cornerRadius,
            bottomStart = cornerRadius * 0.25f,
        )
    }
    val bubbleLayoutDirection = LocalLayoutDirection.current
    // 实时模糊气泡专用：沿真实轮廓贴边的方向性硬高光（左上亮、向右下透明；昼夜强弱不同）
    val liveBubbleEdgeHighlightModifier = Modifier.drawWithCache {
        val strokeWidthPx = 1.dp.toPx()
        val halfStroke = strokeWidthPx / 2f
        val edgeStartAlpha = if (isDarkTheme) 0.38f else 0.78f
        val edgeMidAlpha = if (isDarkTheme) 0.133f else 0.273f
        val edgeBrush = Brush.linearGradient(
            colorStops = arrayOf(
                0f to Color.White.copy(alpha = edgeStartAlpha),
                0.45f to Color.White.copy(alpha = edgeMidAlpha),
                0.75f to Color.Transparent,
                1f to Color.Transparent,
            ),
            start = Offset.Zero,
            end = Offset(size.width, size.height),
        )
        // 描边居中落在轮廓上：按内缩一个描边宽的尺寸取轮廓，再整体平移半个描边宽
        val insetSize = Size(
            width = maxOf(0f, size.width - strokeWidthPx),
            height = maxOf(0f, size.height - strokeWidthPx),
        )
        val edgePath = if (insetSize.width > 0f && insetSize.height > 0f) {
            Path().apply {
                addOutline(
                    shape.createOutline(
                        size = insetSize,
                        layoutDirection = bubbleLayoutDirection,
                        density = this@drawWithCache,
                    )
                )
                translate(Offset(halfStroke, halfStroke))
            }
        } else {
            null
        }
        onDrawBehind {
            if (edgePath != null) {
                drawPath(
                    path = edgePath,
                    brush = edgeBrush,
                    style = Stroke(width = strokeWidthPx),
                )
            }
        }
    }
    val hasImage = imagePath.isNotBlank() && java.io.File(imagePath).exists()
    val frostedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = LIQUID_GLASS_BORDER_ALPHA)
    if (materialMode == DisplayMaterialMode.FLAT && liquidGlassBubbles) {
        // ── 液态玻璃模式（iOS Liquid Glass）──
        // 实时背景模糊 + 均匀半透明填充 + 顶部折射反光 + 边缘高光描边
        Box(
            modifier = Modifier
                .animateContentSize()
                // 投影要在 clip 之前：玻璃片本身是半透明的，阴影必须落在气泡之外，
                // 否则会被 clip 连着裁掉，气泡就像直接印在背景上而不是浮在上面。
                .shadow(
                    elevation = LIQUID_GLASS_SHADOW_ELEVATION,
                    shape = shape,
                    clip = false,
                    ambientColor = Color.Black.copy(alpha = 0.5f),
                    spotColor = Color.Black.copy(alpha = 0.5f),
                )
                .clip(shape)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .border(1.dp, frostedBorderColor, shape)
        ) {
            if (finalLiveBubbleBlurEnabled) {
                // 背景模糊片段：仅模糊背景层，文字保持清晰
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .then(liveFragmentModifier)
                )
            }
            // 自定义气泡背景图。以前这个模式遇到背景图就整段退化成普通气泡，
            // 玻璃质感全丢；现在图铺在玻璃填充之下，高光和描边照常叠加。
            if (hasImage) {
                AsyncImage(
                    model = imagePath,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }
            if (isDarkTheme && finalLiveBubbleBlurEnabled) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .then(liveBubbleNightReadabilityModifier)
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(liquidGlassFillModifier)
            )
            if (finalLiveBubbleBlurEnabled) {
                // 径向光泽：左上亮，模拟玻璃对光的折射
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .then(liveBubbleRadialHighlightModifier)
                )
                // 顶部折射反光带
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .then(liveBubbleEdgeHighlightModifier)
                )
            } else {
                // 无实时模糊时的降级：静态高光 + 顶部反光，保证液态玻璃观感
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .then(glassHighlightModifier)
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .then(liveBubbleEdgeHighlightModifier)
                )
            }
            Column(modifier = Modifier.padding(8.dp)) {
                content()
                MessageTimeLabel(messageTimeText)
            }
        }
    } else if (materialMode == DisplayMaterialMode.GLASS) {
        Box(
            modifier = Modifier
                .animateContentSize()
                .clip(shape)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .border(1.dp, glassBorderColor, shape)
        ) {
            if (finalLiveBubbleBlurEnabled) {
                // 背景图片片段层：仅模糊此子层，文字等前景内容保持清晰。
                // matchParentSize 只覆盖父 Box 已确定尺寸，不参与父 Box 测量，
                // 因此气泡宽度仍由文字内容 + padding + 宽度上限决定。
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .then(liveFragmentModifier)
                )
            }
            if (hasImage) {
                AsyncImage(
                    model = imagePath,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }
            if (finalLiveBubbleBlurEnabled && isDarkTheme) {
                // 夜间深色识读遮罩：位于模糊背景之上、glass fill 之下，压暗亮背景保证白字可读
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .then(liveBubbleNightReadabilityModifier)
                )
            }
            if (!hasImage || overlayEnabled) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .then(glassFillModifier)
                )
            }
            if (finalLiveBubbleBlurEnabled) {
                // 白色径向渐变高光遮罩：位于 glass fill 上方、玻璃高光下方，受气泡圆角裁剪
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .then(liveBubbleRadialHighlightModifier)
                )
            }
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .then(glassHighlightModifier)
            )
            if (finalLiveBubbleBlurEnabled) {
                // 沿真实圆角贴边的方向性硬高光：位于渲染层外、content 之下；仅左上/顶部可见，向右下透明
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .then(liveBubbleEdgeHighlightModifier)
                )
            }
            Column(modifier = Modifier.padding(8.dp)) {
                content()
                MessageTimeLabel(messageTimeText)
            }
        }
    } else if (hasImage) {
        Box(
            modifier = Modifier
                .animateContentSize()
                .clip(shape)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
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
            Column(modifier = Modifier.padding(8.dp)) {
                content()
                MessageTimeLabel(messageTimeText)
            }
        }
    } else {
        Surface(
            modifier = Modifier.animateContentSize(),
            shape = shape,
            color = color.copy(alpha = effectiveAlpha),
            border = if (materialMode == DisplayMaterialMode.TRANSLUCENT) {
                BorderStroke(1.dp, translucentBorderColor)
            } else if (outlined) {
                // 助手气泡默认背景时补一道细描边，把气泡从纯色底上区分出来
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            } else {
                null
            },
            onClick = onClick ?: {},
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                content()
                MessageTimeLabel(messageTimeText)
            }
        }
    }
}

private const val LIQUID_GLASS_FILL_ALPHA = 0.58f
private const val LIQUID_GLASS_BORDER_ALPHA = 0.12f
private const val TRANSLUCENT_BUBBLE_BASE_ALPHA = 0.72f
private const val TRANSLUCENT_BUBBLE_BORDER_ALPHA = 0.18f
private const val GLASS_BUBBLE_BORDER_ALPHA = 0.24f

/**
 * 液态玻璃背景的饱和度倍数。
 *
 * 1.0 是原色。高斯模糊本身会让背景颜色互相平均、显得发灰，稍微提一点饱和度
 * 才像"透过玻璃看到的颜色"而不是"磨砂塑料"。取值偏保守：太高会让深色背景
 * 下的气泡出现偏色。
 */
private const val LIQUID_GLASS_SATURATION = 1.35f

/**
 * 液态玻璃气泡的投影高度。
 *
 * 玻璃片应当浮在背景之上而不是印在上面，一点投影就能把这层关系交代清楚。
 * 数值刻意压得低：气泡是密集重复的元素，投影稍重整屏就会显得脏。
 */
private val LIQUID_GLASS_SHADOW_ELEVATION = 6.dp

/**
 * 构造一个只改饱和度的颜色矩阵。
 *
 * 用 Rec. 709 亮度权重把每个通道往灰度拉或往外推：saturation=0 得到灰度图，
 * 1 得到原图，大于 1 增强。手写而不是用 ColorMatrix.setSaturation() 是为了
 * 不依赖可变对象，可以直接放进 remember 里。
 */
private fun saturationColorMatrix(saturation: Float): android.graphics.ColorMatrix {
    val inv = 1f - saturation
    val r = 0.213f * inv
    val g = 0.715f * inv
    val b = 0.072f * inv
    return android.graphics.ColorMatrix(
        floatArrayOf(
            r + saturation, g, b, 0f, 0f,
            r, g + saturation, b, 0f, 0f,
            r, g, b + saturation, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        )
    )
}
 
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
    val shape = if (isUser) {
        RoundedCornerShape(
            topStart = cornerRadius,
            topEnd = cornerRadius,
            bottomEnd = cornerRadius * 0.25f,
            bottomStart = cornerRadius,
        )
    } else {
        RoundedCornerShape(
            topStart = cornerRadius,
            topEnd = cornerRadius,
            bottomEnd = cornerRadius,
            bottomStart = cornerRadius * 0.25f,
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
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = if (isPlaying) HugeIcons.PauseCircle else HugeIcons.PlayCircle,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
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
                modifier = Modifier.width(90.dp).height(24.dp),
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
                style = MaterialTheme.typography.labelMedium,
                color = if (isUser) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
    }
}

@Composable
private fun ColumnScope.MessageTimeLabel(time: String?) {
    if (time == null) return
    // 不加 fillMaxWidth：一行时间会把整个气泡撑满聊天列，短消息也被拉成整行宽。
    // 靠 align(Alignment.End) 贴右下。
    Text(
        text = time,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
        modifier = Modifier
            .align(Alignment.End)
            .padding(top = 2.dp),
    )
}

