package com.duckblast.game.game.entities

class Crosshair {
    var x: Float = 0f
    var y: Float = 0f

    /** Pixel radius — touch tolerance is added on top in CollisionDetector. */
    var radius: Float = 32f

    fun moveTo(x: Float, y: Float, world: WorldBounds, padding: Float) {
        this.x = x.coerceIn(world.left + padding, world.right - padding)
        this.y = y.coerceIn(world.top + padding, world.bottom - padding)
    }
}
