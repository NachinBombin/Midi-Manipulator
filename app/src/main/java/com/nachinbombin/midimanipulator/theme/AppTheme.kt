package com.nachinbombin.midimanipulator.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun AppTheme(preset: ThemePreset, content: @Composable () -> Unit) {
    val isLight = preset.name == "White Wolf"
    val colorScheme = if (isLight) {
        lightColorScheme(
            primary   = preset.accent,
            secondary = preset.accentSoft,
            tertiary  = preset.accentAlt,
            background= preset.bg,
            surface   = preset.bgElevated,
            onPrimary = preset.bg,
            onBackground = preset.textPrimary,
            onSurface    = preset.textPrimary
        )
    } else {
        darkColorScheme(
            primary   = preset.accent,
            secondary = preset.accentSoft,
            tertiary  = preset.accentAlt,
            background= preset.bg,
            surface   = preset.bgElevated,
            onPrimary = Color(0xFF000000),
            onBackground = preset.textPrimary,
            onSurface    = preset.textPrimary
        )
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
