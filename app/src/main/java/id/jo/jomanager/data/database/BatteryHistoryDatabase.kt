package id.jo.jomanager.data.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [BatteryHistoryEntity::class, BatteryGraphEntry::class, AppProfileEntity::class], version = 4, exportSchema = false)
abstract class BatteryHistoryDatabase : RoomDatabase() {
    abstract fun batteryHistoryDao(): BatteryHistoryDao
    abstract fun batteryGraphDao(): BatteryGraphDao
    abstract fun appProfileDao(): AppProfileDao
}
