package id.jo.jomanager.ui.screens

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import id.jo.jomanager.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import id.jo.jomanager.ui.components.AboutCard
import id.jo.jomanager.ui.components.CpuCard
import id.jo.jomanager.ui.components.GpuCard
import id.jo.jomanager.ui.components.IndeterminateExpressiveLoadingIndicator
import id.jo.jomanager.ui.components.KernelCard
import id.jo.jomanager.ui.components.BatteryCard
import id.jo.jomanager.ui.components.DeviceInfoCard
import id.jo.jomanager.ui.components.MemoryCard
import id.jo.jomanager.ui.components.StorageCard
import id.jo.jomanager.ui.viewmodel.StorageInfoViewModel
import id.jo.jomanager.viewmodel.HomeViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController
) {
    val vm: HomeViewModel = hiltViewModel()
    val storageViewModel: StorageInfoViewModel = hiltViewModel()

    // Trigger the one-time data load after a short delay to allow animations to finish
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(150)
        vm.loadInitialData()
    }

    // Kumpulkan semua state dari ViewModel
    val cpuInfo by vm.cpuInfo.collectAsState()
    val gpuInfo by vm.gpuInfo.collectAsState()
    val batteryInfo by vm.batteryInfo.collectAsState()
    val memoryInfo by vm.memoryInfo.collectAsState()
    val deepSleepInfo by vm.deepSleep.collectAsState()
    val rootStatus by vm.rootStatus.collectAsState()
    val kernelInfo by vm.kernelInfo.collectAsState()
    val appVersion by vm.appVersion.collectAsState()
    val systemInfoState by vm.systemInfo.collectAsState()
    val cpuClusters by vm.cpuClusters.collectAsState()
    val storageInfo by storageViewModel.storageInfo.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val graphData by vm.graphData.collectAsState()

    val lazyListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Listen for destination changes to reset scroll state
    DisposableEffect(navController) {
        val listener = NavController.OnDestinationChangedListener { _, destination, _ ->
            if (destination.route == "home") {
                coroutineScope.launch {
                    lazyListState.scrollToItem(0)
                }
            }
        }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    // Notify the ViewModel about the scroll state to pause data updates during scroll
    LaunchedEffect(lazyListState) {
        snapshotFlow { lazyListState.isScrollInProgress }
            .collect { isScrolling ->
                vm.setScrolling(isScrolling)
            }
    }

    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 100.dp), // Adjust padding to better center the indicator
            contentAlignment = Alignment.Center
        ) {
            IndeterminateExpressiveLoadingIndicator()
        }
    } else {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 0.dp,
                bottom = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            /* 1. CPU */
            item(key = "cpu_card") {
                val currentSystemInfo = systemInfoState
                val clusters = cpuClusters
                if (clusters != null) {
                    val socNameToDisplay = currentSystemInfo?.soc?.takeIf { it.isNotBlank() && it != stringResource(id = R.string.common_unknown_value) } ?: cpuInfo.soc.takeIf { it.isNotBlank() && it != stringResource(id = R.string.unknown_soc) && it != stringResource(id = R.string.common_na) } ?: stringResource(id = R.string.cpu_cpu_label)
                    val board = currentSystemInfo?.board ?: ""
                    val deviceCodename = currentSystemInfo?.codename ?: ""
                    
                    CpuCard(
                        soc = socNameToDisplay,
                        board = board,
                        deviceCodename = deviceCodename,
                        info = cpuInfo,
                        clusters = clusters,
                        graphData = graphData,
                        onGraphModeChange = vm::setCPUGraphMode,
                        modifier1 = false,
                        modifier = Modifier
                    )
                } else {
                    // Show a smaller placeholder if just this data is missing
                    Card(modifier = Modifier.fillMaxWidth().height(150.dp)) { /* Placeholder */ }
                }
            }

            /* 2. GPU */
            item(key = "gpu_card") {
                GpuCard(gpuInfo, graphData.gpuHistory, Modifier)
            }

            /* 3. System Stats */
            val currentBattery = batteryInfo
            val currentMemory = memoryInfo
            val currentDeepSleep = deepSleepInfo
            val currentRoot = rootStatus
            val currentVersion = appVersion
            val currentSystem = systemInfoState

            if (currentBattery != null && currentMemory != null && currentDeepSleep != null &&
                currentRoot != null && currentVersion != null && currentSystem != null) {
                
                item(key = "battery_card") {
                    BatteryCard(
                        batteryInfo = currentBattery,
                        deepSleepInfo = currentDeepSleep
                    )
                }
                
                item(key = "memory_card") {
                    MemoryCard(
                        memoryInfo = currentMemory
                    )
                }
                
                item(key = "storage_card") {
                    StorageCard(
                        storageInfo = storageInfo
                    )
                }
                
                item(key = "device_info_card") {
                    DeviceInfoCard(
                        systemInfo = currentSystem,
                        rooted = currentRoot,
                        version = currentVersion,
                        storageInfo = storageInfo
                    )
                }
            } else {
                item(key = "merged_placeholder") {
                    // Placeholder while data is loading
                    Card(modifier = Modifier.fillMaxWidth().height(200.dp)) { /* Placeholder */ }
                }
            }

            /* 4. Kernel */
            item(key = "kernel_card") {
                val currentKernel = kernelInfo
                if (currentKernel != null) {
                    KernelCard(currentKernel, Modifier)
                } else {
                    // Optional: Placeholder for KernelCard while data is loading
                    Card(modifier = Modifier.fillMaxWidth().height(100.dp)) { /* Placeholder */ }
                }
            }

            /* 5. About */
            item(key = "about_card") {
                AboutCard(false, Modifier)
            }
        }
    }
}