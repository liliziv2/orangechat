package me.rerere.rikkahub.ui.pages.chat

import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Folder01
import me.rerere.hugeicons.stroke.Forward02
import me.rerere.hugeicons.stroke.Pin
import me.rerere.hugeicons.stroke.PinOff
import me.rerere.hugeicons.stroke.Refresh01
import me.rerere.hugeicons.stroke.Delete01
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import me.rerere.rikkahub.ui.theme.materialModeBorderStroke
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.datastore.DisplayMaterialMode
import me.rerere.rikkahub.data.model.Conversation
import me.rerere.rikkahub.ui.theme.extendColors
import androidx.compose.ui.text.TextStyle
import me.rerere.rikkahub.utils.toLocalString
import java.time.LocalDate
import java.time.ZoneId
import kotlin.uuid.Uuid

/**
 * Represents different types of items in the conversation list
 */
sealed class ConversationListItem {
    data class DateHeader(
        val date: LocalDate,
        val label: String
    ) : ConversationListItem()
    data object PinnedHeader : ConversationListItem()
    data class Item(
        val conversation: Conversation
    ) : ConversationListItem()
}

/**
 * 会话列表的字号放大系数。
 *
 * 会话标题与日期标签原本和侧栏其它文字同一档，读起来偏小；这两处各放大 15%，
 * 日期仍然比标题小一级。
 *
 * **不是写死字号**：下面两个样式都以 `MaterialTheme.typography` 为基准再乘这个系数，
 * 所以系统字体缩放、主题字体设置照样生效，只是整体放大一档。
 */
private const val ConversationListTextScale = 1.15f

/**
 * 会话标题样式：在当前计算字号的基准上放大约 15%，行高同比放大。
 *
 * 行高必须一起放大 —— 只放大字号会让行高小于字高，单行文本会被裁掉上下沿。
 * 字重保持 bodyMedium 自带的那一档，不额外加粗（只有标题特别长时才靠省略号，
 * 不靠字重区分层级）。
 */
@Composable
private fun conversationTitleStyle(): TextStyle {
    val base = MaterialTheme.typography.bodyMedium
    return base.copy(
        fontSize = base.fontSize * ConversationListTextScale,
        lineHeight = base.lineHeight * ConversationListTextScale,
    )
}

/**
 * 日期标签样式：同样在当前基准上放大 15%，但基准取 labelMedium ——
 * 比标题的 bodyMedium 小一级，放大之后这一级差仍然保留。
 */
@Composable
private fun conversationDateStyle(): TextStyle {
    val base = MaterialTheme.typography.labelMedium
    return base.copy(
        fontSize = base.fontSize * ConversationListTextScale,
        lineHeight = base.lineHeight * ConversationListTextScale,
    )
}

