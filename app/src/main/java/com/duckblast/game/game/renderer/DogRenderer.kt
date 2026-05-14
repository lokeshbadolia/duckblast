package com.duckblast.game.game.renderer

import android.graphics.Canvas
import android.graphics.Paint
import com.duckblast.game.game.entities.DogMode
import com.duckblast.game.game.entities.DuckDog
import kotlin.math.PI
import kotlin.math.sin

/**
 * Pixel-art hunting-dog companion. All poses (run / sniff / jump / carry /
 * laugh / flinch) are built from rectangles and circles so nothing depends on
 * external sprite assets.
 */
class DogRenderer(private val density: Float) {

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

    fun draw(canvas: Canvas, dog: DuckDog) {
        if (!dog.visible) return
        canvas.save()
        canvas.translate(dog.x, dog.y)

        val w = 92f * density
        val h = 58f * density
        val progress = dog.progress

        when (dog.mode) {
            DogMode.INTRO_RUN -> drawRun(canvas, w, h, progress)
            DogMode.INTRO_SNIFF -> drawSniff(canvas, w, h, progress)
            DogMode.INTRO_JUMP -> drawJump(canvas, w, h, progress)
            DogMode.SUCCESS_CARRY -> drawCarry(canvas, w, h, progress)
            DogMode.LAUGH -> drawLaugh(canvas, w, h, progress)
            DogMode.FLINCH -> drawFlinch(canvas, w, h, progress)
            DogMode.HIDDEN -> Unit
        }
        canvas.restore()
    }

    private fun drawRun(canvas: Canvas, w: Float, h: Float, progress: Float) {
        val offset = (1f - progress) * -200f * density
        canvas.translate(offset, 0f)
        drawBody(canvas, w, h, tail = true, mouthOpen = false, ears = false)
    }

    private fun drawSniff(canvas: Canvas, w: Float, h: Float, progress: Float) {
        // crouched — translate down a hair
        canvas.translate(0f, 6f * density)
        drawBody(canvas, w, h, tail = false, mouthOpen = false, ears = false)
    }

    private fun drawJump(canvas: Canvas, w: Float, h: Float, progress: Float) {
        canvas.translate(0f, -progress * 40f * density)
        drawBody(canvas, w, h, tail = true, mouthOpen = true, ears = true)
    }

    private fun drawCarry(canvas: Canvas, w: Float, h: Float, progress: Float) {
        val bounce = sin(progress * PI.toFloat()).coerceAtLeast(0f)
        canvas.translate(0f, -bounce * 60f * density)
        drawBody(canvas, w, h, tail = true, mouthOpen = true, ears = true)
        // duck in mouth
        paint.color = 0xFF884400.toInt()
        canvas.drawOval(w * 0.30f, -h * 0.65f, w * 0.85f, -h * 0.35f, paint)
        canvas.drawOval(w * 0.30f, -h * 0.65f, w * 0.85f, -h * 0.35f, outline)
        paint.color = 0xFF007700.toInt()
        canvas.drawCircle(w * 0.78f, -h * 0.55f, h * 0.10f, paint)
    }

    private fun drawLaugh(canvas: Canvas, w: Float, h: Float, progress: Float) {
        val bob = sin(progress * PI.toFloat() * 4f) * 4f * density
        canvas.translate(0f, bob)
        drawHeadOnly(canvas, w * 0.85f, h * 0.7f, eyesClosed = true, mouthOpen = true)
    }

    private fun drawFlinch(canvas: Canvas, w: Float, h: Float, progress: Float) {
        val shake = sin(progress * PI.toFloat() * 12f) * 6f * density
        canvas.translate(shake, 0f)
        drawHeadOnly(canvas, w * 0.85f, h * 0.7f, eyesClosed = false, mouthOpen = true)
    }

    private fun drawBody(canvas: Canvas, w: Float, h: Float, tail: Boolean, mouthOpen: Boolean, ears: Boolean) {
        // body
        paint.color = BODY
        canvas.drawRect(-w * 0.40f, -h * 0.30f, w * 0.30f, h * 0.40f, paint)
        canvas.drawRect(-w * 0.40f, -h * 0.30f, w * 0.30f, h * 0.40f, outline)

        // back
        paint.color = BODY_DARK
        canvas.drawRect(-w * 0.40f, -h * 0.30f, w * 0.30f, -h * 0.10f, paint)

        // legs (just two visible stubs)
        paint.color = BODY
        canvas.drawRect(-w * 0.30f, h * 0.30f, -w * 0.10f, h * 0.55f, paint)
        canvas.drawRect(w * 0.05f, h * 0.30f, w * 0.25f, h * 0.55f, paint)
        canvas.drawRect(-w * 0.30f, h * 0.30f, -w * 0.10f, h * 0.55f, outline)
        canvas.drawRect(w * 0.05f, h * 0.30f, w * 0.25f, h * 0.55f, outline)

        // tail
        if (tail) {
            paint.color = BODY
            canvas.drawRect(-w * 0.55f, -h * 0.20f, -w * 0.40f, h * 0.05f, paint)
            canvas.drawRect(-w * 0.55f, -h * 0.20f, -w * 0.40f, h * 0.05f, outline)
        }

        // head
        drawHeadAttached(canvas, w, h, mouthOpen, ears)
    }

