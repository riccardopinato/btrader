package com.utilitylab.easydebloat.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.utilitylab.easydebloat.model.AppSettings
import com.utilitylab.easydebloat.model.DefaultMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore("easy_debloat_settings")

class SettingsStore(private val context: Context) {
    private object Keys {
        val defaultMode = stringPreferencesKey("default_mode")
        val showUnknown = booleanPreferencesKey("show_unknown")
        val showCritical = booleanPreferencesKey("show_critical")
        val dynamicColor = booleanPreferencesKey("dynamic_color")
        val haptics = booleanPreferencesKey("haptics")
    }

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            defaultMode = runCatching { DefaultMode.valueOf(prefs[Keys.defaultMode] ?: DefaultMode.DISABLE.name) }.getOrDefault(DefaultMode.DISABLE),
            showUnknown = prefs[Keys.showUnknown] ?: true,
            showCritical = prefs[Keys.showCritical] ?: true,
            dynamicColor = prefs[Keys.dynamicColor] ?: true,
            haptics = prefs[Keys.haptics] ?: true
        )
    }

    suspend fun setDefaultMode(mode: DefaultMode) = context.settingsDataStore.edit { it[Keys.defaultMode] = mode.name }
    suspend fun setShowUnknown(value: Boolean) = context.settingsDataStore.edit { it[Keys.showUnknown] = value }
    suspend fun setShowCritical(value: Boolean) = context.settingsDataStore.edit { it[Keys.showCritical] = value }
    suspend fun setDynamicColor(value: Boolean) = context.settingsDataStore.edit { it[Keys.dynamicColor] = value }
    suspend fun setHaptics(value: Boolean) = context.settingsDataStore.edit { it[Keys.haptics] = value }
}
