package id.jo.jomanager.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import id.jo.jomanager.R
import id.jo.jomanager.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import id.jo.jomanager.data.repository.BatteryGraphRepository
import id.jo.jomanager.data.repository.SystemRepository
import id.jo.jomanager.data.database.BatteryGraphEntry
import id.jo.jomanager.utils.PreferenceManager
import androidx.core.graphics.createBitmap

@AndroidEntryPoint
class BatteryMonitorService : Service() {

    @Inject
    lateinit var batteryGraphRepository: BatteryGraphRepository

    @Inject
    lateinit var systemRepository: SystemRepository

    @Inject
    lateinit var preferenceManager: PreferenceManager

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private lateinit var notificationManager: NotificationManager
    private var batteryManager: BatteryManager? = null
    private var serviceStartedAtMs: Long = 0L

    private val channelId = "battery_monitor_channel"
    private val notificationId = 1001

    private var lastUpdate = System.currentTimeMillis()
    private var lastHistorySaveTime = 0L
    private var lastCurrent = 0f
    private var designCapacityUah = 0L // autodetected; no hardcoded fallback
    private var realtimeUsedMah = 0.0
    // Legacy baseline fields (no longer used for drain; kept previously for transitions) removed
    // Cumulative consumption tracking
    private var lastSampleElapsedMs: Long = 0L
    private var lastSampleChargeUah: Long = 0L
    private var consumedOnUah: Long = 0L
    private var consumedOffUah: Long = 0L
    private var prevInteractiveForSample: Boolean? = null
    private var lastSamplePercent: Double = Double.NaN
    private var onPercentDrop: Double = 0.0
    private var offPercentDrop: Double = 0.0

    private var hasResetForThisChargeCycle = false

    private var isRunning = false

    // Screen on-time tracking
    private var screenReceiver: BroadcastReceiver? = null
    private var screenOnAccumMs: Long = 0L
    private var screenOnStartAtElapsed: Long? = null
    private var powerReceiver: BroadcastReceiver? = null
    private var windowStartElapsed: Long = -1L
    private var windowStartUptime: Long = -1L
    private var windowAccumMs: Long = 0L // Accumulated window time from previous sessions (for reboot persistence)
    private var nextDelayOverrideMs: Long? = null
    
    // Cache for Charging Control to reduce File I/O
    private var lastKnownBypassState: Boolean? = null

    // Persistence
    private val prefs by lazy {
        if (!isDeviceProtectedStorage) {
            createDeviceProtectedStorageContext().getSharedPreferences("battery_monitor_prefs",
                MODE_PRIVATE
            )
        } else {
            getSharedPreferences("battery_monitor_prefs", MODE_PRIVATE)
        }
    }
    private val keyScreenAccum = "screen_accum_ms"
    private val keyLastElapsed = "last_elapsed_ms"

    override fun onCreate() {
        super.onCreate()
        serviceStartedAtMs = System.currentTimeMillis()
        batteryManager = getSystemService(BATTERY_SERVICE) as BatteryManager
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        val initialNotification = createNotification("Starting...", "Collecting battery data...")
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(notificationId, initialNotification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else
                startForeground(notificationId, initialNotification)
        } catch (e: Exception) {
            // Prevent crash if FGS is blocked
            stopSelf()
        }
        designCapacityUah = getDesignCapacityUah()
        restoreStateIfAny()
        initScreenTracking()
        initPowerTracking()
        startMonitoring()
    }

    private fun computeCurrentBatteryPercent(stickyLevel: Int, chargeUah: Long): Double {
        // Prefer OS-reported percentage to avoid noisy coulomb counter coupling to current draw
        if (stickyLevel >= 0) return stickyLevel.toDouble()
        if (chargeUah > 0L && designCapacityUah > 0L) {
            return (chargeUah.toDouble() * 100.0) / designCapacityUah.toDouble()
        }
        return 0.0
    }

    private fun formatPercent(percent: Double, hasData: Boolean): String {
        if (!hasData) return String.format(Locale.US, "%.1f", 0.0)
        return if (percent >= 99.995) String.format(Locale.US, "%.1f", 100.0)
        else String.format(Locale.US, "%.2f", percent)
    }

