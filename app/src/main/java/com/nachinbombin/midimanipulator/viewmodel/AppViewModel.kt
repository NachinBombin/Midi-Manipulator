package com.nachinbombin.midimanipulator.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class GamepadMapping(
    val deviceDescriptor: String,
    val mappings: Map<String, String> = emptyMap()
)

class AppViewModel : ViewModel() {

    private val _isHardlocked    = MutableStateFlow(false)
    val isHardlocked: StateFlow<Boolean> = _isHardlocked

    private val _isHoldActive    = MutableStateFlow(false)
    val isHoldActive: StateFlow<Boolean> = _isHoldActive

    private val _hardlockedNote  = MutableStateFlow(-1)
    val hardlockedNote: StateFlow<Int> = _hardlockedNote

    private val _joystick1X = MutableStateFlow(0f)
    val joystick1X: StateFlow<Float> = _joystick1X
    private val _joystick1Y = MutableStateFlow(0f)
    val joystick1Y: StateFlow<Float> = _joystick1Y

    private val _joystick2X = MutableStateFlow(0f)
    val joystick2X: StateFlow<Float> = _joystick2X
    private val _joystick2Y = MutableStateFlow(0f)
    val joystick2Y: StateFlow<Float> = _joystick2Y

    private val _activeChordType = MutableStateFlow("Triad")
    val activeChordType: StateFlow<String> = _activeChordType

    private val _portamentoValue = MutableStateFlow(0f)
    val portamentoValue: StateFlow<Float> = _portamentoValue

    private val _pitchBend = MutableStateFlow(0.5f)
    val pitchBend: StateFlow<Float> = _pitchBend

    private val _modValue = MutableStateFlow(0f)
    val modValue: StateFlow<Float> = _modValue

    private val _pitchLocked = MutableStateFlow(false)
    val pitchLocked: StateFlow<Boolean> = _pitchLocked

    private val _modLocked = MutableStateFlow(false)
    val modLocked: StateFlow<Boolean> = _modLocked

    private val _gateLength = MutableStateFlow(0.5f)
    val gateLength: StateFlow<Float> = _gateLength

    private val _rhythmPattern = MutableStateFlow("Quarter")
    val rhythmPattern: StateFlow<String> = _rhythmPattern

    private val _strumLocks = MutableStateFlow(Array(6) { false })
    val strumLocks: StateFlow<Array<Boolean>> = _strumLocks

    private val _gamepadMappings = MutableStateFlow<Map<String, GamepadMapping>>(emptyMap())
    val gamepadMappings: StateFlow<Map<String, GamepadMapping>> = _gamepadMappings

    private val _testModeActive = MutableStateFlow(false)
    val testModeActive: StateFlow<Boolean> = _testModeActive

    private val _lastTriggeredControl = MutableStateFlow("")
    val lastTriggeredControl: StateFlow<String> = _lastTriggeredControl

    fun setHardlock(note: Int, hold: Boolean = false) {
        _hardlockedNote.value = note
        _isHardlocked.value = true
        _isHoldActive.value = hold
    }

    fun clearHardlock() {
        _isHardlocked.value = false
        _isHoldActive.value = false
        _hardlockedNote.value = -1
    }

    fun updateJoystick1(x: Float, y: Float) { _joystick1X.value = x; _joystick1Y.value = y }
    fun updateJoystick2(x: Float, y: Float) { _joystick2X.value = x; _joystick2Y.value = y }
    fun setActiveChord(type: String) { _activeChordType.value = type }
    fun setPortamento(v: Float)  { _portamentoValue.value = v }
    fun setPitchBend(v: Float)   { _pitchBend.value = v }
    fun setMod(v: Float)         { _modValue.value = v }
    fun togglePitchLock()        { _pitchLocked.value = !_pitchLocked.value }
    fun toggleModLock()          { _modLocked.value = !_modLocked.value }
    fun setGate(v: Float)        { _gateLength.value = v }
    fun setRhythm(r: String)     { _rhythmPattern.value = r }

    fun toggleStrumLock(index: Int) {
        val arr = _strumLocks.value.copyOf()
        arr[index] = !arr[index]
        _strumLocks.value = arr
    }

    fun setGamepadMapping(descriptor: String, mapping: GamepadMapping) {
        val map = _gamepadMappings.value.toMutableMap()
        map[descriptor] = mapping
        _gamepadMappings.value = map
    }

    fun setTestMode(active: Boolean)       { _testModeActive.value = active }
    fun setLastTriggered(control: String)  { _lastTriggeredControl.value = control }
}
