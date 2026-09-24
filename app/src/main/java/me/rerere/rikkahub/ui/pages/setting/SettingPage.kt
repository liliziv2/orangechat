package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import me.rerere.rikkahub.ui.theme.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.AiMagic
import me.rerere.hugeicons.stroke.Alert01
import me.rerere.hugeicons.stroke.Database02
import me.rerere.hugeicons.stroke.LookTop
import me.rerere.hugeicons.stroke.MessageMultiple01
import me.rerere.hugeicons.stroke.Package
import me.rerere.hugeicons.stroke.Shield02
import me.rerere.hugeicons.stroke.Sun01
import me.rerere.rikkahub.R
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.data.datastore.isNotConfigured
import me.rerere.rikkahub.data.files.FilesManager
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.components.ui.CollapsibleCardGroup
import me.rerere.rikkahub.ui.components.ui.Select
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.context.Navigator
import me.rerere.rikkahub.ui.hooks.rememberColorMode
import me.rerere.rikkahub.ui.theme.ColorMode
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun SettingPage(vm: SettingVM = koinViewModel()) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val navController = LocalNavController.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    val filesManager: FilesManager = koinInject()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(text = stringResource(R.string.settings))
                },
                navigationIcon = {
                    BackButton()
                },
                scrollBehavior = scrollBehavior,
                actions = {
                },
                colors = CustomColors.topBarColors
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = settingsScaffoldContainerColor(CustomColors.topBarColors.containerColor)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = innerPadding + PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (settings.isNotConfigured()) {
                item {
                    ProviderConfigWarningCard(navController)
                }
            }

            // 一级设置：外观 / 聊天 / 模型与服务 / 扩展 / 安全 / 助手 / 通用
            //
            // 旧结构按内部模块平铺（通用设置、显示设置、插件管理、扩展并列），
            // 用户得先知道「显示设置」这种内部叫法才能找到主题。现在按「我要改什么」
            // 分七组，简单开关/下拉尽量留在本页，只有确实含一组子设置的才进下一页。

            item("appearance") {
                var colorMode by rememberColorMode()
                val selectedColorModeText = when (colorMode) {
                    ColorMode.SYSTEM -> stringResource(R.string.setting_page_color_mode_system)
                    ColorMode.LIGHT -> stringResource(R.string.setting_page_color_mode_light)
                    ColorMode.DARK -> stringResource(R.string.setting_page_color_mode_dark)
                }
                CardGroup(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(HugeIcons.Sun01, null, modifier = Modifier.size(18.dp))
                            Text("外观")
                        }
                    },
                ) {
                    // 高频简单选项直接放首页，用下拉，不再进二级页面。
                    item(
                        trailingContent = {
                            Select(
                                options = ColorMode.entries,
                                selectedOption = colorMode,
                                onOptionSelected = {
                                    colorMode = it
                                    navController.navigate(Screen.Setting) {
                                        popUpTo(Screen.Setting) {
                                            inclusive = true
                                        }
                                    }
                                },
                                optionToString = {
                                    when (it) {
                                        ColorMode.SYSTEM -> stringResource(R.string.setting_page_color_mode_system)
                                        ColorMode.LIGHT -> stringResource(R.string.setting_page_color_mode_light)
                                        ColorMode.DARK -> stringResource(R.string.setting_page_color_mode_dark)
                                    }
                                },
                                modifier = Modifier.width(150.dp)
                            )
                        },
                        headlineContent = { Text(stringResource(R.string.setting_page_color_mode)) },
                        supportingContent = { Text(selectedColorModeText) },
                    )
                    // 原「显示设置」这一层删除，其下条目直接归入「外观」。
                    item(
                        onClick = { navController.navigate(Screen.SettingDisplayTheme) },
                        headlineContent = { Text("主题外观") },
                        supportingContent = { Text("动态色、预设主题、AMOLED 暗黑模式") },
                    )
                    item(
                        onClick = { navController.navigate(Screen.SettingDisplayColor) },
                        headlineContent = { Text("颜色自定义") },
                        supportingContent = { Text("气泡、背景、主色调等颜色") },
                    )
                    item(
                        onClick = { navController.navigate(Screen.SettingDisplayTransparency) },
                        headlineContent = { Text("透明度设置") },
                        supportingContent = { Text("气泡、思维链、侧边栏透明度") },
                    )
                    item(
                        onClick = { navController.navigate(Screen.SettingDisplayIllustration) },
                        headlineContent = { Text("插图素材") },
                        supportingContent = { Text("输入框背景、侧边栏背景、头像挂件") },
                    )
                    item(
                        onClick = { navController.navigate(Screen.SettingDisplayPreset) },
                        headlineContent = { Text("外观预设") },
                        supportingContent = { Text("保存 4 套外观方案，一键切换") },
                    )
                    item(
                        onClick = { navController.navigate(Screen.SettingDisplayUserProfile) },
                        headlineContent = { Text("我的资料卡") },
                        supportingContent = { Text("昵称、简介、人设，可注入到系统提示词") },
                    )
                }
            }

            item("chat") {
                CardGroup(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(HugeIcons.MessageMultiple01, null, modifier = Modifier.size(18.dp))
                            Text("聊天")
                        }
                    },
                ) {
                    item(
                        onClick = { navController.navigate(Screen.SettingDisplayMessage) },
                        headlineContent = { Text("消息显示") },
                        supportingContent = { Text("头像、气泡、字体大小、自定义字体") },
                    )
                    item(
                        onClick = { navController.navigate(Screen.SettingDisplayCodeInteraction) },
                        headlineContent = { Text("代码与交互") },
                        supportingContent = { Text("代码块、回车发送、滚动、音量键等") },
                    )
                    item(
                        onClick = { navController.navigate(Screen.SettingDisplayNotification) },
                        headlineContent = { Text("通知与TTS") },
                        supportingContent = { Text("消息生成通知、TTS 自动朗读") },
                    )
                    item(
                        onClick = { navController.navigate(Screen.SettingDisplayGeneral) },
                        headlineContent = { Text("通用设置") },
                        supportingContent = { Text("启动时新建对话、更新提醒、发送音效") },
                    )
                }
            }

            item("modelServices") {
                CardGroup(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(HugeIcons.AiMagic, null, modifier = Modifier.size(18.dp))
                            Text(stringResource(R.string.setting_page_model_and_services))
                        }
                    },
                ) {
                    item(
                        onClick = { navController.navigate(Screen.SettingModels) },
                        supportingContent = { Text(stringResource(R.string.setting_page_default_model_desc)) },
                        headlineContent = { Text(stringResource(R.string.setting_page_default_model)) },
                    )
                    item(
                        onClick = { navController.navigate(Screen.SettingProvider) },
                        supportingContent = { Text(stringResource(R.string.setting_page_providers_desc)) },
                        headlineContent = { Text(stringResource(R.string.setting_page_providers)) },
                    )
                    item(
                        onClick = { navController.navigate(Screen.SettingSearch) },
                        supportingContent = { Text(stringResource(R.string.setting_page_search_service_desc)) },
                        headlineContent = { Text(stringResource(R.string.setting_page_search_service)) },
                    )
                    item(
                        onClick = { navController.navigate(Screen.SettingSpeech) },
                        supportingContent = { Text(stringResource(R.string.setting_page_tts_service_desc)) },
                        headlineContent = { Text(stringResource(R.string.setting_page_tts_service)) },
                    )
                }
            }

            item("extension") {
                CardGroup(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(HugeIcons.Package, null, modifier = Modifier.size(18.dp))
                            Text("扩展")
                        }
                    },
                ) {
                    // 插件与技能/扩展各自是独立页面,不再并列成两个一级入口,
                    // 也不再为合并而在中间加一层「扩展总页面」。
                    // 插件管理直达插件页;技能、快捷消息、提示词等确实是一组子设置,
                    // 才进入下一页,不在此处再塞图标。
                    item(
                        onClick = { navController.navigate(Screen.SettingPlugins) },
                        supportingContent = { Text("管理本地插件,导入 ZIP 插件包") },
                        headlineContent = { Text("插件管理") },
                    )
                    item(
                        onClick = { navController.navigate(Screen.Extensions) },
                        supportingContent = { Text("技能、快捷消息、提示词、进阶记忆、工作区") },
                        headlineContent = { Text("技能与扩展") },
                    )
                }
            }

            item("security") {
                CardGroup(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(HugeIcons.Shield02, null, modifier = Modifier.size(18.dp))
                            Text("安全")
                        }
                    },
                ) {
                    item(
                        onClick = { navController.navigate(Screen.SettingSecurity) },
                        supportingContent = { Text("工具调用确认、自动批准、工作流拦截等安全选项") },
                        headlineContent = { Text("安全设置") },
                    )
                    item(
                        onClick = { navController.navigate(Screen.SecurityAudit) },
                        supportingContent = { Text("查看插件安装、工作流拦截、敏感操作等安全事件记录") },
                        headlineContent = { Text("安全审计日志") },
                    )
                }
            }

            item("assistant") {
                CardGroup(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(HugeIcons.LookTop, null, modifier = Modifier.size(18.dp))
                            Text("助手")
                        }
                    },
                ) {
                    item(
                        onClick = { navController.navigate(Screen.Assistant) },
                        supportingContent = { Text(stringResource(R.string.setting_page_assistant_desc)) },
                        headlineContent = { Text(stringResource(R.string.setting_page_assistant)) },
                    )
                }
            }

            item("general") {
                val storageState by produceState(-1 to 0L) {
                    value = filesManager.countChatFiles()
                }
                CollapsibleCardGroup(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(HugeIcons.Database02, null, modifier = Modifier.size(18.dp))
                            Text("通用")
                        }
                    },
                    summary = { Text("数据・备份") },
                ) {
                    item(
                        onClick = { navController.navigate(Screen.Backup) },
                        supportingContent = { Text(stringResource(R.string.setting_page_data_backup_desc)) },
                        headlineContent = { Text(stringResource(R.string.setting_page_data_backup)) },
                    )
                    item(
                        onClick = { navController.navigate(Screen.SettingFiles) },
                        supportingContent = {
                            if (storageState.first == -1) {
                                Text(stringResource(R.string.calculating))
                            } else {
                                Text(
                                    stringResource(
                                        R.string.setting_page_chat_storage_desc,
                                        storageState.first,
                                        storageState.second / 1024 / 1024.0
                                    )
                                )
                            }
                        },
                        headlineContent = { Text(stringResource(R.string.setting_page_chat_storage)) },
                    )
                    // 接入与自动化条目多且低频,归入「通用」。
                    item(
                        onClick = { navController.navigate(Screen.SettingMcp) },
                        supportingContent = { Text(stringResource(R.string.setting_page_mcp_desc)) },
                        headlineContent = { Text(stringResource(R.string.setting_page_mcp)) },
                    )
                    item(
                        onClick = { navController.navigate(Screen.SettingWeb) },
                        supportingContent = { Text(stringResource(R.string.setting_page_web_server_desc)) },
                        headlineContent = { Text(stringResource(R.string.setting_page_web_server)) },
                    )
                    item(
                        onClick = { navController.navigate(Screen.SettingSystemTools) },
                        supportingContent = { Text("位置、通知、日历、闹钟等系统工具") },
                        headlineContent = { Text("系统工具") },
                    )
                    item(
                        onClick = { navController.navigate(Screen.SettingProactiveMessage) },
                        supportingContent = { Text("AI 在设定间隔内主动发消息,有记忆有上下文") },
                        headlineContent = { Text("主动消息") },
                    )
                    item(
                        onClick = { navController.navigate(Screen.SettingWeixinBot) },
                        supportingContent = { Text("把微信号变成 AI 入口,扫码登录后用微信收发消息") },
                        headlineContent = { Text("微信 Bot") },
                    )
                    item(
                        onClick = { navController.navigate(Screen.SettingQqBot) },
                        supportingContent = { Text("填 AppID/Secret,用 QQ 私聊跟 AI 对话") },
                        headlineContent = { Text("QQ Bot") },
                    )
                    item(
                        onClick = { navController.navigate(Screen.Workflows) },
                        supportingContent = { Text("Tasker 风格自动化:触发器 + 条件 -> 执行动作,由 AI 编写") },
                        headlineContent = { Text("工作流") },
                    )
                }
            }
        }
    }
}

@Composable
private fun ProviderConfigWarningCard(navController: Navigator) {
    Card(
        modifier = Modifier.padding(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.End
        ) {
            ListItem(
                headlineContent = {
                    Text(stringResource(R.string.setting_page_config_api_title))
                },
                supportingContent = {
                    Text(stringResource(R.string.setting_page_config_api_desc))
                },
                leadingContent = {
                    Icon(HugeIcons.Alert01, null)
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent
                )
            )

            TextButton(
                onClick = {
                    navController.navigate(Screen.SettingProvider)
                }
            ) {
                Text(stringResource(R.string.setting_page_config))
            }
        }
    }
}
