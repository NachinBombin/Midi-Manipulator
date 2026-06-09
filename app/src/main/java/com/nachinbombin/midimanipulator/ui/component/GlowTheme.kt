package com.nachinbombin.midimanipulator.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nachinbombin.midimanipulator.theme.ThemePreset

/**
 * FIX: GlowCard was referenced in PerformanceScreen but the file was missing.
 * A themed card surface with a subtle accent glow border used throughout the performance UI.
 */
@Composable
fun GlowCard(
    theme: ThemePreset,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                drawRoundRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            theme.accent.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        radius = maxOf(size.width, size.height) * 0.8f
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(28f)
                )
            }
            .background(theme.bgElevated, RoundedCornerShape(12.dp))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        theme.accent.copy(alpha = 0.25f),
                        theme.borderSubtle.copy(alpha = 0.5f)
                    )
                ),
                shape = RoundedCornerShape(12.dp)
            ),
        content = content
    )
}
