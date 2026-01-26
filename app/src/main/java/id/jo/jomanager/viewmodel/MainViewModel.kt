package id.jo.jomanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.jo.jomanager.data.repository.SystemRepository
import id.jo.jomanager.utils.PreferenceManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log

@HiltViewModel
class MainViewModel @Inject constructor(
    private val systemRepository: SystemRepository,
    private val preferenceManager: PreferenceManager
) : ViewModel() {

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
}