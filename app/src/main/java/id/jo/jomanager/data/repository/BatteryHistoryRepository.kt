package id.jo.jomanager.data.repository

import id.jo.jomanager.data.database.BatteryHistoryDao
import id.jo.jomanager.data.database.BatteryHistoryEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BatteryHistoryRepository @Inject constructor(
    private val batteryHistoryDao: BatteryHistoryDao
) {
    fun getHistory(): Flow<List<BatteryHistoryEntity>> = batteryHistoryDao.getAllHistory()

    suspend fun insert(history: BatteryHistoryEntity): Long {
        return withContext(Dispatchers.IO) {
            batteryHistoryDao.insert(history)
        }
    }

    suspend fun clearHistory(): Int {
        return withContext(Dispatchers.IO) {
            batteryHistoryDao.clearHistory()
        }
    }
}