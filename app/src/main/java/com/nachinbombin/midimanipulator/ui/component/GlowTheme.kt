package com.nachinbombin.midimanipulator.ui.component

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode

object GlowTheme {
    fun glowBrush(accent: Color, soft: Color, alpha: Float = 1f): Brush =
        Brush.radialGradient(
            colors = listOf(
                soft.copy(alpha = alpha * 0.85f),
                accent.copy(alpha = alpha * 0.55f),
                Color.Transparent
            ),
            radius = 420f
        )

    fun accentGradient(accent: Color, soft: Color): Brush =
        Brush.linearGradient(listOf(accent, soft, accent))

    fun verticalTrackGradient(accent: Color, soft: Color, bg: Color): Brush =
        Brush.verticalGradient(listOf(accent, soft, bg))

    fun sectorGradient(accent: Color, soft: Color): Brush =
        Brush.sweepGradient(
            colorStops = arrayOf(
                0f  to accent.copy(alpha = 0.9f),
                0.5f to soft.copy(alpha = 0.7f),
                1f  to accent.copy(alpha = 0.9f)
            )
        )

    fun cardGradient(bg: Color, elevated: Color): Brush =
        Brush.verticalGradient(listOf(elevated, bg))
}