    private fun readFirstLong(paths: List<String>): Long? {
        for (p in paths) {
            try {
                val s = File(p).readText().trim()
                if (s.isNotEmpty()) {
                    val v = s.toLong()
                    return v
                }
            } catch (_: Exception) { }
        }
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        isRunning = false
        try {
            screenReceiver?.let { unregisterReceiver(it) }
        } catch (_: Exception) { }
        try {
            powerReceiver?.let { unregisterReceiver(it) }
        } catch (_: Exception) { }
        persistState(sync = true)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_RESET) {
            try {
                manualReset()
                val stats = collectSystemStats()
                updateNotification(stats)
            } catch (_: Exception) { }
        } else if (intent?.action == ACTION_BOOT_START) {
            // History auto-reset
            if (preferenceManager.isAutoResetOnReboot()) {
                if (isUserUnlocked(this)) {
                    scope.launch { try { batteryGraphRepository.deleteAllEntries() } catch (_: Exception) { } }
                }
            }
            // Monitor auto-reset (Active Stats)
            // Note: restoreStateIfAny() already handles wiping accumulators if this is true.
            // We call manualReset() here to ensure sampling baselines are also fresh.
            if (preferenceManager.isMonitorAutoResetOnReboot()) {
                manualReset()
            }
        } else if (intent?.action == ACTION_UPDATE_ICON) {
            try {
                val stats = collectSystemStats()
                updateNotification(stats)
            } catch (_: Exception) { }
        }
        return START_STICKY
    }

    // === MAIN LOOP ===
    private fun startMonitoring() {
        if (isRunning) return
        isRunning = true

        val pm = getSystemService(POWER_SERVICE) as PowerManager

        scope.launch {
            while (isRunning) {
                val stats = collectSystemStats()
                updateNotification(stats)
                
                // Adaptive delay: 5s if screen on, 60s if screen off (to match history save interval)
                val d = nextDelayOverrideMs ?: if (pm.isInteractive) 5_000L else 60_000L
                nextDelayOverrideMs = null
                delay(d)
            }
        }
    }

    // === BATTERY LOGIC ===
    private fun collectSystemStats(): BatteryData {
        val bm = batteryManager ?: return BatteryData()

        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = intent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val temp = (intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f

        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || plugged > 0

        checkAutoResetAtLevel(level, charging)
        checkChargingControl(level, plugged > 0)

        // Prefer sysfs for current/voltage; fallback to BatteryManager
        val currentUaSys = readFirstLong(listOf(
            "/sys/class/power_supply/battery/current_now",
            "/sys/class/power_supply/battery/Battery_Current",
            "/sys/class/power_supply/battery/batt_current_now"
        ))
        val voltageUvSys = readFirstLong(listOf(
            "/sys/class/power_supply/battery/voltage_now",
            "/sys/class/power_supply/battery/batt_voltage_now"
        ))

        val currentUa = currentUaSys ?: bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW).toLong()
        val voltageV = (voltageUvSys?.toDouble()?.div(1_000_000.0))
            ?: ((intent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0).toDouble() / 1000.0)
        val powerUwSys = readFirstLong(listOf(
            "/sys/class/power_supply/battery/power_avg",
            "/sys/class/power_supply/battery/power_now"
        ))

        // Map sign so: charging -> positive mA, discharging -> negative mA
        val rawMa = currentUa.toFloat() / 1000f
        var currentMa = rawMa
        if (charging && rawMa < 0f) currentMa = -rawMa
        if (!charging && rawMa > 0f) currentMa = -rawMa

        if (lastCurrent != 0f)
            currentMa = (currentMa * 0.6f) + (lastCurrent * 0.4f)
        lastCurrent = currentMa

        val computedWatt = kotlin.math.abs((voltageV * (currentMa.toDouble() / 1000.0)).toFloat())
        val powerWatt = when {
            powerUwSys != null && powerUwSys != 0L -> kotlin.math.abs(powerUwSys.toFloat() / 1_000_000f)
            else -> computedWatt
        }

        updateRealtimeUsed(currentMa)

        val nowElapsed = SystemClock.elapsedRealtime()
        val nowUptime = SystemClock.uptimeMillis()

        val pm = getSystemService(POWER_SERVICE) as PowerManager
        val interactive = pm.isInteractive

        val currentCharge = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)

        // Attribute consumption since last sample to previous interactive state
        if (lastSampleElapsedMs != 0L) {
            val dt = nowElapsed - lastSampleElapsedMs
            if (dt > 0) {
                val deltaUah = (lastSampleChargeUah - currentCharge).coerceAtLeast(0L)
                val prev = prevInteractiveForSample ?: interactive
                if (prev) consumedOnUah += deltaUah else consumedOffUah += deltaUah
                // Also accumulate percent drop
                val currentPercent = computeCurrentBatteryPercent(level, currentCharge)
                if (!lastSamplePercent.isNaN()) {
                    val dPct = (lastSamplePercent - currentPercent).coerceAtLeast(0.0)
                    if (prev) onPercentDrop += dPct else offPercentDrop += dPct
                }
                lastSamplePercent = currentPercent
            }
        }
        lastSampleElapsedMs = nowElapsed
        lastSampleChargeUah = currentCharge
        prevInteractiveForSample = interactive
        if (lastSamplePercent.isNaN()) {
            lastSamplePercent = computeCurrentBatteryPercent(level, currentCharge)
        }

        // Charging behavior: reset if preferred, otherwise pause window
        if (charging) {
            if (preferenceManager.isMonitorAutoResetOnCharging()) {
                screenOnAccumMs = 0L
                screenOnStartAtElapsed = null
                windowStartElapsed = -1L
                windowStartUptime = -1L
                consumedOnUah = 0L
                consumedOffUah = 0L
                onPercentDrop = 0.0
                offPercentDrop = 0.0
            } else {
                // Not resetting: just ensure we don't keep adding to the active window while charging
                if (windowStartElapsed >= 0L) {
                    val now = SystemClock.elapsedRealtime()
                    windowAccumMs += (now - windowStartElapsed)
                    windowStartElapsed = -1L
                }
                if (windowStartUptime >= 0L) windowStartUptime = -1L
                val start = screenOnStartAtElapsed
                if (start != null) {
                    screenOnAccumMs += (SystemClock.elapsedRealtime() - start)
                    screenOnStartAtElapsed = null
                }
            }
            lastSampleElapsedMs = nowElapsed
            lastSampleChargeUah = currentCharge
            prevInteractiveForSample = interactive
            lastSamplePercent = computeCurrentBatteryPercent(level, currentCharge)
        } else if (windowStartElapsed < 0L || windowStartUptime < 0L) {
            // start window on first non-charging tick
            windowStartElapsed = nowElapsed
            windowStartUptime = nowUptime
            // if screen currently on, start tracking from now
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            screenOnStartAtElapsed = if (pm.isInteractive) nowElapsed else null
            // also initialize sampling baselines
            lastSampleElapsedMs = nowElapsed
            lastSampleChargeUah = currentCharge
            prevInteractiveForSample = interactive
            lastSamplePercent = computeCurrentBatteryPercent(level, currentCharge)
        }

        val currentScreenOnMs = screenOnAccumMs + (screenOnStartAtElapsed?.let { nowElapsed - it } ?: 0L)
        val currentSessionWindowMs = if (windowStartElapsed >= 0L) (nowElapsed - windowStartElapsed) else 0L
        val windowMs = windowAccumMs + currentSessionWindowMs
        val screenOffMs = (windowMs - currentScreenOnMs).coerceAtLeast(0L)

        val awakeMs = if (windowStartUptime >= 0L) (nowUptime - windowStartUptime) else 0L
        val deepSleepMs = (windowMs - awakeMs).coerceAtLeast(0L)
        val awakeOffMs = (awakeMs - currentScreenOnMs).coerceAtLeast(0L)
        // Deep Sleep % of screen-off time, gunakan ms agar akurat; hindari lonjakan jika total < 1s
        val offTotalMsForPct = deepSleepMs + awakeOffMs
        val hasOffWindow = offTotalMsForPct >= 1000L
        val deepSleepPct = if (hasOffWindow) (deepSleepMs * 100.0 / offTotalMsForPct) else 0.0
        val deepSleepPctStr = formatPercent(deepSleepPct, hasOffWindow)
        val deepSleepText = String.format(Locale.US, "%s (%s%%)", formatDurationAdaptive(deepSleepMs), deepSleepPctStr)
        val awakePct = if (hasOffWindow) (awakeOffMs * 100.0 / offTotalMsForPct) else 0.0
        val awakePctStr = formatPercent(awakePct, hasOffWindow)
        val awakeText = String.format(Locale.US, "%s (%s%%)", formatDurationAdaptive(awakeOffMs), awakePctStr)

        // Compute drain strings from cumulative consumption/time
        val onHours = currentScreenOnMs / 3_600_000.0
        val offHours = screenOffMs / 3_600_000.0
        val activeRate = if (onHours > 0) (onPercentDrop / onHours) else 0.0
        val idleRate = if (offHours > 0) (offPercentDrop / offHours) else 0.0
        val activeDrainStr = if (onPercentDrop <= 0.0 || onHours <= 0.0) {
            "0% /hr"
        } else {
            String.format(Locale.US, "%.2f%% /hr", activeRate)
        }
        val idleDrainStr = if (offPercentDrop <= 0.0 || offHours <= 0.0) {
            "0% /hr"
        } else {
            String.format(Locale.US, "%.2f%% /hr", idleRate)
        }

        // Save history every 1 minute
        val nowMs = System.currentTimeMillis()
        if (nowMs - lastHistorySaveTime >= 60_000L) {
            lastHistorySaveTime = nowMs
            if (isUserUnlocked(this)) {
                scope.launch {
                    try {
                        batteryGraphRepository.insertEntry(
                            BatteryGraphEntry(
                                timestamp = nowMs,
                                batteryLevel = level,
                                currentMa = currentMa,
                                isCharging = charging,
                                isScreenOn = interactive,
                                activeDrainRate = activeRate.toFloat(),
                                idleDrainRate = idleRate.toFloat(),
                                temperature = temp
                            )
                        )
                    } catch (_: Exception) { }
                }
            }
            persistState(sync = false)
        }

        return BatteryData(
            level = level,
            temperatureC = temp,
            statusText = getChargingStatus(status, plugged),
            currentMa = currentMa,
            powerWatt = powerWatt,
            activeDrain = activeDrainStr,
            idleDrain = idleDrainStr,
            screenOnTime = formatDurationAdaptive(currentScreenOnMs),
            elapsed = formatDurationAdaptive(screenOffMs),
            uptime = awakeText,
            deepSleep = deepSleepText
        )
    }

    private fun initScreenTracking() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        if (pm.isInteractive) {
            screenOnStartAtElapsed = SystemClock.elapsedRealtime()
            // Instant refresh when service starts and screen is already on
            try {
                scope.launch {
                    val stats = collectSystemStats()
                    updateNotification(stats)
                }
                nextDelayOverrideMs = 5_000L
            } catch (_: Exception) { }
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        if (screenOnStartAtElapsed == null) {
                            screenOnStartAtElapsed = SystemClock.elapsedRealtime()
                        }
                        // Instant refresh on screen ON
                        try {
                            scope.launch {
                                val stats = collectSystemStats()
                                updateNotification(stats)
                            }
                            nextDelayOverrideMs = 5_000L
                        } catch (_: Exception) { }
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        val start = screenOnStartAtElapsed
                        if (start != null) {
                            val now = SystemClock.elapsedRealtime()
                            screenOnAccumMs += (now - start)
                            screenOnStartAtElapsed = null
                        }
                    }
                    Intent.ACTION_USER_PRESENT -> {
                        // User unlocked: ensure latest numbers shown immediately
                        try {
                            scope.launch {
                                val stats = collectSystemStats()
                                updateNotification(stats)
                            }
                            nextDelayOverrideMs = 5_000L
                        } catch (_: Exception) { }
                    }
                }
            }
        }
        screenReceiver = receiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        registerReceiver(receiver, filter)
    }

    private fun initPowerTracking() {
        // Set initial window depending on current charging state
        val sticky = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = sticky?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val plugged = sticky?.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) ?: 0
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || plugged > 0
        if (isCharging) {
            onPowerConnected()
        } else {
            onPowerDisconnected()
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_POWER_CONNECTED -> onPowerConnected()
                    Intent.ACTION_POWER_DISCONNECTED -> onPowerDisconnected()
                }
            }
        }
        powerReceiver = receiver
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
        }
        registerReceiver(receiver, filter)
    }

    private fun onPowerConnected() {
        // Legacy: check history auto-reset
        if (preferenceManager.isAutoResetOnCharging()) {
            fullReset()
        }
        
        // Monitor auto-reset on charging
        if (preferenceManager.isMonitorAutoResetOnCharging()) {
            manualReset()
        } else {
            // Even if we don't reset, we must "close" the current window
            val now = SystemClock.elapsedRealtime()
            if (windowStartElapsed >= 0) {
                windowAccumMs += (now - windowStartElapsed)
                windowStartElapsed = -1L
            }
            if (windowStartUptime >= 0) windowStartUptime = -1L
            val start = screenOnStartAtElapsed
            if (start != null) {
                screenOnAccumMs += (now - start)
                screenOnStartAtElapsed = null
            }
            // And we stop tracking drain baselines for now
            lastSampleElapsedMs = 0L 
        }

        persistState()
        // Tampilkan status charging seketika tanpa menunggu interval berikutnya
        try {
            lastCurrent = 0f
            scope.launch {
                val stats = collectSystemStats()
                updateNotification(stats)
            }
            nextDelayOverrideMs = 5_000L
        } catch (_: Exception) { }
    }

    private fun onPowerDisconnected() {
        if (preferenceManager.isMonitorAutoResetOnCharging()) {
            windowAccumMs = 0L // Start fresh window
            screenOnAccumMs = 0L
            consumedOnUah = 0L
            consumedOffUah = 0L
            onPercentDrop = 0.0
            offPercentDrop = 0.0
        }

        val nowEl = SystemClock.elapsedRealtime()
        val nowUp = SystemClock.uptimeMillis()
        windowStartElapsed = nowEl
        windowStartUptime = nowUp
        
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        screenOnStartAtElapsed = if (pm.isInteractive) nowEl else null
        
        // Set sampling baseline
        val bm = batteryManager
        val charge = bm?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) ?: 0L
        lastSampleElapsedMs = nowEl
        lastSampleChargeUah = charge
        prevInteractiveForSample = pm.isInteractive
        val sticky = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = sticky?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        lastSamplePercent = computeCurrentBatteryPercent(level, charge)
        persistState()
        // Instant refresh to reflect discharging state without waiting for the next tick
        try {
            scope.launch {
                val stats = collectSystemStats()
                updateNotification(stats)
            }
            nextDelayOverrideMs = 5_000L
        } catch (_: Exception) { }
    }

    private fun manualReset() {
        val nowEl = SystemClock.elapsedRealtime()
        val nowUp = SystemClock.uptimeMillis()
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        windowStartElapsed = nowEl
        windowStartUptime = nowUp
        windowAccumMs = 0L
        screenOnAccumMs = 0L
        screenOnStartAtElapsed = if (pm.isInteractive) nowEl else null
        consumedOnUah = 0L
        consumedOffUah = 0L
        onPercentDrop = 0.0
        offPercentDrop = 0.0
        val charge = batteryManager?.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) ?: 0L
        lastSampleElapsedMs = nowEl
        lastSampleChargeUah = charge
        prevInteractiveForSample = pm.isInteractive
        val sticky = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = sticky?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        lastSamplePercent = computeCurrentBatteryPercent(level, charge)
        persistState()
    }

    private fun fullReset() {
        if (isUserUnlocked(this)) {
            scope.launch {
                try {
                    batteryGraphRepository.deleteAllEntries()
                } catch (_: Exception) { }
            }
        }
        manualReset()
        try {
            updateNotification(collectSystemStats())
        } catch (_: Exception) {}
    }

    private fun isUserUnlocked(context: Context): Boolean {
        val um = context.getSystemService(USER_SERVICE) as android.os.UserManager
        return um.isUserUnlocked
    }

    private fun checkChargingControl(level: Int, plugged: Boolean) {
        if (!preferenceManager.isChargingControlEnabled()) {
            lastKnownBypassState = null // Reset cache if disabled
            return
        }
        
        // Only run check if plugged in
        if (plugged) {
            val stopLevel = preferenceManager.getChargingControlStopLevel()
            val resumeLevel = preferenceManager.getChargingControlResumeLevel()
            
            var shouldEnableBypass: Boolean? = null

            // Determine desired state based on levels
            if (level >= stopLevel) {
                shouldEnableBypass = true
            } else if (level <= resumeLevel) {
                shouldEnableBypass = false
            }

            // Apply only if state needs to change or if we don't know the state yet
            if (shouldEnableBypass != null) {
                // If cache is missing, read the actual state once to sync
                if (lastKnownBypassState == null) {
                    lastKnownBypassState = systemRepository.getBypassCharging()
                }

                // Only perform I/O if the desired state differs from the current/cached state
                if (lastKnownBypassState != shouldEnableBypass) {
                    val success = systemRepository.setBypassCharging(shouldEnableBypass)
                    if (success) {
                        lastKnownBypassState = shouldEnableBypass
                        // Force update notification to reflect status change immediately
                        try {
                           scope.launch {
                               updateNotification(collectSystemStats())
                           }
                        } catch (_: Exception) {}
                    }
                }
            }
        } else {
            // Reset cache when unplugged so we verify fresh state next time we plug in
            lastKnownBypassState = null
        }
    }

    private fun checkAutoResetAtLevel(level: Int, isCharging: Boolean) {
        if (isCharging) {
             // History auto-reset
             if (preferenceManager.isAutoResetAtLevel()) {
                 val target = preferenceManager.getAutoResetTargetLevel()
                 if (level >= target && !hasResetForThisChargeCycle) {
                     if (isUserUnlocked(this)) {
                         scope.launch { try { batteryGraphRepository.deleteAllEntries() } catch (_: Exception) { } }
                     }
                     // Don't call fullReset() here as it forces manualReset() too.
                     // We should separate them.
                 }
             }
             
             // Monitor auto-reset
             if (preferenceManager.isMonitorAutoResetAtLevel()) {
                 val target = preferenceManager.getMonitorAutoResetTargetLevel()
                 if (level >= target && !hasResetForThisChargeCycle) {
                     manualReset()
                     hasResetForThisChargeCycle = true
                 }
             } else {
                 // If only history reset was triggered, we still need to manage hasResetForThisChargeCycle flag
                 // but it's shared. Let's assume if EITHER triggers, we set the flag.
                 if (preferenceManager.isAutoResetAtLevel()) {
                     val target = preferenceManager.getAutoResetTargetLevel()
                     if (level >= target) hasResetForThisChargeCycle = true
                 }
             }
        } else {
             hasResetForThisChargeCycle = false
        }
    }

    private fun formatDrainFrom(
        startTimeMs: Long,
        startChargeUah: Long,
        startLevel: Int,
        nowElapsed: Long,
        currentChargeUah: Long,
        currentLevel: Int,
    ): String {
        val elapsedMs = nowElapsed - startTimeMs
        if (elapsedMs <= 0L) return "0.0%/h"
        val hours = elapsedMs / 3_600_000.0
        if (hours <= 0.0) return "0.0%/h"

        val percentDrop = when {
            startLevel >= 0 && currentLevel >= 0 -> (startLevel - currentLevel).toDouble()
            else -> ((startChargeUah - currentChargeUah).toDouble() * 100.0) / designCapacityUah.toDouble()
        }
        if (percentDrop <= 0.0) return "0.0%/h"

        val percentPerHour = percentDrop / hours
        return String.format(Locale.US, "%.2f%%/h", percentPerHour)
    }

    private fun updateRealtimeUsed(currentMa: Float) {
        val now = System.currentTimeMillis()
        val diffSec = (now - lastUpdate) / 1000.0
        lastUpdate = now

        if (currentMa > 0f) {
            val add = (diffSec / 3600.0) * currentMa
            realtimeUsedMah += add
        }
    }

    private fun getDesignCapacityUah(): Long {
        // Try multiple sysfs paths; normalize unit to µAh
        val candidates = listOf(
            "/sys/class/power_supply/battery/charge_full_design",
            "/sys/class/power_supply/battery/charge_full",
            "/sys/class/power_supply/max170xx_battery/charge_full_design",
            "/sys/class/power_supply/max170xx_battery/charge_full"
        )
        for (p in candidates) {
            try {
                val raw = File(p).readText().trim().toLong()
                if (raw > 0) return normalizeToUah(raw)
            } catch (_: Exception) { }
        }
        // Fallback to derive from charge counter and level (no hardcoded value)
        val bm = batteryManager ?: return 0L
        val level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val charge = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER)
        return if (level > 0 && charge > 0) (charge * 100) / level else 0L
    }

    private fun normalizeToUah(raw: Long): Long {
        // Heuristic: some kernels expose in mAh or nAh; normalize to µAh
        return when {
            raw in 1..100_000L -> raw * 1_000L // likely mAh
            raw >= 100_000_000L -> raw / 1_000L // likely nAh
            else -> raw // assume µAh
        }
    }

    // legacy stub removed; baseline is handled via sampling attribution

    private fun getChargingStatus(status: Int, plugged: Int): String {
        return when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
            BatteryManager.BATTERY_STATUS_FULL -> "Full"
            BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
            else -> if (plugged > 0) "Plugged" else "Idle"
        }
    }

    private fun createBatteryIcon(level: Int): IconCompat {
        val size = 24 * 3 // 72px for xhdpi approx
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val paint = Paint().apply {
            color = Color.WHITE
            textSize = size * 0.6f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        val yPos = (canvas.height / 2) - ((paint.descent() + paint.ascent()) / 2)
        canvas.drawText(if (level >= 0) "$level" else "?", canvas.width / 2f, yPos, paint)
        return IconCompat.createWithBitmap(bitmap)
    }

    // === NOTIFICATION ===
    private fun createNotification(title: String, bigText: String, batteryLevel: Int = -1): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
            ?: Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val iconStyle = preferenceManager.getNotificationIconStyle()
        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(null)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentIntent(pendingIntent)
            .setShowWhen(true)
            .setWhen(if (serviceStartedAtMs != 0L) serviceStartedAtMs else System.currentTimeMillis())
            .setOngoing(true)
            .setSilent(true)

        when (iconStyle) {
            PreferenceManager.ICON_STYLE_BATTERY_PERCENT -> {
                builder.setSmallIcon(createBatteryIcon(batteryLevel))
            }
            PreferenceManager.ICON_STYLE_TRANSPARENT -> {
                builder.setSmallIcon(R.drawable.transparent)
            }
            else -> {
                builder.setSmallIcon(R.drawable.jo_png)
            }
        }

        return builder.build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            channelId,
            "Battery Monitor",
            NotificationManager.IMPORTANCE_LOW
        )
        channel.description = "Menampilkan status baterai realtime"
        notificationManager.createNotificationChannel(channel)
    }

    private fun updateNotification(stats: BatteryData) {
        val isChargingLike = stats.statusText == "Charging" || stats.statusText == "Plugged" || stats.statusText == "Full"
        val isFull = stats.statusText == "Full"
        val currentAbsMa = kotlin.math.abs(stats.currentMa)
        val currentDisplayMa = kotlin.math.abs(stats.currentMa.roundToInt())
        val statusLabel = if (isChargingLike && !isFull) {
            when {
                currentAbsMa > 2000f -> "Charging rapidly"
                currentAbsMa in 0.1f..999.9f -> "Charging slowly"
                else -> "Charging"
            }
        } else stats.statusText

        val title = if (isFull) {
            "${stats.level}% · ${stats.temperatureC.roundToInt()}°C · Fully Charged"
        } else {
            val baseTitle = "${stats.level}% · ${stats.temperatureC.roundToInt()}°C · ${statusLabel} · ${currentDisplayMa}mA"
            if (isChargingLike) {
                val wattStr = String.format(Locale.US, "%.1fW", kotlin.math.abs(stats.powerWatt))
                "$baseTitle · $wattStr"
            } else baseTitle
        }
        val onUsedPctStr = "${kotlin.math.max(0, onPercentDrop.roundToInt())}%"
        val offUsedPctStr = "${kotlin.math.max(0, offPercentDrop.roundToInt())}%"
        val bigText = """
            Active drain : ${stats.activeDrain} · idle drain   : ${stats.idleDrain}
            Screen on    : ${stats.screenOnTime} (${onUsedPctStr})
            Screen off   : ${stats.elapsed} (${offUsedPctStr})
            Deep sleep   : ${stats.deepSleep}
            Awake        : ${stats.uptime}
        """.trimIndent()
        notificationManager.notify(notificationId, createNotification(title, bigText, stats.level))
    }

    private fun formatDurationAdaptive(ms: Long): String {
        val totalSec = (ms / 1000).coerceAtLeast(0)
        val h = (totalSec / 3600).toInt()
        val m = ((totalSec % 3600) / 60).toInt()
        val s = (totalSec % 60).toInt()
        return when {
            totalSec < 60 -> String.format(Locale.US, "%ds", s)
            totalSec < 3600 -> String.format(Locale.US, "%dm %ds", m, s)
            else -> String.format(Locale.US, "%dh %dm %ds", h, m, s)
        }
    }

    private fun persistState(sync: Boolean = false) {
        val now = SystemClock.elapsedRealtime()
        val updatedAccum = screenOnAccumMs + (screenOnStartAtElapsed?.let { now - it } ?: 0L)
        val editor = prefs.edit()
            .putLong(keyScreenAccum, updatedAccum)
            .putLong(keyLastElapsed, now)
            .putLong("window_start_elapsed", windowStartElapsed)
            .putLong("window_start_uptime", windowStartUptime)
            .putLong("window_accum_ms", windowAccumMs)
            .putLong("consumed_on_uah", consumedOnUah)
            .putLong("consumed_off_uah", consumedOffUah)
            .putLong("on_percent_drop_bits", java.lang.Double.doubleToRawLongBits(onPercentDrop))
            .putLong("off_percent_drop_bits", java.lang.Double.doubleToRawLongBits(offPercentDrop))
        
        if (sync) {
            editor.apply()
        } else {
            editor.apply()
        }
    }

    private fun restoreStateIfAny() {
        val lastElapsed = prefs.getLong(keyLastElapsed, 0L)
        val savedAccum = prefs.getLong(keyScreenAccum, 0L)
        val savedWindowStart = prefs.getLong("window_start_elapsed", -1L)
        val savedWindowAccum = prefs.getLong("window_accum_ms", 0L)
        
        // Restore drain stats
        val savedConsumedOn = prefs.getLong("consumed_on_uah", 0L)
        val savedConsumedOff = prefs.getLong("consumed_off_uah", 0L)
        val savedOnDrop = java.lang.Double.longBitsToDouble(prefs.getLong("on_percent_drop_bits", 0L))
        val savedOffDrop = java.lang.Double.longBitsToDouble(prefs.getLong("off_percent_drop_bits", 0L))

        val now = SystemClock.elapsedRealtime()
        val isReboot = (lastElapsed == 0L || now < lastElapsed)

        if (isReboot) {
            if (preferenceManager.isMonitorAutoResetOnReboot()) {
                // Reset everything if user requested auto-reset on reboot
                screenOnAccumMs = 0L
                windowAccumMs = 0L
                windowStartElapsed = -1L
                windowStartUptime = -1L
                consumedOnUah = 0L
                consumedOffUah = 0L
                onPercentDrop = 0.0
                offPercentDrop = 0.0
            } else {
                // Persist: Carry over stats
                screenOnAccumMs = savedAccum
                
                // For window duration: add the duration from the previous session
                val prevSessionDuration = if (savedWindowStart >= 0) (lastElapsed - savedWindowStart).coerceAtLeast(0L) else 0L
                windowAccumMs = savedWindowAccum + prevSessionDuration
                
                // Reset start time to now for the new session
                windowStartElapsed = now
                windowStartUptime = SystemClock.uptimeMillis()
                
                // Restore accumulated drain stats
                consumedOnUah = savedConsumedOn
                consumedOffUah = savedConsumedOff
                onPercentDrop = savedOnDrop
                offPercentDrop = savedOffDrop
            }
        } else {
            // Normal restore (service restart, no reboot)
            screenOnAccumMs = savedAccum
            windowAccumMs = savedWindowAccum
            windowStartElapsed = savedWindowStart
            windowStartUptime = prefs.getLong("window_start_uptime", -1L)
            
            consumedOnUah = savedConsumedOn
            consumedOffUah = savedConsumedOff
            onPercentDrop = savedOnDrop
            offPercentDrop = savedOffDrop
        }
    }

    companion object {
        private const val ACTION_RESET = "id.jo.jomanager.action.RESET_BATTERY_MONITOR"
        private const val ACTION_BOOT_START = "id.jo.jomanager.action.BOOT_START_BATTERY_MONITOR"
        private const val ACTION_UPDATE_ICON = "id.jo.jomanager.action.UPDATE_ICON"

        fun start(context: Context, isBoot: Boolean = false) {
            val intent = Intent(context, BatteryMonitorService::class.java)
            if (isBoot) intent.action = ACTION_BOOT_START
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BatteryMonitorService::class.java))
        }

        fun reset(context: Context) {
            val intent = Intent(context, BatteryMonitorService::class.java).apply { action = ACTION_RESET }
            context.startForegroundService(intent)
        }

        fun updateIcon(context: Context) {
            val intent = Intent(context, BatteryMonitorService::class.java).apply { action = ACTION_UPDATE_ICON }
            context.startForegroundService(intent)
        }
    }

    data class BatteryData(
        val level: Int = -1,
        val temperatureC: Float = 0f,
        val statusText: String = "Unknown",
        val currentMa: Float = 0f,
        val powerWatt: Float = 0f,
        val activeDrain: String = "-",
        val idleDrain: String = "-",
        val screenOnTime: String = "-",
        val elapsed: String = "-",
        val uptime: String = "-",
        val deepSleep: String = "-",
    )
}
