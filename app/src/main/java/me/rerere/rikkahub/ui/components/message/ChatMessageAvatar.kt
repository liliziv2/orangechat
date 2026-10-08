package me.rerere.rikkahub.ui.components.message

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.datetime.toJavaLocalDateTime
import me.rerere.ai.core.MessageRole
import me.rerere.ai.provider.Model
import me.rerere.ai.ui.UIMessage
import me.rerere.ai.ui.isEmptyUIMessage
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.Avatar
import me.rerere.rikkahub.ui.components.ui.AutoAIIcon
import me.rerere.rikkahub.ui.components.ui.UIAvatar
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.utils.toLocalString
import java.io.File

// ── 署名行（头像 + 昵称 + 时间）的统一规格 ─────────────────────────────────
//
// 用户侧与助手侧必须**完全同档**：字号、行高、颜色、头像尺寸只由 alignEnd 镜像。
// 旧实现是两套：用户昵称 titleSmall、助手昵称 titleSmallEmphasized（字重不同）；
// 用户时间 labelSmall(11sp)/alpha .6、助手时间 labelSmall 或 titleSmall(14sp)/alpha .8；
// 头像 36dp vs 32dp —— 「User 的时间明显偏小、和 Assistant 不是同一视觉等级」
// 就是这么来的。现在只留这一份规格，两侧共用。

/** 署名行头像尺寸。用户与助手同一档。 */
private val META_AVATAR_SIZE = 32.dp

/** 头像与昵称/时间之间的横向间距。两侧同档。 */
private val META_AVATAR_TEXT_GAP = 8.dp

/** 昵称与时间戳之间的纵向间距。 */
private val META_NAME_TIME_GAP = 2.dp

/** 昵称样式：16sp / 22sp。两侧共用，不再有 Emphasized 与非 Emphasized 之分。 */
private val META_NAME_STYLE = TextStyle(
    fontSize = 16.sp,
    lineHeight = 22.sp,
    fontWeight = FontWeight.Medium,
)

/** 时间戳样式：13sp / 18sp。与昵称同一套字体，只小一档、淡一档。 */
private val META_TIME_STYLE = TextStyle(
    fontSize = 13.sp,
    lineHeight = 18.sp,
)

/** 昵称 / 时间戳的不透明度。两侧必须一致。 */
private const val META_NAME_ALPHA = 0.85f
private const val META_TIME_ALPHA = 0.62f

/**
 * 署名行的「昵称 + 时间戳」列。用户与助手共用这一份实现 ——
 * 字号、行高、颜色、对齐只由 [alignEnd] 镜像，不存在两套 typography。
 * 两者都不显示时整列不渲染（不占位、不留空洞）。
 */
