package com.duckblast.game.audio

import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

/**
 * Generates 16-bit PCM mono audio buffers in memory for every [SoundId].
 *
 * All sounds are described as math: sine waves, white-noise bursts, envelopes
 * and simple mixing — no samples, no copyrighted content. Output is one
 * ShortArray per sound, ready to feed into AudioTrack.MODE_STATIC.
 */
class SoundSynthesizer(private val sampleRate: Int = 44100) {

    private val twoPi = (PI * 2).toFloat()
    private val maxAmp: Short = 24000   // headroom below Short.MAX_VALUE

    fun generate(id: SoundId): ShortArray = when (id) {
        SoundId.GUNSHOT -> gunshot()
        SoundId.DUCK_QUACK -> duckQuack()
        SoundId.DUCK_FALL -> duckFall()
        SoundId.DUCK_ESCAPED -> duckEscaped()
        SoundId.DOG_LAUGH -> dogLaugh()
        SoundId.DOG_BARK -> dogBark()
        SoundId.PLATE_LAUNCH -> plateLaunch()
        SoundId.PLATE_BREAK -> plateBreak()
        SoundId.ROUND_START -> roundStart()
        SoundId.LEVEL_CLEAR -> levelClear()
        SoundId.GAME_OVER -> gameOver()
        SoundId.PERFECT_ROUND -> perfectRound()
        SoundId.SCORE_TICK -> scoreTick()
        SoundId.MENU_SELECT -> menuSelect()
        SoundId.SPLASH_FANFARE -> splashFanfare()
        SoundId.MENU_THEME -> menuTheme()
    }

    // ---------------- shot ----------------

    private fun gunshot(): ShortArray {
        val totalMs = 135
        val out = FloatArray(samplesFor(totalMs))
        // Noise burst: full amp 15ms, then exponential decay to 0 over 120ms
        val burstSamples = samplesFor(15)
        val decaySamples = samplesFor(120)
        for (i in 0 until burstSamples) out[i] = (Random.nextFloat() * 2f - 1f)
        for (i in 0 until decaySamples) {
            val t = i.toFloat() / decaySamples
            val env = exp(-4f * t)
            out[burstSamples + i] = (Random.nextFloat() * 2f - 1f) * env
        }
        // Low thump: 80 Hz sine, 60ms with fast decay
        val thumpSamples = samplesFor(60)
        for (i in 0 until thumpSamples) {
            val t = i.toFloat() / sampleRate
            val env = exp(-30f * t)
            val s = sin(twoPi * 80f * t) * env
            out[i] = out[i] * 0.6f + s * 0.4f
        }
        return normalize(out)
    }

    // ---------------- duck ----------------

    private fun duckQuack(): ShortArray {
        val totalMs = 200
        val total = samplesFor(totalMs)
        val out = FloatArray(total)
        val segmentSamples = samplesFor(40)
        for (i in 0 until total) {
            val seg = i / segmentSamples
            val baseFreq = if (seg % 2 == 0) 400f else 500f
            val t = i.toFloat() / sampleRate
            val vibrato = 20f * sin(twoPi * 8f * t)
            out[i] = sin(twoPi * (baseFreq + vibrato) * t)
        }
        return normalize(applyEnvelope(out, attackMs = 10, releaseMs = 30))
    }

    private fun duckFall(): ShortArray {
        val totalMs = 600
        val total = samplesFor(totalMs)
        val out = FloatArray(total)
        var phase = 0f
        for (i in 0 until total) {
            val t = i.toFloat() / total
            val freq = 800f + (200f - 800f) * t            // 800 → 200 Hz
            val wobble = 30f * sin(twoPi * 12f * (i.toFloat() / sampleRate))
            phase += twoPi * (freq + wobble) / sampleRate
            out[i] = sin(phase)
        }
        return normalize(applyEnvelope(out, attackMs = 20, releaseMs = 80))
    }

