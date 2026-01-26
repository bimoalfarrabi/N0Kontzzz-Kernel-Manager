package id.jo.jomanager.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import id.jo.jomanager.data.model.CpuCluster

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TuningClusterCard(
    cluster: CpuCluster,
    onGovSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(cluster.name, style = MaterialTheme.typography.titleMedium)
            Text("Current: ${cluster.governor}")

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    readOnly = true,
                    value = cluster.governor,
                    onValueChange = {},
                    label = { Text("Governor") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    cluster.availableGovernors.forEach { gov ->
                        DropdownMenuItem(
                            text = { Text(gov) },
                            onClick = {
                                onGovSelected(gov)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}