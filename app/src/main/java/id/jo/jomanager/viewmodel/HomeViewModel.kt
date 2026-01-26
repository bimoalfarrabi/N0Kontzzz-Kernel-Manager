package id.jo.jomanager.viewmodel

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log // Import Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.jo.jomanager.data.model.MemoryInfo
import id.jo.jomanager.data.model.RealtimeCpuInfo
import id.jo.jomanager.data.model.RealtimeGpuInfo
import id.jo.jomanager.data.model.SystemInfo
import id.jo.jomanager.data.model.*
import id.jo.jomanager.data.repository.RootRepository
import id.jo.jomanager.data.repository.SystemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@HiltViewModel
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class HomeViewModel @Inject constructor(
    @field:ApplicationContext private val context: Context,
    private val systemRepo: SystemRepository,
    private val rootRepo: RootRepository
) : ViewModel() {


    private val _cpuInfo = MutableStateFlow(
        RealtimeCpuInfo(
            cores = 0,
            governor = "N/A",
            freqs = emptyList(),
            temp = 0f,
            soc = "N/A",
            cpuLoadPercentage = null
        )
    )
    val cpuInfo: StateFlow<RealtimeCpuInfo> = _cpuInfo.asStateFlow()

    private val _gpuInfo = MutableStateFlow(
        RealtimeGpuInfo(usagePercentage = null, currentFreq = 0, maxFreq = 0)
    )
    val gpuInfo: StateFlow<RealtimeGpuInfo> = _gpuInfo.asStateFlow()

    private val _batteryInfo = MutableStateFlow<BatteryInfo?>(null)
    val batteryInfo: StateFlow<BatteryInfo?> = _batteryInfo.asStateFlow()

    private val _memoryInfo = MutableStateFlow<MemoryInfo?>(null)
    val memoryInfo: StateFlow<MemoryInfo?> = _memoryInfo.asStateFlow()

    private val _deepSleep = MutableStateFlow<DeepSleepInfo?>(null)
    val deepSleep: StateFlow<DeepSleepInfo?> = _deepSleep.asStateFlow()


    private val _kernelInfo = MutableStateFlow<KernelInfo?>(null)
    val kernelInfo: StateFlow<KernelInfo?> = _kernelInfo.asStateFlow()

    private val _rootStatus = MutableStateFlow<Boolean?>(null)
    val rootStatus: StateFlow<Boolean?> = _rootStatus.asStateFlow()

    private val _appVersion = MutableStateFlow<String?>("N/A")
    val appVersion: StateFlow<String?> = _appVersion.asStateFlow()

    private val _systemInfo = MutableStateFlow<SystemInfo?>(null)
    val systemInfo: StateFlow<SystemInfo?> = _systemInfo.asStateFlow()

    private val _isTitleAnimationDone = MutableStateFlow(false)
    val isTitleAnimationDone: StateFlow<Boolean> = _isTitleAnimationDone.asStateFlow()

    fun onTitleAnimationFinished() {
        _isTitleAnimationDone.value = true
    }

    private val _cpuClusters = MutableStateFlow<ImmutableList<CpuCluster>?>(null)
    val cpuClusters: StateFlow<ImmutableList<CpuCluster>?> = _cpuClusters.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isScrolling = MutableStateFlow(false)

    // Graph Data State
    private val _graphData = MutableStateFlow(GraphData())
    val graphData: StateFlow<GraphData> = _graphData.asStateFlow()

    private val isInitialDataLoaded = java.util.concurrent.atomic.AtomicBoolean(false)

    init {
        // The realtime flow is relatively lightweight to start collecting.
        // It will begin emitting data as it becomes available.
        viewModelScope.launch(Dispatchers.Default) {
            _isScrolling
                .flatMapLatest { isScrolling ->
                    if (isScrolling) {
                        emptyFlow()
                    } else {
                        systemRepo.realtimeAggregatedInfoFlow
                            .catch { e ->
                                Log.e("HomeViewModel", "Error in realtimeAggregatedInfoFlow: ${e.message}", e)
                            }
                            .sample(500L)
                    }
                }
                .collect { aggregatedInfo ->
                    _cpuInfo.value = aggregatedInfo.cpuInfo
                    _gpuInfo.value = aggregatedInfo.gpuInfo
                    _batteryInfo.value = aggregatedInfo.batteryInfo
                    _memoryInfo.value = aggregatedInfo.memoryInfo
                    _deepSleep.value = DeepSleepInfo(
                        uptime = aggregatedInfo.uptimeMillis,
                        deepSleep = aggregatedInfo.deepSleepMillis
                    )

                    // Update Graph Data
                    val cpuInfo = aggregatedInfo.cpuInfo
                    val gpuInfo = aggregatedInfo.gpuInfo

                    // Calculate CPU Data Points
                    val avgSpeed = if (cpuInfo.freqs.isNotEmpty()) {
                        cpuInfo.freqs.filter { it > 0 }.map { it.toFloat() }.average().toFloat().takeIf { !it.isNaN() } ?: 0f
                    } else 0f
                    val cpuLoad = (cpuInfo.cpuLoadPercentage ?: 0f).coerceIn(0f, 100f)

                    // Calculate GPU Data Point
                    val gpuUsage = (gpuInfo.usagePercentage ?: 0f).coerceIn(0f, 100f)

                    // Atomically update graph history
                    val currentGraph = _graphData.value
                    _graphData.value = currentGraph.copy(
                        cpuLoadHistory = (currentGraph.cpuLoadHistory + cpuLoad).takeLast(50).toImmutableList(),
                        cpuSpeedHistory = (currentGraph.cpuSpeedHistory + avgSpeed).takeLast(50).toImmutableList(),
                        gpuHistory = (currentGraph.gpuHistory + gpuUsage).takeLast(50).toImmutableList()
                    )
                }
        }
    }

    fun setScrolling(isScrolling: Boolean) {
        _isScrolling.value = isScrolling
    }

    fun setCPUGraphMode(mode: GraphMode) {
        val current = _graphData.value
        _graphData.value = current.copy(cpuGraphMode = mode)
    }

    fun loadInitialData() {
        if (isInitialDataLoaded.getAndSet(true)) return

        _isLoading.value = true
        viewModelScope.launch(Dispatchers.IO) {
            val systemInfoDeferred = async { systemRepo.getSystemInfo() }
            val kernelInfoDeferred = async { systemRepo.getKernelInfo() }
            val rootStatusDeferred = async { rootRepo.isRooted() }
            val cpuClustersDeferred = async { systemRepo.getCpuClusters() }
            val appVersionDeferred = async {
                try {
                    @SuppressLint("PackageManagerGetSignatures")
                    val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    pInfo.versionName
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Error getting app version", e)
                    "N/A"
                }
            }

            _systemInfo.value = systemInfoDeferred.await()
            _kernelInfo.value = kernelInfoDeferred.await()
            _rootStatus.value = rootStatusDeferred.await()
            _cpuClusters.value = cpuClustersDeferred.await().toImmutableList()
            _appVersion.value = appVersionDeferred.await()
            _isLoading.value = false
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d("HomeViewModel", "onCleared called.")
    }

    
}