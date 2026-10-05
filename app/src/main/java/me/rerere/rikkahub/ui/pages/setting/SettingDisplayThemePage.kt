package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import me.rerere.rikkahub.ui.theme.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.R
import me.rerere.rikkahub.Screen
import me.rerere.rikkahub.data.datastore.DisplaySetting
import me.rerere.rikkahub.data.datastore.DisplayMaterialMode
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.components.ui.Select
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.hooks.rememberAmoledDarkMode
import me.rerere.rikkahub.ui.pages.setting.components.PresetThemeButtonGroup
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import org.koin.androidx.compose.koinViewModel
import kotlin.math.roundToInt

@Composable
fun SettingDisplayThemePage(vm: SettingVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    var displaySetting by remember(settings) { mutableStateOf(settings.displaySetting) }
    var amoledDarkMode by rememberAmoledDarkMode()
    val navController = LocalNavController.current

    fun updateDisplaySetting(setting: DisplaySetting) {
        displaySetting = setting
        vm.updateSettings(settings.copy(displaySetting = setting))
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("主题外观", style = MaterialTheme.typography.headlineMedium) },
                navigationIcon = { BackButton() },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = settingsScaffoldContainerColor(CustomColors.topBarColors.containerColor)
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = contentPadding + PaddingValues(vertical = 8.dp),
        ) {
            item {
                // 这里原来是「手写组标题 + 8 个各自裁圆角的 ListItem + 2dp 行距」——
                // 全树唯一一处没用共享容器、而是把分组视觉手搓了一遍的页面。
                // 现在改走共享的 CardGroup（扁平密度）：组标题用分组标题几何、
                // 行间用行分隔线、行自己不再裁圆角。
                CardGroup(
                    flat = true,
                    title = { Text(stringResource(R.string.setting_page_theme_setting)) },
                ) {
                    item(
                        headlineContent = { Text(stringResource(R.string.setting_page_dynamic_color)) },
                        supportingContent = { Text(stringResource(R.string.setting_page_dynamic_color_desc)) },
                        trailingContent = {
                            Switch(
                                checked = settings.dynamicColor,
                                onCheckedChange = { vm.updateSettings(settings.copy(dynamicColor = it)) },
                            )
                        },
                    )
                    item(
                        headlineContent = { Text("材质模式") },
                        supportingContent = {
                            Text(
                                when (displaySetting.materialMode) {
                                    DisplayMaterialMode.FOLLOW_THEME -> "跟随主题"
                                    DisplayMaterialMode.FLAT -> "平面"
                                    DisplayMaterialMode.TRANSLUCENT -> "轻透"
                                    DisplayMaterialMode.GLASS -> "玻璃"
                                }
                            )
                        },
                        trailingContent = {
                            Select(
                                options = DisplayMaterialMode.entries,
                                selectedOption = displaySetting.materialMode,
                                onOptionSelected = {
                                    updateDisplaySetting(displaySetting.copy(materialMode = it))
                                },
                                optionToString = {
                                    when (it) {
                                        DisplayMaterialMode.FOLLOW_THEME -> "跟随主题"
                                        DisplayMaterialMode.FLAT -> "平面"
                                        DisplayMaterialMode.TRANSLUCENT -> "轻透"
                                        DisplayMaterialMode.GLASS -> "玻璃"
                                    }
                                },
                                modifier = Modifier.width(150.dp),
                            )
                        },
                    )
                    item(
                        headlineContent = { Text("界面实时渲染") },
                        supportingContent = { Text("在支持的设备上为玻璃界面启用实时背景渲染") },
                        trailingContent = {
                            Switch(
                                checked = displaySetting.interfaceRealtimeRendering,
                                onCheckedChange = {
                                    updateDisplaySetting(
                                        displaySetting.copy(interfaceRealtimeRendering = it)
                                    )
                                },
                            )
                        },
                    )
                    if (displaySetting.interfaceRealtimeRendering) {
                        item(
                            headlineContent = { Text("聊天气泡实时模糊") },
                            supportingContent = { Text("为普通聊天气泡实时渲染背景模糊") },
                            trailingContent = {
                                Switch(
                                    checked = displaySetting.chatBubbleRealtimeBlur,
                                    onCheckedChange = {
                                        updateDisplaySetting(
                                            displaySetting.copy(chatBubbleRealtimeBlur = it)
                                        )
                                    },
                                )
                            },
                        )
                        item(
                            headlineContent = { Text("液态玻璃气泡") },
                            supportingContent = { Text("iOS Liquid Glass 风格：实时模糊 + 边缘高光 + 顶部折射反光") },
                            trailingContent = {
                                Switch(
                                    checked = displaySetting.liquidGlassBubbles,
                                    onCheckedChange = {
                                        updateDisplaySetting(
                                            displaySetting.copy(liquidGlassBubbles = it)
                                        )
                                    },
                                )
                            },
                        )
                        item(
                            headlineContent = { Text("模糊强度") },
                            supportingContent = {
                                Column {
                                    Slider(
                                        value = displaySetting.interfaceBlurRadius.coerceIn(3f, 20f),
                                        onValueChange = {
                                            updateDisplaySetting(
                                                displaySetting.copy(interfaceBlurRadius = it)
                                            )
                                        },
                                        valueRange = 3f..20f,
                                    )
                                    Text(
                                        text = "${displaySetting.interfaceBlurRadius.coerceIn(3f, 20f).roundToInt()} dp",
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            },
                        )
                    }
                    // Custom theme management entry
                    item(
                        onClick = { navController.navigate(Screen.SettingTheme) },
                        headlineContent = { Text("自定义主题管理") },
                        supportingContent = { Text("HCT 色彩算法自定义主题") },
                    )
                }
            }

            // 预设主题选择器是这一页唯一的专用 preview surface，按 carve-out 保留：
            // surfaceBright 底色 / 4dp 圆角 / PresetThemeButtonGroup 本体一律不动，
            // 只是从「容器内」挪到页面背景上，横向加 16dp 与行对齐。
            if (!settings.dynamicColor) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceBright)
                    ) {
                        PresetThemeButtonGroup(
                            themeId = settings.themeId,
                            modifier = Modifier.fillMaxWidth(),
                            onChangeTheme = { vm.updateSettings(settings.copy(themeId = it)) }
                        )
                    }
                }
            }

            // AMOLED 原来夹在预览面下面、同属上面那一组。拆成第二个（无标题的）
            // CardGroup 是为了让预览面能留在原位 —— CardGroup 的行是先收进 scope
            // 再统一渲染的，没法在行中间插任意 composable。
            item {
                CardGroup(flat = true) {
                    item(
                        headlineContent = { Text(stringResource(R.string.setting_display_page_amoled_dark_mode_title)) },
                        supportingContent = { Text(stringResource(R.string.setting_display_page_amoled_dark_mode_desc)) },
                        trailingContent = {
                            Switch(
                                checked = amoledDarkMode,
                                onCheckedChange = { amoledDarkMode = it }
                            )
                        },
                    )
                }
            }
        }
    }
}