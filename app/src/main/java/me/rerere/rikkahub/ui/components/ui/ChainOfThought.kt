/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.components.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.ArrowDown01
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.hugeicons.stroke.ArrowRight01
import me.rerere.hugeicons.stroke.ArrowUp01
import me.rerere.hugeicons.stroke.Search01
import me.rerere.hugeicons.stroke.Sparkles
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.datastore.DisplayMaterialMode
import me.rerere.rikkahub.ui.theme.LocalMaterialMode

private val LocalCardColor = staticCompositionLocalOf { Color.White }

/**
 * 以时间线/步骤卡片的形式展示一组思考过程。
 *
 * 适用于承载推理步骤、工具调用步骤，或两者混合的链式内容。组件支持：
 * - 在步骤较多时自动折叠，仅展示最后若干步
 * - 点击顶部控制条展开/收起全部步骤
 * - 通过 [collapsedAdaptiveWidth] 控制折叠态是否保持自适应宽度
 *
 * @param modifier 外层卡片的修饰符
 * @param cardColors 卡片配色
 * @param steps 需要渲染的步骤数据列表
 * @param collapsedVisibleCount 折叠时保留可见的尾部步骤数
 * @param collapsedAdaptiveWidth 是否在折叠态下使用内容自适应宽度
 * @param content 每个步骤的具体 UI，由 [ChainOfThoughtScope] 提供步骤构建能力
 */
