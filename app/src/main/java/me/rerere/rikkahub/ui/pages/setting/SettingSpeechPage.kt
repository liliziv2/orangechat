package me.rerere.rikkahub.ui.pages.setting

import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Tick01
import me.rerere.hugeicons.stroke.StopCircle
import me.rerere.hugeicons.stroke.DragDropHorizontal
import me.rerere.hugeicons.stroke.PencilEdit01
import me.rerere.hugeicons.stroke.Add01
import me.rerere.hugeicons.stroke.Mic01
import me.rerere.hugeicons.stroke.Delete01
import me.rerere.hugeicons.stroke.VolumeHigh
import me.rerere.hugeicons.stroke.MusicNote03
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import me.rerere.rikkahub.ui.components.ui.ItemAction
import me.rerere.rikkahub.ui.components.ui.ItemActionMenu
import me.rerere.rikkahub.ui.components.ui.RikkaConfirmDialog
import me.rerere.rikkahub.ui.theme.materialModeBorderStroke
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import me.rerere.rikkahub.ui.theme.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.R
import me.rerere.asr.ASRProviderSetting
import me.rerere.rikkahub.data.datastore.DEFAULT_SYSTEM_TTS_ID
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.AutoAIIcon
import me.rerere.rikkahub.ui.context.LocalTTSState
import me.rerere.rikkahub.ui.pages.setting.components.ASRProviderConfigure
import me.rerere.rikkahub.ui.pages.setting.components.TTSProviderConfigure
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import me.rerere.tts.provider.TTSProviderSetting
import org.koin.androidx.compose.koinViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun SettingSpeechPage(vm: SettingVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    var editingTTSProvider by remember { mutableStateOf<TTSProviderSetting?>(null) }
    var editingASRProvider by remember { mutableStateOf<ASRProviderSetting?>(null) }
    var selectedPage by remember { mutableIntStateOf(0) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    // 二级设置页的标题。与提供商页、模型设置页同一档。
                    Text(
                        text = stringResource(R.string.speech_page_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                navigationIcon = {
                    BackButton()
                },
                actions = {
                    if (selectedPage == 0) {
                        AddTTSProviderButton {
                            vm.updateSettings(
                                settings.copy(
                                    ttsProviders = listOf(it) + settings.ttsProviders
                                )
                            )
                        }
                    } else if (selectedPage == 1) {
                        AddASRProviderButton {
                            vm.updateSettings(
                                settings.copy(
                                    asrProviders = listOf(it) + settings.asrProviders,
                                    selectedASRProviderId = settings.selectedASRProviderId ?: it.id
                                )
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors
            )
        },
        bottomBar = {
            // 底部不再是 NavigationBar（80dp 的双图标导航）。
            //
            // 这两个选项不是「导航到两个页面」，而是同一页里的一个二选一开关 ——
            // 用导航栏承载它，等于把一个小开关做成整条底栏。换成轻量分段切换：
            // 一个 40dp 高的控件，位置、作用、动作全不变，视觉重量掉一大截。
            Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    SegmentedButton(
                        selected = selectedPage == 0,
                        onClick = { selectedPage = 0 },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                        icon = {
                            Icon(
                                imageVector = HugeIcons.VolumeHigh,
                                contentDescription = null,
                                modifier = Modifier.size(SpeechTabIconSize)
                            )
                        },
                    ) {
                        Text(stringResource(R.string.speech_tab_tts))
                    }
                    SegmentedButton(
                        selected = selectedPage == 1,
                        onClick = { selectedPage = 1 },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                        icon = {
                            Icon(
                                imageVector = HugeIcons.Mic01,
                                contentDescription = null,
                                modifier = Modifier.size(SpeechTabIconSize)
                            )
                        },
                    ) {
                        Text(stringResource(R.string.speech_tab_asr))
                    }
                }
            }
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = settingsScaffoldContainerColor(CustomColors.topBarColors.containerColor),
    ) { innerPadding ->
        when (selectedPage) {
            0 -> TTSProviderList(
                settings = settings,
                onUpdateSettings = vm::updateSettings,
                onEdit = { editingTTSProvider = it },
                modifier = Modifier.padding(innerPadding)
            )

            1 -> ASRProviderList(
                settings = settings,
                onUpdateSettings = vm::updateSettings,
                onEdit = { editingASRProvider = it },
                modifier = Modifier.padding(innerPadding)
            )

        }
    }

    // Edit TTS Provider Bottom Sheet
    editingTTSProvider?.let { provider ->
        val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var currentProvider by remember(provider) { mutableStateOf(provider) }

        ModalBottomSheet(
            onDismissRequest = {
                editingTTSProvider = null
            },
            sheetState = bottomSheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle()
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .fillMaxHeight(0.8f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.setting_tts_page_edit_provider),
                    style = MaterialTheme.typography.headlineSmall
                )

                TTSProviderConfigure(
                    setting = currentProvider,
                    onValueChange = { newState ->
                        currentProvider = newState
                    },
                    modifier = Modifier.weight(1f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            editingTTSProvider = null
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }

                    TextButton(
                        onClick = {
                            val newProviders = settings.ttsProviders.map {
                                if (it.id == provider.id) currentProvider else it
                            }
                            vm.updateSettings(settings.copy(ttsProviders = newProviders))
                            editingTTSProvider = null
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.chat_page_save))
                    }
                }
            }
        }
    }

    editingASRProvider?.let { provider ->
        val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var currentProvider by remember(provider) { mutableStateOf(provider) }

        ModalBottomSheet(
            onDismissRequest = {
                editingASRProvider = null
            },
            sheetState = bottomSheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle()
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .fillMaxHeight(0.8f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.setting_asr_page_edit_provider),
                    style = MaterialTheme.typography.headlineSmall
                )

                ASRProviderConfigure(
                    setting = currentProvider,
                    onValueChange = { newState ->
                        currentProvider = newState
                    },
                    modifier = Modifier.weight(1f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            editingASRProvider = null
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }

                    TextButton(
                        onClick = {
                            val newProviders = settings.asrProviders.map {
                                if (it.id == provider.id) currentProvider else it
                            }
                            vm.updateSettings(settings.copy(asrProviders = newProviders))
                            editingASRProvider = null
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.chat_page_save))
                    }
                }
            }
        }
    }
}

