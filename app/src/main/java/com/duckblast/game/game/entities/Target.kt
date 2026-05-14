package com.duckblast.game.game.entities

enum class TargetState {
    ACTIVE,        // flying / arcing — eligible for hits
    HIT_FLASH,     // briefly flashing white right after being shot
    FALLING,       // shot duck dropping; or plate momentarily breaking
    ESCAPING,      // shots ran out — fleeing straight up
    RESOLVED       // off-screen / replaced by particles
}

data class WorldBounds(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

sealed class Target {
    abstract var x: Float
    abstract var y: Float
    abstract val radius: Float
    abstract var state: TargetState
    abstract var escaped: Boolean
    abstract fun update(dt: Float, world: WorldBounds)
    abstract fun onHit()
    abstract fun beginEscape()

    val resolved: Boolean get() = state == TargetState.RESOLVED
    val killable: Boolean get() = state == TargetState.ACTIVE || state == TargetState.ESCAPING
}

/* ---------------- Particle types ---------------- */

class ScorePopup(
    var x: Float,
    var y: Float,
    val text: String,
    val color: Int,                  // ARGB
    val lifetime: Float = 1.0f,
    var elapsed: Float = 0f,
    val rise: Float = 60f
) {
    fun update(dt: Float) {
        elapsed += dt
        y -= rise * dt
    }
    val alpha: Float get() = (1f - elapsed / lifetime).coerceIn(0f, 1f)
    val expired: Boolean get() = elapsed >= lifetime
}

class PlateShard(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var rotationDeg: Float = 0f,
    val rotationSpeed: Float = 360f,
    val gravity: Float = 1800f,
    val lifetime: Float = 0.6f,
    var elapsed: Float = 0f,
    val orientation: Int                // 0 = left half, 1 = right half
) {
    fun update(dt: Float) {
        elapsed += dt
        vy += gravity * dt
        x += vx * dt
        y += vy * dt
        rotationDeg += rotationSpeed * dt
    }
    val alpha: Float get() = (1f - elapsed / lifetime).coerceIn(0f, 1f)
    val expired: Boolean get() = elapsed >= lifetime
}
