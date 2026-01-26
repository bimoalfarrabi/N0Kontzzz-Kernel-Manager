package id.jo.jomanager.data.repository

import android.app.ActivityManager
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import id.jo.jomanager.data.model.MemoryInfo
import id.jo.jomanager.data.model.RealtimeAggregatedInfo
import id.jo.jomanager.data.model.RealtimeCpuInfo
import id.jo.jomanager.data.model.RealtimeGpuInfo
import id.jo.jomanager.data.model.SystemInfo

import id.jo.jomanager.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.channels.awaitClose // Diperlukan untuk callbackFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.io.BufferedReader
import javax.inject.Inject
import javax.inject.Singleton
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.opengles.GL10


@Suppress("UNREACHABLE_CODE")
@Singleton
class SystemRepository @Inject constructor(
    private val context: Context,
    private val tuningRepository: TuningRepository,
    private val rootRepository: RootRepository,
) {

    companion object {
        private const val VALUE_NOT_AVAILABLE = "N/A"
        private const val VALUE_UNKNOWN = "Unknown"
        private const val REALTIME_UPDATE_INTERVAL_MS = 1000L
    }

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val systemInfoMutex = Mutex()
    private var cachedSystemInfo: SystemInfo? = null

    private suspend fun getCachedSystemInfo(): SystemInfo {
        // Menggunakan double-checked locking untuk thread-safety sederhana jika diakses dari coroutine berbeda
        // Meskipun dalam kasus ini, kemungkinan besar akan dipanggil dari scope callbackFlow yang sama.
        return cachedSystemInfo ?: systemInfoMutex.withLock {
            cachedSystemInfo ?: getSystemInfoInternal().also { cachedSystemInfo = it }
        }
    }

    private fun readFileToString(filePath: String, fileDescription: String, attemptSu: Boolean = true): String? {
        val file = File(filePath)
        try {
            if (file.exists() && file.canRead()) {
                val content = file.readText().trim()
                if (content.isNotBlank()) {
                    return content
                } else if (!attemptSu) {
                    return null
                }
            }
        } catch (_: SecurityException) {
        } catch (_: FileNotFoundException) {
        } catch (_: IOException) {
            return null
        } catch (_: Exception) {
            return null
        }

        if (attemptSu) {
            try {
                // Use the root repository for more reliable command execution
                val result = rootRepository.run("cat \"$filePath\"")
                if (result.isNotBlank()) {
                    return result.trim()
                }
            } catch (_: Exception) {
                return null
            }
        }
        return null
    }

    private fun writeStringToFile(filePath: String, content: String, fileDescription: String, attemptSu: Boolean = true): Boolean {
        val file = File(filePath)
        try {
            if (file.exists() && file.canWrite()) {
                file.writeText(content)
                return true
            } else if (!attemptSu) {
                return false
            }
        } catch (_: SecurityException) {
        } catch (_: FileNotFoundException) {
        } catch (_: IOException) {
            return false
        } catch (_: Exception) {
            return false
        }

        if (attemptSu) {
            try {
                // Use the root repository for more reliable command execution
                rootRepository.run("echo \"$content\" > \"$filePath\"")
                return true
            } catch (_: Exception) {
                return false
            }
        }
        return false
    }

    // Variabel untuk menyimpan data CPU sebelumnya untuk perhitungan load
    private var previousCpuData: List<LongArray>? = null

    private suspend fun getCpuRealtimeInternal(): RealtimeCpuInfo {
        val cores = Runtime.getRuntime().availableProcessors()
        val governor = readFileToString("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor", "CPU0 Governor") ?: VALUE_UNKNOWN

        val frequencies = List(cores) { coreIndex ->
            val onlineStr = readFileToString("/sys/devices/system/cpu/cpu$coreIndex/online", "CPU$coreIndex Online Status", attemptSu = false)
            val isOnline = onlineStr == null || onlineStr.trim() != "0"

            if (isOnline) {
                val freqStr = readFileToString("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/scaling_cur_freq", "CPU$coreIndex Current Freq")
                (freqStr?.toLongOrNull()?.div(1000))?.toInt() ?: 0
            } else {
                0
            }
        }

        val tempStr = readFileToString("/sys/class/thermal/thermal_zone0/temp", "Thermal Zone0 Temp")
        val temperature = (tempStr?.toFloatOrNull()?.div(1000)) ?: 0f // Asumsi temp dalam mili-Celsius

        // Menghitung CPU load percentage
        val cpuLoadPercentage = calculateCpuLoadPercentage()

        val systemInfo = getCachedSystemInfo() // Dapatkan info SoC

        return RealtimeCpuInfo(
            cores = cores,
            governor = governor,
            freqs = frequencies,
            temp = temperature,
            soc = systemInfo.soc, // Menambahkan kembali socModel
            cpuLoadPercentage = cpuLoadPercentage
        )
    }

    private fun calculateCpuLoadPercentage(): Float? {
        try {
            // Membaca data CPU dari /proc/stat
            val cpuStat = readFileToString("/proc/stat", "CPU Stat")?.lines()?.firstOrNull { it.startsWith("cpu ") }
            if (cpuStat == null) {
                return null
            }

            // Parsing data CPU
            val cpuData = cpuStat.trim().split("\\s+".toRegex()).drop(1).map { it.toLong() }.toLongArray()
            if (cpuData.size < 4) {
                return null
            }

            // Jika ini adalah pembacaan pertama, simpan data dan kembalikan null
            val previousData = previousCpuData
            if (previousData == null) {
                previousCpuData = listOf(cpuData)
                return null
            }

            // Menghitung perbedaan antara data saat ini dan sebelumnya
            val prevData = previousData.first()
            val diffData = LongArray(cpuData.size) { i -> cpuData[i] - prevData[i] }

            // Menghitung total waktu dan waktu idle
            val total = diffData.sum()
            val idle = diffData[3] + (if (diffData.size > 4) diffData[4] else 0) // idle + iowait

            // Menghitung persentase penggunaan CPU
            val usage = if (total > 0) ((total - idle).toDouble() / total.toDouble() * 100.0) else 0.0
            
            // Simpan data saat ini untuk perhitungan berikutnya
            previousCpuData = listOf(cpuData)

            return usage.toFloat().coerceIn(0f, 100f)
        } catch (_: Exception) {
            return null
        }
    }

    fun getCpuRealtime(): RealtimeCpuInfo {
        return runBlocking { getCpuRealtimeInternal() }
    }

    private fun getBatteryLevelFromApi(): Int {
        val intentFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val batteryStatusIntent =
            context.applicationContext.registerReceiver(null, intentFilter) ?: return -1
        val level = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = batteryStatusIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return if (level != -1 && scale != -1 && scale != 0) {
            (level / scale.toFloat() * 100).toInt()
        } else -1
    }

    private fun getBatteryInfoInternal(statusFromIntent: Int = -1): BatteryInfo {
        val batteryDir = "/sys/class/power_supply/battery"
        val batteryLevelStr = readFileToString("$batteryDir/capacity", "Battery Level Percent from File")
        val finalLevel = batteryLevelStr?.toIntOrNull() ?: getBatteryLevelFromApi().let { if (it == -1) 0 else it }

        var tempStr = readFileToString("$batteryDir/temp", "Battery Temperature")
        var tempSource = "$batteryDir/temp"
        if (tempStr == null) {
            val thermalZoneDirs = File("/sys/class/thermal/").listFiles { dir, name ->
                dir.isDirectory && name.startsWith("thermal_zone")
            }
            thermalZoneDirs?.sortedBy { it.name }?.forEach thermalLoop@{ zoneDir ->
                val type = readFileToString("${zoneDir.path}/type", "Thermal Zone Type (${zoneDir.name})", attemptSu = false)
                if (type != null && (type.contains("battery", ignoreCase = true) || type.contains("แบตเตอรี่") || type.contains("case_therm", ignoreCase = true) || type.contains("ibat_therm", ignoreCase = true))) {
                    tempStr = readFileToString("${zoneDir.path}/temp", "Battery Temperature from ${zoneDir.name} ($type)")
                    if (tempStr != null) {
                        tempSource = "${zoneDir.path}/temp (type: $type)"
                        return@thermalLoop
                    }
                }
            }
        }
        val finalTemperature = tempStr?.toFloatOrNull()?.let { rawTemp ->
            // Jika dari thermal_zone, biasanya dalam mili-Celsius, jika dari power_supply, bisa deci-Celsius
            if (rawTemp > 1000 && (tempSource.contains("thermal_zone") || tempSource.contains("temp_input"))) rawTemp / 1000 else rawTemp / 10
        } ?: 0f

        val cycleCountStr = readFileToString("$batteryDir/cycle_count", "Battery Cycle Count")
        val finalCycleCount = cycleCountStr?.toIntOrNull() ?: run {
            // Try alternative paths for cycle count
            val altCyclePaths = listOf(
                "/sys/class/power_supply/bms/cycle_count",
                "/sys/class/power_supply/battery/cycle_count_summary",
                "/proc/driver/battery_cycle",
                "/proc/battinfo"
            )

            for (altPath in altCyclePaths) {
                val altCycleStr = readFileToString(altPath, "Alternative Battery Cycle Count ($altPath)")
                val cycles = altCycleStr?.toIntOrNull()
                if (cycles != null && cycles > 0) {
                    return@run cycles
                }
            }
            0 // Default if no cycle count found
        }

        // Try multiple paths for battery capacity information
        val designCapacityUahStr = readFileToString("$batteryDir/charge_full_design", "Battery Design Capacity (uAh)")
        val designCapacityUah = designCapacityUahStr?.toLongOrNull() ?: run {
            // Try alternative paths for design capacity
            val altCapacityPaths = listOf(
                "/sys/class/power_supply/bms/charge_full_design",
                "/sys/class/power_supply/battery/energy_full_design",
                "/proc/driver/battery_capacity"
            )

            for (altPath in altCapacityPaths) {
                val altCapStr = readFileToString(altPath, "Alternative Battery Design Capacity ($altPath)")
                val cap = altCapStr?.toLongOrNull()
                if (cap != null && cap > 0) {
                    return@run cap
                }
            }
            null
        }

        val finalDesignCapacityMah = if (designCapacityUah != null && designCapacityUah > 0) {
            // Convert microAh to mAh, handle different units
            when {
                designCapacityUah > 10000000 -> (designCapacityUah / 1000).toInt() // µAh to mAh
                designCapacityUah > 10000 -> (designCapacityUah / 1000).toInt() // µAh to mAh
                else -> designCapacityUah.toInt() // Already in mAh
            }
        } else 0

        var calculatedSohPercentage = 0
        var currentCapacityMah = 0

        if (finalDesignCapacityMah > 0 && designCapacityUah != null) {
            val currentFullUahStr = readFileToString("$batteryDir/charge_full", "Battery Current Full Capacity (uAh)")
            val currentFullUah = currentFullUahStr?.toLongOrNull() ?: run {
                // Try alternative paths for current capacity
                val altCurrentCapPaths = listOf(
                    "/sys/class/power_supply/bms/charge_full",
                    "/sys/class/power_supply/battery/energy_full",
                    "/proc/driver/battery_current_capacity"
                )

                for (altPath in altCurrentCapPaths) {
                    val altCapStr = readFileToString(altPath, "Alternative Battery Current Capacity ($altPath)")
                    val cap = altCapStr?.toLongOrNull()
                    if (cap != null && cap > 0) {
                        return@run cap
                    }
                }
                null
            }

            if (currentFullUah != null && currentFullUah > 0) {
                // Convert microAh to mAh, handle different units
                currentCapacityMah = when {
                    currentFullUah > 10000000 -> (currentFullUah / 1000).toInt() // µAh to mAh
                    currentFullUah > 10000 -> (currentFullUah / 1000).toInt() // µAh to mAh
                    else -> currentFullUah.toInt() // Already in mAh
                }

                // Calculate real battery health percentage: (Current Capacity / Design Capacity) × 100%
                val sohDouble = (currentCapacityMah.toDouble() / finalDesignCapacityMah.toDouble()) * 100.0
                calculatedSohPercentage = sohDouble.toInt().coerceIn(0, 100)

            } else {
                // If we can't read current capacity, try to get health directly from system
                val healthPercentageStr = readFileToString("$batteryDir/health", "Direct Battery Health")
                calculatedSohPercentage = healthPercentageStr?.toIntOrNull()?.coerceIn(0, 100) ?: run {
                    // As last resort, try to estimate from BatteryManager health status
                    val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                    val health = batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN) ?: BatteryManager.BATTERY_HEALTH_UNKNOWN

                    when (health) {
                        BatteryManager.BATTERY_HEALTH_GOOD -> 100
                        BatteryManager.BATTERY_HEALTH_OVERHEAT -> 85
                        BatteryManager.BATTERY_HEALTH_COLD -> 90
                        BatteryManager.BATTERY_HEALTH_DEAD -> 0
                        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> 75
                        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> 50
                        else -> 100 // Default to 100% for unknown
                    }
                }
                currentCapacityMah = (finalDesignCapacityMah * calculatedSohPercentage / 100.0).toInt()
            }
        } else {
            // If no design capacity is available, try to get approximate values

            // Try to get some capacity info from BatteryManager properties
            val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val energyCounter = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)

            if (energyCounter != Int.MIN_VALUE && energyCounter > 0) {
                // Energy counter is in nWh, convert to approximate mAh
                // Assuming average voltage of 3.7V: mAh ≈ nWh / (3.7 * 1000000)
                val estimatedCapacityMah = (energyCounter / (3.7 * 1000000)).toInt()
                if (estimatedCapacityMah > 0) {
                    currentCapacityMah = estimatedCapacityMah
                    // Assume this is 100% health since we don't have design capacity
                    calculatedSohPercentage = 100
                }
            }
        }

        // Determine health status string based on percentage
        val healthStatus = when {
            calculatedSohPercentage >= 90 -> "Excellent"
            calculatedSohPercentage >= 80 -> "Good"
            calculatedSohPercentage >= 70 -> "Fair"
            calculatedSohPercentage >= 60 -> "Poor"
            calculatedSohPercentage > 0 -> "Critical"
            else -> "Unknown"
        }

        val voltagePaths = listOf(
            "$batteryDir/voltage_now",
            "$batteryDir/batt_vol",
            "$batteryDir/batt_voltage",
            "$batteryDir/battery_voltage",
            "/sys/class/power_supply/battery/voltage_now",
            "/sys/class/power_supply/bms/voltage_now",
            "/sys/class/power_supply/main/voltage_now",
            "/sys/class/power_supply/pm8921-bms/voltage_now"
        )
        var finalVoltage: Float? = null
        for (path in voltagePaths) {
            val voltageStr = readFileToString(path, "Battery Voltage from $path")
            if (voltageStr.isNullOrBlank()) continue

            val cleanedVoltage = buildString {
                for (ch in voltageStr) {
                    if (ch.isDigit() || ch == '.' || ch == '-') append(ch)
                }
            }

            val voltageValue = cleanedVoltage.toFloatOrNull()
            if (voltageValue != null && voltageValue > 0f) {
                finalVoltage = voltageValue
                break
            }
        }

        var finalCurrent: Float? = null
        val currentPaths = listOf(
            "$batteryDir/current_now",
            "$batteryDir/current_avg",
            "/sys/class/power_supply/bms/current_now",
            "/sys/class/power_supply/usb/current_now"
        )
        for (path in currentPaths) {
            val currentStr = readFileToString(path, "Battery Current from $path")
            if (currentStr != null) {
                finalCurrent = currentStr.toFloatOrNull()
                // Some kernels add extra characters, so let's be safe
                if (finalCurrent != null) {
                    break // Found a valid value, stop searching
                }
            }
        }

        // Prefer derived wattage from voltage/current; avoid rooting for power_now as banyak device tidak menyediakan
        val finalWattage = if (finalVoltage != null && finalCurrent != null) {
            // voltage_now commonly µV, current in µA; normalisasi ke W
            val v = if (finalVoltage > 10_000) finalVoltage / 1_000_000f else finalVoltage / 1_000_000f
            val i = finalCurrent / 1_000_000f
            (kotlin.math.abs(v * i))
        } else {
            val finalWattageStr = readFileToString("$batteryDir/power_now", "Battery Power Now", attemptSu = false)
            finalWattageStr?.toFloatOrNull()
        }

        val finalTechnology = readFileToString("$batteryDir/technology", "Battery Technology")

        val statusString = readFileToString("$batteryDir/status", "Battery Status")

        // Determine charging status
        val isCharging = when {
            // Prioritize the status from the broadcast intent if it's valid and not unknown
            statusFromIntent != -1 && statusFromIntent != BatteryManager.BATTERY_STATUS_UNKNOWN -> {
                statusFromIntent == BatteryManager.BATTERY_STATUS_CHARGING || statusFromIntent == BatteryManager.BATTERY_STATUS_FULL
            }
            // Then fall back to the file-based logic
            finalCurrent != null -> {
                finalCurrent < -1000f // Negative current means charging
            }
            else -> {
                statusString?.contains("Charging", ignoreCase = true) == true ||
                statusString?.contains("Full", ignoreCase = true) == true
            }
        }

        val finalStatus = when {
            statusString.isNullOrBlank() -> ""
            statusString.contains("Charging", ignoreCase = true) -> "Charging"
            statusString.contains("Discharging", ignoreCase = true) -> "Discharging"
            statusString.contains("Full", ignoreCase = true) -> "Full"
            else -> statusString
        }

        // Normalize current for display: positive for charging, negative for discharging
        val displayCurrent = finalCurrent?.let {
            val absCurrent = kotlin.math.abs(it)
            if (isCharging) absCurrent else -absCurrent
        } ?: 0f

        return BatteryInfo(
            level = finalLevel,
            temp = finalTemperature,
            voltage = finalVoltage ?: 0f,
            isCharging = isCharging,
            current = displayCurrent,
            chargingWattage = finalWattage ?: 0f,
            technology = finalTechnology ?: "Unknown",
            health = healthStatus, // Use the calculated health status
            status = finalStatus,
            chargingType = getChargingTypeFromStatus(statusString),
            powerSource = getChargingTypeFromStatus(statusString),
            healthPercentage = calculatedSohPercentage,
            cycleCount = finalCycleCount, // Use the actual cycle count
            capacity = finalDesignCapacityMah, // Use the actual design capacity
            currentCapacity = currentCapacityMah, // Use the actual current capacity
            plugged = 0
        )
    }

    fun getBatteryInfo(): BatteryInfo {
        return getBatteryInfoInternal()
    }

    private suspend fun getMemoryInfoInternal(): MemoryInfo {
        return try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
            val memoryInfo = android.app.ActivityManager.MemoryInfo()
            activityManager.getMemoryInfo(memoryInfo)

            // Fetch ZRAM and Swap details from TuningRepository
            val zramTotal = tuningRepository.getZramDisksize().firstOrNull() ?: 0L
            val zramUsed = tuningRepository.getZramUsed().firstOrNull() ?: 0L
            val swapTotal = tuningRepository.getSwapTotal().firstOrNull() ?: 0L
            val swapUsed = tuningRepository.getSwapUsed().firstOrNull() ?: 0L

            MemoryInfo(
                used = memoryInfo.totalMem - memoryInfo.availMem,
                total = memoryInfo.totalMem,
                free = memoryInfo.availMem,
                zramTotal = zramTotal,
                zramUsed = zramUsed,
                swapTotal = swapTotal,
                swapUsed = swapUsed
            )
        } catch (e: Exception) {
            MemoryInfo(0, 0, 0)
        }
    }

    fun getMemoryInfo(): MemoryInfo {
        return runBlocking { getMemoryInfoInternal() }
    }

    private suspend fun getGpuModel(): String {
        return try {
            val result = tuningRepository.getOpenGlesDriver().firstOrNull()
            
            if (result != null && result != "N/A" && result.isNotBlank()) {
                // Clean up the result first
                val cleanResult = result.trim()
                
                // The format is: "Qualcomm, Adreno (TM) 650, OpenGL ES 3.2..."
                // So we want the second part after splitting by comma
                val parts = cleanResult.split(",")
                if (parts.size >= 2) {
                    val gpuModel = parts[1].trim()
                    
                    // Clean up the GPU model
                    val cleanGpuModel = gpuModel
                        .replace("(TM)", "")
                        .replace("  ", " ")
                        .trim()
                    
                    return cleanGpuModel
                }
                
                // Fallback: if we can't parse properly, try the old method
                var gpuModel = cleanResult
                val commaIndex = cleanResult.indexOf(',')
                if (commaIndex != -1) {
                    gpuModel = cleanResult.take(commaIndex).trim()
                }
                
                // Remove common prefixes and clean up
                gpuModel = gpuModel
                    .replace("GLES:", "")
                    .replace("OpenGL ES", "")
                    .replace("(TM)", "")
                    .replace("  ", " ")
                    .trim()
                
                
                // If still too generic, fall back to default
                if (gpuModel.equals("Qualcomm", ignoreCase = true) || gpuModel.length < 5) {
                    return "Graphics Processing Unit (GPU)"
                }
                
                return gpuModel
            }
            "Graphics Processing Unit (GPU)"
        } catch (e: Exception) {
            "Graphics Processing Unit (GPU)"
        }
    }

    private var cachedGlVersion: String? = null

    private fun getDetailedGlVersion(): String {
        if (cachedGlVersion != null && cachedGlVersion!!.isNotEmpty()) return cachedGlVersion!!

        // 1. Try basic ActivityManager first as fallback
        var version = try {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.deviceConfigurationInfo.glEsVersion
        } catch (e: Exception) {
            ""
        }

        // 2. Try to get detailed string from EGL (e.g. "OpenGL ES 3.2 V@0530...")
        // We run this in a try-catch block to ensure safety as EGL calls can fail on some devices
        try {
            val egl = EGLContext.getEGL() as EGL10
            val display = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)

            if (display === EGL10.EGL_NO_DISPLAY) return version

            val versionArray = IntArray(2)
            if (!egl.eglInitialize(display, versionArray)) return version

            val configAttribs = intArrayOf(
                EGL10.EGL_RENDERABLE_TYPE, 4, // EGL_OPENGL_ES2_BIT
                EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT,
                EGL10.EGL_NONE
            )

            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfig = IntArray(1)
            egl.eglChooseConfig(display, configAttribs, configs, 1, numConfig)

            if (numConfig[0] == 0) return version
            val config = configs[0]

            val contextAttribs = intArrayOf(
                0x3098, 2, // EGL_CONTEXT_CLIENT_VERSION, 2
                EGL10.EGL_NONE
            )

            val ctx = egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, contextAttribs)

            if (ctx === EGL10.EGL_NO_CONTEXT) return version

            val surfAttribs = intArrayOf(
                EGL10.EGL_WIDTH, 1,
                EGL10.EGL_HEIGHT, 1,
                EGL10.EGL_NONE
            )
            val surface = egl.eglCreatePbufferSurface(display, config, surfAttribs)

            if (surface === EGL10.EGL_NO_SURFACE) {
                egl.eglDestroyContext(display, ctx)
                return version
            }

            if (!egl.eglMakeCurrent(display, surface, surface, ctx)) {
                egl.eglDestroySurface(display, surface)
                egl.eglDestroyContext(display, ctx)
                return version
            }

            val gl = ctx.gl as GL10
            val fullVersion = gl.glGetString(GL10.GL_VERSION)

            // Cleanup
            egl.eglMakeCurrent(display, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT)
            egl.eglDestroySurface(display, surface)
            egl.eglDestroyContext(display, ctx)
            egl.eglTerminate(display)

            if (!fullVersion.isNullOrBlank()) {
                version = fullVersion
            }
        } catch (e: Exception) {
            // EGL failed, keep using the basic version
        }

        cachedGlVersion = version
        return version
    }

    private suspend fun getGpuRealtimeInternal(): RealtimeGpuInfo {
        var currentFreq = 0
        var maxFreq = 0
        var usage = 0
        val gpuModel = getGpuModel()
        val glVersion = getDetailedGlVersion() // Use the detailed fetcher
        
        try {
            // Get current GPU frequency from TuningRepository
            currentFreq = tuningRepository.getCurrentGpuFreq().firstOrNull() ?: 0
            
            // Get max GPU frequency from TuningRepository
            val (_, max) = tuningRepository.getGpuFreq().firstOrNull() ?: (0 to 0)
            maxFreq = max
            
            // Get GPU usage from TuningRepository
            usage = tuningRepository.getGpuUsage().firstOrNull() ?: 0
        } catch (e: Exception) {
        }
        
        return RealtimeGpuInfo(
            usagePercentage = usage.toFloat(),
            currentFreq = currentFreq,
            maxFreq = maxFreq,
            model = gpuModel,
            glVersion = glVersion
        )
    }
    
    fun getGpuRealtime(): RealtimeGpuInfo {
        return runBlocking { getGpuRealtimeInternal() }
    }

    private fun getUptimeMillisInternal(): Long {
        return android.os.SystemClock.elapsedRealtime()
    }

    private fun getDeepSleepMillisInternal(): Long {
        val uptime = android.os.SystemClock.elapsedRealtime()
        val awakeTime = android.os.SystemClock.uptimeMillis()
        return uptime - awakeTime
    }

    @SuppressLint("DefaultLocale")
    private fun formatDuration(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
    fun getDeepSleepInfo(): DeepSleepInfo {
        return DeepSleepInfo(getUptimeMillisInternal(), getDeepSleepMillisInternal())
    }
    
    // Helper function to get awake time (approximation of screen on time)
    fun getAwakeTime(): Long {
        // uptimeMillis returns the time since boot that the CPU has been awake (not in deep sleep)
        // This includes screen on time and other awake periods
        return android.os.SystemClock.uptimeMillis()
    }

    private fun getSystemInfoInternal(): SystemInfo {

        // Improved SoC detection with multiple property sources
        var socName = VALUE_UNKNOWN
        var manufacturer: String? = null
        var model: String? = null
        
        try {
            // Try multiple property sources for SoC detection
            val socProperties = listOf(
                "ro.soc.manufacturer" to "ro.soc.model",
                "ro.hardware" to null,
                "ro.product.board" to null,
                "ro.chipname" to null,
                "ro.board.platform" to null,
                "vendor.product.cpu" to null
            )

            // Try each property pair
            for ((manufacturerProp, modelProp) in socProperties) {
                if (manufacturer.isNullOrBlank()) {
                    manufacturer = getSystemProperty(manufacturerProp)
                }

                if (modelProp != null && model.isNullOrBlank()) {
                    model = getSystemProperty(modelProp)
                }

                // If we have both, break early
                if (!manufacturer.isNullOrBlank() && !model.isNullOrBlank()) {
                    break
                }
            }

            // Additional fallback checks
            if (manufacturer.isNullOrBlank()) {
                manufacturer = getSystemProperty("ro.product.cpu.abi")?.let { abi ->
                    when {
                        abi.contains("arm64") || abi.contains("aarch64") -> "ARM"
                        abi.contains("x86") -> "Intel"
                        else -> null
                    }
                }
            }

            // Parse hardware string for additional info
            val hardware = getSystemProperty("ro.hardware")
            if (!hardware.isNullOrBlank()) {
                when {
                    hardware.startsWith("qcom", ignoreCase = true) -> {
                        if (manufacturer.isNullOrBlank()) manufacturer = "QTI"
                        if (model.isNullOrBlank()) model = hardware.uppercase()
                    }
                    hardware.contains("mtk", ignoreCase = true) || hardware.contains("mediatek", ignoreCase = true) -> {
                        if (manufacturer.isNullOrBlank()) manufacturer = "Mediatek"
                        if (model.isNullOrBlank()) model = hardware
                    }
                    hardware.contains("exynos", ignoreCase = true) -> {
                        if (manufacturer.isNullOrBlank()) manufacturer = "Samsung"
                        if (model.isNullOrBlank()) model = hardware
                    }
                }
            }

            if (!manufacturer.isNullOrBlank() && !model.isNullOrBlank()) {
                socName = when {
                    manufacturer.equals("QTI", ignoreCase = true) && model.equals("SM7475", ignoreCase = true) -> "Qualcomm® Snapdragon™ 7+ Gen 2"
                    manufacturer.equals("QTI", ignoreCase = true) && model.equals("SM8650", ignoreCase = true) -> "Qualcomm® Snapdragon™ 8 Gen 3"
                    manufacturer.equals("QTI", ignoreCase = true) && model.equals("SM8635", ignoreCase = true) -> "Qualcomm® Snapdragon™ 8s Gen 3"
                    manufacturer.equals("QTI", ignoreCase = true) && (model.equals("SDM845", ignoreCase = true) || model.equals("sdm845", ignoreCase = true)) -> "Qualcomm® Snapdragon™ 845"
                    manufacturer.equals("QTI", ignoreCase = true) && model.equals("SM8250", ignoreCase = true) -> "Qualcomm® Snapdragon™ 870"
                    manufacturer.equals("QTI", ignoreCase = true) && model.equals("SM8150", ignoreCase = true) -> "Qualcomm® Snapdragon™ 860"
                    manufacturer.equals("QTI", ignoreCase = true) && (model.equals("SM7435-AB", ignoreCase = true) || model.equals("SM7435", ignoreCase = true)) -> "Qualcomm® Snapdragon™ 7s Gen 2"
                    manufacturer.equals("QTI", ignoreCase = true) && (model.equals("SM8735", ignoreCase = true) || model.equals("sm8735", ignoreCase = true)) -> "Qualcomm® Snapdragon™ 8s Gen 4"
                    manufacturer.equals("QTI", ignoreCase = true) && (model.equals("SDM665", ignoreCase = true) || model.equals("sdm665", ignoreCase = true)) -> "Qualcomm® Snapdragon™ 665"
                    manufacturer.equals("QTI", ignoreCase = true) && (model.equals("SDM660", ignoreCase = true) || model.equals("sdm660", ignoreCase = true)) -> "Qualcomm® Snapdragon™ 660"
                    manufacturer.equals("QTI", ignoreCase = true) && model.equals("SM8750", ignoreCase = true) -> "Qualcomm® Snapdragon™ 8 Elite"
                    manufacturer.equals("Mediatek", ignoreCase = true) && (model.equals("MT6785V/CD", ignoreCase = true) || model.equals("MT6785", ignoreCase = true)) -> "MediaTek Helio G95"
                    manufacturer.equals("Mediatek", ignoreCase = true) && (model.equals("MT6877V/TTZA", ignoreCase = true) || model.equals("MT6877V", ignoreCase = true)) -> "MediaTek Dimensity 1080"
                    manufacturer.equals("Mediatek", ignoreCase = true) && model.equals("MT6833GP", ignoreCase = true) -> "MediaTek Dimensity 6080"
                    manufacturer.equals("Mediatek", ignoreCase = true) && model.equals("MT6769Z", ignoreCase = true) -> "MediaTek Helio G85"
                    manufacturer.equals("Mediatek", ignoreCase = true) && model.equals("MT6989W", ignoreCase = true) -> "MediaTek Dimensity 9300+"
                    else -> "$manufacturer $model"
                }
            } else if (!manufacturer.isNullOrBlank()) {
                socName = manufacturer
            } else if (!model.isNullOrBlank()) {
                socName = model
            }

        } catch (e: Exception) {
        }

        // Get actual display information
        val displayInfo = getDisplayInfo()

        return SystemInfo(
            model = Build.MODEL ?: VALUE_UNKNOWN,
            codename = Build.DEVICE ?: VALUE_UNKNOWN,
            androidVersion = Build.VERSION.RELEASE ?: VALUE_UNKNOWN,
            sdk = Build.VERSION.SDK_INT,
            fingerprint = Build.FINGERPRINT ?: VALUE_UNKNOWN,
            soc = socName,
            board = model ?: VALUE_UNKNOWN,
            screenResolution = displayInfo.resolution,
            displayTechnology = displayInfo.technology,
            refreshRate = displayInfo.refreshRate,
            screenDpi = displayInfo.dpi
        )
    }

    private fun getSystemProperty(property: String): String? {
        return try {
            val process = Runtime.getRuntime().exec("getprop $property")
            val result = BufferedReader(InputStreamReader(process.inputStream)).readLine()?.trim()
            process.waitFor()
            process.destroy()
            if (result.isNullOrBlank()) null else result
        } catch (e: Exception) {
            null
        }
    }

    private data class DisplayInfo(
        val resolution: String,
        val technology: String,
        val refreshRate: String,
        val dpi: String
    )

    private fun getDisplayInfo(): DisplayInfo {
        try {
            val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as android.view.WindowManager
            val resolution: String
            val dpi: String

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val metrics = windowManager.currentWindowMetrics
                val bounds = metrics.bounds
                resolution = "${bounds.width()}x${bounds.height()}"
                dpi = "${context.resources.configuration.densityDpi}"
            } else {
                @Suppress("DEPRECATION")
                val display = windowManager.defaultDisplay
                @Suppress("DEPRECATION")
                val metrics = android.util.DisplayMetrics()
                @Suppress("DEPRECATION")
                display.getRealMetrics(metrics)
                resolution = "${metrics.widthPixels}x${metrics.heightPixels}"
                dpi = "${metrics.densityDpi}"
            }

            // Get refresh rate - hardcoded to 60-120Hz
            val refreshRate = "60-120Hz"

            // Detect display technology - hardcoded to AMOLED
            val technology = "AMOLED"

            return DisplayInfo(resolution, technology, refreshRate, dpi)

        } catch (e: Exception) {
            return DisplayInfo(VALUE_UNKNOWN, "LCD", "60Hz", VALUE_UNKNOWN)
        }
    }



    private fun getChargingTypeFromStatus(statusString: String?): String {
        return when {
            statusString.isNullOrBlank() -> "Unknown"
            statusString.contains("Charging", ignoreCase = true) -> "Charging"
            statusString.contains("Full", ignoreCase = true) -> "Not Charging"
            statusString.contains("Discharging", ignoreCase = true) -> "Not Charging"
            else -> "Unknown"
        }
    }

    fun getSystemInfo(): SystemInfo {
        return runBlocking { getCachedSystemInfo() }
    }

    fun getBootId(): String? {
        return readFileToString("/proc/sys/kernel/random/boot_id", "Boot ID")
    }

    fun getKernelInfo(): KernelInfo {

        // Get kernel version
        val version = readFileToString("/proc/version", "Kernel Version")
            ?: Build.VERSION.RELEASE

        // Improved GKI type detection with version-based detection
        val gkiType = when {
            // Check for specific GKI patterns first (more specific)
            version.contains("gki", ignoreCase = true) ||
            version.contains("generic kernel image", ignoreCase = true) ||
            Build.VERSION.RELEASE.contains("gki", ignoreCase = true) -> "Generic Kernel Image (GKI)"

            // Check for Android Common Kernel patterns
            version.contains("android-mainline", ignoreCase = true) ||
            version.contains("android-common", ignoreCase = true) -> "Android Common Kernel (ACK)"

            // GKI version detection based on Linux kernel version
            version.contains("Linux version", ignoreCase = true) -> {
                // Extract kernel version number
                val versionRegex = """Linux version (\d+\.\d+)""".toRegex()
                val kernelVersionMatch = versionRegex.find(version)
                val kernelVersion = kernelVersionMatch?.groupValues?.get(1)?.toFloatOrNull()

                when {
                    kernelVersion != null && kernelVersion >= 6.6f -> "Generic Kernel Image (GKI 2.0)"
                    kernelVersion != null && kernelVersion >= 5.15f -> "Generic Kernel Image (GKI 2.0)"
                    kernelVersion != null && kernelVersion >= 5.10f -> "Generic Kernel Image (GKI 2.0)"
                    kernelVersion != null && kernelVersion >= 5.4f -> "Generic Kernel Image (GKI 1.0)"
                    kernelVersion != null && kernelVersion >= 4.19f &&
                    (version.contains("android", ignoreCase = true) ||
                     Build.VERSION.SDK_INT >= 29) -> "Generic Kernel Image (GKI)"
                    version.contains("android", ignoreCase = true) -> "Android Kernel"
                    else -> "Linux Kernel $kernelVersion"
                }
            }

            // Check build fingerprint for additional clues
            Build.FINGERPRINT.contains("gki", ignoreCase = true) -> "Generic Kernel Image (GKI)"

            // Fallback check for android (less specific) - moved to lower priority
            version.contains("android", ignoreCase = true) -> "Android Kernel"

            else -> "Custom/OEM Kernel"
        }

        // Get scheduler information with better fallback paths
        val scheduler = readFileToString("/sys/block/sda/queue/scheduler", "I/O Scheduler")
            ?.let { schedulerLine ->
                // Extract currently active scheduler (marked with brackets)
                val activeSchedulerRegex = """\[([^]]+)]""".toRegex()
                activeSchedulerRegex.find(schedulerLine)?.groupValues?.get(1) ?: schedulerLine.trim()
            } ?: run {
                // Try alternative block devices
                val alternativeDevices = listOf("mmcblk0", "nvme0n1", "sdb", "sdc")
                for (device in alternativeDevices) {
                    val altScheduler = readFileToString("/sys/block/$device/queue/scheduler", "I/O Scheduler ($device)")
                    if (altScheduler != null) {
                        val activeSchedulerRegex = """\[([^]]+)]""".toRegex()
                        val result = activeSchedulerRegex.find(altScheduler)?.groupValues?.get(1) ?: altScheduler.trim()
                        if (result.isNotBlank()) return@run result
                    }
                }
                "Unknown"
            }

        // Get SELinux status
        val selinuxStatus = readFileToString("/sys/fs/selinux/enforce", "SELinux Status")
            ?.let { enforceValue ->
                when (enforceValue.trim()) {
                    "1" -> "Enforcing"
                    "0" -> "Permissive"
                    else -> "Unknown"
                }
            } ?: run {
            // Fallback: try getenforce command
            try {
                val process = Runtime.getRuntime().exec("getenforce")
                val result = BufferedReader(InputStreamReader(process.inputStream)).readLine()?.trim()
                process.waitFor()
                process.destroy()
                result ?: "Unknown"
            } catch (e: Exception) {
                "Unknown"
            }
        }

        // Get ABI
        val abi = Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"

        // Get architecture
        val architecture = when {
            abi.contains("arm64") || abi.contains("aarch64") -> "ARM64"
            abi.contains("arm") -> "ARM"
            abi.contains("x86_64") -> "x86_64"
            abi.contains("x86") -> "x86"
            else -> abi
        }

        // Get WireGuard Version
        val wireguardVersion = readFileToString("/sys/module/wireguard/version", "WireGuard Version")

        // Enhanced KernelSU detection - improved with better logging and error handling
        val kernelSuStatus = when {
            // Method 1: Check kernel version for KernelSU signature (primary method)
            version.contains("KernelSU", ignoreCase = true) -> {
                // Extract KernelSU version if available
                val ksuVersionRegex = """KernelSU[ -]?v?(\d+\.\d+\.\d+)""".toRegex()
                val ksuMatch = ksuVersionRegex.find(version)
                if (ksuMatch != null) {
                    "Active (${ksuMatch.groupValues[1]})"
                } else {
                    "Active"
                }
            }

            // Method 2: Check KernelSU directory
            File("/data/adb/ksu").exists() -> "Active"

            // Method 3: Check for KernelSU binary
            File("/system/bin/ksu").exists() -> "Active"

            // Method 4: Try various detection methods
            else -> {
                // Helper function for additional KernelSU checks
                fun checkOtherKsuMethods(): String {
                    // Check kernel cmdline
                    val cmdline = readFileToString("/proc/cmdline", "Kernel Command Line")
                    if (cmdline?.contains("ksu", ignoreCase = true) == true) {
                        return "Active"
                    }

                    // Check for KernelSU manager app
                    try {
                        context.packageManager.getPackageInfo("me.weishu.kernelsu", 0)
                        return "Detected (Manager Installed)"
                    } catch (e: Exception) {
                        // Ignore and continue
                    }

                    // Check system properties
                    if (getSystemProperty("ro.kernel.su")?.isNotEmpty() == true) {
                        return "Active"
                    }

                    // Default case - not detected
                    return "Not Detected"
                }

                // Helper: cek keberadaan binary di PATH atau path absolut
                fun binaryExists(cmd: String): Boolean {
                    return try {
                        if (cmd.contains("/")) {
                            File(cmd).exists()
                        } else {
                            val common = listOf("/system/bin/$cmd", "/system/xbin/$cmd", "/vendor/bin/$cmd")
                            if (common.any { File(it).exists() }) return true
                            val p = Runtime.getRuntime().exec(arrayOf("sh", "-c", "command -v $cmd"))
                            val out = BufferedReader(InputStreamReader(p.inputStream)).readLine()?.trim()
                            val code = p.waitFor()
                            p.destroy()
                            code == 0 && !out.isNullOrEmpty()
                        }
                    } catch (_: Exception) {
                        false
                    }
                }

                // Enhanced function to execute KernelSU commands dengan error handling lebih aman
                fun executeKsuCommand(command: Array<String>, description: String): String? {
                    var process: Process? = null
                    try {
                        // Hindari IOException: No such file or directory saat binary tidak ada
                        if (command.isNotEmpty()) {
                            val bin = command[0]
                            val notFound = when {
                                bin == "ksu" -> !binaryExists("ksu")
                                bin.startsWith("/") -> !File(bin).exists()
                                else -> false
                            }
                            if (notFound) {
                                return null
                            }
                        }

                        process = Runtime.getRuntime().exec(command)
                        val reader = BufferedReader(InputStreamReader(process.inputStream))
                        val errorReader = BufferedReader(InputStreamReader(process.errorStream))

                        val output = StringBuilder()
                        val errorOutput = StringBuilder()
                        var line: String?

                        // Read output
                        while (reader.readLine().also { line = it } != null) {
                            output.append(line).append("\n")
                        }

                        // Read error stream
                        while (errorReader.readLine().also { line = it } != null) {
                            errorOutput.append(line).append("\n")
                        }

                        val exitCode = process.waitFor()

                        if (errorOutput.isNotEmpty()) {
                            Log.w("SystemRepository", "KSU Command Error: $errorOutput")
                        }

                        reader.close()
                        errorReader.close()

                        if (exitCode == 0) {
                            val result = output.toString().trim()
                            return result.ifBlank { null }
                        }

                    } catch (e: Exception) {
                        // Jangan spam stacktrace untuk ENOENT; cukup log ringkas
                    } finally {
                        process?.destroy()
                    }
                    return null
                }

                // Try ksu -V command first
                val ksuVOutput = executeKsuCommand(arrayOf("ksu", "-V"), "ksu -V")
                if (ksuVOutput != null) {
                    "Active ($ksuVOutput)"
                } else {
                    // Try su -c "ksu -V" command
                    val suKsuVOutput = executeKsuCommand(arrayOf("su", "-c", "ksu -V"), "su -c ksu -V")
                    if (suKsuVOutput != null) {
                        "Active ($suKsuVOutput)"
                    } else {
                        // Try /data/adb/ksud --version command
                        val ksudOutput = executeKsuCommand(arrayOf("su", "-c", "/data/adb/ksud --version"), "su -c /data/adb/ksud --version")
                        if (ksudOutput != null) {
                            "Active ($ksudOutput)"
                        } else {
                            // Try alternative ksud paths
                            val alternativeKsudPaths = listOf(
                                "/data/adb/ksud version",
                                "/data/adb/ksu/bin/ksud --version",
                                "/data/adb/modules/kernelsu/bin/ksud --version"
                            )

                            var foundOutput: String? = null
                            for (ksudPath in alternativeKsudPaths) {
                                val altOutput = executeKsuCommand(arrayOf("su", "-c", ksudPath), "su -c $ksudPath")
                                if (altOutput != null) {
                                    foundOutput = altOutput
                                    break
                                }
                            }

                            if (foundOutput != null) {
                                "Active ($foundOutput)"
                            } else {
                                // Check if we can find ksud binary directly
                                val ksudPaths = listOf(
                                    "/data/adb/ksud",
                                    "/data/adb/ksu/bin/ksud",
                                    "/data/adb/modules/kernelsu/bin/ksud"
                                )

                                var binaryFound = false
                                for (ksudPath in ksudPaths) {
                                    if (File(ksudPath).exists()) {
                                        binaryFound = true
                                        break
                                    }
                                }

                                if (binaryFound) {
                                    "Active (Binary Found)"
                                } else {
                                    // Final fallback checks
                                    checkOtherKsuMethods()
                                }
                            }
                        }
                    }
                }
            }
        }

        return KernelInfo(
            version = version,
            gkiType = gkiType,
            scheduler = scheduler,
            selinuxStatus = selinuxStatus,
            abi = abi,
            architecture = architecture,
            kernelSuStatus = kernelSuStatus,
            fingerprint = Build.FINGERPRINT,
            wireguardVersion = wireguardVersion
        )
    }

    // Helper function to determine which KGSL path is available
    private fun getAvailableKgslPath(): String? {
        val paths = listOf(
            "/sys/kernel/e404/kgsl_skip_zeroing",
            "/sys/kernel/lunar_attributes/kgsl_skip_zeroing",
            "/sys/kernel/lunar_attributes/lunar_kgsl_skip_zeroing",
            "/sys/kernel/n0kz_attributes/kgsl_skip_zeroing",
            "/sys/kernel/n0kz_attributes/n0kz_kgsl_skip_zeroing",
            "/sys/kernel/fusionx_attributes/fusionx_kgsl_skip_zeroing",
            "/sys/kernel/fusionx_attributes/kgsl_skip_zeroing"
        )
        
        for (path in paths) {
            try {
                val file = File(path)
                // Check if file exists
                if (file.exists()) {
                    return path
                }
            } catch (e: Exception) {
                // Continue to check the next path
            }
        }
        
        // If direct file check fails, try to read the file using SU
        for (path in paths) {
            try {
                val value = readFileToString(path, "KGSL Skip Pool Zeroing", false)
                if (value != null) {
                    return path
                }
            } catch (e: Exception) {
                // Continue to check the next path
            }
        }
        
        return null
    }

    fun getKgslSkipZeroing(): Boolean {
        val path = getAvailableKgslPath()
        if (path != null) {
            val value = readFileToString(path, "KGSL Skip Pool Zeroing")
            return parseKgslSkipZeroingValue(value)
        }
        return false
    }

    fun setKgslSkipZeroing(enabled: Boolean): Boolean {
        val path = getAvailableKgslPath()
        if (path != null) {
            val value = if (enabled) "1" else "0"
            return writeStringToFile(path, value, "KGSL Skip Pool Zeroing")
        }
        return false
    }

    fun isKgslFeatureAvailable(): Boolean {
        // Try to get an available path
        val path = getAvailableKgslPath()
        if (path != null) {
            return true
        }
        
        // If no path found, try to actually read the value to see if feature is available
        // This handles cases where file exists but needs SU permission to access
        try {
            val paths = listOf(
                "/sys/kernel/e404/kgsl_skip_zeroing",
                "/sys/kernel/lunar_attributes/kgsl_skip_zeroing",
                "/sys/kernel/n0kz_attributes/kgsl_skip_zeroing",
                "/sys/kernel/n0kz_attributes/n0kz_kgsl_skip_zeroing",
                "/sys/kernel/fusionx_attributes/fusionx_kgsl_skip_zeroing"
            )
            
            for (path in paths) {
                val value = readFileToString(path, "KGSL Skip Pool Zeroing")
                if (value != null) {
                    return true
                }
            }
        } catch (e: Exception) {
            // Continue if reading fails
        }
        
        return false
    }

    // Helper function for testing
    fun parseKgslSkipZeroingValue(value: String?): Boolean {
        return value?.toIntOrNull() == 1
    }

    private fun getAvailableAvoidDirtyPtePath(): String? {
        val paths = listOf(
            "/sys/kernel/n0kz_attributes/avoid_dirty_pte",
            "/sys/kernel/e404/avoid_dirty_pte"
        )
        for (path in paths) {
            if (File(path).exists()) return path
        }
        for (path in paths) {
            if (readFileToString(path, "Avoid Dirty PTE Check", false) != null) return path
        }
        return null
    }

    fun isAvoidDirtyPteAvailable(): Boolean {
        return getAvailableAvoidDirtyPtePath() != null
    }

    fun getAvoidDirtyPte(): Boolean {
        val path = getAvailableAvoidDirtyPtePath()
        if (path != null) {
            val value = readFileToString(path, "Avoid Dirty PTE Status")
            return value?.trim() == "1"
        }
        return false
    }

    fun setAvoidDirtyPte(enabled: Boolean): Boolean {
        val path = getAvailableAvoidDirtyPtePath()
        if (path != null) {
            val value = if (enabled) "1" else "0"
            // Use generic robust write approach
            try {
                // Ensure writable
                rootRepository.run("chmod 666 $path")
                // Write
                rootRepository.run("echo \"$value\" > \"$path\"")
                // Restore/Lock permissions (read-only to prevent reset)
                rootRepository.run("chmod 444 $path")
                return true
            } catch (e: Exception) {
                // Fallback to basic writeStringToFile if root/complex commands fail
                return writeStringToFile(path, value, "Avoid Dirty PTE")
            }
        }
        return false
    }

    private val bypassChargingPath = "/sys/class/power_supply/battery/input_suspend"

    fun isBypassChargingAvailable(): Boolean {
        val file = File(bypassChargingPath)
        if (file.exists()) {
            return true
        }
        // If the file doesn't exist directly, try reading it with root.
        // readFileToString will return null if the file doesn't exist even with root.
        return readFileToString(bypassChargingPath, "Bypass Charging Status Check") != null
    }

    fun getBypassCharging(): Boolean {
        val value = readFileToString(bypassChargingPath, "Bypass Charging Status")
        return value?.trim() == "1"
    }

    fun setBypassCharging(enabled: Boolean): Boolean {
        val value = if (enabled) "1" else "0"
        return writeStringToFile(bypassChargingPath, value, "Bypass Charging")
    }

    private val forceFastChargePath = "/sys/kernel/fast_charge/force_fast_charge"

    fun isForceFastChargeAvailable(): Boolean {
        val file = File(forceFastChargePath)
        if (file.exists()) {
            return true
        }
        return readFileToString(forceFastChargePath, "Force Fast Charge Check") != null
    }

    fun getForceFastCharge(): Boolean {
        val value = readFileToString(forceFastChargePath, "Force Fast Charge Status")
        return value?.trim() == "1"
    }

    fun setForceFastCharge(enabled: Boolean): Boolean {
        val value = if (enabled) "1" else "0"
        return writeStringToFile(forceFastChargePath, value, "Force Fast Charge")
    }

    // TCP Congestion Control Algorithm functions

    private fun getCurrentTcpCongestionAlgorithm(): String? {
        return readFileToString("/proc/sys/net/ipv4/tcp_congestion_control", "TCP Congestion Control Algorithm")
    }

    private fun getAvailableTcpCongestionAlgorithms(): List<String> {
        val available = readFileToString("/proc/sys/net/ipv4/tcp_available_congestion_control", "Available TCP Congestion Control Algorithms")
        // Notice: here we use a regular space, and not double-escaped
        return available?.split("\\s+".toRegex())?.filter { it.isNotBlank() } ?: emptyList()
    }

    fun getTcpCongestionAlgorithm(): String {
        return getCurrentTcpCongestionAlgorithm() ?: "Unknown"
    }

    fun setTcpCongestionAlgorithm(algorithm: String): Boolean {
        // First check if the algorithm is available
        val availableAlgorithms = getAvailableTcpCongestionAlgorithms()
        if (!availableAlgorithms.contains(algorithm)) {
            return false
        }

        return writeStringToFile("/proc/sys/net/ipv4/tcp_congestion_control", algorithm, "TCP Congestion Control Algorithm")
    }

    fun getAvailableTcpCongestionAlgorithmsList(): List<String> {
        return getAvailableTcpCongestionAlgorithms()
    }

    // GPU Throttling functions
    private fun getGpuThrottlingStatus(): Boolean? {
        val result = readFileToString("/sys/class/kgsl/kgsl-3d0/throttling", "GPU Throttling Status")
        return when (result?.trim()) {
            "1", "Y", "yes", "on", "enabled" -> true
            "0", "N", "no", "off", "disabled" -> false
            else -> null
        }
    }

    fun isGpuThrottlingEnabled(): Boolean {
        return getGpuThrottlingStatus() ?: false
    }

    fun setGpuThrottling(enabled: Boolean): Boolean {
        val value = if (enabled) "1" else "0"
        return writeStringToFile("/sys/class/kgsl/kgsl-3d0/throttling", value, "GPU Throttling")
    }

    // I/O Scheduler functions
    private fun getCurrentIoScheduler(): String {
        // Try multiple possible paths for different devices
        val paths = listOf(
            "/sys/block/sda/queue/scheduler",  // Common path
            "/sys/block/mmcblk0/queue/scheduler",  // Alternative for some devices
            "/sys/block/sdb/queue/scheduler",  // USB storage
            "/sys/block/nvme0n1/queue/scheduler"  // NVMe storage
        )
        
        for (path in paths) {
            val result = readFileToString(path, "I/O Scheduler from $path")
            if (result != null) {
                // Find the active scheduler (the one in brackets [scheduler])
                val activeMatch = Regex("""\[(\w+)]""").find(result)
                return activeMatch?.groupValues?.get(1) ?: "N/A"
            }
        }
        return "N/A"
    }

    private fun getAvailableIoSchedulers(): List<String> {
        // Try multiple possible paths for different devices
        val paths = listOf(
            "/sys/block/sda/queue/scheduler",
            "/sys/block/mmcblk0/queue/scheduler",
            "/sys/block/sdb/queue/scheduler",
            "/sys/block/nvme0n1/queue/scheduler"
        )
        
        for (path in paths) {
            val result = readFileToString(path, "Available I/O Schedulers from $path")
            if (result != null) {
                // Extract all schedulers, removing brackets from the active one
                val schedulers = Regex("""\[(\w+)]|(\w+)""")
                    .findAll(result)
                    .map { matchResult ->
                        val active = matchResult.groupValues[1]
                        val inactive = matchResult.groupValues[2]
                        active.ifEmpty { inactive }
                    }
                    .filter { it.isNotEmpty() }
                    .toList()
                return schedulers
            }
        }
        return emptyList()
    }

    fun getIoScheduler(): String {
        return getCurrentIoScheduler()
    }

    fun setIoScheduler(scheduler: String): Boolean {
        // First, verify that the scheduler is available
        val availableSchedulers = getAvailableIoSchedulers()
        if (!availableSchedulers.contains(scheduler)) {
            return false
        }
        
        // Try multiple possible paths for different devices
        val paths = listOf(
            "/sys/block/sda/queue/scheduler",
            "/sys/block/mmcblk0/queue/scheduler", 
            "/sys/block/sdb/queue/scheduler",
            "/sys/block/nvme0n1/queue/scheduler"
        )
        
        for (path in paths) {
            // Verify the path exists and is writable
            val testResult = readFileToString(path, "Testing I/O Scheduler Path $path")
            if (testResult != null) {
                return writeStringToFile(path, scheduler, "I/O Scheduler Setting")
            }
        }
        
        return false
    }

    fun getAvailableIoSchedulersList(): List<String> {
        return getAvailableIoSchedulers()
    }

    fun getCpuClusters(): List<CpuCluster> {

        val clusters = mutableListOf<CpuCluster>()
        val cores = Runtime.getRuntime().availableProcessors()

        // Group cores by their frequency ranges to identify clusters
        val coreFreqRanges = mutableMapOf<Int, Pair<Int, Int>>() // core -> (min, max)
        val coreGovernors = mutableMapOf<Int, String>() // core -> governor
        val coreAvailableGovernors = mutableMapOf<Int, List<String>>() // core -> available governors

        for (coreIndex in 0 until cores) {
            val minFreqStr = readFileToString("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/cpuinfo_min_freq", "CPU$coreIndex Min Freq")
            val maxFreqStr = readFileToString("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/cpuinfo_max_freq", "CPU$coreIndex Max Freq")
            val governor = readFileToString("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/scaling_governor", "CPU$coreIndex Governor")
            val availableGovernorsStr = readFileToString("/sys/devices/system/cpu/cpu$coreIndex/cpufreq/scaling_available_governors", "CPU$coreIndex Available Governors")

            val minFreq = (minFreqStr?.toLongOrNull()?.div(1000))?.toInt() ?: 0 // Convert kHz to MHz
            val maxFreq = (maxFreqStr?.toLongOrNull()?.div(1000))?.toInt() ?: 0 // Convert kHz to MHz

            if (minFreq > 0 && maxFreq > 0) {
                coreFreqRanges[coreIndex] = Pair(minFreq, maxFreq)
                coreGovernors[coreIndex] = governor ?: "Unknown"
                coreAvailableGovernors[coreIndex] = availableGovernorsStr?.split("\\s+".toRegex())?.filter { it.isNotBlank() } ?: emptyList()
            }
        }

        // Group cores with similar frequency ranges into clusters
        val frequencyGroups = coreFreqRanges.values.distinct().sortedBy { it.second } // Sort by max frequency

        frequencyGroups.forEachIndexed { index, (minFreq, maxFreq) ->
            val coresInCluster = coreFreqRanges.filter { it.value == Pair(minFreq, maxFreq) }.keys

            if (coresInCluster.isNotEmpty()) {
                val representativeCore = coresInCluster.first()
                val clusterName = when (index) {
                    0 -> "Little Cluster" // Lowest frequency cluster
                    frequencyGroups.size - 1 -> "Prime Cluster" // Highest frequency cluster
                    else -> "Big Cluster"
                }

                val governor = coreGovernors[representativeCore] ?: "Unknown"
                val availableGovernors = coreAvailableGovernors[representativeCore] ?: emptyList()

                clusters.add(
                    CpuCluster(
                        name = clusterName,
                        minFreq = minFreq,
                        maxFreq = maxFreq,
                        governor = governor,
                        availableGovernors = availableGovernors
                    )
                )
            }
        }

        // If no clusters found (fallback), create a single cluster
        if (clusters.isEmpty()) {
            val fallbackGovernor = readFileToString("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor", "CPU0 Governor") ?: "Unknown"
            val fallbackAvailableGovernors = readFileToString("/sys/devices/system/cpu/cpu0/cpufreq/scaling_available_governors", "CPU0 Available Governors")
                ?.split("\\s+".toRegex())?.filter { it.isNotBlank() } ?: emptyList()

            clusters.add(
                CpuCluster(
                    name = "CPU Cluster",
                    minFreq = 0,
                    maxFreq = 0,
                    governor = fallbackGovernor,
                    availableGovernors = fallbackAvailableGovernors
                )
            )
        }

        return clusters
    }

        @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val realtimeAggregatedInfoFlow: Flow<RealtimeAggregatedInfo> = callbackFlow {

        val lastState = java.util.concurrent.atomic.AtomicReference<RealtimeAggregatedInfo>(null)

        // Initial full data fetch
        launch(Dispatchers.IO) {
            val initialData = RealtimeAggregatedInfo(
                cpuInfo = getCpuRealtimeInternal(),
                gpuInfo = getGpuRealtimeInternal(),
                batteryInfo = getBatteryInfoInternal(),
                memoryInfo = getMemoryInfoInternal(),
                uptimeMillis = getUptimeMillisInternal(),
                deepSleepMillis = getDeepSleepMillisInternal()
            )
            lastState.set(initialData)
            trySend(initialData)
        }

        // Decoupled battery update via BroadcastReceiver
        val batteryReceiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                    val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, BatteryManager.BATTERY_STATUS_UNKNOWN)
                    val currentState = lastState.get()
                    if (currentState != null) {
                        launch(Dispatchers.IO) {
                            val newBatteryInfo = getBatteryInfoInternal(status)
                            val newState = currentState.copy(batteryInfo = newBatteryInfo)
                            lastState.set(newState)
                            trySend(newState)
                        }
                    }
                }
            }
        }
        context.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        // Decoupled polling for other system stats
        val pollingJob = launch(Dispatchers.IO) {
            while (isActive) {
                delay(REALTIME_UPDATE_INTERVAL_MS)
                val currentState = lastState.get()
                if (currentState != null) {
                    // Fetch non-battery stats
                    val newCpuInfo = getCpuRealtimeInternal()
                    val newGpuInfo = getGpuRealtimeInternal()
                    val newMemoryInfo = getMemoryInfoInternal()
                    val newUptime = getUptimeMillisInternal()
                    val newDeepSleep = getDeepSleepMillisInternal()

                    // Create new state by copying the last one and updating polled values
                    val newState = currentState.copy(
                        cpuInfo = newCpuInfo,
                        gpuInfo = newGpuInfo,
                        memoryInfo = newMemoryInfo,
                        uptimeMillis = newUptime,
                        deepSleepMillis = newDeepSleep
                    )
                    lastState.set(newState)
                    trySend(newState)
                }
            }
        }

        awaitClose {
            context.unregisterReceiver(batteryReceiver)
            pollingJob.cancel()
        }
    }.shareIn(
        scope = repositoryScope,
        started = SharingStarted.WhileSubscribed(5000),
        replay = 1
    )

    fun onDestroy() {
        repositoryScope.cancel()
    }
}

