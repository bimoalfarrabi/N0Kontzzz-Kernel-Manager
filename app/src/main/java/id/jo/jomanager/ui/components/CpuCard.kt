package id.jo.jomanager.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.jo.jomanager.R
import id.jo.jomanager.data.model.CpuCluster
import id.jo.jomanager.data.model.RealtimeCpuInfo
import id.jo.jomanager.data.model.GraphData
import id.jo.jomanager.data.model.GraphMode
import kotlinx.collections.immutable.ImmutableList
import kotlin.math.roundToInt

const val MAX_HISTORY_POINTS_GRAPH = 50

@Composable
fun CpuCard(
    soc: String,
    board: String,
    deviceCodename: String,
    info: RealtimeCpuInfo,
    clusters: ImmutableList<CpuCluster>,
    graphData: GraphData,
    onGraphModeChange: (GraphMode) -> Unit,
    modifier1: Boolean, // Parameter tetap ada
    modifier: Modifier = Modifier
) {
    val currentGraphMode = graphData.cpuGraphMode

    Card( // Mengganti ElevatedCard menjadi Card dengan konfigurasi baru
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp, 24.dp, 8.dp, 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 12.dp, 16.dp, 0.dp), // Consistent padding
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CpuHeaderSection(soc = soc, board = board, deviceCodename = deviceCodename, info = info)

            if (info.freqs.isNotEmpty()) {
                CpuCoresSection(info = info, clusters = clusters)
            }

            CpuStatsSection(
                info = info, 
                currentGraphMode = currentGraphMode, 
                graphDataHistory = if (currentGraphMode == GraphMode.LOAD) graphData.cpuLoadHistory else graphData.cpuSpeedHistory
            )

            EnhancedCpuGraph(
                graphDataHistory = if (currentGraphMode == GraphMode.LOAD) graphData.cpuLoadHistory else graphData.cpuSpeedHistory,
                currentGraphMode = currentGraphMode,
                primaryColor = MaterialTheme.colorScheme.primary
            )

            CpuGraphModeToggle(
                currentGraphMode = currentGraphMode,
                onModeChanged = onGraphModeChange
            )
        }
    }
}