    private fun duckEscaped(): ShortArray {
        val totalMs = 400
        val total = samplesFor(totalMs)
        val out = FloatArray(total)
        var phase = 0f
        for (i in 0 until total) {
            val t = i.toFloat() / total
            val freq = 300f + (1200f - 300f) * t
            phase += twoPi * freq / sampleRate
            out[i] = sin(phase)
        }
        // Fade out last 100ms
        val fadeSamples = samplesFor(100)
        for (i in 0 until fadeSamples) {
            val idx = total - fadeSamples + i
            out[idx] = out[idx] * (1f - i.toFloat() / fadeSamples)
        }
        return normalize(out)
    }

    // ---------------- dog ----------------

    private fun dogLaugh(): ShortArray {
        val burstMs = 80
        val gapMs = 100
        val total = samplesFor(burstMs * 3 + gapMs * 2)
        val out = FloatArray(total)
        var cursor = 0
        repeat(3) {
            val burstSamples = samplesFor(burstMs)
            for (i in 0 until burstSamples) {
                val t = i.toFloat() / sampleRate
                val env = envelopeAR(i, burstSamples, attackFrac = 0.1f, releaseFrac = 0.4f)
                out[cursor + i] = sin(twoPi * 300f * t) * env
            }
            cursor += burstSamples + samplesFor(gapMs)
        }
        return normalize(out)
    }

    private fun dogBark(): ShortArray {
        val out = FloatArray(samplesFor(110))
        // Noise burst 30ms
        val noiseSamples = samplesFor(30)
        for (i in 0 until noiseSamples) out[i] = (Random.nextFloat() * 2f - 1f) * 0.8f
        // 180 Hz sine 80ms
        val sineSamples = samplesFor(80)
        for (i in 0 until sineSamples) {
            val t = i.toFloat() / sampleRate
            val env = envelopeAR(i, sineSamples, attackFrac = 0.05f, releaseFrac = 0.6f)
            val s = sin(twoPi * 180f * t) * env
            val idx = noiseSamples + i
            if (idx < out.size) out[idx] = s
        }
        return normalize(out)
    }

    // ---------------- plate ----------------

    private fun plateLaunch(): ShortArray {
        // Rising whoosh: low-pass filtered noise with amplitude ramp 0→1 over 200ms
        val total = samplesFor(200)
        val out = FloatArray(total)
        var lp = 0f
        val cutoff = 0.08f         // simple IIR coefficient → low frequencies
        for (i in 0 until total) {
            val noise = Random.nextFloat() * 2f - 1f
            lp += cutoff * (noise - lp)
            val ramp = i.toFloat() / total
            out[i] = lp * ramp
        }
        return normalize(out)
    }

    private fun plateBreak(): ShortArray {
        val burstSamples = samplesFor(25)
        val decaySamples = samplesFor(80)
        val out = FloatArray(burstSamples + decaySamples)
        // High-frequency crack: modulate noise by a 5kHz sine to push energy upward
        for (i in 0 until burstSamples) {
            val t = i.toFloat() / sampleRate
            val carrier = sin(twoPi * 5000f * t)
            out[i] = (Random.nextFloat() * 2f - 1f) * carrier
        }
        for (i in 0 until decaySamples) {
            val t = i.toFloat() / decaySamples
            out[burstSamples + i] = (Random.nextFloat() * 2f - 1f) * exp(-5f * t) * 0.7f
        }
        return normalize(out)
    }

    // ---------------- musical ----------------

    private fun roundStart(): ShortArray {
        val notes = floatArrayOf(262f, 330f, 392f)        // C-E-G
        return concatNotes(notes, durationMs = 150, useSquare = false, fadeReleaseMs = 30)
    }

    private fun levelClear(): ShortArray {
        val notes = floatArrayOf(262f, 294f, 330f, 349f, 392f)
        return concatNotes(notes, durationMs = 120, useSquare = true, fadeReleaseMs = 20)
    }

    private fun gameOver(): ShortArray {
        val notes = floatArrayOf(392f, 330f, 294f, 262f)
        val dry = concatNotes(notes, durationMs = 180, useSquare = true, fadeReleaseMs = 40)
        // Echo: mix in a 120ms-delayed copy at 30% amplitude
        val delaySamples = samplesFor(120)
        val out = FloatArray(dry.size + delaySamples)
        for (i in dry.indices) out[i] += dry[i] / maxAmp.toFloat()
        for (i in dry.indices) out[i + delaySamples] += (dry[i] / maxAmp.toFloat()) * 0.3f
        return normalize(out)
    }

