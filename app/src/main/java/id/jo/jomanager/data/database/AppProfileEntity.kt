package id.jo.jomanager.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_profiles")
data class AppProfileEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val performanceMode: String = "Balanced", // "Balanced" or "Performance"
    val kgslSkipZeroing: Boolean = false,
    val bypassCharging: Boolean = false,
    val isEnabled: Boolean = true
)
