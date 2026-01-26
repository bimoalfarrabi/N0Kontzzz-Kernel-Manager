package id.jo.jomanager.service

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.view.View
import android.graphics.PixelFormat
import android.view.Gravity
import androidx.core.app.NotificationCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.AndroidEntryPoint
import id.jo.jomanager.R
import id.jo.jomanager.data.repository.ThermalRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class ThermalService : Service() {
    @Inject
    lateinit var thermalRepository: ThermalRepository

    @Inject
    @id.jo.jomanager.di.ThermalSettings
    lateinit var thermalDataStore: DataStore<Preferences>

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var monitoringJob: Job? = null
    private val TAG = "ThermalService"
    private var isRootAvailable = false
    private var overlayWindow: View? = null
    private var windowManager: WindowManager? = null

    private val LAST_THERMAL_MODE = intPreferencesKey("last_thermal_mode")

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "thermal_service_channel"
        private const val MONITOR_INTERVAL = 1000L // 1 second
        private const val MAX_RETRY_COUNT = 3
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        checkRootAccess()
        
        // For Android 15+, create a minimal overlay window before starting foreground service
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            createMinimalOverlayWindow()
        }
        
        startForeground(NOTIFICATION_ID, createNotification())
    }

    private fun checkRootAccess() {
        isRootAvailable = Shell.isAppGrantedRoot() == true && Shell.getShell().isRoot == true
        if (!isRootAvailable) {
            Log.e(TAG, "Root access not available, service will not monitor thermal settings")
        }
    }

    /**
     * Creates a minimal overlay window for Android 15+ compatibility.
     * This is required when an app with SYSTEM_ALERT_WINDOW permission starts
     * a foreground service from the background.
     */
    private fun createMinimalOverlayWindow() {

        try {
            if (Settings.canDrawOverlays(this)) {
                windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
                
                // Create a minimal 1x1 pixel view
                val overlayView = View(this).apply {
                    setBackgroundColor(0) // Transparent
                    alpha = 0.0f // Invisible
                }
                
                val params = WindowManager.LayoutParams(
                    1, 1, // 1x1 pixel
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                    WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or 
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                    PixelFormat.TRANSLUCENT
                ).apply {
                    gravity = Gravity.TOP or Gravity.START
                    x = 0
                    y = 0
                }
                
                windowManager?.addView(overlayView, params)
                overlayWindow = overlayView
                
                Log.d(TAG, "Minimal overlay window created for Android 15+ compatibility")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create overlay window", e)
        }
    }
    
    /**
     * Removes the overlay window if it exists.
     */
    private fun removeOverlayWindow() {
        try {
            overlayWindow?.let { view ->
                windowManager?.removeView(view)
                overlayWindow = null
                windowManager = null
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to remove overlay window", e)
        }
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.thermal_service_channel_name),
            NotificationManager.IMPORTANCE_LOW
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, id.jo.jomanager.ui.MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.thermal_service_notification_title))
            .setContentText(getString(R.string.thermal_service_notification_text))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isRootAvailable) {
            Log.e(TAG, "No root access available, stopping service")
            stopSelf()
            return START_NOT_STICKY
        }

        startMonitoring()
        return START_REDELIVER_INTENT
    }

    private fun startMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = serviceScope.launch {
            // Cache the target mode
            var targetMode = 0
            
            // Launch a separate coroutine to observe settings changes
            launch {
                thermalDataStore.data.collect { prefs ->
                    targetMode = prefs[LAST_THERMAL_MODE] ?: 0
                }
            }

            val pm = getSystemService(POWER_SERVICE) as android.os.PowerManager
            var retryCount = 0

            while (isActive) {
                try {
                    // Optimization: Pause monitoring if screen is off, 
                    // unless we are in a critical state (which we don't track here yet).
                    // For now, just slowing down check when screen is off is safer than stopping.
                    if (!pm.isInteractive) {
                        delay(5000)
                        continue
                    }

                    if (!isRootAvailable) {
                        checkRootAccess()
                        if (!isRootAvailable) {
                            if (++retryCount >= MAX_RETRY_COUNT) {
                                Log.e(TAG, "Max retry count reached, stopping service")
                                stopSelf()
                                break
                            }
                            delay(MONITOR_INTERVAL * 2)
                            continue
                        }
                    }

                    // If we're no longer in Dynamic mode, stop the service
                    if (targetMode != 10 && targetMode != 0) {
                         // Wait a bit to ensure targetMode is stable or if we should just stop
                         // But for now, if it's not dynamic, we shouldn't be enforcing it?
                         // The original logic stopped if savedMode != 10.
                         // We'll mimic that behavior but slightly more robustly.
                         Log.d(TAG, "No longer in Dynamic mode ($targetMode), stopping service")
                         stopSelf()
                         break
                    }
                    
                    if (targetMode == 10) {
                        val currentMode = thermalRepository.getCurrentThermalModeIndex().first()

                        if (currentMode != targetMode) {
                            Log.d(TAG, "Thermal mode changed from $targetMode to $currentMode, restoring...")
                            val result = Shell.cmd("echo $targetMode > /sys/class/thermal/thermal_message/sconfig").exec()
                            if (result.isSuccess) {
                                retryCount = 0
                            } else {
                                Log.e(TAG, "Failed to restore thermal mode: ${result.err.joinToString()}")
                                if (++retryCount >= MAX_RETRY_COUNT) {
                                    break
                                }
                            }
                        }
                    }
                    
                    delay(3000) // Increase interval to 3s

                } catch (e: Exception) {
                    Log.e(TAG, "Error in monitoring loop", e)
                    if (++retryCount >= MAX_RETRY_COUNT) {
                        break
                    }
                    delay(MONITOR_INTERVAL * 2)
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        monitoringJob?.cancel()
        serviceScope.cancel()
        removeOverlayWindow()
        Log.d(TAG, "ThermalService destroyed")
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        try {
            val restartServiceIntent = Intent(applicationContext, this.javaClass)
            val restartServicePendingIntent = PendingIntent.getService(
                applicationContext, 1, restartServiceIntent, PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = applicationContext.getSystemService(ALARM_SERVICE) as AlarmManager
            alarmManager.set(AlarmManager.ELAPSED_REALTIME, 1000, restartServicePendingIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule service restart", e)
        }
        super.onTaskRemoved(rootIntent)
    }
}
