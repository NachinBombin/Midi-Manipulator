package com.nachinbombin.midimanipulator.viewmodel

import androidx.lifecycle.ViewModel
import com.nachinbombin.midimanipulator.midi.MidiAnalysis
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class GamepadMapping(
    val deviceDescriptor: String,
    val mappings: Map<String, String> = emptyMap()
)

class AppViewModel : ViewModel() {

    // MIDI analysis (synced from MidiEngine via LaunchedEffect)
    private val _analysis = MutableStateFlow(MidiAnalysis())
    val analysis: StateFlow<MidiAnalysis> = _analysis
    fun updateAnalysis(a: MidiAnalysis) { _analysis.value = a }

    // Hardlock / Hold
    private val _isHardlocked   = MutableStateFlow(false)
    val isHardlocked: StateFlow<Boolean> = _isHardlocked

    private val _isHoldActive   = MutableStateFlow(false)
    val isHoldActive: StateFlow<Boolean> = _isHoldActive

    private val _hardlockedNote = MutableStateFlow(-1)
    val hardlockedNote: StateFlow<Int> = _hardlockedNote

    fun setHardlock(note: Int, hold: Boolean = false) {
        _hardlockedNote.value = note
        _isHardlocked.value   = true
        _isHoldActive.value   = hold
    }

    fun clearHardlock() {
        _isHardlocked.value   = false
        _isHoldActive.value   = false
        _hardlockedNote.value = -1
    }

    // Joysticks
    private val _joystick1X = MutableStateFlow(0f)
    val joystick1X: StateFlow<Float> = _joystick1X
    private val _joystick1Y = MutableStateFlow(0f)
    val joystick1Y: StateFlow<Float> = _joystick1Y

    private val _joystick2X = MutableStateFlow(0f)
    val joystick2X: StateFlow<Float> = _joystick2X
    private val _joystick2Y = MutableStateFlow(0f)
    val joystick2Y: StateFlow<Float> = _joystick2Y

    fun updateJoystick1(x: Float, y: Float) { _joystick1X.value = x; _joystick1Y.value = y }
    fun updateJoystick2(x: Float, y: Float) { _joystick2X.value = x; _joystick2Y.value = y }

    // Chord / Portamento
    private val _activeChordType = MutableStateFlow("Triad")
    val activeChordType: StateFlow<String> = _activeChordType

    private val _portamentoValue = MutableStateFlow(0f)
    val portamentoValue: StateFlow<Float> = _portamentoValue

    fun setActiveChord(type: String) { _activeChordType.value = type }
    fun setPortamento(v: Float)      { _portamentoValue.value = v }

    // Pitch Bend
    private val _pitchBend       = MutableStateFlow(0.5f)
    val pitchBend: StateFlow<Float> = _pitchBend

    private val _pitchBendLocked = MutableStateFlow(false)
    val pitchBendLocked: StateFlow<Boolean> = _pitchBendLocked

    fun setPitchBend(v: Float)  { _pitchBend.value = v }
    fun togglePitchLock()       { _pitchBendLocked.value = !_pitchBendLocked.value }

    // Mod Wheel
    private val _modValue  = MutableStateFlow(0f)
    val modValue: StateFlow<Float> = _modValue

    private val _modLocked = MutableStateFlow(false)
    val modLocked: StateFlow<Boolean> = _modLocked

    fun setMod(v: Float)        { _modValue.value = v }
    fun toggleModLock()         { _modLocked.value = !_modLocked.value }

    // Gate / Rhythm
    private val _gateLength    = MutableStateFlow(0.5f)
    val gateLength: StateFlow<Float> = _gateLength

    private val _rhythmPattern = MutableStateFlow("Quarter")
    val rhythmPattern: StateFlow<String> = _rhythmPattern

    fun setGate(v: Float)       { _gateLength.value = v }
    fun setRhythm(r: String)    { _rhythmPattern.value = r }

    // Strum locks — List<Boolean> for safe Compose recomposition
    private val _strumLocks = MutableStateFlow(List(6) { false })
    val strumLocks: StateFlow<List<Boolean>> = _strumLocks

    fun toggleStrumLock(index: Int) {
        _strumLocks.value = _strumLocks.value.toMutableList().also { it[index] = !it[index] }
    }

    // Gamepad
    private val _gamepadMappings      = MutableStateFlow<Map<String, GamepadMapping>>(emptyMap())
    val gamepadMappings: StateFlow<Map<String, GamepadMapping>> = _gamepadMappings

    private val _testModeActive       = MutableStateFlow(false)
    val testModeActive: StateFlow<Boolean> = _testModeActive

    private val _lastTriggeredControl = MutableStateFlow("")
    val lastTriggeredControl: StateFlow<String> = _lastTriggeredControl

    fun setGamepadMapping(descriptor: String, mapping: GamepadMapping) {
        _gamepadMappings.value = _gamepadMappings.value.toMutableMap().also { it[descriptor] = mapping }
    }

    fun setTestMode(active: Boolean)      { _testModeActive.value = active }
    fun setLastTriggered(control: String) { _lastTriggeredControl.value = control }
}
