package id.jo.jomanager.ui.dialog

import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tune // Added for header icon
import androidx.compose.material3.BasicAlertDialog // Changed from AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api // Added for BasicAlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton // Changed from TextButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource // Added for string resources
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties // Added for BasicAlertDialog
import id.jo.jomanager.R

@OptIn(ExperimentalMaterial3Api::class) // Added opt-in
@Composable
fun BatteryHistoryConfigDialog(
    onDismiss: () -> Unit,
    title: String = stringResource(R.string.battery_history_config_title),
    description: String = stringResource(R.string.battery_history_config_desc),
    resetOnReboot: Boolean,
    onResetOnRebootChange: (Boolean) -> Unit,
    resetOnCharging: Boolean,
    onResetOnChargingChange: (Boolean) -> Unit,
    resetAtLevel: Boolean,
    onResetAtLevelChange: (Boolean) -> Unit,
    targetLevel: Int,
    onTargetLevelChange: (Int) -> Unit
) {
    val view = LocalView.current
    BasicAlertDialog( // Changed from AlertDialog
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        content = { // Explicit content lambda start
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
                                    imageVector = Icons.Default.Tune, // Using Tune icon for settings
                                    contentDescription = title,
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

                        // Content
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { // Reduced spacing
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                // Reset on Reboot
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 8.dp, bottomEnd = 8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.battery_history_config_device_reboot),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Switch(
                                            checked = resetOnReboot,
                                            onCheckedChange = onResetOnRebootChange,
                                            thumbContent = if (resetOnReboot) {
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
    
                                // Reset on Charging
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(R.string.battery_history_config_charger_connected),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Switch(
                                            checked = resetOnCharging,
                                            onCheckedChange = onResetOnChargingChange,
                                            thumbContent = if (resetOnCharging) {
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
    
                                // Reset at Level
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 24.dp, bottomEnd = 24.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = stringResource(R.string.battery_history_config_battery_level, targetLevel),
                                                modifier = Modifier.weight(1f)
                                            )
                                            Switch(
                                                checked = resetAtLevel,
                                                onCheckedChange = onResetAtLevelChange,
                                                thumbContent = if (resetAtLevel) {
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
    
                                        if (resetAtLevel) {
                                            Slider(
                                                value = targetLevel.toFloat(),
                                                onValueChange = { 
                                                    onTargetLevelChange(it.toInt())
                                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                                },
                                                valueRange = 50f..100f,
                                                steps = 49,
                                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center // Centering the button in the row
                        ) {
                            OutlinedButton(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth() // Button fills the width
                            ) {
                                Text(stringResource(R.string.battery_history_config_done))
                            }
                        }
                    }
                }
            }
        } // Explicit content lambda end
    ) // BasicAlertDialog end
} // Composable function end

