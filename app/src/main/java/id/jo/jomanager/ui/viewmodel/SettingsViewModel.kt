package id.jo.jomanager.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import id.jo.jomanager.service.BatteryMonitorService
import id.jo.jomanager.ui.theme.ThemeMode
import id.jo.jomanager.util.ThemeManager
import id.jo.jomanager.utils.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

import id.jo.jomanager.data.repository.BackupRepository
import id.jo.jomanager.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

import android.net.Uri

import id.jo.jomanager.data.model.BackupPreview

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeManager: ThemeManager,
    private val preferenceManager: PreferenceManager,
    private val backupRepository: BackupRepository,
    @param:ApplicationContext private val context: Context
) : ViewModel() {

    private val _notificationIconStyle = MutableStateFlow(preferenceManager.getNotificationIconStyle())
    val notificationIconStyle: StateFlow<Int> = _notificationIconStyle.asStateFlow()

    private val _isBatteryMonitorEnabled = MutableStateFlow(preferenceManager.isBatteryMonitorEnabled())
    val isBatteryMonitorEnabled: StateFlow<Boolean> = _isBatteryMonitorEnabled.asStateFlow()

    private val _backupRestoreEvent = MutableSharedFlow<String>()
    val backupRestoreEvent: SharedFlow<String> = _backupRestoreEvent

    private val _backupPreview = MutableStateFlow<BackupPreview?>(null)
    val backupPreview: StateFlow<BackupPreview?> = _backupPreview.asStateFlow()

    private val _selectedRestoreUri = MutableStateFlow<Uri?>(null)
    val selectedRestoreUri: StateFlow<Uri?> = _selectedRestoreUri.asStateFlow()

    val currentThemeMode: StateFlow<ThemeMode> = themeManager.currentThemeMode
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            ThemeMode.SYSTEM_DEFAULT
        )

    val isAmoledMode: StateFlow<Boolean> = themeManager.isAmoledMode
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            false
        )

    val themeChanged: StateFlow<Boolean> = themeManager.themeChanged
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            false
        )

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            themeManager.setThemeMode(themeMode)
        }
    }

    fun setAmoledMode(enabled: Boolean) {
        viewModelScope.launch {
            themeManager.setAmoledMode(enabled)
        }
    }

    fun setNotificationIconStyle(style: Int) {
        preferenceManager.setNotificationIconStyle(style)
        _notificationIconStyle.value = style
        try {
            BatteryMonitorService.updateIcon(context)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun resetThemeChangedSignal() {
        themeManager.resetThemeChangedSignal()
    }

    fun loadBackupPreview(uri: Uri) {
        viewModelScope.launch {
            val result = backupRepository.getBackupPreview(uri)
            if (result.isSuccess) {
                _backupPreview.value = result.getOrNull()
                _selectedRestoreUri.value = uri
            } else {
                _backupRestoreEvent.emit("${context.getString(R.string.error_restore)}: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun clearBackupPreview() {
        _backupPreview.value = null
        _selectedRestoreUri.value = null
    }

    fun backupSettings(uri: Uri, includeTuning: Boolean, includeNetwork: Boolean, includeBattery: Boolean, includeOther: Boolean) {
        viewModelScope.launch {
            val result = backupRepository.createBackup(uri, includeTuning, includeNetwork, includeBattery, includeOther)
            if (result.isSuccess) {
                _backupRestoreEvent.emit(context.getString(R.string.backup_success))
            } else {
                _backupRestoreEvent.emit("${context.getString(R.string.error_backup)}: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun restoreSettings(uri: Uri, restoreTuning: Boolean, restoreNetwork: Boolean, restoreBattery: Boolean, restoreOther: Boolean) {
        viewModelScope.launch {
            val result = backupRepository.restoreBackup(uri, restoreTuning, restoreNetwork, restoreBattery, restoreOther)
            if (result.isSuccess) {
                _backupRestoreEvent.emit(context.getString(R.string.restore_success))
                // Refresh local states if needed
                _notificationIconStyle.value = preferenceManager.getNotificationIconStyle()
                _isBatteryMonitorEnabled.value = preferenceManager.isBatteryMonitorEnabled()
                clearBackupPreview()
            } else {
                _backupRestoreEvent.emit("${context.getString(R.string.error_restore)}: ${result.exceptionOrNull()?.message}")
            }
        }
    }
}
