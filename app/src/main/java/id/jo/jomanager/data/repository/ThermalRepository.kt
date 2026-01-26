package id.jo.jomanager.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThermalRepository @Inject constructor(
    private val context: Context,
    private val rootRepository: RootRepository,
    private val thermalDataStore: DataStore<Preferences>
) {
    private val TAG = "ThermalRepository"
    private val thermalSysfsNode = "/sys/class/thermal/thermal_message/sconfig"
    private val serviceDir = "/data/adb/service.d"
    private val thermalScriptPath = "$serviceDir/thermal.sh"
    private val persistentScriptPath = "/data/adb/post-fs-data.d/thermal_persist.sh"

    private val LAST_THERMAL_MODE = intPreferencesKey("last_thermal_mode")
    private val USER_MAX_FREQ = intPreferencesKey("user_max_freq")
    private val USER_GOVERNOR = stringPreferencesKey("user_governor")

    data class ThermalProfile(val displayName: String, val index: Int)
    val availableThermalProfiles = listOf(
        ThermalProfile("Dynamic", 10),
        ThermalProfile("Class 0", 11),
        ThermalProfile("AR VR", 15),
        ThermalProfile("In-calls", 8),
        ThermalProfile("Game", 9),
        ThermalProfile("Game 2", 16),
        ThermalProfile("Extreme", 2),
        ThermalProfile("Pubg", 12),
        ThermalProfile("Camera", 13),
        ThermalProfile("Youtube", 14),
        ThermalProfile("Disabled", 0),
        ThermalProfile("Not Set", -1)
    )

    private var userSetMaxFreq: Int = 0
    private var userSetGovernor: String? = null
    private var monitoringJob: Job? = null

    private fun executeRootCommand(cmd: String, logTag: String = TAG): Shell.Result {
        if (!rootRepository.isRootStillAvailable()) {
            Log.e(logTag, "Root access not available for command: $cmd")
            return Shell.cmd("false").exec()
        }
        return try {
            val result = Shell.cmd(cmd).exec()
            if (result.isSuccess) {
                Log.i(logTag, "Root Command Success: '$cmd'")
            } else {
                Log.e(logTag, "Root Command Failed: '$cmd'. Err: ${result.err.joinToString("\n")}")
            }
            result
        } catch (e: Exception) {
            Log.e(logTag, "Exception during root command: '$cmd'", e)
            Shell.cmd("false").exec()
        }
    }

    private fun readRootCommand(cmd: String, logTag: String = TAG): String? {
        if (!rootRepository.isRootStillAvailable()) {
            Log.e(logTag, "Root access not available for command: $cmd")
            return null
        }
        return try {
            val result = Shell.cmd(cmd).exec()
            if (result.isSuccess) {
                val output = result.out.joinToString("\n").trim()
                Log.i(logTag, "Read Root Command Success: '$cmd'. Output: $output")
                output
            } else {
                Log.e(logTag, "Read Root Command Failed: '$cmd'. Err: ${result.err.joinToString("\n")}")
                null
            }
        } catch (e: Exception) {
            Log.e(logTag, "Exception during read root command: '$cmd'", e)
            null
        }
    }

    private suspend fun createPersistentScript(modeIndex: Int): Boolean = withContext(Dispatchers.IO) {
        val scriptContent = """
            #!/system/bin/sh

            # Wait for system to fully boot
            until [ -d /sys/class/thermal ]; do
                sleep 1
            done

            # Set SELinux context for thermal access
            chcon u:object_r:sysfs_thermal:s0 $thermalSysfsNode

            # Apply thermal mode
            echo "$modeIndex" > $thermalSysfsNode

            # Restore proper context
            restorecon $thermalSysfsNode

            exit 0
        """.trimIndent()

        executeRootCommand("mkdir -p /data/adb/post-fs-data.d")
        executeRootCommand("chmod 755 /data/adb/post-fs-data.d")

        val writeResult = executeRootCommand("cat > '$persistentScriptPath' << 'EOF'\n$scriptContent\nEOF")
        if (!writeResult.isSuccess) {
            Log.e(TAG, "Failed to write persistent script")
            return@withContext false
        }

        executeRootCommand("chmod 755 '$persistentScriptPath'")
        executeRootCommand("chown root:root '$persistentScriptPath'")

        return@withContext true
    }

    private suspend fun updateThermalScript(modeIndex: Int): Boolean = withContext(Dispatchers.IO) {
        if (modeIndex <= 0) {
            Log.w(TAG, "updateThermalScript: Invalid modeIndex $modeIndex for creating a script. Aborting.")
            return@withContext false
        }
        val serviceResult = createServiceScript(modeIndex)
        val persistentResult = createPersistentScript(modeIndex)
        return@withContext serviceResult && persistentResult
    }

    private suspend fun createServiceScript(modeIndex: Int): Boolean = withContext(Dispatchers.IO) {
        val scriptContent = """
            #!/system/bin/sh

            # Wait for thermal system
            until [ -d /sys/class/thermal ]; do
                sleep 1
            done

            # Set SELinux context temporarily
            chcon u:object_r:sysfs_thermal:s0 $thermalSysfsNode

            # Apply thermal mode
            echo "$modeIndex" > $thermalSysfsNode

            # Restore context
            restorecon $thermalSysfsNode

            exit 0
        """.trimIndent()

        executeRootCommand("mkdir -p '$serviceDir'")
        executeRootCommand("chmod 755 '$serviceDir'")

        val writeResult = executeRootCommand("cat > '$thermalScriptPath' << 'EOF'\n$scriptContent\nEOF")
        if (!writeResult.isSuccess) {
            return@withContext false
        }

        executeRootCommand("chmod 755 '$thermalScriptPath'")
        executeRootCommand("chown root:root '$thermalScriptPath'")

        return@withContext true
    }

    private suspend fun removeThermalScript(): Boolean = withContext(Dispatchers.IO) {
        if (!rootRepository.isRootStillAvailable()) {
            Log.e(TAG, "removeThermalScript: Root access is not available.")
            return@withContext false
        }
        val rmResult = executeRootCommand("rm -f '$thermalScriptPath'")
        if (rmResult.isSuccess) {
            Log.i(TAG, "removeThermalScript: Successfully removed $thermalScriptPath")
            return@withContext true
        } else {
            Log.e(TAG, "removeThermalScript: Failed to remove $thermalScriptPath. Error: ${rmResult.err.joinToString("\n")}")
            return@withContext false
        }
    }

    private suspend fun restoreLastThermalMode() {
        try {
            val lastMode = thermalDataStore.data.first()[LAST_THERMAL_MODE] ?: 0
            if (lastMode != 0) {
                setThermalModeIndex(lastMode).collect { success ->
                    if (success) {
                        Log.i(TAG, "Successfully restored thermal mode: $lastMode")
                    } else {
                        Log.e(TAG, "Failed to restore thermal mode: $lastMode")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to restore last thermal mode", e)
        }
    }

    private suspend fun saveLastThermalMode(modeIndex: Int) {
        try {
            thermalDataStore.edit { preferences ->
                preferences[LAST_THERMAL_MODE] = modeIndex
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save last thermal mode", e)
        }
    }

    fun getCurrentThermalModeIndex(): Flow<Int> = flow {
        val output = readRootCommand("cat '$thermalSysfsNode'")
        val value = output?.toIntOrNull() ?: -1
        emit(value)
    }.flowOn(Dispatchers.IO)

    fun setThermalModeIndex(modeIndex: Int): Flow<Boolean> = flow {
        if (availableThermalProfiles.none { it.index == modeIndex }) {
            Log.w(TAG, "setThermalModeIndex: Mode index $modeIndex is not supported.")
            emit(false)
            return@flow
        }

        if (!rootRepository.isRootStillAvailable()) {
            Log.e(TAG, "setThermalModeIndex: Root access is not available.")
            emit(false)
            return@flow
        }

        monitoringJob?.cancel()
        monitoringJob = null

        // Set SELinux context and permissions to allow write
        executeRootCommand("chcon u:object_r:sysfs_thermal:s0 $thermalSysfsNode")
        executeRootCommand("chmod 0666 '$thermalSysfsNode'")
        
        val writeOk = executeRootCommand("echo $modeIndex > '$thermalSysfsNode'").isSuccess
        
        // Lock with read-only permissions and do NOT restorecon to prevent system override
        executeRootCommand("chmod 0444 '$thermalSysfsNode'")

        if (!writeOk) {
            Log.e(TAG, "setThermalModeIndex: Failed to write $modeIndex to $thermalSysfsNode")
            emit(false)
            return@flow
        }

        if (modeIndex == 10) {
            monitoringJob = CoroutineScope(Dispatchers.IO).launch {
                while (isActive) {
                    val currentFreqStr = try {
                        rootRepository.run("cat /sys/devices/system/cpu/cpu7/cpufreq/scaling_max_freq").trim()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to read CPU max frequency", e)
                        ""
                    }
                    val currentFreq = currentFreqStr.toIntOrNull() ?: 0

                    val currentGov = try {
                        rootRepository.run("cat /sys/devices/system/cpu/cpu7/cpufreq/scaling_governor").trim()
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to read CPU governor", e)
                        ""
                    }

                    if (userSetMaxFreq > 0 && currentFreq != userSetMaxFreq) {
                        try {
                            rootRepository.run("echo $userSetMaxFreq > /sys/devices/system/cpu/cpu7/cpufreq/scaling_max_freq")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to restore CPU max frequency", e)
                        }
                    }
                    if (!userSetGovernor.isNullOrEmpty() && currentGov != userSetGovernor) {
                        try {
                            rootRepository.run("echo $userSetGovernor > /sys/devices/system/cpu/cpu7/cpufreq/scaling_governor")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to restore CPU governor", e)
                        }
                    }
                    delay(1000)
                }
            }
        }

        thermalDataStore.edit { preferences ->
            preferences[LAST_THERMAL_MODE] = modeIndex
        }

        val scriptOperationSuccess = if (modeIndex == 0) {
            removeThermalScript()
        } else {
            updateThermalScript(modeIndex)
        }

        if (!scriptOperationSuccess) {
            Log.w(TAG, "setThermalModeIndex: Failed to update/remove the thermal boot script.")
        }

        delay(300)

        val verifyValue = readRootCommand("cat '$thermalSysfsNode'")?.toIntOrNull() ?: -1
        val success = verifyValue == modeIndex

        if (success) {
            Log.i(TAG, "setThermalModeIndex: Successfully set and verified thermal mode $modeIndex")
        } else {
            Log.e(TAG, "setThermalModeIndex: Failed to verify thermal mode. Expected $modeIndex but got $verifyValue")
        }

        emit(success)
    }.flowOn(Dispatchers.IO)

    suspend fun setUserCPUSettings(maxFreq: Int, governor: String) {
        userSetMaxFreq = maxFreq
        userSetGovernor = governor
        withContext(Dispatchers.IO) {
            thermalDataStore.edit { preferences ->
                preferences[USER_MAX_FREQ] = maxFreq
                preferences[USER_GOVERNOR] = governor
            }
        }
    }

    private suspend fun restoreUserSettings() {
        val prefs = thermalDataStore.data.first()
        userSetMaxFreq = prefs[USER_MAX_FREQ] ?: 0
        userSetGovernor = prefs[USER_GOVERNOR]

        if (userSetMaxFreq > 0 || !userSetGovernor.isNullOrEmpty()) {
            val currentMode = readRootCommand("cat '$thermalSysfsNode'")?.toIntOrNull() ?: 0
            if (currentMode == 10) {
                setThermalModeIndex(10).collect()
            }
        }
    }

    init {
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            restoreUserSettings()
        }
    }

    fun getSupportedThermalProfiles(): Flow<List<ThermalProfile>> = flow {
        emit(availableThermalProfiles)
    }.flowOn(Dispatchers.IO)

    fun getCurrentThermalProfileName(currentIndex: Int): String {
        return availableThermalProfiles.find { it.index == currentIndex }?.displayName
            ?: if (currentIndex == -1) "Not Set"
            else "Unknown ($currentIndex)"
    }

    suspend fun getSavedThermalMode(): Int {
        return thermalDataStore.data.first()[LAST_THERMAL_MODE] ?: 0
    }
}