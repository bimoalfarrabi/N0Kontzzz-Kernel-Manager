package id.jo.jomanager.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battery_graph_entries")
data class BatteryGraphEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val batteryLevel: Int,
    val currentMa: Float, // Negative = discharging, Positive = charging
    val isCharging: Boolean,
    val isScreenOn: Boolean = false,
    val activeDrainRate: Float, // %/hr
    val idleDrainRate: Float, // %/hr
    val temperature: Float
)
