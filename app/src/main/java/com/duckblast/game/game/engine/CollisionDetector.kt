package com.duckblast.game.game.engine

import com.duckblast.game.game.entities.Target

/**
 * Generous circle-circle hit test — the crosshair center is treated as a
 * point and the target radius is padded by [forgivenessPx] so taps still feel
 * fair on small phones.
 */
object CollisionDetector {

    fun hit(
        crosshairX: Float,
        crosshairY: Float,
        target: Target,
        forgivenessPx: Float
    ): Boolean {
        val dx = crosshairX - target.x
        val dy = crosshairY - target.y
        val r = target.radius + forgivenessPx
        return (dx * dx + dy * dy) <= (r * r)
    }
}
