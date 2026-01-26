package id.jo.jomanager.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import id.jo.jomanager.R
import id.jo.jomanager.data.database.BatteryGraphEntry
import id.jo.jomanager.ui.MainActivity
import id.jo.jomanager.viewmodel.BatteryHistoryViewModel
import id.jo.jomanager.viewmodel.HistoryFilter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import id.jo.jomanager.data.model.AppUsageInfo

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BatteryHistoryScreen(
    navController: NavController,
    viewModel: BatteryHistoryViewModel = hiltViewModel()
) {
    val historyData by viewModel.historyData.collectAsState()
    val currentFilter by viewModel.filter.collectAsState()
    val appUsageList by viewModel.appUsageList.collectAsState()
    val hasUsagePermission by viewModel.hasUsagePermission.collectAsState()
    
    var graphMode by remember { mutableStateOf(BatteryGraphMode.SPEED) }
    var showClearDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val mainActivity = remember(context) { context as? MainActivity }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.loadAppUsageStats()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(Unit) {
        mainActivity?.batteryHistoryFabVisible?.value = true
        mainActivity?.batteryHistoryFabAction?.value = { showClearDialog = true }
        viewModel.loadAppUsageStats()
    }

    DisposableEffect(Unit) {
        onDispose {
            mainActivity?.batteryHistoryFabVisible?.value = false
            mainActivity?.batteryHistoryFabAction?.value = null
        }
    }

    if (showClearDialog) {
        ClearHistoryDialog(
            onConfirm = {
                viewModel.clearHistory()
                showClearDialog = false
            },
            onDismiss = { showClearDialog = false }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Mode Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                val modes = listOf(BatteryGraphMode.SPEED, BatteryGraphMode.DRAIN)
                modes.forEachIndexed { index, mode ->
                    val isSelected = graphMode == mode
                    ToggleButton(
                        checked = isSelected,
                        onCheckedChange = { graphMode = mode },
                        modifier = Modifier
                            .weight(1f)
                            .semantics { role = Role.RadioButton },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            modes.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        }
                    ) {
                        Icon(
                            imageVector = if (mode == BatteryGraphMode.SPEED) Icons.Default.Speed else Icons.Default.BatteryStd,
                            contentDescription = null,
                            modifier = Modifier.size(ToggleButtonDefaults.IconSize)
                        )
                        Spacer(Modifier.size(ToggleButtonDefaults.IconSpacing))
                        Text(
                            text = if (mode == BatteryGraphMode.SPEED) stringResource(R.string.graph_mode_speed_short) else stringResource(R.string.graph_mode_drain)
                        )
                    }
                }
            }

            // Graph Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val filterText = when (currentFilter) {
                        HistoryFilter.LAST_24_HOURS -> stringResource(R.string.filter_last_24h_short)
                        HistoryFilter.SINCE_UNPLUGGED -> stringResource(R.string.filter_since_unplugged)
                        HistoryFilter.PER_CYCLE -> stringResource(R.string.filter_per_cycle)
                    }
                    Text(
                        text = if (graphMode == BatteryGraphMode.SPEED) 
                            stringResource(R.string.graph_title_current, filterText)
                        else 
                            stringResource(R.string.graph_title_drain, filterText),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        if (graphMode == BatteryGraphMode.SPEED) {
                            LegendItem(color = MaterialTheme.colorScheme.primary, label = stringResource(R.string.legend_charge_speed))
                            LegendItem(color = MaterialTheme.colorScheme.tertiary, label = stringResource(R.string.legend_discharge_speed))
                        } else {
                            LegendItem(color = MaterialTheme.colorScheme.primary, label = stringResource(R.string.legend_active_drain))
                            LegendItem(color = MaterialTheme.colorScheme.tertiary, label = stringResource(R.string.legend_idle_drain))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    BatteryHistoryGraph(
                        data = historyData,
                        mode = graphMode,
                        primaryColor = MaterialTheme.colorScheme.primary,
                        secondaryColor = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            // Time Filter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
            ) {
                val filters = listOf(HistoryFilter.LAST_24_HOURS, HistoryFilter.SINCE_UNPLUGGED, HistoryFilter.PER_CYCLE)
                filters.forEachIndexed { index, filterOption ->
                    val isSelected = currentFilter == filterOption
                    ToggleButton(
                        checked = isSelected,
                        onCheckedChange = { viewModel.setFilter(filterOption) },
                        modifier = Modifier
                            .weight(1f)
                            .semantics { role = Role.RadioButton },
                        shapes = when (index) {
                            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                            filters.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                        }
                    ) {
                        Text(
                            text = when (filterOption) {
                                HistoryFilter.LAST_24_HOURS -> stringResource(R.string.filter_last_24h)
                                HistoryFilter.SINCE_UNPLUGGED -> stringResource(R.string.filter_since_unplugged)
                                HistoryFilter.PER_CYCLE -> stringResource(R.string.filter_per_cycle)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            
            // Stats Card
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                BatteryHistoryStatsCard(
                    data = historyData,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                )

                // App Usage Card
                AppUsageCard(
                    appUsageList = appUsageList,
                    hasPermission = hasUsagePermission,
                    onGrantPermission = { 
                        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
                    },
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                )
            }
            
            // Extra spacer for FAB
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClearHistoryDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(0.9f).heightIn(min = 200.dp),
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
                            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(R.string.clear_history_title),
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = stringResource(R.string.clear_history_title),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Content
                    Text(
                        text = stringResource(R.string.clear_history_confirmation),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

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
                            onClick = onConfirm,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text(stringResource(R.string.delete))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(modifier = Modifier.size(12.dp).background(color, RoundedCornerShape(4.dp)))
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}

enum class BatteryGraphMode {
    SPEED, DRAIN
}

@Composable
fun BatteryHistoryGraph(
    data: List<BatteryGraphEntry>,
    mode: BatteryGraphMode,
    primaryColor: Color,
    secondaryColor: Color
) {
    if (data.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.no_data_available), style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    val minTime = data.minOf { it.timestamp }
    val maxTime = data.maxOf { it.timestamp }
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    val (minY, maxY, labelSuffix) = if (mode == BatteryGraphMode.SPEED) {
        val min = data.minOf { it.currentMa }.coerceAtMost(0f)
        val max = data.maxOf { it.currentMa }.coerceAtLeast(0f)
        Triple(min, max, "mA")
    } else {
        val max = data.maxOf { maxOf(it.activeDrainRate, it.idleDrainRate) }.coerceAtLeast(1f)
        Triple(0f, max, "%/hr")
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            Canvas(modifier = Modifier.fillMaxSize().padding(vertical = 24.dp)) {
                val width = size.width
                val height = size.height
                val timeRange = (maxTime - minTime).coerceAtLeast(1L)
                val valRange = (maxY - minY).coerceAtLeast(1f)

                if (mode == BatteryGraphMode.SPEED) {
                    // Draw Zero Line
                    val zeroY = height * (1 - (0f - minY) / valRange)
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.5f),
                        start = Offset(0f, zeroY),
                        end = Offset(width, zeroY),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )

                    val chargePath = Path()
                    val dischargePath = Path()

                    data.forEachIndexed { index, entry ->
                        val x = width * (entry.timestamp - minTime) / timeRange
                        
                        // Charge Path (Positive current, else 0)
                        val chargeVal = entry.currentMa.coerceAtLeast(0f)
                        val chargeY = height * (1 - (chargeVal - minY) / valRange)
                        if (index == 0) chargePath.moveTo(x, chargeY) else chargePath.lineTo(x, chargeY)

                        // Discharge Path (Negative current, else 0)
                        val dischargeVal = entry.currentMa.coerceAtMost(0f)
                        val dischargeY = height * (1 - (dischargeVal - minY) / valRange)
                        if (index == 0) dischargePath.moveTo(x, dischargeY) else dischargePath.lineTo(x, dischargeY)
                    }

                    drawPath(
                        path = chargePath,
                        color = primaryColor,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = dischargePath,
                        color = secondaryColor,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                } else {
                    // DRAIN MODE
                    val activePath = Path()
                    val idlePath = Path()
                    
                    data.forEachIndexed { index, entry ->
                        val x = width * (entry.timestamp - minTime) / timeRange
                        
                        val activeY = height * (1 - entry.activeDrainRate / valRange)
                        if (index == 0) activePath.moveTo(x, activeY) else activePath.lineTo(x, activeY)
                        
                        val idleY = height * (1 - entry.idleDrainRate / valRange)
                        if (index == 0) idlePath.moveTo(x, idleY) else idlePath.lineTo(x, idleY)
                    }
                    
                    drawPath(
                        path = activePath,
                        color = primaryColor,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                    drawPath(
                        path = idlePath,
                        color = secondaryColor,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )
                }
            }

            // Labels
            Text(
                text = "${"%.0f".format(maxY)} $labelSuffix",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.TopStart)
            )
            Text(
                text = "${"%.0f".format(minY)} $labelSuffix",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
        
        // Time Labels
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = timeFormat.format(Date(minTime)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = timeFormat.format(Date(maxTime)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun BatteryHistoryStatsCard(
    data: List<BatteryGraphEntry>,
    shape: Shape = CardDefaults.shape
) {
    if (data.isEmpty()) return

    val chargeEntries = data.filter { it.currentMa > 0 }
    val dischargeEntries = data.filter { it.currentMa < 0 }

    val avgCharge = if (chargeEntries.isNotEmpty()) chargeEntries.map { it.currentMa }.average() else 0.0
    val maxCharge = if (chargeEntries.isNotEmpty()) chargeEntries.maxOf { it.currentMa } else 0f

    val avgChargeTemp = if (chargeEntries.isNotEmpty()) chargeEntries.map { it.temperature.toDouble() }.average() else 0.0
    val maxChargeTemp = if (chargeEntries.isNotEmpty()) chargeEntries.maxOf { it.temperature } else 0f
    
    val avgDischarge = if (dischargeEntries.isNotEmpty()) dischargeEntries.map { kotlin.math.abs(it.currentMa) }.average() else 0.0
    val maxDischarge = if (dischargeEntries.isNotEmpty()) dischargeEntries.minOf { it.currentMa }.let { kotlin.math.abs(it) } else 0f

    val avgDischargeTemp = if (dischargeEntries.isNotEmpty()) dischargeEntries.map { it.temperature.toDouble() }.average() else 0.0
    val maxDischargeTemp = if (dischargeEntries.isNotEmpty()) dischargeEntries.maxOf { it.temperature } else 0f

    val avgActiveDrain = if (data.isNotEmpty()) data.map { it.activeDrainRate.toDouble() }.average() else 0.0
    val avgIdleDrain = if (data.isNotEmpty()) data.map { it.idleDrainRate.toDouble() }.average() else 0.0

    // Calculate time and consumption stats
    var totalDischargeTimeMs = 0L
    var screenOnTimeMs = 0L
    var screenOffTimeMs = 0L
    
    var totalDischargeMah = 0.0
    var screenOnMah = 0.0
    var screenOffMah = 0.0

    // Sort data by timestamp just in case
    val sortedData = data.sortedBy { it.timestamp }
    for (i in 0 until sortedData.size - 1) {
        val current = sortedData[i]
        val next = sortedData[i + 1]
        
        // Skip if gap is too large (e.g. service killed), > 5 mins
        val dt = next.timestamp - current.timestamp
        if (dt > 5 * 60 * 1000) continue
        
        // Only consider discharging periods for these stats
        if (current.currentMa < 0) {
            val avgCurrentMa = (kotlin.math.abs(current.currentMa) + kotlin.math.abs(next.currentMa)) / 2.0
            val mah = (avgCurrentMa * dt) / 3_600_000.0
            
            totalDischargeTimeMs += dt
            totalDischargeMah += mah
            
            if (current.isScreenOn) {
                screenOnTimeMs += dt
                screenOnMah += mah
            } else {
                screenOffTimeMs += dt
                screenOffMah += mah
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.history_stats_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            if (totalDischargeTimeMs > 0) {
                // Helper to format: "2h 30m • 45% (1200 mAh)"
                // Note: % here is share of total observed drain
                fun formatUsage(timeMs: Long, mah: Double): String {
                    val pct = if (totalDischargeMah > 0) (mah / totalDischargeMah * 100).toInt() else 0
                    val duration = formatDuration(timeMs)
                    return "$duration • $pct% (${mah.toInt()} mAh)"
                }

                StatRow(label = stringResource(R.string.stats_total_active_time), value = formatUsage(totalDischargeTimeMs, totalDischargeMah))
                StatRow(label = stringResource(R.string.stats_screen_on_time), value = formatUsage(screenOnTimeMs, screenOnMah))
                StatRow(label = stringResource(R.string.stats_screen_off_time), value = formatUsage(screenOffTimeMs, screenOffMah))
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (avgCharge > 0) {
                StatRow(label = stringResource(R.string.stats_avg_charge_speed), value = "%.0f mA".format(avgCharge))
                StatRow(label = stringResource(R.string.stats_max_charge_speed), value = "%.0f mA".format(maxCharge))
                StatRow(label = stringResource(R.string.stats_avg_charge_temp), value = "%.1f °C".format(avgChargeTemp))
                StatRow(label = stringResource(R.string.stats_max_charge_temp), value = "%.1f °C".format(maxChargeTemp))
            }
            
            if (avgDischarge > 0) {
                StatRow(label = stringResource(R.string.stats_avg_discharge_speed), value = "-%.0f mA".format(avgDischarge))
                StatRow(label = stringResource(R.string.stats_max_discharge_speed), value = "-%.0f mA".format(maxDischarge))
                StatRow(label = stringResource(R.string.stats_avg_discharge_temp), value = "%.1f °C".format(avgDischargeTemp))
                StatRow(label = stringResource(R.string.stats_max_discharge_temp), value = "%.1f °C".format(maxDischargeTemp))
            }
            
            StatRow(label = stringResource(R.string.stats_avg_active_drain), value = "%.2f %%/hr".format(avgActiveDrain))
            StatRow(label = stringResource(R.string.stats_avg_idle_drain), value = "%.2f %%/hr".format(avgIdleDrain))
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSec = ms / 1000
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun AppUsageCard(
    appUsageList: List<AppUsageInfo>,
    hasPermission: Boolean,
    onGrantPermission: () -> Unit,
    shape: Shape
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = stringResource(R.string.app_usage_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!hasPermission) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.usage_permission_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Button(onClick = onGrantPermission) {
                        Text(text = stringResource(R.string.grant_usage_permission))
                    }
                }
            } else if (appUsageList.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_data_available),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    appUsageList.forEach { app ->
                        AppUsageItem(app)
                    }
                }
            }
        }
    }
}

@Composable
fun AppUsageItem(app: AppUsageInfo) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (app.icon != null) {
            Image(
                bitmap = app.icon.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(8.dp))
            )
        }
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Top Row: Name and Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = app.formattedTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.width(64.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }

            // Bottom Row: Progress Bar and Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LinearProgressIndicator(
                    progress = { app.usagePercentage / 100f },
                    modifier = Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
                Text(
                    text = "${app.usagePercentage}% (${"%.1f".format(app.powerConsumptionMah)} mAh)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.End
                )
            }
        }
    }
}
