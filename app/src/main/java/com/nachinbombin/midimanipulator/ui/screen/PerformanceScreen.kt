package com.nachinbombin.midimanipulator.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.nachinbombin.midimanipulator.midi.MidiManager
import com.nachinbombin.midimanipulator.theme.ThemePreset
import com.nachinbombin.midimanipulator.ui.component.*
import com.nachinbombin.midimanipulator.viewmodel.AppViewModel

private val DIATONIC_SECTORS = listOf(
    JoystickSector("Root",  0f,   51.4f),
    JoystickSector("2nd",   51.4f, 102.9f),
    JoystickSector("3rd",  102.9f, 154.3f),
    JoystickSector("4th",  154.3f, 205.7f),
    JoystickSector("5th",  205.7f, 257.1f),
    JoystickSector("6th",  257.1f, 308.6f),
    JoystickSector("7th",  308.6f, 360f)
)

private val CHORD_SECTORS = listOf(
    JoystickSector("Triad",    0f,    25.7f),
    JoystickSector("7th",     25.7f,  51.4f),
    JoystickSector("9th",     51.4f,  77.1f),
    JoystickSector("11th",    77.1f, 102.9f),
    JoystickSector("13th",   102.9f, 128.6f),
    JoystickSector("sus2",   128.6f, 154.3f),
    JoystickSector("sus4",   154.3f, 180f),
    JoystickSector("Power",  180f,   205.7f),
    JoystickSector("add9",   205.7f, 231.4f),
    JoystickSector("maj7",   231.4f, 257.1f),
    JoystickSector("min7",   257.1f, 282.9f),
    JoystickSector("dim7",   282.9f, 308.6f),
    JoystickSector("aug",    308.6f, 334.3f),
    JoystickSector("hdim",   334.3f, 360f)
)

private val STRUM_LABELS = listOf("Major", "Triad", "7th", "9th", "sus2", "sus4")

