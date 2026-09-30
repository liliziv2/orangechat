package me.rerere.rikkahub.ui.components.message

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.ai.pills.PillRegistry
import me.rerere.rikkahub.data.ai.pills.PillStore
import me.rerere.rikkahub.ui.components.ui.ListSelectableItem
import kotlin.uuid.Uuid

/**
 * 药盒。
 *
 * 只借 pilulier 的机制，不搬它的前端组件：这里沿用仓库既有的版式 —— ModalBottomSheet
 * 装一列 [ListSelectableItem]（记忆库选择、会话多选都是这个组件），不新造一套卡片语言。
 *
 * 选中的药丸写进 [PillStore]，挂在 [messageId] 这条消息上；真正注入发生在
 * [me.rerere.rikkahub.data.ai.transformers.PillTransformer]，生成结束后由
 * `ChatService` 清掉（阅后即焚）。
 */
@Composable
fun PillPickerSheet(
    messageId: Uuid,
    onDismissRequest: () -> Unit,
) {
    val pending by PillStore.pending.collectAsState()
    val selected = pending[messageId].orEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(R.string.pill_box_title),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = stringResource(R.string.pill_box_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            PillRegistry.all.forEach { pill ->
                ListSelectableItem(
                    key = pill.code,
                    selectedKeys = selected,
                    onSelectChange = { PillStore.toggle(messageId, pill.code) },
                ) {
                    Column(
                        modifier = Modifier.padding(vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = pill.name,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (pill.whenToUse.isNotEmpty()) {
                            Text(
                                text = pill.whenToUse,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
