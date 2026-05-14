package com.duckblast.game.game.renderer

import android.graphics.Canvas
import android.graphics.Paint
import com.duckblast.game.game.entities.Crosshair

class CrosshairRenderer(private val density: Float) {

    private val stroke = Paint().apply {
        isAntiAlias = false
        style = Paint.Style.STROKE
        color = 0xFFFFFFFF.toInt()
        strokeWidth = 2f * density
    }
    private val outline = Paint().apply {
        isAntiAlias = false
        style = Paint.Style.STROKE
        color = 0xFF000033.toInt()
        strokeWidth = 4f * density
    }
    private val dotFill = Paint().apply {
        isAntiAlias = false
        color = 0xFFFFFFFF.toInt()
    }
    private val dotOutline = Paint().apply {
        isAntiAlias = false
        color = 0xFF000033.toInt()
    }

    fun draw(canvas: Canvas, crosshair: Crosshair) {
        val cx = crosshair.x
        val cy = crosshair.y
        val r = crosshair.radius
        val tick = r * 1.0f
        val gap = r * 0.30f

        // Dark outline ring first, then white ring on top
        canvas.drawCircle(cx, cy, r, outline)
        canvas.drawCircle(cx, cy, r, stroke)

        // 4 perpendicular ticks
        canvas.drawLine(cx, cy - r - gap - tick, cx, cy - r - gap, outline)
        canvas.drawLine(cx, cy - r - gap - tick, cx, cy - r - gap, stroke)
        canvas.drawLine(cx, cy + r + gap, cx, cy + r + gap + tick, outline)
        canvas.drawLine(cx, cy + r + gap, cx, cy + r + gap + tick, stroke)
        canvas.drawLine(cx - r - gap - tick, cy, cx - r - gap, cy, outline)
        canvas.drawLine(cx - r - gap - tick, cy, cx - r - gap, cy, stroke)
        canvas.drawLine(cx + r + gap, cy, cx + r + gap + tick, cy, outline)
        canvas.drawLine(cx + r + gap, cy, cx + r + gap + tick, cy, stroke)

        // Center dot with outline
        canvas.drawCircle(cx, cy, 4f * density, dotOutline)
        canvas.drawCircle(cx, cy, 3f * density, dotFill)
    }
}
