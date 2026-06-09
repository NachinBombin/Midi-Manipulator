package com.nachinbombin.midimanipulator.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.*
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
import kotlinx.coroutines.*
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
    val coroutineScope  = rememberCoroutineScope()
    var lastX           by remember { mutableStateOf(-1f) }
    var lastEventTimeMs by remember { mutableStateOf(0L) }
    var activeIndex     by remember { mutableStateOf(-1) }
    // Track which notes are currently sustained (long-press drone)
    val sustainedNotes  = remember { mutableStateSetOf<Int>() }
    val rippleAnim      = remember { Animatable(0f) }
    var rippleX         by remember { mutableStateOf(0f) }
    // Long-press job per note index
    val longPressJobs   = remember { mutableMapOf<Int, Job>() }

    val glowAlpha by animateFloatAsState(
        targetValue   = if (activeIndex >= 0 || sustainedNotes.isNotEmpty()) 0.55f
                        else if (locked) 0.3f else 0f,
        animationSpec = tween(200), label = "sg"
    )

    val textMeasurer = rememberTextMeasurer()

    // Fire ripple whenever active index changes
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0) {
            rippleAnim.snapTo(0f)
            rippleAnim.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
        }
    }

    // When strip is locked, cancel long-press jobs
    LaunchedEffect(locked) {
        if (locked) {
            longPressJobs.values.forEach { it.cancel() }
            longPressJobs.clear()
        }
    }

    fun noteIndexAt(x: Float, width: Float): Int {
        if (width <= 0f || noteCount <= 0) return 0
        return ((x / width) * noteCount).toInt().coerceIn(0, noteCount - 1)
    }

    /** Derive inter-note delay from swipe speed: fast swipe → 8ms, slow → 20ms */
    fun strumDelayMs(dx: Float): Long {
        val speed = abs(dx).coerceIn(1f, 400f)
        return (20L - ((speed / 400f) * 12f).toLong()).coerceIn(8L, 20L)
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
                    // ── Drag / Strum ──────────────────────────────────────────
                    detectDragGestures(
                        onDragStart = { offset ->
                            if (!locked) {
                                rippleX = offset.x
                                val idx = noteIndexAt(offset.x, size.width.toFloat())
                                lastX = offset.x
                                lastEventTimeMs = System.currentTimeMillis()
                                if (idx != activeIndex) {
                                    // FIX: NoteOn BEFORE NoteOff to prevent gaps
                                    onNoteOn(idx, 80)
                                    if (activeIndex >= 0) onNoteOff(activeIndex)
                                    activeIndex = idx
                                    coroutineScope.launch {
                                        rippleAnim.snapTo(0f)
                                        rippleAnim.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
                                    }
                                }
                            }
                        },
                        onDragEnd = {
                            if (!locked) {
                                if (activeIndex >= 0) onNoteOff(activeIndex)
                                activeIndex = -1
                                lastX = -1f
                            }
                        },
                        onDragCancel = {
                            if (!locked) {
                                if (activeIndex >= 0) onNoteOff(activeIndex)
                                activeIndex = -1
                                lastX = -1f
                            }
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            if (!locked) {
                                val w   = size.width.toFloat()
                                val idx = noteIndexAt(change.position.x, w)
                                val dx  = change.position.x - lastX

                                if (idx != activeIndex) {
                                    val vel      = velocityFromDx(dx)
                                    val delayMs  = strumDelayMs(dx)

                                    coroutineScope.launch {
                                        // FIX: emit NoteOn for next note BEFORE NoteOff for current
                                        // so there is never a gap between consecutive strum notes.
                                        onNoteOn(idx, vel)
                                        delay(delayMs)
                                        if (activeIndex >= 0) onNoteOff(activeIndex)
                                    }
                                    activeIndex = idx
                                    rippleX = change.position.x
                                    coroutineScope.launch {
                                        rippleAnim.snapTo(0f)
                                        rippleAnim.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
                                    }
                                }
                                lastX = change.position.x
                            }
                        }
                    )
                }
                .pointerInput(locked) {
                    // ── Tap (pluck) + Long-press (drone) ─────────────────────
                    detectTapGestures(
                        onTap = { offset ->
                            if (!locked) {
                                val idx = noteIndexAt(offset.x, size.width.toFloat())
                                onNoteOn(idx, 90)
                                coroutineScope.launch {
                                    delay(120)
                                    onNoteOff(idx)
                                }
                            }
                        },
                        onLongPress = { offset ->
                            if (!locked) {
                                val idx = noteIndexAt(offset.x, size.width.toFloat())
                                if (!sustainedNotes.contains(idx)) {
                                    sustainedNotes.add(idx)
                                    onNoteOn(idx, 100)
                                    // Keep note on until pointer is released
                                    longPressJobs[idx] = coroutineScope.launch {
                                        // The note stays on; it will be released by the
                                        // detectDragGestures onDragEnd or by strip unlock.
                                        awaitCancellation()
                                    }
                                }
                            }
                        },
                        onPress = { offset ->
                            // Release any long-press sustained note on finger up
                            tryAwaitRelease()
                            val idx = noteIndexAt(offset.x, size.width.toFloat())
                            if (sustainedNotes.contains(idx)) {
                                sustainedNotes.remove(idx)
                                onNoteOff(idx)
                                longPressJobs[idx]?.cancel()
                                longPressJobs.remove(idx)
                            }
                        }
                    )
                }
        ) {
            val w   = size.width
            val h   = size.height
            val mid = h / 2f

            // ── Background track ────────────────────────────────────────────
            drawRoundRect(
                color        = elevated,
                cornerRadius = CornerRadius(h / 2f),
                size         = size
            )

            // ── Glow overlay when active ─────────────────────────────────
            if (glowAlpha > 0f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        listOf(
                            accent.copy(alpha = glowAlpha * 0.4f),
                            accentSoft.copy(alpha = glowAlpha * 0.6f),
                            accent.copy(alpha = glowAlpha * 0.4f)
                        )
                    ),
                    cornerRadius = CornerRadius(h / 2f),
                    size         = size
                )
            }

            // ── Tick marks ──────────────────────────────────────────────────
            val spacing = w / noteCount.toFloat()
            for (i in 0 until noteCount) {
                val tx = spacing * i + spacing / 2f
                val isActive = i == activeIndex || sustainedNotes.contains(i)
                val tickH = if (isActive) h * 0.85f else h * 0.5f
                val tickColor = if (isActive) accent
                                else textMuted.copy(alpha = if (locked) 0.35f else 0.22f)
                drawLine(
                    color       = tickColor,
                    start       = Offset(tx, mid - tickH / 2f),
                    end         = Offset(tx, mid + tickH / 2f),
                    strokeWidth = if (isActive) 2.5f else 1.2f,
                    cap         = StrokeCap.Round
                )
            }

            // ── Ripple ──────────────────────────────────────────────────────
            if (rippleAnim.value > 0f && rippleAnim.value < 1f) {
                val rr = (h / 2f) + rippleAnim.value * (h * 1.4f)
                drawCircle(
                    color  = accent.copy(alpha = (1f - rippleAnim.value) * 0.45f),
                    radius = rr,
                    center = Offset(rippleX, mid),
                    style  = Stroke(width = 2f)
                )
            }

            // ── Active position highlight ───────────────────────────────────
            if (activeIndex >= 0) {
                val ax = spacing * activeIndex + spacing / 2f
                drawCircle(
                    brush  = Brush.radialGradient(
                        listOf(accent.copy(0.7f), Color.Transparent),
                        center = Offset(ax, mid),
                        radius = h * 0.8f
                    ),
                    radius = h * 0.8f,
                    center = Offset(ax, mid)
                )
            }

            // ── Locked freeze indicator ─────────────────────────────────────
            if (locked) {
                drawRoundRect(
                    color        = Color(0xFFFF4466).copy(alpha = 0.18f),
                    cornerRadius = CornerRadius(h / 2f),
                    size         = size
                )
                drawRoundRect(
                    color        = Color(0xFFFF4466).copy(alpha = 0.55f),
                    cornerRadius = CornerRadius(h / 2f),
                    size         = size,
                    style        = Stroke(width = 1.5f)
                )
            }
        }
    }
}
