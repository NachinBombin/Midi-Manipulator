package com.nachinbombin.midimanipulator.midi

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

data class MidiAnalysis(
    val lastNote: Int = -1,
    val lastNoteName: String = "—",
    val rootNote: Int = -1,
    val rootNoteName: String = "—",
    val scaleName: String = "—",
    val chordContext: String = "—"
)

class MidiEngine(private val context: Context) {

    companion object {
        private const val BUFFER_WINDOW_MS = 4000L
        private const val NOTE_ON  = 0x90
        private const val NOTE_OFF = 0x80

        private val NOTE_NAMES = arrayOf(
            "C","C#","D","D#","E","F","F#","G","G#","A","A#","B"
        )

        fun noteName(note: Int): String {
            if (note < 0) return "—"
            val pc = note % 12
            val octave = note / 12 - 1
            return "${NOTE_NAMES[pc]}$octave"
        }
    }

    private val _analysis = MutableStateFlow(MidiAnalysis())
    val analysis: StateFlow<MidiAnalysis> = _analysis

    private val noteBuffer = ArrayDeque<Pair<Long, Int>>()

    // Track active generated harmony notes so we stop exactly those notes
    private var lastHarmonyNotes: List<Int> = emptyList()

    var hardlockedNote: Int = -1
        private set
    var isHardlocked: Boolean = false
        private set

    var harmonyChannel: Int = 1

    var portamentoTime: Int = 0
        set(value) {
            field = value
            sendCC(5, value.coerceIn(0, 127), harmonyChannel)
            sendCC(65, if (value > 0) 127 else 0, harmonyChannel)
        }

    var pitchBend: Int = 8192
        set(value) {
            field = value
            sendPitchBend(value, harmonyChannel)
        }

    var modValue: Int = 0
        set(value) {
            field = value
            sendCC(1, value.coerceIn(0, 127), harmonyChannel)
        }

    var outputSender: ((ByteArray) -> Unit)? = null

    fun onIncomingNote(noteNumber: Int, velocity: Int, isOn: Boolean) {
        val now = System.currentTimeMillis()
        if (isOn && velocity > 0) {
            noteBuffer.addLast(Pair(now, noteNumber))
            pruneBuffer(now)
            reanalyze(noteNumber)
            val refNote = if (isHardlocked) hardlockedNote else noteNumber
            generateVoices(refNote, velocity)
        } else {
            stopLastHarmonyNotes()
        }
    }

    fun hardlock(note: Int) {
        hardlockedNote = if (note >= 0) note else _analysis.value.lastNote
        isHardlocked = hardlockedNote >= 0
    }

    fun unlock() {
        isHardlocked = false
        hardlockedNote = -1
    }

    // Strum note helpers (called from UI)
    fun strumNoteOn(stripIndex: Int, noteIndex: Int, velocity: Int) {
        sendNoteOn(noteIndex.coerceIn(0, 127), velocity, harmonyChannel)
    }

    fun strumNoteOff(stripIndex: Int, noteIndex: Int) {
        sendNoteOff(noteIndex.coerceIn(0, 127), harmonyChannel)
    }

    private fun pruneBuffer(now: Long) {
        while (noteBuffer.isNotEmpty() && now - noteBuffer.first().first > BUFFER_WINDOW_MS) {
            noteBuffer.removeFirst()
        }
    }

    private fun reanalyze(currentNote: Int) {
        val pitchClasses = noteBuffer.map { it.second % 12 }.toSet()
        val (root, scale) = inferScale(pitchClasses, noteBuffer)
        val chord = inferChord(currentNote, root)
        _analysis.value = MidiAnalysis(
            lastNote     = currentNote,
            lastNoteName = noteName(currentNote),
            rootNote     = root,
            rootNoteName = NOTE_NAMES[root % 12],
            scaleName    = scale,
            chordContext = chord
        )
    }

    private fun inferScale(
        pitchClasses: Set<Int>,
        buffer: ArrayDeque<Pair<Long, Int>>
    ): Pair<Int, String> {
        val scales = mapOf(
            "Major"        to listOf(0,2,4,5,7,9,11),
            "Natural Min"  to listOf(0,2,3,5,7,8,10),
            "Harmonic Min" to listOf(0,2,3,5,7,8,11),
            "Melodic Min"  to listOf(0,2,3,5,7,9,11),
            "Dorian"       to listOf(0,2,3,5,7,9,10),
            "Phrygian"     to listOf(0,1,3,5,7,8,10),
            "Lydian"       to listOf(0,2,4,6,7,9,11),
            "Mixolydian"   to listOf(0,2,4,5,7,9,10),
            "Locrian"      to listOf(0,1,3,5,6,8,10),
            "Penta Major"  to listOf(0,2,4,7,9),
            "Penta Minor"  to listOf(0,3,5,7,10),
            "Blues"        to listOf(0,3,5,6,7,10),
            "Whole Tone"   to listOf(0,2,4,6,8,10),
            "Diminished"   to listOf(0,2,3,5,6,8,9,11)
        )
        if (pitchClasses.isEmpty()) return Pair(0, "Major")

        val now = System.currentTimeMillis()
        val weighted = mutableMapOf<Int, Double>()
        buffer.forEach { (ts, note) ->
            val age = (now - ts).toDouble() / BUFFER_WINDOW_MS
            val w = 1.0 - age * 0.5
            weighted[note % 12] = (weighted[note % 12] ?: 0.0) + w
        }

        var bestScore = -1.0
        var bestRoot = 0
        var bestScale = "Major"

        for (root in 0..11) {
            for ((scaleName, intervals) in scales) {
                val scaleNotes = intervals.map { (root + it) % 12 }.toSet()
                var score = 0.0
                for ((pc, w) in weighted) {
                    if (pc in scaleNotes) score += w
                }
                val coverage = pitchClasses.count { it in scaleNotes }.toDouble() / pitchClasses.size
                score *= coverage
                if (score > bestScore) {
                    bestScore = score
                    bestRoot = root
                    bestScale = scaleName
                }
            }
        }
        return Pair(bestRoot, bestScale)
    }