@Composable
fun ColumnScope.ConversationList(
    current: Conversation,
    conversations: LazyPagingItems<ConversationListItem>,
    conversationJobs: Collection<Uuid>,
    listState: LazyListState,
    modifier: Modifier = Modifier,
    drawerItemAlpha: Float = 1f,
    materialMode: DisplayMaterialMode = DisplayMaterialMode.FLAT,
    onClick: (Conversation) -> Unit = {},
    onDelete: (Conversation) -> Unit = {},
    onRegenerateTitle: (Conversation) -> Unit = {},
    onPin: (Conversation) -> Unit = {},
    onMoveToAssistant: (Conversation) -> Unit = {},
    onMoveToFolder: (Conversation) -> Unit = {}
) {
    var hasScrolledToCurrent by remember(current.id) { mutableStateOf(false) }

    LaunchedEffect(current.id, conversations.itemCount, hasScrolledToCurrent) {
        if (hasScrolledToCurrent) return@LaunchedEffect
        val currentIndex = conversations.itemSnapshotList.items.indexOfFirst {
            (it as? ConversationListItem.Item)?.conversation?.id == current.id
        }
        if (currentIndex >= 0) {
            val isVisible = listState.layoutInfo.visibleItemsInfo.any { it.index == currentIndex }
            if (!isVisible) {
                listState.scrollToItem(currentIndex)
            }
            hasScrolledToCurrent = true
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier,
        // 行距 8dp：条目已有内边距，行距再小就会读成挤在一起。
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (conversations.itemCount == 0) {
            item {
                ConversationListMaterialContainer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    drawerItemAlpha = drawerItemAlpha,
                    materialMode = materialMode,
                ) {
                    Text(
                        text = stringResource(id = R.string.chat_page_no_conversations),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        items(
            count = conversations.itemCount,
            key = conversations.itemKey { item ->
                when (item) {
                    is ConversationListItem.DateHeader -> "date_${item.date}"
                    is ConversationListItem.PinnedHeader -> "pinned_header"
                    is ConversationListItem.Item -> item.conversation.id.toString()
                }
            }
        ) { index ->
            when (val item = conversations[index]) {
                is ConversationListItem.DateHeader -> {
                    DateHeaderItem(
                        label = item.label,
                        modifier = Modifier.animateItem()
                    )
                }

                is ConversationListItem.PinnedHeader -> {
                    PinnedHeader(
                        drawerItemAlpha = drawerItemAlpha,
                        materialMode = materialMode,
                        modifier = Modifier.animateItem()
                    )
                }

                is ConversationListItem.Item -> {
                    ConversationItem(
                        conversation = item.conversation,
                        selected = item.conversation.id == current.id,
                        loading = item.conversation.id in conversationJobs,
                        onClick = onClick,
                        onDelete = onDelete,
                        onRegenerateTitle = onRegenerateTitle,
                        onPin = onPin,
                        onMoveToAssistant = onMoveToAssistant,
                        onMoveToFolder = onMoveToFolder,
                        drawerItemAlpha = drawerItemAlpha,
                        materialMode = materialMode,
                        modifier = Modifier.animateItem()
                    )
                }

                null -> {
                    // Placeholder for loading state
                }
            }
        }
    }
}

@Composable
private fun DateHeaderItem(
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            // 日期上留 12dp 与上一条拉开，下只留 2dp 贴近它统领的那一天。
            .padding(start = 12.dp, end = 12.dp, top = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // 日期分组保留，但仍然是安静的「小标题」：不再是 primary 色 + 加粗。
        // 侧栏里主色应该只留给「当前会话」和新建入口。
        //
        // 字号放大 15%（基准 labelMedium，仍比标题小一级）。对比度同步提上来：
        // 之前 0.72 的 alpha 在浅色主题下淡到快看不见，这里不再压 alpha，
        // 直接用 onSurfaceVariant 本色 —— 它本身已经比标题弱一档，够区分了。
        Text(
            text = label,
            style = conversationDateStyle(),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun PinnedHeader(
    drawerItemAlpha: Float = 1f,
    materialMode: DisplayMaterialMode,
    modifier: Modifier = Modifier
) {
    ConversationListMaterialContainer(
        modifier = modifier
            .fillMaxWidth(),
        shape = RectangleShape,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        drawerItemAlpha = drawerItemAlpha,
        materialMode = materialMode,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = HugeIcons.Pin,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.size(8.dp))
            Text(
                text = stringResource(R.string.pinned_chats),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ConversationItem(
    conversation: Conversation,
    selected: Boolean,
    loading: Boolean,
    modifier: Modifier = Modifier,
    drawerItemAlpha: Float = 1f,
    materialMode: DisplayMaterialMode,
    onDelete: (Conversation) -> Unit = {},
    onRegenerateTitle: (Conversation) -> Unit = {},
    onPin: (Conversation) -> Unit = {},
    onMoveToAssistant: (Conversation) -> Unit = {},
    onMoveToFolder: (Conversation) -> Unit = {},
    onClick: (Conversation) -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val itemShape = RoundedCornerShape(50f)
    // 选中态用主题已有的低对比色，而不是 elevation 提亮出来的厚底块。
    // 透明度在主题色**原有 alpha 上相乘**，不覆盖它：主题自身的半透明设定
    // 与侧栏元素透明度滑块都还要拿它当参照。
    val selectedContainer = MaterialTheme.colorScheme.surfaceVariant
    val backgroundColor = if (selected) {
        selectedContainer.copy(alpha = selectedContainer.alpha * 0.5f)
    } else {
        Color.Transparent
    }
    var showDropdownMenu by remember {
        mutableStateOf(false)
    }
    ConversationListMaterialContainer(
        modifier = modifier
            .clip(itemShape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = LocalIndication.current,
                onClick = { onClick(conversation) },
                onLongClick = {
                    showDropdownMenu = true
                }
            ),
        shape = itemShape,
        color = backgroundColor,
        drawerItemAlpha = drawerItemAlpha,
        materialMode = materialMode,
        hasBackground = selected,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 标题吃掉剩余宽度：右侧置顶/加载标记先占位，长标题在这里省略，
            // 不会把标记挤出条目。整行点击与长按行为不变。
            Text(
                text = conversation.title.ifBlank { stringResource(id = R.string.chat_page_new_message) },
                // 字号放大 15%，行高同比放大；单行省略与点击/长按行为都不变。
                style = conversationTitleStyle(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )

            // 置顶图标
            AnimatedVisibility(conversation.isPinned) {
                Icon(
                    imageVector = HugeIcons.Pin,
                    contentDescription = "Pinned",
                    modifier = Modifier.size(12.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            AnimatedVisibility(loading) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.extendColors.green6)
                        .size(4.dp)
                        .semantics {
                            contentDescription = "Loading"
                        }
                )
            }
            DropdownMenu(
                expanded = showDropdownMenu,
                onDismissRequest = { showDropdownMenu = false },
                border = materialModeBorderStroke(),
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            if (conversation.isPinned) stringResource(R.string.unpin_chat) else stringResource(R.string.pin_chat)
                        )
                    },
                    onClick = {
                        onPin(conversation)
                        showDropdownMenu = false
                    },
                    leadingIcon = {
                        Icon(
                            if (conversation.isPinned) HugeIcons.PinOff else HugeIcons.Pin,
                            null
                        )
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text(stringResource(id = R.string.chat_page_regenerate_title))
                    },
                    onClick = {
                        onRegenerateTitle(conversation)
                        showDropdownMenu = false
                    },
                    leadingIcon = {
                        Icon(HugeIcons.Refresh01, null)
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text(stringResource(R.string.chat_page_move_to_assistant))
                    },
                    onClick = {
                        onMoveToAssistant(conversation)
                        showDropdownMenu = false
                    },
                    leadingIcon = {
                        Icon(HugeIcons.Forward02, null)
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text(stringResource(R.string.chat_page_move_to_folder))
                    },
                    onClick = {
                        onMoveToFolder(conversation)
                        showDropdownMenu = false
                    },
                    leadingIcon = {
                        Icon(HugeIcons.Folder01, null)
                    }
                )

                DropdownMenuItem(
                    text = {
                        Text(stringResource(id = R.string.chat_page_delete))
                    },
                    onClick = {
                        onDelete(conversation)
                        showDropdownMenu = false
                    },
                    leadingIcon = {
                        Icon(HugeIcons.Delete01, null)
                    }
                )
            }
        }
    }
}

@Composable
private fun ConversationListMaterialContainer(
    modifier: Modifier = Modifier,
    shape: Shape,
    color: Color,
    drawerItemAlpha: Float,
    materialMode: DisplayMaterialMode,
    hasBackground: Boolean = true,
    content: @Composable () -> Unit,
) {
    if (!hasBackground) {
        Box(modifier = modifier) {
            content()
        }
        return
    }

    val onSurface = MaterialTheme.colorScheme.onSurface
    val borderModifier = when (materialMode) {
        DisplayMaterialMode.TRANSLUCENT -> Modifier.border(
            width = 1.dp,
            color = onSurface.copy(alpha = 0.05f * drawerItemAlpha),
            shape = shape,
        )

        DisplayMaterialMode.GLASS -> Modifier.border(
            width = 1.dp,
            color = onSurface.copy(alpha = 0.04f * drawerItemAlpha),
            shape = shape,
        )

        DisplayMaterialMode.FLAT,
        DisplayMaterialMode.FOLLOW_THEME -> Modifier
    }

    // 侧栏元素透明度（drawerItemAlpha）必须在**三种材质下都生效**。
    //
    // 旧实现：FLAT / FOLLOW_THEME 直接返回 color —— 平面模式下这个滑块完全失效，
    // 这是用户反复反馈「元素透明度滑块没反应」的真实原因。
    //
    // 现在统一乘上滑块，乘的是「color 已有 alpha × 滑块」而不是把 alpha 覆盖成
    // 滑块值，避免把极淡底色变成不透明块。三种模式的差别只体现在装饰层
    // （描边 / 玻璃高光），不体现在滑块是否起作用。
    val backgroundColor = color.copy(alpha = color.alpha * drawerItemAlpha)

    Box(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .then(borderModifier),
    ) {
        if (materialMode == DisplayMaterialMode.GLASS) {
            ConversationListGlassLayers(
                shape = shape,
                drawerItemAlpha = drawerItemAlpha,
            )
        }
        content()
    }
}

@Composable
private fun BoxScope.ConversationListGlassLayers(
    shape: Shape,
    drawerItemAlpha: Float,
) {
    val colorScheme = MaterialTheme.colorScheme
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
