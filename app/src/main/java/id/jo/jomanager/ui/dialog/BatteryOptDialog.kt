package id.jo.jomanager.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import id.jo.jomanager.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryOptDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onExit: () -> Unit,
    showExitButton: Boolean = false
) {
    BasicAlertDialog(
        onDismissRequest = { /* Prevent dismissal by tapping outside */ },
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(0.9f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.BatteryAlert,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = if (showExitButton) stringResource(R.string.permissions_required) else stringResource(R.string.battery_optimization),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Content
                    Text(
                        text = if (showExitButton) {
                            stringResource(R.string.battery_opt_desc_exit)
                        } else {
                            stringResource(R.string.battery_opt_desc_later)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (showExitButton) {
                            OutlinedButton(
                                onClick = onExit,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                            ) {
                                Text(stringResource(R.string.exit_app_dialog))
                            }
                        } else {
                            OutlinedButton(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(stringResource(R.string.later))
                            }
                        }

                        Button(
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(stringResource(R.string.open_settings))
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Default State")
@Composable
private fun BatteryOptDialogDefaultPreview() {
    MaterialTheme {
        BatteryOptDialog(
            onDismiss = {},
            onConfirm = {},
            onExit = {},
            showExitButton = false
        )
    }
}

@Preview(showBackground = true, name = "Exit State")
@Composable
private fun BatteryOptDialogExitPreview() {
    MaterialTheme {
        BatteryOptDialog(
            onDismiss = {},
            onConfirm = {},
            onExit = {},
            showExitButton = true
        )
    }
}

