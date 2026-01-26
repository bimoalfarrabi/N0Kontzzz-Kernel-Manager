package id.jo.jomanager.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

import id.jo.jomanager.R

data class Language(val code: String, val displayNameResId: Int)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val LANGUAGE_KEY = stringPreferencesKey("app_language")

    val supportedLanguages = listOf(
        Language("en", R.string.language_english),
        Language("id", R.string.language_indonesian)
    )

    private val _language = MutableStateFlow(supportedLanguages[0]) // Default to English
    val language: StateFlow<Language> = _language.asStateFlow()

    init {
        viewModelScope.launch {
            dataStore.data
                .map { preferences ->
                    preferences[LANGUAGE_KEY] ?: "en" // Default to English
                }.first().let { savedLanguageCode ->
                    _language.value = supportedLanguages.find { it.code == savedLanguageCode } ?: supportedLanguages[0]
                }
        }
    }

    fun setLanguage(lang: Language) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[LANGUAGE_KEY] = lang.code
            }
            _language.value = lang
        }
    }
}
