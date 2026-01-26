
package id.jo.jomanager.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import id.jo.jomanager.R
import id.jo.jomanager.data.repository.SystemRepository
import id.jo.jomanager.manager.TileUpdateManager
import id.jo.jomanager.service.ThermalService.Companion.NOTIFICATION_ID
import id.jo.jomanager.utils.PreferenceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RootActionService : Service() {

    @Inject
    lateinit var systemRepository: SystemRepository
    @Inject
    lateinit var preferenceManager: PreferenceManager
    @Inject
    lateinit var tileUpdateManager: TileUpdateManager

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        startForeground(NOTIFICATION_ID, createNotification())

        serviceScope.launch {
            var success = false
            when (action) {
                ACTION_TOGGLE_KGSL -> {
                    val currentState = preferenceManager.getKgslSkipZeroing()
                    val newState = !currentState
                    success = systemRepository.setKgslSkipZeroing(newState)
                    if (success) {
                        preferenceManager.setKgslSkipZeroing(newState)
                    }
                }
                ACTION_TOGGLE_BYPASS_CHARGING -> {
                    val currentState = preferenceManager.getBypassCharging()
                    val newState = !currentState
                    success = systemRepository.setBypassCharging(newState)
                    if (success) {
                        preferenceManager.setBypassCharging(newState)
                    }
                }
            }

            if (success) {
                tileUpdateManager.notifyUpdate(action)
            }

            // After the operation is done, stop the service
            stopSelf(startId)
        }

        return START_STICKY
    }

    private fun createNotification(): Notification {
        val channelId = "root_action_service_channel"
        val channelName = "Root Action Service"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Applying Kernel Settings")
            .setContentText("Processing your request...")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        const val ACTION_TOGGLE_KGSL = "id.jo.jomanager.service.ACTION_TOGGLE_KGSL"
        const val ACTION_TOGGLE_BYPASS_CHARGING = "id.jo.jomanager.service.ACTION_TOGGLE_BYPASS_CHARGING"
    }
}
