package id.jo.jomanager.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.jo.jomanager.R
import id.jo.jomanager.data.model.BatteryInfo
import id.jo.jomanager.data.model.DeepSleepInfo
import id.jo.jomanager.data.model.MemoryInfo
import id.jo.jomanager.data.model.StorageInfo
import id.jo.jomanager.data.model.SystemInfo
import java.util.Locale
import kotlin.math.roundToLong
import kotlin.math.roundToInt

import androidx.compose.ui.res.stringResource

// Helper function to format time duration with seconds
private fun formatTimeWithSeconds(timeInMillis: Long): String {
    val totalSeconds = timeInMillis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
}

// Helper function to format storage size
@Composable
private fun formatStorageSize(bytes: Long): String {
    val tb = 1_000_000_000_000L
    val gb = 1_000_000_000L
    val mb = 1_000_000L
    val kb = 1_000L

    return when {
        bytes >= tb -> stringResource(id = R.string.storage_tb, bytes.toDouble() / tb)
        bytes >= gb -> stringResource(id = R.string.storage_gb, bytes.toDouble() / gb)
        bytes >= mb -> stringResource(id = R.string.storage_mb, bytes.toDouble() / mb)
        bytes >= kb -> stringResource(id = R.string.storage_kb, bytes.toDouble() / kb)
        else -> stringResource(id = R.string.storage_b, bytes)
    }
}

@Composable
fun MergedSystemCard(
    b: BatteryInfo,
    d: DeepSleepInfo,
    rooted: Boolean,
    version: String,
    mem: MemoryInfo,
    systemInfo: SystemInfo,
    storageInfo: StorageInfo,
    modifier: Modifier = Modifier
) {
    // Main container with separated cards
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        // Separate Battery Card
        BatteryCard(
            batteryInfo = b,
            deepSleepInfo = d,
        )

        // Separate Memory Card
        MemoryCard(
            memoryInfo = mem,
        )

        // Separate Storage Card
        StorageCard(
            storageInfo = storageInfo,
        )

        // Device Information Card
        DeviceInfoCard(
            systemInfo = systemInfo,
            rooted = rooted,
            version = version,
            storageInfo = storageInfo
        )
    }
}

@Composable
fun BatteryCard(
    batteryInfo: BatteryInfo,
    deepSleepInfo: DeepSleepInfo?,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp, 8.dp, 8.dp, 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Battery Header Section
            BatteryHeaderSection(batteryInfo = batteryInfo)

            // Battery Progress Section
            BatteryProgressSection(batteryInfo = batteryInfo)

            // Battery Stats Section
            BatteryStatsSection(batteryInfo = batteryInfo, deepSleepInfo = deepSleepInfo)
        }
    }
}

@Composable
fun MemoryCard(
    memoryInfo: MemoryInfo,
    modifier: Modifier = Modifier
) {
    val usedPercentage = ((memoryInfo.used.toDouble() / memoryInfo.total.toDouble()) * 100).roundToInt()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp, 8.dp, 8.dp, 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Memory Header Section
            MemoryHeaderSection(memoryInfo = memoryInfo, usedPercentage = usedPercentage)

            // Memory Progress Section
            MemoryProgressSection(memoryInfo = memoryInfo, usedPercentage = usedPercentage)

            // Memory Stats Section
            MemoryStatsSection(memoryInfo = memoryInfo)
        }
    }
}

@Composable
fun StorageCard(
    storageInfo: StorageInfo,
    modifier: Modifier = Modifier
) {
    val usedPercentage = ((storageInfo.usedSpace.toDouble() / storageInfo.totalSpace.toDouble()) * 100).roundToInt()

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp, 8.dp, 8.dp, 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Storage Header Section
            StorageHeaderSection(storageInfo = storageInfo, usedPercentage = usedPercentage)

            // Storage Progress Section
            StorageProgressSection(storageInfo = storageInfo)

            // Storage Stats Section
            StorageStatsSection(storageInfo = storageInfo)
        }
    }
}

