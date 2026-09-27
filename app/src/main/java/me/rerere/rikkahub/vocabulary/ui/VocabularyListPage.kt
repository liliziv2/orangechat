package me.rerere.rikkahub.vocabulary.ui

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import me.rerere.hugeicons.HugeIcons
import me.rerere.hugeicons.stroke.Book01
import me.rerere.hugeicons.stroke.BookOpen01
import me.rerere.hugeicons.stroke.Clock02
import me.rerere.hugeicons.stroke.DatabaseRestore
import me.rerere.hugeicons.stroke.Delete01
import me.rerere.hugeicons.stroke.FileImport
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.pages.setting.settingsScaffoldContainerColor
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.ui.theme.LargeFlexibleTopAppBar
import me.rerere.rikkahub.utils.plus
import me.rerere.rikkahub.vocabulary.dao.BookStatsRow
import me.rerere.rikkahub.vocabulary.entity.VocabularyEntity
import me.rerere.rikkahub.vocabulary.study.StudyMode
import org.koin.androidx.compose.koinViewModel

/** 词库列表页 */
@Composable
fun VocabularyListPage(
    onOpenBook: (Long, StudyMode) -> Unit,
    vm: VocabularyListVM = koinViewModel(),
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    val books by vm.books.collectAsStateWithLifecycle()
    val deletedBooks by vm.deletedBooks.collectAsStateWithLifecycle()
    val stats by vm.stats.collectAsStateWithLifecycle()
    val todayCount by vm.todayCount.collectAsStateWithLifecycle()
    val notice by vm.notice.collectAsStateWithLifecycle()
    val importing by vm.importing.collectAsStateWithLifecycle()
    val pendingUri by vm.pendingUri.collectAsStateWithLifecycle()
    val pendingName by vm.pendingName.collectAsStateWithLifecycle()

    var bookToDelete by remember { mutableStateOf<VocabularyEntity?>(null) }
    var showDeleted by remember { mutableStateOf(false) }

    // 提前取出来：下面几个回调（对话框按钮、文件选择器）都不是 @Composable，
    // 在里面调 stringResource 编不过。
    val defaultBookName = stringResource(R.string.vocabulary_default_book_name)

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        // mime 一律传通配（下面那行 arrayOf 里的字面量）：各家文件管理器给 .csv 报的
        // 类型很杂 —— text/comma-separated-values、application/vnd.ms-excel、
        // octet-stream……限死 text/csv 会让用户看得见文件却点不动。
        // 真正的二进制文件在解析阶段挡。
        if (uri != null) vm.onFilePicked(uri, displayNameOf(context, uri).orEmpty())
    }

    // 「待复习」跟「现在几点」有关，Room 不会因为时间流逝重发 Flow，只能轮询重算。
    // 放在 LaunchedEffect 里而不是 VM 的 init：跟着 composition 走，离开页面就停。
    LaunchedEffect(Unit) {
        while (true) {
            vm.refresh()
            delay(15_000)
        }
    }

    // ⚠️ 提示文案必须在**组合里**先翻好：LaunchedEffect 的块是普通挂起 lambda，
    // 里面调 stringResource 编不过（它不是 @Composable 上下文）。
    //
    // 先落到一个普通局部变量再判空：`by` 委托属性在 Kotlin 里**不能智能转换**，
    // 直接 `notice?.text()` 会踩到条件化 @Composable 调用的边界。
    val noticeNow = notice
    val noticeText: String? = if (noticeNow == null) null else noticeNow.text()

    LaunchedEffect(noticeNow) {
        if (noticeText != null) {
            // 先清状态再显示：否则连着两次同样的失败（notice 值相等）不会重新触发
            vm.consumeNotice()
            snackbarHostState.showSnackbar(noticeText)
        }
    }

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                navigationIcon = { BackButton() },
                title = { Text(stringResource(R.string.vocabulary_page_title)) },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { picker.launch(arrayOf("*/*")) }) {
                Icon(
                    imageVector = HugeIcons.FileImport,
                    contentDescription = stringResource(R.string.vocabulary_import_icon_desc),
                )
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = settingsScaffoldContainerColor(CustomColors.topBarColors.containerColor),
    ) { innerPadding ->
        LazyColumn(
            contentPadding = innerPadding + PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Text(
                    text = stringResource(R.string.vocabulary_today_studied, todayCount),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (books.isEmpty()) {
                item { EmptyHint() }
            }

            items(books, key = { it.id }) { book ->
                BookCard(
                    book = book,
                    stats = stats[book.id],
                    onLearn = { onOpenBook(book.id, StudyMode.LEARN) },
                    onReview = { onOpenBook(book.id, StudyMode.REVIEW) },
                    onDelete = { bookToDelete = book },
                )
            }

            if (deletedBooks.isNotEmpty()) {
                item {
                    TextButton(onClick = { showDeleted = !showDeleted }) {
                        Text(stringResource(R.string.vocabulary_deleted_books, deletedBooks.size))
                    }
                }
                if (showDeleted) {
                    items(deletedBooks, key = { it.id }) { book ->
                        DeletedRow(book = book, onRestore = { vm.restoreBook(book.id) })
                    }
                }
            }
        }
    }

    if (pendingUri != null) {
        AlertDialog(
            onDismissRequest = { vm.cancelImport() },
            title = { Text(stringResource(R.string.vocabulary_import_title)) },
            text = {
                OutlinedTextField(
                    value = pendingName,
                    onValueChange = { vm.updatePendingName(it) },
                    label = { Text(stringResource(R.string.vocabulary_book_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { vm.confirmImport(defaultBookName) },
                    enabled = !importing,
                ) {
                    Text(
                        if (importing) stringResource(R.string.vocabulary_importing)
                        else stringResource(R.string.vocabulary_import)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.cancelImport() }) {
                    Text(stringResource(R.string.vocabulary_cancel))
                }
            },
        )
    }

    bookToDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text(stringResource(R.string.vocabulary_delete_title, book.name)) },
            confirmButton = {
                TextButton(onClick = {
                    vm.deleteBook(book.id)
                    bookToDelete = null
                }) { Text(stringResource(R.string.vocabulary_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) {
                    Text(stringResource(R.string.vocabulary_cancel))
                }
            },
        )
    }
}

@Composable
private fun EmptyHint() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = HugeIcons.Book01,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.vocabulary_empty_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(
                text = stringResource(R.string.vocabulary_empty_hint),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BookCard(
    book: VocabularyEntity,
    stats: BookStatsRow?,
    onLearn: () -> Unit,
    onReview: () -> Unit,
    onDelete: () -> Unit,
) {
    val due = stats?.due ?: 0
    Card(shape = MaterialTheme.shapes.large) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = book.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    // 简介现在是空的（导入时不写库，见 VocabularyRepository.importCsv），
                    // 空就不渲染，免得留一行空白。
                    if (book.description.isNotBlank()) {
                        Text(
                            text = book.description,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = HugeIcons.Delete01,
                        contentDescription = stringResource(R.string.vocabulary_delete),
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (stats != null) {
                Text(
                    text = stringResource(
                        R.string.vocabulary_stats_line,
                        stats.newCount,
                        stats.studying,
                        stats.due,
                        stats.mastered,
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = onLearn, modifier = Modifier.weight(1f)) {
                    Icon(HugeIcons.BookOpen01, null, Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.vocabulary_learn_new))
                }
                if (due > 0) {
                    FilledTonalButton(onClick = onReview, modifier = Modifier.weight(1f)) {
                        Icon(HugeIcons.Clock02, null, Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(stringResource(R.string.vocabulary_review_count, due))
                    }
                }
            }
        }
    }
}

@Composable
private fun DeletedRow(book: VocabularyEntity, onRestore: () -> Unit) {
    Card(shape = MaterialTheme.shapes.large) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = book.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onRestore) {
                Icon(HugeIcons.DatabaseRestore, null, Modifier.size(16.dp))
                Spacer(Modifier.size(6.dp))
                Text(stringResource(R.string.vocabulary_restore))
            }
        }
    }
}

/** 取文件在系统里的显示名，用来给词库预填一个名字。取不到就算了 */
private fun displayNameOf(context: android.content.Context, uri: Uri): String? = runCatching {
    context.contentResolver
        .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
}.getOrNull()
