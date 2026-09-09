/*
 * 橘瓣 OrangeChat
 * 衍生自 RikkaHub (https://github.com/rikkahub/rikkahub)，原作者 RE
 * 本项目基于 GNU AGPL v3 开源，详见根目录 LICENSE 文件
 */

package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.data.datastore.DisplaySetting
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import org.koin.androidx.compose.koinViewModel

/**
 * 我的资料卡：昵称 / 简介 / 人设，可选择注入到系统提示词。
 * 对应占位符 {{user_bio}} 与 {{user_persona}}。
 */
@Composable
fun SettingDisplayUserProfilePage(vm: SettingVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    var displaySetting by remember(settings) { mutableStateOf(settings.displaySetting) }

    fun updateDisplaySetting(setting: DisplaySetting) {
        displaySetting = setting
        vm.updateSettings(settings.copy(displaySetting = setting))
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("我的资料卡") },
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
                Card(modifier = Modifier.padding(horizontal = 8.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "资料卡内容会作为一段简短介绍加到系统提示词里，帮助助手记住你是谁。留空的字段不会注入。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = displaySetting.userNickname,
                            onValueChange = {
                                updateDisplaySetting(displaySetting.copy(userNickname = it))
                            },
                            label = { Text("昵称") },
                            placeholder = { Text("助手怎么称呼你") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = displaySetting.userBio,
                            onValueChange = {
                                updateDisplaySetting(displaySetting.copy(userBio = it))
                            },
                            label = { Text("简介") },
                            placeholder = { Text("职业、爱好、作息、常聊的话题…") },
                            minLines = 3,
                            maxLines = 8,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = displaySetting.userPersona,
                            onValueChange = {
                                updateDisplaySetting(displaySetting.copy(userPersona = it))
                            },
                            label = { Text("人设 / 扮演身份") },
                            placeholder = { Text("在角色扮演里你的身份、性格、说话方式") },
                            minLines = 3,
                            maxLines = 8,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
            item {
                CardGroup(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    title = { Text("注入设置") },
                ) {
                    item(
                        headlineContent = { Text("注入到系统提示词") },
                        supportingContent = { Text("关闭后资料卡只能通过占位符手动引用") },
                        trailingContent = {
                            Switch(
                                checked = displaySetting.injectUserProfile,
                                onCheckedChange = {
                                    updateDisplaySetting(displaySetting.copy(injectUserProfile = it))
                                }
                            )
                        },
                    )
                }
            }
            item {
                Card(modifier = Modifier.padding(horizontal = 8.dp)) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "可用占位符",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            text = "{{nickname}} 昵称\n{{user_bio}} 简介\n{{user_persona}} 人设",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
