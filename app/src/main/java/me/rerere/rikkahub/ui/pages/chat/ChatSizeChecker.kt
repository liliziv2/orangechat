/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.chat

import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Alert01
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import java.util.Locale
import me.rerere.ai.core.MessageRole
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.model.Conversation

// 消息节点数量警告阈值
const val MESSAGE_NODE_WARNING_THRESHOLD = 768
const val LAST_ASSISTANT_INPUT_TOKEN_WARNING_THRESHOLD = 300_000

/** 上下文用量转黄 / 转红的比例阈值 */
const val CONTEXT_USAGE_WARN_RATIO = 0.85f
const val CONTEXT_USAGE_DANGER_RATIO = 0.95f

data class ConversationSizeInfo(
    val nodeCount: Int,
    val lastAssistantInputTokens: Int,
    val exceedNodeCountThreshold: Boolean,
    val exceedInputTokenThreshold: Boolean,
    val showWarning: Boolean,
    /** 当前模型的上下文窗口大小；模型表里没有记录时为 null */
    val contextLength: Int? = null,
) {
    /**
     * 上下文已用比例（0..1+）。没有上限数据或还没有过一次生成时为 null。
     *
     * 注意这是"上一次生成实际带了多少"，不是"下一次会带多少"——下一轮会更多。
     * 也可能超过 1：某些网关报的 promptTokens 含缓存创建部分，或模型表里的上限偏保守。
     */
    val contextUsageRatio: Float?
        get() {
            val limit = contextLength ?: return null
            if (limit <= 0 || lastAssistantInputTokens <= 0) return null
            return lastAssistantInputTokens.toFloat() / limit
        }
}

@Composable
fun rememberConversationSizeInfo(
    conversation: Conversation,
    contextLength: Int? = null,
): ConversationSizeInfo {
    return remember(conversation.messageNodes, contextLength) {
        val nodeCount = conversation.messageNodes.size
        val lastAssistantInputTokens = conversation.messageNodes.asReversed()
            .map { it.currentMessage }
            .firstOrNull { it.role == MessageRole.ASSISTANT }
            ?.usage
            ?.promptTokens
            ?: 0
        val exceedNodeCountThreshold = nodeCount > MESSAGE_NODE_WARNING_THRESHOLD
        val exceedInputTokenThreshold = lastAssistantInputTokens > LAST_ASSISTANT_INPUT_TOKEN_WARNING_THRESHOLD
        ConversationSizeInfo(
            nodeCount = nodeCount,
            lastAssistantInputTokens = lastAssistantInputTokens,
            exceedNodeCountThreshold = exceedNodeCountThreshold,
            exceedInputTokenThreshold = exceedInputTokenThreshold,
            showWarning = exceedNodeCountThreshold && exceedInputTokenThreshold,
            contextLength = contextLength,
        )
    }
}

/**
 * 上下文用量条：一条细线 + "已用/上限"。
 *
 * 这是表，不是闸：它不会裁剪或压缩任何消息，满了照发，真正的上限由上游端点划。
 * 存在的意义是让"这轮带了多少上下文"这件事在超限报错之前就看得见。
 *
 * 数据来源是上一条助手消息报的 promptTokens，所以第一轮生成完成前不显示。
 */
@Composable
fun ContextUsageBar(
    sizeInfo: ConversationSizeInfo,
    modifier: Modifier = Modifier,
) {
    val ratio = sizeInfo.contextUsageRatio ?: return
    val limit = sizeInfo.contextLength ?: return
    val clamped = ratio.coerceIn(0f, 1f)
    val color = when {
        ratio >= CONTEXT_USAGE_DANGER_RATIO -> MaterialTheme.colorScheme.error
        ratio >= CONTEXT_USAGE_WARN_RATIO -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "${formatTokenCount(sizeInfo.lastAssistantInputTokens)} / ${formatTokenCount(limit)}",
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
        LinearProgressIndicator(
            progress = { clamped },
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp),
            color = color,
            trackColor = color.copy(alpha = 0.16f),
            drawStopIndicator = {},
        )
    }
}

/** 12345 -> 12.3k，1200000 -> 1.2M。上下文数量级下精确到个位没有意义。 */
private fun formatTokenCount(tokens: Int): String = when {
    tokens >= 1_000_000 -> String.format(Locale.US, "%.1fM", tokens / 1_000_000.0)
    tokens >= 1_000 -> String.format(Locale.US, "%.1fk", tokens / 1_000.0)
    else -> tokens.toString()
}

@Composable
fun ConversationSizeWarningDialog(
    sizeInfo: ConversationSizeInfo,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = HugeIcons.Alert01,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary
            )
        },
        title = {
            Text(text = stringResource(R.string.chat_size_dialog_title))
        },
        text = {
            Text(text = stringResource(R.string.chat_size_dialog_content, sizeInfo.nodeCount))
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.confirm))
            }
        }
    )
}