@Composable
fun PerformanceScreen(
    viewModel: AppViewModel,
    midiManager: MidiManager,
    theme: ThemePreset
) {
    val analysis    by viewModel.analysis.collectAsState()
    val isLocked    by viewModel.isHardlocked.collectAsState()
    val isHold      by viewModel.isHoldActive.collectAsState()
    val j1x         by viewModel.joystick1X.collectAsState()
    val j1y         by viewModel.joystick1Y.collectAsState()
    val j2x         by viewModel.joystick2X.collectAsState()
    val j2y         by viewModel.joystick2Y.collectAsState()
    val portamento  by viewModel.portamentoValue.collectAsState()
    val pitchBend   by viewModel.pitchBend.collectAsState()
    val pitchLocked by viewModel.pitchBendLocked.collectAsState()
    val modValue    by viewModel.modValue.collectAsState()
    val modLocked   by viewModel.modLocked.collectAsState()
    val strumLocks  by viewModel.strumLocks.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(theme.bgVoices, theme.bg, theme.bg)
                )
            )
            .verticalScroll(rememberScrollState())
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── Header Bar ────────────────────────────────────────────────────────
        HeaderBar(analysis = analysis, theme = theme)

        // ── Reference Note Controls ──────────────────────────────────────────
        ReferenceControls(
            isLocked  = isLocked,
            isHold    = isHold,
            theme     = theme,
            onSelect  = {
                if (isLocked && !isHold) viewModel.clearHardlock()
                else viewModel.setHardlock(analysis.lastNote, hold = false)
            },
            onHold    = {
                if (isHold) viewModel.clearHardlock()
                else viewModel.setHardlock(analysis.lastNote, hold = true)
            }
        )

        // ── Joysticks + Portamento ───────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            JoystickView(
                x = j1x, y = j1y,
                accent = theme.accent, accentSoft = theme.accentSoft,
                bg = theme.bg, elevated = theme.bgElevated,
                textPrimary = theme.textPrimary, textMuted = theme.textMuted,
                sectors = DIATONIC_SECTORS,
                activeSectorIndex = sectorIndex(j1x, j1y, DIATONIC_SECTORS.size),
                onMove    = { x, y -> viewModel.updateJoystick1(x, y) },
                onRelease = { viewModel.updateJoystick1(0f, 0f) },
                modifier = Modifier.weight(1f)
            )

            // Center portamento slider
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.width(44.dp)
            ) {
                Text(
                    text     = "PORTA",
                    color    = theme.textMuted,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                VerticalSlider(
                    value    = portamento,
                    accent   = theme.accent, accentSoft = theme.accentSoft,
                    bg       = theme.bg, elevated = theme.bgElevated,
                    textMuted= theme.textMuted,
                    height   = 160.dp,
                    onValueChange = { viewModel.setPortamento(it) }
                )
            }

            JoystickView(
                x = j2x, y = j2y,
                accent = theme.accent, accentSoft = theme.accentSoft,
                bg = theme.bg, elevated = theme.bgElevated,
                textPrimary = theme.textPrimary, textMuted = theme.textMuted,
                sectors = CHORD_SECTORS,
                activeSectorIndex = sectorIndex(j2x, j2y, CHORD_SECTORS.size),
                onMove    = { x, y -> viewModel.updateJoystick2(x, y) },
                onRelease = { viewModel.updateJoystick2(0f, 0f) },
                modifier = Modifier.weight(1f)
            )
        }

        // ── Strum Strips ─────────────────────────────────────────────────────
        GlowCard(theme = theme) {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.padding(10.dp)
            ) {
                Text(
                    text     = "STRUM",
                    color    = theme.textMuted,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
                STRUM_LABELS.forEachIndexed { idx, label ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text      = label,
                            color     = theme.textMuted,
                            fontSize  = 10.sp,
                            modifier  = Modifier.width(42.dp)
                        )
                        StrumStrip(
                            label     = label,
                            noteCount = 12,
                            accent    = theme.accent, accentSoft = theme.accentSoft,
                            bg        = theme.bg, elevated = theme.bgElevated,
                            textPrimary = theme.textPrimary, textMuted = theme.textMuted,
                            locked    = strumLocks.getOrElse(idx) { false },
                            onLockToggle = { viewModel.toggleStrumLock(idx) },
                            onNoteOn  = { ni, v -> midiManager.engine.strumNoteOn(idx, ni, v) },
                            onNoteOff = { ni    -> midiManager.engine.strumNoteOff(idx, ni) },
                            modifier  = Modifier.weight(1f)
                        )
                        // Lock icon
                        val lockColor by animateColorAsState(
                            targetValue = if (strumLocks.getOrElse(idx) { false })
                                Color(0xFFFF4466) else theme.textMuted.copy(0.4f),
                            animationSpec = tween(200), label = "lc"
                        )
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(lockColor.copy(0.12f))
                                .clickable { viewModel.toggleStrumLock(idx) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("\uD83D\uDD12", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // ── Pitch / Mod Wheels ───────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WheelControl(
                label    = "PITCH",
                value    = pitchBend,
                locked   = pitchLocked,
                theme    = theme,
                onValueChange = { viewModel.setPitchBend(it) },
                onLockToggle  = { viewModel.togglePitchLock() },
                modifier = Modifier.weight(1f)
            )
            WheelControl(
                label    = "MOD",
                value    = modValue,
                locked   = modLocked,
                theme    = theme,
                onValueChange = { viewModel.setMod(it) },
                onLockToggle  = { viewModel.toggleModLock() },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ── Header Bar ───────────────────────────────────────────────────────────────
@Composable
private fun HeaderBar(analysis: com.nachinbombin.midimanipulator.midi.MidiAnalysis, theme: ThemePreset) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(theme.bgElevated, theme.bgVoices, theme.bgElevated)
                )
            )
            .border(1.dp, Brush.horizontalGradient(listOf(
                theme.accent.copy(0.3f), theme.accentSoft.copy(0.15f), theme.accent.copy(0.3f)
            )), RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        HeaderItem("NOTE",  analysis.lastNoteName, theme)
        HeaderDivider(theme)
        HeaderItem("ROOT",  analysis.rootNoteName,  theme)
        HeaderDivider(theme)
        HeaderItem("SCALE", analysis.scaleName,     theme)
        HeaderDivider(theme)
        HeaderItem("CHORD", analysis.chordContext,  theme)
    }
}

@Composable
private fun HeaderItem(label: String, value: String, theme: ThemePreset) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = theme.textMuted, fontSize = 8.sp,
            fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Text(value, color = theme.accent, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HeaderDivider(theme: ThemePreset) {
    Box(
        modifier = Modifier
            .width(1.dp)
            .height(32.dp)
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, theme.borderSubtle, Color.Transparent)
                )
            )
    )
}

