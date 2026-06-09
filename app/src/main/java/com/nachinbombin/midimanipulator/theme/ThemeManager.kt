package com.nachinbombin.midimanipulator.theme

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class ThemeManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("midi_perf_prefs", Context.MODE_PRIVATE)

    private val _currentTheme = MutableStateFlow(loadTheme())
    val currentTheme: StateFlow<ThemePreset> = _currentTheme

    private val _currentThemeName = MutableStateFlow(_currentTheme.value.name)
    val currentThemeName: StateFlow<String> = _currentThemeName

    fun setTheme(name: String) {
        val preset = ThemePresets.all.firstOrNull { it.name == name } ?: ThemePresets.Default
        prefs.edit().putString(PREF_THEME, name).apply()
        _currentTheme.value  = preset
        _currentThemeName.value = preset.name
    }

    private fun loadTheme(): ThemePreset {
        val saved = prefs.getString(PREF_THEME, ThemePresets.Default.name)
        return ThemePresets.all.firstOrNull { it.name == saved } ?: ThemePresets.Default
    }

    companion object {
        private const val PREF_THEME = "selected_theme"
    }
}
