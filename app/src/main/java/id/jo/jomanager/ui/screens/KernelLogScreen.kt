package id.jo.jomanager.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import id.jo.jomanager.viewmodel.KernelLogViewModel
import androidx.compose.ui.res.stringResource
import id.jo.jomanager.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KernelLogScreen(
    navController: NavController,
    viewModel: KernelLogViewModel = hiltViewModel()
) {
    val logContent by viewModel.logContent.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val isPaused by viewModel.isPaused.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val isSearchVisible by viewModel.isSearchVisible.collectAsStateWithLifecycle()
    
    val listState = rememberLazyListState()
    var isInitialLoad by remember { mutableStateOf(true) }
    
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val logs = viewModel.getRawLogs()
                    context.contentResolver.openOutputStream(it)?.use { outputStream ->
                        outputStream.write(logs.toByteArray())
                    }
                    Toast.makeText(context, "Log saved successfully", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Failed to save log: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    LaunchedEffect(Unit) {
        viewModel.exportTrigger.collect {
            exportLauncher.launch("kernel_log_${System.currentTimeMillis()}.log")
        }
    }

    DisposableEffect(Unit) {
        viewModel.startMonitoring()
        onDispose {
            viewModel.stopMonitoring()
        }
    }
    
    // Auto-scroll to bottom when logs are loaded
    LaunchedEffect(logContent) {
        if (logContent.isNotEmpty()) {
            if (isInitialLoad) {
                listState.scrollToItem(logContent.size - 1)
                isInitialLoad = false
            } else if (!isPaused && !isSearchVisible) {
                val layoutInfo = listState.layoutInfo
                val totalItems = layoutInfo.totalItemsCount
                val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                
                // If we are near the bottom, scroll to the new bottom
                if (lastVisibleItemIndex >= totalItems - 5) {
                    listState.animateScrollToItem(logContent.size - 1)
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
             if (isSearchVisible) {
                TextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    placeholder = { Text("Search logs...") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
            
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (isLoading && logContent.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (error != null) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = stringResource(R.string.kernel_log_error_fetch, error ?: "Unknown"), color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.loadLogs() }) {
                            Text(stringResource(R.string.kernel_log_retry))
                        }
                    }
                } else {
                    if (logContent.isEmpty()) {
                         Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (searchQuery.isNotEmpty()) "No logs match your search" else stringResource(R.string.kernel_log_empty)
                            )
                         }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp)
                                .alpha(if (isInitialLoad) 0f else 1f),
                                contentPadding = PaddingValues(bottom = 100.dp)
                        ) {
                            items(logContent) { line ->
                                Text(
                                    text = line,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp
                                    ),
                                    modifier = Modifier.padding(vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
                
                // Overlay to mask the initial scroll jump
                if (logContent.isNotEmpty() && isInitialLoad) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
        
        // Show loading indicator overlay if refreshing but content exists
        if (isLoading && logContent.isNotEmpty()) {
             LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }

        // Scroll Navigation FABs
        if (logContent.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = { scope.launch { listState.animateScrollToItem(0) } },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ) {
                    Icon(Icons.Default.VerticalAlignTop, contentDescription = "Scroll to Top")
                }

                SmallFloatingActionButton(
                    onClick = { scope.launch { listState.animateScrollToItem(logContent.size - 1) } },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.VerticalAlignBottom, contentDescription = "Scroll to Bottom")
                }
            }
        }
    }
}
