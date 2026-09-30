package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.ArrowRight01
import me.rerere.rikkahub.ui.components.ui.toHexString
import me.rerere.rikkahub.ui.theme.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.data.datastore.DisplaySetting
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.components.ui.ColorPickerDialog
import me.rerere.rikkahub.ui.components.ui.toComposeColor
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingDisplayColorPage(vm: SettingVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    var displaySetting by remember(settings) { mutableStateOf(settings.displaySetting) }

    fun updateDisplaySetting(setting: DisplaySetting) {
        displaySetting = setting
        vm.updateSettings(settings.copy(displaySetting = setting))
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showChatTextColorPicker by remember { mutableStateOf(false) }
    var showGlobalTextColorPicker by remember { mutableStateOf(false) }
    var showUserBubbleColorPicker by remember { mutableStateOf(false) }
    var showAssistantBubbleColorPicker by remember { mutableStateOf(false) }
    var showThinkingBubbleColorPicker by remember { mutableStateOf(false) }
    var showChatBackgroundColorPicker by remember { mutableStateOf(false) }
    var showPrimaryColorPicker by remember { mutableStateOf(false) }
    var showInputFieldColorPicker by remember { mutableStateOf(false) }

    if (showChatTextColorPicker) {
        ColorPickerDialog(
            initialColor = displaySetting.chatTextColor,
            defaultColor = MaterialTheme.colorScheme.onSurface,
            onConfirm = { updateDisplaySetting(displaySetting.copy(chatTextColor = it)) },
            onDismiss = { showChatTextColorPicker = false }
        )
    }
    if (showGlobalTextColorPicker) {
        ColorPickerDialog(
            initialColor = displaySetting.globalTextColor,
            defaultColor = MaterialTheme.colorScheme.background,
            onConfirm = { updateDisplaySetting(displaySetting.copy(globalTextColor = it)) },
            onDismiss = { showGlobalTextColorPicker = false }
        )
    }
    if (showUserBubbleColorPicker) {
        ColorPickerDialog(
            initialColor = displaySetting.userBubbleColor,
            defaultColor = MaterialTheme.colorScheme.secondaryContainer,
            onConfirm = { updateDisplaySetting(displaySetting.copy(userBubbleColor = it)) },
            onDismiss = { showUserBubbleColorPicker = false }
        )
    }
    if (showAssistantBubbleColorPicker) {
        ColorPickerDialog(
            initialColor = displaySetting.assistantBubbleColor,
            defaultColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            onConfirm = { updateDisplaySetting(displaySetting.copy(assistantBubbleColor = it)) },
            onDismiss = { showAssistantBubbleColorPicker = false }
        )
    }
    if (showThinkingBubbleColorPicker) {
        ColorPickerDialog(
            initialColor = displaySetting.thinkingBubbleColor,
            defaultColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            onConfirm = { updateDisplaySetting(displaySetting.copy(thinkingBubbleColor = it)) },
            onDismiss = { showThinkingBubbleColorPicker = false }
        )
    }
    if (showChatBackgroundColorPicker) {
        ColorPickerDialog(
            initialColor = displaySetting.chatBackgroundColor,
            defaultColor = MaterialTheme.colorScheme.background,
            onConfirm = { updateDisplaySetting(displaySetting.copy(chatBackgroundColor = it)) },
            onDismiss = { showChatBackgroundColorPicker = false }
        )
    }
    if (showPrimaryColorPicker) {
        ColorPickerDialog(
            initialColor = displaySetting.primaryColor,
            defaultColor = MaterialTheme.colorScheme.primary,
            onConfirm = { updateDisplaySetting(displaySetting.copy(primaryColor = it)) },
            onDismiss = { showPrimaryColorPicker = false }
        )
    }
    if (showInputFieldColorPicker) {
        ColorPickerDialog(
            initialColor = displaySetting.inputFieldColor,
            defaultColor = MaterialTheme.colorScheme.surfaceContainerLowest,
            onConfirm = { updateDisplaySetting(displaySetting.copy(inputFieldColor = it)) },
            onDismiss = { showInputFieldColorPicker = false }
        )
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text("颜色自定义", style = MaterialTheme.typography.headlineMedium) },
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
            contentPadding = contentPadding + PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 文字
            item {
                CardGroup(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    title = { Text("文字") },
                ) {
                    item(
                        onClick = { showChatTextColorPicker = true },
                        leadingContent = {
                            ColorSwatch(displaySetting.chatTextColor?.toComposeColor() ?: Color.Gray)
                        },
                        trailingContent = { ColorValue(displaySetting.chatTextColor) },
                        headlineContent = { Text("聊天正文颜色") },
                    )
                    item(
                        onClick = { showGlobalTextColorPicker = true },
                        leadingContent = {
                            ColorSwatch(displaySetting.globalTextColor?.toComposeColor() ?: Color.Gray)
                        },
                        trailingContent = { ColorValue(displaySetting.globalTextColor) },
                        headlineContent = { Text("全局字体颜色") },
                    )
                }
            }

            // 气泡
            item {
                CardGroup(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    title = { Text("气泡") },
                ) {
                    item(
                        onClick = { showUserBubbleColorPicker = true },
                        leadingContent = {
                            ColorSwatch(
                                displaySetting.userBubbleColor?.toComposeColor()
                                    ?: MaterialTheme.colorScheme.secondaryContainer
                            )
                        },
                        supportingContent = { Text("自定义用户消息气泡背景色") },
                        trailingContent = { ColorValue(displaySetting.userBubbleColor) },
                        headlineContent = { Text("用户气泡颜色") },
                    )
                    item(
                        onClick = { showAssistantBubbleColorPicker = true },
                        leadingContent = {
                            ColorSwatch(
                                displaySetting.assistantBubbleColor?.toComposeColor()
                                    ?: MaterialTheme.colorScheme.surfaceContainerHigh
                            )
                        },
                        supportingContent = { Text("自定义AI消息气泡背景色") },
                        trailingContent = { ColorValue(displaySetting.assistantBubbleColor) },
                        headlineContent = { Text("AI气泡颜色") },
                    )
                    item(
                        onClick = { showThinkingBubbleColorPicker = true },
                        leadingContent = {
                            ColorSwatch(displaySetting.thinkingBubbleColor?.toComposeColor() ?: Color.Gray)
                        },
                        trailingContent = { ColorValue(displaySetting.thinkingBubbleColor) },
                        headlineContent = { Text("思维链气泡颜色") },
                    )
                }
            }

            // 界面
            item {
                CardGroup(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    title = { Text("界面") },
                ) {
                    item(
                        onClick = { showChatBackgroundColorPicker = true },
                        leadingContent = {
                            ColorSwatch(displaySetting.chatBackgroundColor?.toComposeColor() ?: Color.Gray)
                        },
                        supportingContent = { Text("有背景图时图片优先") },
                        trailingContent = { ColorValue(displaySetting.chatBackgroundColor) },
                        headlineContent = { Text("聊天背景色") },
                    )
                    item(
                        onClick = { showPrimaryColorPicker = true },
                        leadingContent = {
                            ColorSwatch(
                                displaySetting.primaryColor?.toComposeColor()
                                    ?: MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = { ColorValue(displaySetting.primaryColor) },
                        headlineContent = { Text("主色调（按钮/链接）") },
                    )
                    item(
                        onClick = { showInputFieldColorPicker = true },
                        leadingContent = {
                            ColorSwatch(
                                displaySetting.inputFieldColor?.toComposeColor()
                                    ?: MaterialTheme.colorScheme.surfaceContainerLowest
                            )
                        },
                        supportingContent = { Text("有背景图时图片优先") },
                        trailingContent = { ColorValue(displaySetting.inputFieldColor) },
                        headlineContent = { Text("输入框背景颜色") },
                    )
                }
            }
        }
    }
}

/**
 * 颜色预览色块。
 *
 * 比旧版（16dp 圆形）大一圈、改成圆角方 —— 这一页本身是「看颜色」的页面，
 * 色块应该是一行里的视觉重点，而不是缩在行末按钮旁边的小圆点。
 */
@Composable
private fun ColorSwatch(color: Color) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(color, RoundedCornerShape(8.dp)),
    )
}

/**
 * 行末的当前值：自定义过显示 hex，没自定义过显示「自定义」入口。
 *
 * hex 复用 [ColorPickerDialog] 里那套（`#RRGGBB`，带 alpha 时 `#AARRGGBB`），不另写一份。
 * 箭头只是「点进去才打开选择器」的提示 —— 整行都可点，不需要单独一个按钮。
 */
@Composable
private fun ColorValue(value: Long?) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = value?.toHexString() ?: "自定义",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            imageVector = HugeIcons.ArrowRight01,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}