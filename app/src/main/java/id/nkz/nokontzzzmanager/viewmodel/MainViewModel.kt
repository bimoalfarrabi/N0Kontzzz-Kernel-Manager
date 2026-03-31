package id.nkz.nokontzzzmanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.nkz.nokontzzzmanager.data.repository.SystemRepository
import id.nkz.nokontzzzmanager.utils.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import id.nkz.nokontzzzmanager.data.config.KernelSupportConfig
import id.nkz.nokontzzzmanager.data.repository.RootRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class MainViewModel @Inject constructor(
    private val systemRepository: SystemRepository,
    private val preferenceManager: PreferenceManager,
    private val rootRepo: RootRepository
) : ViewModel() {

    private val _isRootAvailable = MutableStateFlow<Boolean?>(null)
    val isRootAvailable = _isRootAvailable.asStateFlow()

    private val _isKernelSupported = MutableStateFlow<Boolean?>(null)
    val isKernelSupported = _isKernelSupported.asStateFlow()

    suspend fun checkRootAndKernel() {
        val rooted = rootRepo.checkRootFresh()
        _isRootAvailable.value = rooted
        
        if (rooted) {
            _isKernelSupported.value = verifyKernelSupport()
        }
    }

    private suspend fun verifyKernelSupport(): Boolean {
        val config = KernelSupportConfig()

        return try {
            val versionLine = rootRepo.run("cat /proc/version")

            if (versionLine.isNotBlank()) {
                // Special check for E404R kernel (keep it here because special case)
                val e404rMatch = (versionLine.contains("4.19.404R", ignoreCase = true) || 
                                  versionLine.contains("5.10.404R", ignoreCase = true))
                val hostMatch = (versionLine.contains("vyn", ignoreCase = true) && 
                                (versionLine.contains("fedora", ignoreCase = true) || 
                                versionLine.contains("zorin", ignoreCase = true))) ||
                                (versionLine.contains("sai", ignoreCase = true) && 
                                versionLine.contains("ServerHive", ignoreCase = true))
                val buildHostMatch = versionLine.contains("build-user@build-host", ignoreCase = true)

                if (e404rMatch && (hostMatch || buildHostMatch)) {
                    return true
                }

                for (signature in config.supportedSignatures) {
                    if (versionLine.contains(signature, ignoreCase = true)) {
                        val signatureKey = signature.lowercase()
                        val supportedHosts = config.kernelHosts[signatureKey]

                        return when {
                            supportedHosts != null -> {
                                supportedHosts.any { versionLine.contains(it, ignoreCase = true) }
                            }
                            else -> true
                        }
                    }
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    fun runFailsafeNetworkStorageRestore() {
        if (!preferenceManager.isApplyNetworkStorageOnBoot()) return

        viewModelScope.launch(Dispatchers.IO) {
            val currentBootId = systemRepository.getBootId() ?: return@launch
            val lastAppliedBootId = preferenceManager.getLastAppliedBootId()

            if (currentBootId != lastAppliedBootId) {
                Log.d("MainViewModel", "Failsafe: New boot detected ($currentBootId), applying network & storage settings...")
                
                // Give a small delay to ensure system stability
                delay(2000)

                var tcpSuccess = false
                val savedTcpAlgo = preferenceManager.getTcpCongestionAlgorithm()
                if (!savedTcpAlgo.isNullOrEmpty()) {
                    // Try to apply with retries
                    for (i in 1..3) {
                        if (systemRepository.setTcpCongestionAlgorithm(savedTcpAlgo)) {
                            tcpSuccess = true
                            Log.d("MainViewModel", "Failsafe: Restored TCP Congestion to $savedTcpAlgo")
                            break
                        }
                        delay(1000)
                    }
                }

                var ioSuccess = false
                val savedIoScheduler = preferenceManager.getIoScheduler()
                if (!savedIoScheduler.isNullOrEmpty()) {
                     for (i in 1..3) {
                        if (systemRepository.setIoScheduler(savedIoScheduler)) {
                            ioSuccess = true
                            Log.d("MainViewModel", "Failsafe: Restored I/O Scheduler to $savedIoScheduler")
                            break
                        }
                        delay(1000)
                    }
                }

                // If we attempted to restore and at least finished our attempts, mark this boot ID as handled.
                // Even if it failed, we don't want to retry every time the app opens in this session.
                preferenceManager.setLastAppliedBootId(currentBootId)
            } else {
                Log.d("MainViewModel", "Failsafe: Already applied for this boot session.")
            }
        }
    }

    fun refreshBatteryMonitor(context: android.content.Context) {
        if (preferenceManager.isBatteryMonitorEnabled()) {
            id.nkz.nokontzzzmanager.service.BatteryMonitorService.updateIcon(context)
        }
    }
}