package id.jo.jomanager.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(history: BatteryHistoryEntity): Long

    @Query("SELECT * FROM battery_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<BatteryHistoryEntity>>

    @Query("DELETE FROM battery_history")
    suspend fun clearHistory(): Int
}
