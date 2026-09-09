/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.data.datastore.AppearancePreset
import me.rerere.rikkahub.data.datastore.DisplaySetting
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import org.koin.androidx.compose.koinViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 外观预设：4 个槽位，保存当前的外观设置快照，一键切换。
 * 预设只覆盖视觉字段（颜色、透明度、字体、气泡、背景图等），不动行为开关和用户资料。
 */
@Composable
fun SettingDisplayPresetPage(vm: SettingVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val displaySetting = settings.displaySetting
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // 保存对话框：记录目标槽位与名称
    var savingSlot by remember { mutableIntStateOf(-1) }
    var savingName by remember { mutableStateOf("") }
    var pendingApply by remember { mutableStateOf<AppearancePreset?>(null) }
    var pendingDelete by remember { mutableStateOf<AppearancePreset?>(null) }

    fun updateDisplay(setting: DisplaySetting) {
        vm.updateSettings(settings.copy(displaySetting = setting))
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("外观预设") },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = CustomColors.topBarColors.containerColor
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding + PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    colors = CustomColors.cardColorsOnSurfaceContainer
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "把当前的配色、透明度、字体、气泡样式保存成预设，之后一键切回。",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = "预设不包含用户资料和功能开关。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                CardGroup(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    title = { Text("预设槽位") },
                ) {
                    repeat(DisplaySetting.APPEARANCE_PRESET_SLOTS) { slot ->
                        val preset = displaySetting.appearancePresets.firstOrNull { it.slot == slot }
                        item(
                            headlineContent = {
                                Text(preset?.name?.ifBlank { "预设 ${slot + 1}" } ?: "预设 ${slot + 1}")
                            },
                            supportingContent = {
                                Text(
                                    text = if (preset == null) {
                                        "空槽位"
                                    } else {
                                        "保存于 ${preset.savedAt.toDisplayTime()}"
                                    }
                                )
                            },
                            trailingContent = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    if (preset != null) {
                                        OutlinedButton(onClick = { pendingApply = preset }) {
                                            Text("应用")
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            savingSlot = slot
                                            savingName = preset?.name
                                                ?.ifBlank { "预设 ${slot + 1}" }
                                                ?: "预设 ${slot + 1}"
                                        }
                                    ) {
                                        Text(if (preset == null) "保存" else "覆盖")
                                    }
                                }
                            },
                            onClick = if (preset != null) {
                                { pendingDelete = preset }
                            } else {
                                null
                            },
                        )
                    }
                }
            }

            item {
                Text(
                    text = "点击已保存的槽位可删除该预设。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }

    if (savingSlot >= 0) {
        AlertDialog(
            onDismissRequest = { savingSlot = -1 },
            title = { Text("保存外观预设") },
            text = {
                OutlinedTextField(
                    value = savingName,
                    onValueChange = { savingName = it },
                    label = { Text("预设名称") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val slot = savingSlot
                        val newPreset = AppearancePreset(
                            slot = slot,
                            name = savingName.ifBlank { "预设 ${slot + 1}" },
                            snapshot = displaySetting.toAppearanceSnapshot(),
                            savedAt = System.currentTimeMillis(),
                        )
                        updateDisplay(
                            displaySetting.copy(
                                appearancePresets = displaySetting.appearancePresets
                                    .filterNot { it.slot == slot } + newPreset
                            )
                        )
                        savingSlot = -1
                    }
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { savingSlot = -1 }) {
                    Text("取消")
                }
            }
        )
    }

    pendingApply?.let { preset ->
        AlertDialog(
            onDismissRequest = { pendingApply = null },
            title = { Text("应用预设") },
            text = {
                Text("将当前外观替换为「${preset.name.ifBlank { "预设 ${preset.slot + 1}" }}」？当前未保存的外观改动会丢失。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        updateDisplay(displaySetting.applyAppearanceSnapshot(preset.snapshot))
                        pendingApply = null
                    }
                ) {
                    Text("应用")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingApply = null }) {
                    Text("取消")
                }
            }
        )
    }

    pendingDelete?.let { preset ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除预设") },
            text = { Text("删除「${preset.name.ifBlank { "预设 ${preset.slot + 1}" }}」？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        updateDisplay(
                            displaySetting.copy(
                                appearancePresets = displaySetting.appearancePresets
                                    .filterNot { it.slot == preset.slot }
                            )
                        )
                        pendingDelete = null
                    }
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text("取消")
                }
            }
        )
    }
}

private val presetTimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

private fun Long.toDisplayTime(): String {
    if (this <= 0L) return "未知时间"
    return presetTimeFormatter.format(
        Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault())
    )
}
