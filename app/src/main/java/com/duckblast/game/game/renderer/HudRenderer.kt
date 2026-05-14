package com.duckblast.game.game.renderer

import android.graphics.Canvas
import android.graphics.Paint
import com.duckblast.game.game.engine.GameEngine

class HudRenderer(private val sprites: SpriteManager, private val density: Float) {

    private val barBg = Paint().apply {
        isAntiAlias = false
        color = 0xFF000033.toInt()
    }
    private val topStripBg = Paint().apply {
        isAntiAlias = false
        color = 0xCC000033.toInt()
    }
    private val labelText = Paint().apply {
        isAntiAlias = false
        typeface = sprites.pixelTypeface
        color = 0xFFFFFFFF.toInt()
    }
    private val numberText = Paint().apply {
        isAntiAlias = false
        typeface = sprites.pixelTypeface
        color = 0xFFFCFC00.toInt()
    }
    private val iconFill = Paint().apply {
        isAntiAlias = false
        color = 0xFFFCFC00.toInt()
    }
    private val iconOutline = Paint().apply {
        isAntiAlias = false
        style = Paint.Style.STROKE
        color = 0xFFFFFFFF.toInt()
        strokeWidth = 1.5f * density
    }
    private val miniDuckFill = Paint().apply {
        isAntiAlias = false
        color = 0xFFFFFFFF.toInt()
    }
    private val miniDuckOutline = Paint().apply {
        isAntiAlias = false
        style = Paint.Style.STROKE
        color = 0xFFFFFFFF.toInt()
        strokeWidth = 1.5f * density
    }

    fun draw(canvas: Canvas, engine: GameEngine) {
        val w = engine.screenWidthPx
        val h = engine.screenHeightPx
        val density = density

        // ---------- TOP STRIP ----------
        val topH = h * GameEngine.TOP_BAR_FRACTION
        canvas.drawRect(0f, 0f, w, topH, topStripBg)
        labelText.textSize = 12f * density
        labelText.textAlign = Paint.Align.CENTER
        canvas.drawText(
            "HI-SCORE  ${formatNumber(engine.hiScore)}",
            w * 0.5f,
            topH * 0.65f,
            labelText
        )

        // ---------- BOTTOM STRIP ----------
        val hudH = h * GameEngine.HUD_FRACTION
        val hudTop = h - hudH
        canvas.drawRect(0f, hudTop, w, h, barBg)

        val pad = 16f * density
        val labelSize = 11f * density
        val numberSize = 22f * density

        // SCORE block (left)
        labelText.textSize = labelSize
        labelText.textAlign = Paint.Align.LEFT
        canvas.drawText("SCORE", pad, hudTop + 18f * density, labelText)
        numberText.textSize = numberSize
        numberText.textAlign = Paint.Align.LEFT
        canvas.drawText(formatNumber(engine.score), pad, hudTop + 44f * density, numberText)

        // Ammo bullets
        val ammoY = hudTop + 64f * density
        val config = engine.currentConfig
        for (i in 0 until config.shotsPerRound) {
            val cx = pad + i * 14f * density + 6f * density
            val top = ammoY
            val bottom = ammoY + 14f * density
            if (i < engine.shotsRemaining) {
                canvas.drawRect(cx - 3f * density, top, cx + 3f * density, bottom, iconFill)
            } else {
                canvas.drawRect(cx - 3f * density, top, cx + 3f * density, bottom, iconOutline)
            }
        }

        // Center: round duck-icon counter
        val perRound = config.ducksPerRound
        val counterY = hudTop + 28f * density
        val iconStep = 16f * density
        val totalWidth = perRound * iconStep
        val counterStartX = (w - totalWidth) / 2f + iconStep / 2f
        for (i in 0 until perRound) {
            val cx = counterStartX + i * iconStep
            if (i < engine.hitsThisRound) {
                drawDuckIcon(canvas, cx, counterY, density * 6f, miniDuckFill, filled = true)
            } else {
                drawDuckIcon(canvas, cx, counterY, density * 6f, miniDuckOutline, filled = false)
            }
        }
        // Round x/y under counter
        labelText.textSize = 9f * density
        labelText.textAlign = Paint.Align.CENTER
        canvas.drawText(
            "ROUND ${engine.roundIndex.coerceAtMost(config.roundsPerLevel - 1) + 1}/${config.roundsPerLevel}",
            w * 0.5f,
            hudTop + 50f * density,
            labelText
        )
        // Pass status
        canvas.drawText(
            "PASS ${engine.hitsThisLevel}/${config.ducksToPass}",
            w * 0.5f,
            hudTop + 68f * density,
            labelText
        )

        // LEVEL block (right)
        labelText.textSize = labelSize
        labelText.textAlign = Paint.Align.RIGHT
        canvas.drawText("LEVEL", w - pad, hudTop + 18f * density, labelText)
        numberText.textAlign = Paint.Align.RIGHT
        canvas.drawText(engine.currentLevel.toString(), w - pad, hudTop + 44f * density, numberText)
        labelText.textSize = 9f * density
        canvas.drawText(
            if (config.mode == com.duckblast.game.game.level.GameMode.PLATE) "PLATE" else "DUCK",
            w - pad,
            hudTop + 62f * density,
            labelText
        )

        // reset alignments for next pass
        labelText.textAlign = Paint.Align.LEFT
        numberText.textAlign = Paint.Align.LEFT
    }

    private fun drawDuckIcon(canvas: Canvas, cx: Float, cy: Float, r: Float, paint: Paint, filled: Boolean) {
        // Pixel duck silhouette — body oval + small head dot
        if (filled) {
            canvas.drawOval(cx - r, cy - r * 0.6f, cx + r, cy + r * 0.6f, paint)
            canvas.drawCircle(cx + r * 0.7f, cy - r * 0.3f, r * 0.35f, paint)
        } else {
            canvas.drawOval(cx - r, cy - r * 0.6f, cx + r, cy + r * 0.6f, paint)
            canvas.drawCircle(cx + r * 0.7f, cy - r * 0.3f, r * 0.35f, paint)
        }
    }

    private fun formatNumber(value: Long): String {
        // Plain decimal — pixel HUDs look better without comma grouping
        return value.toString().padStart(6, '0')
    }
}
