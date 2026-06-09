package com.nachinbombin.midimanipulator.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun AppTheme(preset: ThemePreset, content: @Composable () -> Unit) {
    // FIX: onPrimary was hardcoded to Color(0xFF000000) for dark themes.
    // Use each preset's textPrimary as the text-on-accent colour, which is
    // correct for all themes (dark themes use light textPrimary, White Wolf
    // uses dark textPrimary — the preset already encodes this correctly).
    val isLight = preset.name == "White Wolf"
    val colorScheme = if (isLight) {
        lightColorScheme(
            primary      = preset.accent,
            secondary    = preset.accentSoft,
            tertiary     = preset.accentAlt,
            background   = preset.bg,
            surface      = preset.bgElevated,
            onPrimary    = preset.bg,
            onBackground = preset.textPrimary,
            onSurface    = preset.textPrimary
        )
    } else {
        darkColorScheme(
            primary      = preset.accent,
            secondary    = preset.accentSoft,
            tertiary     = preset.accentAlt,
            background   = preset.bg,
            surface      = preset.bgElevated,
            onPrimary    = preset.textPrimary,   // was hardcoded black — fixed
            onBackground = preset.textPrimary,
            onSurface    = preset.textPrimary
        )
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
