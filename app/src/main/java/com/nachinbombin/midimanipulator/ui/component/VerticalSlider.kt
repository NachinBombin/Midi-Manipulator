package com.nachinbombin.midimanipulator.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.nachinbombin.midimanipulator.theme.ThemePreset

@Composable
fun VerticalSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 32.dp,
    height: Dp = 160.dp,
    theme: ThemePreset,
    locked: Boolean = false,
    springBack: Boolean = false,
    onRelease: (() -> Unit)? = null
) {
    Canvas(
        modifier = modifier
            .width(width)
            .height(height)
            .pointerInput(locked) {
                if (!locked) {
                    detectDragGestures(
                        onDragEnd = {
                            if (springBack) onValueChange(0.5f)
                            onRelease?.invoke()
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val newVal = (1f - (change.position.y / this.size.height)).coerceIn(0f, 1f)
                            onValueChange(newVal)
                        }
                    )
                }
            }
    ) {
        val trackW = size.width * 0.35f
        val left   = (size.width - trackW) / 2f
        val radius = 6f

        // Track
        drawRect(color = Color(theme.borderSubtle.value), topLeft = Offset(left, 0f), size = Size(trackW, size.height))

        // Fill
        val fillH = size.height * value
        drawRect(
            color    = Color(theme.accent.value),
            topLeft  = Offset(left, size.height - fillH),
            size     = Size(trackW, fillH)
        )

        // Thumb
        val thumbY = size.height - size.height * value
        drawCircle(
            color  = if (locked) Color(0xFFFF4444) else Color(theme.accent.value),
            radius = size.width * 0.38f,
            center = Offset(size.width / 2f, thumbY)
        )
    }
}