    private fun inferChord(note: Int, root: Int): String {
        val degree = ((note % 12) - root + 12) % 12
        val diatonicDegrees = listOf(0, 2, 4, 5, 7, 9, 11)
        val romanNumerals   = listOf("I", "ii", "iii", "IV", "V", "vi", "vii°")
        val idx = diatonicDegrees.indexOf(degree)
        return if (idx >= 0) romanNumerals[idx] else "?"
    }

    private fun generateVoices(rootNote: Int, velocity: Int) {
        stopLastHarmonyNotes()
        val root = _analysis.value.rootNote
        val scaleName = _analysis.value.scaleName
        val third = snapToScale(rootNote + 4, root, scaleName).coerceIn(0, 127)
        val fifth  = snapToScale(rootNote + 7, root, scaleName).coerceIn(0, 127)
        lastHarmonyNotes = listOf(third, fifth)
        sendNoteOn(third, velocity, harmonyChannel)
        sendNoteOn(fifth, velocity, harmonyChannel)
    }

    private fun stopLastHarmonyNotes() {
        lastHarmonyNotes.forEach { sendNoteOff(it, harmonyChannel) }
        lastHarmonyNotes = emptyList()
    }

    private fun snapToScale(note: Int, root: Int, scaleName: String): Int {
        val intervals = scaleIntervals(scaleName)
        val pc = ((note % 12) - root + 12) % 12
        val nearest = intervals.minByOrNull { abs(it - pc) } ?: pc
        return note + (nearest - pc)
    }

    fun scaleIntervals(scaleName: String): List<Int> = when (scaleName) {
        "Major"        -> listOf(0,2,4,5,7,9,11)
        "Natural Min"  -> listOf(0,2,3,5,7,8,10)
        "Harmonic Min" -> listOf(0,2,3,5,7,8,11)
        "Melodic Min"  -> listOf(0,2,3,5,7,9,11)
        "Dorian"       -> listOf(0,2,3,5,7,9,10)
        "Phrygian"     -> listOf(0,1,3,5,7,8,10)
        "Lydian"       -> listOf(0,2,4,6,7,9,11)
        "Mixolydian"   -> listOf(0,2,4,5,7,9,10)
        "Locrian"      -> listOf(0,1,3,5,6,8,10)
        "Penta Major"  -> listOf(0,2,4,7,9)
        "Penta Minor"  -> listOf(0,3,5,7,10)
        "Blues"        -> listOf(0,3,5,6,7,10)
        "Whole Tone"   -> listOf(0,2,4,6,8,10)
        "Diminished"   -> listOf(0,2,3,5,6,8,9,11)
        else           -> listOf(0,2,4,5,7,9,11)
    }

    fun buildChordVoicing(rootNote: Int, chordType: String): List<Int> {
        val root = rootNote % 12
        val octave = rootNote / 12
        val base = root + octave * 12
        return when (chordType) {
            "Triad"    -> listOf(base, base+4, base+7)
            "7th"      -> listOf(base, base+4, base+7, base+10)
            "9th"      -> listOf(base, base+4, base+7, base+10, base+14)
            "11th"     -> listOf(base, base+4, base+7, base+10, base+14, base+17)
            "13th"     -> listOf(base, base+4, base+7, base+10, base+14, base+17, base+21)
            "sus2"     -> listOf(base, base+2, base+7)
            "sus4"     -> listOf(base, base+5, base+7)
            "Power"    -> listOf(base, base+7)
            "add9"     -> listOf(base, base+4, base+7, base+14)
            "maj7"     -> listOf(base, base+4, base+7, base+11)
            "min7"     -> listOf(base, base+3, base+7, base+10)
            "dim7"     -> listOf(base, base+3, base+6, base+9)
            "aug"      -> listOf(base, base+4, base+8)
            "half-dim" -> listOf(base, base+3, base+6, base+10)
            "Major"    -> listOf(base, base+4, base+7)
            else       -> listOf(base, base+4, base+7)
        }.map { it.coerceIn(0, 127) }
    }

    fun sendNoteOn(note: Int, velocity: Int, channel: Int) {
        outputSender?.invoke(byteArrayOf(
            (NOTE_ON or (channel and 0x0F)).toByte(),
            note.toByte(),
            velocity.toByte()
        ))
    }

    fun sendNoteOff(note: Int, channel: Int) {
        outputSender?.invoke(byteArrayOf(
            (NOTE_OFF or (channel and 0x0F)).toByte(),
            note.toByte(),
            0
        ))
    }

    fun sendCC(cc: Int, value: Int, channel: Int) {
        outputSender?.invoke(byteArrayOf(
            (0xB0 or (channel and 0x0F)).toByte(),
            cc.toByte(),
            value.toByte()
        ))
    }

    fun sendPitchBend(value: Int, channel: Int) {
        val lsb = (value and 0x7F).toByte()
        val msb = ((value shr 7) and 0x7F).toByte()
        outputSender?.invoke(byteArrayOf(
            (0xE0 or (channel and 0x0F)).toByte(),
            lsb, msb
        ))
    }
}
