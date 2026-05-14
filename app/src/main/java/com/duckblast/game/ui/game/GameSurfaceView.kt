package com.duckblast.game.ui.game

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.duckblast.game.game.engine.GameEngine
import com.duckblast.game.game.engine.GameLoop
import com.duckblast.game.game.renderer.GameRenderer

/**
 * Hosts the game's [SurfaceHolder] and drives the [GameLoop]. The actual game
 * state lives on the [GameViewModel] — this view only forwards size + touch
 * input and owns the per-surface renderer + loop.
 */
class GameSurfaceView(
    context: Context,
    private val viewModel: GameViewModel
) : SurfaceView(context), SurfaceHolder.Callback {

    private val engine: GameEngine = viewModel.engine
    private val renderer: GameRenderer = GameRenderer(context, engine)
    private val loop: GameLoop = GameLoop(holder, ::onUpdate, ::onRender)

    init {
        holder.addCallback(this)
        isFocusable = true
        isFocusableInTouchMode = true
        keepScreenOn = true
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // surfaceChanged will fire next with actual dimensions; start the
        // loop there so we never tick a zero-size canvas.
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        val density = context.resources.displayMetrics.density
        engine.setSize(width.toFloat(), height.toFloat(), density)
        viewModel.onSurfaceReady()
        if (!loop.isRunning()) loop.start()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        loop.stop()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN,
            MotionEvent.ACTION_MOVE -> engine.onTouchMove(event.x, event.y)
            MotionEvent.ACTION_UP -> engine.onTouchUp(event.x, event.y)
            MotionEvent.ACTION_CANCEL -> Unit
        }
        return true
    }

    private fun onUpdate(dt: Float) {
        engine.update(dt)
    }

    private fun onRender(canvas: Canvas) {
        renderer.render(canvas)
    }

    fun recycleRenderer() {
        renderer.recycle()
    }
}
