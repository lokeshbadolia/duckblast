package com.duckblast.game.game.renderer

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import com.duckblast.game.game.engine.GameEngine
import com.duckblast.game.game.engine.GameState
import com.duckblast.game.game.entities.DogMode
import com.duckblast.game.game.entities.Duck
import com.duckblast.game.game.entities.Plate

/**
 * Top-level renderer. Walks the engine's state once per frame and asks each
 * sub-renderer to draw its slice of the scene in z-order.
 */
class GameRenderer(
    context: Context,
    private val engine: GameEngine
) {
    private val density: Float = context.resources.displayMetrics.density
    private val sprites = SpriteManager(density)
    private val background = BackgroundRenderer(density)
    private val ducks = DuckRenderer(density)
    private val dog = DogRenderer(density)
    private val crosshair = CrosshairRenderer(density)
    private val gun = GunRenderer(density)
    private val hud = HudRenderer(sprites, density)
    private val particles = ParticleRenderer(sprites, density)

    private val clearPaint = Paint().apply {
        isAntiAlias = false
        color = 0xFF5C94FC.toInt()
    }
    private val overlayPaint = Paint().apply {
        isAntiAlias = false
        typeface = sprites.pixelTypeface
        textAlign = Paint.Align.CENTER
    }

    fun render(canvas: Canvas) {
        canvas.drawColor(0xFF5C94FC.toInt())
        background.draw(canvas, engine.screenWidthPx, engine.screenHeightPx)

        val state = engine.state
        val dogMode = engine.dog.mode

        // Dog under targets when running on the ground
        if (engine.dog.visible && dogMode in groundLevelDog) {
            dog.draw(canvas, engine.dog)
        }

        // Targets
        for (target in engine.targets) {
            when (target) {
                is Duck -> ducks.drawDuck(canvas, target)
                is Plate -> ducks.drawPlate(canvas, target)
            }
        }

        // Shards & score popups (sit between targets and gun)
        for (shard in engine.plateShards) particles.drawShard(canvas, shard)

        // Dog on top of play area when popping up
        if (engine.dog.visible && dogMode in popUpDog) {
            dog.draw(canvas, engine.dog)
        }

        // Score popups
        for (popup in engine.scorePopups) particles.drawScorePopup(canvas, popup)

        // Gun
        gun.draw(canvas, engine.gun)

        // Crosshair — visible during anything but Idle / GameOver
        if (state != GameState.Idle && state != GameState.GameOver) {
            crosshair.draw(canvas, engine.crosshair)
        }

        // HUD always last
        hud.draw(canvas, engine)

        // Optional state overlays
        when (state) {
            is GameState.RoundResult -> {
                if (state.hitsThisRound == state.targetsThisRound && state.targetsThisRound > 0) {
                    drawCenterText(canvas, "PERFECT!", 0xFFFCFC00.toInt())
                } else if (state.hitsThisRound == 0) {
                    drawCenterText(canvas, "YOU MISSED!", 0xFFD03030.toInt())
                }
            }
            is GameState.LevelClear -> {
                drawCenterText(canvas, "LEVEL ${state.level} CLEAR", 0xFFFCFC00.toInt())
            }
            GameState.GameOver -> {
                drawCenterText(canvas, "GAME OVER", 0xFFD03030.toInt())
            }
            GameState.Paused -> {
                drawDimOverlay(canvas)
                drawCenterText(canvas, "PAUSED", 0xFFFFFFFF.toInt())
            }
            else -> Unit
        }
    }

    private fun drawCenterText(canvas: Canvas, label: String, color: Int) {
        overlayPaint.color = color
        overlayPaint.textSize = 32f * density
        val cx = engine.screenWidthPx / 2f
        val cy = engine.screenHeightPx * 0.32f
        // shadow
        overlayPaint.color = 0xFF000033.toInt()
        canvas.drawText(label, cx + 3f * density, cy + 3f * density, overlayPaint)
        overlayPaint.color = color
        canvas.drawText(label, cx, cy, overlayPaint)
    }

    private fun drawDimOverlay(canvas: Canvas) {
        clearPaint.color = 0x99000033.toInt()
        canvas.drawRect(0f, 0f, engine.screenWidthPx, engine.screenHeightPx, clearPaint)
        clearPaint.color = 0xFF5C94FC.toInt()
    }

    fun recycle() {
        background.recycle()
    }

    companion object {
        private val groundLevelDog = setOf(
            DogMode.INTRO_RUN, DogMode.INTRO_SNIFF, DogMode.INTRO_JUMP, DogMode.FLINCH
        )
        private val popUpDog = setOf(DogMode.SUCCESS_CARRY, DogMode.LAUGH)
    }
}
