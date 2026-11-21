package com.imtihanpro.app.ui.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

enum class SoundEffectType {
    TAP_TICK,
    CORRECT_CHIME,
    WRONG_THUD,
    CARD_FLIP,
    STREAK_FANFARE,
}

enum class AmbientSoundMode(
    val label: String,
    val subtitle: String,
) {
    NONE("Silent Mode", "Pure quiet focus"),
    RAIN("Warm Rain", "Gentle filtered rainfall for memorization"),
    BROWN_NOISE("Deep Brown Noise", "Low-frequency blanket masking distractions"),
    ALPHA("Soft Alpha Tone", "Calming 10 Hz modulated warm harmonic"),
}

/**
 * Zero-asset procedural PCM audio synthesizer for crisp quiz SFX and haptics.
 * Adds 0 KB of audio binaries to the Play Store APK.
 */
object ImtihanSensoryEngine {
    private val sfxScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private const val SAMPLE_RATE = 22050

    fun playSoundEffect(type: SoundEffectType, enabled: Boolean) {
        if (!enabled) return
        sfxScope.launch {
            runCatching {
                val pcm = synthesizeSfx(type)
                playShortPcmBuffer(pcm)
            }
        }
    }

    private fun synthesizeSfx(type: SoundEffectType): ShortArray {
        return when (type) {
            SoundEffectType.TAP_TICK -> {
                val samples = (SAMPLE_RATE * 0.045f).toInt()
                ShortArray(samples) { i ->
                    val t = i.toFloat() / SAMPLE_RATE
                    val env = exp(-t * 85f)
                    val wave = sin(2.0 * PI * 920.0 * t).toFloat()
                    (wave * env * 0.35f * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }
            SoundEffectType.CARD_FLIP -> {
                val samples = (SAMPLE_RATE * 0.085f).toInt()
                ShortArray(samples) { i ->
                    val t = i.toFloat() / SAMPLE_RATE
                    val freq = 340f + 560f * (i.toFloat() / samples)
                    val env = sin(PI * i / samples).toFloat() * 0.32f
                    val wave = sin(2.0 * PI * freq * t).toFloat()
                    (wave * env * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }
            SoundEffectType.CORRECT_CHIME -> {
                val samples = (SAMPLE_RATE * 0.28f).toInt()
                ShortArray(samples) { i ->
                    val t = i.toFloat() / SAMPLE_RATE
                    val f1 = if (t < 0.11f) 587.33 else 880.0
                    val localT = if (t < 0.11f) t else t - 0.11f
                    val env = exp(-localT * 13f) * 0.42f
                    val wave = (sin(2.0 * PI * f1 * t) + 0.3 * sin(2.0 * PI * f1 * 2.0 * t)).toFloat()
                    (wave * env * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }
            SoundEffectType.WRONG_THUD -> {
                val samples = (SAMPLE_RATE * 0.22f).toInt()
                ShortArray(samples) { i ->
                    val t = i.toFloat() / SAMPLE_RATE
                    val freq = 195f - 65f * (i.toFloat() / samples)
                    val env = exp(-t * 15f) * 0.45f
                    val wave = sin(2.0 * PI * freq * t).toFloat()
                    (wave * env * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }
            SoundEffectType.STREAK_FANFARE -> {
                val notes = listOf(523.25, 659.25, 783.99, 1046.50)
                val stepSamples = (SAMPLE_RATE * 0.09f).toInt()
                val tailSamples = (SAMPLE_RATE * 0.25f).toInt()
                val total = notes.size * stepSamples + tailSamples
                ShortArray(total) { i ->
                    val noteIdx = (i / stepSamples).coerceAtMost(notes.lastIndex)
                    val noteStart = noteIdx * stepSamples
                    val localT = (i - noteStart).toFloat() / SAMPLE_RATE
                    val env = exp(-localT * 9.5f) * 0.40f
                    val f = notes[noteIdx]
                    val t = i.toFloat() / SAMPLE_RATE
                    val wave = (sin(2.0 * PI * f * t) + 0.25 * sin(2.0 * PI * f * 1.5 * t)).toFloat()
                    (wave * env * Short.MAX_VALUE).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }
            }
        }
    }

    private fun playShortPcmBuffer(samples: ShortArray) {
        val byteLen = samples.size * 2
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val format = AudioFormat.Builder()
            .setSampleRate(SAMPLE_RATE)
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()

        val track = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AudioTrack.Builder()
                .setAudioAttributes(attrs)
                .setAudioFormat(format)
                .setBufferSizeInBytes(byteLen.coerceAtLeast(2048))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
        } else {
            @Suppress("DEPRECATION")
            AudioTrack(
                android.media.AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                byteLen.coerceAtLeast(2048),
                AudioTrack.MODE_STATIC
            )
        }

        try {
            track.write(samples, 0, samples.size)
            track.play()
            val durationMs = ((samples.size.toFloat() / SAMPLE_RATE) * 1000f).toLong() + 60L
            Thread.sleep(durationMs)
        } finally {
            runCatching {
                track.stop()
                track.release()
            }
        }
    }
}

/**
 * Lifecycle-safe continuous ambient study soundscape synthesizer for Focus Pomodoro Studio.
 */
class AmbientSoundscapePlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var playJob: Job? = null
    private var activeTrack: AudioTrack? = null
    private var currentMode: AmbientSoundMode = AmbientSoundMode.NONE

    fun setMode(mode: AmbientSoundMode) {
        if (currentMode == mode && playJob?.isActive == true) return
        currentMode = mode
        stop()
        if (mode == AmbientSoundMode.NONE) return

        playJob = scope.launch {
            runCatching {
                val sampleRate = 16000
                val chunkSamples = 2048
                val minBuf = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                ).coerceAtLeast(chunkSamples * 4)

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(sampleRate)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(minBuf)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()

                activeTrack = track
                try {
                    track.play()

                    val buffer = ShortArray(chunkSamples)
                    var brownState = 0f
                    var rainLp = 0f
                    var phase = 0.0
                    var lfoPhase = 0.0

                    while (isActive) {
                        when (mode) {
                            AmbientSoundMode.BROWN_NOISE -> {
                                for (i in 0 until chunkSamples) {
                                    val white = Random.nextFloat() * 2f - 1f
                                    brownState = (brownState + 0.025f * white) / 1.025f
                                    val out = (brownState * 2.2f).coerceIn(-0.45f, 0.45f)
                                    buffer[i] = (out * Short.MAX_VALUE).toInt().toShort()
                                }
                            }
                            AmbientSoundMode.RAIN -> {
                                for (i in 0 until chunkSamples) {
                                    val white = Random.nextFloat() * 2f - 1f
                                    rainLp = rainLp * 0.86f + white * 0.14f
                                    val sparkle = if (Random.nextInt(900) == 0) (Random.nextFloat() - 0.5f) * 0.35f else 0f
                                    val out = ((rainLp * 0.9f) + sparkle).coerceIn(-0.35f, 0.35f)
                                    buffer[i] = (out * Short.MAX_VALUE).toInt().toShort()
                                }
                            }
                            AmbientSoundMode.ALPHA -> {
                                for (i in 0 until chunkSamples) {
                                    phase += 2.0 * PI * 174.0 / sampleRate
                                    lfoPhase += 2.0 * PI * 0.18 / sampleRate
                                    val mod = 0.65 + 0.35 * sin(lfoPhase)
                                    val base = sin(phase) * 0.22 + sin(phase * 1.5) * 0.08
                                    val out = (base * mod).toFloat().coerceIn(-0.32f, 0.32f)
                                    buffer[i] = (out * Short.MAX_VALUE).toInt().toShort()
                                }
                            }
                            AmbientSoundMode.NONE -> break
                        }
                        if (!isActive) break
                        track.write(buffer, 0, chunkSamples)
                    }
                } finally {
                    runCatching {
                        track.pause()
                        track.flush()
                        track.stop()
                    }
                    runCatching { track.release() }
                    if (activeTrack === track) {
                        activeTrack = null
                    }
                }
            }
        }
    }

    fun stop() {
        val oldJob = playJob
        playJob = null
        oldJob?.cancel()
    }
}

class ImtihanFeedbackController(
    private val haptic: HapticFeedback,
    val soundEnabled: Boolean,
    val hapticsEnabled: Boolean,
) {
    fun tap() {
        if (hapticsEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        ImtihanSensoryEngine.playSoundEffect(SoundEffectType.TAP_TICK, soundEnabled)
    }

    fun flipCard() {
        if (hapticsEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        ImtihanSensoryEngine.playSoundEffect(SoundEffectType.CARD_FLIP, soundEnabled)
    }

    fun correct() {
        if (hapticsEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        ImtihanSensoryEngine.playSoundEffect(SoundEffectType.CORRECT_CHIME, soundEnabled)
    }

    fun wrong() {
        if (hapticsEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        ImtihanSensoryEngine.playSoundEffect(SoundEffectType.WRONG_THUD, soundEnabled)
    }

    fun fanfare() {
        if (hapticsEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
        ImtihanSensoryEngine.playSoundEffect(SoundEffectType.STREAK_FANFARE, soundEnabled)
    }
}

@Composable
fun rememberImtihanFeedback(
    soundEnabled: Boolean,
    hapticsEnabled: Boolean,
): ImtihanFeedbackController {
    val haptic = LocalHapticFeedback.current
    return remember(haptic, soundEnabled, hapticsEnabled) {
        ImtihanFeedbackController(
            haptic = haptic,
            soundEnabled = soundEnabled,
            hapticsEnabled = hapticsEnabled,
        )
    }
}
