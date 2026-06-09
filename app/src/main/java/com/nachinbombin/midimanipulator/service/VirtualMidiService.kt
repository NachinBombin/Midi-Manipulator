package com.nachinbombin.midimanipulator.service

import android.media.midi.MidiDeviceService
import android.media.midi.MidiReceiver

/**
 * Exposes the app as a virtual MIDI device so DAWs and other apps
 * can connect to it as an inter-app MIDI port.
 *
 * Declared in AndroidManifest.xml with:
 *   <service android:name=".service.VirtualMidiService"
 *            android:permission="android.permission.BIND_MIDI_DEVICE_SERVICE">
 *     <intent-filter>
 *       <action android:name="android.media.midi.MidiDeviceService" />
 *     </intent-filter>
 *     <meta-data android:name="android.media.midi"
 *                android:resource="@xml/virtual_device_info" />
 *   </service>
 */
class VirtualMidiService : MidiDeviceService() {

    override fun onGetInputPortReceivers(): Array<MidiReceiver> {
        // Return one receiver port — bytes sent here are forwarded to MidiEngine
        return arrayOf(object : MidiReceiver() {
            override fun onSend(msg: ByteArray, offset: Int, count: Int, timestamp: Long) {
                if (count >= 3) {
                    val status   = msg[offset].toInt() and 0xFF
                    val note     = msg[offset + 1].toInt() and 0xFF
                    val velocity = msg[offset + 2].toInt() and 0xFF
                    val isNoteOn = (status and 0xF0) == 0x90 && velocity > 0
                    val isNoteOff= (status and 0xF0) == 0x80
                    if (isNoteOn || isNoteOff) {
                        // Post to engine on its own thread
                        engineRef?.onIncomingNote(note, velocity, isNoteOn)
                    }
                }
            }
        })
    }

    companion object {
        // Set by MidiManager.initialize() after engine is ready
        var engineRef: com.nachinbombin.midimanipulator.midi.MidiEngine? = null
    }
}
