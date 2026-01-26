package id.jo.jomanager.data.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupData(
    val tuning: TuningSettings? = null,
    val networkStorage: NetworkStorageSettings? = null,
    val battery: BatterySettings? = null,
    val other: OtherSettings? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isValid: Boolean
        get() = tuning != null || networkStorage != null || battery != null || other != null
}

@Serializable
data class TuningSettings(
    val thermalMode: Int? = null,
    val cpuGovernor: String? = null,
    val cpuMaxFreq: Int? = null,
    // Add GPU/RAM here if/when they are persisted
)

@Serializable
data class NetworkStorageSettings(
    val tcpCongestion: String? = null,
    val ioScheduler: String? = null
)

@Serializable
data class BatterySettings(
    val bypassCharging: Boolean? = null,
    val forceFastCharge: Boolean? = null,
    val chargingControlEnabled: Boolean? = null,
    val stopLevel: Int? = null,
    val resumeLevel: Int? = null,
    val batteryMonitorEnabled: Boolean? = null
)

@Serializable
data class OtherSettings(
    val kgslSkipZeroing: Boolean? = null,
    val notificationIconStyle: Int? = null,
    // Auto reset settings could go here too
)

data class BackupPreview(
    val hasTuning: Boolean,
    val hasNetwork: Boolean,
    val hasBattery: Boolean,
    val hasOther: Boolean,
    val timestamp: Long
)
