package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.localization.AppLanguage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "adriel_school_prefs")

class AppPreferences(private val context: Context) {

    private val KEY_DARK_MODE = booleanPreferencesKey("dark_mode")
    private val KEY_LANGUAGE = stringPreferencesKey("language")
    private val KEY_OFFLINE_ENTRY = booleanPreferencesKey("offline_entry_mode")

    val darkModeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_DARK_MODE] ?: false
    }

    val languageFlow: Flow<AppLanguage> = context.dataStore.data.map { prefs ->
        when (prefs[KEY_LANGUAGE]) {
            "EN" -> AppLanguage.EN
            else -> AppLanguage.FR
        }
    }

    val offlineEntryModeFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_OFFLINE_ENTRY] ?: false
    }

    suspend fun setDarkMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_DARK_MODE] = enabled }
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.dataStore.edit { it[KEY_LANGUAGE] = language.code }
    }

    suspend fun setOfflineEntryMode(enabled: Boolean) {
        context.dataStore.edit { it[KEY_OFFLINE_ENTRY] = enabled }
    }
}
