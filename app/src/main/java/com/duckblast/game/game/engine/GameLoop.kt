package com.duckblast.game.game.engine

import android.graphics.Canvas
import android.view.SurfaceHolder

/**
 * Fixed-timestep loop driven by a dedicated thread. The thread locks the
 * hardware canvas, calls [onUpdate] with a clamped delta-time, calls
 * [onRender], then unlocks and posts. Targets 60 FPS with [Thread.sleep]
 * back-off; dt is clamped to 50 ms to keep physics sane after a stall.
 */
class GameLoop(
    private val surfaceHolder: SurfaceHolder,
    private val onUpdate: (dt: Float) -> Unit,
    private val onRender: (canvas: Canvas) -> Unit
) {

    @Volatile private var running = false
    private var loopThread: Thread? = null

    fun isRunning(): Boolean = running

    fun start() {
        if (running) return
        running = true
        loopThread = Thread(::loop, "DuckBlast-GameLoop").apply {
            priority = Thread.MAX_PRIORITY - 1
            start()
        }
    }

    fun stop() {
        running = false
        loopThread?.let { thread ->
            thread.interrupt()
            try {
                thread.join(LOOP_JOIN_TIMEOUT_MS)
            } catch (_: InterruptedException) {
                Thread.currentThread().interrupt()
            }
        }
        loopThread = null
    }

    private fun loop() {
        var lastNs = System.nanoTime()
        val frameTargetNs = 1_000_000_000L / TARGET_FPS
        while (running) {
            val frameStartNs = System.nanoTime()
            val rawDeltaNs = frameStartNs - lastNs
            lastNs = frameStartNs
            val cappedNs = if (rawDeltaNs > MAX_FRAME_NS) MAX_FRAME_NS else rawDeltaNs
            val dt = cappedNs / 1_000_000_000f

            try {
                onUpdate(dt)
            } catch (t: Throwable) {
                if (running) throw t
            }

            var canvas: Canvas? = null
            try {
                canvas = surfaceHolder.lockHardwareCanvas()
                if (canvas != null) onRender(canvas)
            } catch (_: IllegalStateException) {
                // Surface destroyed between checks — skip frame.
            } finally {
                if (canvas != null) {
                    try {
                        surfaceHolder.unlockCanvasAndPost(canvas)
                    } catch (_: Exception) {
                        // Surface gone — give up the frame.
                    }
                }
            }

            val elapsedNs = System.nanoTime() - frameStartNs
            val sleepNs = frameTargetNs - elapsedNs
            if (sleepNs > 0) {
                try {
                    Thread.sleep(sleepNs / 1_000_000L, (sleepNs % 1_000_000L).toInt())
                } catch (_: InterruptedException) {
                    if (!running) break
                }
            }
        }
    }

    companion object {
        private const val TARGET_FPS = 60L
        private const val MAX_FRAME_NS = 50_000_000L      // clamp dt at 50ms
        private const val LOOP_JOIN_TIMEOUT_MS = 500L
    }
}
