package com.duckblast.game.game.entities

class Plate(
    override var x: Float,
    override var y: Float,
    var vx: Float,
    var vy: Float,
    override val radius: Float,
    val gravity: Float = 900f
) : Target() {

    override var state: TargetState = TargetState.ACTIVE
    override var escaped: Boolean = false
    var rotationDeg: Float = 0f
        private set

    private val rotationSpeed: Float = 480f      // deg/s — visually ~8° per 60fps frame

    override fun update(dt: Float, world: WorldBounds) {
        when (state) {
            TargetState.ACTIVE -> {
                vy += gravity * dt
                x += vx * dt
                y += vy * dt
                rotationDeg += rotationSpeed * dt
                if (x < world.left - radius * 2f || x > world.right + radius * 2f
                    || y > world.bottom + radius * 2f) {
                    escaped = true
                    state = TargetState.RESOLVED
                }
            }
            TargetState.HIT_FLASH,
            TargetState.FALLING,
            TargetState.ESCAPING -> {
                state = TargetState.RESOLVED      // shards take over rendering
            }
            TargetState.RESOLVED -> Unit
        }
    }

    override fun onHit() {
        if (state == TargetState.ACTIVE) state = TargetState.HIT_FLASH
    }

    /**
     * Plates don't get an escape mode — they fly their parabola and either get
     * hit or off-screen on their own. Implemented as a no-op for the [Target]
     * contract.
     */
    override fun beginEscape() = Unit
}
