package com.example.util

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.MediaPlayer
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.sin

object AudioPlayerHelper {

    private var activeMediaPlayer: MediaPlayer? = null
    private var activeAudioTrack: AudioTrack? = null
    private var isPlayingAudio = false
    var onAlertCompletedListener: (() -> Unit)? = null

    fun playAudioUri(
        context: Context,
        uriString: String?,
        onComplete: (() -> Unit)? = null
    ) {
        stopAudio()

        if (!uriString.isNullOrBlank()) {
            try {
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )

                    val directFile = File(uriString)
                    if (directFile.exists()) {
                        val fis = java.io.FileInputStream(directFile)
                        setDataSource(fis.fd)
                        fis.close()
                    } else {
                        val uri = if (uriString.startsWith("content://") || uriString.startsWith("file://")) {
                            Uri.parse(uriString)
                        } else {
                            Uri.fromFile(directFile)
                        }
                        setDataSource(context, uri)
                    }

                    prepare()
                    setOnCompletionListener {
                        isPlayingAudio = false
                        it.release()
                        activeMediaPlayer = null
                        onComplete?.invoke()
                        onAlertCompletedListener?.invoke()
                    }
                    setOnErrorListener { _, _, _ ->
                        isPlayingAudio = false
                        activeMediaPlayer = null
                        onComplete?.invoke()
                        onAlertCompletedListener?.invoke()
                        true
                    }
                    start()
                }
                activeMediaPlayer = mp
                isPlayingAudio = true
                return
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Fallback: Play high quality synthesized Adhan / Chime harmonics
        playSynthesizedChime(onComplete)
    }

    fun playTimeChime(onComplete: (() -> Unit)? = null) {
        stopAudio()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val sampleRate = 44100
                // 4-note ascending Islamic mosque clock Westminster / Haram chime
                val notes = listOf(
                    Pair(523.25, 0.45), // C5
                    Pair(659.25, 0.45), // E5
                    Pair(783.99, 0.45), // G5
                    Pair(1046.50, 0.80) // C6
                )

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(sampleRate * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                audioTrack.play()
                isPlayingAudio = true

                for ((freq, durationSec) in notes) {
                    val numSamples = (durationSec * sampleRate).toInt()
                    val buffer = ShortArray(numSamples)
                    for (i in 0 until numSamples) {
                        val t = i.toDouble() / sampleRate
                        // Bell envelope decay
                        val envelope = Math.exp(-3.5 * t / durationSec)
                        // Harmonics for rich brass bell sound
                        val s1 = sin(2 * Math.PI * freq * t)
                        val s2 = 0.5 * sin(2 * Math.PI * (freq * 2) * t)
                        val s3 = 0.25 * sin(2 * Math.PI * (freq * 3) * t)
                        val sample = (s1 + s2 + s3) * envelope * Short.MAX_VALUE * 0.7
                        buffer[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                    audioTrack.write(buffer, 0, buffer.size)
                }

                audioTrack.stop()
                audioTrack.release()
                isPlayingAudio = false
                CoroutineScope(Dispatchers.Main).launch {
                    onComplete?.invoke()
                    onAlertCompletedListener?.invoke()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                CoroutineScope(Dispatchers.Main).launch {
                    onComplete?.invoke()
                    onAlertCompletedListener?.invoke()
                }
            }
        }
    }

    fun playSynthesizedChime(onComplete: (() -> Unit)? = null) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val sampleRate = 44100
                // Melodic Adhan Takbeer Motif (Allahu Akbar, Allahu Akbar)
                val notes = listOf(
                    Pair(392.00, 0.6), // G4
                    Pair(440.00, 0.7), // A4
                    Pair(523.25, 0.9), // C5
                    Pair(440.00, 0.6), // A4
                    Pair(392.00, 1.2), // G4
                    Pair(349.23, 0.5), // F4
                    Pair(392.00, 1.4)  // G4
                )

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(sampleRate * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                activeAudioTrack = audioTrack
                audioTrack.play()
                isPlayingAudio = true

                for ((freq, durationSec) in notes) {
                    if (!isPlayingAudio) break
                    val numSamples = (durationSec * sampleRate).toInt()
                    val buffer = ShortArray(numSamples)
                    for (i in 0 until numSamples) {
                        val t = i.toDouble() / sampleRate
                        val attack = (t / 0.05).coerceAtMost(1.0)
                        val decay = Math.exp(-1.5 * t / durationSec)
                        val s1 = sin(2 * Math.PI * freq * t)
                        val s2 = 0.3 * sin(2 * Math.PI * freq * 2 * t)
                        val sample = (s1 + s2) * attack * decay * Short.MAX_VALUE * 0.75
                        buffer[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                    audioTrack.write(buffer, 0, buffer.size)
                }

                audioTrack.stop()
                audioTrack.release()
                activeAudioTrack = null
                isPlayingAudio = false
                CoroutineScope(Dispatchers.Main).launch {
                    onComplete?.invoke()
                    onAlertCompletedListener?.invoke()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isPlayingAudio = false
                activeAudioTrack = null
                CoroutineScope(Dispatchers.Main).launch {
                    onComplete?.invoke()
                    onAlertCompletedListener?.invoke()
                }
            }
        }
    }

    fun playDuaMelody(onComplete: (() -> Unit)? = null) {
        stopAudio()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val sampleRate = 44100
                // Gentle, ambient spiritual harmonic progression for Post-Adhan Dua contemplation
                val chords = listOf(
                    Pair(349.23, 3.5), // F4
                    Pair(392.00, 3.5), // G4
                    Pair(440.00, 3.5), // A4
                    Pair(523.25, 4.0), // C5
                    Pair(440.00, 3.5), // A4
                    Pair(349.23, 4.0)  // F4 peaceful resolution
                )

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(sampleRate * 2)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                activeAudioTrack = audioTrack
                audioTrack.play()
                isPlayingAudio = true

                for ((freq, durationSec) in chords) {
                    if (!isPlayingAudio) break
                    val numSamples = (durationSec * sampleRate).toInt()
                    val buffer = ShortArray(numSamples)
                    for (i in 0 until numSamples) {
                        val t = i.toDouble() / sampleRate
                        val attack = (t / 0.8).coerceAtMost(1.0)
                        val decay = Math.exp(-0.8 * t / durationSec)
                        val s1 = sin(2 * Math.PI * freq * t)
                        val s2 = 0.4 * sin(2 * Math.PI * (freq * 1.5) * t) // Fifth
                        val s3 = 0.25 * sin(2 * Math.PI * (freq * 2) * t) // Octave
                        val sample = (s1 + s2 + s3) * attack * decay * Short.MAX_VALUE * 0.45
                        buffer[i] = sample.toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                    audioTrack.write(buffer, 0, buffer.size)
                }

                audioTrack.stop()
                audioTrack.release()
                activeAudioTrack = null
                isPlayingAudio = false
                CoroutineScope(Dispatchers.Main).launch {
                    onComplete?.invoke()
                    onAlertCompletedListener?.invoke()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                isPlayingAudio = false
                activeAudioTrack = null
                CoroutineScope(Dispatchers.Main).launch {
                    onComplete?.invoke()
                    onAlertCompletedListener?.invoke()
                }
            }
        }
    }

    fun stopAudio() {
        try {
            isPlayingAudio = false
            activeMediaPlayer?.let {
                if (it.isPlaying) {
                    it.stop()
                }
                it.release()
            }
            activeMediaPlayer = null
            activeAudioTrack?.let {
                it.stop()
                it.release()
            }
            activeAudioTrack = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun isPlaying(): Boolean = isPlayingAudio
}
