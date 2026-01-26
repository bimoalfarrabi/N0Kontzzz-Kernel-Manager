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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

private val Context.cpuSettingsStore: DataStore<Preferences> by preferencesDataStore(name = "cpu_settings")

@Singleton
class CPUManager @Inject constructor(
    private val context: Context
) {
    private val TAG = "CPUManager"

    companion object {
        private val CPU7_GOVERNOR = stringPreferencesKey("cpu7_governor")
        private val CPU7_MAX_FREQ = intPreferencesKey("cpu7_max_freq")
        private val CPU7_MIN_FREQ = intPreferencesKey("cpu7_min_freq")
    }

    private val persistentScriptPath = "/data/adb/post-fs-data.d/cpu_settings.sh"

    private suspend fun createPersistentScript(governor: String, maxFreq: Int, minFreq: Int): Boolean {
        val scriptContent = """
            #!/system/bin/sh
            
            # Wait for CPU sysfs
            while [ ! -d /sys/devices/system/cpu/cpu7 ]; do
                sleep 1
            done
            
            # Set SELinux contexts
            chcon -R u:object_r:sysfs_devices_system_cpu:s0 /sys/devices/system/cpu/cpu7/cpufreq/
            
            # Apply CPU settings
            echo $governor > /sys/devices/system/cpu/cpu7/cpufreq/scaling_governor
            echo $maxFreq > /sys/devices/system/cpu/cpu7/cpufreq/scaling_max_freq
            echo $minFreq > /sys/devices/system/cpu/cpu7/cpufreq/scaling_min_freq
            
            # Restore contexts
            restorecon -R /sys/devices/system/cpu/cpu7/cpufreq/
            
            exit 0
        """.trimIndent()

        Shell.cmd(
            "mkdir -p /data/adb/post-fs-data.d",
            "chmod 755 /data/adb/post-fs-data.d",
            "cat > '$persistentScriptPath' << 'EOF'\\n$scriptContent\\nEOF",
            "chmod 755 '$persistentScriptPath'",
            "chown root:root '$persistentScriptPath'"
        ).exec()

        return true
    }

    suspend fun setCPU7Settings(governor: String, maxFreq: Int, minFreq: Int = 300000): Boolean = withContext(Dispatchers.IO) {
        try {
            // Save to persistent storage first
            context.cpuSettingsStore.edit { prefs ->
                prefs[CPU7_GOVERNOR] = governor
                prefs[CPU7_MAX_FREQ] = maxFreq
                prefs[CPU7_MIN_FREQ] = minFreq
            }

            // Set SELinux context
            Shell.cmd("chcon -R u:object_r:sysfs_devices_system_cpu:s0 /sys/devices/system/cpu/cpu7/cpufreq/").exec()

            // Apply settings
            val result = Shell.cmd(
                "echo $governor > /sys/devices/system/cpu/cpu7/cpufreq/scaling_governor",
                "echo $maxFreq > /sys/devices/system/cpu/cpu7/cpufreq/scaling_max_freq",
                "echo $minFreq > /sys/devices/system/cpu/cpu7/cpufreq/scaling_min_freq"
            ).exec()

            // Restore context
            Shell.cmd("restorecon -R /sys/devices/system/cpu/cpu7/cpufreq/").exec()

            // Create persistence script
            createPersistentScript(governor, maxFreq, minFreq)

            return@withContext result.isSuccess
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set CPU7 settings", e)
            return@withContext false
        }
    }

    suspend fun getCurrentCPU7Settings(): Triple<String, Int, Int> = withContext(Dispatchers.IO) {
        val prefs = context.cpuSettingsStore.data.first()
        return@withContext Triple(
            prefs[CPU7_GOVERNOR] ?: "schedutil",
            prefs[CPU7_MAX_FREQ] ?: 2246000,
            prefs[CPU7_MIN_FREQ] ?: 300000
        )
    }

    suspend fun restoreCPU7Settings() {
        val (governor, maxFreq, minFreq) = getCurrentCPU7Settings()
        setCPU7Settings(governor, maxFreq, minFreq)
    }

    fun observeCPU7Settings(): Flow<Triple<String, Int, Int>> = flow {
        context.cpuSettingsStore.data.collect { prefs ->
            emit(Triple(
                prefs[CPU7_GOVERNOR] ?: "schedutil",
                prefs[CPU7_MAX_FREQ] ?: 2246000,
                prefs[CPU7_MIN_FREQ] ?: 300000
            ))
        }
    }.flowOn(Dispatchers.IO)
}