@Composable
private fun BatteryHeaderSection(
    batteryInfo: BatteryInfo
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = R.string.battery_status),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Battery Status Box with temperature
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.battery_status_template, batteryInfo.level, batteryInfo.temp, if (batteryInfo.isCharging) stringResource(id = R.string.charging) else stringResource(id = R.string.discharging)),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
        }

        // Battery Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                imageVector = if (batteryInfo.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryFull,
                contentDescription = stringResource(id = R.string.battery_toggle),
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun MemoryHeaderSection(
    memoryInfo: MemoryInfo,
    usedPercentage: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = R.string.memory_status),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Memory Status Box
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                val totalGb = (memoryInfo.total.toDouble() / 1_000_000_000.0).roundToLong()
                val zramGb = (memoryInfo.zramTotal.toDouble() / 1_000_000_000.0).roundToLong()

                val memoryText = if (zramGb > 0) {
                    stringResource(id = R.string.memory_status_template_with_zram, usedPercentage, totalGb, zramGb)
                } else {
                    stringResource(id = R.string.memory_status_template, usedPercentage, totalGb)
                }

                Text(
                    text = memoryText,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
        }

        // Memory Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                painter = painterResource(id = R.drawable.memory_alt_24),
                contentDescription = stringResource(id = R.string.memory_status),
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun StorageHeaderSection(
    storageInfo: StorageInfo,
    usedPercentage: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = R.string.storage_status),
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Storage Status Box
            Box(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.storage_status_template, usedPercentage, formatStorageSize(storageInfo.totalSpace)),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                )
            }
        }

        // Storage Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                imageVector = Icons.Default.Storage,
                contentDescription = stringResource(id = R.string.storage_status),
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun BatteryProgressSection(
    batteryInfo: BatteryInfo
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.BatteryFull,
                    contentDescription = stringResource(id = R.string.battery_toggle),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(id = R.string.charge_level),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = stringResource(id = R.string.usage_percentage, batteryInfo.level),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.primary
            )
        }

        val progressColor = when {
            batteryInfo.level > 70 -> MaterialTheme.colorScheme.primary
            batteryInfo.level > 30 -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.error
        }
        LinearProgressIndicator(
            progress = { batteryInfo.level / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )
    }
}

@Composable
private fun MemoryProgressSection(
    memoryInfo: MemoryInfo,
    usedPercentage: Int
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // RAM Usage Progress Bar
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Memory,
                        contentDescription = stringResource(id = R.string.memory_status),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(id = R.string.ram_usage),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = stringResource(id = R.string.usage_percentage, usedPercentage),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            val progressColor = when {
                usedPercentage < 60 -> MaterialTheme.colorScheme.primary
                usedPercentage < 80 -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.error
            }
            LinearProgressIndicator(
                progress = { usedPercentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = progressColor,
                trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            )
        }

        // ZRAM Usage Progress Bar (only show if zram is available)
        if (memoryInfo.zramTotal > 0) {
            val zramUsedPercentage = ((memoryInfo.zramUsed.toDouble() / memoryInfo.zramTotal.toDouble()) * 100).roundToInt()

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Compress,
                            contentDescription = stringResource(id = R.string.zram),
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.zram_usage),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = stringResource(id = R.string.usage_percentage, zramUsedPercentage),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                LinearProgressIndicator(
                    progress = { zramUsedPercentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                )
            }
        }


    }
}

