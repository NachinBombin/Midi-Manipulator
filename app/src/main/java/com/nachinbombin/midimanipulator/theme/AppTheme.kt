package com.nachinbombin.midimanipulator.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun AppTheme(preset: ThemePreset, content: @Composable () -> Unit) {
    val isLight = preset.id == ThemePresets.WhiteWolf.id

    val colorScheme = if (isLight) lightColorScheme(
        primary    = preset.accent,
        background = preset.bg,
        surface    = preset.bgElevated,
        onPrimary  = preset.bg,
        onBackground = preset.textPrimary,
        onSurface  = preset.textPrimary
    ) else darkColorScheme(
        primary    = preset.accent,
        background = preset.bg,
        surface    = preset.bgElevated,
        onPrimary  = preset.bg,
        onBackground = preset.textPrimary,
        onSurface  = preset.textPrimary
    )

    MaterialTheme(colorScheme = colorScheme, content = content)
}
