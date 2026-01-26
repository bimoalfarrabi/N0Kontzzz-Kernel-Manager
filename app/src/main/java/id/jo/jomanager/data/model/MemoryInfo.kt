package id.jo.jomanager.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class MemoryInfo(
    val used: Long,
    val total: Long,
    val free: Long,
    val zramTotal: Long = 0L,
    val zramUsed: Long = 0L,
    val swapTotal: Long = 0L,
    val swapUsed: Long = 0L,
)