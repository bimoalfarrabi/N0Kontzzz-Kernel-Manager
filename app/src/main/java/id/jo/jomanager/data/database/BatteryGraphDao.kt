package id.jo.jomanager.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BatteryGraphDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: BatteryGraphEntry): Long

    @Query("SELECT * FROM battery_graph_entries ORDER BY timestamp ASC")
    fun getAllEntries(): Flow<List<BatteryGraphEntry>>

    @Query("SELECT * FROM battery_graph_entries WHERE timestamp >= :startTime ORDER BY timestamp ASC")
    fun getEntriesSince(startTime: Long): Flow<List<BatteryGraphEntry>>

    @Query("DELETE FROM battery_graph_entries WHERE timestamp < :cutoffTime")
    suspend fun deleteOldEntries(cutoffTime: Long): Int

    @Query("DELETE FROM battery_graph_entries")
    suspend fun deleteAllEntries(): Int
}
