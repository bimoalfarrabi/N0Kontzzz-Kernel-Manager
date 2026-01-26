package id.jo.jomanager.ui.dialog

import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import id.jo.jomanager.R
import id.jo.jomanager.data.model.BatteryInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChargingControlDialog(
    onDismiss: () -> Unit,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    stopLevel: Int,
    onStopLevelChange: (Int) -> Unit,
    resumeLevel: Int,
    onResumeLevelChange: (Int) -> Unit,
    batteryInfo: BatteryInfo?,
    isBypassActive: Boolean
) {
    val view = LocalView.current
    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        content = {
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
                                    imageVector = Icons.Default.BatteryChargingFull,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Text(
                                text = stringResource(R.string.charging_control_title),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Content
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            
                            // Enable Switch
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = if (enabled) RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp) else RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = stringResource(R.string.enable_charging_control),
                                        modifier = Modifier.weight(1f)
                                    )
                                    Switch(
                                        checked = enabled,
                                        onCheckedChange = onEnabledChange,
                                        thumbContent = if (enabled) {
                                            { Icon(Icons.Default.Check, null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                                        } else {
                                            { Icon(Icons.Default.Close, null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                                        }
                                    )
                                }
                            }

                            if (enabled) {
                                // Stop Level
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp, 8.dp)
                                    ) {
                                        Text(stringResource(R.string.stop_charging_at, stopLevel))
                                        Slider(
                                            value = stopLevel.toFloat(),
                                            onValueChange = { 
                                                onStopLevelChange(it.toInt()) 
                                                view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                            },
                                            valueRange = 50f..95f,
                                            steps = 44,
                                        )
                                    }
                                }

                                // Resume Level
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp, 8.dp)
                                    ) {
                                        Text(stringResource(R.string.resume_charging_at, resumeLevel))
                                        Slider(
                                            value = resumeLevel.toFloat(),
                                            onValueChange = { 
                                                // Ensure resume level is always lower than stop level
                                                if (it < stopLevel) {
                                                    onResumeLevelChange(it.toInt())
                                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                }
                                            },
                                            valueRange = 40f..85f,
                                            steps = 44
                                        )
                                    }
                                }

                                // Status
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        val level = batteryInfo?.level ?: 0
                                        val isCharging = batteryInfo?.isCharging == true
                                        // Status logic:
                                        // STOPPED = Bypass is active (input_suspend=1)
                                        // LIMIT REACHED = STOPPED & level >= stopLevel
                                        // CHARGING = Plugged & Not Stopped
                                        // IDLE/DISCHARGING = Not Plugged
                                        val statusText = when {
                                            level >= stopLevel && isBypassActive -> stringResource(R.string.status_limit_reached)
                                            isBypassActive -> stringResource(R.string.status_stopped)
                                            isCharging -> stringResource(R.string.status_charging)
                                            else -> stringResource(R.string.status_idle) 
                                        }
                                        Text(stringResource(R.string.charging_status, statusText))
                                    }
                                }
                            }
                        }

                        // Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(stringResource(R.string.battery_history_config_done))
                            }
                        }
                    }
                }
            }
        }
    )
}
