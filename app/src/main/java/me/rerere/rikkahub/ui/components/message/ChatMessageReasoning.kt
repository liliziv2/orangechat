package me.rerere.rikkahub.ui.components.message
 
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import me.rerere.ai.provider.Model
import me.rerere.ai.ui.UIMessagePart
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.model.Assistant
import me.rerere.rikkahub.data.model.AssistantAffectScope
import me.rerere.rikkahub.data.model.replaceRegexes
import me.rerere.rikkahub.ui.components.richtext.MarkdownBlock
import me.rerere.rikkahub.ui.components.ui.ChainOfThoughtScope
import me.rerere.rikkahub.ui.components.ui.icons.OrangePetalIcon
import me.rerere.rikkahub.ui.context.LocalDisplaySettings
import me.rerere.rikkahub.ui.modifier.shimmer
import me.rerere.rikkahub.utils.extractThinkingTitle
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
 
enum class ReasoningCardState(val expanded: Boolean) {
    Collapsed(false),
    Preview(true),
    Expanded(true),
}
 
@Stable
private class ReasoningState(
    val scrollState: ScrollState,
    initialDuration: Duration,
) {
    var expandState by mutableStateOf(ReasoningCardState.Collapsed)
    var duration by mutableStateOf(initialDuration)
 
    fun onExpandedChange(nextExpanded: Boolean, loading: Boolean) {
        expandState = if (loading) {
            if (nextExpanded) ReasoningCardState.Expanded else ReasoningCardState.Preview
        } else {
            if (nextExpanded) ReasoningCardState.Expanded else ReasoningCardState.Collapsed
        }
    }
}
 
@Composable
private fun rememberReasoningState(reasoning: UIMessagePart.Reasoning): Pair<ReasoningState, Boolean> {
    val displaySettings = LocalDisplaySettings.current
    val loading = reasoning.finishedAt == null
    val scrollState = rememberScrollState()
 
    val state = remember(reasoning.createdAt) {
        ReasoningState(
            scrollState = scrollState,
            initialDuration = reasoning.finishedAt?.let { it - reasoning.createdAt }
                ?: (Clock.System.now() - reasoning.createdAt)
        )
    }
 
    LaunchedEffect(reasoning.reasoning, loading) {
        if (loading) {
            if (!state.expandState.expanded && displaySettings.showThinkingContent)
                state.expandState = ReasoningCardState.Preview
            scrollState.animateScrollTo(scrollState.maxValue)
        } else {
            if (state.expandState.expanded) {
                state.expandState = if (displaySettings.autoCloseThinking)
                    ReasoningCardState.Collapsed
                else
                    ReasoningCardState.Expanded
            }
        }
    }
 
    LaunchedEffect(loading) {
        if (loading) {
            while (isActive) {
                state.duration = (reasoning.finishedAt ?: Clock.System.now()) - reasoning.createdAt
                delay(50)
            }
        }
    }
 
    return state to loading
}
 
