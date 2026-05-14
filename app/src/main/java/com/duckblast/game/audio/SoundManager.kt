package com.duckblast.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Plays synthesized one-shot sounds. Sounds are generated once on first init
 * via [SoundSynthesizer] and cached in memory. Playback uses a fixed thread
 * pool of 4 workers — up to 4 sounds can overlap.
 */
class SoundManager(context: Context) {

    private val appContext = context.applicationContext
    private val sampleRate = 44100
    private val synth = SoundSynthesizer(sampleRate)
    private val samples = ConcurrentHashMap<SoundId, ShortArray>()
    private val executor = Executors.newFixedThreadPool(MAX_VOICES)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val loadLock = Mutex()

    @Volatile var masterVolume: Float = 1f
        set(value) { field = value.coerceIn(0f, 1f) }

    @Volatile var muted: Boolean = false

    @Volatile private var preloaded = false

    private val loopLock = Any()
    private var loopTrack: AudioTrack? = null
    private var loopId: SoundId? = null

    fun preload(): Job = scope.launch {
        loadLock.withLock {
            if (preloaded) return@withLock
            for (id in SoundId.entries) {
                if (!samples.containsKey(id)) {
                    samples[id] = synth.generate(id)
                }
            }
            preloaded = true
        }
    }

    fun play(id: SoundId, volume: Float = 1f) {
        if (muted) return
        val effective = (masterVolume * volume).coerceIn(0f, 1f)
        if (executor.isShutdown) return
        executor.submit {
            try {
                // Lazy-generate on miss so calls that fire before preload completes
                // (e.g. splash intro on cold start) still produce audio.
                val data = samples[id] ?: synth.generate(id).also { samples[id] = it }
                val track = buildTrack(data.size)
                track.write(data, 0, data.size)
                track.setVolume(effective)
                track.play()
                val durationMs = (data.size * 1000L) / sampleRate
                Thread.sleep(durationMs + 60)
                runCatching { track.stop() }
                track.release()
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            } catch (_: IllegalStateException) {
                // AudioTrack was released mid-write — swallow.
            }
        }
    }

    /**
     * Start an infinitely-looping playback of [id]. Replaces any currently
     * playing loop. Idempotent if [id] is already the active loop.
     */
    fun playLoop(id: SoundId, volume: Float = 0.5f) {
        if (muted) return
        if (executor.isShutdown) return
        val effective = (masterVolume * volume).coerceIn(0f, 1f)
        executor.submit {
            synchronized(loopLock) {
                if (loopId == id && loopTrack != null) return@submit
                stopLoopInternal()
                try {
                    val data = samples[id] ?: synth.generate(id).also { samples[id] = it }
                    val track = buildTrack(data.size)
                    track.write(data, 0, data.size)
                    track.setVolume(effective)
                    track.setLoopPoints(0, data.size, -1)
                    track.play()
                    loopTrack = track
                    loopId = id
                } catch (_: IllegalStateException) {
                    // Track torn down mid-setup — leave fields cleared.
                }
            }
        }
    }

    /** Stop the active loop (if any). */
    fun stopLoop() {
        if (executor.isShutdown) {
            synchronized(loopLock) { stopLoopInternal() }
            return
        }
        executor.submit {
            synchronized(loopLock) { stopLoopInternal() }
        }
    }

    private fun stopLoopInternal() {
        loopTrack?.let { t ->
            runCatching { t.stop() }
            runCatching { t.release() }
        }
        loopTrack = null
        loopId = null
    }

    private fun buildTrack(samples: Int): AudioTrack {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val bytes = samples * 2
        return AudioTrack.Builder()
            .setAudioAttributes(attrs)
            .setAudioFormat(format)
            .setBufferSizeInBytes(bytes)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
    }

    fun shutdown() {
        synchronized(loopLock) { stopLoopInternal() }
        executor.shutdownNow()
        scope.coroutineContext[Job]?.cancel()
    }

    companion object {
        private const val MAX_VOICES = 4
    }
}
