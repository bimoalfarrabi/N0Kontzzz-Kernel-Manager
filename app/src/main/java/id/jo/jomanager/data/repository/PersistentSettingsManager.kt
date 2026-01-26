package id.jo.jomanager.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.persistentSettingsStore: DataStore<Preferences> by preferencesDataStore(name = "persistent_settings")

@Singleton
class PersistentSettingsManager @Inject constructor(
    private val context: Context
) {
    private val TAG = "PersistentSettingsManager"

    companion object {
        private val THERMAL_MODE = intPreferencesKey("thermal_mode")
        private val CPU7_GOVERNOR = stringPreferencesKey("cpu7_governor")
        private val CPU7_MAX_FREQ = intPreferencesKey("cpu7_max_freq")
        private val LAST_KNOWN_STATE = stringPreferencesKey("last_known_state")
    }

    private fun getCurrentGovernor(): String {
        return try {
            val result = Shell.cmd("cat /sys/devices/system/cpu/cpu7/cpufreq/scaling_governor").exec()
            if (result.isSuccess) {
                result.out.joinToString("").trim()
            } else {
                Shell.cmd("cat /sys/devices/system/cpu/cpu7/cpufreq/scaling_available_governors").exec()
                    .out.joinToString("").trim().split(" ").firstOrNull() ?: "schedutil"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading current governor", e)
            "schedutil" // Default fallback only if everything fails
        }
    }

    private fun getMaxFreqAvailable(): Int {
        return try {
            val result = Shell.cmd("cat /sys/devices/system/cpu/cpu7/cpufreq/cpuinfo_max_freq").exec()
            if (result.isSuccess) {
                result.out.joinToString("").trim().toIntOrNull() ?: getDefaultMaxFreq()
            } else {
                getDefaultMaxFreq()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading max frequency", e)
            getDefaultMaxFreq()
        }
    }

    private fun getDefaultMaxFreq(): Int {
        return try {
            // Try to read available frequencies and get the highest one
            val freqList = Shell.cmd("cat /sys/devices/system/cpu/cpu7/cpufreq/scaling_available_frequencies").exec()
            if (freqList.isSuccess) {
                freqList.out.joinToString("").trim()
                    .split(" ")
                    .mapNotNull { it.toIntOrNull() }
                    .maxOrNull() ?: 0
            } else {
                // If can't read available frequencies, try to read current max as last resort
                val currentMax = Shell.cmd("cat /sys/devices/system/cpu/cpu7/cpufreq/scaling_max_freq").exec()
                currentMax.out.joinToString("").trim().toIntOrNull() ?: 0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error determining default max frequency", e)
            0 // Return 0 to indicate error condition
        }
    }

    private fun getCurrentMaxFreq(): Int {
        return try {
            val result = Shell.cmd("cat /sys/devices/system/cpu/cpu7/cpufreq/scaling_max_freq").exec()
            if (result.isSuccess) {
                val current = result.out.joinToString("").trim().toIntOrNull()
                if (current != null && current > 0) {
                    current
                } else {
                    getMaxFreqAvailable()
                }
            } else {
                getMaxFreqAvailable()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading current max frequency", e)
            getMaxFreqAvailable()
        }
    }

    suspend fun saveThermalMode(mode: Int) {
        context.persistentSettingsStore.edit { preferences ->
            preferences[THERMAL_MODE] = mode
        }
    }

    suspend fun saveCpu7Settings(governor: String, maxFreq: Int) {
        context.persistentSettingsStore.edit { preferences ->
            preferences[CPU7_GOVERNOR] = governor
            preferences[CPU7_MAX_FREQ] = maxFreq
        }
    }

    suspend fun getLastThermalMode(): Int {
        // Get current thermal mode from device
        val currentMode = try {
            val result = Shell.cmd("cat /sys/class/thermal/thermal_message/sconfig").exec()
            if (result.isSuccess) {
                result.out.joinToString("").trim().toIntOrNull() ?: 10
            } else {
                10
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading current thermal mode", e)
            10
        }

        return context.persistentSettingsStore.data.first()[THERMAL_MODE] ?: currentMode
    }

    suspend fun getCpu7Settings(): Pair<String, Int> {
        val currentGovernor = getCurrentGovernor()
        val currentMaxFreq = getCurrentMaxFreq()
        val maxAvailable = getMaxFreqAvailable()

        val prefs = context.persistentSettingsStore.data.first()
        return Pair(
            prefs[CPU7_GOVERNOR] ?: currentGovernor,
            (prefs[CPU7_MAX_FREQ] ?: currentMaxFreq).coerceAtMost(maxAvailable)
        )
    }

    suspend fun applyLastKnownSettings() {
        try {
            val thermalMode = getLastThermalMode()
            val (governor, maxFreq) = getCpu7Settings()

            // Apply CPU settings first
            if (Shell.isAppGrantedRoot() == true) {
                Shell.cmd(
                    "echo $governor > /sys/devices/system/cpu/cpu7/cpufreq/scaling_governor",
                    "echo $maxFreq > /sys/devices/system/cpu/cpu7/cpufreq/scaling_max_freq"
                ).exec()

                // Apply thermal mode last to ensure it doesn't override CPU settings
                Shell.cmd("echo $thermalMode > /sys/class/thermal/thermal_message/sconfig").exec()

            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to apply last known settings", e)
        }
    }

    fun observeSettings(): Flow<Triple<Int, String, Int>> {
        return context.persistentSettingsStore.data.map { preferences ->
            Triple(
                preferences[THERMAL_MODE] ?: getLastThermalMode(),
                preferences[CPU7_GOVERNOR] ?: getCurrentGovernor(),
                preferences[CPU7_MAX_FREQ] ?: getCurrentMaxFreq()
            )
        }
    }
}
