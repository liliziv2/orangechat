package me.rerere.rikkahub.ui.pages.setting

import me.rerere.ai.core.ReasoningLevel
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Earth
import me.rerere.hugeicons.stroke.View
import me.rerere.hugeicons.stroke.FileZip
import me.rerere.hugeicons.stroke.Mortarboard01
import me.rerere.hugeicons.stroke.Message01
import me.rerere.hugeicons.stroke.MessageMultiple01
import me.rerere.hugeicons.stroke.Notebook01
import me.rerere.hugeicons.stroke.Tools
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import me.rerere.rikkahub.ui.theme.LargeFlexibleTopAppBar
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.ai.provider.ModelType
import me.rerere.rikkahub.R
import me.rerere.rikkahub.data.ai.prompts.DEFAULT_COMPRESS_PROMPT
import me.rerere.rikkahub.data.ai.prompts.DEFAULT_OCR_PROMPT
import me.rerere.rikkahub.data.ai.prompts.DEFAULT_SUGGESTION_PROMPT
import me.rerere.rikkahub.data.ai.prompts.DEFAULT_TITLE_PROMPT
import me.rerere.rikkahub.data.ai.prompts.DEFAULT_TRANSLATION_PROMPT
import me.rerere.rikkahub.ui.components.ai.ReasoningButton
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.ui.components.ai.ModelSelector
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.FormItem
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.plus
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingModelPage(vm: SettingVM = koinViewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    // 三级设置页的标题。同一档 headlineMedium —— 从「海报标题」降下来，
                    // 保留左上大标题的位置感，但不再抢内容。
                    Text(
                        text = stringResource(R.string.setting_model_page_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                navigationIcon = {
                    BackButton()
                },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = settingsScaffoldContainerColor(CustomColors.topBarColors.containerColor),
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // 行自带左右内边距，容器只留一点上下呼吸位 —— 与提供商页一致。
            // 行与行之间不再留 12dp 空档：分界交给那根发丝线，列表才是连续的。
            contentPadding = contentPadding + PaddingValues(vertical = 8.dp),
        ) {
            item {
                DefaultChatModelSetting(settings = settings, vm = vm)
            }

            item {
                ModelRowDivider()
            }

            item {
                DefaultTitleModelSetting(settings = settings, vm = vm)
            }

            item {
                ModelRowDivider()
            }

            item {
                DefaultSuggestionModelSetting(settings = settings, vm = vm)
            }

            item {
                ModelRowDivider()
            }

            item {
                DefaultTranslationModelSetting(settings = settings, vm = vm)
            }

            item {
                ModelRowDivider()
            }

            item {
                DefaultOcrModelSetting(settings = settings, vm = vm)
            }

            item {
                ModelRowDivider()
            }

            item {
                DefaultCompressModelSetting(settings = settings, vm = vm)
            }
        }
    }
}

