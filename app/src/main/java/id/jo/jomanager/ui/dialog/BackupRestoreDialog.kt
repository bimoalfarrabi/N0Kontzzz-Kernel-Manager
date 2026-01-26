package id.jo.jomanager.ui.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.window.DialogProperties
import id.jo.jomanager.R

import id.jo.jomanager.data.model.BackupPreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreDialog(
    onDismiss: () -> Unit,
    onBackup: (Boolean, Boolean, Boolean, Boolean) -> Unit,
    onSelectFile: () -> Unit,
    onRestore: (Boolean, Boolean, Boolean, Boolean) -> Unit,
    preview: BackupPreview? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 for Backup, 1 for Restore
    
    // Checkbox states
    var includeTuning by remember { mutableStateOf(true) }
    var includeNetwork by remember { mutableStateOf(true) }
    var includeBattery by remember { mutableStateOf(true) }
    var includeOther by remember { mutableStateOf(true) }

    // Sync checkboxes with preview when it arrives
    LaunchedEffect(preview) {
        if (preview != null) {
            selectedTab = 1
            includeTuning = preview.hasTuning
            includeNetwork = preview.hasNetwork
            includeBattery = preview.hasBattery
            includeOther = preview.hasOther
        }
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .heightIn(min = 300.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.backup_restore_title),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    PrimaryTabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = {
                            TabRowDefaults.PrimaryIndicator(
                                modifier = Modifier.tabIndicatorOffset(selectedTab),
                                width = Dp.Unspecified
                            )
                        }
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text(stringResource(R.string.backup)) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text(stringResource(R.string.restore)) }
                        )
                    }

                    if (selectedTab == 1 && preview == null) {
                        // Restore initial state: Select File button
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.backup_restore_select_file_msg),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = onSelectFile,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.backup_restore_select_file_btn))
                            }
                        }
                    } else {
                        // List of items (Backup or Restore-after-selection)
                        Text(
                            text = if (selectedTab == 0) stringResource(R.string.select_items_to_backup) 
                                   else stringResource(R.string.select_items_to_restore),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (selectedTab == 0 || preview?.hasTuning == true) {
                                BackupCheckboxItem(
                                    label = stringResource(R.string.category_tuning),
                                    description = stringResource(R.string.desc_tuning),
                                    checked = includeTuning,
                                    onCheckedChange = { includeTuning = it },
                                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                                )
                            }
                            if (selectedTab == 0 || preview?.hasNetwork == true) {
                                BackupCheckboxItem(
                                    label = stringResource(R.string.category_network_storage),
                                    description = stringResource(R.string.desc_network_storage),
                                    checked = includeNetwork,
                                    onCheckedChange = { includeNetwork = it },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                            if (selectedTab == 0 || preview?.hasBattery == true) {
                                BackupCheckboxItem(
                                    label = stringResource(R.string.category_battery),
                                    description = stringResource(R.string.desc_battery),
                                    checked = includeBattery,
                                    onCheckedChange = { includeBattery = it },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                            if (selectedTab == 0 || preview?.hasOther == true) {
                                BackupCheckboxItem(
                                    label = stringResource(R.string.category_other),
                                    description = stringResource(R.string.desc_other),
                                    checked = includeOther,
                                    onCheckedChange = { includeOther = it },
                                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        
                        val isRestoreReady = selectedTab == 1 && preview != null && (includeTuning || includeNetwork || includeBattery || includeOther)
                        val isBackupReady = selectedTab == 0 && (includeTuning || includeNetwork || includeBattery || includeOther)

                        Button(
                            onClick = {
                                if (selectedTab == 0) {
                                    onBackup(includeTuning, includeNetwork, includeBattery, includeOther)
                                } else {
                                    onRestore(includeTuning, includeNetwork, includeBattery, includeOther)
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            enabled = if (selectedTab == 0) isBackupReady else isRestoreReady
                        ) {
                            Text(if (selectedTab == 0) stringResource(R.string.backup) else stringResource(R.string.restore))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BackupCheckboxItem(
    label: String,
    description: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    shape: RoundedCornerShape
) {
    Card(
        onClick = { onCheckedChange(!checked) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = shape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (description != null) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
