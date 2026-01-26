package id.jo.jomanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import id.jo.jomanager.R
import id.jo.jomanager.ui.MainActivity
import id.jo.jomanager.ui.components.IndeterminateExpressiveLoadingIndicator
import id.jo.jomanager.viewmodel.ProcessFilter
import id.jo.jomanager.viewmodel.ProcessInfo
import id.jo.jomanager.viewmodel.ProcessMonitorViewModel
import id.jo.jomanager.viewmodel.ProcessSort
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessMonitorScreen(
    navController: NavController,
    viewModel: ProcessMonitorViewModel = hiltViewModel()
) {
    val processes by viewModel.processes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val sampleRate by viewModel.sampleRate.collectAsState()
    val maxProcesses by viewModel.maxProcesses.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val filterOption by viewModel.filterOption.collectAsState()

    var showSettingsDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val mainActivity = remember(context) { context as? MainActivity }

    LaunchedEffect(Unit) {
        viewModel.startMonitoring()
    }

    LaunchedEffect(isLoading) {
        mainActivity?.processMonitorFabVisible?.value = !isLoading
        mainActivity?.processMonitorFabAction?.value = { showSettingsDialog = true }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.stopMonitoring()
            mainActivity?.processMonitorFabVisible?.value = false
            mainActivity?.processMonitorFabAction?.value = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (processes.isEmpty() && isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                IndeterminateExpressiveLoadingIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Header
                item {
                     Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                         Text(
                             text = stringResource(R.string.process_name),
                             style = MaterialTheme.typography.labelMedium,
                             color = MaterialTheme.colorScheme.onSurfaceVariant,
                             modifier = Modifier.weight(1f)
                         )
                         Row(modifier = Modifier.width(160.dp), horizontalArrangement = Arrangement.End) {
                             Text(
                                 text = "CPU",
                                 style = MaterialTheme.typography.labelMedium,
                                 color = MaterialTheme.colorScheme.onSurfaceVariant,
                                 modifier = Modifier.width(50.dp).padding(end = 8.dp),
                                 textAlign = androidx.compose.ui.text.style.TextAlign.End
                             )
                             Text(
                                 text = "RAM(MB)",
                                 style = MaterialTheme.typography.labelMedium,
                                 color = MaterialTheme.colorScheme.onSurfaceVariant,
                                 modifier = Modifier.width(70.dp),
                                 textAlign = androidx.compose.ui.text.style.TextAlign.End
                             )
                         }
                    }
                }
                
                items(processes, key = { it.pid }) { process ->
                    ProcessItem(process)
                }
                
                // Extra spacer for FAB
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }

    if (showSettingsDialog) {
        ProcessMonitorSettingsDialog(
            currentRate = sampleRate,
            currentMax = maxProcesses,
            currentSort = sortOption,
            currentFilter = filterOption,
            onDismiss = { showSettingsDialog = false },
            onConfirm = { rate, max, sort, filter ->
                viewModel.updateSettings(rate, max, sort, filter)
                showSettingsDialog = false
            }
        )
    }
}

@Composable
fun ProcessItem(process: ProcessInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (process.isUserApp) {
                Icon(
                    imageVector = Icons.Default.Android,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp).padding(end = 8.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = process.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${process.pid} • ${process.user}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row(modifier = Modifier.width(160.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                // CPU
                Text(
                    text = String.format(Locale.getDefault(), "%.1f%%", process.cpu),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (process.cpu > 20) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(50.dp).padding(end = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
                
                // RAM
                Text(
                    text = "${process.ram.toInt()}MB",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(70.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ProcessMonitorSettingsDialog(
    currentRate: Long,
    currentMax: Int,
    currentSort: ProcessSort,
    currentFilter: ProcessFilter,
    onDismiss: () -> Unit,
    onConfirm: (Long, Int, ProcessSort, ProcessFilter) -> Unit
) {
    var rateText by remember { mutableStateOf(currentRate.toString()) }
    var maxText by remember { mutableStateOf(currentMax.toString()) }
    var sortOption by remember { mutableStateOf(currentSort) }
    var filterOption by remember { mutableStateOf(currentFilter) }

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
                    .heightIn(min = 200.dp),
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
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.process_monitor_settings),
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = stringResource(R.string.process_monitor_settings),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Content
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = rateText,
                            onValueChange = { rateText = it.filter { char -> char.isDigit() } },
                            label = { Text(stringResource(R.string.process_monitor_sample_rate)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        OutlinedTextField(
                            value = maxText,
                            onValueChange = { maxText = it.filter { char -> char.isDigit() } },
                            label = { Text(stringResource(R.string.process_monitor_max_processes)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        
                        // Sort Options
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.sort_by), 
                                style = MaterialTheme.typography.titleSmall, 
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                            ) {
                                val sortOptions = listOf(ProcessSort.CPU, ProcessSort.RAM)
                                sortOptions.forEachIndexed { index, option ->
                                    val isSelected = sortOption == option
                                    ToggleButton(
                                        checked = isSelected,
                                        onCheckedChange = { sortOption = option },
                                        modifier = Modifier
                                            .weight(1f)
                                            .semantics { role = Role.RadioButton },
                                        shapes = when (index) {
                                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                            sortOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                        }
                                    ) {
                                        Text(text = option.name)
                                    }
                                }
                            }
                        }

                        // Filter Options
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = stringResource(R.string.filter), 
                                style = MaterialTheme.typography.titleSmall, 
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                            ) {
                                val filterOptions = listOf(ProcessFilter.ALL, ProcessFilter.USER_APPS, ProcessFilter.SYSTEM_ROOT)
                                filterOptions.forEachIndexed { index, option ->
                                    val isSelected = filterOption == option
                                    ToggleButton(
                                        checked = isSelected,
                                        onCheckedChange = { filterOption = option },
                                        modifier = Modifier
                                            .weight(1f)
                                            .semantics { role = Role.RadioButton },
                                        shapes = when (index) {
                                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                            filterOptions.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                                        }
                                    ) {
                                        Text(
                                            text = when(option) {
                                                ProcessFilter.ALL -> stringResource(R.string.filter_all)
                                                ProcessFilter.USER_APPS -> stringResource(R.string.filter_user)
                                                ProcessFilter.SYSTEM_ROOT -> stringResource(R.string.filter_system)
                                            },
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                    
                    // Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        Button(
                            onClick = {
                                val rate = rateText.toLongOrNull() ?: 2000L
                                val max = maxText.toIntOrNull() ?: 20
                                onConfirm(rate, max, sortOption, filterOption)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(stringResource(R.string.app_profiles_save))
                        }
                    }
                }
            }
        }
    }
}