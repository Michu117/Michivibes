package com.michu117.michivibes.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class DataStoreManager(private val context: Context) {
    companion object {
        val THEME_KEY = stringPreferencesKey("theme")
        val YEAR_KEY = intPreferencesKey("selected_year")
        val AUTO_EXPORT_KEY = booleanPreferencesKey("auto_export")
        val LANGUAGE_KEY = stringPreferencesKey("language")
        val FIRST_LAUNCH_KEY = booleanPreferencesKey("first_launch")
    }

    val theme: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[THEME_KEY] ?: ThemeMode.SYSTEM
    }

    val selectedYear: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[YEAR_KEY] ?: java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
    }

    val autoExport: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[AUTO_EXPORT_KEY] ?: false
    }

    val language: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[LANGUAGE_KEY] ?: "es"
    }

    val firstLaunch: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[FIRST_LAUNCH_KEY] ?: true
    }

    suspend fun setTheme(theme: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }

    suspend fun setSelectedYear(year: Int) {
        context.dataStore.edit { preferences ->
            preferences[YEAR_KEY] = year
        }
    }

    suspend fun setAutoExport(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_EXPORT_KEY] = enabled
        }
    }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language
        }
    }

    suspend fun setFirstLaunch(isFirst: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FIRST_LAUNCH_KEY] = isFirst
        }
    }

    suspend fun clearAll() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}

object ThemeMode {
    const val LIGHT = "light"
    const val DARK = "dark"
    const val AMOLED = "amoled"
    const val SYSTEM = "system"
}
