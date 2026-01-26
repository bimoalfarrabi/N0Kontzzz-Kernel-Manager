package id.jo.jomanager.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import android.os.UserManager
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale
import androidx.core.content.edit

object LocaleHelper {

    private const val PREFS_NAME = "settings"
    private const val PREF_KEY_APP_LOCALE = "app_locale"

    // Force in-app language picker for all versions due to system intent issues on some devices.
    val useSystemLanguageSettings: Boolean
        get() = false

    fun applyLanguage(context: Context): Context {
        if (useSystemLanguageSettings) {
            return context
        }
        val prefs = getSafePrefs(context)
        val localeTag = prefs?.getString(PREF_KEY_APP_LOCALE, "system") ?: "system"
        
        val locale = when (localeTag) {
            "system" -> null
            else -> parseLocaleTag(localeTag)
        }

        return updateResources(context, locale)
    }

    fun setLocale(context: Context, localeTag: String) {
        val prefs = getSafePrefs(context)
        prefs?.edit { putString(PREF_KEY_APP_LOCALE, localeTag) }

        if (!context.isDeviceProtectedStorage) {
            val deviceContext = context.createDeviceProtectedStorageContext()
            deviceContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
                putString(PREF_KEY_APP_LOCALE, localeTag)
            }
        }

        val localeList = if (localeTag == "system") {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            val appLocale = parseLocaleTag(localeTag)
            LocaleListCompat.create(appLocale)
        }
        
        AppCompatDelegate.setApplicationLocales(localeList)
    }

    fun launchSystemLanguageSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            try {
                val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                    data = Uri.fromParts("package", context.packageName, null)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Failed to open language settings: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun getCurrentLocaleTag(context: Context): String {
        val prefs = getSafePrefs(context)
        return prefs?.getString(PREF_KEY_APP_LOCALE, "system") ?: "system"
    }

    private fun getSafePrefs(context: Context): SharedPreferences? {

        if (context.isDeviceProtectedStorage) {
            return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }

        val userManager = context.getSystemService(UserManager::class.java)
        return if (userManager != null && !userManager.isUserUnlocked) {
            val deviceContext = context.createDeviceProtectedStorageContext()
            deviceContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        } else {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    fun getLocaleDisplayName(context: Context, localeTag: String): String {
        return when (localeTag) {
            "system" -> context.getString(id.jo.jomanager.R.string.theme_system)
            "en" -> "English"
            "in" -> "Indonesia"
            else -> parseLocaleTag(localeTag).displayName
        }
    }

    private fun parseLocaleTag(tag: String): Locale {
        return try {
            if (tag.contains("_")) {
                val parts = tag.split("_")
                Locale.Builder().setLanguage(parts[0]).setRegion(parts.getOrNull(1) ?: "").build()
            } else {
                Locale.Builder().setLanguage(tag).build()
            }
        } catch (e: Exception) {
            Locale.getDefault()
        }
    }

    private fun updateResources(context: Context, locale: Locale?): Context {
        val newConfig = Configuration(context.resources.configuration)
        val targetLocale = locale ?: context.resources.configuration.locales[0]
        
        val localeList = LocaleList(targetLocale)
        newConfig.setLocales(localeList)
        Locale.setDefault(targetLocale)
        
        return context.createConfigurationContext(newConfig)
    }

    fun restartActivity(context: Context) {
        if (context is Activity) {
            context.recreate()
        }
    }
}