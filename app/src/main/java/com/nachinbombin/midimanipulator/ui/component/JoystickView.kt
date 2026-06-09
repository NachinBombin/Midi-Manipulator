package com.nachinbombin.midimanipulator.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nachinbombin.midimanipulator.theme.ThemePreset
import kotlin.math.*

@Composable
fun JoystickView(
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    theme: ThemePreset,
    sectors: List<String>,
    onPositionChanged: (x: Float, y: Float, sector: Int, velocity: Int) -> Unit,
    onRelease: () -> Unit
) {
    var thumbPos by remember { mutableStateOf(Offset.Zero) }
    var activeSector by remember { mutableStateOf(-1) }
    var lastAngleDeg by remember { mutableStateOf(0f) }
    val HYSTERESIS_DEG = 5f

    Canvas(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        thumbPos = Offset.Zero
                        activeSector = -1
                        onRelease()
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val center = Offset(this.size.width / 2f, this.size.height / 2f)
                        val raw    = change.position - center
                        val maxRadius = this.size.width / 2f * 0.8f
                        val dist   = raw.getDistance().coerceAtMost(maxRadius)
                        val angle  = atan2(raw.y, raw.x)
                        thumbPos   = Offset(cos(angle) * dist, sin(angle) * dist)

                        val velocity   = ((dist / maxRadius) * 127).toInt().coerceIn(0, 127)
                        val angleDeg   = ((Math.toDegrees(angle.toDouble()) + 360.0) % 360.0).toFloat()
                        val sectorSpan = 360f / sectors.size

                        val rawSector = ((angleDeg / sectorSpan).toInt()) % sectors.size
                        if (activeSector == -1 || abs(angleDeg - lastAngleDeg) > HYSTERESIS_DEG) {
                            activeSector  = rawSector
                            lastAngleDeg  = angleDeg
                        }
                        onPositionChanged(thumbPos.x / maxRadius, thumbPos.y / maxRadius, activeSector, velocity)
                    }
                )
            }
    ) {
        val center      = Offset(this.size.width / 2f, this.size.height / 2f)
        val outerRadius = this.size.width / 2f * 0.95f
        val ringOuter   = outerRadius * 0.98f
        val ringInner   = outerRadius * 0.72f
        val ringMid     = (ringOuter + ringInner) / 2f
        val ringStroke  = ringOuter - ringInner
        val thumbRadius = outerRadius * 0.18f

        // Background
        drawCircle(color = Color(theme.bgElevated.value), radius = outerRadius, center = center)
        drawCircle(color = Color(theme.borderSubtle.value), radius = outerRadius, center = center, style = Stroke(1.5f))

        // Sector arcs
        val sectorCount = sectors.size
        val sweepAngle  = 360f / sectorCount
        sectors.forEachIndexed { i, _ ->
            val startAngle = i * sweepAngle - 90f + 1f
            val isActive   = i == activeSector
            val arcLeft    = center.x - ringMid
            val arcTop     = center.y - ringMid
            drawArc(
                color      = if (isActive) Color(theme.accent.value) else Color(theme.borderSubtle.value),
                startAngle = startAngle,
                sweepAngle = sweepAngle - 2f,
                useCenter  = false,
                style      = Stroke(width = ringStroke * 0.7f),
                topLeft    = Offset(arcLeft, arcTop),
                size       = Size(ringMid * 2f, ringMid * 2f)
            )
        }

        // Inner circle
        drawCircle(color = Color(theme.bg.value), radius = ringInner * 0.94f, center = center)

        // Thumb knob
        val thumbCenter = center + thumbPos
        drawCircle(color = Color(theme.accent.value).copy(alpha = 0.85f), radius = thumbRadius, center = thumbCenter)
        drawCircle(color = Color(theme.accentSoft.value), radius = thumbRadius * 0.45f, center = thumbCenter)
    }
}
