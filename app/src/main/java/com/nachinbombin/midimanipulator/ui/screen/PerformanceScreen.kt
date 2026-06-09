package com.nachinbombin.midimanipulator.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nachinbombin.midimanipulator.midi.MidiEngine
import com.nachinbombin.midimanipulator.theme.ThemeManager
import com.nachinbombin.midimanipulator.theme.ThemePreset
import com.nachinbombin.midimanipulator.ui.component.*
import com.nachinbombin.midimanipulator.viewmodel.AppViewModel

@Composable
fun PerformanceScreen(
    viewModel: AppViewModel,
    midiEngine: MidiEngine,
    themeManager: ThemeManager
) {
    val theme       = themeManager.currentTheme.collectAsState().value
    val analysis    by midiEngine.analysis.collectAsState()
    val hardlocked  by viewModel.isHardlocked.collectAsState()
    val holdActive  by viewModel.isHoldActive.collectAsState()
    val portVal     by viewModel.portamentoValue.collectAsState()
    val pitchVal    by viewModel.pitchBend.collectAsState()
    val modVal      by viewModel.modValue.collectAsState()
    val pitchLock   by viewModel.pitchLocked.collectAsState()
    val modLock     by viewModel.modLocked.collectAsState()
    val gateLen     by viewModel.gateLength.collectAsState()
    val rhythm      by viewModel.rhythmPattern.collectAsState()
    val strumLocks  by viewModel.strumLocks.collectAsState()

    val noteNames   = listOf("C","C#","D","D#","E","F","F#","G","G#","A","A#","B")
    fun midiToName(n: Int) = if (n < 0) "—" else "${noteNames[n % 12]}${n / 12 - 1}"

    val melodicSectors  = listOf("R","2","3","4","5","6","7","Oct")
    val chordTypes      = listOf("Triad","7th","9th","11th","13th","sus2","sus4","Power","add9","maj7","min7","dim7","aug","half-dim")
    val strumLabels     = listOf("Major","Triad","7th","9th","sus2","sus4")
    val rhythmOptions   = listOf("Whole","Half","Quarter","Eighth","Triplet","Dotted","Synco")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(theme.bgVoices.value))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(theme.bgElevated.value), RoundedCornerShape(10.dp))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            HeaderChip("Note",  midiToName(analysis.lastNote),  theme)
            HeaderChip("Root",  midiToName(analysis.rootNote),  theme)
            HeaderChip("Scale", analysis.scaleName,             theme)
            HeaderChip("Chord", analysis.chordContext,          theme)
        }

        // Reference Note Controls
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GlowButton("SELECT NOTE", hardlocked && !holdActive, theme, Modifier.weight(1f)) {
                if (hardlocked && !holdActive) viewModel.clearHardlock()
                else viewModel.setHardlock(analysis.lastNote, hold = false)
            }
            GlowButton("HOLD NOTE", holdActive, theme, Modifier.weight(1f)) {
                if (holdActive) viewModel.clearHardlock()
                else viewModel.setHardlock(analysis.lastNote, hold = true)
            }
        }

        if (holdActive) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Gate", color = Color(theme.textMuted.value), fontSize = 11.sp)
                VerticalSlider(value = gateLen, onValueChange = { viewModel.setGate(it) },
                    width = 28.dp, height = 52.dp, theme = theme)
                Spacer(Modifier.width(8.dp))
                Text("Rhythm", color = Color(theme.textMuted.value), fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    rhythmOptions.forEach { r ->
                        Box(
                            modifier = Modifier
                                .background(
                                    if (rhythm == r) Color(theme.accent.value) else Color(theme.bgElevated.value),
                                    RoundedCornerShape(4.dp)
                                )
                                .clickable { viewModel.setRhythm(r) }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) { Text(r, color = Color(if (rhythm == r) theme.bg.value else theme.textMuted.value), fontSize = 9.sp) }
                    }
                }
            }
        }

        // Joystick Row
        Row(
            Modifier.fillMaxWidth().height(220.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("MELODIC", color = Color(theme.textMuted.value), fontSize = 9.sp)
                JoystickView(
                    size    = 180.dp, theme = theme, sectors = melodicSectors,
                    onPositionChanged = { _, _, sector, vel ->
                        if (!hardlocked) viewModel.setHardlock(analysis.lastNote, hold = false)
                        val refNote = viewModel.hardlockedNote.value.takeIf { it >= 0 } ?: analysis.lastNote
                        if (refNote >= 0) {
                            val scaleDeg = midiEngine.scaleIntervals(analysis.scaleName)
                            val pc   = if (sector < scaleDeg.size) scaleDeg[sector] else sector
                            val note = ((refNote / 12) * 12 + (analysis.rootNote + pc) % 12).coerceIn(0, 127)
                            midiEngine.sendNoteOn(note, vel, 0)
                        }
                    },
                    onRelease = { midiEngine.sendNoteOff(60, 0) }
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text("PORT", color = Color(theme.textMuted.value), fontSize = 9.sp)
                VerticalSlider(
                    value = portVal,
                    onValueChange = { v ->
                        viewModel.setPortamento(v)
                        val cc5 = (v * 127).toInt()
                        midiEngine.portamentoTime = cc5
                        midiEngine.sendCC(5, cc5, 0)
                        midiEngine.sendCC(65, if (cc5 > 0) 64 else 0, 0)
                    },
                    width = 30.dp, height = 160.dp, theme = theme
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("HARMONIC", color = Color(theme.textMuted.value), fontSize = 9.sp)
                JoystickView(
                    size    = 180.dp, theme = theme, sectors = chordTypes.take(14),
                    onPositionChanged = { _, _, sector, vel ->
                        val chordType = chordTypes.getOrElse(sector) { "Triad" }
                        viewModel.setActiveChord(chordType)
                        val refNote = viewModel.hardlockedNote.value.takeIf { it >= 0 } ?: analysis.lastNote
                        if (refNote >= 0) {
                            midiEngine.buildChordVoicing(refNote, chordType)
                                .forEach { n -> midiEngine.sendNoteOn(n, vel, midiEngine.harmonyChannel) }
                        }
                    },
                    onRelease = {}
                )
            }
        }

        // Strum Strips
        Text("STRUM", color = Color(theme.textMuted.value), fontSize = 10.sp)
        strumLabels.forEachIndexed { i, label ->
            val refNote = viewModel.hardlockedNote.value.takeIf { it >= 0 } ?: analysis.lastNote
            val notes = if (refNote >= 0) {
                val base = midiEngine.buildChordVoicing(refNote, label)
                (base + base.map { it + 12 } + base.map { it + 24 }).map { it.coerceIn(0, 127) }
            } else emptyList()
            StrumStrip(
                label = label, chordNotes = notes, theme = theme,
                locked = strumLocks[i], onLockToggle = { viewModel.toggleStrumLock(i) },
                midiEngine = midiEngine
            )
        }

        // Performance Wheels
        Row(
            Modifier.fillMaxWidth().padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("PITCH", color = Color(theme.textMuted.value), fontSize = 9.sp)
                VerticalSlider(
                    value = pitchVal,
                    onValueChange = { v ->
                        if (!pitchLock) {
                            viewModel.setPitchBend(v)
                            val bend = ((v - 0.5f) * 2f * 8192).toInt() + 8192
                            midiEngine.pitchBend = bend.coerceIn(0, 16383)
                            midiEngine.sendPitchBend(midiEngine.pitchBend, 0)
                        }
                    },
                    width = 36.dp, height = 100.dp, theme = theme,
                    locked = pitchLock, springBack = !pitchLock,
                    onRelease = { if (!pitchLock) { viewModel.setPitchBend(0.5f); midiEngine.sendPitchBend(8192, 0) } }
                )
                LockIcon(pitchLock) { viewModel.togglePitchLock() }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("MOD", color = Color(theme.textMuted.value), fontSize = 9.sp)
                VerticalSlider(
                    value = modVal,
                    onValueChange = { v ->
                        if (!modLock) {
                            viewModel.setMod(v)
                            midiEngine.modValue = (v * 127).toInt()
                            midiEngine.sendCC(1, midiEngine.modValue, 0)
                        }
                    },
                    width = 36.dp, height = 100.dp, theme = theme,
                    locked = modLock, springBack = !modLock,
                    onRelease = { if (!modLock) { viewModel.setMod(0f); midiEngine.sendCC(1, 0, 0) } }
                )
                LockIcon(modLock) { viewModel.toggleModLock() }
            }
        }
    }
}

@Composable
private fun HeaderChip(label: String, value: String, theme: ThemePreset) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = Color(theme.textMuted.value), fontSize = 9.sp)
        Text(value, color = Color(theme.textPrimary.value), fontSize = 13.sp)
    }
}

@Composable
private fun GlowButton(
    label: String, active: Boolean, theme: ThemePreset,
    modifier: Modifier = Modifier, onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .background(
                if (active) Color(theme.accent.value) else Color(theme.bgElevated.value),
                RoundedCornerShape(8.dp)
            )
            .border(
                width = if (active) 2.dp else 1.dp,
                color = if (active) Color(theme.accentAlt.value) else Color(theme.borderSubtle.value),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (active) Color(theme.bg.value) else Color(theme.textMuted.value), fontSize = 11.sp)
    }
}

@Composable
private fun LockIcon(locked: Boolean, onClick: () -> Unit) {
    Text(
        if (locked) "🔴" else "⚪",
        fontSize = 14.sp,
        modifier = Modifier.padding(top = 4.dp).clickable { onClick() }
    )
}
