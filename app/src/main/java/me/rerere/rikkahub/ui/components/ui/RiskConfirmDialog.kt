package me.rerere.rikkahub.ui.components.ui

import me.rerere.rikkahub.ui.theme.popupContainerColor

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.rerere.rikkahub.R

@Composable
fun RiskConfirmDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = stringResource(R.string.risk_confirm_understood),
    dismissText: String = stringResource(R.string.risk_confirm_cancel),
) {
    AlertDialog(
        containerColor = popupContainerColor(
            androidx.compose.material3.MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        }
    )
}
