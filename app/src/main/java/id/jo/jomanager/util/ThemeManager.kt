/*
 * Copyright (c) 2025 Rve <rve27github @gmail.com>
 * All Rights Reserved.
 */
package id.jo.jomanager.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import id.jo.jomanager.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeManager @Inject constructor(
    private val context: Context,
    private val dataStore: DataStore<Preferences>
) {
    companion object {
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val AMOLED_MODE_KEY = booleanPreferencesKey("amoled_mode")
    }

    private val _themeChanged = MutableStateFlow(false)
    val themeChanged: StateFlow<Boolean> = _themeChanged.asStateFlow()

    val currentThemeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        val themeModeString = preferences[THEME_MODE_KEY] ?: ThemeMode.SYSTEM_DEFAULT.name
        // Safely convert string to ThemeMode, falling back to SYSTEM_DEFAULT if invalid
        try {
            ThemeMode.valueOf(themeModeString.uppercase())
        } catch (e: IllegalArgumentException) {
            // Handle legacy values or invalid values
            when (themeModeString.lowercase()) {
                "system" -> ThemeMode.SYSTEM_DEFAULT
                "light" -> ThemeMode.LIGHT
                "dark" -> ThemeMode.DARK
                else -> ThemeMode.SYSTEM_DEFAULT
            }
        }
    }

    val isAmoledMode: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[AMOLED_MODE_KEY] ?: false
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = themeMode.name
        }
        
        // Apply the theme mode to the app
        when (themeMode) {
            ThemeMode.LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            ThemeMode.DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            ThemeMode.SYSTEM_DEFAULT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
        
        // Beri sinyal bahwa tema telah berubah
        _themeChanged.value = true
    }

    suspend fun setAmoledMode(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[AMOLED_MODE_KEY] = enabled
        }
        _themeChanged.value = true
    }

    // Fungsi untuk mereset sinyal setelah tema diterapkan
    fun resetThemeChangedSignal() {
        _themeChanged.value = false
    }
}