@Composable
private fun TTSProviderList(
    settings: Settings,
    onUpdateSettings: (Settings) -> Unit,
    onEdit: (TTSProviderSetting) -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val newProviders = settings.ttsProviders.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        onUpdateSettings(settings.copy(ttsProviders = newProviders))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        // 行自带左右内边距，容器只留一点上下呼吸位；行距交给发丝线。
        contentPadding = PaddingValues(vertical = 8.dp),
        state = lazyListState
    ) {
        itemsIndexed(settings.ttsProviders, key = { _, it -> it.id }) { index, provider ->
            ReorderableItem(
                state = reorderableState,
                key = provider.id
            ) { isDragging ->
                TTSProviderItem(
                    modifier = Modifier
                        .scale(if (isDragging) 0.95f else 1f)
                        .fillMaxWidth()
                        .animateItem(),
                    provider = provider,
                    showDivider = index > 0,
                    dragHandle = {
                        val haptic = LocalHapticFeedback.current
                        IconButton(
                            onClick = {},
                            modifier = Modifier
                                .longPressDraggableHandle(
                                    onDragStarted = {
                                        haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                    },
                                    onDragStopped = {
                                        haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                    }
                                )
                        ) {
                            Icon(
                                imageVector = HugeIcons.DragDropHorizontal,
                                contentDescription = null
                            )
                        }
                    },
                    isSelected = settings.selectedTTSProviderId == provider.id,
                    onSelect = {
                        onUpdateSettings(settings.copy(selectedTTSProviderId = provider.id))
                    },
                    onEdit = {
                        onEdit(provider)
                    },
                    onDelete = {
                        val newProviders = settings.ttsProviders - provider
                        val newSelectedId =
                            if (settings.selectedTTSProviderId == provider.id) DEFAULT_SYSTEM_TTS_ID else settings.selectedTTSProviderId
                        onUpdateSettings(
                            settings.copy(
                                ttsProviders = newProviders,
                                selectedTTSProviderId = newSelectedId
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ASRProviderList(
    settings: Settings,
    onUpdateSettings: (Settings) -> Unit,
    onEdit: (ASRProviderSetting) -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val newProviders = settings.asrProviders.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        onUpdateSettings(settings.copy(asrProviders = newProviders))
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .imePadding(),
        // 与 TTS 列表同一套：单列、发丝线分界、无卡面。
        contentPadding = PaddingValues(vertical = 8.dp),
        state = lazyListState
    ) {
        itemsIndexed(settings.asrProviders, key = { _, it -> it.id }) { index, provider ->
            ReorderableItem(
                state = reorderableState,
                key = provider.id
            ) { isDragging ->
                ASRProviderItem(
                    modifier = Modifier
                        .scale(if (isDragging) 0.95f else 1f)
                        .fillMaxWidth()
                        .animateItem(),
                    provider = provider,
                    showDivider = index > 0,
                    dragHandle = {
                        val haptic = LocalHapticFeedback.current
                        IconButton(
                            onClick = {},
                            modifier = Modifier
                                .longPressDraggableHandle(
                                    onDragStarted = {
                                        haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                                    },
                                    onDragStopped = {
                                        haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                    }
                                )
                        ) {
                            Icon(
                                imageVector = HugeIcons.DragDropHorizontal,
                                contentDescription = null
                            )
                        }
                    },
                    isSelected = settings.selectedASRProviderId == provider.id,
                    onSelect = {
                        onUpdateSettings(settings.copy(selectedASRProviderId = provider.id))
                    },
                    onEdit = {
                        onEdit(provider)
                    },
                    onDelete = {
                        val newProviders = settings.asrProviders - provider
                        val newSelectedId =
                            if (settings.selectedASRProviderId == provider.id) {
                                newProviders.firstOrNull()?.id
                            } else {
                                settings.selectedASRProviderId
                            }
                        onUpdateSettings(
                            settings.copy(
                                asrProviders = newProviders,
                                selectedASRProviderId = newSelectedId
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun AddTTSProviderButton(onAdd: (TTSProviderSetting) -> Unit) {
    var showBottomSheet by remember { mutableStateOf(false) }
    var currentProvider: TTSProviderSetting by remember { mutableStateOf(TTSProviderSetting.SystemTTS()) }

    IconButton(
        onClick = {
            currentProvider = TTSProviderSetting.SystemTTS()
            showBottomSheet = true
        }
    ) {
        Icon(HugeIcons.Add01, stringResource(R.string.setting_tts_page_add_provider_content_description))
    }

    if (showBottomSheet) {
        val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
            },
            sheetState = bottomSheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle()
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .fillMaxHeight(0.8f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.setting_tts_page_add_provider),
                    style = MaterialTheme.typography.headlineSmall
                )

                TTSProviderConfigure(
                    setting = currentProvider,
                    onValueChange = { newState ->
                        currentProvider = newState
                    },
                    modifier = Modifier.weight(1f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            showBottomSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }

                    TextButton(
                        onClick = {
                            onAdd(currentProvider)
                            showBottomSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.setting_tts_page_add))
                    }
                }
            }
        }
    }
}

@Composable
private fun AddASRProviderButton(onAdd: (ASRProviderSetting) -> Unit) {
    var showBottomSheet by remember { mutableStateOf(false) }
    var showTypeMenu by remember { mutableStateOf(false) }
    var currentProvider: ASRProviderSetting by remember { mutableStateOf(ASRProviderSetting.OpenAIRealtime()) }

    Box {
        IconButton(
            onClick = { showTypeMenu = true }
        ) {
            Icon(HugeIcons.Add01, stringResource(R.string.setting_asr_page_add_provider))
        }
        DropdownMenu(
            expanded = showTypeMenu,
            onDismissRequest = { showTypeMenu = false },
            border = materialModeBorderStroke(),
        ) {
            DropdownMenuItem(
                text = { Text("OpenAI Realtime") },
                onClick = {
                    currentProvider = ASRProviderSetting.OpenAIRealtime()
                    showTypeMenu = false
                    showBottomSheet = true
                }
            )
            DropdownMenuItem(
                text = { Text("SiliconFlow") },
                onClick = {
                    currentProvider = ASRProviderSetting.SiliconFlow()
                    showTypeMenu = false
                    showBottomSheet = true
                }
            )
            DropdownMenuItem(
                text = { Text("Volcengine") },
                onClick = {
                    currentProvider = ASRProviderSetting.Volcengine()
                    showTypeMenu = false
                    showBottomSheet = true
                }
            )
            DropdownMenuItem(
                text = { Text("MiMo") },
                onClick = {
                    currentProvider = ASRProviderSetting.MiMo()
                    showTypeMenu = false
                    showBottomSheet = true
                }
            )
        }
    }

    if (showBottomSheet) {
        val bottomSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = {
                showBottomSheet = false
            },
            sheetState = bottomSheetState,
            dragHandle = {
                BottomSheetDefaults.DragHandle()
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .fillMaxHeight(0.8f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.setting_asr_page_add_provider),
                    style = MaterialTheme.typography.headlineSmall
                )

                ASRProviderConfigure(
                    setting = currentProvider,
                    onValueChange = { newState ->
                        currentProvider = newState
                    },
                    modifier = Modifier.weight(1f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            showBottomSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.cancel))
                    }

                    TextButton(
                        onClick = {
                            onAdd(currentProvider)
                            showBottomSheet = false
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(stringResource(R.string.setting_tts_page_add))
                    }
                }
            }
        }
    }
}

@Composable
private fun TTSProviderItem(
    provider: TTSProviderSetting,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
    isSelected: Boolean = false,
    dragHandle: @Composable () -> Unit,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val tts = LocalTTSState.current
    val isSpeaking by tts.isSpeaking.collectAsState()
    val isAvailable by tts.isAvailable.collectAsState()

    // 一行，不是一张卡。
    //
    // 原来是 Card：选中项把整块底色刷成 primaryContainer，再配一个 RadioButton
    // 和一整枚「已选择」标签 —— 同一件事说了三遍，还把整页染成一大块主题色。
    // 现在与提供商页同一套语言：一行两三层信息，选中只由一枚主题色小标记交代。
    // 点整行即选中（原来 RadioButton 的行为），选择/编辑/删除/拖拽/试听全部保留。
    Column(modifier = modifier.fillMaxWidth()) {
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = SpeechRowDividerAlpha),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onSelect)
                    .padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AutoAIIcon(
                    name = provider.name.ifEmpty { stringResource(R.string.setting_tts_page_default_name) },
                    modifier = Modifier.size(SpeechProviderIconSize)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = provider.name.ifEmpty { stringResource(R.string.setting_tts_page_default_name) },
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = when (provider) {
                            is TTSProviderSetting.OpenAI -> stringResource(R.string.setting_tts_page_provider_openai)
                            is TTSProviderSetting.Gemini -> stringResource(R.string.setting_tts_page_provider_gemini)
                            is TTSProviderSetting.MiniMax -> "MiniMax"
                            is TTSProviderSetting.SystemTTS -> stringResource(R.string.setting_tts_page_provider_system)
                            is TTSProviderSetting.Qwen -> "Qwen"
                            is TTSProviderSetting.Groq -> "Groq"
                            is TTSProviderSetting.XAI -> "xAI"
                            is TTSProviderSetting.MiMo -> "MiMo"
                            is TTSProviderSetting.ElevenLabs -> "ElevenLabs"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 选中态只由这一枚主题色小标记交代：不再整块刷底色，也不再堆标签。
                if (isSelected) {
                    Icon(
                        imageVector = HugeIcons.Tick01,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(SpeechSelectedMarkSize)
                    )
                }
            }

            // 试听（仅选中且可用时出现）。和拖拽、⋯ 一样：能直接点的就放裸图标，
            // 不再各自占一块圆形底色。
            if (isSelected && isAvailable) {
                val testText = stringResource(R.string.setting_tts_page_test_text)
                IconButton(
                    onClick = {
                        if (!isSpeaking) {
                            tts.speak(testText)
                        } else {
                            tts.stop()
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (isSpeaking) HugeIcons.StopCircle else HugeIcons.VolumeHigh,
                        contentDescription = if (isSpeaking) stringResource(R.string.stop) else stringResource(R.string.test_tts),
                        tint = if (isSpeaking) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }

            dragHandle()

            ItemActionMenu(
                actions = listOf(
                    ItemAction(
                        text = stringResource(R.string.edit),
                        icon = HugeIcons.PencilEdit01,
                        onClick = onEdit,
                    ),
                    ItemAction(
                        text = stringResource(R.string.delete),
                        icon = HugeIcons.Delete01,
                        destructive = true,
                        enabled = provider.id != DEFAULT_SYSTEM_TTS_ID,
                        onClick = { showDeleteDialog = true },
                    ),
                ),
            )
        }
    }

    RikkaConfirmDialog(
        show = showDeleteDialog,
        title = stringResource(R.string.confirm_delete),
        confirmText = stringResource(R.string.delete),
        dismissText = stringResource(R.string.cancel),
        onConfirm = {
            showDeleteDialog = false
            onDelete()
        },
        onDismiss = { showDeleteDialog = false },
    ) {
        Text(provider.name.ifEmpty { stringResource(R.string.setting_tts_page_default_name) })
    }
}

/** 行间分割线透明度。与提供商页、模型设置页同一档。 */
private const val SpeechRowDividerAlpha = 0.5f

/** 提供商图标尺寸。原来 32dp 偏抢，收到 26dp —— 名字才是这一行的主体。 */
private val SpeechProviderIconSize = 26.dp

/** 选中标记尺寸。选中态只有这一枚主题色小勾。 */
private val SpeechSelectedMarkSize = 18.dp

/** 底部分段切换里的图标尺寸。 */
private val SpeechTabIconSize = 18.dp

@Composable
private fun ASRProviderItem(
    provider: ASRProviderSetting,
    modifier: Modifier = Modifier,
    showDivider: Boolean = false,
    isSelected: Boolean = false,
    dragHandle: @Composable () -> Unit,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }

    // 与 TTSProviderItem 同一套语言：一行、无卡面、选中只用一枚主题色小标记。
    Column(modifier = modifier.fillMaxWidth()) {
        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = SpeechRowDividerAlpha),
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onSelect)
                    .padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AutoAIIcon(
                    name = provider.name.ifEmpty { stringResource(R.string.setting_asr_page_default_name) },
                    modifier = Modifier.size(SpeechProviderIconSize)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = provider.name.ifEmpty { stringResource(R.string.setting_asr_page_default_name) },
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = when (provider) {
                            is ASRProviderSetting.OpenAIRealtime -> "OpenAI Realtime"
                            is ASRProviderSetting.SiliconFlow -> "SiliconFlow"
                            is ASRProviderSetting.Volcengine -> "Volcengine"
                            is ASRProviderSetting.MiMo -> "MiMo"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isSelected) {
                    Icon(
                        imageVector = HugeIcons.Tick01,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(SpeechSelectedMarkSize)
                    )
                }
            }

            dragHandle()

            ItemActionMenu(
                actions = listOf(
                    ItemAction(
                        text = stringResource(R.string.edit),
                        icon = HugeIcons.PencilEdit01,
                        onClick = onEdit,
                    ),
                    ItemAction(
                        text = stringResource(R.string.delete),
                        icon = HugeIcons.Delete01,
                        destructive = true,
                        onClick = { showDeleteDialog = true },
                    ),
                ),
            )
        }
    }

    RikkaConfirmDialog(
        show = showDeleteDialog,
        title = stringResource(R.string.confirm_delete),
        confirmText = stringResource(R.string.delete),
        dismissText = stringResource(R.string.cancel),
        onConfirm = {
            showDeleteDialog = false
            onDelete()
        },
        onDismiss = { showDeleteDialog = false },
    ) {
        Text(provider.name.ifEmpty { stringResource(R.string.setting_asr_page_default_name) })
    }
}