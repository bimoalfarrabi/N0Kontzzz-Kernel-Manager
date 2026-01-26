package id.jo.jomanager.data.model

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

@Immutable
@Serializable
data class StorageInfo(
    val totalSpace: Long,      // Total storage space in bytes
    val freeSpace: Long,       // Free storage space in bytes
    val usedSpace: Long,       // Used storage space in bytes
    val internalTotalSpace: Long,  // Internal storage total space
    val internalFreeSpace: Long,   // Internal storage free space
    val internalUsedSpace: Long,   // Internal storage used space
    val externalTotalSpace: Long?, // External storage total space (nullable for devices without SD card)
    val externalFreeSpace: Long?,  // External storage free space (nullable for devices without SD card)
    val externalUsedSpace: Long?,  // External storage used space (nullable for devices without SD card)
    val hasExternalStorage: Boolean // Whether device has external storage
)
