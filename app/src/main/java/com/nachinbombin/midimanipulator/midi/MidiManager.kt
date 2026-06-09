package com.nachinbombin.midimanipulator.midi

import android.content.Context
import android.media.midi.MidiDeviceInfo
import android.media.midi.MidiInputPort
import android.media.midi.MidiOutputPort
import android.media.midi.MidiReceiver
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class MidiPortInfo(
    val deviceId: Int,
    val portIndex: Int,
    val name: String,
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

    private var connectedOutputPort: MidiOutputPort? = null
    private var connectedInputPort: MidiInputPort? = null

    private val handler = Handler(Looper.getMainLooper())

    fun initialize() {
        engine.outputSender = { bytes ->
            try { connectedInputPort?.send(bytes, 0, bytes.size) } catch (e: Exception) { /* ignore */ }
        }
        refreshDevices()
        systemMidiManager.registerDeviceCallback(
            object : android.media.midi.MidiManager.DeviceCallback() {
                override fun onDeviceAdded(device: MidiDeviceInfo) { refreshDevices() }
                override fun onDeviceRemoved(device: MidiDeviceInfo) { refreshDevices() }
            }, handler
        )
    }

    fun refreshDevices() {
        val devices = systemMidiManager.devices
        val portList = mutableListOf<MidiPortInfo>()
        devices.forEach { info ->
            val name = info.properties.getString(MidiDeviceInfo.PROPERTY_NAME) ?: "Unknown"
            repeat(info.inputPortCount) { i ->
                portList.add(MidiPortInfo(info.id, i, "$name (In $i)", isInput = true))
            }
            repeat(info.outputPortCount) { i ->
                portList.add(MidiPortInfo(info.id, i, "$name (Out $i)", isInput = false))
            }
        }
        _ports.value = portList
    }

    fun connectInput(portInfo: MidiPortInfo) {
        val device = systemMidiManager.devices.firstOrNull { it.id == portInfo.deviceId } ?: return
        systemMidiManager.openDevice(device, { midiDevice ->
            if (midiDevice == null) return@openDevice
            val outputPort = midiDevice.openOutputPort(portInfo.portIndex) ?: return@openDevice
            outputPort.connect(object : MidiReceiver() {
                override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
                    if (count >= 3) {
                        val status   = msg[offset].toInt() and 0xFF
                        val note     = msg[offset + 1].toInt() and 0xFF
                        val vel      = msg[offset + 2].toInt() and 0xFF
                        val isNoteOn  = (status and 0xF0) == 0x90
                        val isNoteOff = (status and 0xF0) == 0x80
                        if (isNoteOn || isNoteOff) {
                            engine.onIncomingNote(note, vel, isNoteOn && vel > 0)
                        }
                        logActivity("Ch${(status and 0x0F) + 1} ${if (isNoteOn) "ON" else if (isNoteOff) "OFF" else "MSG"} N$note V$vel")
                    }
                    try { connectedInputPort?.send(msg, offset, count) } catch (e: Exception) { /* passthrough */ }
                }
            })
            connectedOutputPort = outputPort
        }, handler)
    }

    fun connectOutput(portInfo: MidiPortInfo) {
        val device = systemMidiManager.devices.firstOrNull { it.id == portInfo.deviceId } ?: return
        systemMidiManager.openDevice(device, { midiDevice ->
            connectedInputPort = midiDevice?.openInputPort(portInfo.portIndex)
        }, handler)
    }

    private fun logActivity(msg: String) {
        val current = _activityLog.value.toMutableList()
        current.add(0, msg)
        if (current.size > 100) current.removeLast()
        _activityLog.value = current
    }

    fun release() {
        try { connectedOutputPort?.close() } catch (e: Exception) { /* ignore */ }
        try { connectedInputPort?.close() } catch (e: Exception) { /* ignore */ }
    }
}
