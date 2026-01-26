package id.jo.jomanager.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import id.jo.jomanager.data.repository.SystemRepository
import id.jo.jomanager.data.repository.TuningRepository
import id.jo.jomanager.data.repository.ThermalRepository
import id.jo.jomanager.utils.PreferenceManager
import android.content.Intent
import androidx.core.content.ContextCompat
import id.jo.jomanager.service.ThermalService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@HiltWorker
class RestoreSettingsWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val systemRepository: SystemRepository,
    private val tuningRepository: TuningRepository,
    private val thermalRepository: ThermalRepository,
    private val preferenceManager: PreferenceManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("RestoreSettingsWorker", "Restoring system settings...")

        restoreNetworkAndIoSettings()
        restoreMiscSettings()
        restorePerformanceMode()
        restoreCpuSettings() // New method for per-cluster restore
        restoreGpuSettings()
        restoreRamSettings()
        restoreThermalSettings()

        return Result.success()
    }

    private suspend fun restoreCpuSettings() {
        if (!preferenceManager.isApplyCpuOnBoot()) {
            Log.d("RestoreSettingsWorker", "Skipping CPU restore (disabled by user)")
            return
        }

        // Restore Core Online Status
        for (coreId in 0..7) {
            preferenceManager.getCpuCoreOnline(coreId)?.let { online ->
                var success = false
                for (i in 1..5) {
                    if (tuningRepository.setCoreOnline(coreId, online)) {
                        success = true
                        Log.d("RestoreSettingsWorker", "Restored Core $coreId online status to $online: success")
                        break
                    }
                    delay(500)
                }
                if (!success) Log.e("RestoreSettingsWorker", "Failed to restore Core $coreId online status")
            }
        }

        val clusters = tuningRepository.getClusterLeaders()
        clusters.forEach { cluster ->
            // Restore Governor
            preferenceManager.getCpuGov(cluster)?.let { gov ->
                var success = false
                for (i in 1..5) {
                    if (tuningRepository.setCpuGov(cluster, gov)) {
                        success = true
                        Log.d("RestoreSettingsWorker", "Restored CPU Governor for $cluster to $gov: success")
                        break
                    }
                    delay(2000)
                }
                if (!success) Log.e("RestoreSettingsWorker", "Failed to restore CPU Gov for $cluster to $gov")
            }

            // Restore Frequencies
            val minFreq = preferenceManager.getCpuMinFreq(cluster)
            val maxFreq = preferenceManager.getCpuMaxFreq(cluster)
            if (minFreq != -1 && maxFreq != -1) {
                var success = false
                for (i in 1..5) {
                    if (tuningRepository.setCpuFreq(cluster, minFreq, maxFreq)) {
                        success = true
                        Log.d("RestoreSettingsWorker", "Restored CPU Frequencies for $cluster to $minFreq - $maxFreq: success")
                        break
                    }
                    delay(2000)
                }
                if (!success) Log.e("RestoreSettingsWorker", "Failed to restore CPU Freqs for $cluster")
            }
        }
    }

    private suspend fun restoreNetworkAndIoSettings() {
        if (!preferenceManager.isApplyNetworkStorageOnBoot()) {
            Log.d("RestoreSettingsWorker", "Skipping Network & IO restore (disabled by user)")
            return
        }

        // Restore TCP Congestion Algorithm
        val savedTcpAlgo = preferenceManager.getTcpCongestionAlgorithm()
        if (!savedTcpAlgo.isNullOrEmpty()) {
            var success = false
            for (i in 1..5) {
                if (systemRepository.setTcpCongestionAlgorithm(savedTcpAlgo)) {
                    success = true
                    Log.d("RestoreSettingsWorker", "Restored TCP Congestion to $savedTcpAlgo: success")
                    break
                }
                Log.d("RestoreSettingsWorker", "Failed to restore TCP Congestion (attempt $i), retrying...")
                delay(2000)
            }
            if (!success) {
                Log.e("RestoreSettingsWorker", "Failed to restore TCP Congestion to $savedTcpAlgo after 5 attempts")
            }
        }

        // Restore I/O Scheduler
        val savedIoScheduler = preferenceManager.getIoScheduler()
        if (!savedIoScheduler.isNullOrEmpty()) {
            var success = false
            for (i in 1..5) {
                if (systemRepository.setIoScheduler(savedIoScheduler)) {
                    success = true
                    Log.d("RestoreSettingsWorker", "Restored I/O Scheduler to $savedIoScheduler: success")
                    break
                }
                Log.d("RestoreSettingsWorker", "Failed to restore I/O Scheduler (attempt $i), retrying...")
                delay(2000)
            }
            if (!success) {
                Log.e("RestoreSettingsWorker", "Failed to restore I/O Scheduler to $savedIoScheduler after 5 attempts")
            }
        }
    }

    private fun restoreMiscSettings() {
        // Restore Avoid Dirty PTE
        if (preferenceManager.getAvoidDirtyPte()) {
            val success = systemRepository.setAvoidDirtyPte(true)
            Log.d("RestoreSettingsWorker", "Restored Avoid Dirty PTE: $success")
        }

        // Restore KGSL Skip Zeroing
        if (preferenceManager.getKgslSkipZeroing()) {
            val success = systemRepository.setKgslSkipZeroing(true)
            Log.d("RestoreSettingsWorker", "Restored KGSL Skip Zeroing: $success")
        }

        // Restore Bypass Charging
        if (preferenceManager.getBypassCharging()) {
            val success = systemRepository.setBypassCharging(true)
            Log.d("RestoreSettingsWorker", "Restored Bypass Charging: $success")
        }

        // Restore Force Fast Charge
        if (preferenceManager.getForceFastCharge()) {
            val success = systemRepository.setForceFastCharge(true)
            Log.d("RestoreSettingsWorker", "Restored Force Fast Charge: $success")
        }
    }

    private suspend fun restorePerformanceMode() {
        if (!preferenceManager.isApplyPerformanceModeOnBoot()) {
            Log.d("RestoreSettingsWorker", "Skipping Performance Mode restore (disabled by user)")
            return
        }

        val mode = preferenceManager.getPerformanceMode()
        
        val governor = when (mode) {
            "Performance" -> "performance"
            "Powersave" -> "powersave"
            else -> "schedutil"
        }

        val clusters = tuningRepository.getClusterLeaders()
        clusters.forEach { cluster ->
            var success = false
            for (i in 1..5) {
                if (tuningRepository.setCpuGov(cluster, governor)) {
                    success = true
                    Log.d("RestoreSettingsWorker", "Restored Performance Mode ($mode -> $governor) on $cluster: success")
                    break
                }
                delay(2000)
            }
        }
    }

    private suspend fun restoreGpuSettings() {
        if (!preferenceManager.isApplyGpuOnBoot()) {
            Log.d("RestoreSettingsWorker", "Skipping GPU restore (disabled by user)")
            return
        }

        // Governor
        preferenceManager.getGpuGovernor()?.let { gov ->
            var success = false
            for (i in 1..5) {
                if (tuningRepository.setGpuGov(gov)) {
                    success = true
                    Log.d("RestoreSettingsWorker", "Restored GPU Governor to $gov: success")
                    break
                }
                delay(2000)
            }
        }

        // Frequencies
        val minFreq = preferenceManager.getGpuMinFreq()
        if (minFreq != -1) {
            for (i in 1..5) {
                if (tuningRepository.setGpuMinFreq(minFreq)) break
                delay(2000)
            }
        }
        
        val maxFreq = preferenceManager.getGpuMaxFreq()
        if (maxFreq != -1) {
            for (i in 1..5) {
                if (tuningRepository.setGpuMaxFreq(maxFreq)) break
                delay(2000)
            }
        }

        // Power Level
        val powerLevel = preferenceManager.getGpuPowerLevel()
        if (powerLevel != -1) {
            for (i in 1..5) {
                if (tuningRepository.setGpuPowerLevel(powerLevel.toFloat())) break
                delay(2000)
            }
        }

        // Throttling
        preferenceManager.getGpuThrottling()?.let { enabled ->
            for (i in 1..5) {
                if (systemRepository.setGpuThrottling(enabled)) break
                delay(2000)
            }
        }
    }

    private suspend fun restoreRamSettings() {
        if (!preferenceManager.isApplyRamOnBoot()) {
            Log.d("RestoreSettingsWorker", "Skipping RAM restore (disabled by user)")
            return
        }

        // 1. Check if ZRAM should be enabled
        val shouldEnableZram = preferenceManager.isZramEnabledPref()
        
        if (shouldEnableZram) {
            val zramSize = preferenceManager.getZramDisksize()
            val zramAlgo = preferenceManager.getZramCompression()
            
            // Only proceed if we have at least a valid size setting
            if (zramSize != -1L) {
                 Log.d("RestoreSettingsWorker", "Restoring ZRAM: Size=${zramSize}, Algo=${zramAlgo ?: "default"}")
                 for (i in 1..5) {
                    if (tuningRepository.applyFullZramConfig(zramSize, zramAlgo)) {
                        Log.d("RestoreSettingsWorker", "Restored ZRAM config: success")
                        break
                    }
                    delay(2000)
                 }
            }
        } else {
            // User explicitly disabled ZRAM. Ensure it's off.
            Log.d("RestoreSettingsWorker", "ZRAM disabled by preference. Ensuring it is off.")
            // We can just set size to 0 using existing method which handles swapoff/reset
            tuningRepository.setZramEnabled(false).first()
        }

        // Swappiness
        val swappiness = preferenceManager.getSwappiness()
        if (swappiness != -1) {
            for (i in 1..5) {
                if (tuningRepository.setSwappiness(swappiness)) break
                delay(2000)
            }
        }

        // Dirty Ratio
        val dirtyRatio = preferenceManager.getDirtyRatio()
        if (dirtyRatio != -1) {
            for (i in 1..5) {
                if (tuningRepository.setDirtyRatio(dirtyRatio)) break
                delay(2000)
            }
        }

        // Dirty Background Ratio
        val dirtyBgRatio = preferenceManager.getDirtyBackgroundRatio()
        if (dirtyBgRatio != -1) {
            for (i in 1..5) {
                if (tuningRepository.setDirtyBackgroundRatio(dirtyBgRatio)) break
                delay(2000)
            }
        }

        // Dirty Writeback
        val dirtyWriteback = preferenceManager.getDirtyWriteback()
        if (dirtyWriteback != -1) {
            for (i in 1..5) {
                if (tuningRepository.setDirtyWriteback(dirtyWriteback * 100)) break
                delay(2000)
            }
        }

        // Dirty Expire
        val dirtyExpire = preferenceManager.getDirtyExpire()
        if (dirtyExpire != -1) {
            for (i in 1..5) {
                if (tuningRepository.setDirtyExpireCentisecs(dirtyExpire)) break
                delay(2000)
            }
        }

        // Min Free Memory
        val minFree = preferenceManager.getMinFreeMemory()
        if (minFree != -1) {
            for (i in 1..5) {
                if (tuningRepository.setMinFreeMemory(minFree * 1024)) break
                delay(2000)
            }
        }
    }

    private suspend fun restoreThermalSettings() {
        if (!preferenceManager.isApplyThermalOnBoot()) {
            Log.d("RestoreSettingsWorker", "Skipping Thermal restore (disabled by user)")
            return
        }

        val thermalPrefs = applicationContext.getSharedPreferences("thermal_settings_prefs", Context.MODE_PRIVATE)
        val lastSavedIndex = thermalPrefs.getInt("last_applied_thermal_index", -2)

        if (lastSavedIndex != -2) {
            var success = false
            for (i in 1..5) {
                val ok = thermalRepository.setThermalModeIndex(lastSavedIndex).first()
                if (ok) {
                    success = true
                    Log.d("RestoreSettingsWorker", "Restored Thermal Profile Index: $lastSavedIndex")
                    
                    // Handle Dynamic Mode Service
                    if (lastSavedIndex == 10) {
                        val intent = Intent(applicationContext, ThermalService::class.java)
                        intent.putExtra("thermal_mode", lastSavedIndex)
                        try {
                            ContextCompat.startForegroundService(applicationContext, intent)
                        } catch (e: Exception) {
                            Log.e("RestoreSettingsWorker", "Failed to start ThermalService", e)
                        }
                    }
                    break
                }
                delay(2000)
            }
            if (!success) Log.e("RestoreSettingsWorker", "Failed to restore Thermal Profile Index: $lastSavedIndex")
        }
    }
}
