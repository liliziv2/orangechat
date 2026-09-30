package me.rerere.rikkahub.ui.pages.setting

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.ui.Alignment
import me.rerere.rikkahub.ui.components.ui.SettingsRowDivider
import me.rerere.rikkahub.ui.components.ui.SettingsSectionTitle
import me.rerere.rikkahub.ui.theme.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.datastore.DisplaySetting
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.permission.PermissionManager
import me.rerere.rikkahub.ui.components.ui.permission.PermissionNotification
import me.rerere.rikkahub.ui.components.ui.permission.rememberPermissionState
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingDisplayNotificationPage(vm: SettingVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    var displaySetting by remember(settings) { mutableStateOf(settings.displaySetting) }

    fun updateDisplaySetting(setting: DisplaySetting) {
        displaySetting = setting
        vm.updateSettings(settings.copy(displaySetting = setting))
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val permissionState = rememberPermissionState(
        permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) setOf(
            PermissionNotification
        ) else emptySet(),
    )
    PermissionManager(permissionState = permissionState)

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("通知与TTS", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = settingsScaffoldContainerColor(CustomColors.topBarColors.containerColor)
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = contentPadding + PaddingValues(vertical = 8.dp),
        ) {
            // 通知设置
            item { SettingsSectionTitle("通知设置") }
            item {
                SettingsSwitchRow(
                    title = stringResource(R.string.setting_display_page_notification_message_generated),
                    description = stringResource(R.string.setting_display_page_notification_message_generated_desc),
                    checked = displaySetting.enableNotificationOnMessageGeneration,
                    onCheckedChange = {
                        if (it && !permissionState.allPermissionsGranted) {
                            permissionState.requestPermissions()
                        }
                        updateDisplaySetting(displaySetting.copy(enableNotificationOnMessageGeneration = it))
                    },
                )
            }
            if (displaySetting.enableNotificationOnMessageGeneration) {
                item {
                    SettingsSwitchRow(
                        title = stringResource(R.string.setting_display_page_live_update_notification),
                        description = stringResource(R.string.setting_display_page_live_update_notification_desc),
                        checked = displaySetting.enableLiveUpdateNotification,
                        onCheckedChange = {
                            updateDisplaySetting(displaySetting.copy(enableLiveUpdateNotification = it))
                        },
                    )
                }
            }

            // TTS 设置
            item { SettingsSectionTitle(stringResource(R.string.setting_page_tts_settings)) }
            item {
                SettingsSwitchRow(
                    title = stringResource(R.string.setting_display_page_tts_only_read_quoted_title),
                    description = stringResource(R.string.setting_display_page_tts_only_read_quoted_desc),
                    checked = displaySetting.ttsOnlyReadQuoted,
                    onCheckedChange = {
                        updateDisplaySetting(displaySetting.copy(ttsOnlyReadQuoted = it))
                    },
                )
            }
            item {
                SettingsSwitchRow(
                    title = stringResource(R.string.setting_display_page_tts_english_only_title),
                    description = stringResource(R.string.setting_display_page_tts_english_only_desc),
                    checked = displaySetting.ttsEnglishOnly,
                    onCheckedChange = {
                        updateDisplaySetting(displaySetting.copy(ttsEnglishOnly = it))
                    },
                )
            }
            item {
                SettingsSwitchRow(
                    title = stringResource(R.string.setting_display_page_auto_play_tts_title),
                    description = stringResource(R.string.setting_display_page_auto_play_tts_desc),
                    checked = displaySetting.autoPlayTTSAfterGeneration,
                    onCheckedChange = {
                        updateDisplaySetting(displaySetting.copy(autoPlayTTSAfterGeneration = it))
                    },
                )
            }
            item {
                SettingsSwitchRow(
                    title = "自动生成语音条",
                    description = "回复结束后自动合成一条语音条插进消息里，可回放。开启后不再另外朗读一遍。每条回复都会走一次 TTS 合成，按量计费的服务商会产生费用",
                    checked = displaySetting.autoVoiceMessageAfterGeneration,
                    onCheckedChange = {
                        updateDisplaySetting(displaySetting.copy(autoVoiceMessageAfterGeneration = it))
                    },
                )
            }
            item {
                SettingsSwitchRow(
                    title = "语音条旁保留文字",
                    description = "关掉后，有语音条的回复只显示语音条。语音条本身不再带展开文字的按钮，想看原文就开着这项",
                    checked = displaySetting.showTextWithVoiceMessage,
                    onCheckedChange = {
                        updateDisplaySetting(displaySetting.copy(showTextWithVoiceMessage = it))
                    },
                )
            }
        }
    }
}

/**
 * 一行开关设置：标题 + 说明在左，`Switch` 直接做行末控件。
 *
 * 与 `SettingModelPage.ModelFeatureRow` 同一档几何（行内边距、标题 `titleSmall`、
 * 说明 `labelSmall` 且变暗、行末 `end = 4.dp` 让出控件的触摸区），
 * 区别只在这里的控件在行末、而不是另起一行。
 *
 * `end = 4.dp` 是给 `Switch` 留的：它的触摸区比轨道宽，视觉右边距因此落在 16dp 左右。
 */
@Composable
private fun SettingsSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 14.dp, end = 4.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
        SettingsRowDivider()
    }
}