@Composable
private fun CpuHeaderSection(
    soc: String,
    board: String,
    deviceCodename: String,
    info: RealtimeCpuInfo
) {
    val (marketingName, boardName) = remember(board, deviceCodename) {
        when (board.uppercase()) {
            "SM8250" -> {
                if (deviceCodename.lowercase() in listOf("munch", "alioth")) {
                    "Qualcomm® Snapdragon™ 870 5G" to "SM8250-AC"
                } else {
                    "Qualcomm® Snapdragon™ 865 5G" to "SM8250"
                }
            }
            "SM8250-AB" -> "Qualcomm® Snapdragon™ 865+ 5G" to "SM8250-AB"
            "SM8250-AC" -> "Qualcomm® Snapdragon™ 870 5G" to "SM8250-AC"
            else -> "Definitely not Kona family" to ""
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = marketingName,
                style = MaterialTheme.typography.headlineSmall, // M3 Typography
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (boardName.isNotEmpty()) {
                Text(
                    text = boardName,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Surface( // Using Surface for the label background
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = if ((soc.isNotBlank() && soc != "Unknown SoC" && soc != "N/A") ||
                        (info.soc.isNotBlank() && info.soc != "Unknown SoC" && info.soc != "N/A"))
                        stringResource(R.string.cpu_soc_label) else stringResource(R.string.cpu_cpu_label),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

        // Simplified CPU icon
        Box(
            modifier = Modifier
                .size(56.dp) // Slightly smaller
                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Memory, // Memory icon is fine
                contentDescription = "CPU Icon",
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun CpuCoresSection(info: RealtimeCpuInfo, clusters: ImmutableList<CpuCluster>) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Text(
            text = stringResource(R.string.cpu_cores_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))

        // --- Active Cores Section ---
        val activeCoresFreq = info.freqs
        val itemsPerRow = 4 // Fixed at 4 as per the layout description
        val totalCores = activeCoresFreq.size

        activeCoresFreq.chunked(itemsPerRow).forEachIndexed { rowIndex, freqsInRow ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(2.dp) // Horizontal spacing set to 2dp
            ) {
                freqsInRow.forEachIndexed { cardIndexInRow, freq ->
                    val absoluteIndex = rowIndex * itemsPerRow + cardIndexInRow
                    
                    val shape = when (absoluteIndex) {
                        0 -> RoundedCornerShape(topStart = 12.dp, topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                        3 -> RoundedCornerShape(topStart = 4.dp, topEnd = 12.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
                        4 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 12.dp, bottomEnd = 4.dp)
                        7 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 4.dp, bottomEnd = 12.dp)
                        else -> RoundedCornerShape(4.dp)
                    }

                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = shape, // Apply conditional shape
                        color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 6.dp)
                        ) {
                            Text(
                                text = "Core $absoluteIndex", // Use absolute index
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            if (freq == 0) {
                                Text(
                                    text = stringResource(R.string.cpu_core_offline),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                )
                            } else {
                                Text(
                                    text = "$freq",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = "MHz",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                )
                            }
                        }
                    }
                }
                // Fill remaining space in the row if chunk size is not met
                repeat((itemsPerRow - freqsInRow.size).coerceAtLeast(0)) {
                    Spacer(Modifier.weight(1f))
                }
            }
            if (rowIndex < (totalCores - 1) / itemsPerRow) {
                Spacer(modifier = Modifier.height(2.dp)) // Vertical spacing set to 2dp
            }
        }

        // Add a spacer between the two sections
        Spacer(modifier = Modifier.height(16.dp))

        // --- CPU Clusters Section ---
        if (clusters.isNotEmpty()) {
            Text(
                text = "CPU Clusters", // Title for this section
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))
            clusters.forEachIndexed { index, cluster ->
                if (index > 0) {
                    Spacer(modifier = Modifier.height(2.dp))
                }
                // Determine rounded corners based on position
                val shape = when (index) {
                    0 if index == clusters.size - 1 -> RoundedCornerShape(12.dp) // Only item
                    0 -> RoundedCornerShape(12.dp, 12.dp, 4.dp, 4.dp) // First item
                    clusters.size - 1 -> RoundedCornerShape(4.dp, 4.dp, 12.dp, 12.dp) // Last item
                    else -> RoundedCornerShape(4.dp) // Middle items
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = shape,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp, horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = cluster.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Max: ${cluster.maxFreq} MHz",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CpuStatsSection(
    info: RealtimeCpuInfo,
    currentGraphMode: GraphMode,
    graphDataHistory: ImmutableList<Float>
) {
    Card( // Using Card for stats section
        modifier = Modifier.fillMaxWidth(),
        shape = CardDefaults.shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.system_stats_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround, // SpaceAround for better distribution
                verticalAlignment = Alignment.Top // Align to top for potentially varying text heights
            ) {
                StatItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Speed,
                    label = stringResource(R.string.cpu_governor_label),
                    value = info.governor.takeIf { it.isNotBlank() } ?: "N/A",
                    iconColor = MaterialTheme.colorScheme.primary
                )

                StatItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Thermostat,
                    label = stringResource(R.string.temperature_label),
                    value = "${info.temp}°C",
                    iconColor = when {
                        info.temp > 80 -> MaterialTheme.colorScheme.error
                        info.temp > 60 -> MaterialTheme.colorScheme.tertiary // M3 warning/orange
                        else -> MaterialTheme.colorScheme.primary
                    }
                )

                if (currentGraphMode == GraphMode.LOAD && graphDataHistory.isNotEmpty()) {
                    StatItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.BarChart,
                        label = stringResource(R.string.cpu_load_label),
                        value = "${graphDataHistory.lastOrNull()?.roundToInt() ?: 0}%",
                        iconColor = MaterialTheme.colorScheme.primary
                    )
                } else {
                     StatItem(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Memory, // Using Memory icon for Cores
                        label = stringResource(R.string.cpu_cores_label),
                        value = "${info.cores}",
                        iconColor = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun StatItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    iconColor: Color // Icon color, text will use onSurface/onSurfaceVariant
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp), // Add some horizontal padding
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(24.dp) // M3 standard icon size
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium, // M3 Typography
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold), // M3 Typography
            color = MaterialTheme.colorScheme.onSurface, // Main content color
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun EnhancedCpuGraph(
    graphDataHistory: ImmutableList<Float>,
    currentGraphMode: GraphMode,
    primaryColor: Color
) {
    val path = remember { Path() }
    val fillPath = remember { Path() }

    Surface( // Use Surface for graph background
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(12.dp), // Consistent rounding
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(8.dp), // Padding inside the graph box
            contentAlignment = Alignment.Center
        ) {
            if (graphDataHistory.size > 1) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    path.reset()
                    fillPath.reset()

                    val (yAxisMin, yAxisMax) = when (currentGraphMode) {
                        GraphMode.SPEED -> {
                            val dataMin = graphDataHistory.filter { it > 0 }.minOrNull() ?: 0f
                            val dataMax = (graphDataHistory.filter { it > 0 }.maxOrNull() ?: 4000f).coerceAtLeast(dataMin + 100f)
                            val yPadding = (dataMax - dataMin).coerceAtLeast(100f) * 0.1f
                            (dataMin - yPadding).coerceAtLeast(0f) to (dataMax + yPadding).coerceAtLeast(dataMin + 100f)
                        }
                        GraphMode.LOAD -> 0f to 100f
                    }

                    val effectiveYRange = (yAxisMax - yAxisMin).coerceAtLeast(1f)
                    val stepX = size.width / (MAX_HISTORY_POINTS_GRAPH - 1).coerceAtLeast(1).toFloat()

                    graphDataHistory.forEachIndexed { index, dataPoint ->
                        val x = size.width - (graphDataHistory.size - 1 - index) * stepX
                        val normalizedData = ((dataPoint.coerceAtLeast(yAxisMin) - yAxisMin) / effectiveYRange).coerceIn(0f, 1f)
                        val y = size.height * (1 - normalizedData)

                        val clampedY = y.coerceIn(0f, size.height)
                        if (index == 0) {
                            path.moveTo(x, clampedY)
                            fillPath.moveTo(x, size.height)
                            fillPath.lineTo(x, clampedY)
                        } else {
                            path.lineTo(x, clampedY)
                            fillPath.lineTo(x, clampedY)
                        }
                    }

                    if (graphDataHistory.isNotEmpty()) { // Ensure fillPath is closed properly
                        fillPath.lineTo(size.width, size.height)
                        fillPath.close()

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.3f),
                                    primaryColor.copy(alpha = 0.05f)
                                )
                            )
                        )
                    }

                    drawPath(
                        path = path,
                        color = primaryColor,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Draw a small circle for the last data point
                    if (graphDataHistory.isNotEmpty()) {
                        val lastDataPoint = graphDataHistory.last()
                        val lastX = size.width
                        val lastNormalizedData = ((lastDataPoint.coerceAtLeast(yAxisMin) - yAxisMin) / effectiveYRange).coerceIn(0f, 1f)
                        val lastY = size.height * (1 - lastNormalizedData)
                         drawCircle(
                            color = primaryColor,
                            radius = 4.dp.toPx(),
                            center = Offset(lastX, lastY.coerceIn(0f, size.height))
                        )
                    }
                }
            } else {
                Text(
                    text = stringResource(R.string.gathering_data_placeholder),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun CpuGraphModeToggle(
    currentGraphMode: GraphMode,
    onModeChanged: (GraphMode) -> Unit
) {
    // No separate surface needed, integrate directly or use a simple Row
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp), // Add some vertical padding
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(
                imageVector = if (currentGraphMode == GraphMode.SPEED) Icons.Default.Speed else Icons.Default.BarChart,
                contentDescription = "Current Graph Mode Icon", // Better content description
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = if (currentGraphMode == GraphMode.SPEED)
                    stringResource(R.string.cpu_graph_mode_speed) else stringResource(R.string.cpu_graph_mode_load),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Switch(
            checked = currentGraphMode == GraphMode.LOAD,
            onCheckedChange = { isChecked ->
                onModeChanged(if (isChecked) GraphMode.LOAD else GraphMode.SPEED)
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.primary,
                checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                checkedIconColor = MaterialTheme.colorScheme.onPrimary,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant,
                uncheckedIconColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            thumbContent = if (currentGraphMode == GraphMode.LOAD) {
                {
                    Icon(
                        imageVector = Icons.Filled.BarChart,
                        contentDescription = stringResource(R.string.cpu_load_label),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                }
            } else {
                {
                    Icon(
                        imageVector = Icons.Filled.Speed,
                        contentDescription = stringResource(R.string.cpu_graph_mode_speed),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(SwitchDefaults.IconSize),
                    )
                }
            }
        )
    }
}