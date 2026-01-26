package id.jo.jomanager.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import id.jo.jomanager.viewmodel.TuningViewModel
import kotlin.math.abs

import androidx.compose.ui.res.stringResource
import id.jo.jomanager.R

@Composable
fun CpuGovernorCard(
    vm: TuningViewModel,
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit
) {
    val clusters = vm.cpuClusters
    val availableGovernors by vm.generalAvailableCpuGovernors.collectAsState()
    val coreStates by vm.coreStates.collectAsState()

    var showGovernorDialogForCluster by remember { mutableStateOf<String?>(null) }
    var showMinFreqDialogForCluster by remember { mutableStateOf<String?>(null) }
    var showMaxFreqDialogForCluster by remember { mutableStateOf<String?>(null) }
    var showCoreDialogForCluster by remember { mutableStateOf<String?>(null) }

    val rotationAngle by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "DropdownRotation"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp, 8.dp, 8.dp, 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with expand/collapse
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandChange(!isExpanded) },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
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
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Text(
                        text = stringResource(id = R.string.cpu_control),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) stringResource(id = R.string.collapse) else stringResource(id = R.string.expand),
                    modifier = Modifier.graphicsLayer { rotationZ = rotationAngle },
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = stringResource(id = R.string.configure_cpu_governor_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)) + 
                        fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)),
                exit = shrinkVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)) + 
                       fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
            ) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (availableGovernors.isEmpty() && clusters.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            ),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    stringResource(id = R.string.loading_cpu_data),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    } else if (clusters.isNotEmpty()) {
                        val totalClusters = clusters.size
                        clusters.forEachIndexed { index, clusterName ->
                            // Fungsi untuk menentukan bentuk rounded corner berdasarkan posisi
                            val clusterCardShape = when (totalClusters) {
                                1 -> RoundedCornerShape(24.dp) // If only one cluster, all corners same
                                else -> {
                                    when (index) {
                                        0 -> RoundedCornerShape( // First cluster: 24dp top, 8dp bottom
                                            topStart = 12.dp,
                                            topEnd = 12.dp,
                                            bottomStart = 4.dp,
                                            bottomEnd = 4.dp
                                        )
                                        totalClusters - 1 -> RoundedCornerShape( // Last cluster: 8dp top, 24dp bottom
                                            topStart = 4.dp,
                                            topEnd = 4.dp,
                                            bottomStart = 12.dp,
                                            bottomEnd = 12.dp
                                        )
                                        else -> RoundedCornerShape(4.dp) // Middle clusters: 8dp all sides
                                    }
                                }
                            }
                            
                            CpuClusterCard(
                                clusterName = clusterName,
                                vm = vm,
                                onGovernorClick = { showGovernorDialogForCluster = clusterName },
                                onMinFrequencyClick = { showMinFreqDialogForCluster = clusterName },
                                onMaxFrequencyClick = { showMaxFreqDialogForCluster = clusterName },
                                onCoreClick = { showCoreDialogForCluster = clusterName },
                                shape = clusterCardShape
                            )
                        }
                    }
                }
            }
        }
    }

    if (showGovernorDialogForCluster != null) {
        GovernorSelectionDialog(
            clusterName = showGovernorDialogForCluster!!,
            availableGovernors = availableGovernors,
            currentSelectedGovernor = vm.getCpuGov(showGovernorDialogForCluster!!).collectAsState().value,
            onGovernorSelected = { selectedGov ->
                vm.setCpuGov(showGovernorDialogForCluster!!, selectedGov)
                showGovernorDialogForCluster = null
            },
            onDismiss = { showGovernorDialogForCluster = null }
        )
    }

    if (showMinFreqDialogForCluster != null) {
        val currentFreqPair by vm.getCpuFreq(showMinFreqDialogForCluster!!).collectAsState()
        val availableFrequencies by vm.getAvailableCpuFrequencies(showMinFreqDialogForCluster!!).collectAsState()

        MinFrequencySelectionDialog(
            clusterName = showMinFreqDialogForCluster!!,
            currentMinFreq = currentFreqPair.first,
            allAvailableFrequencies = availableFrequencies,
            onMinFrequencySelected = { minFreq ->
                val currentMaxFreq = currentFreqPair.second
                vm.setCpuFreq(showMinFreqDialogForCluster!!, minFreq, currentMaxFreq)
                showMinFreqDialogForCluster = null
            },
            onDismiss = { showMinFreqDialogForCluster = null }
        )
    }

    if (showMaxFreqDialogForCluster != null) {
        val currentFreqPair by vm.getCpuFreq(showMaxFreqDialogForCluster!!).collectAsState()
        val availableFrequencies by vm.getAvailableCpuFrequencies(showMaxFreqDialogForCluster!!).collectAsState()

        MaxFrequencySelectionDialog(
            clusterName = showMaxFreqDialogForCluster!!,
            currentMaxFreq = currentFreqPair.second,
            allAvailableFrequencies = availableFrequencies,
            onMaxFrequencySelected = { maxFreq ->
                val currentMinFreq = currentFreqPair.first
                vm.setCpuFreq(showMaxFreqDialogForCluster!!, currentMinFreq, maxFreq)
                showMaxFreqDialogForCluster = null
            },
            onDismiss = { showMaxFreqDialogForCluster = null }
        )
    }

    if (showCoreDialogForCluster != null) {
        CoreStatusDialog(
            clusterName = showCoreDialogForCluster!!,
            coreStates = coreStates,
            onCoreToggled = { coreId ->
                vm.toggleCore(coreId)
            },
            onDismiss = { showCoreDialogForCluster = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GovernorSelectionDialog(
    clusterName: String,
    availableGovernors: List<String>,
    currentSelectedGovernor: String,
    onGovernorSelected: (String) -> Unit,
    onDismiss: () -> Unit,
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
                                contentDescription = stringResource(id = R.string.cpu_governor_label),
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = stringResource(id = R.string.set_governor_for_cluster, clusterName.replaceFirstChar { it.titlecase() }),
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Options List
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val sortedGovernors = availableGovernors.sorted()
                        itemsIndexed(sortedGovernors) { index, governor ->
                            val isSelected = governor == currentSelectedGovernor
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
private fun MinFrequencySelectionDialog(
    clusterName: String,
    currentMinFreq: Int,
    allAvailableFrequencies: List<Int>,
    onMinFrequencySelected: (min: Int) -> Unit,
    onDismiss: () -> Unit,
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
                                contentDescription = stringResource(id = R.string.frequency),
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(id = R.string.set_min_frequency),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = clusterName.replaceFirstChar { it.titlecase() },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Options List
                    if (allAvailableFrequencies.isEmpty()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 350.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            val sortedFrequencies = allAvailableFrequencies.sorted()
                            itemsIndexed(sortedFrequencies) { index, frequency ->
                                val isSelected = frequency == currentMinFreq
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = getDialogListItemShape(index, sortedFrequencies.size),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                                    ),
                                    onClick = { onMinFrequencySelected(frequency) }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { onMinFrequencySelected(frequency) },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = MaterialTheme.colorScheme.primary,
                                                unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        )
                                        Text(
                                            text = stringResource(id = R.string.cpu_freq_mhz, frequency / 1000),
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
private fun MaxFrequencySelectionDialog(
    clusterName: String,
    currentMaxFreq: Int,
    allAvailableFrequencies: List<Int>,
    onMaxFrequencySelected: (max: Int) -> Unit,
    onDismiss: () -> Unit,
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
                                contentDescription = stringResource(id = R.string.frequency),
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(id = R.string.set_max_frequency),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = clusterName.replaceFirstChar { it.titlecase() },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Options List
                    if (allAvailableFrequencies.isEmpty()) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    } else {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 350.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            val sortedFrequencies = allAvailableFrequencies.sorted()
                            itemsIndexed(sortedFrequencies) { index, frequency ->
                                val isSelected = frequency == currentMaxFreq
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = getDialogListItemShape(index, sortedFrequencies.size),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                                    ),
                                    onClick = { onMaxFrequencySelected(frequency) }
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { onMaxFrequencySelected(frequency) },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = MaterialTheme.colorScheme.primary,
                                                unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                            )
                                        )
                                        Text(
                                            text = stringResource(id = R.string.cpu_freq_mhz, frequency / 1000),
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
private fun CoreStatusDialog(
    clusterName: String,
    coreStates: List<Boolean>,
    onCoreToggled: (Int) -> Unit,
    onDismiss: () -> Unit,
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
                                imageVector = Icons.Default.Memory,
                                contentDescription = stringResource(id = R.string.core_status),
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(id = R.string.core_status),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = clusterName.replaceFirstChar { it.titlecase() },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Options List
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        itemsIndexed(coreStates) { index, isOnline ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = getDialogListItemShape(index, coreStates.size),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isOnline) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer
                                ),
                                onClick = { onCoreToggled(index) }
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.core_x, index),
                                        style = MaterialTheme.typography.bodyLarge.copy(
                                            fontWeight = FontWeight.Normal
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Switch(
                                        checked = isOnline,
                                        onCheckedChange = { onCoreToggled(index) },
                                        thumbContent = if (isOnline) {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                                )
                                            }
                                        } else {
                                            {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                                )
                                            }
                                        }
                                    )
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

private fun findClosestFrequency(target: Int, availableFrequencies: List<Int>): Int {
    if (availableFrequencies.isEmpty()) return target.coerceAtLeast(0)
    if (target in availableFrequencies) return target
    return availableFrequencies.minByOrNull { abs(it - target) } ?: target.coerceAtLeast(0)
}

@Composable
fun CpuClusterCard(
    clusterName: String,
    vm: TuningViewModel,
    onGovernorClick: () -> Unit,
    onMinFrequencyClick: () -> Unit,
    onMaxFrequencyClick: () -> Unit,
    onCoreClick: () -> Unit,
    shape: RoundedCornerShape
) {
    val currentGovernor by vm.getCpuGov(clusterName).collectAsState()
    val currentFreqPair by vm.getCpuFreq(clusterName).collectAsState()
    val availableFrequenciesForCluster by vm.getAvailableCpuFrequencies(clusterName).collectAsState()
    val coreStates by vm.coreStates.collectAsState()
    
    // Map cluster identifiers to display names
    val displayClusterName = when (clusterName) {
        "cpu0" -> stringResource(id = R.string.little_cluster)
        "cpu4" -> stringResource(id = R.string.big_cluster)
        "cpu7" -> stringResource(id = R.string.prime_cluster)
        else -> clusterName.uppercase()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Enhanced Header with cluster-specific styling
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Cluster icon with themed background
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Memory,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Column {
                        Text(
                            text = displayClusterName.uppercase(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(id = R.string.cluster_control),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Status indicator
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                ) {
                    Text(
                        text = if (currentGovernor != "..." && currentGovernor != "Error") stringResource(id = R.string.active) else stringResource(id = R.string.loading),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (currentGovernor != "..." && currentGovernor != "Error")
                            MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            // Enhanced Control Sections with custom rounded corners
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Governor Section (first card - 12dp top, 4dp bottom)
                ControlSection(
                    icon = Icons.Default.Tune,
                    title = stringResource(id = R.string.cpu_governor_label),
                    value = if (currentGovernor == "..." || currentGovernor == "Error") currentGovernor else currentGovernor,
                    isLoading = currentGovernor == "..." || currentGovernor == "Error",
                    themeColor = MaterialTheme.colorScheme.primary,
                    onClick = onGovernorClick,
                    enabled = currentGovernor != "..." && currentGovernor != "Error",
                    cornerShape = RoundedCornerShape(
                        topStart = 12.dp,
                        topEnd = 12.dp,
                        bottomStart = 4.dp,
                        bottomEnd = 4.dp
                    )
                )

                // Min Frequency Section (middle card - 8dp all sides)
                val minFreqText = when {
                    currentGovernor == "..." -> stringResource(id = R.string.loading_ellipsis)
                    currentGovernor == "Error" -> stringResource(id = R.string.error)
                    currentFreqPair.first > 0 -> stringResource(id = R.string.cpu_freq_mhz, currentFreqPair.first / 1000)
                    currentFreqPair.first == -1 -> stringResource(id = R.string.error)
                    else -> stringResource(id = R.string.loading_ellipsis)
                }

                ControlSection(
                    icon = Icons.Default.Speed,
                    title = stringResource(id = R.string.min_frequency),
                    value = minFreqText,
                    isLoading = minFreqText == stringResource(id = R.string.loading_ellipsis),
                    themeColor = MaterialTheme.colorScheme.primary,
                    onClick = onMinFrequencyClick,
                    enabled = availableFrequenciesForCluster.isNotEmpty(),
                    cornerShape = RoundedCornerShape(4.dp)
                )

                // Max Frequency Section (middle card - 8dp all sides)
                val maxFreqText = when {
                    currentGovernor == "..." -> stringResource(id = R.string.loading_ellipsis)
                    currentGovernor == "Error" -> stringResource(id = R.string.error)
                    currentFreqPair.second > 0 -> stringResource(id = R.string.cpu_freq_mhz, currentFreqPair.second / 1000)
                    currentFreqPair.second == -1 -> stringResource(id = R.string.error)
                    else -> stringResource(id = R.string.loading_ellipsis)
                }

                ControlSection(
                    icon = Icons.Default.Speed,
                    title = stringResource(id = R.string.max_frequency),
                    value = maxFreqText,
                    isLoading = maxFreqText == stringResource(id = R.string.loading_ellipsis),
                    themeColor = MaterialTheme.colorScheme.primary,
                    onClick = onMaxFrequencyClick,
                    enabled = availableFrequenciesForCluster.isNotEmpty(),
                    cornerShape = RoundedCornerShape(4.dp)
                )

                // Core Status Section (last card - 8dp top, 12dp bottom)
                ControlSection(
                    icon = Icons.Default.Memory,
                    title = stringResource(id = R.string.core_status),
                    value = stringResource(id = R.string.cores_online, coreStates.count { it }, coreStates.size),
                    isLoading = false,
                    themeColor = MaterialTheme.colorScheme.primary,
                    onClick = onCoreClick,
                    enabled = true,
                    cornerShape = RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 4.dp,
                        bottomStart = 12.dp,
                        bottomEnd = 12.dp
                    )
                )
            }
        }
    }
}

@Composable
private fun ControlSection(
    icon: ImageVector,
    title: String,
    value: String,
    isLoading: Boolean,
    themeColor: Color,
    onClick: () -> Unit,
    enabled: Boolean,
    cornerShape: RoundedCornerShape = RoundedCornerShape(24.dp)
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { onClick() },
        shape = cornerShape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon with themed background
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        color = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                if (isLoading) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = value,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = if (enabled) themeColor else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Arrow indicator
            if (enabled) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(id = R.string.expand),
                    tint = themeColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
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