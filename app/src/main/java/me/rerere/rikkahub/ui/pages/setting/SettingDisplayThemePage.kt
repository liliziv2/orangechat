package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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

@Composable
fun SettingDisplayThemePage(vm: SettingVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    var displaySetting by remember(settings) { mutableStateOf(settings.displaySetting) }
    var amoledDarkMode by rememberAmoledDarkMode()
    val navController = LocalNavController.current

    // 材质模式收敛到三种：FOLLOW_THEME 已从下拉里隐藏（枚举与旧数据都保留不动）。
    // 旧数据里存着 FOLLOW_THEME 的用户（这也是 DisplaySetting 的默认值）在渲染上
    // 本来就等同于 FLAT（见 Theme.kt：FOLLOW_THEME -> FLAT），所以这里把它归一化
    // 成 FLAT 显示 —— 否则会出现「当前值不在下拉列表里」的空档。
    val visibleMaterialMode =
        if (displaySetting.materialMode == DisplayMaterialMode.FOLLOW_THEME) {
            DisplayMaterialMode.FLAT
        } else {
            displaySetting.materialMode
        }

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
                                when (visibleMaterialMode) {
                                    DisplayMaterialMode.FOLLOW_THEME -> "跟随主题"
                                    DisplayMaterialMode.FLAT -> "平面"
                                    DisplayMaterialMode.TRANSLUCENT -> "轻透"
                                    DisplayMaterialMode.GLASS -> "玻璃"
                                }
                            )
                        },
                        trailingContent = {
                            Select(
                                options = MATERIAL_MODE_OPTIONS,
                                selectedOption = visibleMaterialMode,
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

/**
 * 「材质模式」下拉实际暴露给用户的选项。
 *
 * FOLLOW_THEME 已从下拉里隐藏 —— 它在渲染上等同于 FLAT，暴露出来只是让同一个
 * 视觉结果出现两个名字。DisplayMaterialMode 枚举本身仍保留 4 个值：旧数据里
 * 存着 follow_theme 的用户靠 ignoreUnknownKeys 与上面的归一化继续正常工作。
 */
private val MATERIAL_MODE_OPTIONS: List<DisplayMaterialMode> =
    DisplayMaterialMode.entries.filter { it != DisplayMaterialMode.FOLLOW_THEME }
