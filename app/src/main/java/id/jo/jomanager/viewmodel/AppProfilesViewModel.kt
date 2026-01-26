package id.jo.jomanager.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.jo.jomanager.data.database.AppProfileEntity
import id.jo.jomanager.data.repository.AppProfileRepository
import id.jo.jomanager.service.AppMonitorService
import id.jo.jomanager.utils.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

import id.jo.jomanager.data.repository.TuningRepository
import id.jo.jomanager.data.repository.SystemRepository
import kotlinx.coroutines.flow.first

@HiltViewModel
class AppProfilesViewModel @Inject constructor(
    private val application: Application,
    private val appProfileRepository: AppProfileRepository,
    private val preferenceManager: PreferenceManager,
    private val systemRepository: SystemRepository,
    private val tuningRepository: TuningRepository
) : AndroidViewModel(application) {

    val profiles = appProfileRepository.getAllProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isKgslFeatureAvailable = MutableStateFlow<Boolean?>(null)
    val isKgslFeatureAvailable: StateFlow<Boolean?> = _isKgslFeatureAvailable.asStateFlow()

    private val _isPowersaveAvailable = MutableStateFlow<Boolean>(false)
    val isPowersaveAvailable: StateFlow<Boolean> = _isPowersaveAvailable.asStateFlow()
    
    init {
        // Check if KGSL feature is available
        _isKgslFeatureAvailable.value = systemRepository.isKgslFeatureAvailable()
        
        // Check if Powersave governor is available
        viewModelScope.launch {
            checkPowersaveAvailability()
        }
    }

    private suspend fun checkPowersaveAvailability() {
        val clusters = listOf("cpu0", "cpu4", "cpu7") // Typical clusters
        var available = false
        for (cluster in clusters) {
            try {
                val govs = tuningRepository.getAvailableCpuGovernors(cluster).first()
                if (govs.contains("powersave")) {
                    available = true
                    break
                }
            } catch (e: Exception) {
                // Ignore errors checking specific clusters
            }
        }
        _isPowersaveAvailable.value = available
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    private var allApps: List<AppInfo> = emptyList()

    private val _filteredApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val filteredApps = _filteredApps.asStateFlow()

    private val _isLoadingApps = MutableStateFlow(false)
    val isLoadingApps: StateFlow<Boolean> = _isLoadingApps.asStateFlow()
    
    // Check if service is running or permissions granted? 
    // We can check usage stats permission here.
    
    fun loadInstalledApps() {
        viewModelScope.launch {
            _isLoadingApps.value = true
            val apps = withContext(Dispatchers.IO) {
                val pm = application.packageManager
                val packages = pm.getInstalledPackages(0)
                packages.mapNotNull { packageInfo ->
                    val appInfo = packageInfo.applicationInfo ?: return@mapNotNull null
                    
                    // Filter system apps, but keep updated system apps (e.g. Gmail, Chrome updates)
                    // Basically exclude apps that are system and NOT updated system apps
                    if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 && 
                        (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0) {
                        return@mapNotNull null
                    }
                    
                    val label = appInfo.loadLabel(pm).toString()

                    AppInfo(
                        packageName = packageInfo.packageName,
                        appName = label
                    )
                }.sortedBy { it.appName.lowercase() }
            }
            allApps = apps
            filterApps(_searchQuery.value)
            _isLoadingApps.value = false
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        filterApps(query)
    }

    private fun filterApps(query: String) {
        if (query.isBlank()) {
            _filteredApps.value = allApps
        } else {
            _filteredApps.value = allApps.filter { 
                it.appName.contains(query, ignoreCase = true) || 
                it.packageName.contains(query, ignoreCase = true) 
            }
        }
    }

    fun addProfile(appInfo: AppInfo) {
        viewModelScope.launch {
            val profile = AppProfileEntity(
                packageName = appInfo.packageName,
                appName = appInfo.appName,
                // Default settings
                performanceMode = "Balanced",
                kgslSkipZeroing = false,
                bypassCharging = false,
                isEnabled = true
            )
            appProfileRepository.insertProfile(profile)
        }
    }

    fun updateProfile(profile: AppProfileEntity) {
        viewModelScope.launch {
            appProfileRepository.insertProfile(profile)
        }
    }

    fun deleteProfile(profile: AppProfileEntity) {
        viewModelScope.launch {
            appProfileRepository.deleteProfile(profile)
        }
    }

    fun toggleService(enabled: Boolean) {
        // We can use PreferenceManager to store a "Service Enabled" toggle if we want the user to easily disable the whole feature.
        // For now, let's just start/stop service.
        val intent = Intent(application, AppMonitorService::class.java)
        if (enabled) {
            application.startService(intent)
        } else {
            application.stopService(intent)
        }
    }
    
    fun hasUsageStatsPermission(): Boolean {
        val appOps = application.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        val mode = appOps.checkOpNoThrow(
            android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
            android.os.Process.myUid(),
            application.packageName
        )
        return mode == android.app.AppOpsManager.MODE_ALLOWED
    }
}

data class AppInfo(
    val packageName: String,
    val appName: String
)