    private fun perfectRound(): ShortArray {
        val intro = concatNotes(floatArrayOf(523f, 659f, 784f, 1047f),
            durationMs = 100, useSquare = false, fadeReleaseMs = 20)
        // Trill: alternate 784/880 Hz every 30ms for 300ms
        val trillTotal = samplesFor(300)
        val trillStep = samplesFor(30)
        val trill = FloatArray(trillTotal)
        for (i in 0 until trillTotal) {
            val freq = if ((i / trillStep) % 2 == 0) 784f else 880f
            val t = i.toFloat() / sampleRate
            trill[i] = sin(twoPi * freq * t)
        }
        val trillPcm = normalize(applyEnvelope(trill, 10, 40))
        val out = ShortArray(intro.size + trillPcm.size)
        intro.copyInto(out, 0)
        trillPcm.copyInto(out, intro.size)
        return out
    }

    // ---------------- ui ----------------

    private fun scoreTick(): ShortArray {
        val total = samplesFor(20)
        val out = FloatArray(total)
        for (i in 0 until total) {
            val t = i.toFloat() / sampleRate
            out[i] = sin(twoPi * 800f * t)
        }
        return normalize(out)            // hard cut, no release
    }

    private fun menuSelect(): ShortArray {
        val total = samplesFor(40)
        val out = FloatArray(total)
        for (i in 0 until total) {
            val t = i.toFloat() / sampleRate
            val env = 1f - (i.toFloat() / total)
            out[i] = sin(twoPi * 440f * t) * env
        }
        return normalize(out)
    }

    // ---------------- intros ----------------

    /** Rising 4-note arpeggio capped by a held C-major chord. ~1.4s total. */
    private fun splashFanfare(): ShortArray {
        val arp = floatArrayOf(523f, 659f, 784f, 1047f)            // C5 E5 G5 C6
        val arpNoteMs = 130
        val arpSamples = samplesFor(arpNoteMs)
        val arpBuf = ShortArray(arpSamples * arp.size)
        for ((idx, freq) in arp.withIndex()) {
            val buf = FloatArray(arpSamples)
            for (i in 0 until arpSamples) {
                val t = i.toFloat() / sampleRate
                val raw = sin(twoPi * freq * t)
                val sq = if (raw > 0f) 1f else -1f
                buf[i] = raw * 0.7f + sq * 0.3f                    // hybrid sine+square → NES feel
            }
            val shaped = normalize(applyEnvelope(buf, attackMs = 6, releaseMs = 30))
            shaped.copyInto(arpBuf, idx * arpSamples)
        }

        // Sustain chord C5+E5+G5+C6 for 700ms with slow decay
        val chord = floatArrayOf(523f, 659f, 784f, 1047f)
        val chordMs = 700
        val chordSamples = samplesFor(chordMs)
        val chordBuf = FloatArray(chordSamples)
        for (i in 0 until chordSamples) {
            val t = i.toFloat() / sampleRate
            var s = 0f
            for (f in chord) s += sin(twoPi * f * t)
            chordBuf[i] = s / chord.size
        }
        // Decay envelope: slow exponential
        for (i in 0 until chordSamples) {
            val t = i.toFloat() / chordSamples
            chordBuf[i] *= exp(-2.5f * t)
        }
        val chordPcm = normalize(applyEnvelope(chordBuf, attackMs = 10, releaseMs = 80))

        val out = ShortArray(arpBuf.size + chordPcm.size)
        arpBuf.copyInto(out, 0)
        chordPcm.copyInto(out, arpBuf.size)
        return out
    }

