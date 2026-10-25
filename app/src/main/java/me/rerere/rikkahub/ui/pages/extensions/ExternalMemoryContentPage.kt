package me.rerere.rikkahub.ui.pages.extensions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Alert01
import me.rerere.hugeicons.stroke.Brain01
import me.rerere.hugeicons.stroke.Refresh01
import me.rerere.rikkahub.data.datastore.getCurrentAssistant
import me.rerere.rikkahub.data.model.ExternalMemory
import me.rerere.rikkahub.data.service.ExternalMemoryService
import me.rerere.rikkahub.data.service.ExternalMemorySummary
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.pages.setting.settingsScaffoldContainerColor
import me.rerere.rikkahub.ui.theme.LargeFlexibleTopAppBar
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.theme.CustomColors

/**
 * 外置记忆（Supabase）记忆卡片只读查看页。
 *
 * 为什么需要它：
 * 记忆卡片实际存放在远端 `memory_summaries` 表里，`GenerationHandler` 每轮
 * 对话都会读它并拼成「近期记忆卡片」注入 system prompt —— 但此前**没有任何
 * UI 能看到这些内容**。用户只知道 AI「记住了东西」，看不到具体记了什么，
 * 记错了也没法察觉。
 *
 * 本地 `MemoryBankPage` 查的是 Room，与外置记忆是两条独立轨道，不能复用，
 * 所以这里单独做一个只读列表页。
 *
 * 本轮范围（只读）：
 * - 列出该配置下最近 N 条记忆卡片（`queryLatestSummaries`）；
 * - 下拉刷新；
 * - 失败时显示明确错误，不静默吞掉。
 * 不做删除/编辑，避免动到远端数据。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalMemoryContentPage(
    memoryId: String,
    onBack: () -> Unit,
) {
    val settings = LocalSettings.current
    val memory = remember(memoryId, settings.externalMemories) {
        settings.externalMemories.find { it.id.toString() == memoryId }
    }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // 列表状态：用一个密封结果表达，避免「空列表」与「加载失败」被混为一谈。
    var items by remember(memoryId) { mutableStateOf<List<ExternalMemorySummary>>(emptyList()) }
    var errorMessage by remember(memoryId) { mutableStateOf<String?>(null) }
    var loading by remember(memoryId) { mutableStateOf(false) }
    var refreshTick by remember(memoryId) { mutableStateOf(0) }

    // 取哪个助手的卡片：优先当前助手，与 GenerationHandler 注入上下文时的口径一致。
    val assistantId = remember(settings) { settings.getCurrentAssistant().id.toString() }

    LaunchedEffect(memoryId, refreshTick) {
        val target = memory ?: return@LaunchedEffect
        loading = true
        errorMessage = null
        val service = ExternalMemoryService(target)
        // 取比默认召回更多一些，方便用户往回翻；上限 100 避免一次拉爆。
        val limit = (target.recallCount * 4).coerceIn(10, 100)
        service.queryLatestSummaries(assistantId = assistantId, limit = limit)
            .onSuccess { list ->
                items = list
                loading = false
            }
            .onFailure { e ->
                // 错误脱敏：只保留可读信息，绝不把 supabaseKey 带出来。
                errorMessage = sanitizeError(e)
                loading = false
            }
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(memory?.name?.ifBlank { "记忆内容" } ?: "记忆内容") },
                navigationIcon = { BackButton() },
                actions = {
                    IconButton(onClick = { refreshTick++ }, enabled = !loading) {
                        Icon(HugeIcons.Refresh01, contentDescription = "刷新")
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = settingsScaffoldContainerColor(CustomColors.topBarColors.containerColor),
    ) { innerPadding ->
        when {
            memory == null -> {
                // 配置被删除/ID 失效时给明确提示，而不是空白页。
                EmptyState(
                    icon = HugeIcons.Alert01,
                    title = "找不到这个记忆库配置",
                    subtitle = "它可能已被删除，请返回上一页重新选择。",
                    modifier = Modifier.padding(innerPadding),
                )
            }

            else -> {
                PullToRefreshBox(
                    isRefreshing = loading,
                    onRefresh = { refreshTick++ },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // 顶部一行说明：让用户知道这是哪张表、哪个助手、多少条。
                        item {
                            SummaryHeader(
                                tableName = memory?.summariesTableName.orEmpty(),
                                count = items.size,
                                loading = loading,
                            )
                        }

                        if (errorMessage != null) {
                            item {
                                ErrorCard(message = errorMessage!!)
                            }
                        } else if (!loading && items.isEmpty()) {
                            item {
                                EmptyState(
                                    icon = HugeIcons.Brain01,
                                    title = "还没有记忆卡片",
                                    subtitle = "这个记忆库在 Supabase 的 " +
                                        "${memory?.summariesTableName.orEmpty()} 表里暂时没有记录。",
                                )
                            }
                        }

                        items(items, key = { it.id }) { summary ->
                            SummaryCard(summary = summary)
                        }
                    }
                }
            }
        }
    }
}

/** 错误信息脱敏：去掉任何可能出现的 key / token 片段。 */
private fun sanitizeError(e: Throwable): String {
    val raw = e.message?.takeIf { it.isNotBlank() } ?: e::class.simpleName ?: "未知错误"
    // Supabase 的错误体可能很长，截断并剔除疑似凭证的片段。
    return raw
        .replace(Regex("(?i)(apikey|authorization|bearer|key)\\s*[:=]\\s*\\S+"), "$1=***")
        .take(300)
}

@Composable
private fun SummaryHeader(
    tableName: String,
    count: Int,
    loading: Boolean,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = if (loading) "正在读取…" else "共 $count 条记忆卡片",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "数据表：$tableName（Supabase 远端）",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun SummaryCard(summary: ExternalMemorySummary) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // 时间：远端返回的是 ISO 字符串，原样显示比强行解析更稳。
            if (summary.createdAt.isNotBlank()) {
                Text(
                    text = summary.createdAt.replace('T', ' ').removeSuffix("Z").take(19),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
            Text(
                text = summary.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                imageVector = HugeIcons.Alert01,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "读取失败",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
                    maxLines = 6,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(44.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
