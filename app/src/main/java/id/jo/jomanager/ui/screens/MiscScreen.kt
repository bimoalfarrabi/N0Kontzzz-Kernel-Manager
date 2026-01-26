package id.jo.jomanager.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.PowerManager
import android.provider.Settings
import android.content.Intent
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import id.jo.jomanager.ui.dialog.TcpCongestionDialog
import id.jo.jomanager.viewmodel.MiscViewModel
import id.jo.jomanager.ui.dialog.IoSchedulerDialog
import id.jo.jomanager.ui.dialog.BatteryHistoryConfigDialog
import id.jo.jomanager.ui.dialog.ChargingControlDialog

import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import id.jo.jomanager.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.core.net.toUri

@SuppressLint("BatteryLife")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiscScreen(
    navController: NavController? = null,
    viewModel: MiscViewModel = hiltViewModel(),
) {
    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.loadInitialData()
        lazyListState.scrollToItem(0)
    }

    val kgslSkipZeroingEnabled by viewModel.kgslSkipZeroingEnabled.collectAsStateWithLifecycle()
    val isKgslFeatureAvailable by viewModel.isKgslFeatureAvailable.collectAsStateWithLifecycle()
    val avoidDirtyPteEnabled by viewModel.avoidDirtyPteEnabled.collectAsStateWithLifecycle()
    val isAvoidDirtyPteAvailable by viewModel.isAvoidDirtyPteAvailable.collectAsStateWithLifecycle()
    val bypassChargingEnabled by viewModel.bypassChargingEnabled.collectAsStateWithLifecycle()
    val isBypassChargingAvailable by viewModel.isBypassChargingAvailable.collectAsStateWithLifecycle()
    val forceFastChargeEnabled by viewModel.forceFastChargeEnabled.collectAsStateWithLifecycle()
    val isForceFastChargeAvailable by viewModel.isForceFastChargeAvailable.collectAsStateWithLifecycle()
    val batteryMonitorEnabled by viewModel.batteryMonitorEnabled.collectAsStateWithLifecycle()
    val tcpCongestionAlgorithm by viewModel.tcpCongestionAlgorithm.collectAsStateWithLifecycle()
    val availableTcpAlgorithms by viewModel.availableTcpCongestionAlgorithms.collectAsStateWithLifecycle()
    val ioScheduler by viewModel.ioScheduler.collectAsStateWithLifecycle()
    val availableIoSchedulers by viewModel.availableIoSchedulers.collectAsStateWithLifecycle()
    val applyNetworkStorageOnBoot by viewModel.applyNetworkStorageOnBoot.collectAsStateWithLifecycle()

    val autoResetOnReboot by viewModel.autoResetOnReboot.collectAsStateWithLifecycle()
    val autoResetOnCharging by viewModel.autoResetOnCharging.collectAsStateWithLifecycle()
    val autoResetAtLevel by viewModel.autoResetAtLevel.collectAsStateWithLifecycle()
    val autoResetTargetLevel by viewModel.autoResetTargetLevel.collectAsStateWithLifecycle()

    val monitorAutoResetOnReboot by viewModel.monitorAutoResetOnReboot.collectAsStateWithLifecycle()
    val monitorAutoResetOnCharging by viewModel.monitorAutoResetOnCharging.collectAsStateWithLifecycle()
    val monitorAutoResetAtLevel by viewModel.monitorAutoResetAtLevel.collectAsStateWithLifecycle()
    val monitorAutoResetTargetLevel by viewModel.monitorAutoResetTargetLevel.collectAsStateWithLifecycle()

    val chargingControlEnabled by viewModel.chargingControlEnabled.collectAsStateWithLifecycle()
    val chargingControlStopLevel by viewModel.chargingControlStopLevel.collectAsStateWithLifecycle()
    val chargingControlResumeLevel by viewModel.chargingControlResumeLevel.collectAsStateWithLifecycle()
    val batteryInfo by viewModel.batteryInfo.collectAsStateWithLifecycle()

    var showAutoResetDialog by remember { mutableStateOf(false) }
    var showMonitorAutoResetDialog by remember { mutableStateOf(false) }
    var showChargingControlDialog by remember { mutableStateOf(false) }

    if (showAutoResetDialog) {
        BatteryHistoryConfigDialog(
            onDismiss = { showAutoResetDialog = false },
            resetOnReboot = autoResetOnReboot,
            onResetOnRebootChange = viewModel::setAutoResetOnReboot,
            resetOnCharging = autoResetOnCharging,
            onResetOnChargingChange = viewModel::setAutoResetOnCharging,
            resetAtLevel = autoResetAtLevel,
            onResetAtLevelChange = viewModel::setAutoResetAtLevel,
            targetLevel = autoResetTargetLevel,
            onTargetLevelChange = viewModel::setAutoResetTargetLevel
        )
    }

    if (showMonitorAutoResetDialog) {
        BatteryHistoryConfigDialog(
            onDismiss = { showMonitorAutoResetDialog = false },
            title = stringResource(R.string.battery_monitor_config_title),
            description = stringResource(R.string.battery_monitor_config_desc),
            resetOnReboot = monitorAutoResetOnReboot,
            onResetOnRebootChange = viewModel::setMonitorAutoResetOnReboot,
            resetOnCharging = monitorAutoResetOnCharging,
            onResetOnChargingChange = viewModel::setMonitorAutoResetOnCharging,
            resetAtLevel = monitorAutoResetAtLevel,
            onResetAtLevelChange = viewModel::setMonitorAutoResetAtLevel,
            targetLevel = monitorAutoResetTargetLevel,
            onTargetLevelChange = viewModel::setMonitorAutoResetTargetLevel
        )
    }

    if (showChargingControlDialog) {
        ChargingControlDialog(
            onDismiss = { showChargingControlDialog = false },
            enabled = chargingControlEnabled,
            onEnabledChange = viewModel::setChargingControlEnabled,
            stopLevel = chargingControlStopLevel,
            onStopLevelChange = viewModel::setChargingControlStopLevel,
            resumeLevel = chargingControlResumeLevel,
            onResumeLevelChange = viewModel::setChargingControlResumeLevel,
            batteryInfo = batteryInfo,
            isBypassActive = bypassChargingEnabled
        )
    }

    val context = LocalContext.current
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.toggleBatteryMonitor(true)
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 0.dp,
            bottom = 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Group 1: GPU & Power
        item {
            Text(stringResource(id = R.string.gpu_power), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
        }
        // KGSL Skip Pool Zeroing feature
        item {
            KgslSkipZeroingCard(
                kgslSkipZeroingEnabled = kgslSkipZeroingEnabled,
                isKgslFeatureAvailable = isKgslFeatureAvailable,
                onToggleKgslSkipZeroing = { enabled ->
                    viewModel.toggleKgslSkipZeroing(enabled)
                }
            )
        }

        // Avoid Dirty PTE feature
        item {
            AvoidDirtyPteCard(
                avoidDirtyPteEnabled = avoidDirtyPteEnabled,
                isAvoidDirtyPteAvailable = isAvoidDirtyPteAvailable,
                onToggleAvoidDirtyPte = { enabled ->
                    viewModel.toggleAvoidDirtyPte(enabled)
                }
            )
        }

        // Bypass Charging feature
        item {
            BypassChargingCard(
                bypassChargingEnabled = bypassChargingEnabled,
                isBypassChargingAvailable = isBypassChargingAvailable,
                isChargingControlEnabled = chargingControlEnabled,
                onToggleBypassCharging = { enabled ->
                    viewModel.toggleBypassCharging(enabled)
                }
            )
        }

        // Force Fast Charge feature
        item {
            ForceFastChargeCard(
                forceFastChargeEnabled = forceFastChargeEnabled,
                isForceFastChargeAvailable = isForceFastChargeAvailable,
                onToggleForceFastCharge = { enabled ->
                    viewModel.toggleForceFastCharge(enabled)
                }
            )
        }

        // Charging Control feature
        item {
            ChargingControlCard(
                enabled = chargingControlEnabled,
                isBatteryMonitorEnabled = batteryMonitorEnabled,
                isBypassChargingEnabled = bypassChargingEnabled,
                onClick = { showChargingControlDialog = true }
            )
        }

        // Spacer between groups
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Group: Battery
        item {
            Text(stringResource(id = R.string.battery_settings), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Battery Monitor toggle
        item {
            BatteryMonitorCard(
                enabled = batteryMonitorEnabled,
                onToggle = { enabled ->
                    if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!granted) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            // Prompt ignore battery optimizations jika belum di-whitelist
                            val pm = context.getSystemService(PowerManager::class.java)
                            if (pm != null && !pm.isIgnoringBatteryOptimizations(context.packageName)) {
                                try {
                                    val i = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = ("package:" + context.packageName).toUri()
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(i)
                                } catch (_: Exception) { }
                            }
                            viewModel.toggleBatteryMonitor(true)
                        }
                    } else {
                        viewModel.toggleBatteryMonitor(enabled)
                    }
                }
            )
        }

        // Battery Monitor Auto Reset Config
        item {
            BatteryMonitorAutoResetCard(
                onClick = { showMonitorAutoResetDialog = true }
            )
        }

        // Battery Monitor manual reset
        item {
            BatteryMonitorResetCard(
                onReset = {
                    viewModel.resetBatteryMonitor()
                },
                onEnsurePermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.POST_NOTIFICATIONS
                        ) == PackageManager.PERMISSION_GRANTED
                        if (!granted) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                }
            )
        }

        // Battery History
        item {
            BatteryHistoryCard(
                onClick = { navController?.navigate("battery_history") },
                onSettingsClick = { showAutoResetDialog = true }
            )
        }

        // Spacer between groups
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Group 3: Automation
        item {
            Text(stringResource(id = R.string.automation_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // App Profiles
        item {
            AppProfilesCard(
                onClick = { navController?.navigate("app_profiles") }
            )
        }

        // Spacer between groups
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Group: System Monitor
        item {
            Text(stringResource(id = R.string.system_stats_title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // Process Monitor
        item {
            ProcessMonitorCard(
                onClick = { navController?.navigate("process_monitor") }
            )
        }

        // Dexopt
        item {
            DexoptCard(
                onClick = { navController?.navigate("dexopt") }
            )
        }
        
        // Kernel Log
        item {
            KernelLogCard(
                onClick = { navController?.navigate("kernel_log") }
            )
        }
        
        // Spacer between groups
        item {
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Group 2: Network & Storage
        item {
            Text(stringResource(id = R.string.network_storage), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        // TCP Congestion Control Algorithm feature
        item {
            TcpCongestionControlCard(
                tcpCongestionAlgorithm = tcpCongestionAlgorithm,
                availableAlgorithms = availableTcpAlgorithms,
                onAlgorithmChange = { algorithm ->
                    viewModel.updateTcpCongestionAlgorithm(algorithm)
                }
            )
        }
        
        // I/O Scheduler feature
        item {
            IoSchedulerCard(
                ioScheduler = ioScheduler,
                availableSchedulers = availableIoSchedulers,
                onSchedulerChange = { scheduler ->
                    viewModel.updateIoScheduler(scheduler)
                }
            )
        }
        
        // Apply Network & Storage on Boot
        item {
            NetworkStorageOnBootCard(
                applyOnBoot = applyNetworkStorageOnBoot,
                onToggle = { enabled ->
                    viewModel.setApplyNetworkStorageOnBoot(enabled)
                }
            )
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkStorageOnBootCard(
    applyOnBoot: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = {
            onToggle(!applyOnBoot)
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon with themed background
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = if (applyOnBoot) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = null,
                        tint = if (applyOnBoot) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.apply_on_boot_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(id = R.string.apply_on_boot_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = applyOnBoot,
                    onCheckedChange = null,
                    thumbContent = if (applyOnBoot) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    } else {
                        {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    }
                )
            }
            
            if (applyOnBoot) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(id = R.string.apply_on_boot_active),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.apply_on_boot_active_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KgslSkipZeroingCard(
    kgslSkipZeroingEnabled: Boolean,
    isKgslFeatureAvailable: Boolean?,
    onToggleKgslSkipZeroing: (Boolean) -> Unit,
) {
    // Treat null as false for UI purposes, preventing flicker during initial load
    val featureAvailable = isKgslFeatureAvailable == true

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = {
            if (featureAvailable) {
                onToggleKgslSkipZeroing(!kgslSkipZeroingEnabled)
            }
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon with themed background
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = if (featureAvailable) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.VideogameAsset,
                        contentDescription = null,
                        tint = if (featureAvailable) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.kgsl_skip_pool_zeroing),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (featureAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.5f
                        )
                    )
                    Text(
                        text = if (featureAvailable) {
                            stringResource(id = R.string.kgsl_skip_pool_zeroing_desc)
                        } else {
                            stringResource(id = R.string.feature_not_available)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (featureAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.5f
                        )
                    )
                }

                Switch(
                    checked = kgslSkipZeroingEnabled,
                    onCheckedChange = null,
                    enabled = featureAvailable,
                    thumbContent = if (kgslSkipZeroingEnabled) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    } else {
                        {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    }
                )
            }

            if (kgslSkipZeroingEnabled && featureAvailable) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(id = R.string.performance_mode_active),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.kgsl_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AvoidDirtyPteCard(
    avoidDirtyPteEnabled: Boolean,
    isAvoidDirtyPteAvailable: Boolean?,
    onToggleAvoidDirtyPte: (Boolean) -> Unit,
) {
    val featureAvailable = isAvoidDirtyPteAvailable == true

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = {
            if (featureAvailable) {
                onToggleAvoidDirtyPte(!avoidDirtyPteEnabled)
            }
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon with themed background
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = if (featureAvailable) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = null,
                        tint = if (featureAvailable) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.avoid_dirty_pte),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (featureAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.5f
                        )
                    )
                    Text(
                        text = if (featureAvailable) {
                            stringResource(id = R.string.avoid_dirty_pte_desc)
                        } else {
                            stringResource(id = R.string.feature_not_available)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (featureAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.5f
                        )
                    )
                }

                Switch(
                    checked = avoidDirtyPteEnabled,
                    onCheckedChange = null,
                    enabled = featureAvailable,
                    thumbContent = if (avoidDirtyPteEnabled) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    } else {
                        {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    }
                )
            }

            if (avoidDirtyPteEnabled && featureAvailable) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(id = R.string.avoid_dirty_pte_activated),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.avoid_dirty_pte_active_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppProfilesCard(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon with themed background
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AppSettingsAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.app_profiles_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(id = R.string.app_profiles_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open App Profiles",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryMonitorResetCard(
    onReset: () -> Unit,
    onEnsurePermission: () -> Unit,
) {
    val context = LocalContext.current
    val toastMessage = stringResource(R.string.battery_stats_reset_toast)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = {
            onReset()
            Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon with themed background
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.battery_monitor_reset_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(id = R.string.battery_monitor_reset_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Reset",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryMonitorAutoResetCard(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon with themed background
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.battery_monitor_config_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(id = R.string.battery_monitor_config_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Configure",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryMonitorCard(
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp, 24.dp, 8.dp, 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = { onToggle(!enabled) }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon with themed background
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MonitorHeart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.battery_monitor),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(id = R.string.battery_monitor_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = null,
                    thumbContent = if (enabled) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    } else {
                        {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TcpCongestionControlCard(
    tcpCongestionAlgorithm: String?,
    availableAlgorithms: List<String>,
    onAlgorithmChange: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = { 
            if (availableAlgorithms.isNotEmpty()) {
                showDialog = true 
            }
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon with themed background
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Router,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.tcp_congestion_control),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = tcpCongestionAlgorithm ?: "...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(id = R.string.change_algorithm),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    if (showDialog) {
        TcpCongestionDialog(
            currentAlgorithm = tcpCongestionAlgorithm ?: "",
            availableAlgorithms = availableAlgorithms,
            onAlgorithmSelected = { algorithm ->
                onAlgorithmChange(algorithm)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
fun IoSchedulerCard(
    ioScheduler: String?,
    availableSchedulers: List<String>,
    onSchedulerChange: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = { 
            if (availableSchedulers.isNotEmpty()) {
                showDialog = true 
            }
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon with themed background
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.io_scheduler),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = ioScheduler ?: "...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(id = R.string.change_scheduler),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    if (showDialog) {
        IoSchedulerDialog(
            currentScheduler = ioScheduler ?: "",
            availableSchedulers = availableSchedulers,
            onSchedulerSelected = { scheduler ->
                onSchedulerChange(scheduler)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BypassChargingCard(
    bypassChargingEnabled: Boolean,
    isBypassChargingAvailable: Boolean?,
    isChargingControlEnabled: Boolean,
    onToggleBypassCharging: (Boolean) -> Unit,
) {
    // Treat null as false for UI purposes, preventing flicker during initial load
    val featureAvailable = isBypassChargingAvailable == true
    val isEnabled = featureAvailable && !isChargingControlEnabled

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = {
            if (isEnabled) {
                onToggleBypassCharging(!bypassChargingEnabled)
            }
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon with themed background
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = if (isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BatteryChargingFull,
                        contentDescription = null,
                        tint = if (isEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.bypass_charging),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.5f
                        )
                    )
                    Text(
                        text = if (isChargingControlEnabled) {
                            stringResource(id = R.string.charging_control_active_subtitle)
                        } else if (featureAvailable) {
                            stringResource(id = R.string.bypass_charging_desc)
                        } else {
                            stringResource(id = R.string.feature_not_available)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.5f
                        )
                    )
                }

                Switch(
                    checked = bypassChargingEnabled,
                    onCheckedChange = null,
                    enabled = isEnabled,
                    thumbContent = if (bypassChargingEnabled) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    } else {
                        {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    }
                )
            }

            if (bypassChargingEnabled && featureAvailable) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(id = R.string.bypass_charging_activated),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.bypass_charging_active_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargingControlCard(
    enabled: Boolean,
    isBatteryMonitorEnabled: Boolean,
    isBypassChargingEnabled: Boolean,
    onClick: () -> Unit
) {
    val isEnabled = isBatteryMonitorEnabled && !isBypassChargingEnabled
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp, 8.dp, 24.dp, 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = {
            if (isEnabled) {
                onClick()
            }
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon with themed background
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = if (isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BatterySaver,
                        contentDescription = null,
                        tint = if (isEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.charging_control_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = if (isBypassChargingEnabled) {
                             stringResource(id = R.string.bypass_active_subtitle)
                        } else if (isBatteryMonitorEnabled) {
                            stringResource(id = R.string.charging_control_desc)
                        } else {
                            stringResource(id = R.string.requires_battery_monitor)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    
                    if (isBatteryMonitorEnabled && !isBypassChargingEnabled) {
                        Text(
                            text = stringResource(id = R.string.charging_control_bypass_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Configure",
                    tint = if (isEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryHistoryCard(
    onClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon with themed background
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.battery_history_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(id = R.string.battery_history_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Auto-Reset Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "View History",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForceFastChargeCard(
    forceFastChargeEnabled: Boolean,
    isForceFastChargeAvailable: Boolean?,
    onToggleForceFastCharge: (Boolean) -> Unit,
) {
    // Treat null as false for UI purposes, preventing flicker during initial load
    val featureAvailable = isForceFastChargeAvailable == true

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = {
            if (featureAvailable) {
                onToggleForceFastCharge(!forceFastChargeEnabled)
            }
        }
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon with themed background
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = if (featureAvailable) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = null,
                        tint = if (featureAvailable) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.force_fast_charge),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (featureAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.5f
                        )
                    )
                    Text(
                        text = if (featureAvailable) {
                            stringResource(id = R.string.force_fast_charge_desc)
                        } else {
                            stringResource(id = R.string.feature_not_available)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (featureAvailable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(
                            alpha = 0.5f
                        )
                    )
                }

                Switch(
                    checked = forceFastChargeEnabled,
                    onCheckedChange = null,
                    enabled = featureAvailable,
                    thumbContent = if (forceFastChargeEnabled) {
                        {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    } else {
                        {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    }
                )
            }

            if (forceFastChargeEnabled && featureAvailable) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = stringResource(id = R.string.force_fast_charge_activated),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.force_fast_charge_active_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProcessMonitorCard(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp, 24.dp, 8.dp, 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon with themed background
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Analytics,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.process_monitor_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(id = R.string.process_monitor_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open Process Monitor",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DexoptCard(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon with themed background
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SettingsSuggest,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.dexopt_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(id = R.string.dexopt_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open Dexopt",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KernelLogCard(
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp, 8.dp, 24.dp, 24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.kernel_log_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(id = R.string.kernel_log_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open Kernel Log",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}