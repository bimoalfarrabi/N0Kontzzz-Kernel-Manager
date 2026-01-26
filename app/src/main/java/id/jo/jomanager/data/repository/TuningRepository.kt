package id.jo.jomanager.data.repository

import android.app.ActivityManager
import android.content.Context
import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

@Singleton
class TuningRepository @Inject constructor(
    private val context: Context
) {


    // Thermal
    private val thermalSysfsNode = "/sys/class/thermal/thermal_message/sconfig"

    // CPU
    private val cpuBaseSysfsPath = "/sys/devices/system/cpu"
    private val coreOnlinePath = "$cpuBaseSysfsPath/cpu%d/online"
    private val cpuGovPath = "$cpuBaseSysfsPath/%s/cpufreq/scaling_governor"
    private val cpuMinFreqPath = "$cpuBaseSysfsPath/%s/cpufreq/scaling_min_freq"
    private val cpuMaxFreqPath = "$cpuBaseSysfsPath/%s/cpufreq/scaling_max_freq"
    private val cpuAvailableGovsPath = "$cpuBaseSysfsPath/%s/cpufreq/scaling_available_governors"
    private val cpuAvailableFreqsPath = "$cpuBaseSysfsPath/%s/cpufreq/scaling_available_frequencies"

    // GPU
    private val gpuBaseSysfsPath = "/sys/class/kgsl/kgsl-3d0"
    private val gpuGovPath = "$gpuBaseSysfsPath/devfreq/governor"
    private val gpuAvailableGovsPath = "$gpuBaseSysfsPath/devfreq/available_governors"
    private val gpuMinFreqPath = "$gpuBaseSysfsPath/devfreq/min_freq"
    private val gpuMaxFreqPath = "$gpuBaseSysfsPath/devfreq/max_freq"
    private val gpuCurFreqPath = "$gpuBaseSysfsPath/devfreq/cur_freq"
    private val gpuAvailableFreqsPath = "$gpuBaseSysfsPath/devfreq/available_frequencies"
    private val gpuCurrentPowerLevelPath = "$gpuBaseSysfsPath/default_pwrlevel"
    private val gpuMinPowerLevelPath = "$gpuBaseSysfsPath/min_pwrlevel"
    private val gpuMaxPowerLevelPath = "$gpuBaseSysfsPath/max_pwrlevel"

    // RAM
    private val zramControlPath = "/sys/block/zram0"
    private val zramResetPath = "$zramControlPath/reset"
    private val zramDisksizePath = "$zramControlPath/disksize"
    private val zramCompAlgorithmPath = "$zramControlPath/comp_algorithm"
    private val zramInitStatePath = "$zramControlPath/initstate"
    private val swappinessPath = "/proc/sys/vm/swappiness"
    private val dirtyRatioPath = "/proc/sys/vm/dirty_ratio"
    private val dirtyBackgroundRatioPath = "/proc/sys/vm/dirty_background_ratio"
    private val dirtyWritebackCentisecsPath = "/proc/sys/vm/dirty_writeback_centisecs"
    private val dirtyExpireCentisecsPath = "/proc/sys/vm/dirty_expire_centisecs"
    private val minFreeKbytesPath = "/proc/sys/vm/min_free_kbytes"
    private val minFreeMemoryPath = "/proc/meminfo"

    /* ----------------------------------------------------------
       Helper Shell
       ---------------------------------------------------------- */
    private var isSuShellWorking = true
    private fun runTuningCommand(cmd: String): Boolean {
        val originalSelinuxMode = getSelinuxModeInternal()
        val needsSelinuxChange = originalSelinuxMode.equals("Enforcing", ignoreCase = true)

        if (needsSelinuxChange) {
            setSelinuxModeInternal(false)
        }

        val success = executeShellCommand(cmd)
        if (needsSelinuxChange) {
            setSelinuxModeInternal(true) // Set kembali ke Enforcing (1)
        }
        return success
    }
    private fun executeShellCommand(cmd: String): Boolean {
        if (isSuShellWorking) {
            try {
                val result = Shell.cmd(cmd).exec()
                return if (result.isSuccess) {
                    true
                } else {
                    isSuShellWorking = false
                    executeShellCommandFallback(cmd)
                }
            } catch (e: Exception) {
                isSuShellWorking = false
                return executeShellCommandFallback(cmd)
            }
        } else {
            return executeShellCommandFallback(cmd)
        }
    }
    private fun runShellCommand(cmd: String): Boolean {
        return runTuningCommand(cmd)
    }


    private fun readShellCommand(cmd: String): String {
        if (isSuShellWorking) {
            try {
                val result = Shell.cmd(cmd).exec()
                return if (result.isSuccess) result.out.joinToString("\n").trim() else {
                    isSuShellWorking = false
                    readShellCommandFallback(cmd)
                }
            } catch (e: Exception) {
                isSuShellWorking = false
                return readShellCommandFallback(cmd)
            }
        } else {
            return readShellCommandFallback(cmd)
        }
    }

    /* ----------------------------------------------------------
       SELinux Specific Helpers (Internal)
       ---------------------------------------------------------- */
    private fun setSelinuxModeInternal(enforcing: Boolean): Boolean {
        val mode = if (enforcing) "1" else "0"
        return executeShellCommand("setenforce $mode")
    }

    private fun getSelinuxModeInternal(): String {
        // Gunakan readShellCommand karena ini adalah operasi baca
        val result = readShellCommand("getenforce").trim()
        return result // Akan mengembalikan "Enforcing", "Permissive"
    }

    // Public functions for SELinux if needed by ViewModel/UI, though likely not directly
    fun setSelinuxEnforcing(): Flow<Boolean> = flow {
        emit(setSelinuxModeInternal(true))
    }.flowOn(Dispatchers.IO)

    fun setSelinuxPermissive(): Flow<Boolean> = flow {
        emit(setSelinuxModeInternal(false))
    }.flowOn(Dispatchers.IO)

    fun getCurrentSelinuxMode(): Flow<String> = flow {
        emit(getSelinuxModeInternal())
    }.flowOn(Dispatchers.IO)


    /* ----------------------------------------------------------
       RAM detection
       ---------------------------------------------------------- */
    private val totalRamBytes: Long
        get() {
            val memInfo = ActivityManager.MemoryInfo()
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            am.getMemoryInfo(memInfo)
            return memInfo.totalMem
        }

    fun calculateMaxZramSize(): Long {
        val ramGB = ceil(totalRamBytes / (1024.0 * 1024.0 * 1024.0)).toLong()
        return when {
            ramGB <= 3L -> 1_073_741_824L   // 1 GB
            ramGB <= 4L -> 2_147_483_648L   // 2 GB
            ramGB <= 6L -> 4_294_967_296L   // 4 GB
            ramGB <= 8L -> 9_663_676_416L   // 9 GB
            ramGB <= 12L -> 15_032_385_536L // 14 GB
            ramGB <= 16L -> 19_327_352_832L // 18 GB
            else -> totalRamBytes / 4       // 25%
        }
    }
    
    private suspend fun resizeZramSafely(newSizeBytes: Long): Boolean = withContext(NonCancellable) {
        val originalSelinuxMode = getSelinuxModeInternal()
        val needsSelinuxChange = originalSelinuxMode.equals("Enforcing", ignoreCase = true)
        var overallSuccess = true

        if (needsSelinuxChange) {
            setSelinuxModeInternal(false)
        }

        if (!executeShellCommand("swapoff /dev/block/zram0 2>/dev/null || true")) overallSuccess = false
        if (!executeShellCommand("echo 1 > $zramResetPath")) overallSuccess = false
        if (!executeShellCommand("echo $newSizeBytes > $zramDisksizePath")) overallSuccess = false
        
        if (newSizeBytes > 0) {
            if (!executeShellCommand("mkswap /dev/block/zram0 2>/dev/null || true")) overallSuccess = false
            if (!executeShellCommand("swapon /dev/block/zram0 2>/dev/null || true")) overallSuccess = false
        }

        if (needsSelinuxChange) {
            setSelinuxModeInternal(true)
        }
        overallSuccess
    }

    fun setZramEnabled(enabled: Boolean): Flow<Boolean> = flow {
        resizeZramSafely(if (enabled) calculateMaxZramSize() else 0)
        emit(readShellCommand("cat $zramInitStatePath").trim() == "1")
    }.flowOn(Dispatchers.IO)

    suspend fun setZramDisksize(sizeBytes: Long): Boolean {
        return resizeZramSafely(sizeBytes)
    }

    suspend fun applyFullZramConfig(sizeBytes: Long, algorithm: String?): Boolean = withContext(NonCancellable) {
        val originalSelinuxMode = getSelinuxModeInternal()
        val needsSelinuxChange = originalSelinuxMode.equals("Enforcing", ignoreCase = true)
        var overallSuccess = true

        if (needsSelinuxChange) {
            setSelinuxModeInternal(false)
        }

        // 1. Swapoff
        if (!executeShellCommand("swapoff /dev/block/zram0 2>/dev/null || true")) overallSuccess = false
        // 2. Reset
        if (!executeShellCommand("echo 1 > $zramResetPath")) overallSuccess = false
        
        // 3. Set Algorithm (if provided)
        if (algorithm != null) {
            // Ensure permission
            executeShellCommand("chmod 666 $zramCompAlgorithmPath")
            if (!executeShellCommand("echo $algorithm > $zramCompAlgorithmPath")) {
                // If setting algo fails, we continue trying to set size, but mark overall as potentially partial
                // However, for ZRAM setup, usually we want strict success. 
                // We'll log it internally via return false if needed, but here we proceed.
            }
        }

        // 4. Set Disksize
        if (!executeShellCommand("echo $sizeBytes > $zramDisksizePath")) overallSuccess = false
        
        // 5. Mkswap & Swapon (Only if size > 0)
        if (sizeBytes > 0) {
            if (!executeShellCommand("mkswap /dev/block/zram0 2>/dev/null || true")) overallSuccess = false
            if (!executeShellCommand("swapon /dev/block/zram0 2>/dev/null || true")) overallSuccess = false
        }

        if (needsSelinuxChange) {
            setSelinuxModeInternal(true)
        }
        overallSuccess
    }

    fun getZramDisksize(): Flow<Long> = flow {
        emit(readShellCommand("cat $zramDisksizePath").toLongOrNull() ?: 0L)
    }.flowOn(Dispatchers.IO)

    fun getZramEnabled(): Flow<Boolean> = flow {
        emit(readShellCommand("cat $zramInitStatePath").trim() == "1")
    }.flowOn(Dispatchers.IO)

    fun getZramUsed(): Flow<Long> = flow {
        val mmStat = readShellCommand("cat /sys/block/zram0/mm_stat").trim()
        val stats = mmStat.split("[ \t\n\r]+".toRegex())
        // mem_used_total is the 3rd value in mm_stat
        val usedBytes = if (stats.size >= 3) stats[2].toLongOrNull() ?: 0L else 0L
        emit(usedBytes)
    }.flowOn(Dispatchers.IO)

    fun getSwapUsed(): Flow<Long> = flow {
        val swapInfo = readShellCommand("cat /proc/swaps")
        val totalUsedKb = swapInfo.lines()
            .drop(1) // Skip header line
            .map { it.trim().split("[ \t\n\r]+".toRegex()) }
            .filter { it.size >= 4 }
            .sumOf { it[3].toLongOrNull() ?: 0L }
        emit(totalUsedKb * 1024) // Convert KB to Bytes
    }.flowOn(Dispatchers.IO)

    suspend fun setCompressionAlgorithm(algo: String): Boolean = withContext(NonCancellable) {
        val currentSize = readShellCommand("cat $zramDisksizePath").toLongOrNull() ?: 0L
        if (readShellCommand("if [ -e $zramControlPath ]; then echo 1; else echo 0; fi").trim() != "1") {
            return@withContext false
        }

        // Tangani SELinux untuk blok perintah ini
        val originalSelinuxMode = getSelinuxModeInternal()
        val needsSelinuxChange = originalSelinuxMode.equals("Enforcing", ignoreCase = true)

        if (needsSelinuxChange) {
            setSelinuxModeInternal(false)
        }

        executeShellCommand("chmod 666 $zramCompAlgorithmPath")

        val commands = if (currentSize > 0) {
            """
            swapoff /dev/block/zram0 2>/dev/null || true
            echo 1 > $zramResetPath
            echo $algo > $zramCompAlgorithmPath
            echo $currentSize > $zramDisksizePath
            mkswap /dev/block/zram0 2>/dev/null || true
            swapon /dev/block/zram0 2>/dev/null || true
            """.trimIndent()
        } else {
            "echo $algo > $zramCompAlgorithmPath"
        }

        val success = executeShellCommand(commands)

        if (needsSelinuxChange) {
            setSelinuxModeInternal(true)
        }
        success
    }

    fun getCompressionAlgorithms(): Flow<List<String>> = flow {
        emit(readShellCommand("cat $zramCompAlgorithmPath").split(" ").filter { it.isNotBlank() }.sorted())
    }.flowOn(Dispatchers.IO)

    fun getCurrentCompression(): Flow<String> = flow {
        val raw = readShellCommand("cat $zramCompAlgorithmPath").trim()
        val active = raw.split(" ").firstOrNull { it.startsWith("[") }
            ?.removeSurrounding("[", "]")
            ?: raw.split(" ").firstOrNull()
            ?: "lz4"
        emit(active)
    }.flowOn(Dispatchers.IO)
    fun setSwappiness(value: Int): Boolean =
        runTuningCommand("echo $value > $swappinessPath")

    fun getSwappiness(): Flow<Int> = flow {
        emit(readShellCommand("cat $swappinessPath").toIntOrNull() ?: 60)
    }.flowOn(Dispatchers.IO)

    fun setDirtyRatio(value: Int): Boolean =
        runTuningCommand("echo $value > $dirtyRatioPath")

    fun getDirtyRatio(): Flow<Int> = flow {
        emit(readShellCommand("cat $dirtyRatioPath").toIntOrNull() ?: 20)
    }.flowOn(Dispatchers.IO)

    fun setDirtyBackgroundRatio(value: Int): Boolean =
        runTuningCommand("echo $value > $dirtyBackgroundRatioPath")

    fun getDirtyBackgroundRatio(): Flow<Int> = flow {
        emit(readShellCommand("cat $dirtyBackgroundRatioPath").toIntOrNull() ?: 10)
    }.flowOn(Dispatchers.IO)

    fun setDirtyWriteback(valueCentisecs: Int): Boolean =
        runTuningCommand("echo $valueCentisecs > $dirtyWritebackCentisecsPath")

    fun getDirtyWriteback(): Flow<Int> = flow {
        emit((readShellCommand("cat $dirtyWritebackCentisecsPath").toIntOrNull() ?: 3000) / 100)
    }.flowOn(Dispatchers.IO)

    fun setDirtyExpireCentisecs(valueCentisecsInput: Int): Boolean =
        runTuningCommand("echo $valueCentisecsInput > $dirtyExpireCentisecsPath")

    fun getDirtyExpireCentisecs(): Flow<Int> = flow {
        emit((readShellCommand("cat $dirtyExpireCentisecsPath").toIntOrNull() ?: 3000) / 100)
    }.flowOn(Dispatchers.IO)

    fun setMinFreeMemory(valueKBytes: Int): Boolean =
        runTuningCommand("echo $valueKBytes > $minFreeKbytesPath")

    fun getMinFreeMemory(): Flow<Int> = flow {
        emit(readShellCommand("cat $minFreeKbytesPath").toIntOrNull() ?: (128 * 1024))
    }.flowOn(Dispatchers.IO)


    /* ----------------------------------------------------------
       CPU
       ---------------------------------------------------------- */
    fun getCpuGov(cluster: String): Flow<String> = flow {
        emit(readShellCommand("cat ${cpuGovPath.format(cluster)}"))
    }.flowOn(Dispatchers.IO)

    fun setCpuGov(cluster: String, gov: String): Boolean {
        val chmodSuccess = runTuningCommand("chmod 666 ${cpuGovPath.format(cluster)}")
        return chmodSuccess && runTuningCommand("echo $gov > ${cpuGovPath.format(cluster)}")
    }

    fun getCpuFreq(cluster: String): Flow<Pair<Int, Int>> = flow {
        val min = readShellCommand("cat ${cpuMinFreqPath.format(cluster)}").toIntOrNull() ?: -1
        val max = readShellCommand("cat ${cpuMaxFreqPath.format(cluster)}").toIntOrNull() ?: -1
        emit(min to max)
    }.flowOn(Dispatchers.IO)

    fun setCpuFreq(cluster: String, min: Int, max: Int): Boolean {
        val chmodMinSuccess = runTuningCommand("chmod 666 ${cpuMinFreqPath.format(cluster)}")
        val chmodMaxSuccess = runTuningCommand("chmod 666 ${cpuMaxFreqPath.format(cluster)}")
        val setMinSuccess = runTuningCommand("echo $min > ${cpuMinFreqPath.format(cluster)}")
        val setMaxSuccess = runTuningCommand("echo $max > ${cpuMaxFreqPath.format(cluster)}")
        return chmodMinSuccess && chmodMaxSuccess && setMinSuccess && setMaxSuccess
    }

    fun getAvailableCpuGovernors(cluster: String): Flow<List<String>> = flow {
        emit(readShellCommand("cat ${cpuAvailableGovsPath.format(cluster)}")
            .split(" ")
            .filter { it.isNotBlank() }
            .sorted())
    }.flowOn(Dispatchers.IO)

    fun getAvailableCpuFrequencies(cluster: String): Flow<List<Int>> = flow {
        emit(readShellCommand("cat ${cpuAvailableFreqsPath.format(cluster)}")
            .split(" ")
            .mapNotNull { it.toIntOrNull() }
            .sorted())
    }.flowOn(Dispatchers.IO)

    fun setCoreOnline(coreId: Int, isOnline: Boolean): Boolean {
        val value = if (isOnline) 1 else 0
        // Chmod mungkin diperlukan dan harus berada dalam blok SELinux jika digunakan
        var chmodSuccess = true
        if (coreId >= 4) {
            chmodSuccess = runTuningCommand("chmod 666 ${coreOnlinePath.format(coreId)}")
        }
        return chmodSuccess && runTuningCommand("echo $value > ${coreOnlinePath.format(coreId)}")
    }

    fun getCoreOnline(coreId: Int): Boolean =
        readShellCommand("cat ${coreOnlinePath.format(coreId)}").trim() == "1"


    fun getNumberOfCores(): Int {
        val presentCores = readShellCommand("cat /sys/devices/system/cpu/present") // e.g., "0-7"
        if (presentCores.contains("-")) {
            try {
                return presentCores.split("-").last().toInt() + 1
            } catch (e: NumberFormatException) {
            }
        }

        var count = 0
        while (readShellCommand("if [ -d /sys/devices/system/cpu/cpu$count ]; then echo 1; else echo 0; fi").trim() == "1") {
            count++
        }
        return if (count > 0) count else 8
    }

    /**
     * Dynamically identifies the leader CPU for each cluster.
     * Returns a list of CPU identifiers (e.g., "cpu0", "cpu4", "cpu7").
     */
    fun getClusterLeaders(): List<String> {
        val leaders = mutableListOf<String>()
        val seenCpus = mutableSetOf<Int>()
        val numCores = getNumberOfCores()

        for (i in 0 until numCores) {
            if (i in seenCpus) continue

            // "cpu$i" is a new cluster leader
            leaders.add("cpu$i")
            seenCpus.add(i)

            // Find other CPUs in this cluster to mark them as seen
            val affectedCpusContent = readShellCommand("cat /sys/devices/system/cpu/cpu$i/cpufreq/affected_cpus")
            if (affectedCpusContent.isNotBlank()) {
                val clusterCpus = affectedCpusContent.split(" ")
                    .mapNotNull { it.trim().toIntOrNull() }
                seenCpus.addAll(clusterCpus)
            } else {
                // Fallback: checks related_cpus if affected_cpus is missing/empty
                val relatedCpusContent = readShellCommand("cat /sys/devices/system/cpu/cpu$i/cpufreq/related_cpus")
                 if (relatedCpusContent.isNotBlank()) {
                    val clusterCpus = relatedCpusContent.split(" ")
                        .mapNotNull { it.trim().toIntOrNull() }
                    seenCpus.addAll(clusterCpus)
                }
            }
        }
        return leaders
    }


    /* ----------------------------------------------------------
       GPU
       ---------------------------------------------------------- */
    fun getGpuGov(): Flow<String> = flow {
        emit(readShellCommand("cat $gpuGovPath"))
    }.flowOn(Dispatchers.IO)

    fun setGpuGov(gov: String): Boolean {
        val chmodSuccess = runTuningCommand("chmod 666 $gpuGovPath")
        return chmodSuccess && runTuningCommand("echo $gov > $gpuGovPath")
    }

    fun getGpuFreq(): Flow<Pair<Int, Int>> = flow {

        val rawMinOutput = readShellCommand("cat $gpuMinFreqPath").trim()

        val rawMaxOutput = readShellCommand("cat $gpuMaxFreqPath").trim()

        val minHz = rawMinOutput.toIntOrNull() ?: 0

        val maxHz = rawMaxOutput.toIntOrNull() ?: 0

        val minMhzResult = if (minHz == 0) 0 else minHz / 1000000
        val maxMhzResult = if (maxHz == 0) 0 else maxHz / 1000000

        emit(minMhzResult to maxMhzResult)
    }.flowOn(Dispatchers.IO)


    fun getCurrentGpuFreq(): Flow<Int> = flow {
        val rawOutput = readShellCommand("cat $gpuCurFreqPath").trim()

        val freqHz = rawOutput.toIntOrNull() ?: 0

        val freqMhzResult = if (freqHz == 0) 0 else freqHz / 1000000

        emit(freqMhzResult)
    }.flowOn(Dispatchers.IO)

    fun getGpuUsage(): Flow<Int> = flow {
        val rawOutput = readShellCommand("cat /sys/class/kgsl/kgsl-3d0/gpu_busy_percentage").trim()

        if (rawOutput.isEmpty()) {
            emit(0)
            return@flow
        }

        val cleanedOutput = rawOutput.replace("%", "").trim()
        val usage = cleanedOutput.toIntOrNull() ?: 0

        emit(usage.coerceIn(0, 100))
    }.flowOn(Dispatchers.IO)


    fun setGpuMinFreq(freqMHz: Int): Boolean {
        return runTuningCommand("echo ${freqMHz * 1000000} > $gpuMinFreqPath")
    }

    fun setGpuMaxFreq(freqMHz: Int): Boolean {
        return runTuningCommand("echo ${freqMHz * 1000000} > $gpuMaxFreqPath")
    }



    fun getAvailableGpuGovernors(): Flow<List<String>> = flow {
        emit(readShellCommand("cat $gpuAvailableGovsPath")
            .split(" ")
            .filter { it.isNotBlank() }
            .sorted())
    }.flowOn(Dispatchers.IO)

    fun getAvailableGpuFrequencies(): Flow<List<Int>> = flow {
        emit(readShellCommand("cat $gpuAvailableFreqsPath")
            .split(" ")
            .mapNotNull { it.toIntOrNull() }
            .map { freqHz -> if (freqHz > 100000) freqHz / 1000000 else freqHz }
            .sorted())
    }.flowOn(Dispatchers.IO)

    fun getGpuPowerLevelRange(): Flow<Pair<Float, Float>> = flow {
        // Asumsi nilai power level adalah integer
        val min = readShellCommand("cat $gpuMinPowerLevelPath").toFloatOrNull() ?: 0f
        val max = readShellCommand("cat $gpuMaxPowerLevelPath").toFloatOrNull() ?: 0f
        emit(min to max)
    }.flowOn(Dispatchers.IO)

    fun getCurrentGpuPowerLevel(): Flow<Float> = flow {
        emit(readShellCommand("cat $gpuCurrentPowerLevelPath").toFloatOrNull() ?: 0f)
    }.flowOn(Dispatchers.IO)

    fun setGpuPowerLevel(level: Float): Boolean =
        runTuningCommand("echo ${level.toInt()} > $gpuCurrentPowerLevelPath")


    /* ----------------------------------------------------------
       Thermal
       ---------------------------------------------------------- */
    fun getCurrentThermalModeIndex(): Flow<Int> = flow {
        // Jika pembacaan gagal, -1 adalah indikator yang baik
        emit(readShellCommand("cat $thermalSysfsNode").toIntOrNull() ?: -1)
    }.flowOn(Dispatchers.IO)

    fun setThermalModeIndex(modeIndex: Int): Flow<Boolean> = flow {
        val preChmodOk = runTuningCommand("chmod 0666 $thermalSysfsNode")
        val writeOk = runTuningCommand("echo $modeIndex > $thermalSysfsNode")
        val postChmodOk = runTuningCommand("chmod 0666 $thermalSysfsNode")
        emit(preChmodOk && writeOk && postChmodOk)
    }.flowOn(Dispatchers.IO)

    /* ----------------------------------------------------------
       OpenGL / Vulkan / Renderer
       ---------------------------------------------------------- */
    fun getOpenGlesDriver(): Flow<String> = flow {
        val rawOutput = readShellCommand("dumpsys SurfaceFlinger | grep \"GLES:\"")
        val glesInfo = rawOutput.substringAfter("GLES:", "").trim()
        emit(
            glesInfo.ifBlank { "N/A" }
        )
    }.flowOn(Dispatchers.IO)

    fun getVulkanApiVersion(): Flow<String> = flow {
        emit(readShellCommand("getprop ro.hardware.vulkan.version").ifEmpty { "N/A" })
    }.flowOn(Dispatchers.IO)

    fun rebootDevice(): Flow<Boolean> = flow {
        emit(executeShellCommand("reboot"))
    }.flowOn(Dispatchers.IO)

    /**
     * Restart SurfaceFlinger service to apply Vulkan settings properly
     * This is especially needed for Android 16 custom ROMs
     */
    private fun restartSurfaceFlinger(): Boolean {

        return try {
            // Method 1: Stop and start SurfaceFlinger service
            val stopSuccess = executeShellCommand("stop surfaceflinger")
            if (stopSuccess) {
                // Wait a moment for service to fully stop
                Thread.sleep(1000)
                val startSuccess = executeShellCommand("start surfaceflinger")
                if (startSuccess) {
                    return true
                }
            }

            // Method 2: Kill SurfaceFlinger process (it will auto-restart)
            val killSuccess = executeShellCommand("pkill -f surfaceflinger || killall surfaceflinger")
            if (killSuccess) {
                return true
            }

            // Method 3: Use service command
            val serviceSuccess = executeShellCommand("service call SurfaceFlinger 1008")
            if (serviceSuccess) {
                return true
            }

            false

        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if Vulkan render engine is actually enabled in SurfaceFlinger
     * This addresses the issue where properties show true but dumpsys shows false
     */
    private fun checkVulkanRenderEngineStatus(): Boolean {
        return try {
            val dumpsysOutput = readShellCommand("dumpsys SurfaceFlinger")

            // Check for various indicators that Vulkan is enabled
            val vulkanIndicators = listOf(
                "vulkan_renderengine: true",
                "vulkan_renderengine:true",
                "RenderEngine: vulkan",
                "Vulkan API",
                "VkInstance",
                "Vulkan render engine active"
            )

            val vulkanEnabled = vulkanIndicators.any { indicator ->
                dumpsysOutput.contains(indicator, ignoreCase = true)
            }

            // Also check for negative indicators
            val negativeIndicators = listOf(
                "vulkan_renderengine: false",
                "vulkan_renderengine:false",
                "RenderEngine: gl",
                "RenderEngine: gles"
            )

            val vulkanDisabled = negativeIndicators.any { indicator ->
                dumpsysOutput.contains(indicator, ignoreCase = true)
            }

            vulkanEnabled && !vulkanDisabled

        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get detailed SurfaceFlinger and Vulkan status for debugging
     * This helps diagnose Android 16 custom ROM compatibility issues
     */
    fun getVulkanRenderEngineStatus(): Flow<Map<String, String>> = flow {
        val statusMap = mutableMapOf<String, String>()

        try {
            // Get basic properties
            statusMap["debug.hwui.renderer"] = readShellCommand("getprop debug.hwui.renderer").ifEmpty { "not_set" }
            statusMap["ro.hwui.use_vulkan"] = readShellCommand("getprop ro.hwui.use_vulkan").ifEmpty { "not_set" }
            statusMap["debug.hwui.force_vulkan"] = readShellCommand("getprop debug.hwui.force_vulkan").ifEmpty { "not_set" }
            statusMap["ro.surface_flinger.use_vk_drivers"] = readShellCommand("getprop ro.surface_flinger.use_vk_drivers").ifEmpty { "not_set" }

            // Get Vulkan API version and hardware info
            statusMap["ro.hardware.vulkan.version"] = readShellCommand("getprop ro.hardware.vulkan.version").ifEmpty { "not_available" }
            statusMap["ro.hardware.vulkan.level"] = readShellCommand("getprop ro.hardware.vulkan.level").ifEmpty { "not_available" }

            // Check SurfaceFlinger dumpsys output
            val dumpsysOutput = readShellCommand("dumpsys SurfaceFlinger")
            val vulkanEngineMatch = Regex("vulkan_renderengine:\\s*(true|false)", RegexOption.IGNORE_CASE)
                .find(dumpsysOutput)
            statusMap["surfaceflinger_vulkan_engine"] = vulkanEngineMatch?.groupValues?.get(1) ?: "not_found"

            // Check for render engine type
            val renderEngineMatch = Regex("RenderEngine:\\s*(\\w+)", RegexOption.IGNORE_CASE)
                .find(dumpsysOutput)
            statusMap["render_engine_type"] = renderEngineMatch?.groupValues?.get(1) ?: "not_found"

            // Check Android version and build info for custom ROM detection
            statusMap["android_version"] = readShellCommand("getprop ro.build.version.release").ifEmpty { "unknown" }
            statusMap["android_sdk"] = readShellCommand("getprop ro.build.version.sdk").ifEmpty { "unknown" }
            statusMap["build_type"] = readShellCommand("getprop ro.build.type").ifEmpty { "unknown" }
            statusMap["build_tags"] = readShellCommand("getprop ro.build.tags").ifEmpty { "unknown" }

            // Check for custom ROM indicators
            val customRomIndicators = listOf(
                "ro.modversion",
                "ro.build.display.id",
                "ro.custom.build.version",
                "ro.lineage.version",
                "ro.arrow.version"
            )

            customRomIndicators.forEach { prop ->
                val value = readShellCommand("getprop $prop")
                if (value.isNotEmpty()) {
                    statusMap["custom_rom_$prop"] = value
                }
            }

            // Check SELinux status as it can affect Vulkan
            statusMap["selinux_status"] = getSelinuxModeInternal()


        } catch (e: Exception) {
            statusMap["error"] = e.message ?: "unknown_error"
        }

        emit(statusMap.toMap())
    }.flowOn(Dispatchers.IO)
    /* ----------------------------------------------------------
       Fallback Shell using Runtime.exec
       ---------------------------------------------------------- */
    private fun executeShellCommandFallback(cmd: String): Boolean {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            process.inputStream.bufferedReader().use { it.readText() }
            process.errorStream.bufferedReader().use { it.readText() }

            val exitCode = process.waitFor()
            return exitCode == 0
        } catch (e: Exception) {
            return false
        }
    }

    private fun readShellCommandFallback(cmd: String): String {
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
            val output = process.inputStream.bufferedReader().use { it.readText() }
            process.errorStream.bufferedReader().use { it.readText() }
            val exitCode = process.waitFor()

            return if (exitCode == 0) {
                output.trim()
            } else {
                ""
            }
        } catch (e: Exception) {
            return ""
        }
    }



    fun getSwapTotal(): Flow<Long> = flow {
        val swapInfo = readShellCommand("cat /proc/swaps")
        val totalSizeKb = swapInfo.lines()
            .drop(1) // Skip header line
            .map { it.trim().split("[ \t\n\r]+".toRegex()) }
            .filter { it.size >= 3 }
            .sumOf { it[2].toLongOrNull() ?: 0L }
        emit(totalSizeKb * 1024) // Convert KB to Bytes
    }.flowOn(Dispatchers.IO)
}
