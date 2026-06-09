package com.nachinbombin.midimanipulator.theme

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ThemeManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "midi_manipulator_prefs"
        private const val KEY_THEME_ID = "theme_id"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _currentTheme = MutableStateFlow(
        ThemePresets.all[prefs.getInt(KEY_THEME_ID, 0)]
    )
    val currentTheme: StateFlow<ThemePreset> = _currentTheme

    fun setTheme(preset: ThemePreset) {
        _currentTheme.value = preset
        prefs.edit { putInt(KEY_THEME_ID, preset.id) }
    }
}