@Composable
fun <T> ChainOfThought(
    modifier: Modifier = Modifier,
    cardColors: CardColors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    ),
    steps: List<T>,
    collapsedVisibleCount: Int = 2,
    collapsedAdaptiveWidth: Boolean = false,
    content: @Composable ChainOfThoughtScope.(T) -> Unit
) {
    val settings = LocalSettings.current
    val thinkingAlpha = 1f - settings.displaySetting.thinkingChainTransparency / 100f
    val materialMode = LocalMaterialMode.current
    val useTranslucentSurface = materialMode == DisplayMaterialMode.TRANSLUCENT ||
        materialMode == DisplayMaterialMode.GLASS
    val materialBaseAlpha = when (materialMode) {
        DisplayMaterialMode.TRANSLUCENT -> TRANSLUCENT_THINKING_BASE_ALPHA
        DisplayMaterialMode.GLASS -> GLASS_THINKING_BASE_ALPHA
        DisplayMaterialMode.FOLLOW_THEME,
        DisplayMaterialMode.FLAT -> 1f
    }

    var expanded by remember { mutableStateOf(false) }
    val canCollapse = steps.size > collapsedVisibleCount
    val shouldFillCollapseControlWidth = expanded || !collapsedAdaptiveWidth

    val thinkingBubbleColor = settings.displaySetting.thinkingBubbleColor?.let { it.toComposeColor() }
    val effectiveCardColors = CardDefaults.cardColors(
        containerColor = (thinkingBubbleColor ?: cardColors.containerColor).copy(
            alpha = materialBaseAlpha * thinkingAlpha
        ),
        contentColor = cardColors.contentColor,
        disabledContainerColor = cardColors.disabledContainerColor,
        disabledContentColor = cardColors.disabledContentColor,
    )
    // 边框与玻璃三层随卡片容器一起去掉：思考不再是独立卡片，就没有"材质"可言。
    // useTranslucentSurface / materialBaseAlpha 仍用于 thinkingBubbleColor 的透明度合成。

    CompositionLocalProvider(
        LocalCardColor provides cardColors.containerColor
    ) {
        // 思考状态融入消息流，而不是做成一张独立卡片。
        //
        // 原来这里是 Card（16dp 圆角 + 边框 + GLASS 玻璃三层），于是每次思考
        // 都在对话里插进一个「AI 卡片」，视觉重量跟消息气泡平级甚至更重。
        // 但思考是过程信息，不是一条消息 —— 它应该像旁注一样贴在消息流里。
        //
        // 现在去掉卡片容器：无背景、无边框、无圆角、无玻璃层。
        // 保留全部行为（展开/收起、实时内容、计时、自动关闭）和缩进对齐。
        // 用户自定义的 thinkingBubbleColor 仍然生效——只是不再默认画一张卡。
        val hasCustomSurface = thinkingBubbleColor != null
        Column(
            modifier = modifier
                .then(
                    if (hasCustomSurface) {
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(effectiveCardColors.containerColor)
                    } else {
                        Modifier
                    }
                )
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .animateContentSize(
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                ),
        ) {
                val visibleSteps = if (expanded || !canCollapse) {
                    steps
                } else {
                    steps.takeLast(collapsedVisibleCount)
                }

                // 显示展开/折叠按钮（统一在顶部）
                if (canCollapse) {
                    Row(
                        modifier = Modifier
                            .then(
                                if (shouldFillCollapseControlWidth) {
                                    Modifier.fillMaxWidth()
                                } else {
                                    Modifier
                                }
                            )
                            .clip(MaterialTheme.shapes.small)
                            .clickable { expanded = !expanded }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 左侧：图标区域（24.dp，和步骤图标对齐）
                        Box(
                            modifier = Modifier.width(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = if (expanded) HugeIcons.ArrowUp01 else HugeIcons.ArrowDown01,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }

                        // 右侧：文字区域（8.dp 间距后开始，和步骤 label 对齐）
                        Text(
                            modifier = Modifier.padding(start = 8.dp),
                            text = if (expanded) {
                                stringResource(R.string.chain_of_thought_collapse)
                            } else {
                                stringResource(
                                    R.string.chain_of_thought_show_more_steps,
                                    steps.size - collapsedVisibleCount
                                )
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                // 时间轴装饰线已移除。
                //
                // 思考状态不再是"时间线组件"：去掉步骤左侧那条竖直连线后，
                // 每一步就是消息流里独立的一行状态，不再暗示"这些步骤串成一条链"。
                // 多步骤时靠间距区分，层级由文字本身的缩进交代。
                val scope = remember { ChainOfThoughtScopeImpl() }
                Column {
                    visibleSteps.fastForEach { step ->
                        scope.content(step)
                    }
                }
            }
    }
}

private const val TRANSLUCENT_THINKING_BASE_ALPHA = 0.78f
private const val GLASS_THINKING_BASE_ALPHA = 0.56f

/**
 * [ChainOfThought] 内部使用的步骤渲染作用域。
 *
 * 通过该作用域可以声明单个步骤的图标、标题、附加信息以及可展开内容，
 * 并复用统一的时间线布局与交互行为。
 */
interface ChainOfThoughtScope {
    /**
     * 声明一个非受控步骤，由组件内部管理展开/折叠状态。
     *
     * @param icon 步骤图标
     * @param label 步骤标题区域
     * @param extra 标题右侧的附加信息
     * @param onClick 自定义点击行为；设置后优先于展开/折叠逻辑
     * @param collapsedAdaptiveWidth 是否在折叠且内容隐藏时使用自适应宽度
     * @param content 步骤展开后显示的内容；为 `null` 时步骤不可展开
     */
    @Composable
    fun ChainOfThoughtStep(
        icon: (@Composable () -> Unit)? = null,
        label: (@Composable () -> Unit),
        extra: (@Composable () -> Unit)? = null,
        onClick: (() -> Unit)? = null,
        collapsedAdaptiveWidth: Boolean = false,
        content: (@Composable () -> Unit)? = null,
    )

    /**
     * 声明一个受控步骤，由外部传入展开状态。
     *
     * 适合需要与外部状态联动的场景，例如“推理中预览 / 完成后收起”。
     *
     * @param expanded 当前是否处于展开状态
     * @param onExpandedChange 展开状态变化回调
     * @param icon 步骤图标
     * @param label 步骤标题区域
     * @param extra 标题右侧的附加信息
     * @param onClick 自定义点击行为；设置后优先于展开/折叠逻辑
     * @param collapsedAdaptiveWidth 是否在折叠且内容隐藏时使用自适应宽度
     * @param contentVisible 是否展示内容区域，可与 [expanded] 解耦
     * @param content 步骤内容；为 `null` 时步骤不可展开
     */
    @Composable
    fun ControlledChainOfThoughtStep(
        expanded: Boolean,
        onExpandedChange: (Boolean) -> Unit,
        icon: (@Composable () -> Unit)? = null,
        label: (@Composable () -> Unit),
        extra: (@Composable () -> Unit)? = null,
        onClick: (() -> Unit)? = null,
        collapsedAdaptiveWidth: Boolean = false,
        contentVisible: Boolean = expanded,
        content: (@Composable () -> Unit)? = null,
    )
}

private class ChainOfThoughtScopeImpl : ChainOfThoughtScope {
    @Composable
    override fun ChainOfThoughtStep(
        icon: @Composable (() -> Unit)?,
        label: @Composable (() -> Unit),
        extra: @Composable (() -> Unit)?,
        onClick: (() -> Unit)?,
        collapsedAdaptiveWidth: Boolean,
        content: @Composable (() -> Unit)?
    ) {
        var expanded by remember { mutableStateOf(false) }
        ChainOfThoughtStepContent(
            icon = icon,
            label = label,
            extra = extra,
            onClick = onClick,
            collapsedAdaptiveWidth = collapsedAdaptiveWidth,
            expanded = expanded,
            onExpandedChange = { expanded = it },
            contentVisible = expanded,
            content = content,
        )
    }

    @Composable
    override fun ControlledChainOfThoughtStep(
        expanded: Boolean,
        onExpandedChange: (Boolean) -> Unit,
        icon: @Composable (() -> Unit)?,
        label: @Composable (() -> Unit),
        extra: @Composable (() -> Unit)?,
        onClick: (() -> Unit)?,
        collapsedAdaptiveWidth: Boolean,
        contentVisible: Boolean,
        content: @Composable (() -> Unit)?
    ) {
        ChainOfThoughtStepContent(
            icon = icon,
            label = label,
            extra = extra,
            onClick = onClick,
            collapsedAdaptiveWidth = collapsedAdaptiveWidth,
            expanded = expanded,
            onExpandedChange = onExpandedChange,
            contentVisible = contentVisible,
            content = content,
        )
    }

    @Composable
    private fun ChainOfThoughtStepContent(
        icon: @Composable (() -> Unit)?,
        label: @Composable (() -> Unit),
        extra: @Composable (() -> Unit)?,
        onClick: (() -> Unit)?,
        collapsedAdaptiveWidth: Boolean,
        expanded: Boolean,
        onExpandedChange: (Boolean) -> Unit,
        contentVisible: Boolean,
        content: @Composable (() -> Unit)?
    ) {
        val hasContent = content != null
        val shouldFillMaxWidth = !collapsedAdaptiveWidth || contentVisible

        Column(
            modifier = Modifier.then(
                if (shouldFillMaxWidth) {
                    Modifier.fillMaxWidth()
                } else {
                    Modifier
                }
            ),
        ) {
            // Label 行：Icon + Label + Extra + 指示器
            Row(
                modifier = Modifier
                    .then(
                        if (shouldFillMaxWidth) {
                            Modifier.fillMaxWidth()
                        } else {
                            Modifier
                        }
                    )
                    .then(
                        if (onClick != null) {
                            Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable { onClick() }
                        } else if (hasContent) {
                            Modifier
                                .clip(MaterialTheme.shapes.small)
                                .clickable { onExpandedChange(!expanded) }
                        } else {
                            Modifier
                        }
                    )
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 节点视觉已移除：不再画"方形背景块 + 圆点"。
                //
                // 那个 20dp 不透明方块原本的职责是遮住背后的时间轴连线，连线删掉后
                // 它就只剩一个突兀的色块；里面的 8dp 圆点则是"流程节点"的暗示。
                // 两者一起构成了节点式视觉，跟"自然嵌入聊天流的一行状态"是冲突的。
                //
                // 现在：有 icon 就直接渲染（调用方自己决定画什么，例如头像），
                // 没有 icon 就完全不占位，文字直接顶到行首。
                if (icon != null) {
                    Box(
                        modifier = Modifier.size(20.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        icon()
                    }
                }

                // Label
                Box(
                    modifier = Modifier.then(
                        if (shouldFillMaxWidth) {
                            Modifier.weight(1f)
                        } else {
                            Modifier
                        }
                    )
                ) {
                    label()
                }

                // Extra
                if (extra != null) {
                    extra()
                }

                // 指示器：onClick 显示向右箭头，content 显示展开/折叠箭头
                if (onClick != null) {
                    Icon(
                        imageVector = HugeIcons.ArrowRight01,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (hasContent) {
                    Icon(
                        imageVector = if (expanded) HugeIcons.ArrowUp01 else HugeIcons.ArrowDown01,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 展开内容（缩进对齐 label）
            if (contentVisible && hasContent) {
                Box(
                    modifier = Modifier
                        .then(
                            if (shouldFillMaxWidth) {
                                Modifier.fillMaxWidth()
                            } else {
                                Modifier
                            }
                        )
                        // 缩进 32dp → 4dp：原来是为了让内容跟时间轴节点右侧的 label 对齐，
                        // 节点和连线都删了之后，深缩进只会让思考内容看着像被挂在某根轴上。
                        .padding(start = 4.dp, top = 4.dp, bottom = 8.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChainOfThoughtPreview() {
    // 定义步骤数据类
    data class StepData(
        val label: String,
        val icon: ImageVector?,
        val status: String?,
        val hasContent: Boolean = false,
        val hasOnClick: Boolean = false,
        val controlled: Boolean = false,
    )

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text("Chain of thought")
                    }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier.padding(innerPadding),
            ) {
                // 受控状态示例
                var controlledExpanded by remember { mutableStateOf(false) }

                ChainOfThought(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    steps = listOf(
                        StepData("Searching the web", HugeIcons.Search01, "3 results", hasContent = true),
                        StepData("Reading documents", HugeIcons.Sparkles, "Completed", hasOnClick = true),
                        StepData(
                            "Analyzing results (controlled)",
                            HugeIcons.Sparkles,
                            "In progress",
                            hasContent = true,
                            controlled = true
                        ),
                        StepData("Step without icon", null, null),
                        StepData("Final step", HugeIcons.Sparkles, "Done"),
                    ),
                    collapsedVisibleCount = 2,
                ) { step ->
                    val iconComposable: (@Composable () -> Unit)? = step.icon?.let {
                        {
                            Icon(
                                imageVector = it,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                    val labelComposable: @Composable () -> Unit = {
                        Text(step.label, style = MaterialTheme.typography.bodyMedium)
                    }
                    val extraComposable: (@Composable () -> Unit)? = step.status?.let {
                        {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    val onClickHandler: (() -> Unit)? = if (step.hasOnClick) {
                        { /* Open bottom sheet */ }
                    } else null
                    val contentComposable: (@Composable () -> Unit)? = if (step.hasContent) {
                        {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (step.label.contains("Search")) {
                                    listOf(
                                        "example.com - Example Domain",
                                        "docs.example.com - Documentation",
                                        "blog.example.com - Blog Post"
                                    ).forEach { result ->
                                        Text(
                                            text = "• $result",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                } else {
                                    Text(
                                        text = "This is expandable content showing detailed analysis. " +
                                            "It can contain multiple lines of text, code snippets, " +
                                            "or any other composable content.",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    } else null

                    if (step.controlled) {
                        // 受控版本
                        ControlledChainOfThoughtStep(
                            expanded = controlledExpanded,
                            onExpandedChange = { controlledExpanded = it },
                            icon = iconComposable,
                            label = labelComposable,
                            extra = extraComposable,
                            onClick = onClickHandler,
                            content = contentComposable,
                        )
                    } else {
                        // 非受控版本
                        ChainOfThoughtStep(
                            icon = iconComposable,
                            label = labelComposable,
                            extra = extraComposable,
                            onClick = onClickHandler,
                            content = contentComposable,
                        )
                    }
                }
            }
        }
    }
}
