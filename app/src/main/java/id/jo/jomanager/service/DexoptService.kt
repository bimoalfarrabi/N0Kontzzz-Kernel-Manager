package id.jo.jomanager.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.topjohnwu.superuser.CallbackList
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.AndroidEntryPoint
import id.jo.jomanager.R
import id.jo.jomanager.data.repository.DexoptRepository
import id.jo.jomanager.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class DexoptService : Service() {

    @Inject
    lateinit var dexoptRepository: DexoptRepository

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val CHANNEL_ID = "dexopt_channel"
    private val NOTIFICATION_ID = 2001
    private val FINISHED_NOTIFICATION_ID = 2002

    companion object {
        fun start(context: Context) {
            val intent = Intent(context, DexoptService::class.java)
            context.startForegroundService(intent)
        }
        
        fun stop(context: Context) {
             context.stopService(Intent(context, DexoptService::class.java))
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        runDexopt()
        return START_NOT_STICKY
    }

    private fun runDexopt() {
        dexoptRepository.setRunning(true)
        
        serviceScope.launch {
            // Buffer to hold the latest log
            var lastLogMessage = "Initializing..."
            
            // CRITICAL FIX: Use a DEDICATED Shell instance to prevent blocking the main app shell
            // This ensures that MainActivity checks do not freeze waiting for this command
            var dedicatedShell: Shell? = null
            
            try {
                // Build a new root shell instance
                dedicatedShell = Shell.Builder.create()
                    .build()
            } catch (e: Exception) {
                dexoptRepository.updateLastLog("Error creating shell: ${e.message}")
                stopSelf()
                return@launch
            }

            val callbackList = object : CallbackList<String>() {
                override fun onAddElement(s: String) {
                    lastLogMessage = s
                }
            }

            // Start a coroutine to push updates periodically
            val updaterJob = launch {
                while (isActive) {
                    dexoptRepository.updateLastLog(lastLogMessage)
                    delay(200) // Update UI 5 times a second
                }
            }

            try {
                dexoptRepository.updateLastLog("Starting optimization (Dedicated Shell)...")
                
                // Execute commands on the DEDICATED shell instance
                // We use the same nice/ionice optimization
                dedicatedShell.newJob().add(
                    "nice -n 19 pm compile -a -f -m speed-profile",
                    "nice -n 19 pm compile -a -f compile-layouts",
                    "nice -n 19 pm bg-dexopt-job"
                ).to(callbackList).exec()

                dexoptRepository.updateLastLog("Dexopt process finished.")
            } catch (e: Exception) {
                dexoptRepository.updateLastLog("Error: ${e.message}")
            } finally {
                updaterJob.cancel()
                dexoptRepository.updateLastLog("Finished.")
                
                try {
                    dedicatedShell.close()
                } catch (e: Exception) { /* ignore */ }
                
                dexoptRepository.setFinished(true)
                showFinishedNotification()
                stopSelf()
            }
        }
    }

    private fun createNotificationChannel() {
        val name = "Dexopt Service"
        val descriptionText = "Shows notification when Dexopt is running"
        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Dexopt Running")
            .setContentText("Optimizing apps in background...")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun showFinishedNotification() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Dexopt Finished")
            .setContentText("Optimization process completed successfully.")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(FINISHED_NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
