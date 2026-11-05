package com.wisdomtower.academy.ui.games.common

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.HandlerThread
import kotlin.math.sin

/**
 * Synthesized short PCM tones (22050 Hz, 16-bit mono) into static AudioTracks.
 * Any failure = silent game, never a crash. [mutedState] skips play.
 */
object GameAudio {

    enum class Sfx {
        Tap, Correct, Wrong, Combo, HeartLost, Victory, Defeat, Tick, Freeze, Shot, Hit
    }

    @Volatile
    var mutedState: Boolean = false

    @Volatile
    private var masterVolume: Float = 0.5f

    private var ready = false
    private val tracks = HashMap<Sfx, AudioTrack>()
    private var thread: HandlerThread? = null
    private var handler: Handler? = null

    private const val SAMPLE_RATE = 22050

    /**
     * Reads the same preferences edited by SettingsScreen. Calling init from any game
     * refreshes both mute and volume before that game starts.
     */
    fun init(context: Context) {
        val prefs = context.applicationContext.getSharedPreferences("wt_settings_prefs", Context.MODE_PRIVATE)
        mutedState = !prefs.getBoolean("sound_effects", true)
        masterVolume = prefs.getFloat("sound_volume", 0.5f).coerceIn(0f, 1f)
        applyTrackVolume()
        if (ready) return
        try {
            val t = HandlerThread("GameAudio").also { it.start() }
            thread = t
            handler = Handler(t.looper)
            handler?.post {
                try {
                    tracks[Sfx.Tap] = buildTrack(tone(880.0, 0.04, 0.25))
                    tracks[Sfx.Correct] = buildTrack(chirp(523.25, 784.0, 0.12, 0.3))
                    tracks[Sfx.Wrong] = buildTrack(tone(110.0, 0.18, 0.35))
                    tracks[Sfx.Combo] = buildTrack(chirp(440.0, 880.0, 0.1, 0.28))
                    tracks[Sfx.HeartLost] = buildTrack(tone(180.0, 0.22, 0.4))
                    tracks[Sfx.Victory] = buildTrack(fanfare())
                    tracks[Sfx.Defeat] = buildTrack(tone(90.0, 0.35, 0.4))
                    tracks[Sfx.Tick] = buildTrack(tone(1200.0, 0.025, 0.15))
                    tracks[Sfx.Freeze] = buildTrack(tone(600.0, 0.15, 0.2))
                    tracks[Sfx.Shot] = buildTrack(tone(400.0, 0.05, 0.3))
                    tracks[Sfx.Hit] = buildTrack(tone(220.0, 0.08, 0.35))
                    applyTrackVolume()
                    ready = true
                } catch (_: Exception) {
                    ready = false
                }
            }
        } catch (_: Exception) {
            ready = false
        }
    }

    /** Apply the Settings screen's master effects switch immediately, including during play. */
    fun setMuted(value: Boolean) {
        mutedState = value
    }

    /** Apply the Settings screen's master volume immediately to every synthesized effect. */
    fun setVolume(value: Float) {
        masterVolume = value.coerceIn(0f, 1f)
        applyTrackVolume()
    }

    private fun applyTrackVolume() {
        val volume = masterVolume
        try {
            handler?.post {
                tracks.values.forEach { track ->
                    try { track.setVolume(volume) } catch (_: Exception) { }
                }
            } ?: tracks.values.forEach { track ->
                try { track.setVolume(volume) } catch (_: Exception) { }
            }
        } catch (_: Exception) {
        }
    }

    fun play(sfx: Sfx) {
        if (mutedState) return
        val track = tracks[sfx] ?: return
        try {
            handler?.post {
                try {
                    track.stop()
                    track.reloadStaticData()
                    track.play()
                } catch (_: Exception) {
                }
            }
        } catch (_: Exception) {
        }
    }

    fun release() {
        try {
            for (t in tracks.values) {
                try {
                    t.stop()
                    t.release()
                } catch (_: Exception) {
                }
            }
            tracks.clear()
            thread?.quitSafely()
            thread = null
            handler = null
            ready = false
        } catch (_: Exception) {
        }
    }

    private fun buildTrack(pcm: ShortArray): AudioTrack {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val format = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val track = AudioTrack.Builder()
            .setAudioAttributes(attrs)
            .setAudioFormat(format)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(pcm.size * 2)
            .build()
        track.write(pcm, 0, pcm.size)
        return track
    }

    private fun tone(freq: Double, seconds: Double, volume: Double): ShortArray {
        val n = (SAMPLE_RATE * seconds).toInt().coerceAtLeast(1)
        val out = ShortArray(n)
        val twoPi = 2.0 * Math.PI
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = 1.0 - (i.toDouble() / n)
            val sample = sin(twoPi * freq * t) * volume * env
            out[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun chirp(f0: Double, f1: Double, seconds: Double, volume: Double): ShortArray {
        val n = (SAMPLE_RATE * seconds).toInt().coerceAtLeast(1)
        val out = ShortArray(n)
        val twoPi = 2.0 * Math.PI
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val frac = i.toDouble() / n
            val freq = f0 + (f1 - f0) * frac
            val env = 1.0 - frac * 0.5
            val sample = sin(twoPi * freq * t) * volume * env
            out[i] = (sample * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun fanfare(): ShortArray {
        val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.5)
        val parts = ArrayList<Short>()
        for (f in notes) {
            val chunk = tone(f, 0.09, 0.28)
            for (s in chunk) parts.add(s)
        }
        return parts.toShortArray()
    }
}
