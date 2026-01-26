package id.jo.jomanager.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class BatteryInfo(
    val level: Int = 0,
    val temp: Float = 0f, // Changed from temperature to temp
    val voltage: Float = 0f,
    val chargingWattage: Float = 0f,
    val isCharging: Boolean = false,
    val current: Float = 0f, // Added current field
    val technology: String = "Unknown",
    val health: String = "Unknown",
    val status: String = "Not Charging", // Changed from chargingStatus to status
    val powerSource: String = "Unknown", // USB/AC/Wireless/Battery
    val healthPercentage: Int = 0, // Health percentage (0-100)
    val cycleCount: Int = 0, // Battery charge cycles
    val capacity: Int = 0, // Design capacity in mAh
    val currentCapacity: Int = 0, // Current capacity in mAh
    val plugged: Int = 0, // Raw plugged value from BatteryManager
    val chargingType: String = "Unknown" // Added chargingType field for compatibility
)
