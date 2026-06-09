package com.nachinbombin.midimanipulator.midi

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.abs

data class MidiAnalysis(
    val lastNote: Int = -1,
    val lastNoteName: String = "\u2014",
    val rootNote: Int = -1,
    val rootNoteName: String = "\u2014",
    val scaleName: String = "\u2014",
    val chordContext: String = "\u2014"
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
            if (note < 0) return "\u2014"
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
    // Track active joystick melody note
    private var activeJoystickNote: Int = -1
    // Track active chord notes from joystick 2
    private var activeChordNotes: List<Int> = emptyList()
    // Map strum stripIndex -> set of active note numbers so NoteOff hits the right notes
    private val activeStrumNotes = mutableMapOf<Int, MutableSet<Int>>()

    var hardlockedNote: Int = -1
        private set
    var isHardlocked: Boolean = false
        private set

    /** Channel (0-based) used for all generated harmony/chord/joystick voices */
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

    /** Set by MidiManager so the engine can write bytes to the selected output port. */
    var outputSender: ((ByteArray) -> Unit)? = null

    // ─────────────────────────────────────────────────────────────────────────
    // Public API — called from outside
    // ─────────────────────────────────────────────────────────────────────────

    fun onIncomingNote(noteNumber: Int, velocity: Int, isOn: Boolean) {
        val now = System.currentTimeMillis()
        if (isOn && velocity > 0) {
            noteBuffer.addLast(Pair(now, noteNumber))
            pruneBuffer(now)
            reanalyze(noteNumber)
            val refNote = if (isHardlocked) hardlockedNote else noteNumber
            generateVoices(refNote, velocity)
        } else {
            // FIX: stop harmony voices when the source note is released
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

    // ─── Strum helpers ────────────────────────────────────────────────────────

    /**
     * FIX: was passing noteIndex raw as MIDI note number.
     * Now computes the actual MIDI note from the chord root + interval across 3 octaves of
     * the chord voicing. noteIndex 0..N-1 maps to chord tones ascending over 3 octaves.
     */
    fun strumNoteOn(stripIndex: Int, noteIndex: Int, velocity: Int) {
        val root = if (isHardlocked) hardlockedNote else _analysis.value.lastNote
        val baseRoot = if (root >= 0) root else 60  // default to middle C
        val chordType = STRUM_CHORD_TYPES.getOrElse(stripIndex) { "Triad" }
        val intervals = buildChordVoicing(baseRoot, chordType)
        // Spread across 3 octaves: repeat the chord voicing, shifting up 12 each pass
        val octave  = noteIndex / intervals.size
        val degree  = noteIndex % intervals.size
        val midiNote = (intervals.getOrElse(degree) { baseRoot } + octave * 12).coerceIn(0, 127)
        activeStrumNotes.getOrPut(stripIndex) { mutableSetOf() }.add(midiNote)
        sendNoteOn(midiNote, velocity, harmonyChannel)
    }

    fun strumNoteOff(stripIndex: Int, noteIndex: Int) {
        val root = if (isHardlocked) hardlockedNote else _analysis.value.lastNote
        val baseRoot = if (root >= 0) root else 60
        val chordType = STRUM_CHORD_TYPES.getOrElse(stripIndex) { "Triad" }
        val intervals = buildChordVoicing(baseRoot, chordType)
        val octave   = noteIndex / intervals.size
        val degree   = noteIndex % intervals.size
        val midiNote = (intervals.getOrElse(degree) { baseRoot } + octave * 12).coerceIn(0, 127)
        sendNoteOff(midiNote, harmonyChannel)
        activeStrumNotes[stripIndex]?.remove(midiNote)
    }

    /** Release all notes for a given strip (used when strip lock is toggled off). */
    fun releaseStrumStrip(stripIndex: Int) {
        activeStrumNotes[stripIndex]?.forEach { sendNoteOff(it, harmonyChannel) }
        activeStrumNotes[stripIndex]?.clear()
    }

    // ─── Joystick 1 — melodic single voice ────────────────────────────────────

    /**
     * FIX: note calculation was using root pitch-class addition which produced wrong octave.
     * Now anchors to the reference note's octave and adds the scale interval offset directly.
     */
    fun joystickMelodyUpdate(sectorDegree: Int, distance: Float) {
        val refNote = if (isHardlocked) hardlockedNote else _analysis.value.lastNote
        if (refNote < 0) return

        val root      = _analysis.value.rootNote
        val scaleName = _analysis.value.scaleName
        val intervals = scaleIntervals(scaleName)
        // Anchor octave to refNote; interval is semitones above root in same octave block
        val rootInOctave = (refNote / 12) * 12 + root % 12
        val interval     = if (intervals.isNotEmpty()) intervals[sectorDegree % intervals.size] else sectorDegree * 2
        val rawNote      = rootInOctave + interval
        val targetNote   = snapToScale(rawNote, root, scaleName).coerceIn(0, 127)
        val velocity     = (distance * 127f).toInt().coerceIn(0, 127)

        if (targetNote != activeJoystickNote) {
            if (activeJoystickNote >= 0) sendNoteOff(activeJoystickNote, harmonyChannel)
            if (velocity > 0) sendNoteOn(targetNote, velocity, harmonyChannel)
            activeJoystickNote = if (velocity > 0) targetNote else -1
        } else if (velocity == 0 && activeJoystickNote >= 0) {
            sendNoteOff(activeJoystickNote, harmonyChannel)
            activeJoystickNote = -1
        }
    }

    fun joystickMelodyRelease() {
        if (activeJoystickNote >= 0) {
            sendNoteOff(activeJoystickNote, harmonyChannel)
            activeJoystickNote = -1
        }
    }

    // ─── Joystick 2 — chord voicing ───────────────────────────────────────────

    fun joystickChordUpdate(chordType: String, distance: Float) {
        val refNote = if (isHardlocked) hardlockedNote else _analysis.value.lastNote
        if (refNote < 0) return
        val velocity = (distance * 127f).toInt().coerceIn(1, 127)
        val notes    = buildChordVoicing(refNote, chordType)
        if (notes != activeChordNotes) {
            activeChordNotes.forEach { sendNoteOff(it, harmonyChannel) }
            notes.forEach { sendNoteOn(it.coerceIn(0, 127), velocity, harmonyChannel) }
            activeChordNotes = notes
        }
    }

    fun joystickChordRelease() {
        activeChordNotes.forEach { sendNoteOff(it, harmonyChannel) }
        activeChordNotes = emptyList()
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Internal analysis
    // ─────────────────────────────────────────────────────────────────────────

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
        // Time-weighted pitch-class accumulator: recent notes score higher
        val weighted = mutableMapOf<Int, Double>()
        buffer.forEach { (ts, note) ->
            val age = (now - ts).toDouble() / BUFFER_WINDOW_MS   // 0 = just now, 1 = oldest
            val w   = 1.0 - age * 0.5                             // weight: 1.0 → 0.5
            weighted[note % 12] = (weighted[note % 12] ?: 0.0) + w
        }

        var bestScore = -1.0
        var bestRoot  = 0
        var bestScale = "Major"

        for (root in 0..11) {
            for ((scaleName, intervals) in scales) {
                val scaleNotes = intervals.map { (root + it) % 12 }.toSet()
                var score = 0.0
                for ((pc, w) in weighted) {
                    if (pc in scaleNotes) score += w
                }
                // Weight by how many of the played pitch-classes the scale covers
                val coverage = pitchClasses.count { it in scaleNotes }.toDouble() / pitchClasses.size
                score *= coverage
                if (score > bestScore) {
                    bestScore = score
                    bestRoot  = root
                    bestScale = scaleName
                }
            }
        }
        return Pair(bestRoot, bestScale)
    }

    private fun inferChord(note: Int, root: Int): String {
        val degree = ((note % 12) - root + 12) % 12
        val diatonicDegrees = listOf(0, 2, 4, 5, 7, 9, 11)
        val romanNumerals   = listOf("I", "ii", "iii", "IV", "V", "vi", "vii\u00B0")
        val idx = diatonicDegrees.indexOf(degree)
        return if (idx >= 0) romanNumerals[idx] else "?"
    }

    private fun generateVoices(rootNote: Int, velocity: Int) {
        stopLastHarmonyNotes()
        val root      = _analysis.value.rootNote
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

    // ─────────────────────────────────────────────────────────────────────────
    // Scale / chord helpers
    // ─────────────────────────────────────────────────────────────────────────

    fun snapToScale(note: Int, root: Int, scaleName: String): Int {
        val intervals = scaleIntervals(scaleName)
        if (intervals.isEmpty()) return note
        val octave = note / 12
        val pc     = note % 12
        val relPc  = ((pc - root) + 12) % 12
        // Find closest scale degree by semitone distance
        val closest = intervals.minByOrNull { abs(it - relPc) } ?: relPc
        return octave * 12 + ((root + closest) % 12)
    }

    private fun scaleIntervals(scaleName: String): List<Int> = when (scaleName) {
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

    private fun buildChordVoicing(root: Int, chordType: String): List<Int> {
        val intervals = when (chordType) {
            "Triad"  -> listOf(0, 4, 7)
            "7th"    -> listOf(0, 4, 7, 10)
            "9th"    -> listOf(0, 4, 7, 10, 14)
            "11th"   -> listOf(0, 4, 7, 10, 14, 17)
            "13th"   -> listOf(0, 4, 7, 10, 14, 17, 21)
            "sus2"   -> listOf(0, 2, 7)
            "sus4"   -> listOf(0, 5, 7)
            "Power"  -> listOf(0, 7)
            "add9"   -> listOf(0, 4, 7, 14)
            "maj7"   -> listOf(0, 4, 7, 11)
            "min7"   -> listOf(0, 3, 7, 10)
            "dim7"   -> listOf(0, 3, 6, 9)
            "aug"    -> listOf(0, 4, 8)
            "hdim"   -> listOf(0, 3, 6, 10)
            else     -> listOf(0, 4, 7)
        }
        return intervals.map { (root + it).coerceIn(0, 127) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // MIDI send helpers
    // ─────────────────────────────────────────────────────────────────────────

    private fun sendNoteOn(note: Int, velocity: Int, channel: Int) {
        val ch = channel.coerceIn(0, 15)
        outputSender?.invoke(byteArrayOf(
            (NOTE_ON  or ch).toByte(),
            note.coerceIn(0, 127).toByte(),
            velocity.coerceIn(0, 127).toByte()
        ))
    }

    private fun sendNoteOff(note: Int, channel: Int) {
        val ch = channel.coerceIn(0, 15)
        outputSender?.invoke(byteArrayOf(
            (NOTE_OFF or ch).toByte(),
            note.coerceIn(0, 127).toByte(),
            0x00
        ))
    }

    private fun sendCC(cc: Int, value: Int, channel: Int) {
        val ch = channel.coerceIn(0, 15)
        outputSender?.invoke(byteArrayOf(
            (0xB0 or ch).toByte(),
            cc.coerceIn(0, 127).toByte(),
            value.coerceIn(0, 127).toByte()
        ))
    }

    private fun sendPitchBend(value: Int, channel: Int) {
        // 14-bit pitch bend: 0..16383, center = 8192
        val v   = value.coerceIn(0, 16383)
        val lsb = (v and 0x7F).toByte()
        val msb = ((v shr 7) and 0x7F).toByte()
        val ch  = channel.coerceIn(0, 15)
        outputSender?.invoke(byteArrayOf((0xE0 or ch).toByte(), lsb, msb))
    }

    companion object {
        // Chord type per strum strip index (matches STRUM_LABELS in PerformanceScreen)
        val STRUM_CHORD_TYPES = listOf("Major", "Triad", "7th", "9th", "sus2", "sus4")
    }
}
