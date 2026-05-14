package com.duckblast.game.game.entities

import kotlin.math.PI
import kotlin.math.atan2

class Gun {
    var x: Float = 0f
    var y: Float = 0f

    var currentAngleDeg: Float = -90f
        private set
    var targetAngleDeg: Float = -90f
        private set

    /** Negative pixels = recoiled back along the barrel direction. */
    var recoilOffset: Float = 0f
        private set
    var muzzleFlashAlpha: Float = 0f
        private set

    private val recoilBackMs = 0.08f
    private val recoilReturnMs = 0.12f
    private val recoilPx = 8f
    private var recoilTimer: Float = 0f
    private val muzzleFlashDuration: Float = 0.033f
    private var muzzleFlashTimer: Float = 0f

    fun update(dt: Float, crosshair: Crosshair) {
        val dx = crosshair.x - x
        val dy = crosshair.y - y
        targetAngleDeg = (atan2(dy, dx) * 180.0 / PI).toFloat()
        currentAngleDeg += (shortestAngle(targetAngleDeg - currentAngleDeg)) * 0.25f

        if (recoilTimer > 0f) {
            recoilTimer -= dt
            val total = recoilBackMs + recoilReturnMs
            val tFromEnd = recoilTimer.coerceAtLeast(0f)
            val tFromStart = (total - tFromEnd).coerceAtLeast(0f)
            recoilOffset = if (tFromStart < recoilBackMs) {
                -recoilPx * (tFromStart / recoilBackMs)
            } else {
                val r = (tFromStart - recoilBackMs) / recoilReturnMs
                -recoilPx * (1f - r.coerceIn(0f, 1f))
            }
        } else {
            recoilOffset = 0f
        }

        if (muzzleFlashTimer > 0f) {
            muzzleFlashTimer -= dt
            muzzleFlashAlpha = (muzzleFlashTimer / muzzleFlashDuration).coerceIn(0f, 1f)
        } else {
            muzzleFlashAlpha = 0f
        }
    }

    fun fire() {
        recoilTimer = recoilBackMs + recoilReturnMs
        muzzleFlashTimer = muzzleFlashDuration
    }

    private fun shortestAngle(deltaDeg: Float): Float {
        var d = deltaDeg
        while (d > 180f) d -= 360f
        while (d < -180f) d += 360f
        return d
    }
}
