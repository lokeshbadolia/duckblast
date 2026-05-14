package com.duckblast.game.game.renderer

import android.graphics.Canvas
import android.graphics.Paint
import com.duckblast.game.game.entities.Gun

class GunRenderer(private val density: Float) {

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

    fun draw(canvas: Canvas, gun: Gun) {
        canvas.save()
        canvas.translate(gun.x, gun.y)
        canvas.rotate(gun.currentAngleDeg)
        canvas.translate(gun.recoilOffset, 0f)

        val len = 80f * density
        val barrelH = 14f * density
        val stockH = 28f * density

        // Stock (chunky end)
        paint.color = STOCK_COLOR
        canvas.drawRect(-len * 0.55f, -stockH * 0.5f, -len * 0.10f, stockH * 0.5f, paint)
        canvas.drawRect(-len * 0.55f, -stockH * 0.5f, -len * 0.10f, stockH * 0.5f, outline)

        // Receiver
        paint.color = RECEIVER_COLOR
        canvas.drawRect(-len * 0.10f, -stockH * 0.4f, len * 0.10f, stockH * 0.4f, paint)
        canvas.drawRect(-len * 0.10f, -stockH * 0.4f, len * 0.10f, stockH * 0.4f, outline)

        // Barrel
        paint.color = BARREL_COLOR
        canvas.drawRect(len * 0.10f, -barrelH * 0.5f, len * 0.55f, barrelH * 0.5f, paint)
        canvas.drawRect(len * 0.10f, -barrelH * 0.5f, len * 0.55f, barrelH * 0.5f, outline)

        // Sight bead
        paint.color = 0xFF000033.toInt()
        canvas.drawRect(len * 0.50f, -barrelH * 0.70f, len * 0.55f, -barrelH * 0.50f, paint)

        // Muzzle flash
        val flash = gun.muzzleFlashAlpha
        if (flash > 0f) {
            val a = (flash * 255f).toInt().coerceIn(0, 255)
            paint.color = (a shl 24) or FLASH_RGB
            canvas.drawCircle(len * 0.55f + 6f * density, 0f, 12f * density, paint)
            paint.color = (a shl 24) or INNER_FLASH_RGB
            canvas.drawCircle(len * 0.55f + 6f * density, 0f, 6f * density, paint)
        }

        canvas.restore()
    }

    companion object {
        private const val STOCK_COLOR: Int = 0xFF553300.toInt()
        private const val RECEIVER_COLOR: Int = 0xFF222222.toInt()
        private const val BARREL_COLOR: Int = 0xFF333333.toInt()
        private const val FLASH_RGB: Int = 0x00FCFC00
        private const val INNER_FLASH_RGB: Int = 0x00FFFFFF
    }
}
