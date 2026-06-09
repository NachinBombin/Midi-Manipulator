package com.nachinbombin.midimanipulator.ui.component

import android.os.Handler
import android.os.Looper
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nachinbombin.midimanipulator.midi.MidiEngine
import com.nachinbombin.midimanipulator.theme.ThemePreset
import kotlin.math.abs

@Composable
fun StrumStrip(
    label: String,
    chordNotes: List<Int>,
    theme: ThemePreset,
    locked: Boolean,
    onLockToggle: () -> Unit,
    midiEngine: MidiEngine,
    modifier: Modifier = Modifier
) {
    val activeNotes = remember { mutableSetOf<Int>() }
    val lockColor by animateColorAsState(
        targetValue = if (locked) Color(0xFFFF4444) else Color(theme.textMuted.value)
    )
    val handler = remember { Handler(Looper.getMainLooper()) }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(Color(theme.bgElevated.value), RoundedCornerShape(8.dp))
            .border(1.dp, Color(theme.borderSubtle.value), RoundedCornerShape(8.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Lock toggle
        Box(
            modifier = Modifier
                .size(36.dp)
                .pointerInput(Unit) { detectTapGestures { onLockToggle() } },
            contentAlignment = Alignment.Center
        ) {
            Text("🔒", fontSize = 14.sp, color = lockColor)
        }

        Text(
            label,
            color    = Color(theme.textMuted.value),
            fontSize = 10.sp,
            modifier = Modifier.width(44.dp)
        )

        // Strum canvas
        Canvas(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .pointerInput(locked) {
                    if (!locked) {
                        detectDragGestures(
                            onDragEnd = {
                                activeNotes.forEach { note ->
                                    midiEngine.sendNoteOff(note, midiEngine.harmonyChannel)
                                }
                                activeNotes.clear()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                if (chordNotes.isEmpty()) return@detectDragGestures
                                val x         = change.position.x
                                val totalW    = size.width.toFloat()
                                val spacing   = totalW / chordNotes.size
                                val noteIndex = (x / spacing).toInt().coerceIn(0, chordNotes.size - 1)
                                val note      = chordNotes[noteIndex]
                                if (!activeNotes.contains(note)) {
                                    val swipeSpeed = abs(dragAmount.x)
                                    val velocity   = (64 + (swipeSpeed * 1.5f).toInt()).coerceIn(40, 127)
                                    // Overlap: Note On before previous Note Off
                                    midiEngine.sendNoteOn(note, velocity, midiEngine.harmonyChannel)
                                    activeNotes.add(note)
                                    // Release previous note after 12ms
                                    if (noteIndex > 0) {
                                        val prev = chordNotes[noteIndex - 1]
                                        if (activeNotes.contains(prev)) {
                                            handler.postDelayed({
                                                midiEngine.sendNoteOff(prev, midiEngine.harmonyChannel)
                                                activeNotes.remove(prev)
                                            }, 12L)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
        ) {
            if (chordNotes.isEmpty()) return@Canvas
            val spacing = size.width / chordNotes.size
            chordNotes.forEachIndexed { i, note ->
                val x      = i * spacing + spacing / 2f
                val active = activeNotes.contains(note)
                drawLine(
                    color       = if (active) Color(theme.accent.value) else Color(theme.borderSubtle.value),
                    start       = Offset(x, size.height * 0.2f),
                    end         = Offset(x, size.height * 0.8f),
                    strokeWidth = if (active) 3f else 1.5f
                )
                drawCircle(
                    color  = if (active) Color(theme.accent.value) else Color(theme.borderSubtle.value),
                    radius = if (active) 5f else 3.5f,
                    center = Offset(x, size.height / 2f)
                )
            }
        }
    }
}
