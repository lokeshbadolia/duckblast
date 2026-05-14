package com.duckblast.game.game.entities

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

class Duck(
    override var x: Float,
    override var y: Float,
    var baseVx: Float,           // horizontal velocity (px/s)
    var baseVy: Float,           // vertical velocity (px/s, negative = up)
    private val zigzagAmplitude: Float,
    private val zigzagFrequency: Float,
    override val radius: Float
) : Target() {

    override var state: TargetState = TargetState.ACTIVE
    override var escaped: Boolean = false

    val isGreenHead: Boolean = (kotlin.random.Random.nextFloat() < 0.5f)
    var facingRight: Boolean = baseVx >= 0f
        private set

    var animationFrame: Int = 0
        private set
    private var animationTimer: Float = 0f
    private val frameDuration: Float = 0.12f

    private var elapsed: Float = 0f
    private var hitTimer: Float = 0f
    private val hitFlashDuration: Float = 0.30f
    private var fallVy: Float = 0f
    private val fallGravity: Float = 1400f

    override fun update(dt: Float, world: WorldBounds) {
        elapsed += dt
        when (state) {
            TargetState.ACTIVE -> {
                stepFlight(dt, world)
                bounce(world)
                if (y < world.top - radius) {
                    escaped = true
                    state = TargetState.RESOLVED
                }
                animate(dt)
            }
            TargetState.HIT_FLASH -> {
                hitTimer += dt
                animate(dt)
                if (hitTimer >= hitFlashDuration) {
                    state = TargetState.FALLING
                    fallVy = 200f
                }
            }
            TargetState.FALLING -> {
                fallVy += fallGravity * dt
                y += fallVy * dt
                if (y > world.bottom + radius * 2f) {
                    state = TargetState.RESOLVED
                }
            }
            TargetState.ESCAPING -> {
                y += baseVy * dt
                animate(dt)
                if (y < world.top - radius) {
                    escaped = true
                    state = TargetState.RESOLVED
                }
            }
            TargetState.RESOLVED -> Unit
        }
    }

    private fun stepFlight(dt: Float, world: WorldBounds) {
        x += baseVx * dt
        y += baseVy * dt
        val zig = zigzagAmplitude * sin((2.0 * PI).toFloat() * zigzagFrequency * elapsed) * dt
        x += zig
    }

    private fun bounce(world: WorldBounds) {
        if (x < world.left + radius) {
            x = world.left + radius
            baseVx = abs(baseVx)
            facingRight = true
        } else if (x > world.right - radius) {
            x = world.right - radius
            baseVx = -abs(baseVx)
            facingRight = false
        }
    }

    private fun animate(dt: Float) {
        animationTimer += dt
        if (animationTimer >= frameDuration) {
            animationTimer -= frameDuration
            animationFrame = (animationFrame + 1) % 3
        }
    }

    override fun onHit() {
        if (state == TargetState.ACTIVE || state == TargetState.ESCAPING) {
            state = TargetState.HIT_FLASH
            hitTimer = 0f
        }
    }

    override fun beginEscape() {
        if (state == TargetState.ACTIVE) {
            state = TargetState.ESCAPING
            baseVx = 0f
            baseVy = -abs(baseVy) * 2f - 120f
        }
    }
}
