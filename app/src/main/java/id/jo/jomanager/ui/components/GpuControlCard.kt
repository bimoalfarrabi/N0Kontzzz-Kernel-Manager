package id.jo.jomanager.ui.components

import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideogameAsset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import id.jo.jomanager.R
import id.jo.jomanager.viewmodel.TuningViewModel
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun GpuControlCard(
    tuningViewModel: TuningViewModel = hiltViewModel(),
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    blur: Boolean = false
) {
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

    // State variables
    var showGovernorDialog by remember { mutableStateOf(false) }
    var showMinFreqDialog by remember { mutableStateOf(false) }
    var showMaxFreqDialog by remember { mutableStateOf(false) }

    // Collect GPU states from ViewModel
    val gpuGovernor by tuningViewModel.currentGpuGovernor.collectAsState()
    val availableGovernors by tuningViewModel.availableGpuGovernors.collectAsState()
    val gpuMinFreq by tuningViewModel.currentGpuMinFreq.collectAsState()
    val gpuMaxFreq by tuningViewModel.currentGpuMaxFreq.collectAsState()
    val availableGpuFrequencies by tuningViewModel.availableGpuFrequencies.collectAsState()
    val gpuPowerLevelRange by tuningViewModel.gpuPowerLevelRange.collectAsState()
    val currentGpuPowerLevel by tuningViewModel.currentGpuPowerLevel.collectAsState()
    
    // State for power level that updates during dragging but only applies when released
    var tempPowerLevel by remember { mutableFloatStateOf(currentGpuPowerLevel) }
    
    // Update tempPowerLevel when currentGpuPowerLevel changes externally
    LaunchedEffect(currentGpuPowerLevel) {
        tempPowerLevel = currentGpuPowerLevel
    }

    // No frequency ranges needed since we're using dialogs instead of sliders

    // Animation values - Simplified MD3 animation
    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "arrow_rotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = RoundedCornerShape(8.dp, 8.dp, 8.dp, 8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandChange(!isExpanded) },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.gpu_card),
                            contentDescription = stringResource(id = R.string.gpu_icon_desc),
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Column {
                        Text(
                            text = stringResource(id = R.string.gpu_control_title),
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) stringResource(id = R.string.collapse) else stringResource(id = R.string.expand),
                    modifier = Modifier
                        .size(24.dp)
                        .graphicsLayer { rotationZ = rotationAngle },
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = stringResource(id = R.string.gpu_control_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Expanded Content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)) + 
                        fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)),
                exit = shrinkVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)) + 
                       fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
            ) {
                Column {
                    // GPU Governor Control
                    GPUControlSection(
                        title = stringResource(id = R.string.gpu_governor),
                        description = stringResource(id = R.string.gpu_governor_desc),
                        icon = Icons.Default.Tune
                    ) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showGovernorDialog = true },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            shape = getRoundedCornerShape(0, 1) // Only one item
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
                                        text = stringResource(id = R.string.current_governor),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = gpuGovernor.takeIf { it != "..." && it.isNotBlank() } ?: stringResource(id = R.string.common_unknown_value),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = stringResource(id = R.string.change_governor),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // GPU Frequency Control
                    GPUControlSection(
                        title = stringResource(id = R.string.gpu_frequency),
                        description = stringResource(id = R.string.gpu_frequency_desc),
                        icon = Icons.Default.Speed
                    ) {
                        // Cards for GPU frequency control with dialogs
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(2.dp) // Changed from 12.dp to 2.dp as per your other requirements
                        ) {
                            // Min Frequency Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showMinFreqDialog = true },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = getRoundedCornerShape(0, 2) // First card in group of 2
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
                                            text = stringResource(id = R.string.min_frequency_label),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = stringResource(id = R.string.cpu_freq_mhz, gpuMinFreq),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = stringResource(id = R.string.change_min_frequency),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Max Frequency Card
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showMaxFreqDialog = true },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = getRoundedCornerShape(1, 2) // Second card in group of 2
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
                                            text = stringResource(id = R.string.max_frequency_label),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = stringResource(id = R.string.cpu_freq_mhz, gpuMaxFreq),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = stringResource(id = R.string.change_max_frequency),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }



                    Spacer(modifier = Modifier.height(16.dp))

                    // GPU Power Level and Throttling Control
                    GPUControlSection(
                        title = stringResource(id = R.string.gpu_power_level),
                        description = stringResource(id = R.string.gpu_power_level_desc),
                        icon = Icons.Default.VideogameAsset
                    ) {
                        // Determine min and max values for GPU power level at this scope
                        val minPowerLevel = minOf(gpuPowerLevelRange.first, gpuPowerLevelRange.second)
                        val maxPowerLevel = maxOf(gpuPowerLevelRange.first, gpuPowerLevelRange.second)
                        
                        Column(
                            verticalArrangement = Arrangement.spacedBy(2.dp) // Changed from 12.dp to 2.dp as per your other requirements
                        ) {
                            // GPU Power Level Card
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = getRoundedCornerShape(0, 2) // First card in group of 2
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.power_level),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                        Text(
                                            text = tempPowerLevel.toInt().toString(),
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Slider(
                                        value = tempPowerLevel,
                                        onValueChange = { newValue ->
                                            // Update the temporary value during dragging for smooth UI
                                            tempPowerLevel = newValue
                                            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                        },
                                        onValueChangeFinished = {
                                            // Only apply the change when user stops dragging
                                            tuningViewModel.setGpuPowerLevel(tempPowerLevel)
                                        },
                                        valueRange = minPowerLevel..maxPowerLevel,
                                        colors = SliderDefaults.colors(
                                            thumbColor = MaterialTheme.colorScheme.primary,
                                            activeTrackColor = MaterialTheme.colorScheme.primary,
                                            inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        steps = if (abs(maxPowerLevel - minPowerLevel) > 1f) {
                                            abs(maxPowerLevel - minPowerLevel).toInt() - 1
                                        } else {
                                            0 // No steps if the range is too small
                                        }
                                    )
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = minPowerLevel.toInt().toString(),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = maxPowerLevel.toInt().toString(),
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // GPU Throttling Switch Card
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                ),
                                shape = getRoundedCornerShape(1, 2) // Second card in group of 2
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
                                            text = stringResource(id = R.string.gpu_throttling),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = stringResource(id = R.string.gpu_throttling_desc),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    val gpuThrottlingEnabled by tuningViewModel.gpuThrottlingEnabled.collectAsState()
                                    Switch(
                                        checked = gpuThrottlingEnabled,
                                        onCheckedChange = { checked ->
                                            tuningViewModel.toggleGpuThrottling(checked)
                                        },
                                        thumbContent = if (gpuThrottlingEnabled) {
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
                }
            }
        }
    }

    // Governor Selection Dialog
    if (showGovernorDialog) {
        GpuGovernorSelectionDialog(
            availableGovernors = availableGovernors,
            currentGovernor = gpuGovernor,
            onGovernorSelected = { governor ->
                coroutineScope.launch {
                    tuningViewModel.setGpuGovernor(governor)
                }
                showGovernorDialog = false
            },
            onDismiss = { showGovernorDialog = false }
        )
    }



    // Min Frequency Selection Dialog
    if (showMinFreqDialog) {
        GpuFrequencySelectionDialog(
            title = stringResource(id = R.string.set_min_frequency),
            availableFrequencies = availableGpuFrequencies,
            currentFrequency = gpuMinFreq,
            onFrequencySelected = { frequency ->
                coroutineScope.launch {
                    tuningViewModel.setGpuMinFrequency(frequency)
                }
                showMinFreqDialog = false
            },
            onDismiss = { showMinFreqDialog = false }
        )
    }

    // Max Frequency Selection Dialog
    if (showMaxFreqDialog) {
        GpuFrequencySelectionDialog(
            title = stringResource(id = R.string.set_max_frequency),
            availableFrequencies = availableGpuFrequencies,
            currentFrequency = gpuMaxFreq,
            onFrequencySelected = { frequency ->
                coroutineScope.launch {
                    tuningViewModel.setGpuMaxFrequency(frequency)
                }
                showMaxFreqDialog = false
            },
            onDismiss = { showMaxFreqDialog = false }
        )
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GpuFrequencySelectionDialog(
    title: String,
    availableFrequencies: List<Int>,
    currentFrequency: Int,
    onFrequencySelected: (Int) -> Unit,
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
                modifier = Modifier.fillMaxWidth(0.9f).heightIn(min = 300.dp, max = 600.dp),
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
                                imageVector = Icons.Default.Speed,
                                contentDescription = stringResource(id = R.string.gpu_frequency),
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Options List
                    if (availableFrequencies.isEmpty()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 350.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            val sortedFrequencies = availableFrequencies.sorted()
                            itemsIndexed(sortedFrequencies) { index, frequency ->
                                val isSelected = frequency == currentFrequency
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = getDialogListItemShape(index, sortedFrequencies.size),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                                    ),
                                    onClick = { onFrequencySelected(frequency) }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { onFrequencySelected(frequency) },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = MaterialTheme.colorScheme.primary,
                                                unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        )
                                        Text(
                                            text = stringResource(id = R.string.cpu_freq_mhz, frequency),
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GpuGovernorSelectionDialog(
    availableGovernors: List<String>,
    currentGovernor: String,
    onGovernorSelected: (String) -> Unit,
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
                modifier = Modifier.fillMaxWidth(0.9f).heightIn(min = 300.dp, max = 600.dp),
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
                                imageVector = Icons.Default.Tune,
                                contentDescription = stringResource(id = R.string.gpu_governor),
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.set_gpu_governor),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Options List
                    if (availableGovernors.isEmpty()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 350.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            val sortedGovernors = availableGovernors.sorted()
                            itemsIndexed(sortedGovernors) { index, governor ->
                                val isSelected = governor == currentGovernor
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = getDialogListItemShape(index, sortedGovernors.size),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                                    ),
                                    onClick = { onGovernorSelected(governor) }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { onGovernorSelected(governor) },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = MaterialTheme.colorScheme.primary,
                                                unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        )
                                        Text(
                                            text = governor,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            ),
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
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

@Composable
private fun GPUControlSection(
    title: String,
    description: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(42.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        content()
    }
}

private fun getRoundedCornerShape(index: Int, totalItems: Int): RoundedCornerShape {
    return when (totalItems) {
        1 -> RoundedCornerShape(12.dp) // If only one card, all corners 12dp
        2 -> {
            when (index) {
                0 -> RoundedCornerShape( // First card: 12dp top, 4dp bottom
                    topStart = 12.dp,
                    topEnd = 12.dp,
                    bottomStart = 4.dp,
                    bottomEnd = 4.dp
                )
                1 -> RoundedCornerShape( // Second card: 4dp top, 12dp bottom
                    topStart = 4.dp,
                    topEnd = 4.dp,
                    bottomStart = 12.dp,
                    bottomEnd = 12.dp
                )
                else -> RoundedCornerShape(12.dp) // Default case
            }
        }
        else -> RoundedCornerShape(12.dp) // Default for other cases
    }
}

private fun getDialogListItemShape(index: Int, totalItems: Int): RoundedCornerShape {
    return when {
        totalItems == 1 -> RoundedCornerShape(16.dp)
        index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        index == totalItems - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
        else -> RoundedCornerShape(4.dp)
    }
}