    private fun drawHeadAttached(canvas: Canvas, w: Float, h: Float, mouthOpen: Boolean, ears: Boolean) {
        // head
        paint.color = BODY
        canvas.drawRect(w * 0.18f, -h * 0.65f, w * 0.55f, -h * 0.20f, paint)
        canvas.drawRect(w * 0.18f, -h * 0.65f, w * 0.55f, -h * 0.20f, outline)

        // ear
        paint.color = BODY_DARK
        val earTopOffset = if (ears) -h * 0.20f else 0f
        canvas.drawRect(w * 0.16f, -h * 0.85f + earTopOffset, w * 0.30f, -h * 0.40f + earTopOffset, paint)
        canvas.drawRect(w * 0.16f, -h * 0.85f + earTopOffset, w * 0.30f, -h * 0.40f + earTopOffset, outline)

        // snout
        paint.color = SNOUT
        canvas.drawRect(w * 0.50f, -h * 0.50f, w * 0.75f, -h * 0.25f, paint)
        canvas.drawRect(w * 0.50f, -h * 0.50f, w * 0.75f, -h * 0.25f, outline)

        // nose
        paint.color = 0xFF000000.toInt()
        canvas.drawCircle(w * 0.72f, -h * 0.40f, h * 0.06f, paint)

        // mouth
        paint.color = if (mouthOpen) 0xFF441111.toInt() else 0xFF222222.toInt()
        canvas.drawRect(w * 0.45f, -h * 0.30f, w * 0.65f, -h * 0.22f, paint)

        // eye
        paint.color = 0xFFFFFFFF.toInt()
        canvas.drawCircle(w * 0.34f, -h * 0.48f, h * 0.07f, paint)
        paint.color = 0xFF000000.toInt()
        canvas.drawCircle(w * 0.35f, -h * 0.47f, h * 0.035f, paint)
    }

    private fun drawHeadOnly(canvas: Canvas, w: Float, h: Float, eyesClosed: Boolean, mouthOpen: Boolean) {
        // head pops above grass: only top half visible
        paint.color = BODY
        canvas.drawRect(-w * 0.30f, -h, w * 0.30f, 0f, paint)
        canvas.drawRect(-w * 0.30f, -h, w * 0.30f, 0f, outline)

        // ears
        paint.color = BODY_DARK
        canvas.drawRect(-w * 0.38f, -h * 1.30f, -w * 0.10f, -h * 0.70f, paint)
        canvas.drawRect(w * 0.10f, -h * 1.30f, w * 0.38f, -h * 0.70f, paint)
        canvas.drawRect(-w * 0.38f, -h * 1.30f, -w * 0.10f, -h * 0.70f, outline)
        canvas.drawRect(w * 0.10f, -h * 1.30f, w * 0.38f, -h * 0.70f, outline)

        // eyes
        if (eyesClosed) {
            paint.color = 0xFF000000.toInt()
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f * density
            canvas.drawLine(-w * 0.18f, -h * 0.55f, -w * 0.04f, -h * 0.45f, paint)
            canvas.drawLine(-w * 0.04f, -h * 0.55f, -w * 0.18f, -h * 0.45f, paint)
            canvas.drawLine(w * 0.04f, -h * 0.55f, w * 0.18f, -h * 0.45f, paint)
            canvas.drawLine(w * 0.18f, -h * 0.55f, w * 0.04f, -h * 0.45f, paint)
            paint.style = Paint.Style.FILL
        } else {
            paint.color = 0xFFFFFFFF.toInt()
            canvas.drawCircle(-w * 0.12f, -h * 0.50f, w * 0.06f, paint)
            canvas.drawCircle(w * 0.12f, -h * 0.50f, w * 0.06f, paint)
            paint.color = 0xFF000000.toInt()
            canvas.drawCircle(-w * 0.12f, -h * 0.50f, w * 0.03f, paint)
            canvas.drawCircle(w * 0.12f, -h * 0.50f, w * 0.03f, paint)
        }

        // mouth
        paint.color = if (mouthOpen) 0xFF441111.toInt() else 0xFF222222.toInt()
        canvas.drawRect(-w * 0.18f, -h * 0.20f, w * 0.18f, -h * 0.05f, paint)

        // snout / nose
        paint.color = SNOUT
        canvas.drawRect(-w * 0.10f, -h * 0.35f, w * 0.10f, -h * 0.20f, paint)
    }

    companion object {
        private const val BODY: Int = 0xFFC68A4F.toInt()
        private const val BODY_DARK: Int = 0xFF884400.toInt()
        private const val SNOUT: Int = 0xFFE2B988.toInt()
    }
}
