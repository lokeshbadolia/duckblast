package com.duckblast.game.game.renderer

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader

/**
 * Pre-bakes the layered backdrop (sky → trees → hills → grass → bushes →
 * ground) into a Bitmap whenever the canvas size changes; per-frame the
 * bitmap is blitted with anti-aliasing disabled to keep the pixel look.
 */
class BackgroundRenderer(private val density: Float) {

    private var cached: Bitmap? = null
    private var cachedW: Int = 0
    private var cachedH: Int = 0
    private val blit = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
        isDither = false
    }

    fun draw(canvas: Canvas, widthPx: Float, heightPx: Float) {
        val w = widthPx.toInt().coerceAtLeast(1)
        val h = heightPx.toInt().coerceAtLeast(1)
        val cache = cached
        if (cache == null || cachedW != w || cachedH != h) {
            cached?.recycle()
            cached = buildBitmap(w, h)
            cachedW = w
            cachedH = h
        }
        canvas.drawBitmap(cached!!, 0f, 0f, blit)
    }

    private fun buildBitmap(w: Int, h: Int): Bitmap {
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint().apply { isAntiAlias = false; isFilterBitmap = false }

        // Sky gradient
        p.shader = LinearGradient(0f, 0f, 0f, h.toFloat(),
            SKY_TOP, SKY_BOTTOM, Shader.TileMode.CLAMP)
        c.drawRect(0f, 0f, w.toFloat(), h.toFloat(), p)
        p.shader = null

        // Distant tree silhouettes (chunky rectangles, ~60% screen height)
        p.color = TREE_DARK
        val treeBandTop = h * 0.58f
        val treeBandH = h * 0.10f
        val treeStep = w / 5f
        var t = -treeStep * 0.3f
        var idx = 0
        while (t < w + treeStep) {
            val tall = idx % 2 == 0
            val rectH = if (tall) treeBandH else treeBandH * 0.65f
            c.drawRect(t, treeBandTop + (treeBandH - rectH), t + treeStep * 0.55f, treeBandTop + treeBandH, p)
            t += treeStep * 0.45f
            idx += 1
        }

        // Rolling hills (two big ovals at ~70% screen height)
        p.color = GRASS_DARK
        c.drawOval(RectF(-w * 0.1f, h * 0.66f, w * 0.55f, h * 0.86f), p)
        c.drawOval(RectF(w * 0.45f, h * 0.68f, w * 1.10f, h * 0.88f), p)

        // Foreground grass strip
        p.color = GRASS_LIGHT
        c.drawRect(0f, h * 0.78f, w.toFloat(), h * 0.85f, p)

        // Bush clusters along grass line
        p.color = BUSH_GREEN
        val bushY = h * 0.81f
        val bushR = w * 0.05f
        var bx = -bushR
        var bIdx = 0
        while (bx < w + bushR) {
            c.drawCircle(bx, bushY, bushR, p)
            c.drawCircle(bx + bushR * 0.6f, bushY + bushR * 0.1f, bushR * 0.75f, p)
            bx += bushR * 1.9f
            bIdx += 1
        }

        // Ground strip
        p.color = GROUND_BROWN
        c.drawRect(0f, h * 0.85f, w.toFloat(), h.toFloat(), p)

        return bmp
    }

    fun recycle() {
        cached?.recycle()
        cached = null
        cachedW = 0
        cachedH = 0
    }

    companion object {
        private const val SKY_TOP: Int = 0xFF5C94FC.toInt()
        private const val SKY_BOTTOM: Int = 0xFFA8D8EA.toInt()
        private const val GRASS_DARK: Int = 0xFF009900.toInt()
        private const val GRASS_LIGHT: Int = 0xFF00CC00.toInt()
        private const val BUSH_GREEN: Int = 0xFF006600.toInt()
        private const val TREE_DARK: Int = 0xFF003300.toInt()
        private const val GROUND_BROWN: Int = 0xFF884400.toInt()
    }
}
