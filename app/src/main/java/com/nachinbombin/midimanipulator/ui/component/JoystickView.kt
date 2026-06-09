package com.nachinbombin.midimanipulator.ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.*

data class JoystickSector(
    val label: String,
    val startDeg: Float,
    val endDeg: Float
)

@Composable
fun JoystickView(
    x: Float,
    y: Float,
    accent: Color,
    accentSoft: Color,
    bg: Color,
    elevated: Color,
    textPrimary: Color,
    textMuted: Color,
    sectors: List<JoystickSector>,
    activeSectorIndex: Int,
    size: Dp = 180.dp,
    onMove: (x: Float, y: Float) -> Unit,
    onRelease: () -> Unit = {}
) {
    val isDragging = remember { mutableStateOf(false) }

    // Glow pulse animation when dragging
    val glowAlpha by animateFloatAsState(
        targetValue = if (isDragging.value) 0.55f else 0.15f,
        animationSpec = tween(300, easing = FastOutSlowInEasing), label = "glow"
    )
    val glowRadius by animateFloatAsState(
        targetValue = if (isDragging.value) 1.0f else 0.6f,
        animationSpec = tween(400, easing = FastOutSlowInEasing), label = "glowR"
    )

    // Thumb scale spring
    val thumbScale by animateFloatAsState(
        targetValue = if (isDragging.value) 1.18f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium), label = "thumb"
    )

    Box(
        modifier = Modifier
            .size(size)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { isDragging.value = true },
                    onDragEnd   = { isDragging.value = false; onRelease() },
                    onDragCancel= { isDragging.value = false; onRelease() },
                    onDrag      = { change, _ ->
                        change.consume()
                        val cx = size.toPx() / 2f
                        val cy = size.toPx() / 2f
                        val px = (change.position.x - cx) / cx
                        val py = (change.position.y - cy) / cy
                        val len = sqrt(px * px + py * py).coerceAtMost(1f)
                        val ang = atan2(py, px)
                        onMove(cos(ang) * len, sin(ang) * len)
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val cx = this.size.width / 2f
            val cy = this.size.height / 2f
            val outerR = this.size.minDimension / 2f
            val rimR   = outerR * 0.92f
            val innerR = outerR * 0.30f
            val thumbR = outerR * 0.21f * thumbScale
            val tx     = cx + x * (outerR - thumbR - 8f)
            val ty     = cy + y * (outerR - thumbR - 8f)

            // ── Outer glow bloom ──────────────────────────────────────────────
            drawCircle(
                brush = Brush.radialGradient(
                    colors  = listOf(accent.copy(alpha = glowAlpha * 0.9f), Color.Transparent),
                    center  = Offset(cx, cy),
                    radius  = outerR * (1.3f + glowRadius * 0.3f)
                ),
                radius = outerR * (1.3f + glowRadius * 0.3f),
                center = Offset(cx, cy)
            )

            // ── Base plate gradient ───────────────────────────────────────────
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(elevated, bg),
                    center = Offset(cx, cy * 0.7f),
                    radius = outerR
                ),
                radius = outerR,
                center = Offset(cx, cy)
            )

            // ── Sector ring ───────────────────────────────────────────────────
            val sectorCount = sectors.size
            sectors.forEachIndexed { i, sector ->
                val isActive = i == activeSectorIndex
                val sweepAngle = 360f / sectorCount
                val startAngle = sector.startDeg - 90f
                val arcColor = if (isActive)
                    Brush.sweepGradient(listOf(accent, accentSoft, accent))
                else
                    Brush.sweepGradient(listOf(
                        textMuted.copy(alpha = 0.25f),
                        textMuted.copy(alpha = 0.12f),
                        textMuted.copy(alpha = 0.25f)
                    ))

                // Sector fill
                drawArc(
                    brush      = arcColor,
                    startAngle = startAngle + 1f,
                    sweepAngle = sweepAngle - 2f,
                    useCenter  = true,
                    topLeft    = Offset(cx - rimR, cy - rimR),
                    size       = Size(rimR * 2, rimR * 2),
                    alpha      = if (isActive) 0.35f else 0.10f
                )

                // Sector border line
                val linePx = startAngle * PI.toFloat() / 180f
                drawLine(
                    color       = if (isActive) accent.copy(0.7f) else textMuted.copy(0.18f),
                    start       = Offset(cx + innerR * cos(linePx), cy + innerR * sin(linePx)),
                    end         = Offset(cx + rimR  * cos(linePx), cy + rimR  * sin(linePx)),
                    strokeWidth = if (isActive) 2.5f else 1f
                )

                // Active sector outer glow arc
                if (isActive) {
                    drawArc(
                        brush      = Brush.sweepGradient(listOf(accent.copy(0.9f), accentSoft.copy(0.6f), accent.copy(0.9f))),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter  = false,
                        topLeft    = Offset(cx - rimR + 2f, cy - rimR + 2f),
                        size       = Size((rimR - 2f) * 2, (rimR - 2f) * 2),
                        style      = Stroke(width = 3.5f, cap = StrokeCap.Round),
                        alpha      = glowAlpha * 2f
                    )
                }
            }

            // ── Rim ring ──────────────────────────────────────────────────────
            drawCircle(
                brush  = Brush.sweepGradient(listOf(accent.copy(0.6f), accentSoft.copy(0.3f), accent.copy(0.6f))),
                radius = rimR,
                center = Offset(cx, cy),
                style  = Stroke(width = 2f)
            )

            // ── Inner dead-zone ring ──────────────────────────────────────────
            drawCircle(
                color  = textMuted.copy(alpha = 0.15f),
                radius = innerR,
                center = Offset(cx, cy),
                style  = Stroke(width = 1.2f)
            )

            // ── Thumb glow ────────────────────────────────────────────────────
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accentSoft.copy(alpha = glowAlpha * 1.2f), Color.Transparent),
                    center = Offset(tx, ty),
                    radius = thumbR * 2.8f
                ),
                radius = thumbR * 2.8f,
                center = Offset(tx, ty)
            )

            // ── Thumb fill (gradient sphere) ──────────────────────────────────
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.3f),
                        accent.copy(alpha = 0.9f),
                        accentSoft.copy(alpha = 0.7f)
                    ),
                    center = Offset(tx - thumbR * 0.25f, ty - thumbR * 0.25f),
                    radius = thumbR
                ),
                radius = thumbR,
                center = Offset(tx, ty)
            )

            // Thumb rim
            drawCircle(
                color  = Color.White.copy(alpha = 0.35f),
                radius = thumbR,
                center = Offset(tx, ty),
                style  = Stroke(width = 1.5f)
            )
        }
    }
}
