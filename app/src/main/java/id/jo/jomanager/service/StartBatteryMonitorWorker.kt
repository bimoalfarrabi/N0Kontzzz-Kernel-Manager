package id.jo.jomanager.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class StartBatteryMonitorWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        return try {
            BatteryMonitorService.start(applicationContext, isBoot = true)
            Result.success()
        } catch (t: Throwable) {
            Result.retry()
        }
    }
}


