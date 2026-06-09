package com.nachinbombin.midimanipulator.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun StrumStrip(
    label: String,
    noteCount: Int,
    accent: Color,
    accentSoft: Color,
    bg: Color,
    elevated: Color,
    textPrimary: Color,
    textMuted: Color,
    locked: Boolean,
    onLockToggle: () -> Unit,
    onNoteOn:  (noteIndex: Int, velocity: Int) -> Unit,
    onNoteOff: (noteIndex: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var lastX      by remember { mutableStateOf(-1f) }
    var activeIndex by remember { mutableStateOf(-1) }
    val rippleAnim  = remember { Animatable(0f) }
    var rippleX     by remember { mutableStateOf(0f) }

    val glowAlpha by animateFloatAsState(
        targetValue   = if (activeIndex >= 0) 0.55f else if (locked) 0.3f else 0f,
        animationSpec = tween(200), label = "sg"
    )

    val textMeasurer = rememberTextMeasurer()

    // FIX: LaunchedEffect body was truncated — completed ripple trigger
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            rippleAnim.snapTo(0f)
            rippleAnim.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
        }
    }

    fun noteIndexAt(x: Float, width: Float): Int {
        if (width <= 0f || noteCount <= 0) return 0
        return ((x / width) * noteCount).toInt().coerceIn(0, noteCount - 1)
    }

    fun velocityFromDx(dx: Float): Int {
        val speed = abs(dx).coerceIn(1f, 300f)
        return (speed / 300f * 127f).toInt().coerceIn(20, 127)
    }

    Box(
        modifier = modifier.height(36.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(locked) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (!locked) {
                                rippleX = offset.x
                                val idx = noteIndexAt(offset.x, size.width.toFloat())
                                if (idx != activeIndex) {
                                    if (activeIndex >= 0) onNoteOff(activeIndex)
                                    onNoteOn(idx, 80)
                                    activeIndex = idx
                                    lastX = offset.x
                                    coroutineScope.launch {
                                        rippleAnim.snapTo(0f)
                                        rippleAnim.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
                                    }
                                }
                            }
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (!locked) {
                                val idx = noteIndexAt(change.position.x, size.width.toFloat())
                                if (idx != activeIndex) {
                                    // Strum continuity: send new NoteOn BEFORE NoteOff of previous
                                    val vel = velocityFromDx(dragAmount.x)
                                    onNoteOn(idx, vel)
                                    if (activeIndex >= 0) onNoteOff(activeIndex)
                                    activeIndex = idx
                                    rippleX = change.position.x
                                    coroutineScope.launch {
                                        rippleAnim.snapTo(0f)
                                        rippleAnim.animateTo(1f, tween(280, easing = FastOutSlowInEasing))
                                    }
                                }
                                lastX = change.position.x
                            }
                        },
                        onDragEnd = {
                            if (!locked && activeIndex >= 0) {
                                onNoteOff(activeIndex)
                                activeIndex = -1
                            }
                        },
                        onDragCancel = {
                            if (!locked && activeIndex >= 0) {
                                onNoteOff(activeIndex)
                                activeIndex = -1
                            }
                        }
                    )
                }
                .pointerInput(locked) {
                    detectTapGestures(
                        onTap = { offset ->
                            if (!locked) {
                                val idx = noteIndexAt(offset.x, size.width.toFloat())
                                onNoteOn(idx, 100)
                                // Short pluck: fire NoteOff after a brief delay
                                coroutineScope.launch {
                                    kotlinx.coroutines.delay(120)
                                    onNoteOff(idx)
                                }
                            }
                        },
                        onLongPress = { offset ->
                            if (!locked) {
                                val idx = noteIndexAt(offset.x, size.width.toFloat())
                                onNoteOn(idx, 110)
                                activeIndex = idx
                            }
                        }
                    )
                }
        ) {
            val w = size.width
            val h = size.height
            val stripH = h * 0.55f
            val topY   = (h - stripH) / 2f

            // Track background
            drawRoundRect(
                brush = Brush.horizontalGradient(
                    listOf(elevated, bg.copy(alpha = 0.7f), elevated)
                ),
                topLeft = Offset(0f, topY),
                size    = Size(w, stripH),
                cornerRadius = CornerRadius(stripH / 2f)
            )

            // Glow overlay when active
            if (glowAlpha > 0f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        listOf(
                            accentSoft.copy(alpha = glowAlpha * 0.4f),
                            accent.copy(alpha = glowAlpha * 0.6f),
                            accentSoft.copy(alpha = glowAlpha * 0.4f)
                        )
                    ),
                    topLeft = Offset(0f, topY),
                    size    = Size(w, stripH),
                    cornerRadius = CornerRadius(stripH / 2f)
                )
            }

            // Track border
            drawRoundRect(
                color  = accent.copy(alpha = if (activeIndex >= 0 || locked) 0.55f else 0.15f),
                topLeft = Offset(0f, topY),
                size    = Size(w, stripH),
                cornerRadius = CornerRadius(stripH / 2f),
                style  = Stroke(width = 1.2f)
            )

            // Tick marks
            val tickSpacing = w / noteCount
            for (i in 0 until noteCount) {
                val tx      = tickSpacing * i + tickSpacing / 2f
                val isActive = i == activeIndex
                val tickH   = if (isActive) stripH * 0.9f else stripH * 0.55f
                val tickTop = topY + (stripH - tickH) / 2f
                drawLine(
                    color       = if (isActive) accent else textMuted.copy(alpha = 0.35f),
                    start       = Offset(tx, tickTop),
                    end         = Offset(tx, tickTop + tickH),
                    strokeWidth = if (isActive) 2.5f else 1f
                )
            }

            // Ripple
            val ra = rippleAnim.value
            if (ra > 0f && ra < 1f) {
                drawCircle(
                    brush  = Brush.radialGradient(
                        listOf(accent.copy(alpha = (1f - ra) * 0.55f), Color.Transparent),
                        center = Offset(rippleX, h / 2f),
                        radius = ra * w * 0.35f
                    ),
                    radius = ra * w * 0.35f,
                    center = Offset(rippleX, h / 2f)
                )
            }
        }
    }
}
