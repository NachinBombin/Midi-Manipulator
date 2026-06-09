package com.nachinbombin.midimanipulator.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*

@Composable
fun GlowButton(
    label: String,
    active: Boolean,
    accent: Color,
    accentSoft: Color,
    bg: Color,
    elevated: Color,
    textPrimary: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val glowAlpha by animateFloatAsState(
        targetValue = if (active) 0.75f else if (isPressed) 0.45f else 0f,
        animationSpec = tween(220, easing = FastOutSlowInEasing), label = "gba"
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (active) 1f else if (isPressed) 0.6f else 0.25f,
        animationSpec = tween(200), label = "bba"
    )
    val bgAlpha by animateFloatAsState(
        targetValue = if (active) 0.35f else if (isPressed) 0.2f else 0.08f,
        animationSpec = tween(200), label = "bga"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .drawBehind {
                if (glowAlpha > 0f) {
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                accent.copy(alpha = glowAlpha * 0.6f),
                                accentSoft.copy(alpha = glowAlpha * 0.3f),
                                Color.Transparent
                            ),
                            radius = maxOf(size.width, size.height) * 1.2f
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(28f)
                    )
                }
            }
            .background(
                brush = Brush.verticalGradient(
                    listOf(
                        accent.copy(alpha = bgAlpha + 0.06f),
                        accentSoft.copy(alpha = bgAlpha)
                    )
                ),
                shape = RoundedCornerShape(10.dp)
            )
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    listOf(accent.copy(borderAlpha), accentSoft.copy(borderAlpha * 0.6f))
                ),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(interactionSource = interactionSource, indication = null) { onClick() }
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text       = label,
            color      = if (active) accentSoft else textPrimary.copy(alpha = 0.75f),
            fontSize   = 12.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            letterSpacing = 0.8.sp
        )
    }
}
