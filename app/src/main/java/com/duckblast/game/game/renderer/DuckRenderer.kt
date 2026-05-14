package com.duckblast.game.game.renderer

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.duckblast.game.game.entities.Duck
import com.duckblast.game.game.entities.Plate
import com.duckblast.game.game.entities.TargetState
import kotlin.math.PI
import kotlin.math.sin

/**
 * Renders [Duck] and [Plate] targets straight from Canvas primitives — bold
 * outlines, flat fills, pixel-aligned shapes. Handles flap animation, hit
 * flash, falling rotation, and the green/brown head variants.
 */
class DuckRenderer(private val density: Float) {

    private val paint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
    }
    private val outline = Paint().apply {
        isAntiAlias = false
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        color = 0xFF000033.toInt()
    }
    private val tmpRect = RectF()

    fun drawDuck(canvas: Canvas, duck: Duck) {
        canvas.save()
        canvas.translate(duck.x, duck.y)
        if (duck.state == TargetState.FALLING) {
            canvas.rotate(180f * (sin(System.currentTimeMillis() / 100.0).toFloat()).coerceIn(-0.05f, 0.05f) * 20f)
        }
        if (!duck.facingRight && duck.state != TargetState.FALLING) {
            canvas.scale(-1f, 1f)
        }

        val r = duck.radius
        val flashOn = duck.state == TargetState.HIT_FLASH &&
            (System.currentTimeMillis() / 50L) % 2L == 0L

        // Body
        paint.color = if (flashOn) DUCK_FLASH else DUCK_BROWN
        tmpRect.set(-r, -r * 0.55f, r, r * 0.55f)
        canvas.drawOval(tmpRect, paint)
        canvas.drawOval(tmpRect, outline)

        // White chest
        paint.color = if (flashOn) DUCK_FLASH else DUCK_WHITE
        tmpRect.set(-r * 0.55f, -r * 0.10f, r * 0.55f, r * 0.32f)
        canvas.drawOval(tmpRect, paint)

        // Head
        paint.color = if (flashOn) DUCK_FLASH else (if (duck.isGreenHead) DUCK_HEAD_GREEN else DUCK_HEAD_BROWN)
        canvas.drawCircle(r * 0.72f, -r * 0.32f, r * 0.34f, paint)
        canvas.drawCircle(r * 0.72f, -r * 0.32f, r * 0.34f, outline)

        // Beak
        paint.color = DUCK_BEAK
        canvas.drawRect(r * 0.90f, -r * 0.30f, r * 1.22f, -r * 0.08f, paint)
        canvas.drawRect(r * 0.90f, -r * 0.30f, r * 1.22f, -r * 0.08f, outline)

        // Eye (X if falling, dot otherwise)
        paint.color = 0xFF000000.toInt()
        if (duck.state == TargetState.FALLING) {
            paint.strokeWidth = 2f * density
            paint.style = Paint.Style.STROKE
            canvas.drawLine(r * 0.62f, -r * 0.42f, r * 0.78f, -r * 0.28f, paint)
            canvas.drawLine(r * 0.78f, -r * 0.42f, r * 0.62f, -r * 0.28f, paint)
            paint.style = Paint.Style.FILL
        } else {
            canvas.drawCircle(r * 0.78f, -r * 0.40f, r * 0.06f, paint)
        }

        // Wing — 3-frame flap based on duck.animationFrame
        paint.color = if (flashOn) DUCK_FLASH else DUCK_WING_DARK
        val wingY = when (duck.animationFrame) {
            0 -> -r * 0.32f
            1 -> -r * 0.05f
            else -> r * 0.22f
        }
        tmpRect.set(-r * 0.32f, wingY - r * 0.10f, r * 0.40f, wingY + r * 0.18f)
        canvas.drawRect(tmpRect, paint)
        canvas.drawRect(tmpRect, outline)

        canvas.restore()
    }

    fun drawPlate(canvas: Canvas, plate: Plate) {
        canvas.save()
        canvas.translate(plate.x, plate.y)
        canvas.rotate(plate.rotationDeg)
        val r = plate.radius
        val flashOn = plate.state == TargetState.HIT_FLASH

        paint.color = if (flashOn) DUCK_FLASH else PLATE_OUTER
        canvas.drawCircle(0f, 0f, r, paint)
        canvas.drawCircle(0f, 0f, r, outline)

        paint.color = if (flashOn) DUCK_FLASH else PLATE_INNER
        canvas.drawCircle(0f, 0f, r * 0.62f, paint)

        // Highlight arc
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f * density
        paint.color = PLATE_HIGHLIGHT
        tmpRect.set(-r * 0.85f, -r * 0.85f, r * 0.85f, r * 0.85f)
        canvas.drawArc(tmpRect, 200f, 70f, false, paint)
        paint.style = Paint.Style.FILL

        canvas.restore()
    }

    companion object {
        private const val DUCK_BROWN: Int = 0xFF884400.toInt()
        private const val DUCK_WHITE: Int = 0xFFFFFFFF.toInt()
        private const val DUCK_HEAD_GREEN: Int = 0xFF007700.toInt()
        private const val DUCK_HEAD_BROWN: Int = 0xFF552200.toInt()
        private const val DUCK_BEAK: Int = 0xFFFFA600.toInt()
        private const val DUCK_WING_DARK: Int = 0xFF552200.toInt()
        private const val DUCK_FLASH: Int = 0xFFFFFFFF.toInt()
        private const val PLATE_OUTER: Int = 0xFFB87333.toInt()
        private const val PLATE_INNER: Int = 0xFF884400.toInt()
        private const val PLATE_HIGHLIGHT: Int = 0xFFE2A968.toInt()
    }
}
