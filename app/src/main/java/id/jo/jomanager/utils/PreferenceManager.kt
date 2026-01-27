package id.jo.jomanager.utils

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class PreferenceManager @Inject constructor(
    @field:ApplicationContext private val context: Context,
) {
    companion object {
        private const val KEY_TARGET_GAME_PACKAGES = "target_game_packages"
        private const val KEY_KGSL_SKIP_ZEROING = "kgsl_skip_zeroing"
        private const val KEY_AVOID_DIRTY_PTE = "avoid_dirty_pte"
        private const val KEY_BYPASS_CHARGING = "bypass_charging"
        private const val KEY_FORCE_FAST_CHARGE = "force_fast_charge"
        private const val KEY_BATTERY_MONITOR_ENABLED = "battery_monitor_enabled"

        private const val KEY_CHARGING_CONTROL_ENABLED = "charging_control_enabled"
        private const val KEY_CHARGING_CONTROL_STOP_LEVEL = "charging_control_stop_level"
        private const val KEY_CHARGING_CONTROL_RESUME_LEVEL = "charging_control_resume_level"

        private const val KEY_AUTO_RESET_ON_REBOOT = "auto_reset_on_reboot"
        private const val KEY_AUTO_RESET_ON_CHARGING = "auto_reset_on_charging"
        private const val KEY_AUTO_RESET_AT_LEVEL = "auto_reset_at_level"
        private const val KEY_AUTO_RESET_TARGET_LEVEL = "auto_reset_target_level"

        private const val KEY_MONITOR_AUTO_RESET_ON_REBOOT = "monitor_auto_reset_on_reboot"
        private const val KEY_MONITOR_AUTO_RESET_ON_CHARGING = "monitor_auto_reset_on_charging"
        private const val KEY_MONITOR_AUTO_RESET_AT_LEVEL = "monitor_auto_reset_at_level"
        private const val KEY_MONITOR_AUTO_RESET_TARGET_LEVEL = "monitor_auto_reset_target_level"

        private const val KEY_NOTIFICATION_ICON_STYLE = "notification_icon_style"

        const val ICON_STYLE_BATTERY_PERCENT = 0
        const val ICON_STYLE_APP_LOGO = 1
        const val ICON_STYLE_TRANSPARENT = 2

        private const val KEY_TCP_CONGESTION_ALGORITHM = "tcp_congestion_algorithm"
        private const val KEY_IO_SCHEDULER = "io_scheduler"
        private const val KEY_APPLY_NETWORK_STORAGE_ON_BOOT = "apply_network_storage_on_boot"
        private const val KEY_LAST_APPLIED_BOOT_ID = "last_applied_boot_id"

        // Set on Boot Keys
        private const val KEY_APPLY_PERFORMANCE_MODE_ON_BOOT = "apply_performance_mode_on_boot"
        private const val KEY_APPLY_CPU_ON_BOOT = "apply_cpu_on_boot"
        private const val KEY_APPLY_GPU_ON_BOOT = "apply_gpu_on_boot"
        private const val KEY_APPLY_THERMAL_ON_BOOT = "apply_thermal_on_boot"
        private const val KEY_APPLY_RAM_ON_BOOT = "apply_ram_on_boot"

        // GPU
        private const val KEY_GPU_GOVERNOR = "gpu_governor"
        private const val KEY_GPU_MIN_FREQ = "gpu_min_freq"
        private const val KEY_GPU_MAX_FREQ = "gpu_max_freq"
        private const val KEY_GPU_POWER_LEVEL = "gpu_power_level"
        private const val KEY_GPU_THROTTLING = "gpu_throttling"

        // RAM
        private const val KEY_ZRAM_ENABLED_PREF = "zram_enabled_pref"
        private const val KEY_ZRAM_DISKSIZE = "zram_disksize"
        private const val KEY_ZRAM_COMPRESSION = "zram_compression"
        private const val KEY_SWAPPINESS = "swappiness"
        private const val KEY_DIRTY_RATIO = "dirty_ratio"
        private const val KEY_DIRTY_BACKGROUND_RATIO = "dirty_background_ratio"
        private const val KEY_DIRTY_WRITEBACK = "dirty_writeback"
        private const val KEY_DIRTY_EXPIRE = "dirty_expire"
        private const val KEY_MIN_FREE_MEMORY = "min_free_memory"

        // CPU Per-cluster
        private const val KEY_CPU_GOV_PREFIX = "cpu_gov_"
        private const val KEY_CPU_MIN_FREQ_PREFIX = "cpu_min_freq_"
        private const val KEY_CPU_MAX_FREQ_PREFIX = "cpu_max_freq_"
        private const val KEY_CPU_CORE_ONLINE_PREFIX = "cpu_core_online_"

        // Performance Mode
        private const val KEY_PERFORMANCE_MODE = "last_applied_performance_mode"
    }

    private fun credentialPrefs(): SharedPreferences? {
        return try {
            context.getSharedPreferences("jo_preferences", Context.MODE_PRIVATE)
        } catch (_: IllegalStateException) {
            null
        }
    }

    private fun deviceProtectedPrefs(): SharedPreferences {
        val deviceContext = context.createDeviceProtectedStorageContext()
        return deviceContext.getSharedPreferences("jo_preferences", Context.MODE_PRIVATE)
    }

    fun setTargetGamePackages(packages: Set<String>) {
        credentialPrefs()?.edit {
            putStringSet(KEY_TARGET_GAME_PACKAGES, packages)
        }
    }

    fun getTargetGamePackages(): Set<String> {
        credentialPrefs()?.getStringSet(KEY_TARGET_GAME_PACKAGES, null)?.let { return it }
        return deviceProtectedPrefs().getStringSet(KEY_TARGET_GAME_PACKAGES, emptySet()) ?: emptySet()
    }

    fun setKgslSkipZeroing(enabled: Boolean) {
        credentialPrefs()?.edit {
            putBoolean(KEY_KGSL_SKIP_ZEROING, enabled)
        }
    }

    fun getKgslSkipZeroing(): Boolean {
        credentialPrefs()?.getBoolean(KEY_KGSL_SKIP_ZEROING, false)?.let { return it }
        return deviceProtectedPrefs().getBoolean(KEY_KGSL_SKIP_ZEROING, false)
    }

    fun setAvoidDirtyPte(enabled: Boolean) {
        credentialPrefs()?.edit {
            putBoolean(KEY_AVOID_DIRTY_PTE, enabled)
        }
        deviceProtectedPrefs().edit {
            putBoolean(KEY_AVOID_DIRTY_PTE, enabled)
        }
    }

    fun getAvoidDirtyPte(): Boolean {
        credentialPrefs()?.let { prefs ->
            if (prefs.contains(KEY_AVOID_DIRTY_PTE)) {
                return prefs.getBoolean(KEY_AVOID_DIRTY_PTE, false)
            }
        }
        return deviceProtectedPrefs().getBoolean(KEY_AVOID_DIRTY_PTE, false)
    }

    fun setBypassCharging(enabled: Boolean) {
        credentialPrefs()?.edit {
            putBoolean(KEY_BYPASS_CHARGING, enabled)
        }
    }

    fun getBypassCharging(): Boolean {
        credentialPrefs()?.getBoolean(KEY_BYPASS_CHARGING, false)?.let { return it }
        return deviceProtectedPrefs().getBoolean(KEY_BYPASS_CHARGING, false)
    }

    fun setForceFastCharge(enabled: Boolean) {
        credentialPrefs()?.edit {
            putBoolean(KEY_FORCE_FAST_CHARGE, enabled)
        }
    }

    fun getForceFastCharge(): Boolean {
        credentialPrefs()?.getBoolean(KEY_FORCE_FAST_CHARGE, false)?.let { return it }
        return deviceProtectedPrefs().getBoolean(KEY_FORCE_FAST_CHARGE, false)
    }

    fun setBatteryMonitorEnabled(enabled: Boolean) {
        credentialPrefs()?.edit {
            putBoolean(KEY_BATTERY_MONITOR_ENABLED, enabled)
        }
        deviceProtectedPrefs().edit {
            putBoolean(KEY_BATTERY_MONITOR_ENABLED, enabled)
        }
    }

    fun isBatteryMonitorEnabled(): Boolean {
        val primary = credentialPrefs()?.getBoolean(KEY_BATTERY_MONITOR_ENABLED, false)
        if (primary == true) return true
        return deviceProtectedPrefs().getBoolean(KEY_BATTERY_MONITOR_ENABLED, false)
    }

    fun setChargingControlEnabled(enabled: Boolean) {
        credentialPrefs()?.edit { putBoolean(KEY_CHARGING_CONTROL_ENABLED, enabled) }
        deviceProtectedPrefs().edit { putBoolean(KEY_CHARGING_CONTROL_ENABLED, enabled) }
    }

    fun isChargingControlEnabled(): Boolean {
        return credentialPrefs()?.getBoolean(KEY_CHARGING_CONTROL_ENABLED, false)
            ?: deviceProtectedPrefs().getBoolean(KEY_CHARGING_CONTROL_ENABLED, false)
    }

    fun setChargingControlStopLevel(level: Int) {
        credentialPrefs()?.edit { putInt(KEY_CHARGING_CONTROL_STOP_LEVEL, level) }
        deviceProtectedPrefs().edit { putInt(KEY_CHARGING_CONTROL_STOP_LEVEL, level) }
    }

    fun getChargingControlStopLevel(): Int {
        return credentialPrefs()?.getInt(KEY_CHARGING_CONTROL_STOP_LEVEL, 80)
            ?: deviceProtectedPrefs().getInt(KEY_CHARGING_CONTROL_STOP_LEVEL, 80)
    }

    fun setChargingControlResumeLevel(level: Int) {
        credentialPrefs()?.edit { putInt(KEY_CHARGING_CONTROL_RESUME_LEVEL, level) }
        deviceProtectedPrefs().edit { putInt(KEY_CHARGING_CONTROL_RESUME_LEVEL, level) }
    }

    fun getChargingControlResumeLevel(): Int {
        return credentialPrefs()?.getInt(KEY_CHARGING_CONTROL_RESUME_LEVEL, 70)
            ?: deviceProtectedPrefs().getInt(KEY_CHARGING_CONTROL_RESUME_LEVEL, 70)
    }

    fun setAutoResetOnReboot(enabled: Boolean) {
        credentialPrefs()?.edit { putBoolean(KEY_AUTO_RESET_ON_REBOOT, enabled) }
        deviceProtectedPrefs().edit { putBoolean(KEY_AUTO_RESET_ON_REBOOT, enabled) }
    }

    fun isAutoResetOnReboot(): Boolean {
        return credentialPrefs()?.getBoolean(KEY_AUTO_RESET_ON_REBOOT, false)
            ?: deviceProtectedPrefs().getBoolean(KEY_AUTO_RESET_ON_REBOOT, false)
    }

    fun setAutoResetOnCharging(enabled: Boolean) {
        credentialPrefs()?.edit { putBoolean(KEY_AUTO_RESET_ON_CHARGING, enabled) }
        deviceProtectedPrefs().edit { putBoolean(KEY_AUTO_RESET_ON_CHARGING, enabled) }
    }

    fun isAutoResetOnCharging(): Boolean {
        return credentialPrefs()?.getBoolean(KEY_AUTO_RESET_ON_CHARGING, false)
            ?: deviceProtectedPrefs().getBoolean(KEY_AUTO_RESET_ON_CHARGING, false)
    }

    fun setAutoResetAtLevel(enabled: Boolean) {
        credentialPrefs()?.edit { putBoolean(KEY_AUTO_RESET_AT_LEVEL, enabled) }
        deviceProtectedPrefs().edit { putBoolean(KEY_AUTO_RESET_AT_LEVEL, enabled) }
    }

    fun isAutoResetAtLevel(): Boolean {
        return credentialPrefs()?.getBoolean(KEY_AUTO_RESET_AT_LEVEL, false)
            ?: deviceProtectedPrefs().getBoolean(KEY_AUTO_RESET_AT_LEVEL, false)
    }

    fun setAutoResetTargetLevel(level: Int) {
        credentialPrefs()?.edit { putInt(KEY_AUTO_RESET_TARGET_LEVEL, level) }
        deviceProtectedPrefs().edit { putInt(KEY_AUTO_RESET_TARGET_LEVEL, level) }
    }

    fun getAutoResetTargetLevel(): Int {
        return credentialPrefs()?.getInt(KEY_AUTO_RESET_TARGET_LEVEL, 90)
            ?: deviceProtectedPrefs().getInt(KEY_AUTO_RESET_TARGET_LEVEL, 90)
    }

    fun setMonitorAutoResetOnReboot(enabled: Boolean) {
        credentialPrefs()?.edit { putBoolean(KEY_MONITOR_AUTO_RESET_ON_REBOOT, enabled) }
        deviceProtectedPrefs().edit { putBoolean(KEY_MONITOR_AUTO_RESET_ON_REBOOT, enabled) }
    }

    fun isMonitorAutoResetOnReboot(): Boolean {
        return credentialPrefs()?.getBoolean(KEY_MONITOR_AUTO_RESET_ON_REBOOT, false)
            ?: deviceProtectedPrefs().getBoolean(KEY_MONITOR_AUTO_RESET_ON_REBOOT, false)
    }

    fun setMonitorAutoResetOnCharging(enabled: Boolean) {
        credentialPrefs()?.edit { putBoolean(KEY_MONITOR_AUTO_RESET_ON_CHARGING, enabled) }
        deviceProtectedPrefs().edit { putBoolean(KEY_MONITOR_AUTO_RESET_ON_CHARGING, enabled) }
    }

    fun isMonitorAutoResetOnCharging(): Boolean {
        return credentialPrefs()?.getBoolean(KEY_MONITOR_AUTO_RESET_ON_CHARGING, false)
            ?: deviceProtectedPrefs().getBoolean(KEY_MONITOR_AUTO_RESET_ON_CHARGING, false)
    }

    fun setMonitorAutoResetAtLevel(enabled: Boolean) {
        credentialPrefs()?.edit { putBoolean(KEY_MONITOR_AUTO_RESET_AT_LEVEL, enabled) }
        deviceProtectedPrefs().edit { putBoolean(KEY_MONITOR_AUTO_RESET_AT_LEVEL, enabled) }
    }

    fun isMonitorAutoResetAtLevel(): Boolean {
        return credentialPrefs()?.getBoolean(KEY_MONITOR_AUTO_RESET_AT_LEVEL, false)
            ?: deviceProtectedPrefs().getBoolean(KEY_MONITOR_AUTO_RESET_AT_LEVEL, false)
    }

    fun setMonitorAutoResetTargetLevel(level: Int) {
        credentialPrefs()?.edit { putInt(KEY_MONITOR_AUTO_RESET_TARGET_LEVEL, level) }
        deviceProtectedPrefs().edit { putInt(KEY_MONITOR_AUTO_RESET_TARGET_LEVEL, level) }
    }

    fun getMonitorAutoResetTargetLevel(): Int {
        return credentialPrefs()?.getInt(KEY_MONITOR_AUTO_RESET_TARGET_LEVEL, 90)
            ?: deviceProtectedPrefs().getInt(KEY_MONITOR_AUTO_RESET_TARGET_LEVEL, 90)
    }

    fun setNotificationIconStyle(style: Int) {
        credentialPrefs()?.edit { putInt(KEY_NOTIFICATION_ICON_STYLE, style) }
        deviceProtectedPrefs().edit { putInt(KEY_NOTIFICATION_ICON_STYLE, style) }
    }

    fun getNotificationIconStyle(): Int {
        return credentialPrefs()?.getInt(KEY_NOTIFICATION_ICON_STYLE, ICON_STYLE_APP_LOGO)
            ?: deviceProtectedPrefs().getInt(KEY_NOTIFICATION_ICON_STYLE, ICON_STYLE_APP_LOGO)
    }

    fun setTcpCongestionAlgorithm(algorithm: String?) {
        credentialPrefs()?.edit {
            if (algorithm == null) remove(KEY_TCP_CONGESTION_ALGORITHM)
            else putString(KEY_TCP_CONGESTION_ALGORITHM, algorithm)
        }
        deviceProtectedPrefs().edit {
            if (algorithm == null) remove(KEY_TCP_CONGESTION_ALGORITHM)
            else putString(KEY_TCP_CONGESTION_ALGORITHM, algorithm)
        }
    }

    fun getTcpCongestionAlgorithm(): String? {
        return credentialPrefs()?.getString(KEY_TCP_CONGESTION_ALGORITHM, null)
            ?: deviceProtectedPrefs().getString(KEY_TCP_CONGESTION_ALGORITHM, null)
    }

    fun setIoScheduler(scheduler: String?) {
        credentialPrefs()?.edit {
            if (scheduler == null) remove(KEY_IO_SCHEDULER)
            else putString(KEY_IO_SCHEDULER, scheduler)
        }
        deviceProtectedPrefs().edit {
            if (scheduler == null) remove(KEY_IO_SCHEDULER)
            else putString(KEY_IO_SCHEDULER, scheduler)
        }
    }

    fun getIoScheduler(): String? {
        return credentialPrefs()?.getString(KEY_IO_SCHEDULER, null)
            ?: deviceProtectedPrefs().getString(KEY_IO_SCHEDULER, null)
    }

    fun setApplyNetworkStorageOnBoot(enabled: Boolean) {
        credentialPrefs()?.edit { putBoolean(KEY_APPLY_NETWORK_STORAGE_ON_BOOT, enabled) }
        deviceProtectedPrefs().edit { putBoolean(KEY_APPLY_NETWORK_STORAGE_ON_BOOT, enabled) }
    }

    fun isApplyNetworkStorageOnBoot(): Boolean {
        return credentialPrefs()?.getBoolean(KEY_APPLY_NETWORK_STORAGE_ON_BOOT, false)
            ?: deviceProtectedPrefs().getBoolean(KEY_APPLY_NETWORK_STORAGE_ON_BOOT, false)
    }

    fun setLastAppliedBootId(bootId: String) {
        credentialPrefs()?.edit { putString(KEY_LAST_APPLIED_BOOT_ID, bootId) }
        deviceProtectedPrefs().edit { putString(KEY_LAST_APPLIED_BOOT_ID, bootId) }
    }

    fun getLastAppliedBootId(): String? {
        return credentialPrefs()?.getString(KEY_LAST_APPLIED_BOOT_ID, null)
            ?: deviceProtectedPrefs().getString(KEY_LAST_APPLIED_BOOT_ID, null)
    }

    // Set on Boot Methods
    fun setApplyPerformanceModeOnBoot(enabled: Boolean) {
        credentialPrefs()?.edit { putBoolean(KEY_APPLY_PERFORMANCE_MODE_ON_BOOT, enabled) }
        deviceProtectedPrefs().edit { putBoolean(KEY_APPLY_PERFORMANCE_MODE_ON_BOOT, enabled) }
    }

    fun isApplyPerformanceModeOnBoot(): Boolean {
        return credentialPrefs()?.getBoolean(KEY_APPLY_PERFORMANCE_MODE_ON_BOOT, false)
            ?: deviceProtectedPrefs().getBoolean(KEY_APPLY_PERFORMANCE_MODE_ON_BOOT, false)
    }

    fun setApplyCpuOnBoot(enabled: Boolean) {
        credentialPrefs()?.edit { putBoolean(KEY_APPLY_CPU_ON_BOOT, enabled) }
        deviceProtectedPrefs().edit { putBoolean(KEY_APPLY_CPU_ON_BOOT, enabled) }
    }

    fun isApplyCpuOnBoot(): Boolean {
        return credentialPrefs()?.getBoolean(KEY_APPLY_CPU_ON_BOOT, false)
            ?: deviceProtectedPrefs().getBoolean(KEY_APPLY_CPU_ON_BOOT, false)
    }

    fun setApplyGpuOnBoot(enabled: Boolean) {
        credentialPrefs()?.edit { putBoolean(KEY_APPLY_GPU_ON_BOOT, enabled) }
        deviceProtectedPrefs().edit { putBoolean(KEY_APPLY_GPU_ON_BOOT, enabled) }
    }

    fun isApplyGpuOnBoot(): Boolean {
        return credentialPrefs()?.getBoolean(KEY_APPLY_GPU_ON_BOOT, false)
            ?: deviceProtectedPrefs().getBoolean(KEY_APPLY_GPU_ON_BOOT, false)
    }

    fun setApplyThermalOnBoot(enabled: Boolean) {
        credentialPrefs()?.edit { putBoolean(KEY_APPLY_THERMAL_ON_BOOT, enabled) }
        deviceProtectedPrefs().edit { putBoolean(KEY_APPLY_THERMAL_ON_BOOT, enabled) }
    }

    fun isApplyThermalOnBoot(): Boolean {
        return credentialPrefs()?.getBoolean(KEY_APPLY_THERMAL_ON_BOOT, false)
            ?: deviceProtectedPrefs().getBoolean(KEY_APPLY_THERMAL_ON_BOOT, false)
    }

    fun setApplyRamOnBoot(enabled: Boolean) {
        credentialPrefs()?.edit { putBoolean(KEY_APPLY_RAM_ON_BOOT, enabled) }
        deviceProtectedPrefs().edit { putBoolean(KEY_APPLY_RAM_ON_BOOT, enabled) }
    }

    fun isApplyRamOnBoot(): Boolean {
        return credentialPrefs()?.getBoolean(KEY_APPLY_RAM_ON_BOOT, false)
            ?: deviceProtectedPrefs().getBoolean(KEY_APPLY_RAM_ON_BOOT, false)
    }

    // GPU
    fun setGpuGovernor(governor: String?) {
        credentialPrefs()?.edit {
            if (governor == null) remove(KEY_GPU_GOVERNOR)
            else putString(KEY_GPU_GOVERNOR, governor)
        }
    }

    fun getGpuGovernor(): String? {
        return credentialPrefs()?.getString(KEY_GPU_GOVERNOR, null)
    }

    fun setGpuMinFreq(freq: Int) {
        credentialPrefs()?.edit { putInt(KEY_GPU_MIN_FREQ, freq) }
    }

    fun getGpuMinFreq(): Int {
        return credentialPrefs()?.getInt(KEY_GPU_MIN_FREQ, -1) ?: -1
    }

    fun setGpuMaxFreq(freq: Int) {
        credentialPrefs()?.edit { putInt(KEY_GPU_MAX_FREQ, freq) }
    }

    fun getGpuMaxFreq(): Int {
        return credentialPrefs()?.getInt(KEY_GPU_MAX_FREQ, -1) ?: -1
    }

    fun setGpuPowerLevel(level: Int) {
        credentialPrefs()?.edit { putInt(KEY_GPU_POWER_LEVEL, level) }
    }

    fun getGpuPowerLevel(): Int {
        return credentialPrefs()?.getInt(KEY_GPU_POWER_LEVEL, -1) ?: -1
    }

    fun setGpuThrottling(enabled: Boolean) {
        credentialPrefs()?.edit { putBoolean(KEY_GPU_THROTTLING, enabled) }
    }

    fun getGpuThrottling(): Boolean? {
        if (credentialPrefs()?.contains(KEY_GPU_THROTTLING) == true) {
            return credentialPrefs()?.getBoolean(KEY_GPU_THROTTLING, false)
        }
        return null
    }

    // RAM
    fun setZramEnabledPref(enabled: Boolean) {
        credentialPrefs()?.edit { putBoolean(KEY_ZRAM_ENABLED_PREF, enabled) }
        deviceProtectedPrefs().edit { putBoolean(KEY_ZRAM_ENABLED_PREF, enabled) }
    }

    fun isZramEnabledPref(): Boolean {
        // Default to true if not set, or maybe false?
        // Logic: If user never touched it, assume enabled if device has it?
        // Safer default: true (so existing users don't lose ZRAM on update),
        // but checking key existence is better.
        val creds = credentialPrefs()
        if (creds != null && creds.contains(KEY_ZRAM_ENABLED_PREF)) {
            return creds.getBoolean(KEY_ZRAM_ENABLED_PREF, true)
        }
        return deviceProtectedPrefs().getBoolean(KEY_ZRAM_ENABLED_PREF, true)
    }

    fun setZramDisksize(size: Long) {
        credentialPrefs()?.edit { putLong(KEY_ZRAM_DISKSIZE, size) }
        deviceProtectedPrefs().edit { putLong(KEY_ZRAM_DISKSIZE, size) }
    }

    fun getZramDisksize(): Long {
        return credentialPrefs()?.getLong(KEY_ZRAM_DISKSIZE, -1L)
            ?: deviceProtectedPrefs().getLong(KEY_ZRAM_DISKSIZE, -1L)
    }

    fun setZramCompression(algo: String?) {
        credentialPrefs()?.edit {
            if (algo == null) remove(KEY_ZRAM_COMPRESSION)
            else putString(KEY_ZRAM_COMPRESSION, algo)
        }
        deviceProtectedPrefs().edit {
            if (algo == null) remove(KEY_ZRAM_COMPRESSION)
            else putString(KEY_ZRAM_COMPRESSION, algo)
        }
    }

    fun getZramCompression(): String? {
        return credentialPrefs()?.getString(KEY_ZRAM_COMPRESSION, null)
            ?: deviceProtectedPrefs().getString(KEY_ZRAM_COMPRESSION, null)
    }

    fun setSwappiness(value: Int) {
        credentialPrefs()?.edit { putInt(KEY_SWAPPINESS, value) }
        deviceProtectedPrefs().edit { putInt(KEY_SWAPPINESS, value) }
    }

    fun getSwappiness(): Int {
        return credentialPrefs()?.getInt(KEY_SWAPPINESS, -1)
            ?.takeIf { it != -1 }
            ?: deviceProtectedPrefs().getInt(KEY_SWAPPINESS, -1)
    }

    fun setDirtyRatio(value: Int) {
        credentialPrefs()?.edit { putInt(KEY_DIRTY_RATIO, value) }
        deviceProtectedPrefs().edit { putInt(KEY_DIRTY_RATIO, value) }
    }

    fun getDirtyRatio(): Int {
        return credentialPrefs()?.getInt(KEY_DIRTY_RATIO, -1)
            ?.takeIf { it != -1 }
            ?: deviceProtectedPrefs().getInt(KEY_DIRTY_RATIO, -1)
    }

    fun setDirtyBackgroundRatio(value: Int) {
        credentialPrefs()?.edit { putInt(KEY_DIRTY_BACKGROUND_RATIO, value) }
        deviceProtectedPrefs().edit { putInt(KEY_DIRTY_BACKGROUND_RATIO, value) }
    }

    fun getDirtyBackgroundRatio(): Int {
        return credentialPrefs()?.getInt(KEY_DIRTY_BACKGROUND_RATIO, -1)
            ?.takeIf { it != -1 }
            ?: deviceProtectedPrefs().getInt(KEY_DIRTY_BACKGROUND_RATIO, -1)
    }

    fun setDirtyWriteback(value: Int) {
        credentialPrefs()?.edit { putInt(KEY_DIRTY_WRITEBACK, value) }
        deviceProtectedPrefs().edit { putInt(KEY_DIRTY_WRITEBACK, value) }
    }

    fun getDirtyWriteback(): Int {
        return credentialPrefs()?.getInt(KEY_DIRTY_WRITEBACK, -1)
            ?.takeIf { it != -1 }
            ?: deviceProtectedPrefs().getInt(KEY_DIRTY_WRITEBACK, -1)
    }

    fun setDirtyExpire(value: Int) {
        credentialPrefs()?.edit { putInt(KEY_DIRTY_EXPIRE, value) }
        deviceProtectedPrefs().edit { putInt(KEY_DIRTY_EXPIRE, value) }
    }

    fun getDirtyExpire(): Int {
        return credentialPrefs()?.getInt(KEY_DIRTY_EXPIRE, -1)
            ?.takeIf { it != -1 }
            ?: deviceProtectedPrefs().getInt(KEY_DIRTY_EXPIRE, -1)
    }

    fun setMinFreeMemory(value: Int) {
        credentialPrefs()?.edit { putInt(KEY_MIN_FREE_MEMORY, value) }
        deviceProtectedPrefs().edit { putInt(KEY_MIN_FREE_MEMORY, value) }
    }

    fun getMinFreeMemory(): Int {
        return credentialPrefs()?.getInt(KEY_MIN_FREE_MEMORY, -1)
            ?.takeIf { it != -1 }
            ?: deviceProtectedPrefs().getInt(KEY_MIN_FREE_MEMORY, -1)
    }

    // CPU Per-cluster
    fun setCpuGov(cluster: String, gov: String) {
        credentialPrefs()?.edit { putString(KEY_CPU_GOV_PREFIX + cluster, gov) }
        deviceProtectedPrefs().edit { putString(KEY_CPU_GOV_PREFIX + cluster, gov) }
    }

    fun getCpuGov(cluster: String): String? {
        return credentialPrefs()?.getString(KEY_CPU_GOV_PREFIX + cluster, null)
            ?: deviceProtectedPrefs().getString(KEY_CPU_GOV_PREFIX + cluster, null)
    }

    fun setCpuMinFreq(cluster: String, freq: Int) {
        credentialPrefs()?.edit { putInt(KEY_CPU_MIN_FREQ_PREFIX + cluster, freq) }
        deviceProtectedPrefs().edit { putInt(KEY_CPU_MIN_FREQ_PREFIX + cluster, freq) }
    }

    fun getCpuMinFreq(cluster: String): Int {
        return credentialPrefs()?.getInt(KEY_CPU_MIN_FREQ_PREFIX + cluster, -1)
            ?.takeIf { it != -1 }
            ?: deviceProtectedPrefs().getInt(KEY_CPU_MIN_FREQ_PREFIX + cluster, -1)
    }

    fun setCpuMaxFreq(cluster: String, freq: Int) {
        credentialPrefs()?.edit { putInt(KEY_CPU_MAX_FREQ_PREFIX + cluster, freq) }
        deviceProtectedPrefs().edit { putInt(KEY_CPU_MAX_FREQ_PREFIX + cluster, freq) }
    }

    fun getCpuMaxFreq(cluster: String): Int {
        return credentialPrefs()?.getInt(KEY_CPU_MAX_FREQ_PREFIX + cluster, -1)
            ?.takeIf { it != -1 }
            ?: deviceProtectedPrefs().getInt(KEY_CPU_MAX_FREQ_PREFIX + cluster, -1)
    }

    fun setCpuCoreOnline(coreId: Int, online: Boolean) {
        credentialPrefs()?.edit { putBoolean(KEY_CPU_CORE_ONLINE_PREFIX + coreId, online) }
        deviceProtectedPrefs().edit { putBoolean(KEY_CPU_CORE_ONLINE_PREFIX + coreId, online) }
    }

    fun getCpuCoreOnline(coreId: Int): Boolean? {
        if (credentialPrefs()?.contains(KEY_CPU_CORE_ONLINE_PREFIX + coreId) == true) {
            return credentialPrefs()?.getBoolean(KEY_CPU_CORE_ONLINE_PREFIX + coreId, true)
        }
        if (deviceProtectedPrefs().contains(KEY_CPU_CORE_ONLINE_PREFIX + coreId)) {
            return deviceProtectedPrefs().getBoolean(KEY_CPU_CORE_ONLINE_PREFIX + coreId, true)
        }
        return null
    }

    // Performance Mode
    fun setPerformanceMode(mode: String) {
        credentialPrefs()?.edit { putString(KEY_PERFORMANCE_MODE, mode) }
        deviceProtectedPrefs().edit { putString(KEY_PERFORMANCE_MODE, mode) }
    }

    fun getPerformanceMode(): String {
        return credentialPrefs()?.getString(KEY_PERFORMANCE_MODE, null)
            ?: deviceProtectedPrefs().getString(KEY_PERFORMANCE_MODE, "Balanced") ?: "Balanced"
    }
}