@Composable
private fun DefaultTranslationModelSetting(
    settings: Settings,
    vm: SettingVM
) {
    var showModal by remember { mutableStateOf(false) }
    ModelFeatureRow(
        title = {
            Text(
                stringResource(R.string.setting_model_page_translate_model),
                maxLines = 1
            )
        },
        description = {
            Text(stringResource(R.string.setting_model_page_translate_model_desc))
        },
        icon = {
            Icon(HugeIcons.Earth, null)
        },
        actions = {
            Box(modifier = Modifier.weight(1f)) {
                ModelSelector(
                    modelId = settings.translateModeId,
                    type = ModelType.CHAT,
                    onSelect = {
                        vm.updateSettings(
                            settings.copy(
                                translateModeId = it.id
                            )
                        )
                    },
                    providers = settings.providers,
                    // 图标收到 20dp、名字抬到 titleSmall：这一行的主体是「当前值」，
                    // 不再是那枚兔子/橘子图标。
                    iconSize = ModelValueIconSize,
                    nameStyle = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.wrapContentWidth()
                )
            }
            // 原来是一枚 filledTonal 的圆形按钮 —— 和模型名、✕ 排在一起时，
            // 三块形状各说各话。改成裸图标：能直接点的就直接点，不再占一块底色。
            IconButton(onClick = { showModal = true }) {
                Icon(HugeIcons.Tools, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )

    if (showModal) {
        ModalBottomSheet(
            onDismissRequest = {
                showModal = false
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FormItem(
                    label = {
                        Text(stringResource(R.string.assistant_page_thinking_budget))
                    },
                ) {
                    ReasoningButton(
                        reasoningLevel = ReasoningLevel.fromBudgetTokens(settings.translateThinkingBudget),
                        onUpdateReasoningLevel = {
                            vm.updateSettings(settings.copy(translateThinkingBudget = it.budgetTokens))
                        }
                    )
                }

                FormItem(
                    label = {
                        Text(stringResource(R.string.setting_model_page_prompt))
                    },
                    description = {
                        Text(stringResource(R.string.setting_model_page_translate_prompt_vars))
                    }
                ) {
                    OutlinedTextField(
                        value = settings.translatePrompt,
                        onValueChange = {
                            vm.updateSettings(
                                settings.copy(
                                    translatePrompt = it
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 10,
                    )
                    TextButton(
                        onClick = {
                            vm.updateSettings(
                                settings.copy(
                                    translatePrompt = DEFAULT_TRANSLATION_PROMPT
                                )
                            )
                        }
                    ) {
                        Text(stringResource(R.string.setting_model_page_reset_to_default))
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultSuggestionModelSetting(
    settings: Settings,
    vm: SettingVM
) {
    var showModal by remember { mutableStateOf(false) }
    ModelFeatureRow(
        title = {
            Text(
                text = stringResource(R.string.setting_model_page_suggestion_model),
                maxLines = 1
            )
        },
        description = {
            Text(stringResource(R.string.setting_model_page_suggestion_model_desc))
        },
        icon = {
            Icon(HugeIcons.MessageMultiple01, null)
        },
        actions = {
            Box(modifier = Modifier.weight(1f)) {
                ModelSelector(
                    modelId = settings.suggestionModelId,
                    type = ModelType.CHAT,
                    onSelect = {
                        vm.updateSettings(
                            settings.copy(
                                suggestionModelId = it.id
                            )
                        )
                    },
                    providers = settings.providers,
                    allowClear = true,
                    // 图标收到 20dp、名字抬到 titleSmall：这一行的主体是「当前值」，
                    // 不再是那枚兔子/橘子图标。
                    iconSize = ModelValueIconSize,
                    nameStyle = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.wrapContentWidth()
                )
            }
            // 原来是一枚 filledTonal 的圆形按钮 —— 和模型名、✕ 排在一起时，
            // 三块形状各说各话。改成裸图标：能直接点的就直接点，不再占一块底色。
            IconButton(onClick = { showModal = true }) {
                Icon(HugeIcons.Tools, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )

    if (showModal) {
        ModalBottomSheet(
            onDismissRequest = {
                showModal = false
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FormItem(
                    label = {
                        Text(stringResource(R.string.setting_model_page_prompt))
                    },
                    description = {
                        Text(stringResource(R.string.setting_model_page_suggestion_prompt_vars))
                    }
                ) {
                    OutlinedTextField(
                        value = settings.suggestionPrompt,
                        onValueChange = {
                            vm.updateSettings(
                                settings.copy(
                                    suggestionPrompt = it
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 8
                    )
                    TextButton(
                        onClick = {
                            vm.updateSettings(
                                settings.copy(
                                    suggestionPrompt = DEFAULT_SUGGESTION_PROMPT
                                )
                            )
                        }
                    ) {
                        Text(stringResource(R.string.setting_model_page_reset_to_default))
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultTitleModelSetting(
    settings: Settings,
    vm: SettingVM
) {
    var showModal by remember { mutableStateOf(false) }
    ModelFeatureRow(
        title = {
            Text(stringResource(R.string.setting_model_page_title_model), maxLines = 1)
        },
        description = {
            Text(stringResource(R.string.setting_model_page_title_model_desc))
        },
        icon = {
            Icon(HugeIcons.Notebook01, null)
        },
        actions = {
            Box(modifier = Modifier.weight(1f)) {
                ModelSelector(
                    modelId = settings.titleModelId,
                    type = ModelType.CHAT,
                    onSelect = {
                        vm.updateSettings(
                            settings.copy(
                                titleModelId = it.id
                            )
                        )
                    },
                    providers = settings.providers,
                    allowClear = true,
                    // 图标收到 20dp、名字抬到 titleSmall：这一行的主体是「当前值」，
                    // 不再是那枚兔子/橘子图标。
                    iconSize = ModelValueIconSize,
                    nameStyle = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.wrapContentWidth()
                )
            }
            // 原来是一枚 filledTonal 的圆形按钮 —— 和模型名、✕ 排在一起时，
            // 三块形状各说各话。改成裸图标：能直接点的就直接点，不再占一块底色。
            IconButton(onClick = { showModal = true }) {
                Icon(HugeIcons.Tools, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )

    if (showModal) {
        ModalBottomSheet(
            onDismissRequest = {
                showModal = false
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FormItem(
                    label = {
                        Text(stringResource(R.string.setting_model_page_prompt))
                    },
                    description = {
                        Text(stringResource(R.string.setting_model_page_suggestion_prompt_vars))
                    }
                ) {
                    OutlinedTextField(
                        value = settings.titlePrompt,
                        onValueChange = {
                            vm.updateSettings(
                                settings.copy(
                                    titlePrompt = it
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 8
                    )
                    TextButton(
                        onClick = {
                            vm.updateSettings(
                                settings.copy(
                                    titlePrompt = DEFAULT_TITLE_PROMPT
                                )
                            )
                        }
                    ) {
                        Text(stringResource(R.string.setting_model_page_reset_to_default))
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultChatModelSetting(
    settings: Settings,
    vm: SettingVM
) {
    ModelFeatureRow(
        icon = {
            Icon(HugeIcons.Message01, null)
        },
        title = {
            Text(stringResource(R.string.setting_model_page_chat_model), maxLines = 1)
        },
        description = {
            Text(stringResource(R.string.setting_model_page_chat_model_desc))
        },
        actions = {
            Box(modifier = Modifier.weight(1f)) {
                ModelSelector(
                    modelId = settings.chatModelId,
                    type = ModelType.CHAT,
                    onSelect = {
                        vm.updateSettings(
                            settings.copy(
                                chatModelId = it.id
                            )
                        )
                    },
                    providers = settings.providers,
                    // 图标收到 20dp、名字抬到 titleSmall：这一行的主体是「当前值」，
                    // 不再是那枚兔子/橘子图标。
                    iconSize = ModelValueIconSize,
                    nameStyle = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.wrapContentWidth()
                )
            }
        }
    )
}

@Composable
private fun DefaultOcrModelSetting(
    settings: Settings,
    vm: SettingVM
) {
    var showModal by remember { mutableStateOf(false) }
    ModelFeatureRow(
        title = {
            Text(
                stringResource(R.string.setting_model_page_ocr_model),
                maxLines = 1
            )
        },
        description = {
            Text(stringResource(R.string.setting_model_page_ocr_model_desc))
        },
        icon = {
            Icon(HugeIcons.View, null)
        },
        actions = {
            Box(modifier = Modifier.weight(1f)) {
                ModelSelector(
                    modelId = settings.ocrModelId,
                    type = ModelType.CHAT,
                    onSelect = {
                        vm.updateSettings(
                            settings.copy(
                                ocrModelId = it.id
                            )
                        )
                    },
                    providers = settings.providers,
                    // 图标收到 20dp、名字抬到 titleSmall：这一行的主体是「当前值」，
                    // 不再是那枚兔子/橘子图标。
                    iconSize = ModelValueIconSize,
                    nameStyle = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.wrapContentWidth()
                )
            }
            // 原来是一枚 filledTonal 的圆形按钮 —— 和模型名、✕ 排在一起时，
            // 三块形状各说各话。改成裸图标：能直接点的就直接点，不再占一块底色。
            IconButton(onClick = { showModal = true }) {
                Icon(HugeIcons.Tools, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )

    if (showModal) {
        ModalBottomSheet(
            onDismissRequest = {
                showModal = false
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FormItem(
                    label = {
                        Text(stringResource(R.string.setting_model_page_prompt))
                    },
                    description = {
                        Text(stringResource(R.string.setting_model_page_ocr_prompt_vars))
                    }
                ) {
                    OutlinedTextField(
                        value = settings.ocrPrompt,
                        onValueChange = {
                            vm.updateSettings(
                                settings.copy(
                                    ocrPrompt = it
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 10,
                    )
                    TextButton(
                        onClick = {
                            vm.updateSettings(
                                settings.copy(
                                    ocrPrompt = DEFAULT_OCR_PROMPT
                                )
                            )
                        }
                    ) {
                        Text(stringResource(R.string.setting_model_page_reset_to_default))
                    }
                }
            }
        }
    }
}

@Composable
private fun DefaultCompressModelSetting(
    settings: Settings,
    vm: SettingVM
) {
    var showModal by remember { mutableStateOf(false) }
    ModelFeatureRow(
        title = {
            Text(
                stringResource(R.string.setting_model_page_compress_model),
                maxLines = 1
            )
        },
        description = {
            Text(stringResource(R.string.setting_model_page_compress_model_desc))
        },
        icon = {
            Icon(HugeIcons.FileZip, null)
        },
        actions = {
            Box(modifier = Modifier.weight(1f)) {
                ModelSelector(
                    modelId = settings.compressModelId,
                    type = ModelType.CHAT,
                    onSelect = {
                        vm.updateSettings(
                            settings.copy(
                                compressModelId = it.id
                            )
                        )
                    },
                    providers = settings.providers,
                    // 图标收到 20dp、名字抬到 titleSmall：这一行的主体是「当前值」，
                    // 不再是那枚兔子/橘子图标。
                    iconSize = ModelValueIconSize,
                    nameStyle = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.wrapContentWidth()
                )
            }
            // 原来是一枚 filledTonal 的圆形按钮 —— 和模型名、✕ 排在一起时，
            // 三块形状各说各话。改成裸图标：能直接点的就直接点，不再占一块底色。
            IconButton(onClick = { showModal = true }) {
                Icon(HugeIcons.Tools, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    )

    if (showModal) {
        ModalBottomSheet(
            onDismissRequest = {
                showModal = false
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FormItem(
                    label = {
                        Text(stringResource(R.string.setting_model_page_prompt))
                    },
                    description = {
                        Text(stringResource(R.string.setting_model_page_compress_prompt_vars))
                    }
                ) {
                    OutlinedTextField(
                        value = settings.compressPrompt,
                        onValueChange = {
                            vm.updateSettings(
                                settings.copy(
                                    compressPrompt = it
                                )
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 10,
                    )
                    TextButton(
                        onClick = {
                            vm.updateSettings(
                                settings.copy(
                                    compressPrompt = DEFAULT_COMPRESS_PROMPT
                                )
                            )
                        }
                    ) {
                        Text(stringResource(R.string.setting_model_page_reset_to_default))
                    }
                }
            }
        }
    }
}

@Composable
private fun ModelFeatureRow(
    modifier: Modifier = Modifier,
    description: @Composable () -> Unit = {},
    icon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit
) {
    // 一行，不是一张卡。
    //
    // 原来是每个角色一张 OutlinedCard：描边 + 16dp 内边距 + 两块之间再插 12dp 空档，
    // 六个角色就是六张厚卡叠下来，整页读起来像「设置后台」。现在卡片整个撤掉，
    // 一行一个角色，靠行距和一根发丝线分界 —— 与模型页、提供商页同一套语言。
    //
    // 层级也换了主次：角色名（聊天模型 / 标题总结模型 …）退成安静的标签，
    // 「现在用的是哪个模型」成为这一行的主体 —— 那才是来这一页要找的答案。
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 14.dp, end = 4.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 功能图标收到 20dp：它是这一行的分类记号，不该和模型名抢注意力。
        Box(
            modifier = Modifier.size(ModelFeatureIconSize),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }

        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                ProvideTextStyle(MaterialTheme.typography.titleSmall) {
                    title()
                }
                ProvideTextStyle(
                    MaterialTheme.typography.labelSmall.copy(
                        color = LocalContentColor.current.copy(alpha = 0.62f)
                    )
                ) {
                    description()
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    actions()
                }
            }
        }
    }
}

/** 角色行之间的分割线。发丝级 —— 只把行读成一个列表，不构成分界。 */
@Composable
private fun ModelRowDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = ModelRowDividerAlpha),
    )
}

/** 角色行之间分割线的透明度。与提供商页同一档。 */
private const val ModelRowDividerAlpha = 0.5f

/** 功能图标尺寸。它是这一行的分类记号，不是主体。 */
private val ModelFeatureIconSize = 20.dp

/** 当前值里那枚模型图标的尺寸。收到 20dp，让模型名读起来是主体。 */
private val ModelValueIconSize = 20.dp