// ── Reference Controls ────────────────────────────────────────────────────────
@Composable
private fun ReferenceControls(
    isLocked: Boolean, isHold: Boolean, theme: ThemePreset,
    onSelect: () -> Unit, onHold: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GlowButton(
            label = "SELECT NOTE", active = isLocked && !isHold,
            accent = theme.accent, accentSoft = theme.accentSoft,
            bg = theme.bg, elevated = theme.bgElevated, textPrimary = theme.textPrimary,
            onClick = onSelect, modifier = Modifier.weight(1f)
        )
        GlowButton(
            label = "HOLD NOTE", active = isHold,
            accent = theme.accentAlt, accentSoft = theme.accent,
            bg = theme.bg, elevated = theme.bgElevated, textPrimary = theme.textPrimary,
            onClick = onHold, modifier = Modifier.weight(1f)
        )
    }
}

// ── Wheel Control (Pitch / Mod) ────────────────────────────────────────────────
@Composable
private fun WheelControl(
    label: String, value: Float, locked: Boolean, theme: ThemePreset,
    onValueChange: (Float) -> Unit, onLockToggle: () -> Unit, modifier: Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(label, color = theme.textMuted, fontSize = 9.sp,
                fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            val lockColor by animateColorAsState(
                targetValue = if (locked) Color(0xFFFF4466) else theme.textMuted.copy(0.4f),
                animationSpec = tween(200), label = "wlc"
            )
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(lockColor.copy(0.15f))
                    .clickable { onLockToggle() },
                contentAlignment = Alignment.Center
            ) {
                Text("\uD83D\uDD12", fontSize = 8.sp)
            }
        }
        VerticalSlider(
            value    = value, locked = locked,
            accent   = theme.accent, accentSoft = theme.accentSoft,
            bg       = theme.bg, elevated = theme.bgElevated, textMuted = theme.textMuted,
            width    = 44.dp, height = 88.dp,
            onValueChange  = onValueChange
        )
    }
}

// ── Glow Card ────────────────────────────────────────────────────────────────
@Composable
fun GlowCard(theme: ThemePreset, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .drawBehind {
                // Subtle outer glow
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            theme.accent.copy(0.06f),
                            theme.accentSoft.copy(0.03f),
                            Color.Transparent
                        )
                    ),
                    cornerRadius = CornerRadius(42f)
                )
            }
            .background(
                Brush.verticalGradient(
                    listOf(theme.bgElevated.copy(0.97f), theme.bg.copy(0.95f))
                )
            )
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(
                        theme.accent.copy(0.25f),
                        theme.borderSubtle.copy(0.5f),
                        theme.accentSoft.copy(0.15f)
                    )
                ),
                RoundedCornerShape(14.dp)
            )
    ) {
        content()
    }
}

private fun sectorIndex(x: Float, y: Float, count: Int): Int {
    if (x == 0f && y == 0f) return -1
    val angleDeg = (Math.toDegrees(Math.atan2(y.toDouble(), x.toDouble())).toFloat() + 360f) % 360f
    return ((angleDeg / 360f) * count).toInt().coerceIn(0, count - 1)
}
