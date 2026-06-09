package com.nachinbombin.midimanipulator.midi

import android.content.Context
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiInputPort
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.os.Handler
import android.os.Looper
import com.nachinbombin.midimanipulator.service.VirtualMidiService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class MidiPortInfo(
    val deviceId: Int,
    val portIndex: Int,
    val name: String,
    /** true = device input port (app writes to it); false = device output port (app reads from it) */
    val isInput: Boolean
)

class MidiManager(private val context: Context) {

    private val systemMidiManager =
        context.getSystemService(Context.MIDI_SERVICE) as android.media.midi.MidiManager

    val engine = MidiEngine(context)

    private val _ports = MutableStateFlow<List<MidiPortInfo>>(emptyList())
    val ports: StateFlow<List<MidiPortInfo>> = _ports

    private val _activityLog = MutableStateFlow<List<String>>(emptyList())
    val activityLog: StateFlow<List<String>> = _activityLog

    private var connectedOutputPort: MidiOutputPort? = null  // device output port → we read from this
    private var connectedInputPort: MidiInputPort?  = null   // device input port  → we write to this

    private val handler = Handler(Looper.getMainLooper())

    private val deviceCallback = object : android.media.midi.MidiManager.DeviceCallback() {
        override fun onDeviceAdded(device: MidiDeviceInfo)   { refreshDevices() }
        override fun onDeviceRemoved(device: MidiDeviceInfo) { refreshDevices() }
    }

    fun initialize() {
        // Wire engine output to the connected device input port
        engine.outputSender = { bytes ->
            try { connectedInputPort?.send(bytes, 0, bytes.size) } catch (_: Exception) { }
        }
        // Set engineRef for virtual inter-app port
        VirtualMidiService.engineRef = engine

        refreshDevices()
        systemMidiManager.registerDeviceCallback(deviceCallback, handler)
    }

    fun refreshDevices() {
        val devices  = systemMidiManager.devices
        val portList = mutableListOf<MidiPortInfo>()
        devices.forEach { info ->
            val name = info.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "Unknown"
            repeat(info.inputPortCount)  { i ->
                portList.add(MidiPortInfo(info.id, i, "$name (In $i)",  isInput = true))
            }
            repeat(info.outputPortCount) { i ->
                portList.add(MidiPortInfo(info.id, i, "$name (Out $i)", isInput = false))
            }
        }
        _ports.value = portList
    }

    /** Open a device output port to listen for incoming MIDI (e.g. from a keyboard). */
    fun connectInput(portInfo: MidiPortInfo) {
        val device = systemMidiManager.devices.firstOrNull { it.id == portInfo.deviceId } ?: return
        systemMidiManager.openDevice(device, { midiDevice ->
            if (midiDevice == null) return@openDevice
            connectedOutputPort?.close()
            val outputPort = midiDevice.openOutputPort(portInfo.portIndex) ?: return@openDevice
            connectedOutputPort = outputPort
            outputPort.connect(object : MidiReceiver() {
                override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
                    if (count >= 3) {
                        val status   = msg[offset].toInt() and 0xFF
                        val note     = msg[offset + 1].toInt() and 0xFF
                        val vel      = msg[offset + 2].toInt() and 0xFF
                        val isNoteOn  = (status and 0xF0) == 0x90 && vel > 0
                        val isNoteOff = (status and 0xF0) == 0x80 ||
                                        ((status and 0xF0) == 0x90 && vel == 0)
                        if (isNoteOn || isNoteOff) engine.onIncomingNote(note, vel, isNoteOn)
                        logActivity("Ch${(status and 0x0F) + 1} " +
                            "${if (isNoteOn) "ON" else "OFF"} note=$note vel=$vel")
                    }
                }
            })
            logActivity("Input connected: ${portInfo.name}")
        }, handler)
    }

    /** Open a device input port to send MIDI to it (e.g. to a synth). */
    fun connectOutput(portInfo: MidiPortInfo) {
        val device = systemMidiManager.devices.firstOrNull { it.id == portInfo.deviceId } ?: return
        systemMidiManager.openDevice(device, { midiDevice ->
            if (midiDevice == null) return@openDevice
            connectedInputPort?.close()
            val inputPort = midiDevice.openInputPort(portInfo.portIndex) ?: return@openDevice
            connectedInputPort = inputPort
            // Rewire engine sender to newly connected port
            engine.outputSender = { bytes ->
                try { connectedInputPort?.send(bytes, 0, bytes.size) } catch (_: Exception) { }
            }
            logActivity("Output connected: ${portInfo.name}")
        }, handler)
    }

    /** Clean up all open ports and stop device callbacks. Called from Activity.onDestroy(). */
    fun release() {
        engine.allNotesOff()
        try { connectedOutputPort?.close() } catch (_: Exception) { }
        try { connectedInputPort?.close()  } catch (_: Exception) { }
        connectedOutputPort = null
        connectedInputPort  = null
        systemMidiManager.unregisterDeviceCallback(deviceCallback)
        VirtualMidiService.engineRef = null
    }

    private fun logActivity(msg: String) {
        val ts = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
            .format(java.util.Date())
        _activityLog.value = (listOf("[$ts] $msg") + _activityLog.value).take(200)
    }
}
