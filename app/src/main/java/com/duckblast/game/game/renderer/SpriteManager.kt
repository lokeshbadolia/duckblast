package com.duckblast.game.game.renderer

import android.graphics.Paint
import android.graphics.Typeface

/**
 * Shared rendering state: pixel typeface, master density, reusable bitmap
 * paint that disables filtering so blits stay sharp.
 */
class SpriteManager(val density: Float) {

    val pixelTypeface: Typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)

    val bitmapBlit: Paint = Paint().apply {
        isAntiAlias = false
        isFilterBitmap = false
        isDither = false
    }
}
