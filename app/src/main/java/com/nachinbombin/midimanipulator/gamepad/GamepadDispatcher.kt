package com.nachinbombin.midimanipulator.gamepad

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.nachinbombin.midimanipulator.midi.MidiEngine
import com.nachinbombin.midimanipulator.viewmodel.AppViewModel
import kotlin.math.abs

class GamepadDispatcher(
    private val viewModel: AppViewModel,
    private val midiEngine: MidiEngine
) {

    fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.source and InputDevice.SOURCE_GAMEPAD != InputDevice.SOURCE_GAMEPAD) return false
        if (event.action != KeyEvent.ACTION_DOWN && event.action != KeyEvent.ACTION_UP) return false

        val descriptor  = event.device?.descriptor ?: return false
        val mappings    = viewModel.gamepadMappings.value[descriptor]?.mappings ?: return false
        val buttonLabel = keyCodeToLabel(event.keyCode) ?: return false
        val target      = mappings[buttonLabel] ?: return false

        if (viewModel.testModeActive.value) {
            viewModel.setLastTriggered(buttonLabel)
            return true
        }

        handleTarget(target, event.action == KeyEvent.ACTION_DOWN, 1f)
        return true
    }

    fun onMotionEvent(event: MotionEvent): Boolean {
        if (event.source and InputDevice.SOURCE_GAMEPAD  != InputDevice.SOURCE_GAMEPAD &&
            event.source and InputDevice.SOURCE_JOYSTICK != InputDevice.SOURCE_JOYSTICK) return false

        val descriptor = event.device?.descriptor ?: return false
        val mappings   = viewModel.gamepadMappings.value[descriptor]?.mappings ?: return false

        fun axis(id: Int) = event.getAxisValue(id)

        val axisMap = mapOf(
            "LStick X" to axis(MotionEvent.AXIS_X),
            "LStick Y" to axis(MotionEvent.AXIS_Y),
            "RStick X" to axis(MotionEvent.AXIS_Z),
            "RStick Y" to axis(MotionEvent.AXIS_RZ),
            "L2"       to axis(MotionEvent.AXIS_LTRIGGER),
            "R2"       to axis(MotionEvent.AXIS_RTRIGGER),
            "D-Left"   to if (axis(MotionEvent.AXIS_HAT_X) < -0.5f) 1f else 0f,
            "D-Right"  to if (axis(MotionEvent.AXIS_HAT_X) >  0.5f) 1f else 0f,
            "D-Up"     to if (axis(MotionEvent.AXIS_HAT_Y) < -0.5f) 1f else 0f,
            "D-Down"   to if (axis(MotionEvent.AXIS_HAT_Y) >  0.5f) 1f else 0f
        )

        axisMap.forEach { (axisLabel, value) ->
            val target = mappings[axisLabel] ?: return@forEach
            if (viewModel.testModeActive.value) {
                if (abs(value) > 0.15f) viewModel.setLastTriggered(axisLabel)
            } else {
                handleTarget(target, value != 0f, value)
            }
        }
        return true
    }

    private fun handleTarget(target: String, isActive: Boolean, value: Float) {
        val norm   = value.coerceIn(-1f, 1f)
        val norm01 = (norm + 1f) / 2f

        when {
            target == "Joystick1 X" -> viewModel.updateJoystick1(norm, viewModel.joystick1Y.value)
            target == "Joystick1 Y" -> viewModel.updateJoystick1(viewModel.joystick1X.value, norm)
            target == "Joystick2 X" -> viewModel.updateJoystick2(norm, viewModel.joystick2Y.value)
            target == "Joystick2 Y" -> viewModel.updateJoystick2(viewModel.joystick2X.value, norm)

            target.startsWith("Chord: ") -> {
                if (isActive) viewModel.setActiveChord(target.removePrefix("Chord: "))
            }

            // FIX: Strum branch was a stub. Now resolves strip index and dispatches
            // to engine using norm01 (0..1) as a position across the strip's notes.
            target.startsWith("Strum ") -> {
                val stripStr  = target.removePrefix("Strum ").trim()
                val stripIdx  = (stripStr.toIntOrNull() ?: 1) - 1   // "Strum 1" → index 0
                val noteIndex = (norm01 * 11f).toInt().coerceIn(0, 11)
                if (isActive) {
                    val vel = (abs(norm) * 127f).toInt().coerceIn(20, 127)
                    midiEngine.strumNoteOn(stripIdx, noteIndex, vel)
                } else {
                    midiEngine.strumNoteOff(stripIdx, noteIndex)
                }
            }

            target == "Select Note" -> {
                if (isActive) {
                    val note = viewModel.analysis.value.lastNote
                    if (viewModel.isHardlocked.value && !viewModel.isHoldActive.value)
                        viewModel.clearHardlock()
                    else
                        viewModel.setHardlock(note, hold = false)
                }
            }

            target == "Hold Note" -> {
                if (isActive) {
                    val note = viewModel.analysis.value.lastNote
                    if (viewModel.isHoldActive.value) viewModel.clearHardlock()
                    else viewModel.setHardlock(note, hold = true)
                }
            }

            target == "Portamento" -> viewModel.setPortamento(norm01)
            target == "Pitch Wheel" -> viewModel.setPitchBend(norm01)
            target == "Mod Wheel"   -> viewModel.setMod(norm01)
        }
    }

    private fun keyCodeToLabel(keyCode: Int): String? = when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_A      -> "A"
        KeyEvent.KEYCODE_BUTTON_B      -> "B"
        KeyEvent.KEYCODE_BUTTON_X      -> "X"
        KeyEvent.KEYCODE_BUTTON_Y      -> "Y"
        KeyEvent.KEYCODE_BUTTON_L1     -> "L1"
        KeyEvent.KEYCODE_BUTTON_R1     -> "R1"
        KeyEvent.KEYCODE_BUTTON_L2     -> "L2"
        KeyEvent.KEYCODE_BUTTON_R2     -> "R2"
        KeyEvent.KEYCODE_DPAD_UP       -> "D-Up"
        KeyEvent.KEYCODE_DPAD_DOWN     -> "D-Down"
        KeyEvent.KEYCODE_DPAD_LEFT     -> "D-Left"
        KeyEvent.KEYCODE_DPAD_RIGHT    -> "D-Right"
        KeyEvent.KEYCODE_BUTTON_THUMBL -> "LStick X"
        KeyEvent.KEYCODE_BUTTON_THUMBR -> "RStick X"
        else -> null
    }
}