    /**
     * Seamlessly-loopable menu theme. ~3.84s — a 16-note melody in C major over a
     * 4-note bass line. Each note has an attack/release envelope so the buffer
     * starts and ends near zero, which means setLoopPoints(0, end, -1) won't click.
     */
    private fun menuTheme(): ShortArray {
        val noteMs = 240
        val melody = floatArrayOf(
            523f, 659f, 784f, 659f,    // C5 E5 G5 E5
            698f, 880f, 784f, 659f,    // F5 A5 G5 E5
            587f, 698f, 880f, 698f,    // D5 F5 A5 F5
            784f, 659f, 587f, 523f     // G5 E5 D5 C5
        )
        val bass = floatArrayOf(131f, 175f, 196f, 131f) // C3 F3 G3 C3 — one note per bar

        val noteSamples = samplesFor(noteMs)
        val totalSamples = noteSamples * melody.size
        val barSamples = noteSamples * 4
        val out = FloatArray(totalSamples)

        // Melody — soft square wave, prominent
        for ((idx, freq) in melody.withIndex()) {
            val offset = idx * noteSamples
            for (i in 0 until noteSamples) {
                val t = i.toFloat() / sampleRate
                val raw = sin(twoPi * freq * t)
                val sq = if (raw > 0f) 1f else -1f
                val tone = raw * 0.4f + sq * 0.6f
                val env = envelopeAR(i, noteSamples, attackFrac = 0.05f, releaseFrac = 0.55f)
                out[offset + i] += tone * env * 0.55f
            }
        }

        // Bass — softer, longer notes, sine for warmth
        for ((idx, freq) in bass.withIndex()) {
            val offset = idx * barSamples
            for (i in 0 until barSamples) {
                val t = i.toFloat() / sampleRate
                val env = envelopeAR(i, barSamples, attackFrac = 0.03f, releaseFrac = 0.35f)
                out[offset + i] += sin(twoPi * freq * t) * env * 0.35f
            }
        }

        return normalize(out)
    }

    // ---------------- helpers ----------------

    private fun concatNotes(
        notes: FloatArray,
        durationMs: Int,
        useSquare: Boolean,
        fadeReleaseMs: Int
    ): ShortArray {
        val noteSamples = samplesFor(durationMs)
        val out = ShortArray(noteSamples * notes.size)
        for ((idx, freq) in notes.withIndex()) {
            val buf = FloatArray(noteSamples)
            for (i in 0 until noteSamples) {
                val t = i.toFloat() / sampleRate
                val raw = sin(twoPi * freq * t)
                buf[i] = if (useSquare) (if (raw > 0f) 1f else -1f) else raw
            }
            val shaped = normalize(applyEnvelope(buf, attackMs = 4, releaseMs = fadeReleaseMs))
            shaped.copyInto(out, idx * noteSamples)
        }
        return out
    }

    private fun applyEnvelope(buffer: FloatArray, attackMs: Int, releaseMs: Int): FloatArray {
        val attack = samplesFor(attackMs).coerceAtMost(buffer.size / 2)
        val release = samplesFor(releaseMs).coerceAtMost(buffer.size / 2)
        for (i in 0 until attack) buffer[i] *= i.toFloat() / attack
        for (i in 0 until release) {
            val idx = buffer.size - 1 - i
            buffer[idx] *= i.toFloat() / release
        }
        return buffer
    }

    private fun envelopeAR(
        i: Int,
        length: Int,
        attackFrac: Float,
        releaseFrac: Float
    ): Float {
        val attack = (length * attackFrac).toInt().coerceAtLeast(1)
        val release = (length * releaseFrac).toInt().coerceAtLeast(1)
        return when {
            i < attack -> i.toFloat() / attack
            i >= length - release -> ((length - i).toFloat() / release).coerceAtLeast(0f)
            else -> 1f
        }
    }

    private fun normalize(buffer: FloatArray): ShortArray {
        var peak = 0f
        for (v in buffer) {
            val abs = if (v < 0f) -v else v
            if (abs > peak) peak = abs
        }
        if (peak == 0f) return ShortArray(buffer.size)
        val gain = (maxAmp.toFloat() / peak).coerceAtMost(maxAmp.toFloat())
        val out = ShortArray(buffer.size)
        for (i in buffer.indices) {
            val s = (buffer[i] * gain).toInt()
            out[i] = s.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun samplesFor(durationMs: Int): Int = (sampleRate * durationMs) / 1000
}