@Composable
private fun ReasoningContent(
    reasoning: UIMessagePart.Reasoning,
    assistant: Assistant?,
    expandState: ReasoningCardState,
    scrollState: ScrollState,
    fadeHeight: Float,
) {
    val isPreview = expandState == ReasoningCardState.Preview
    val displaySettings = LocalDisplaySettings.current
    val thinkingStyle = MaterialTheme.typography.bodySmall.copy(
        fontSize = MaterialTheme.typography.bodySmall.fontSize * displaySettings.thinkingFontSizeRatio,
        lineHeight = MaterialTheme.typography.bodySmall.lineHeight * displaySettings.thinkingFontSizeRatio,
    )
 
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .let { contentModifier ->
                if (isPreview) {
                    contentModifier
                        .graphicsLayer { alpha = 0.99f }
                        .drawWithCache {
                            val brush = Brush.verticalGradient(
                                startY = 0f,
                                endY = size.height,
                                colorStops = arrayOf(
                                    0.0f to Color.Transparent,
                                    (fadeHeight / size.height) to Color.Black,
                                    (1 - fadeHeight / size.height) to Color.Black,
                                    1.0f to Color.Transparent
                                )
                            )
                            onDrawWithContent {
                                drawContent()
                                drawRect(
                                    brush = brush,
                                    size = Size(size.width, size.height),
                                    blendMode = BlendMode.DstIn,
                                )
                            }
                        }
                        .heightIn(max = 100.dp)
                        .verticalScroll(scrollState)
                } else {
                    contentModifier
                }
            }
    ) {
        SelectionContainer {
            MarkdownBlock(
                content = reasoning.reasoning.replaceRegexes(
                    assistant = assistant,
                    scope = AssistantAffectScope.ASSISTANT,
                    visual = true,
                ),
                style = thinkingStyle,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
 
@Composable
fun ChainOfThoughtScope.ChatMessageReasoningStep(
    reasoning: UIMessagePart.Reasoning,
    model: Model?,
    assistant: Assistant?,
    fadeHeight: Float = 64f,
    collapsedAdaptiveWidth: Boolean = false,
) {
    val (state, loading) = rememberReasoningState(reasoning)
    val thinkingTitle = reasoning.reasoning.extractThinkingTitle()
    val showThinkingTitle = loading && thinkingTitle != null
 
    ControlledChainOfThoughtStep(
        expanded = state.expandState == ReasoningCardState.Expanded,
        onExpandedChange = { state.onExpandedChange(it, loading) },
        // 轻入口的图标：橘瓣自己的花瓣。
        //
        // OrangePetalIcon 是用户提供的品牌图形，此前全仓零调用 —— 它原本属于
        // 「卡片容器 + 秒数胶囊」那一套，批 28 把卡片拆掉之后图标就一直悬空。
        // 现在捡回来当思考入口的标识：入口于是仍然带着橘瓣自己的脸，
        // 而不是又一个通用 sparkles。
        //
        // 旧注释写「不给图标、靠 ChainOfThought 画的 8dp 小圆点交代步骤」，
        // 那句话已经过时 —— 组件现在 icon == null 时完全不占位。
        icon = { ReasoningPetalIcon(loading = loading) },
        label = {
            if (showThinkingTitle) {
                ReasoningTitle(title = thinkingTitle!!)
            } else {
                // 轻量自然语言状态。
                //
                // 生成中：「栖 正在思考…」；带工具调用时：「栖 正在使用工具…」。
                // 结束后才交代耗时，且耗时不再是主体 —— 它只是这行文字的收尾。
                // 助手名为空时退回中性主语，不硬塞产品名。
                val who = assistant?.name?.takeIf { it.isNotBlank() }
                Text(
                    text = if (loading) {
                        if (who != null) {
                            stringResource(R.string.reasoning_status_thinking_named, who)
                        } else {
                            stringResource(R.string.reasoning_status_thinking)
                        }
                    } else {
                        // 完成态不再报「思考了 1940.3 秒」。
                        // 一次 32 分钟的思考会渲染成四位整数 + 一位小数，而它只是
                        // 一行旁注 —— 数字比它要交代的信息长得多。压成 32m 这种记号，
                        // 入口读起来才真的轻。
                        stringResource(
                            R.string.reasoning_entry_label,
                            compactDuration(state.duration)
                        )
                    },
                    // bodySmall 而不是 titleSmall：这是旁注，不是标题
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.shimmer(isLoading = loading),
                )
            }
        },
        extra = {
            if (showThinkingTitle && state.duration > 0.seconds) {
                Text(
                    text = compactDuration(state.duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.shimmer(isLoading = loading),
                )
            }
        },
        collapsedAdaptiveWidth = collapsedAdaptiveWidth,
        contentVisible = state.expandState != ReasoningCardState.Collapsed,
        content = {
            ReasoningContent(
                reasoning = reasoning,
                assistant = assistant,
                expandState = state.expandState,
                scrollState = state.scrollState,
                fadeHeight = fadeHeight,
            )
        },
    )
}
 
 
/**
 * 思考入口的图标 —— 橘瓣自己的花瓣（见 [OrangePetalIcon]）。
 *
 * 生成中转一圈（1.6s/圈、线性）：这是「正在思考」唯一的动效，幅度很小，
 * 不改变行高、不参与布局测量。结束之后走 else 分支 —— 无限动画会随分支
 * 一起 dispose，历史消息里的入口不会继续空转。
 */
@Composable
private fun ReasoningPetalIcon(loading: Boolean) {
    if (loading) {
        val transition = rememberInfiniteTransition(label = "reasoning-petal")
        val angle by transition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = PetalSpinMillis, easing = LinearEasing),
            ),
            label = "reasoning-petal-angle",
        )
        ReasoningPetal(angle = angle)
    } else {
        ReasoningPetal(angle = 0f)
    }
}

@Composable
private fun ReasoningPetal(angle: Float) {
    Icon(
        imageVector = OrangePetalIcon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .size(PetalIconSize)
            .graphicsLayer { rotationZ = angle },
    )
}

/** 入口图标尺寸。比 ChainOfThought 的 20dp 图标位小一档：它是标识，不是按钮。 */
private val PetalIconSize = 15.dp

/** 花瓣转一圈的时长。偏慢 —— 这是旁注上的呼吸，不是加载指示器。 */
private const val PetalSpinMillis = 1600

/**
 * 极短的耗时记号（只给入口用，不进正文）。
 *
 * 60 秒以内报秒、以内报分、再往上带小时。原来是 `deep_thinking_seconds`
 * 的 `%1$.1f`，一次 32 分钟的思考会写成「思考了 1940.3 秒」。
 */
private fun compactDuration(duration: Duration): String {
    val total = duration.inWholeSeconds.coerceAtLeast(0L)
    return when {
        total < 60L -> "${total}s"
        total < 3600L -> "${total / 60L}m"
        else -> "${total / 3600L}h${(total % 3600L) / 60L}m"
    }
}

@Composable
private fun ReasoningTitle(title: String) {
    AnimatedContent(
        targetState = title,
        transitionSpec = {
            (slideInVertically { height -> height } + fadeIn()).togetherWith(
                slideOutVertically { height -> -height } + fadeOut()
            )
        }
    ) {
        Text(
            text = it,
            // titleSmall + secondary 会让这行状态比回复正文还重 —— 视觉重量倒挂。
            // 它是旁注，跟下面那行状态文字用同一档。
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .shimmer(true),
        )
    }
}