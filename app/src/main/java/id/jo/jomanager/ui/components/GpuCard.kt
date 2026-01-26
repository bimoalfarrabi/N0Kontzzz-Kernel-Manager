package id.jo.jomanager.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import id.jo.jomanager.R
import id.jo.jomanager.data.model.RealtimeGpuInfo
import kotlinx.collections.immutable.ImmutableList
import androidx.compose.foundation.clickable
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import android.widget.Toast
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.platform.LocalContext

const val MAX_GPU_HISTORY_POINTS = 50

@Composable
fun GpuCard(
    info: RealtimeGpuInfo,
    gpuHistory: ImmutableList<Float>,
    modifier: Modifier = Modifier
) {
    var showDriverDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp, 8.dp, 8.dp, 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GpuHeaderSection(
                info = info,
                onDriverInfoClick = { showDriverDialog = true }
            )

            GpuStatsSection(info = info, graphDataHistory = gpuHistory)

            EnhancedGpuGraph(
                graphDataHistory = gpuHistory,
                primaryColor = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (showDriverDialog) {
        GpuDriverDialog(
            info = info,
            onDismiss = { showDriverDialog = false }
        )
    }
}

@Composable
private fun GpuHeaderSection(
    info: RealtimeGpuInfo,
    onDriverInfoClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = if (info.model.isNotBlank() && info.model != "N/A" && info.model != "Graphics Processing Unit (GPU)") {
                    // Truncate long GPU model names but make sure we show meaningful info
                    var modelName = info.model
                    // Add trademark symbol after Adreno
                    if (modelName.contains("Adreno")) {
                        modelName = modelName.replace("Adreno", "Adreno™")
                    }
                    if (modelName.length > 25) {
                        modelName.take(22) + "..."
                    } else {
                        modelName
                    }
                } else {
                    stringResource(R.string.graphics_processing_unit_gpu)
                },
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = stringResource(R.string.gpu_label),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                if (info.glVersion.isNotBlank()) {
                        val simplifiedVersion = remember(info.glVersion) {
                        val v = info.glVersion.replace("OpenGL ES", "").trim()
                        if (v.length > 20) v.take(20) + "..." else v
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier.clickable { onDriverInfoClick() }
                    ) {
                        Text(
                            text = stringResource(R.string.gpu_gl_es_prefix, simplifiedVersion),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // GPU icon
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.gpu_card),
                contentDescription = stringResource(id = R.string.gpu_icon_desc),
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GpuDriverDialog(
    info: RealtimeGpuInfo,
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    
    val vendor = remember(info.model) {
        when {
            info.model.contains("Adreno", ignoreCase = true) -> "Qualcomm"
            info.model.contains("Mali", ignoreCase = true) -> "ARM"
            info.model.contains("PowerVR", ignoreCase = true) -> "Imagination Technologies"
            info.model.contains("Xclipse", ignoreCase = true) -> "Samsung (AMD RDNA)"
            info.model.contains("Immortalis", ignoreCase = true) -> "ARM"
            info.model.contains("Vivante", ignoreCase = true) -> "VeriSilicon"
            else -> context.getString(R.string.unknown_vendor)
        }
    }

    // Prepare labels and values using resources inside composable context where needed
    val labelRenderer = stringResource(R.string.gpu_renderer)
    val labelVendor = stringResource(R.string.gpu_vendor)
    val labelDriver = stringResource(R.string.gpu_driver_version)
    val labelOpenGl = stringResource(R.string.gpu_opengl_version)
    val unknownText = stringResource(R.string.unknown)
    val copiedText = stringResource(R.string.copied_to_clipboard)

    val driverDetails = listOf(
        labelRenderer to info.model,
        labelVendor to vendor,
        labelDriver to info.glVersion, 
    )

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.fillMaxWidth().wrapContentHeight(),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color.Transparent),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) + 
                        scaleIn(initialScale = 0.95f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)),
                exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + 
                       scaleOut(targetScale = 0.95f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
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
                                    imageVector = Icons.Default.Info, // Changed icon to Info
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Column {
                                Text(
                                    text = stringResource(R.string.gpu_driver_info_title),
                                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.gpu_driver_info_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Details List
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            driverDetails.forEachIndexed { index, (label, value) ->
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                                    ),
                                    shape = when {
                                        driverDetails.size == 1 -> RoundedCornerShape(24.dp)
                                        index == 0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp)
                                        index == driverDetails.size - 1 -> RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp)
                                        else -> RoundedCornerShape(8.dp)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = label,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = value.ifBlank { unknownText },
                                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        
                                        IconButton(
                                            onClick = {
                                                clipboardManager.setText(AnnotatedString("$label: $value"))
                                                Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentCopy,
                                                contentDescription = "Copy",
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Dismiss Button
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(stringResource(id = R.string.close))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GpuStatsSection(
    info: RealtimeGpuInfo,
    graphDataHistory: ImmutableList<Float>
) {
    Card(
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
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.Top
            ) {
                StatItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Speed,
                    label = stringResource(R.string.gpu_current_freq),
                    value = if (info.currentFreq > 0) stringResource(id = R.string.cpu_freq_mhz, info.currentFreq) else stringResource(id = R.string.common_na),
                    iconColor = MaterialTheme.colorScheme.primary
                )

                StatItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.Memory,
                    label = stringResource(R.string.gpu_max_freq),
                    value = if (info.maxFreq > 0) stringResource(id = R.string.cpu_freq_mhz, info.maxFreq) else stringResource(id = R.string.common_na),
                    iconColor = MaterialTheme.colorScheme.primary
                )

                StatItem(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.BarChart,
                    label = stringResource(R.string.gpu_usage),
                    value = if (info.usagePercentage != null) stringResource(id = R.string.gpu_usage_format, info.usagePercentage.toInt()) else stringResource(id = R.string.common_na),
                    iconColor = when {
                        (info.usagePercentage?.toInt() ?: 0) > 80 -> MaterialTheme.colorScheme.error
                        (info.usagePercentage?.toInt() ?: 0) > 60 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.primary
                    }
                )
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
    iconColor: Color
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun EnhancedGpuGraph(
    graphDataHistory: ImmutableList<Float>,
    primaryColor: Color
) {
    val path = remember { Path() }
    val fillPath = remember { Path() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    ) {
        Box(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            if (graphDataHistory.size > 1) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    path.reset()
                    fillPath.reset()

                    val yAxisMin = 0f
                    val yAxisMax = 100f
                    val effectiveYRange = (yAxisMax - yAxisMin).coerceAtLeast(1f)
                    val stepX = size.width / (MAX_GPU_HISTORY_POINTS - 1).coerceAtLeast(1).toFloat()

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

                    if (graphDataHistory.isNotEmpty()) {
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