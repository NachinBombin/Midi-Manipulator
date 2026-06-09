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
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun StrumStrip(
    label: String,
    noteCount: Int,         // ticks in the strip (e.g. 12 for 3 octaves × 4 notes)
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
    var lastX by remember { mutableStateOf(-1f) }
    var activeIndex by remember { mutableStateOf(-1) }
    var rippleX by remember { mutableStateOf(0f) }
    var rippleAlpha by remember { mutableStateOf(0f) }
    val rippleAnim = remember { Animatable(0f) }

    // Glow when active
    val glowAlpha by animateFloatAsState(
        targetValue = if (activeIndex >= 0) 0.55f else if (locked) 0.3f else 0f,
        animationSpec = tween(200), label = "sg"
    )

    val textMeasurer = rememberTextMeasurer()

    suspend fun triggerRipple(x: Float) {
        rippleX = x
        rippleAnim.snapTo(0f)
        rippleAnim.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
    }

    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) triggerRipple(rippleX)
    }

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .pointerInput(locked) {
                    if (locked) return@pointerInput
                    detectDragGestures(
                        onDragStart = { off ->
                            lastX = off.x
                            rippleX = off.x
                        },
                        onDragEnd    = {
                            if (activeIndex >= 0) onNoteOff(activeIndex)
                            activeIndex = -1; lastX = -1f
                        },
                        onDragCancel = {
                            if (activeIndex >= 0) onNoteOff(activeIndex)
                            activeIndex = -1; lastX = -1f
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val w = size.width
                            val pct = (change.position.x / w).coerceIn(0f, 1f)
                            val idx = (pct * noteCount).toInt().coerceIn(0, noteCount - 1)
                            val swipeVel = if (lastX >= 0f)
                                abs(change.position.x - lastX).coerceAtMost(120f) / 120f
                            else 0.5f
                            val vel = (40 + swipeVel * 87f).roundToInt()
                            if (idx != activeIndex) {
                                if (activeIndex >= 0) onNoteOff(activeIndex)
                                onNoteOn(idx, vel)
                                activeIndex = idx
                                rippleX = change.position.x
                            }
                            lastX = change.position.x
                        }
                    )
                }
                .pointerInput(locked) {
                    if (locked) return@pointerInput
                    detectTapGestures(
                        onTap = { off ->
                            val pct = (off.x / size.width).coerceIn(0f, 1f)
                            val idx = (pct * noteCount).toInt().coerceIn(0, noteCount - 1)
                            onNoteOn(idx, 80)
                            onNoteOff(idx)
                        }
                    )
                }
        ) {
            val w = size.width
            val h = size.height
            val tickW = w / noteCount

            // ── Background gradient ─────────────────────────────────────────
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(elevated.copy(alpha = 0.9f), bg)
                ),
                cornerRadius = CornerRadius(10f)
            )

            // ── Glow wash over active zone ──────────────────────────────────
            if (glowAlpha > 0f) {
                val glowX = if (activeIndex >= 0) (activeIndex + 0.5f) * tickW else rippleX
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            accent.copy(alpha = glowAlpha * 0.4f),
                            accentSoft.copy(alpha = glowAlpha * 0.55f),
                            accent.copy(alpha = glowAlpha * 0.4f),
                            Color.Transparent
                        ),
                        startX = glowX - w * 0.25f,
                        endX   = glowX + w * 0.25f
                    )
                )
            }

            // ── Ripple circle ───────────────────────────────────────────────
            val rp = rippleAnim.value
            if (rp > 0f && rp < 1f) {
                drawCircle(
                    color  = accent.copy(alpha = (1f - rp) * 0.55f),
                    radius = rp * w * 0.38f,
                    center = Offset(rippleX, h / 2f)
                )
            }

            // ── Tick marks ─────────────────────────────────────────────────
            for (i in 0 until noteCount) {
                val tx = (i + 0.5f) * tickW
                val isActive = i == activeIndex
                // Tall tick for octave boundaries
                val isOctave = i % (noteCount / 3) == 0
                val tickH = when {
                    isActive -> h * 0.78f
                    isOctave -> h * 0.52f
                    else     -> h * 0.35f
                }
                // Glow behind active tick
                if (isActive) {
                    drawLine(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.Transparent, accent.copy(0.6f), Color.Transparent),
                            startY = (h - tickH) / 2f, endY = (h + tickH) / 2f
                        ),
                        start       = Offset(tx, (h - tickH) / 2f),
                        end         = Offset(tx, (h + tickH) / 2f),
                        strokeWidth = 5f
                    )
                }
                drawLine(
                    color       = if (isActive) accentSoft else if (isOctave) textMuted.copy(0.6f) else textMuted.copy(0.25f),
                    start       = Offset(tx, (h - tickH) / 2f),
                    end         = Offset(tx, (h + tickH) / 2f),
                    strokeWidth = if (isActive) 2.5f else if (isOctave) 1.8f else 1f
                )
            }

            // ── Border ─────────────────────────────────────────────────────
            drawRoundRect(
                color        = if (locked) Color(0xFFFF4466).copy(0.6f) else accent.copy(if (activeIndex >= 0) 0.5f else 0.15f),
                cornerRadius = CornerRadius(10f),
                style        = Stroke(width = 1.8f)
            )
        }
    }
}
