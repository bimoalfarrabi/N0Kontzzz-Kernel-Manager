package id.jo.jomanager.service

import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.IBinder
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import id.jo.jomanager.data.database.AppProfileEntity
import id.jo.jomanager.data.repository.AppProfileRepository
import id.jo.jomanager.data.repository.SystemRepository
import id.jo.jomanager.utils.PreferenceManager
import kotlinx.coroutines.*
import javax.inject.Inject

import id.jo.jomanager.data.repository.TuningRepository

@AndroidEntryPoint
class AppMonitorService : Service() {

    @Inject
    lateinit var appProfileRepository: AppProfileRepository

    @Inject
    lateinit var systemRepository: SystemRepository

    @Inject
    lateinit var tuningRepository: TuningRepository

    @Inject
    lateinit var preferenceManager: PreferenceManager
    
    // Raw shared prefs removed as we now use PreferenceManager

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitorJob: Job? = null
    private var lastPackageName: String? = null
    private var isProfileApplied = false

    private val usageStatsManager by lazy {
        getSystemService(USAGE_STATS_SERVICE) as UsageStatsManager
    }

    private val powerManager by lazy {
        getSystemService(POWER_SERVICE) as android.os.PowerManager
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startMonitoring()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (monitorJob == null || !monitorJob!!.isActive) {
            startMonitoring()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        serviceScope.cancel()
    }

    private fun startMonitoring() {
        monitorJob?.cancel()
        monitorJob = serviceScope.launch {
            while (isActive) {
                if (powerManager.isInteractive) {
                    checkForegroundApp()
                    delay(1000) // Check every 1 second
                } else {
                    // Screen is off, pause monitoring
                    delay(5000)
                }
            }
        }
    }

    private fun stopMonitoring() {
        monitorJob?.cancel()
    }

    private suspend fun checkForegroundApp() {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - 2000 // Look back 2 seconds

        val events = usageStatsManager.queryEvents(startTime, endTime)
        val event = UsageEvents.Event()
        var currentApp = ""

        // Iterate to find the latest ACTIVITY_RESUMED event
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED) {
                currentApp = event.packageName
            }
        }

        // If no new event found, currentApp will be empty, so we skip
        if (currentApp.isNotEmpty() && currentApp != lastPackageName) {
            lastPackageName = currentApp
            handleAppChange(currentApp)
        }
    }

    private suspend fun handleAppChange(packageName: String) {
        val profile = appProfileRepository.getProfile(packageName)

        if (profile != null && profile.isEnabled) {
            applyProfile(profile)
            isProfileApplied = true
        } else {
            // Only revert if we previously applied a profile
            if (isProfileApplied) {
                applyGlobalSettings()
                isProfileApplied = false
            }
        }
    }

    private suspend fun applyProfile(profile: AppProfileEntity) {
        Log.d("AppMonitorService", "Applying profile for ${profile.appName}: ${profile.performanceMode}")
        
        // 1. Performance Mode
        applyPerformanceMode(profile.performanceMode)

        // 2. KGSL
        systemRepository.setKgslSkipZeroing(profile.kgslSkipZeroing)

        // 3. Bypass Charging
        systemRepository.setBypassCharging(profile.bypassCharging)
    }

    private suspend fun applyGlobalSettings() {
        Log.d("AppMonitorService", "Reverting to global settings")

        // 1. Performance Mode
        val globalMode = preferenceManager.getPerformanceMode()
        applyPerformanceMode(globalMode)

        // 2. KGSL
        val globalKgsl = preferenceManager.getKgslSkipZeroing()
        systemRepository.setKgslSkipZeroing(globalKgsl)

        // 3. Bypass Charging
        val globalBypass = preferenceManager.getBypassCharging()
        systemRepository.setBypassCharging(globalBypass)
    }

    private fun applyPerformanceMode(mode: String) {
        val governor = if (mode == "Performance") "performance" else "schedutil"
        
        // Dynamically get cluster leaders instead of hardcoding
        val clusterNodes = tuningRepository.getClusterLeaders()
        clusterNodes.forEach { cluster ->
             tuningRepository.setCpuGov(cluster, governor)
        }
    }
}
