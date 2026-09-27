package me.rerere.rikkahub.vocabulary.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.pages.setting.settingsScaffoldContainerColor
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.ui.theme.LargeFlexibleTopAppBar
import me.rerere.rikkahub.utils.plus
import me.rerere.rikkahub.vocabulary.algorithm.StudyConstants
import me.rerere.rikkahub.vocabulary.study.SessionState
import me.rerere.rikkahub.vocabulary.study.StudyMode
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * 学习页。
 *
 * 三种形态按顺序出现：续学询问 → 卡片 → 成绩单。
 * `review` 只影响「这一组从哪个队列取卡」，续学的话还是接着上次那个模式。
 */
@Composable
fun VocabularyStudyPage(vocabId: Long, review: Boolean) {
    val vm: VocabularyStudyVM = koinViewModel(parameters = { parametersOf(vocabId) })
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }

    val book by vm.book.collectAsStateWithLifecycle()
    val session by vm.session.collectAsStateWithLifecycle()
    val resumable by vm.resumable.collectAsStateWithLifecycle()
    val summary by vm.summary.collectAsStateWithLifecycle()
    val notice by vm.notice.collectAsStateWithLifecycle()
    val ready by vm.ready.collectAsStateWithLifecycle()

    // enter 自己幂等（ready 之后直接返回），所以转屏重建也不会把这一组重开
    LaunchedEffect(vocabId) { vm.enter(review) }

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

    val pageTitle = stringResource(R.string.vocabulary_page_title)

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                navigationIcon = { BackButton() },
                title = { Text(book?.name ?: pageTitle) },
                subtitle = {
                    val progress = session?.let { "${it.answeredCount} / ${it.total}" }
                    Text(
                        text = progress ?: pageTitle,
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = CustomColors.topBarColors,
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = settingsScaffoldContainerColor(CustomColors.topBarColors.containerColor),
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            val currentSummary = summary
            val currentSession = session
            when {
                currentSummary != null -> SummaryCard(
                    summary = currentSummary,
                    onAgain = {
                        vm.dismissSummary()
                        vm.start(if (review) StudyMode.REVIEW else StudyMode.LEARN)
                    },
                    onDone = { vm.dismissSummary() },
                )

                currentSession != null -> StudyBody(session = currentSession, vm = vm)

                ready -> Text(
                    text = stringResource(R.string.vocabulary_no_words_in_group),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    // 有没学完的会话就先问一句，不直接把人扔回学习页
    resumable?.let { state ->
        AlertDialog(
            // 必须选一个，点外面不算
            onDismissRequest = { },
            title = { Text(stringResource(R.string.vocabulary_resume_title)) },
            text = {
                Text(
                    stringResource(
                        R.string.vocabulary_resume_text,
                        state.answeredCount,
                        state.total,
                    )
                )
            },
            confirmButton = {
                TextButton(onClick = { vm.continueSession() }) {
                    Text(stringResource(R.string.vocabulary_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = { vm.restartSession() }) {
                    Text(stringResource(R.string.vocabulary_restart))
                }
            },
        )
    }
}

@Composable
private fun StudyBody(session: SessionState, vm: VocabularyStudyVM) {
    val card = session.current ?: return
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Card(shape = MaterialTheme.shapes.large) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = card.card.word,
                    style = MaterialTheme.typography.headlineMedium,
                )
                if (card.card.ipa.isNotBlank()) {
                    Text(
                        text = card.card.ipa,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (session.revealed) {
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = card.card.translation,
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    if (card.card.pos.isNotBlank()) {
                        Text(
                            text = card.card.pos,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (card.card.example.isNotBlank()) {
                        Text(
                            text = card.card.example,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }

        if (session.revealed) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { vm.answer(StudyConstants.ANSWER_FORGOT) },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.vocabulary_answer_forgot)) }
                OutlinedButton(
                    onClick = { vm.answer(StudyConstants.ANSWER_FUZZY) },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.vocabulary_answer_fuzzy)) }
                Button(
                    onClick = { vm.answer(StudyConstants.ANSWER_KNOW) },
                    modifier = Modifier.weight(1f),
                ) { Text(stringResource(R.string.vocabulary_answer_know)) }
            }
        } else {
            Button(
                onClick = { vm.reveal() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.vocabulary_reveal)) }
        }

        // 「我本来就会」是次要动作，放主按钮下方做成灰字；也不需要先翻开答案
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TextButton(onClick = { vm.markMastered() }) {
                Text(
                    text = stringResource(R.string.vocabulary_already_know),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(
    summary: StudySummary,
    onAgain: () -> Unit,
    onDone: () -> Unit,
) {
    Card(shape = MaterialTheme.shapes.large) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.vocabulary_summary_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.vocabulary_summary_times, summary.times),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.size(4.dp))
            Text(
                text = stringResource(R.string.vocabulary_summary_completed, summary.completed),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (summary.firstTry > 0) {
                Text(
                    text = stringResource(R.string.vocabulary_summary_first_try, summary.firstTry),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (summary.repeatCount > 0) {
                Text(
                    text = stringResource(R.string.vocabulary_summary_repeat, summary.repeatCount),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (summary.parked > 0) {
                Text(
                    text = stringResource(R.string.vocabulary_summary_parked, summary.parked),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.size(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onAgain, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.vocabulary_again))
                }
                TextButton(onClick = onDone) {
                    Text(stringResource(R.string.vocabulary_done))
                }
            }
        }
    }
}
