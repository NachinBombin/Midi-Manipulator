package com.nachinbombin.midimanipulator.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun VerticalSlider(
    value: Float,           // 0..1
    locked: Boolean = false,
    accent: Color,
    accentSoft: Color,
    bg: Color,
    elevated: Color,
    textMuted: Color,
    width: Dp = 44.dp,
    height: Dp = 160.dp,
    onValueChange: (Float) -> Unit,
    onLockToggle: (() -> Unit)? = null
) {
    var isDragging by remember { mutableStateOf(false) }

    // Glow intensity
    val glowAlpha by animateFloatAsState(
        targetValue = if (isDragging) 0.7f else if (locked) 0.45f else 0.0f,
        animationSpec = tween(250, easing = FastOutSlowInEasing), label = "sg"
    )
    val thumbScale by animateFloatAsState(
        targetValue = if (isDragging) 1.3f else 1f,
        animationSpec = spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessHigh), label = "ts"
    )
    // Spring-back when not locked & released
    val displayValue by animateFloatAsState(
        targetValue = value,
        animationSpec = if (!isDragging && !locked)
            spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium)
        else
            tween(0), label = "sv"
    )

    Canvas(
        modifier = Modifier
            .size(width, height)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd   = { isDragging = false },
                    onDragCancel= { isDragging = false },
                    onDrag      = { change, _ ->
                        change.consume()
                        val v = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                        onValueChange(v)
                    }
                )
            }
    ) {
        val w  = size.width
        val h  = size.height
        val cx = w / 2f
        val trackW  = w * 0.28f
        val trackLeft   = cx - trackW / 2f
        val trackRight  = cx + trackW / 2f
        val thumbY  = h - displayValue * h
        val thumbR  = (w * 0.34f) * thumbScale

        // ── Track background ────────────────────────────────────────────────
        drawRoundRect(
            brush       = Brush.verticalGradient(
                colors  = listOf(elevated, bg),
                startY  = 0f, endY = h
            ),
            topLeft     = Offset(trackLeft, 0f),
            size        = Size(trackW, h),
            cornerRadius= CornerRadius(trackW / 2f)
        )

        // ── Filled portion (bottom → thumb) ─────────────────────────────────
        if (displayValue > 0.01f) {
            drawRoundRect(
                brush       = Brush.verticalGradient(
                    colors  = listOf(accentSoft.copy(alpha = 0.25f), accent.copy(alpha = 0.85f)),
                    startY  = thumbY, endY = h
                ),
                topLeft     = Offset(trackLeft, thumbY),
                size        = Size(trackW, h - thumbY),
                cornerRadius= CornerRadius(trackW / 2f)
            )
        }

        // ── Track border ────────────────────────────────────────────────────
        drawRoundRect(
            color       = if (isDragging) accent.copy(0.55f) else textMuted.copy(0.18f),
            topLeft     = Offset(trackLeft, 0f),
            size        = Size(trackW, h),
            cornerRadius= CornerRadius(trackW / 2f),
            style       = Stroke(1.5f)
        )

        // ── Glow corona behind thumb ─────────────────────────────────────────
        if (glowAlpha > 0.01f) {
            drawCircle(
                brush  = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = glowAlpha * 0.85f),
                        accentSoft.copy(alpha = glowAlpha * 0.4f),
                        Color.Transparent
                    ),
                    center = Offset(cx, thumbY),
                    radius = thumbR * 3.5f
                ),
                radius = thumbR * 3.5f,
                center = Offset(cx, thumbY)
            )
        }

        // ── Thumb sphere ────────────────────────────────────────────────────
        val thumbColor = if (locked) accent.copy(alpha = 0.9f) else accent
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color.White.copy(alpha = 0.4f),
                    thumbColor,
                    accentSoft.copy(alpha = 0.7f)
                ),
                center = Offset(cx - thumbR * 0.2f, thumbY - thumbR * 0.2f),
                radius = thumbR
            ),
            radius = thumbR,
            center = Offset(cx, thumbY)
        )
        // Thumb rim
        drawCircle(
            color  = Color.White.copy(alpha = if (isDragging) 0.55f else 0.25f),
            radius = thumbR,
            center = Offset(cx, thumbY),
            style  = Stroke(if (isDragging) 2.0f else 1.2f)
        )

        // ── Lock indicator dot ───────────────────────────────────────────────
        if (locked) {
            drawCircle(
                color  = Color(0xFFFF4466),
                radius = 5f,
                center = Offset(cx + thumbR + 7f, thumbY)
            )
        }
    }
}
