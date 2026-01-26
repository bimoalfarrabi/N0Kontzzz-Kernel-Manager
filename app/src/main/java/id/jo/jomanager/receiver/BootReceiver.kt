package id.jo.jomanager.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import id.jo.jomanager.service.BatteryMonitorService
import android.app.AlarmManager
import android.app.PendingIntent
import id.jo.jomanager.ui.WarmStartActivity
import androidx.work.ExistingWorkPolicy
import androidx.work.Configuration
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import id.jo.jomanager.service.StartBatteryMonitorWorker
import android.os.Build

class BootReceiver : BroadcastReceiver() {


    private val TAG = "BootReceiver"
    private val ACTION_ALARM_START = "id.jo.jomanager.action.ALARM_START_BATTERY_MONITOR"

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Boot event received: ${intent.action}")
        val action = intent.action
        if (action == ACTION_ALARM_START) {
            val pending = goAsync()
            Thread {
                try { tryStartFgs(context) } finally { try { pending.finish() } catch (_: Exception) {} }
            }.start()
            return
        }
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_LOCKED_BOOT_COMPLETED ||
            action == Intent.ACTION_USER_UNLOCKED ||
            action == Intent.ACTION_USER_PRESENT ||
            action == "com.htc.intent.action.QUICKBOOT_POWERON" ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_PACKAGE_REPLACED) {
            // Use goAsync to allow delayed start even if FGS is temporarily blocked
            val pending = goAsync()
            Thread {
                try {
                    handleBootCompleted(context)
                } finally {
                    try { pending.finish() } catch (_: Exception) {}
                }
            }.start()
        }
    }

    private fun handleBootCompleted(context: Context) {
        Log.d(TAG, "Handling boot completed event")
        
        // Always attempt to restore settings on boot
        val restoreReq = OneTimeWorkRequestBuilder<id.jo.jomanager.worker.RestoreSettingsWorker>().build()
        WorkManager.getInstance(context).enqueue(restoreReq)

        runCatching {
            val enabled = isBatteryMonitorEnabled(context)
            if (enabled) {
                // Try immediate start, and if it fails, fall back to robust methods
                if (!tryStartFgs(context)) {
                    Log.w(TAG, "Immediate FGS start failed, enqueueing WorkManager fallback")
                    val req = OneTimeWorkRequestBuilder<StartBatteryMonitorWorker>()
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .build()
                    try {
                        WorkManager.getInstance(context)
                            .enqueueUniqueWork("start_battery_monitor", ExistingWorkPolicy.REPLACE, req)
                    } catch (ise: IllegalStateException) {
                        WorkManager.initialize(
                            context,
                            Configuration.Builder().setMinimumLoggingLevel(Log.DEBUG).build()
                        )
                        WorkManager.getInstance(context)
                            .enqueueUniqueWork("start_battery_monitor", ExistingWorkPolicy.REPLACE, req)
                    }
                    // Schedule multiple alarm fallbacks to improve reliability across ROMs
                    scheduleAlarm(context, 15_000L)
                    scheduleAlarm(context, 45_000L)
                    scheduleAlarm(context, 120_000L)
                    // Launch a transparent warm-start Activity to create app process if needed
                    try {
                        val i = Intent(context, WarmStartActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(i)
                        Log.d(TAG, "WarmStartActivity launched to prewarm process")
                    } catch (t: Throwable) {
                        Log.w(TAG, "Failed to launch WarmStartActivity: ${t.message}")
                    }
                }
            } else {
                Log.d(TAG, "BatteryMonitorService not enabled; skipping auto-start")
            }
        }.onFailure {
            Log.e(TAG, "Failed to auto-start BatteryMonitorService", it)
        }
    }

    private fun tryStartFgs(context: Context): Boolean {
        return try {
            BatteryMonitorService.start(context, isBoot = true)
            Log.d(TAG, "BatteryMonitorService auto-started")
            true
        } catch (t: Throwable) {
            Log.w(TAG, "FGS start failed: ${t.message}")
            false
        }
    }

    private fun scheduleAlarm(context: Context, delayMs: Long) {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val pi = PendingIntent.getBroadcast(
                context,
                20045,
                Intent(context, BootReceiver::class.java).setAction(ACTION_ALARM_START),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            val triggerAt = System.currentTimeMillis() + delayMs
            try {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } catch (_: SecurityException) {
                am.set(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
            Log.d(TAG, "Alarm scheduled in ${delayMs}ms for BatteryMonitorService start")
        } catch (t: Throwable) {
            Log.w(TAG, "Failed to schedule alarm fallback: ${t.message}")
        }
    }

    private fun isBatteryMonitorEnabled(context: Context): Boolean {
        val deviceContext = context.createDeviceProtectedStorageContext()
        val dpPrefs = deviceContext.getSharedPreferences("nkm_preferences", Context.MODE_PRIVATE)
        if (dpPrefs.getBoolean("battery_monitor_enabled", false)) return true
        return try {
            context.getSharedPreferences("nkm_preferences", Context.MODE_PRIVATE)
                .getBoolean("battery_monitor_enabled", false)
        } catch (_: IllegalStateException) {
            false
        }
    }
}
