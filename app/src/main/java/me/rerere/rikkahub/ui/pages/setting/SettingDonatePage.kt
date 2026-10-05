package me.rerere.rikkahub.ui.pages.setting

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import me.rerere.rikkahub.ui.theme.LargeFlexibleTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import me.rerere.rikkahub.R
import me.rerere.rikkahub.ui.components.nav.BackButton
import me.rerere.rikkahub.ui.components.ui.CardGroup
import me.rerere.rikkahub.ui.theme.CustomColors
import me.rerere.rikkahub.utils.openUrl
import me.rerere.rikkahub.utils.plus

@Composable
fun SettingDonatePage() {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        topBar = {
            LargeFlexibleTopAppBar(
                title = {
                    Text(text = stringResource(R.string.donate_page_title))
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
            contentPadding = contentPadding + PaddingValues(vertical = 8.dp),
        ) {
            item {
                DonateMethodsCardGroup()
            }
        }
    }
}

@Composable
private fun DonateMethodsCardGroup() {
    val context = LocalContext.current
    CardGroup(
        flat = true,
        title = { Text(stringResource(R.string.donate_page_donation_methods)) },
    ) {
        item(
            onClick = { context.openUrl("https://github.com/sue1231513/orangechat") },
            leadingContent = {
                AsyncImage(
                    model = R.drawable.kofi,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                )
            },
            supportingContent = { Text(stringResource(R.string.donate_page_kofi_desc)) },
            headlineContent = { Text("GitHub") },
        )
        item(
            onClick = { context.openUrl("https://github.com/sue1231513/orangechat") },
            leadingContent = {
                Icon(
                    painter = painterResource(R.drawable.afdian),
                    contentDescription = null,
                )
            },
            supportingContent = { Text(stringResource(R.string.donate_page_afdian_desc)) },
            headlineContent = { Text("项目主页") },
        )
    }
}