@Composable
private fun BatteryStatsSection(
    batteryInfo: BatteryInfo,
    deepSleepInfo: DeepSleepInfo?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Battery Stats Row 1 - Voltage and Uptime
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Voltage
                SystemStatItem(
                    icon = Icons.Default.ElectricBolt,
                    label = stringResource(id = R.string.voltage),
                    value = run {
                        val formattedVoltage = if (batteryInfo.voltage > 0) {
                            val voltageInVolts = when {
                                batteryInfo.voltage > 1000000 -> batteryInfo.voltage / 1000000f
                                batteryInfo.voltage > 1000 -> batteryInfo.voltage / 1000f
                                else -> batteryInfo.voltage
                            }
                            String.format(Locale.getDefault(), "%.2f", voltageInVolts).trimEnd('0').trimEnd('.')
                        } else "0"
                        stringResource(id = R.string.v, formattedVoltage)
                    },
                    modifier = Modifier.weight(1f)
                )

                // Uptime
                SystemStatItem(
                    icon = Icons.Default.AccessTime,
                    label = stringResource(id = R.string.uptime),
                    value = deepSleepInfo?.let { formatTimeWithSeconds(it.uptime) } ?: stringResource(id = R.string.common_na),
                    modifier = Modifier.weight(1f)
                )
            }

            // Battery Stats Row 2 - Health and Cycles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Battery Health
                SystemStatItem(
                    icon = Icons.Default.HealthAndSafety,
                    label = stringResource(id = R.string.health),
                    value = if (batteryInfo.healthPercentage > 0) stringResource(id = R.string.usage_percentage, batteryInfo.healthPercentage) else batteryInfo.health,
                    modifier = Modifier.weight(1f)
                )

                // Battery Cycles
                SystemStatItem(
                    icon = Icons.Default.Autorenew,
                    label = stringResource(id = R.string.cycles),
                    value = if (batteryInfo.cycleCount > 0) "${batteryInfo.cycleCount}" else stringResource(id = R.string.common_na),
                    modifier = Modifier.weight(1f)
                )
            }

            // Battery Stats Row 3 - Technology and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Battery Technology
                SystemStatItem(
                    icon = Icons.Default.Science,
                    label = stringResource(id = R.string.technology),
                    value = batteryInfo.technology,
                    modifier = Modifier.weight(1f)
                )

                // Battery Status
                SystemStatItem(
                    icon = if (batteryInfo.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                    label = stringResource(id = R.string.status),
                    value = if (batteryInfo.isCharging) stringResource(id = R.string.charging) else stringResource(id = R.string.discharging),
                    modifier = Modifier.weight(1f)
                )
            }

            // Battery Stats Row 4 - Current Capacity and Design Capacity (combined in one row)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Current Capacity
                SystemStatItem(
                    icon = Icons.Default.Battery6Bar,
                    label = stringResource(id = R.string.current_cap),
                    value = if (batteryInfo.currentCapacity > 0) stringResource(id = R.string.mah, batteryInfo.currentCapacity) else stringResource(id = R.string.common_na),
                    modifier = Modifier.weight(1f)
                )

                // Design Capacity (moved to same row to utilize space efficiently)
                SystemStatItem(
                    icon = Icons.Default.BatterySaver,
                    label = stringResource(id = R.string.design_cap),
                    value = if (batteryInfo.capacity > 0) stringResource(id = R.string.mah, batteryInfo.capacity) else stringResource(id = R.string.common_na),
                    modifier = Modifier.weight(1f)
                )
            }

            // Battery Stats Row 5 - Deep Sleep and Screen On Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Deep Sleep
                SystemStatItem(
                    icon = Icons.Default.NightsStay,
                    label = stringResource(id = R.string.deep_sleep),
                    value = deepSleepInfo?.let { 
                        if (it.uptime > 0) {
                            val percentage = (it.deepSleep.toFloat() / it.uptime.toFloat()) * 100
                            stringResource(id = R.string.deep_sleep_percentage, percentage)
                        } else stringResource(id = R.string.deep_sleep_default)
                    } ?: stringResource(id = R.string.common_na),
                    modifier = Modifier.weight(1f)
                )

                // Screen On Time
                SystemStatItem(
                    icon = Icons.Default.ScreenLockRotation,
                    label = stringResource(id = R.string.screen_on),
                    value = deepSleepInfo?.let { 
                        if (it.uptime > 0) {
                            val awakeTime = it.uptime - it.deepSleep  // Time the device has been awake
                            val awakeDurationFormatted = formatTimeWithSeconds(awakeTime)
                            awakeDurationFormatted
                        } else stringResource(id = R.string.common_na)
                    } ?: stringResource(id = R.string.common_na),
                    modifier = Modifier.weight(1f)
                )
            }

            // Battery Stats Row 6 - Current (always visible)
            val currentMa = batteryInfo.current / 1000f
            val currentDisplay = stringResource(id = R.string.ma, currentMa.toDouble())

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SystemStatItem(
                    icon = if (batteryInfo.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryAlert,
                    label = stringResource(id = R.string.current),
                    value = currentDisplay,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MemoryStatsSection(
    memoryInfo: MemoryInfo
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Memory Stats Row 1 - Used and Free RAM
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Used RAM
                SystemStatItem(
                    icon = Icons.Default.Memory,
                    label = stringResource(id = R.string.used_ram),
                    value = stringResource(id = R.string.mb, memoryInfo.used / (1024 * 1024)),
                    modifier = Modifier.weight(1f)
                )

                // Free RAM
                SystemStatItem(
                    icon = Icons.Default.Storage,
                    label = stringResource(id = R.string.free_ram),
                    value = stringResource(id = R.string.mb, memoryInfo.free / (1024 * 1024)),
                    modifier = Modifier.weight(1f)
                )
            }

            // Memory Stats Row 2 - Total RAM and Usage Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Total RAM
                SystemStatItem(
                    icon = Icons.Default.Widgets,
                    label = stringResource(id = R.string.total_ram),
                    value = stringResource(id = R.string.mb, memoryInfo.total / (1024 * 1024)),
                    modifier = Modifier.weight(1f)
                )

                // Usage Percentage
                SystemStatItem(
                    icon = Icons.Default.Analytics,
                    label = stringResource(id = R.string.usage_percentage_label),
                    value = stringResource(id = R.string.usage_percentage, ((memoryInfo.used.toDouble() / memoryInfo.total.toDouble()) * 100).roundToInt()),
                    modifier = Modifier.weight(1f)
                )
            }

            // Memory Stats Row 3 - ZRAM Stats (only show if zram is available)
            if (memoryInfo.zramTotal > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // ZRAM Used
                    SystemStatItem(
                        icon = Icons.Default.Compress,
                        label = stringResource(id = R.string.zram_used),
                        value = stringResource(id = R.string.mb, memoryInfo.zramUsed / (1024 * 1024)),
                        modifier = Modifier.weight(1f)
                    )

                    // ZRAM Total
                    SystemStatItem(
                        icon = Icons.Default.Compress,
                        label = stringResource(id = R.string.zram_total),
                        value = stringResource(id = R.string.mb, memoryInfo.zramTotal / (1024 * 1024)),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemStatItem(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun DeviceInfoCard(
    systemInfo: SystemInfo,
    rooted: Boolean,
    version: String,
    storageInfo: StorageInfo,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp, 8.dp, 8.dp, 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Device Info Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(id = R.string.device_information),
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Root Status Box
                        Box(
                            modifier = Modifier
                                .background(
                                    if (rooted) MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.errorContainer,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (rooted) stringResource(id = R.string.rooted) else stringResource(id = R.string.not_rooted),
                                color = if (rooted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                            )
                        }

                        // Version Box
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.version_template, version),
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }
                }

                // Device Icon
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                        imageVector = Icons.Rounded.Smartphone,
                        contentDescription = stringResource(id = R.string.device),
                        modifier = Modifier.size(28.dp),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            // Device Stats
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Device Info Row 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SystemStatItem(
                            icon = Icons.Default.PhoneAndroid,
                            label = stringResource(id = R.string.model),
                            value = systemInfo.model,
                            modifier = Modifier.weight(1f)
                        )

                        SystemStatItem(
                            icon = Icons.Default.Code,
                            label = stringResource(id = R.string.codename),
                            value = systemInfo.codename,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Device Info Row 2
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SystemStatItem(
                            icon = Icons.Default.Android,
                            label = stringResource(id = R.string.android),
                            value = systemInfo.androidVersion,
                            modifier = Modifier.weight(1f)
                        )

                        SystemStatItem(
                            icon = Icons.Default.Build,
                            label = stringResource(id = R.string.sdk),
                            value = systemInfo.sdk.toString(),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Device Info Row 3 - SoC and Fingerprint
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SystemStatItem(
                            icon = Icons.Default.DeveloperBoard,
                            label = stringResource(id = R.string.cpu_soc_label),
                            value = systemInfo.soc,
                            modifier = Modifier.weight(1f)
                        )

                        SystemStatItem(
                            icon = Icons.Default.Fingerprint,
                            label = stringResource(id = R.string.fingerprint),
                            value = systemInfo.fingerprint.substringAfterLast("/").substringBefore(":"),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Device Info Row 4 - Display Resolution and Technology
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SystemStatItem(
                            icon = Icons.Default.AspectRatio,
                            label = stringResource(id = R.string.resolution),
                            value = systemInfo.screenResolution,
                            modifier = Modifier.weight(1f)
                        )

                        SystemStatItem(
                            icon = Icons.Default.DisplaySettings,
                            label = stringResource(id = R.string.technology),
                            value = systemInfo.displayTechnology,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Device Info Row 5 - Refresh Rate and DPI
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        SystemStatItem(
                            icon = Icons.Default.Speed,
                            label = stringResource(id = R.string.refresh_rate),
                            value = systemInfo.refreshRate,
                            modifier = Modifier.weight(1f)
                        )

                        SystemStatItem(
                            icon = Icons.Default.PhotoSizeSelectSmall,
                            label = stringResource(id = R.string.dpi),
                            value = systemInfo.screenDpi,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StorageProgressSection(
    storageInfo: StorageInfo
) {
    val usedPercentage = if (storageInfo.totalSpace > 0) {
        ((storageInfo.usedSpace.toDouble() / storageInfo.totalSpace.toDouble()) * 100).roundToInt()
    } else 0

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Storage,
                    contentDescription = stringResource(id = R.string.internal_storage),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(id = R.string.internal_storage),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = stringResource(id = R.string.storage_usage_template, formatStorageSize(storageInfo.usedSpace), formatStorageSize(storageInfo.totalSpace)),
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.secondary
            )
        }

        val progressColor = when {
            usedPercentage < 70 -> MaterialTheme.colorScheme.primary
            usedPercentage < 85 -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.error
        }
        LinearProgressIndicator(
            progress = { usedPercentage / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = progressColor,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        )

        // Storage Details
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(id = R.string.used_template, formatStorageSize(storageInfo.usedSpace)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(id = R.string.free_template, formatStorageSize(storageInfo.totalSpace - storageInfo.usedSpace)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun StorageStatsSection(
    storageInfo: StorageInfo
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Storage Stats Row 1 - Used and Free
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Used Storage
                SystemStatItem(
                    icon = Icons.Default.Storage,
                    label = stringResource(id = R.string.used_storage),
                    value = formatStorageSize(storageInfo.usedSpace),
                    modifier = Modifier.weight(1f)
                )

                // Free Storage
                SystemStatItem(
                    icon = Icons.Default.Storage,
                    label = stringResource(id = R.string.free_storage),
                    value = formatStorageSize(storageInfo.totalSpace - storageInfo.usedSpace),
                    modifier = Modifier.weight(1f)
                )
            }

            // Storage Stats Row 2 - Total and Usage Percentage
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Total Storage
                SystemStatItem(
                    icon = Icons.Default.Storage,
                    label = stringResource(id = R.string.total_storage),
                    value = formatStorageSize(storageInfo.totalSpace),
                    modifier = Modifier.weight(1f)
                )

                // Usage Percentage
                SystemStatItem(
                    icon = Icons.Default.Analytics,
                    label = stringResource(id = R.string.usage_percentage_label),
                    value = stringResource(id = R.string.usage_percentage, ((storageInfo.usedSpace.toDouble() / storageInfo.totalSpace.toDouble()) * 100).roundToInt()),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}