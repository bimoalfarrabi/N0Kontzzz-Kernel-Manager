package id.jo.jomanager.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "battery_history")
data class BatteryHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val timestamp: Long,
    val activeDrain: Int,
    val idleDrain: Int,
    val screenOnTime: Long,
    val screenOffTime: Long,
    val deepSleepTime: Long,
    val awakeTime: Long,
    val chargingSpeed: Int,
    val dischargingRate: Int
)
