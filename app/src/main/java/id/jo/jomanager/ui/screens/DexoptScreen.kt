package id.jo.jomanager.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import id.jo.jomanager.R
import id.jo.jomanager.viewmodel.DexoptViewModel

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DexoptScreen(
    navController: NavController,
    viewModel: DexoptViewModel = hiltViewModel()
) {
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val isFinished by viewModel.isFinished.collectAsStateWithLifecycle()
    val recentLogs by viewModel.recentLogs.collectAsStateWithLifecycle()
    
    var showConfirmDialog by remember { mutableStateOf(false) }

    if (showConfirmDialog) {
        BasicAlertDialog(
            onDismissRequest = { showConfirmDialog = false },
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
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow)) + 
                                scaleIn(initialScale = 0.95f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)),
                        exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) + 
                               scaleOut(targetScale = 0.95f, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
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
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = stringResource(R.string.dexopt_run_dexopt),
                                            modifier = Modifier.size(28.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Text(
                                        text = stringResource(R.string.dexopt_confirm_title),
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                // Content
                                Text(
                                    text = stringResource(R.string.dexopt_confirm_content),
                                    style = MaterialTheme.typography.bodyMedium
                                )

                                // Actions
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { showConfirmDialog = false },
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(stringResource(R.string.cancel))
                                    }
                                    Button(
                                        onClick = {
                                            showConfirmDialog = false
                                            viewModel.runDexopt()
                                        },
                                        shape = RoundedCornerShape(16.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(stringResource(R.string.dexopt_start))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    Scaffold(
        floatingActionButton = {
            if (!isRunning) {
                ExtendedFloatingActionButton(
                    onClick = { showConfirmDialog = true },
                    icon = { Icon(Icons.Default.PlayArrow, stringResource(R.string.dexopt_run_dexopt)) },
                    text = { Text(stringResource(R.string.dexopt_run_dexopt)) }
                )
            }
        },
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = paddingValues.calculateBottomPadding()),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.dexopt_about_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.dexopt_about_content),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            if (isRunning || isFinished) {
                item {
                    if (isRunning) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    strokeWidth = 3.dp
                                )
                                Text(
                                    text = stringResource(R.string.dexopt_running),
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    } else if (isFinished) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.dexopt_finished),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                item {
                    LogPreview(logs = recentLogs)
                }
            }
        }
    }
}

@Composable
fun LogPreview(logs: List<String>) {
    // Intelligent deduplication:
    // 1. Filter out consecutive duplicates (so "Job running..." appears once, not 10x in a row)
    // 2. Take the last 50 logs to keep UI performant
    val filteredLogs = remember(logs) {
        val uniqueList = mutableListOf<String>()
        if (logs.isNotEmpty()) {
            uniqueList.add(logs[0])
            for (i in 1 until logs.size) {
                if (logs[i] != logs[i - 1]) {
                    uniqueList.add(logs[i])
                }
            }
        }
        uniqueList.takeLast(50)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.dexopt_last_logs),
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
            
            // Display filtered logs
            filteredLogs.forEach { log ->
                Text(
                    text = log,
                    color = Color(0xFF00FF00),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}