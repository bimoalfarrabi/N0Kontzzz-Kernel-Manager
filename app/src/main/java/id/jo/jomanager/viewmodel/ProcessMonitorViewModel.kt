package id.jo.jomanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import id.jo.jomanager.data.repository.RootRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.io.File

data class ProcessInfo(
    val pid: String,
    val user: String,
    val cpu: Float,
    val ram: Float, // MB
    val name: String,
    val isUserApp: Boolean
)

enum class ProcessSort {
    CPU, RAM
}

enum class ProcessFilter {
    ALL, USER_APPS, SYSTEM_ROOT
}

@HiltViewModel
class ProcessMonitorViewModel @Inject constructor(
    private val rootRepository: RootRepository
) : ViewModel() {

    private val _processes = MutableStateFlow<List<ProcessInfo>>(emptyList())
    val processes: StateFlow<List<ProcessInfo>> = _processes.asStateFlow()

    private val _sampleRate = MutableStateFlow(2000L)
    val sampleRate: StateFlow<Long> = _sampleRate.asStateFlow()

    private val _maxProcesses = MutableStateFlow(20)
    val maxProcesses: StateFlow<Int> = _maxProcesses.asStateFlow()

    private val _sortOption = MutableStateFlow(ProcessSort.CPU)
    val sortOption: StateFlow<ProcessSort> = _sortOption.asStateFlow()

    private val _filterOption = MutableStateFlow(ProcessFilter.ALL)
    val filterOption: StateFlow<ProcessFilter> = _filterOption.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var monitorJob: Job? = null

    // State for delta calculation
    private var prevTotalCpuTime: Long = 0
    private var prevProcessCpuTimes: Map<String, Long> = emptyMap()

    fun startMonitoring() {
        if (monitorJob?.isActive == true) return

        // Set loading true immediately if we have no data
        if (_processes.value.isEmpty()) {
            _isLoading.value = true
        }

        monitorJob = viewModelScope.launch(Dispatchers.IO) {
            // Reset state
            prevTotalCpuTime = 0
            prevProcessCpuTimes = emptyMap()
            
            while (isActive) {
                try {
                    // 1. Capture Snapshot
                    val tempPath = "/data/local/tmp/nkm_proc_snapshot"
                    val cmd = "(cat /proc/stat; echo '###'; cat /proc/[0-9]*/stat) > $tempPath && chmod 644 $tempPath"
                    rootRepository.run(cmd)

                    val file = File(tempPath)
                    if (!file.exists() || !file.canRead()) {
                        delay(500)
                        continue
                    }

                    val content = file.readText()
                    val (currentTotalCpu, currentProcessCpuTimes) = parseCpuSnapshot(content)

                    // 2. If we have previous data, calculate delta
                    if (prevTotalCpuTime > 0) {
                        val totalDelta = currentTotalCpu - prevTotalCpuTime
                        
                        if (totalDelta > 0) {
                            // 3. Get Process Details via PS
                            val psDetails = getProcessDetails()

                            val newProcessList = mutableListOf<ProcessInfo>()

                            for ((pid, currTicks) in currentProcessCpuTimes) {
                                val prevTicks = prevProcessCpuTimes[pid] ?: 0L
                                val procDelta = currTicks - prevTicks
                                
                                if (procDelta >= 0) {
                                    val cpuUsage = (procDelta.toFloat() / totalDelta) * 100f

                                    val details = psDetails[pid]
                                    if (details != null) {
                                        val isUserApp = details.user.startsWith("u0_") || 
                                                        details.user.startsWith("app_") || 
                                                        details.name.contains("com.") || 
                                                        details.name.contains("id.")

                                        newProcessList.add(
                                            ProcessInfo(
                                                pid = pid,
                                                user = details.user,
                                                cpu = cpuUsage,
                                                ram = details.rssMb,
                                                name = details.name,
                                                isUserApp = isUserApp
                                            )
                                        )
                                    }
                                }
                            }

                            // 4. Sort and Filter
                            val filtered = when (_filterOption.value) {
                                ProcessFilter.ALL -> newProcessList
                                ProcessFilter.USER_APPS -> newProcessList.filter { it.isUserApp }
                                ProcessFilter.SYSTEM_ROOT -> newProcessList.filter { !it.isUserApp }
                            }

                            val sorted = when (_sortOption.value) {
                                ProcessSort.CPU -> filtered.sortedWith(compareByDescending<ProcessInfo> { it.cpu }.thenByDescending { it.ram })
                                ProcessSort.RAM -> filtered.sortedWith(compareByDescending<ProcessInfo> { it.ram }.thenByDescending { it.cpu })
                            }

                            _processes.value = sorted.take(_maxProcesses.value)
                            
                            // Only hide loading once we have successfully updated the list
                            _isLoading.value = false
                        }
                    }

                    // Update Previous State
                    prevTotalCpuTime = currentTotalCpu
                    prevProcessCpuTimes = currentProcessCpuTimes

                } catch (e: Exception) {
                    e.printStackTrace()
                    // Keep loading true if we failed and list is empty, 
                    // or maybe turn off if retries fail? 
                    // For now, let it retry.
                }

                delay(_sampleRate.value)
            }
        }
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
        viewModelScope.launch(Dispatchers.IO) {
            try {
                rootRepository.run("rm /data/local/tmp/nkm_proc_snapshot")
            } catch (_: Exception) {}
        }
    }

    fun updateSettings(rate: Long, max: Int, sort: ProcessSort, filter: ProcessFilter) {
        _sampleRate.value = rate.coerceAtLeast(500)
        _maxProcesses.value = max.coerceIn(5, 100)
        _sortOption.value = sort
        _filterOption.value = filter
        
        if (monitorJob?.isActive == true) {
            // Let the loop pick up new values
        } else {
            startMonitoring()
        }
    }

    private data class PsDetails(val user: String, val rssMb: Float, val name: String)

    private fun getProcessDetails(): Map<String, PsDetails> {
        val map = mutableMapOf<String, PsDetails>()
        try {
            val output = rootRepository.run("ps -A -o pid,user,rss,args")
            val lines = output.lines()
            if (lines.isEmpty()) return map

            for (i in 1 until lines.size) {
                val line = lines[i].trim()
                if (line.isEmpty()) continue
                
                val parts = line.split("\\s+".toRegex())
                if (parts.size < 4) continue

                val pid = parts[0]
                val user = parts[1]
                val rssKb = parts[2].toFloatOrNull() ?: 0f
                val rssMb = rssKb / 1024f

                val name = if (parts.size > 3) {
                    parts.subList(3, parts.size).joinToString(" ")
                } else {
                    parts.getOrNull(3) ?: "?"
                }
                
                val simpleName = name.split("/").last()

                map[pid] = PsDetails(user, rssMb, simpleName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return map
    }

    private fun parseCpuSnapshot(content: String): Pair<Long, Map<String, Long>> {
        var totalCpu: Long = 0
        val procMap = mutableMapOf<String, Long>()
        
        val lines = content.lines()
        var parsingProcs = false
        
        for (line in lines) {
            if (line == "###") {
                parsingProcs = true
                continue
            }
            
            if (!parsingProcs) {
                if (line.startsWith("cpu ")) {
                    val parts = line.split("\\s+".toRegex()).filter { it.isNotEmpty() }
                    for (i in 1 until parts.size) {
                        totalCpu += parts[i].toLongOrNull() ?: 0L
                    }
                }
            } else {
                val closeParenIdx = line.lastIndexOf(')')
                if (closeParenIdx == -1) continue
                
                val pidStr = line.substringBefore('(').trim()
                val statsStr = line.substring(closeParenIdx + 2)
                val stats = statsStr.split(" ")
                
                if (stats.size > 12) {
                    val utime = stats[11].toLongOrNull() ?: 0L
                    val stime = stats[12].toLongOrNull() ?: 0L
                    val totalTicks = utime + stime
                    procMap[pidStr] = totalTicks
                }
            }
        }
        
        return Pair(totalCpu, procMap)
    }

    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
    }
}
