package me.rerere.rikkahub.ui.components.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.MoreVertical
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.theme.materialModeBorderStroke

/**
 * 列表项次要操作（编辑 / 复制 / 导出 / 删除 …）的统一描述。
 *
 * - [destructive] = true 时整行用 error 色渲染，约定用于删除等破坏性操作。
 * - [enabled] = false 时按 Material3 的禁用透明度变灰。
 */
@Immutable
data class ItemAction(
    val text: String,
    val icon: ImageVector,
    val destructive: Boolean = false,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

/**
 * 列表项右侧统一的「⋮」次要操作菜单。
 *
 * 约定：点击列表项本体执行主操作（进入详情 / 编辑），⋮ 承载次要操作；
 * 删除类动作放在 [actions] 末尾并置 `destructive = true`，调用方负责二次确认。
 */
@Composable
fun ItemActionMenu(
    actions: List<ItemAction>,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        IconButton(onClick = { expanded = true }) {
            Icon(
                imageVector = HugeIcons.MoreVertical,
                contentDescription = stringResource(R.string.more_options),
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            border = materialModeBorderStroke(),
        ) {
            actions.forEach { action ->
                val actionColor = when {
                    !action.enabled -> MaterialTheme.colorScheme.onSurface.copy(alpha = DISABLED_ALPHA)
                    action.destructive -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                }
                DropdownMenuItem(
                    text = { Text(text = action.text, color = actionColor) },
                    leadingIcon = {
                        Icon(
                            imageVector = action.icon,
                            contentDescription = null,
                            tint = actionColor,
                        )
                    },
                    enabled = action.enabled,
                    onClick = {
                        expanded = false
                        action.onClick()
                    },
                )
            }
        }
    }
}

/** 与 Material3 `DisabledAlpha` 一致，保证禁用项外观不变。 */
private const val DISABLED_ALPHA = 0.38f
