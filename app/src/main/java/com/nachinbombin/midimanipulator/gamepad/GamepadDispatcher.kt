package com.nachinbombin.midimanipulator.gamepad

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.nachinbombin.midimanipulator.midi.MidiEngine
import com.nachinbombin.midimanipulator.viewmodel.AppViewModel

class GamepadDispatcher(
    private val viewModel: AppViewModel,
    private val midiEngine: MidiEngine
) {

    fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.source and InputDevice.SOURCE_GAMEPAD != InputDevice.SOURCE_GAMEPAD) return false
        if (event.action != KeyEvent.ACTION_DOWN && event.action != KeyEvent.ACTION_UP) return false

        val descriptor = event.device?.descriptor ?: return false
        val mappings   = viewModel.gamepadMappings.value[descriptor]?.mappings ?: return false
        val buttonLabel = keyCodeToLabel(event.keyCode) ?: return false
        val target     = mappings[buttonLabel] ?: return false

        if (viewModel.testModeActive.value) {
            viewModel.setLastTriggered(buttonLabel)
            return true
        }

        val isDown = event.action == KeyEvent.ACTION_DOWN
        handleTarget(target, isDown, 1f)
        return true
    }

    fun onMotionEvent(event: MotionEvent): Boolean {
        if (event.source and InputDevice.SOURCE_JOYSTICK != InputDevice.SOURCE_JOYSTICK) return false

        val descriptor = event.device?.descriptor ?: return false
        val mappings   = viewModel.gamepadMappings.value[descriptor]?.mappings ?: return false

        val lx = event.getAxisValue(MotionEvent.AXIS_X)
        val ly = event.getAxisValue(MotionEvent.AXIS_Y)
        val rx = event.getAxisValue(MotionEvent.AXIS_Z)
        val ry = event.getAxisValue(MotionEvent.AXIS_RZ)

        if (viewModel.testModeActive.value) {
            if (lx != 0f || ly != 0f) viewModel.setLastTriggered("LStick X")
            if (rx != 0f || ry != 0f) viewModel.setLastTriggered("RStick X")
            return true
        }

        mappings.forEach { (button, target) ->
            val axisVal = when (button) {
                "LStick X" -> lx; "LStick Y" -> ly
                "RStick X" -> rx; "RStick Y" -> ry
                else -> return@forEach
            }
            when (target) {
                "Joystick1 X" -> viewModel.updateJoystick1(axisVal, viewModel.joystick1Y.value)
                "Joystick1 Y" -> viewModel.updateJoystick1(viewModel.joystick1X.value, axisVal)
                "Joystick2 X" -> viewModel.updateJoystick2(axisVal, viewModel.joystick2Y.value)
                "Joystick2 Y" -> viewModel.updateJoystick2(viewModel.joystick2X.value, axisVal)
                "Portamento"  -> viewModel.setPortamento((axisVal + 1f) / 2f)
                "Pitch Wheel" -> viewModel.setPitchBend((axisVal + 1f) / 2f)
                "Mod Wheel"   -> viewModel.setMod((axisVal + 1f) / 2f)
            }
        }
        return true
    }

    private fun handleTarget(target: String, isDown: Boolean, value: Float) {
        when {
            target == "Select Note" && isDown -> {
                val analysis = midiEngine.analysis.value
                if (viewModel.isHardlocked.value && !viewModel.isHoldActive.value) viewModel.clearHardlock()
                else viewModel.setHardlock(analysis.lastNote, hold = false)
            }
            target == "Hold Note" && isDown -> {
                val analysis = midiEngine.analysis.value
                if (viewModel.isHoldActive.value) viewModel.clearHardlock()
                else viewModel.setHardlock(analysis.lastNote, hold = true)
            }
            target.startsWith("Chord: ") && isDown -> {
                viewModel.setActiveChord(target.removePrefix("Chord: "))
            }
            target.startsWith("Strum ") -> { /* handled by strum strip via viewmodel axis */ }
        }
    }

    private fun keyCodeToLabel(keyCode: Int): String? = when (keyCode) {
        KeyEvent.KEYCODE_BUTTON_A     -> "A"
        KeyEvent.KEYCODE_BUTTON_B     -> "B"
        KeyEvent.KEYCODE_BUTTON_X     -> "X"
        KeyEvent.KEYCODE_BUTTON_Y     -> "Y"
        KeyEvent.KEYCODE_BUTTON_L1    -> "L1"
        KeyEvent.KEYCODE_BUTTON_R1    -> "R1"
        KeyEvent.KEYCODE_BUTTON_L2    -> "L2"
        KeyEvent.KEYCODE_BUTTON_R2    -> "R2"
        KeyEvent.KEYCODE_DPAD_UP      -> "D-Up"
        KeyEvent.KEYCODE_DPAD_DOWN    -> "D-Down"
        KeyEvent.KEYCODE_DPAD_LEFT    -> "D-Left"
        KeyEvent.KEYCODE_DPAD_RIGHT   -> "D-Right"
        else -> null
    }
}