@Composable
private fun MetaNameTimeColumn(
    nickname: String,
    timeText: String,
    showName: Boolean,
    showTime: Boolean,
    alignEnd: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!showName && !showTime) return
    Column(
        modifier = modifier,
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(META_NAME_TIME_GAP),
    ) {
        if (showName) {
            Text(
                text = nickname,
                style = META_NAME_STYLE,
                color = LocalContentColor.current.copy(alpha = META_NAME_ALPHA),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showTime) {
            Text(
                text = timeText,
                style = META_TIME_STYLE,
                color = LocalContentColor.current.copy(alpha = META_TIME_ALPHA),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun AvatarFrameOverlay(
    framePath: String,
    offsetX: Float,
    offsetY: Float,
    scale: Float,
    baseSize: Float,
) {
    if (framePath.isNotBlank() && File(framePath).exists()) {
        val context = LocalContext.current
        val bitmap = remember(framePath) {
            runCatching {
                BitmapFactory.decodeFile(framePath)?.asImageBitmap()
            }.getOrNull()
        }
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Avatar Frame",
                modifier = Modifier
                    .size((baseSize * scale).dp)
                    .offset(x = offsetX.dp, y = offsetY.dp),
                contentScale = ContentScale.Fit,
                alpha = 1f,
            )
        }
    }
}

@Composable
fun ChatMessageUserAvatar(
    message: UIMessage,
    avatar: Avatar,
    nickname: String,
    modifier: Modifier = Modifier,
) {
    val settings = LocalSettings.current
    if (message.role == MessageRole.USER && !message.parts.isEmptyUIMessage() && settings.displaySetting.showUserAvatar) {
        // 署名行本身**不带纵向 padding** —— 它和气泡之间的距离由 ChatMessage 的
        // 外层 cluster 间距统一决定（文字 10dp / 图片 5dp）。在这里加 padding 只会
        // 把用户侧的署名行从消息顶端往下推、和助手侧错开一档。
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(META_AVATAR_TEXT_GAP, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetaNameTimeColumn(
                nickname = nickname.ifEmpty { stringResource(R.string.user_default_name) },
                timeText = message.createdAt.toJavaLocalDateTime().toLocalString(),
                showName = true,
                showTime = settings.displaySetting.showDateBelowName,
                alignEnd = true,
            )
            Box(contentAlignment = Alignment.Center) {
                UIAvatar(
                    name = nickname,
                    modifier = Modifier.size(META_AVATAR_SIZE),
                    value = avatar,
                    loading = false,
                )
                AvatarFrameOverlay(
                    framePath = settings.displaySetting.userAvatarFramePath,
                    offsetX = settings.displaySetting.userAvatarFrameOffsetX,
                    offsetY = settings.displaySetting.userAvatarFrameOffsetY,
                    scale = settings.displaySetting.userAvatarFrameScale,
                    baseSize = META_AVATAR_SIZE.value,
                )
            }
        }
    }
}

@Composable
fun ChatMessageAssistantAvatar(
    message: UIMessage,
    loading: Boolean,
    model: Model?,
    assistant: Assistant?,
    modifier: Modifier = Modifier,
) {
    val settings = LocalSettings.current
    val showIcon = settings.displaySetting.showModelIcon
    val useAssistantAvatar = assistant?.useAssistantAvatar == true
    if (message.role == MessageRole.ASSISTANT && (model != null || useAssistantAvatar)) {
        // 昵称与时间戳走与用户侧**同一份**实现（MetaNameTimeColumn）：
        // 字号 / 行高 / 颜色只由 alignEnd 镜像，不再有第二套 typography。
        val name = if (useAssistantAvatar) {
            assistant?.name.orEmpty().ifEmpty {
                stringResource(R.string.assistant_page_default_assistant)
            }
        } else {
            model?.displayName.orEmpty()
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(META_AVATAR_TEXT_GAP),
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
        ) {
            if (showIcon) {
                Box(contentAlignment = Alignment.Center) {
                    if (useAssistantAvatar && assistant != null) {
                        UIAvatar(
                            name = assistant.name,
                            modifier = Modifier.size(META_AVATAR_SIZE),
                            value = assistant.avatar,
                            loading = loading,
                        )
                    } else {
                        AutoAIIcon(
                            name = model?.modelId ?: assistant?.name.orEmpty(),
                            modifier = Modifier.size(META_AVATAR_SIZE),
                            loading = loading,
                        )
                    }
                    AvatarFrameOverlay(
                        framePath = settings.displaySetting.aiAvatarFramePath,
                        offsetX = settings.displaySetting.aiAvatarFrameOffsetX,
                        offsetY = settings.displaySetting.aiAvatarFrameOffsetY,
                        scale = settings.displaySetting.aiAvatarFrameScale,
                        baseSize = META_AVATAR_SIZE.value,
                    )
                }
            }
            MetaNameTimeColumn(
                nickname = name,
                timeText = message.createdAt.toJavaLocalDateTime().toLocalString(),
                showName = settings.displaySetting.showModelName,
                // 时间戳的**显示门控保持原样**：旧实现把它嵌在 showModelName 之下，
                // 本轮不改显示行为，只统一字号 / 行高 / 颜色（见 META_* 常量）。
                showTime = settings.displaySetting.showModelName && settings.displaySetting.showDateBelowName,
                alignEnd = false,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * 侧边头像：贴在气泡左/右的一枚圆头像，用于 ChatAvatarMode.SIDE 版式。
 * 助手侧优先用助手头像，其次是模型图标；用户侧用用户头像。
 * 头像框（挂件）沿用与署名行相同的偏移与缩放配置，只是基准尺寸不同。
 */
@Composable
fun ChatMessageSideAvatar(
    role: MessageRole,
    model: Model?,
    assistant: Assistant?,
    loading: Boolean = false,
    size: Dp = 32.dp,
    modifier: Modifier = Modifier,
) {
    val settings = LocalSettings.current
    val display = settings.displaySetting
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        if (role == MessageRole.USER) {
            UIAvatar(
                name = display.userNickname,
                modifier = Modifier.size(size),
                value = display.userAvatar,
                loading = false,
            )
            AvatarFrameOverlay(
                framePath = display.userAvatarFramePath,
                offsetX = display.userAvatarFrameOffsetX,
                offsetY = display.userAvatarFrameOffsetY,
                scale = display.userAvatarFrameScale,
                baseSize = size.value,
            )
        } else {
            if (assistant?.useAssistantAvatar == true) {
                UIAvatar(
                    name = assistant.name,
                    modifier = Modifier.size(size),
                    value = assistant.avatar,
                    loading = loading,
                )
            } else {
                AutoAIIcon(
                    name = model?.modelId ?: assistant?.name.orEmpty(),
                    modifier = Modifier.size(size),
                    loading = loading,
                )
            }
            AvatarFrameOverlay(
                framePath = display.aiAvatarFramePath,
                offsetX = display.aiAvatarFrameOffsetX,
                offsetY = display.aiAvatarFrameOffsetY,
                scale = display.aiAvatarFrameScale,
                baseSize = size.value,
            )
        }
    }
}

/**
 * 顶栏双头像：对方在左、自己在右，右边那枚用负偏移叠压在左边那枚上面。
 * 左侧（对方）留在上层，所以先画右侧再画左侧。
 */
@Composable
fun ChatTopBarDualAvatar(
    model: Model?,
    assistant: Assistant?,
    size: Dp = 28.dp,
    overlap: Dp = 8.dp,
    modifier: Modifier = Modifier,
) {
    val ringColor = MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = modifier.size(width = size * 2 - overlap, height = size),
        contentAlignment = Alignment.CenterStart,
    ) {
        // 自己：下层，靠右
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .border(1.dp, ringColor, CircleShape)
                .padding(1.dp)
        ) {
            ChatMessageSideAvatar(
                role = MessageRole.USER,
                model = null,
                assistant = null,
                size = size - 2.dp,
            )
        }
        // 对方：上层，靠左
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .border(1.dp, ringColor, CircleShape)
                .padding(1.dp)
        ) {
            ChatMessageSideAvatar(
                role = MessageRole.ASSISTANT,
                model = model,
                assistant = assistant,
                size = size - 2.dp,
            )
        }
    }
}

/**
 * 头像位的兜底记号。关掉头像时槽位不留空洞，画一枚小图形代替：
 * 自己是实心四角星，对方是描边菱形——形状本身就分得出这一段是谁说的。
 * 路径取自 14x14 视区，按槽位尺寸等比缩放。
 */
@Composable
fun ChatMessageSideMark(
    mine: Boolean,
    size: Dp = 32.dp,
    modifier: Modifier = Modifier,
) {
    val color = if (mine) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    } else {
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
    }
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size * 0.42f)) {
            val u = this.size.minDimension / 14f
            fun p(x: Float, y: Float) = Offset(x * u, y * u)
            if (mine) {
                val star = Path().apply {
                    moveTo(p(7f, 0.8f).x, p(7f, 0.8f).y)
                    lineTo(p(8.2f, 5.4f).x, p(8.2f, 5.4f).y)
                    lineTo(p(12.6f, 7f).x, p(12.6f, 7f).y)
                    lineTo(p(8.2f, 8.6f).x, p(8.2f, 8.6f).y)
                    lineTo(p(7f, 13.2f).x, p(7f, 13.2f).y)
                    lineTo(p(5.8f, 8.6f).x, p(5.8f, 8.6f).y)
                    lineTo(p(1.4f, 7f).x, p(1.4f, 7f).y)
                    lineTo(p(5.8f, 5.4f).x, p(5.8f, 5.4f).y)
                    close()
                }
                drawPath(star, color = color)
            } else {
                val diamond = Path().apply {
                    moveTo(p(7f, 1f).x, p(7f, 1f).y)
                    lineTo(p(12.4f, 7f).x, p(12.4f, 7f).y)
                    lineTo(p(7f, 13f).x, p(7f, 13f).y)
                    lineTo(p(1.6f, 7f).x, p(1.6f, 7f).y)
                    close()
                }
                drawPath(diamond, color = color, style = Stroke(width = u))
            }
        }
    }
}
