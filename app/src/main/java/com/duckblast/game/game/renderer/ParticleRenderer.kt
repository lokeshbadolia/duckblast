package com.duckblast.game.game.renderer

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.duckblast.game.game.entities.PlateShard
import com.duckblast.game.game.entities.ScorePopup

class ParticleRenderer(private val sprites: SpriteManager, private val density: Float) {

    private val text = Paint().apply {
        isAntiAlias = false
        typeface = sprites.pixelTypeface
        textAlign = Paint.Align.CENTER
    }
    private val textShadow = Paint().apply {
        isAntiAlias = false
        typeface = sprites.pixelTypeface
        textAlign = Paint.Align.CENTER
        color = 0xFF000033.toInt()
    }
    private val shard = Paint().apply { isAntiAlias = false }
    private val shardOutline = Paint().apply {
        isAntiAlias = false
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        color = 0xFF000033.toInt()
    }
    private val tmpRect = RectF()

    fun drawScorePopup(canvas: Canvas, p: ScorePopup) {
        val size = 16f * density
        text.textSize = size
        textShadow.textSize = size
        val alpha = (p.alpha * 255f).toInt().coerceIn(0, 255)
        textShadow.color = (alpha shl 24) or 0x00000033
        text.color = (alpha shl 24) or (p.color and 0x00FFFFFF)
        canvas.drawText(p.text, p.x + 2f * density, p.y + 2f * density, textShadow)
        canvas.drawText(p.text, p.x, p.y, text)
    }

    fun drawShard(canvas: Canvas, s: PlateShard) {
        canvas.save()
        canvas.translate(s.x, s.y)
        canvas.rotate(s.rotationDeg)
        val alpha = (s.alpha * 255f).toInt().coerceIn(0, 255)
        shard.color = (alpha shl 24) or SHARD_RGB
        val r = 22f * density
        tmpRect.set(-r, -r, r, r)
        val start = if (s.orientation == 0) 90f else 270f
        canvas.drawArc(tmpRect, start, 180f, true, shard)
        canvas.drawArc(tmpRect, start, 180f, true, shardOutline)
        canvas.restore()
    }

    companion object {
        private const val SHARD_RGB: Int = 0x00B87333
    }
}
