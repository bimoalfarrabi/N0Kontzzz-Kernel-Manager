package id.jo.jomanager.ui

import android.app.Activity
import android.os.Build
import android.os.Bundle
import id.jo.jomanager.service.BatteryMonitorService

class WarmStartActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val sp = getSharedPreferences("jo_preferences", MODE_PRIVATE)
            if (sp.getBoolean("battery_monitor_enabled", false)) {
                BatteryMonitorService.start(this)
            }
        } catch (_: Throwable) { }
        finish()
        if (Build.VERSION.SDK_INT >= 34) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}



