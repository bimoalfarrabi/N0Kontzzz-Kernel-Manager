package id.jo.jomanager.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.jo.jomanager.data.repository.SystemRepository
import id.jo.jomanager.data.repository.ThermalRepository
import id.jo.jomanager.data.repository.TuningRepository
import id.jo.jomanager.service.ThermalService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

import id.jo.jomanager.utils.PreferenceManager
import id.jo.jomanager.R

@HiltViewModel
class TuningViewModel @Inject constructor(
    private val application: Application,
    private val repo: TuningRepository,
    private val thermalRepo: ThermalRepository,
    private val systemRepo: SystemRepository,
    private val preferenceManager: PreferenceManager
) : AndroidViewModel(application) {

    private val thermalPrefs: SharedPreferences by lazy {
        application.getSharedPreferences("thermal_settings_prefs", Context.MODE_PRIVATE)
    }
    // performancePrefs removed
    private val KEY_LAST_APPLIED_THERMAL_INDEX = "last_applied_thermal_index"
    // KEY_LAST_APPLIED_PERFORMANCE_MODE removed

    val cpuClusters = listOf("cpu0", "cpu4", "cpu7")

    //<editor-fold desc="StateFlows">
    // Dynamic cluster information with proper names
    private val _dynamicCpuClusters = MutableStateFlow<List<String>>(emptyList())
    val dynamicCpuClusters: StateFlow<List<String>> = _dynamicCpuClusters.asStateFlow()

    // UI State for card expansion
    private val _expandedCards = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val expandedCards: StateFlow<Map<String, Boolean>> = _expandedCards.asStateFlow()

    fun toggleCardExpansion(cardId: String) {
        val current = _expandedCards.value.toMutableMap()
        current[cardId] = !(current[cardId] ?: false)
        _expandedCards.value = current
    }

    // Set on Boot StateFlows
    private val _applyPerformanceModeOnBoot = MutableStateFlow(preferenceManager.isApplyPerformanceModeOnBoot())
    val applyPerformanceModeOnBoot: StateFlow<Boolean> = _applyPerformanceModeOnBoot.asStateFlow()

    private val _applyCpuOnBoot = MutableStateFlow(preferenceManager.isApplyCpuOnBoot())
    val applyCpuOnBoot: StateFlow<Boolean> = _applyCpuOnBoot.asStateFlow()

    private val _applyGpuOnBoot = MutableStateFlow(preferenceManager.isApplyGpuOnBoot())
    val applyGpuOnBoot: StateFlow<Boolean> = _applyGpuOnBoot.asStateFlow()

    private val _applyThermalOnBoot = MutableStateFlow(preferenceManager.isApplyThermalOnBoot())
    val applyThermalOnBoot: StateFlow<Boolean> = _applyThermalOnBoot.asStateFlow()

    private val _applyRamOnBoot = MutableStateFlow(preferenceManager.isApplyRamOnBoot())
    val applyRamOnBoot: StateFlow<Boolean> = _applyRamOnBoot.asStateFlow()

    fun toggleApplyPerformanceModeOnBoot(enabled: Boolean) {
        preferenceManager.setApplyPerformanceModeOnBoot(enabled)
        _applyPerformanceModeOnBoot.value = enabled
    }

    fun toggleApplyCpuOnBoot(enabled: Boolean) {
        preferenceManager.setApplyCpuOnBoot(enabled)
        _applyCpuOnBoot.value = enabled
    }

    fun toggleApplyGpuOnBoot(enabled: Boolean) {
        preferenceManager.setApplyGpuOnBoot(enabled)
        _applyGpuOnBoot.value = enabled
    }

    fun toggleApplyThermalOnBoot(enabled: Boolean) {
        preferenceManager.setApplyThermalOnBoot(enabled)
        _applyThermalOnBoot.value = enabled
    }

    fun toggleApplyRamOnBoot(enabled: Boolean) {
        preferenceManager.setApplyRamOnBoot(enabled)
        _applyRamOnBoot.value = enabled
    }

    /* ---------------- CPU ---------------- */
    private val _performanceMode = MutableStateFlow(preferenceManager.getPerformanceMode())
    val performanceMode: StateFlow<String> = _performanceMode.asStateFlow()

    private val _coreStates = MutableStateFlow(List(8) { true })
    val coreStates: StateFlow<List<Boolean>> = _coreStates.asStateFlow()

    private val _generalAvailableCpuGovernors = MutableStateFlow<List<String>>(emptyList())
    val generalAvailableCpuGovernors: StateFlow<List<String>> = _generalAvailableCpuGovernors.asStateFlow()

    private val _availableCpuFrequenciesPerClusterMap = MutableStateFlow<Map<String, List<Int>>>(emptyMap())
    private val _currentCpuGovernors = mutableMapOf<String, MutableStateFlow<String>>()
    private val _currentCpuFrequencies = mutableMapOf<String, MutableStateFlow<Pair<Int, Int>>>()

    // Initialize these eagerly to ensure non-null flows for combine
    init {
        cpuClusters.forEach { cluster ->
            _currentCpuGovernors.getOrPut(cluster) { MutableStateFlow("...") }
            _currentCpuFrequencies.getOrPut(cluster) { MutableStateFlow(0 to 0) }
        }
    }

    // Logic to validate active performance mode based on real-time governor state
    val activePerformanceMode: StateFlow<String?> = combine(
        getCpuGov("cpu0"),
        getCpuGov("cpu4"),
        getCpuGov("cpu7")
    ) { gov0, gov4, gov7 ->
        // Check if governors are still loading
        if (gov0 == "..." || gov4 == "..." || gov7 == "...") return@combine null

        val governors = listOf(gov0, gov4, gov7)
        val uniqueGovs = governors.distinct()

        // Valid only if all clusters share the same governor
        if (uniqueGovs.size == 1) {
            when (uniqueGovs.first()) {
                "powersave" -> "Powersave"
                "schedutil" -> "Balanced"
                "performance" -> "Performance"
                else -> null // Default kernel governor or other custom governor -> No mode active
            }
        } else {
            null // Mixed governors (Manual per-cluster changes) -> No mode active
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /* ---------------- GPU ---------------- */
    private val _availableGpuGovernors = MutableStateFlow<List<String>>(emptyList())
    val availableGpuGovernors: StateFlow<List<String>> = _availableGpuGovernors.asStateFlow()

    private val _currentGpuGovernor = MutableStateFlow("...")
    val currentGpuGovernor: StateFlow<String> = _currentGpuGovernor.asStateFlow()

    private val _availableGpuFrequencies = MutableStateFlow<List<Int>>(emptyList())
    val availableGpuFrequencies: StateFlow<List<Int>> = _availableGpuFrequencies.asStateFlow()

    private val _currentGpuMinFreq = MutableStateFlow(0)
    val currentGpuMinFreq: StateFlow<Int> = _currentGpuMinFreq.asStateFlow()

    private val _currentGpuMaxFreq = MutableStateFlow(0)
    val currentGpuMaxFreq: StateFlow<Int> = _currentGpuMaxFreq.asStateFlow()

    private val _gpuPowerLevelRange = MutableStateFlow(0f to 5f)
    val gpuPowerLevelRange: StateFlow<Pair<Float, Float>> = _gpuPowerLevelRange.asStateFlow()

    private val _currentGpuPowerLevel = MutableStateFlow(0f)
    val currentGpuPowerLevel: StateFlow<Float> = _currentGpuPowerLevel.asStateFlow()

    private val _gpuThrottlingEnabled = MutableStateFlow(false)
    val gpuThrottlingEnabled: StateFlow<Boolean> = _gpuThrottlingEnabled.asStateFlow()

    /* ---------------- OpenGL / Vulkan / Renderer ---------------- */
    private val _currentOpenGlesDriver = MutableStateFlow("Loading...")
    val currentOpenGlesDriver: StateFlow<String> = _currentOpenGlesDriver.asStateFlow()

    private val _vulkanApiVersion = MutableStateFlow("Loading...")
    val vulkanApiVersion: StateFlow<String> = _vulkanApiVersion.asStateFlow()

    /* ---------------- Reboot dialog ---------------- */
    private val _showRebootConfirmationDialog = MutableStateFlow(false)
    val showRebootConfirmationDialog: StateFlow<Boolean> = _showRebootConfirmationDialog.asStateFlow()

    private val _rebootCommandFeedback = MutableSharedFlow<String>()
    val rebootCommandFeedback: SharedFlow<String> = _rebootCommandFeedback.asSharedFlow()

    /* ---------------- RAM Control ---------------- */
    private val _zramEnabled = MutableStateFlow(false)
    val zramEnabled: StateFlow<Boolean> = _zramEnabled.asStateFlow()

    private val _zramOperationInProgress = MutableStateFlow(false)
    val zramOperationInProgress: StateFlow<Boolean> = _zramOperationInProgress.asStateFlow()

    private val _zramDisksize = MutableStateFlow(536870912L) // 512 MB default
    val zramDisksize: StateFlow<Long> = _zramDisksize.asStateFlow()

    private val _compressionAlgorithms = MutableStateFlow<List<String>>(emptyList())
    val compressionAlgorithms: StateFlow<List<String>> = _compressionAlgorithms.asStateFlow()

    private val _currentCompression = MutableStateFlow("")
    val currentCompression: StateFlow<String> = _currentCompression.asStateFlow()

    private val _swappiness = MutableStateFlow(60)
    val swappiness: StateFlow<Int> = _swappiness.asStateFlow()

    private val _dirtyRatio = MutableStateFlow(20)
    val dirtyRatio: StateFlow<Int> = _dirtyRatio.asStateFlow()

    private val _dirtyBackgroundRatio = MutableStateFlow(10)
    val dirtyBackgroundRatio: StateFlow<Int> = _dirtyBackgroundRatio.asStateFlow()

    private val _dirtyWriteback = MutableStateFlow(30)
    val dirtyWriteback: StateFlow<Int> = _dirtyWriteback.asStateFlow()

    private val _dirtyExpireCentisecs = MutableStateFlow(300)
    val dirtyExpireCentisecs: StateFlow<Int> = _dirtyExpireCentisecs.asStateFlow()

    private val _minFreeMemory = MutableStateFlow(128)
    val minFreeMemory: StateFlow<Int> = _minFreeMemory.asStateFlow()



    /* Max ZRAM otomatis 6 GB untuk 8 GB RAM */
    private val _maxZramSize = MutableStateFlow(repo.calculateMaxZramSize())
    val maxZramSize: StateFlow<Long> = _maxZramSize.asStateFlow()

    /* ---------------- Thermal ---------------- */
    private val _currentThermalModeIndex = MutableStateFlow<Int?>(null)
    val currentThermalModeIndex: StateFlow<Int?> = _currentThermalModeIndex.asStateFlow()

    val currentThermalProfileName: StateFlow<String> =
        _currentThermalModeIndex.map { idx ->
            idx?.let { thermalRepo.getCurrentThermalProfileName(it) } ?: "Loading..."
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Loading...")

    val supportedThermalProfiles: StateFlow<List<ThermalRepository.ThermalProfile>> =
        thermalRepo.getSupportedThermalProfiles()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    //</editor-fold>

    //<editor-fold desc="Load Flags">
    private val isCpuDataLoaded = AtomicBoolean(false)
    private val isGpuDataLoaded = AtomicBoolean(false)
    private val isRamDataLoaded = AtomicBoolean(false)
    private val isThermalDataLoaded = AtomicBoolean(false)

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    //</editor-fold>

    /* ---------------- Init ---------------- */
    init {
        Log.d("TuningVM_Init", "ViewModel initializing...")
        // initializeCpuStateFlows() // Removed as it is handled in the field declaration init
        fetchDynamicCpuClusters()
        Log.d("TuningVM_Init", "ViewModel initialization complete.")
    }

    //<editor-fold desc="Lazy Load Functions">
    fun loadAllData() {
        viewModelScope.launch {
            val everythingLoaded = isCpuDataLoaded.get() && isGpuDataLoaded.get() && isRamDataLoaded.get() && isThermalDataLoaded.get()

            if (!everythingLoaded) {
                _isLoading.value = true
                // Use coroutineScope to wait for all child coroutines to complete
                coroutineScope {
                    launch { loadCpuData() }
                    launch { loadGpuData() }
                    launch { loadRamData() }
                    launch { loadThermalData() }
                }
                _isLoading.value = false
            } else {
                // Just refresh values silently
                refreshRealtimeData()
            }
        }
    }

    private suspend fun refreshRealtimeData() {
        withContext(Dispatchers.IO) {
            // CPU
            cpuClusters.forEach { cluster ->
                launch { repo.getCpuGov(cluster).take(1).collect { _currentCpuGovernors[cluster]?.value = it } }
                launch { repo.getCpuFreq(cluster).take(1).collect { _currentCpuFrequencies[cluster]?.value = it } }
            }
            refreshCoreStates()

            // GPU
            launch {
                try {
                    _currentGpuGovernor.value = repo.getGpuGov().first()
                    val (min, max) = repo.getGpuFreq().first()
                    _currentGpuMinFreq.value = min
                    _currentGpuMaxFreq.value = max
                    _currentGpuPowerLevel.value = repo.getCurrentGpuPowerLevel().first()
                    _gpuThrottlingEnabled.value = systemRepo.isGpuThrottlingEnabled()
                } catch (e: Exception) { Log.e("TuningVM", "Error refreshing GPU", e) }
            }

            // Thermal
            fetchCurrentThermalMode()
            
            // RAM settings are mostly static/preference based, no need to heavy refresh
        }
    }

    private suspend fun loadCpuData() {
        if (isCpuDataLoaded.getAndSet(true)) return
        Log.d("TuningVM_LazyLoad", "Loading CPU data...")
        withContext(Dispatchers.IO) {
            fetchAllCpuData()
            refreshCoreStates()
            
            // Allow time for flows to emit initial values
            delay(200) 
            
            // 1. Self-healing for Performance Mode
            if (preferenceManager.isApplyPerformanceModeOnBoot()) {
                val preferredMode = preferenceManager.getPerformanceMode()
                val gov0 = _currentCpuGovernors["cpu0"]?.value
                
                // Only act if we have valid governor data
                if (gov0 != null && gov0 != "...") {
                    if (preferredMode == "Performance" && gov0 != "performance") {
                        Log.d("TuningVM_SelfHeal", "Re-applying Performance Mode")
                        onPerformanceModeChange(preferredMode)
                        return@withContext // Exit to avoid conflict with manual settings below
                    } else if (preferredMode == "Powersave" && gov0 != "powersave") {
                        Log.d("TuningVM_SelfHeal", "Re-applying Powersave Mode")
                        onPerformanceModeChange(preferredMode)
                        return@withContext
                    }
                }
            }

            // 2. Self-healing for Manual CPU Settings (Per-cluster)
            if (preferenceManager.isApplyCpuOnBoot()) {
                cpuClusters.forEach { cluster ->
                    // Governor Check
                    val prefGov = preferenceManager.getCpuGov(cluster)
                    val currentGov = _currentCpuGovernors[cluster]?.value
                    
                    if (!prefGov.isNullOrEmpty() && currentGov != "..." && currentGov != prefGov) {
                         Log.d("TuningVM_SelfHeal", "Re-applying CPU Gov for $cluster: $prefGov")
                         setCpuGov(cluster, prefGov)
                    }

                    // Frequency Check
                    val prefMin = preferenceManager.getCpuMinFreq(cluster)
                    val prefMax = preferenceManager.getCpuMaxFreq(cluster)
                    val currentFreqs = _currentCpuFrequencies[cluster]?.value ?: (0 to 0)
                    val (currMin, currMax) = currentFreqs
                    
                    var needsUpdate = false
                    // Determine target frequencies
                    var targetMin = currMin
                    var targetMax = currMax

                    if (prefMin != -1 && prefMin != currMin) {
                        targetMin = prefMin
                        needsUpdate = true
                    }
                    if (prefMax != -1 && prefMax != currMax) {
                        targetMax = prefMax
                        needsUpdate = true
                    }

                    if (needsUpdate) {
                         Log.d("TuningVM_SelfHeal", "Re-applying CPU Freqs for $cluster: $targetMin - $targetMax")
                         setCpuFreq(cluster, targetMin, targetMax)
                    }
                }

                // Self-healing for Core Online Status
                (0..7).forEach { coreId ->
                    val prefOnline = preferenceManager.getCpuCoreOnline(coreId)
                    if (prefOnline != null) {
                        val currentOnline = repo.getCoreOnline(coreId)
                        if (currentOnline != prefOnline) {
                            Log.d("TuningVM_SelfHeal", "Re-applying Core $coreId Status: $prefOnline")
                            repo.setCoreOnline(coreId, prefOnline)
                        }
                    }
                }
                refreshCoreStates()
            }
        }
    }

    private suspend fun loadGpuData() {
        if (isGpuDataLoaded.getAndSet(true)) return
        Log.d("TuningVM_LazyLoad", "Loading GPU data...")
        withContext(Dispatchers.IO) {
            fetchGpuData()
            fetchOpenGlesDriver()
            fetchVulkanApiVersion()

            // Self-healing for GPU
            if (preferenceManager.isApplyGpuOnBoot()) {
                val prefGov = preferenceManager.getGpuGovernor()
                if (prefGov != null && _currentGpuGovernor.value != prefGov) {
                    Log.d("TuningVM_SelfHeal", "Re-applying GPU Governor: $prefGov")
                    setGpuGovernor(prefGov)
                }

                val prefMin = preferenceManager.getGpuMinFreq()
                val prefMax = preferenceManager.getGpuMaxFreq()
                if ((prefMin != -1 && _currentGpuMinFreq.value != prefMin) || 
                    (prefMax != -1 && _currentGpuMaxFreq.value != prefMax)) {
                    Log.d("TuningVM_SelfHeal", "Re-applying GPU Frequencies")
                    if (prefMin != -1) repo.setGpuMinFreq(prefMin)
                    if (prefMax != -1) repo.setGpuMaxFreq(prefMax)
                    fetchGpuData()
                }
            }
        }
    }

    private suspend fun loadRamData() {
        if (isRamDataLoaded.getAndSet(true)) return
        Log.d("TuningVM_LazyLoad", "Loading RAM data...")
        withContext(Dispatchers.IO) {
            fetchRamControlData()
            
            // Allow a small delay for flows to update state
            delay(150)

            // Full Self-healing for RAM
            if (preferenceManager.isApplyRamOnBoot()) {
                Log.d("TuningVM_SelfHeal", "Starting RAM Self-healing check")
                
                // 1. ZRAM Disksize
                val prefZramSize = preferenceManager.getZramDisksize()
                if (prefZramSize != -1L && _zramDisksize.value != prefZramSize) {
                    Log.d("TuningVM_SelfHeal", "Re-applying ZRAM Size: $prefZramSize")
                    setZramDisksize(prefZramSize)
                }

                // 2. Compression Algorithm
                val prefAlgo = preferenceManager.getZramCompression()
                if (prefAlgo != null && _currentCompression.value != prefAlgo) {
                    Log.d("TuningVM_SelfHeal", "Re-applying Compression: $prefAlgo")
                    setCompression(prefAlgo)
                }

                // 3. Swappiness
                val prefSwappiness = preferenceManager.getSwappiness()
                if (prefSwappiness != -1 && _swappiness.value != prefSwappiness) {
                    Log.d("TuningVM_SelfHeal", "Re-applying Swappiness: $prefSwappiness")
                    setSwappiness(prefSwappiness)
                }

                // 4. Dirty Ratio
                val prefDirtyRatio = preferenceManager.getDirtyRatio()
                if (prefDirtyRatio != -1 && _dirtyRatio.value != prefDirtyRatio) {
                    Log.d("TuningVM_SelfHeal", "Re-applying Dirty Ratio: $prefDirtyRatio")
                    setDirtyRatio(prefDirtyRatio)
                }

                // 5. Dirty Background Ratio
                val prefDirtyBg = preferenceManager.getDirtyBackgroundRatio()
                if (prefDirtyBg != -1 && _dirtyBackgroundRatio.value != prefDirtyBg) {
                    Log.d("TuningVM_SelfHeal", "Re-applying Dirty Background Ratio: $prefDirtyBg")
                    setDirtyBackgroundRatio(prefDirtyBg)
                }

                // 6. Dirty Writeback
                val prefWriteback = preferenceManager.getDirtyWriteback()
                if (prefWriteback != -1 && _dirtyWriteback.value != prefWriteback) {
                    Log.d("TuningVM_SelfHeal", "Re-applying Dirty Writeback: $prefWriteback")
                    setDirtyWriteback(prefWriteback)
                }

                // 7. Dirty Expire
                val prefExpire = preferenceManager.getDirtyExpire()
                if (prefExpire != -1 && _dirtyExpireCentisecs.value != prefExpire) {
                    Log.d("TuningVM_SelfHeal", "Re-applying Dirty Expire: $prefExpire")
                    setDirtyExpireCentisecs(prefExpire)
                }

                // 8. Min Free Memory
                val prefMinFree = preferenceManager.getMinFreeMemory()
                if (prefMinFree != -1 && _minFreeMemory.value != prefMinFree) {
                    Log.d("TuningVM_SelfHeal", "Re-applying Min Free Memory: $prefMinFree")
                    setMinFreeMemory(prefMinFree)
                }
            }
        }
    }

    private suspend fun loadThermalData() {
        if (isThermalDataLoaded.getAndSet(true)) return
        Log.d("TuningVM_LazyLoad", "Loading Thermal data...")
        withContext(Dispatchers.IO) {
            // Check if "Set on Boot" is enabled for Thermal
            if (preferenceManager.isApplyThermalOnBoot()) {
                // Prioritize restoring the user's last saved setting.
                val lastSavedIndex = thermalPrefs.getInt(KEY_LAST_APPLIED_THERMAL_INDEX, -2) // Use -2 to indicate no value saved

                if (lastSavedIndex != -2) {
                    // A profile was previously saved by the user. Restore it.
                    val profile = thermalRepo.availableThermalProfiles.find { it.index == lastSavedIndex }
                    if (profile != null) {
                        // Check if current kernel mode matches
                        val currentKernelIndex = thermalRepo.getCurrentThermalModeIndex().first()
                        if (currentKernelIndex != lastSavedIndex) {
                            Log.d("TuningVM_Thermal", "Restoring saved thermal profile (Self-heal): ${profile.displayName}")
                            setThermalProfileInternal(profile, isRestoring = true)
                        } else {
                            _currentThermalModeIndex.value = currentKernelIndex
                        }
                    } else {
                        fetchCurrentThermalMode()
                    }
                } else {
                    // No setting was ever saved by the user. Just read the current kernel state.
                    Log.d("TuningVM_Thermal", "No saved thermal profile found. Fetching from kernel.")
                    fetchCurrentThermalMode()
                }
            } else {
                // Set on Boot is disabled, so we just show what the kernel currently has
                // We do NOT restore any previous app-side preference here
                Log.d("TuningVM_Thermal", "Set on Boot disabled. Fetching current kernel state.")
                fetchCurrentThermalMode()
            }
        }
    }
    //</editor-fold>

    private fun fetchDynamicCpuClusters() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val clusters = systemRepo.getCpuClusters()
                // Map the dynamic cluster names to the corresponding cpu cluster identifiers
                val clusterNames = clusters.map { cluster ->
                    when {
                        cluster.name.contains("Little", ignoreCase = true) -> "cpu0"
                        cluster.name.contains("Big", ignoreCase = true) -> "cpu4"
                        cluster.name.contains("Prime", ignoreCase = true) -> "cpu7"
                        else -> {
                            // Fallback to original naming
                            when (cluster.name) {
                                "Efficiency Cluster" -> "cpu0"
                                "Performance Cluster" -> "cpu7"
                                else -> "cpu4" // Mid cluster
                            }
                        }
                    }
                }
                _dynamicCpuClusters.value = clusterNames
            } catch (e: Exception) {
                Log.e("TuningViewModel", "Error fetching dynamic CPU clusters", e)
                // Fallback to hardcoded values
                _dynamicCpuClusters.value = cpuClusters
            }
        }
    }

    /* ---------------- CPU ---------------- */
    // private fun initializeCpuStateFlows() { ... } // Removed

    private suspend fun fetchAllCpuData() {
        Log.d("TuningVM_CPU", "Fetching all CPU data...")
        val tempGovernors = mutableMapOf<String, List<String>>()
        val tempFreqs = mutableMapOf<String, List<Int>>()

        try {
            coroutineScope {
                cpuClusters.forEach { cluster ->
                    launch {
                        try {
                            repo.getCpuGov(cluster).take(1).collect { _currentCpuGovernors[cluster]?.value = it }
                        } catch (e: Exception) {
                            Log.e("TuningVM_CPU", "Error fetching CPU governor for $cluster", e)
                        }
                    }
                    launch {
                        try {
                            repo.getCpuFreq(cluster).take(1).collect { _currentCpuFrequencies[cluster]?.value = it }
                        } catch (e: Exception) {
                            Log.e("TuningVM_CPU", "Error fetching CPU frequency for $cluster", e)
                        }
                    }
                    launch {
                        try {
                            repo.getAvailableCpuGovernors(cluster).collect { tempGovernors[cluster] = it }
                        } catch (e: Exception) {
                            Log.e("TuningVM_CPU", "Error fetching available CPU governors for $cluster", e)
                        }
                    }
                    launch {
                        try {
                            repo.getAvailableCpuFrequencies(cluster).collect { tempFreqs[cluster] = it }
                        } catch (e: Exception) {
                            Log.e("TuningVM_CPU", "Error fetching available CPU frequencies for $cluster", e)
                        }
                    }
                }
            }
            _availableCpuFrequenciesPerClusterMap.value = tempFreqs
            if (tempGovernors.isNotEmpty()) _generalAvailableCpuGovernors.value = tempGovernors.values.flatten().distinct().sorted()

            Log.d("TuningVM_CPU", "Finished fetching all CPU data.")
        } catch (e: Exception) {
            Log.e("TuningVM_CPU", "Error in fetchAllCpuData", e)
        }
    }

    fun getCpuGov(cluster: String): StateFlow<String> = _currentCpuGovernors.getOrPut(cluster) { MutableStateFlow("...") }.asStateFlow()
    fun getCpuFreq(cluster: String): StateFlow<Pair<Int, Int>> = _currentCpuFrequencies.getOrPut(cluster) { MutableStateFlow(0 to 0) }.asStateFlow()
    fun getAvailableCpuFrequencies(cluster: String): StateFlow<List<Int>> = _availableCpuFrequenciesPerClusterMap.map { it[cluster] ?: emptyList() }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), emptyList())

    fun setCpuGov(cluster: String, gov: String) = viewModelScope.launch(Dispatchers.IO) {
        if (setCpuGovInternal(cluster, gov)) {
            // Logic: If user manually sets a governor (Custom mode), 
            // the global "Performance Mode" concept is no longer valid.
            // So we disable "Performance Mode on Boot" to prevent override.
            if (_applyPerformanceModeOnBoot.value) {
                toggleApplyPerformanceModeOnBoot(false)
                
                // UX Fix: If user had "Performance Mode on Boot" enabled, it means they WANTED persistence.
                // Since we just killed the Global Persistence, we should migrate their intent 
                // to the Granular/Manual Persistence ("CPU on Boot").
                // Otherwise, they might reboot and lose their new manual settings unexpectedly.
                if (!_applyCpuOnBoot.value) {
                    toggleApplyCpuOnBoot(true)
                }
            }
        }
    }

    private suspend fun setCpuGovInternal(cluster: String, gov: String): Boolean {
        if (repo.setCpuGov(cluster, gov)) {
            preferenceManager.setCpuGov(cluster, gov)
            // Update UI State immediately
            repo.getCpuGov(cluster).take(1).collect { _currentCpuGovernors[cluster]?.value = it }
            return true
        }
        return false
    }

    fun setCpuFreq(cluster: String, min: Int, max: Int) = viewModelScope.launch(Dispatchers.IO) {
        if (repo.setCpuFreq(cluster, min, max)) {
            preferenceManager.setCpuMinFreq(cluster, min)
            preferenceManager.setCpuMaxFreq(cluster, max)
            repo.getCpuFreq(cluster).take(1).collect { _currentCpuFrequencies[cluster]?.value = it }
        }
    }

    fun onPerformanceModeChange(mode: String) {
        _performanceMode.value = mode
        preferenceManager.setPerformanceMode(mode)
        val governor = when (mode) {
            "Performance" -> "performance"
            "Powersave" -> "powersave"
            else -> "schedutil"
        }
        viewModelScope.launch(Dispatchers.IO) {
            cpuClusters.forEach { cluster ->
                // Use internal method to avoid triggering the "Disable Boot Pref" logic
                setCpuGovInternal(cluster, governor)
            }
        }
    }

    fun toggleCore(coreId: Int) = viewModelScope.launch(Dispatchers.IO) {
        val newStates = _coreStates.value.toMutableList()
        val newState = !newStates[coreId]
        if (!newState && newStates.count { it } == 1) {
            _rebootCommandFeedback.emit("Setidaknya 1 core harus tetap online")
            return@launch
        }
        if (repo.setCoreOnline(coreId, newState)) {
            newStates[coreId] = newState
            _coreStates.value = newStates
            preferenceManager.setCpuCoreOnline(coreId, newState)
        } else {
            Log.e("TuningVM_CPU", "Failed toggle core $coreId")
        }
    }

    private suspend fun refreshCoreStates() = withContext(Dispatchers.IO) {
        _coreStates.value = (0 until 8).map { repo.getCoreOnline(it) }
    }

    /* ---------------- GPU ---------------- */
    private suspend fun fetchGpuData() = withContext(Dispatchers.IO) {
        try {
            _availableGpuGovernors.value = repo.getAvailableGpuGovernors().first()
            _currentGpuGovernor.value = repo.getGpuGov().first()
            _availableGpuFrequencies.value = repo.getAvailableGpuFrequencies().first()
            val (min, max) = repo.getGpuFreq().first()
            _currentGpuMinFreq.value = min
            _currentGpuMaxFreq.value = max
            Log.d("ViewModelGPU", "StateFlows updated: _currentGpuMinFreq=${_currentGpuMinFreq.value}, _currentGpuMaxFreq=${_currentGpuMaxFreq.value}")
            _gpuPowerLevelRange.value = repo.getGpuPowerLevelRange().first()
            _currentGpuPowerLevel.value = repo.getCurrentGpuPowerLevel().first()
            // Fetch GPU throttling status
            _gpuThrottlingEnabled.value = systemRepo.isGpuThrottlingEnabled()
        } catch (e: Exception) {
            Log.e("ViewModelGPU", "Error fetching GPU data", e)
        }
    }

    fun setGpuGovernor(gov: String) = viewModelScope.launch(Dispatchers.IO) {
        if (repo.setGpuGov(gov)) {
            preferenceManager.setGpuGovernor(gov)
            repo.getGpuGov().take(1).collect { _currentGpuGovernor.value = it }
        }
    }

    fun setGpuMinFrequency(freqKHz: Int) = viewModelScope.launch(Dispatchers.IO) {
        if (repo.setGpuMinFreq(freqKHz)) {
            preferenceManager.setGpuMinFreq(freqKHz)
            val (min, _) = repo.getGpuFreq().first()
            _currentGpuMinFreq.value = min
            fetchGpuData()
        }
    }

    fun setGpuMaxFrequency(freqKHz: Int) = viewModelScope.launch(Dispatchers.IO) {
        if (repo.setGpuMaxFreq(freqKHz)) {
            preferenceManager.setGpuMaxFreq(freqKHz)
            val (_, max) = repo.getGpuFreq().first()
            _currentGpuMaxFreq.value = max
            fetchGpuData()
        }
    }

    fun setGpuPowerLevel(level: Float) = viewModelScope.launch(Dispatchers.IO) {
        if (repo.setGpuPowerLevel(level)) {
            preferenceManager.setGpuPowerLevel(level.toInt())
            repo.getCurrentGpuPowerLevel().take(1).collect { _currentGpuPowerLevel.value = it }
        }
    }

    fun toggleGpuThrottling(enabled: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        val success = systemRepo.setGpuThrottling(enabled)
        if (success) {
            preferenceManager.setGpuThrottling(enabled)
            _gpuThrottlingEnabled.value = enabled
        } else {
            // If failed, refresh the actual value from system
            _gpuThrottlingEnabled.value = systemRepo.isGpuThrottlingEnabled()
        }
    }

    /* ---------------- OpenGL / Vulkan ---------------- */
    private suspend fun fetchOpenGlesDriver() = withContext(Dispatchers.IO) {
        repo.getOpenGlesDriver().collect { _currentOpenGlesDriver.value = it }
    }

    private suspend fun fetchVulkanApiVersion() = withContext(Dispatchers.IO) {
        repo.getVulkanApiVersion().collect { version ->
            _vulkanApiVersion.value = version
        }
    }

    fun confirmAndRebootDevice() {
        _showRebootConfirmationDialog.value = false
        viewModelScope.launch { repo.rebootDevice().collect { /* ignore, device reboot */ } }
    }

    fun cancelRebootConfirmation() {
        _showRebootConfirmationDialog.value = false
    }

    /* ---------------- RAM Control ---------------- */
    private suspend fun fetchRamControlData() = coroutineScope {
        launch(Dispatchers.IO) { repo.getZramEnabled().collect { _zramEnabled.value = it } }
        launch(Dispatchers.IO) { repo.getZramDisksize().collect { _zramDisksize.value = it } }
        launch(Dispatchers.IO) {
            repo.getCompressionAlgorithms().collect {
                _compressionAlgorithms.value = it
                repo.getCurrentCompression().firstOrNull()?.let { currentAlgo -> _currentCompression.value = currentAlgo }
            }
        }
        launch(Dispatchers.IO) { repo.getSwappiness().collect { _swappiness.value = it } }
        launch(Dispatchers.IO) { repo.getDirtyRatio().collect { _dirtyRatio.value = it } }
        launch(Dispatchers.IO) { repo.getDirtyBackgroundRatio().collect { _dirtyBackgroundRatio.value = it } }
        launch(Dispatchers.IO) { repo.getDirtyWriteback().collect { _dirtyWriteback.value = it } }
        launch(Dispatchers.IO) { repo.getDirtyExpireCentisecs().collect { _dirtyExpireCentisecs.value = it } }
        launch(Dispatchers.IO) { repo.getMinFreeMemory().collect { _minFreeMemory.value = it } }

    }

    fun setZramEnabled(enabled: Boolean) = viewModelScope.launch(Dispatchers.IO) {
        _zramOperationInProgress.value = true
        // Save preference immediately
        preferenceManager.setZramEnabledPref(enabled)
        
        try {
            var success = false
            if (enabled) {
                // Optimized: Apply size and algo in one go to avoid double-reset (Zombie restart)
                val targetSize = preferenceManager.getZramDisksize().takeIf { it > 0 } ?: repo.calculateMaxZramSize()
                val targetAlgo = preferenceManager.getZramCompression()
                success = repo.applyFullZramConfig(targetSize, targetAlgo)
            } else {
                // Disabling: Just use standard disable flow
                repo.setZramEnabled(false).collect { success = !it }
            }

            // Refresh state regardless of path taken
            repo.getZramEnabled().take(1).collect { isEnabled ->
                _zramEnabled.value = isEnabled
                // If enabled successfully, ensure displayed size is synced
                if (isEnabled) {
                    repo.getZramDisksize().take(1).collect { _zramDisksize.value = it }
                }
            }
        } finally {
            _zramOperationInProgress.value = false
        }
    }

    fun setZramDisksize(sizeBytes: Long) = viewModelScope.launch(Dispatchers.IO) {
        val max = repo.calculateMaxZramSize()
        if (sizeBytes < 512 * 1024 * 1024 || sizeBytes > max) {
            _rebootCommandFeedback.emit("Ukuran ZRAM tidak valid (512 MB - ${max / 1024 / 1024} MB)")
            return@launch
        }
        if (repo.setZramDisksize(sizeBytes)) {
            preferenceManager.setZramDisksize(sizeBytes)
            repo.getZramDisksize().take(1).collect { _zramDisksize.value = it }
        }
    }

    fun setCompression(algo: String) = viewModelScope.launch(Dispatchers.IO) {
        if (algo != _currentCompression.value) {
            if (repo.setCompressionAlgorithm(algo)) {
                preferenceManager.setZramCompression(algo)
                repo.getCurrentCompression().take(1).collect { _currentCompression.value = it }
            }
        }
    }

    fun setSwappiness(value: Int) = viewModelScope.launch(Dispatchers.IO) {
        if (repo.setSwappiness(value)) {
            preferenceManager.setSwappiness(value)
            repo.getSwappiness().take(1).collect { _swappiness.value = it }
        }
    }

    fun setDirtyRatio(value: Int) = viewModelScope.launch(Dispatchers.IO) {
        if (repo.setDirtyRatio(value)) {
            preferenceManager.setDirtyRatio(value)
            repo.getDirtyRatio().take(1).collect { _dirtyRatio.value = it }
        }
    }

    fun setDirtyBackgroundRatio(value: Int) = viewModelScope.launch(Dispatchers.IO) {
        if (repo.setDirtyBackgroundRatio(value)) {
            preferenceManager.setDirtyBackgroundRatio(value)
            repo.getDirtyBackgroundRatio().take(1).collect { _dirtyBackgroundRatio.value = it }
        }
    }

    fun setDirtyWriteback(value: Int) = viewModelScope.launch(Dispatchers.IO) {
        if (repo.setDirtyWriteback(value * 100)) {
            preferenceManager.setDirtyWriteback(value)
            repo.getDirtyWriteback().take(1).collect { _dirtyWriteback.value = it }
        }
    }

    fun setDirtyExpireCentisecs(value: Int) = viewModelScope.launch(Dispatchers.IO) {
        if (repo.setDirtyExpireCentisecs(value)) {
            preferenceManager.setDirtyExpire(value)
            repo.getDirtyExpireCentisecs().take(1).collect { _dirtyExpireCentisecs.value = it }
        }
    }

    fun setMinFreeMemory(value: Int) = viewModelScope.launch(Dispatchers.IO) {
        if (repo.setMinFreeMemory(value * 1024)) {
            preferenceManager.setMinFreeMemory(value)
            repo.getMinFreeMemory().take(1).collect { _minFreeMemory.value = it }
        }
    }



    /* ---------------- Thermal ---------------- */
    private suspend fun fetchCurrentThermalMode() {
        withContext(Dispatchers.IO) {
            thermalRepo.getCurrentThermalModeIndex()
                .catch { e ->
                    Log.e("TuningVM_Thermal", "Error getting current thermal mode", e)
                }
                .collect { index ->
                    _currentThermalModeIndex.value = index
                }
        }
    }

    private suspend fun applyLastSavedThermalProfile() {
        try {
            val idx = thermalPrefs.getInt(KEY_LAST_APPLIED_THERMAL_INDEX, -1)
            val profile = thermalRepo.availableThermalProfiles.find { it.index == idx }
            if (profile != null && _currentThermalModeIndex.value != idx) {
                setThermalProfileInternal(profile, isRestoring = true)
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    private suspend fun setThermalProfileInternal(profile: ThermalRepository.ThermalProfile, isRestoring: Boolean) {
        thermalRepo.setThermalModeIndex(profile.index).collect { ok ->
            if (ok) {
                _currentThermalModeIndex.value = profile.index
                if (!isRestoring) thermalPrefs.edit { putInt(KEY_LAST_APPLIED_THERMAL_INDEX, profile.index) }
                // For Dynamic mode (10), we need continuous monitoring
                // For other modes, persistent scripts handle reboot persistence
                if (profile.index == 10) {
                    // Only start service for Dynamic mode which requires CPU monitoring
                    val intent = Intent(application, ThermalService::class.java)
                    intent.putExtra("thermal_mode", profile.index)
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                            // For Android 15+, start foreground service with proper handling
                            ContextCompat.startForegroundService(application, intent)
                        } else {
                            application.startService(intent)
                        }
                        Log.d("TuningVM_Thermal", "Started ThermalService for Dynamic mode")
                    } catch (e: Exception) {
                        Log.e("TuningVM_Thermal", "Failed to start ThermalService", e)
                    }
                } else {
                    // For other modes, stop the service if running
                    stopThermalService()
                }
            } else {
                fetchCurrentThermalMode()
            }
        }
    }

    private fun stopThermalService() {
        val intent = Intent(application, ThermalService::class.java)
        application.stopService(intent)
        Log.d("TuningVM_Thermal", "Stopped ThermalService")
    }

    fun setThermalProfile(profile: ThermalRepository.ThermalProfile) =
        viewModelScope.launch { setThermalProfileInternal(profile, isRestoring = false) }
}