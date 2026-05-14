package com.duckblast.game.haptics

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

enum class VibrationType {
    GUNSHOT,
    HIT_TARGET,
    MISS,
    DOG_APPEAR,
    LEVEL_CLEAR,
    GAME_OVER
}

class VibrationManager(context: Context) {

    private val appContext = context.applicationContext

    @Volatile var enabled: Boolean = true

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = appContext.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            appContext.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun vibrate(type: VibrationType) {
        if (!enabled) return
        val v = vibrator?.takeIf { it.hasVibrator() } ?: return
        val effect: VibrationEffect = when (type) {
            VibrationType.GUNSHOT -> VibrationEffect.createOneShot(40, DEFAULT_AMPLITUDE)
            VibrationType.HIT_TARGET -> VibrationEffect.createWaveform(
                longArrayOf(0, 20, 20, 20), -1
            )
            VibrationType.MISS -> return            // no haptic on miss
            VibrationType.DOG_APPEAR -> VibrationEffect.createOneShot(60, 80)
            VibrationType.LEVEL_CLEAR -> VibrationEffect.createWaveform(
                longArrayOf(0, 30, 40, 30, 40, 60), -1
            )
            VibrationType.GAME_OVER -> VibrationEffect.createOneShot(200, DEFAULT_AMPLITUDE)
        }
        runCatching { v.vibrate(effect) }
    }

    fun cancel() {
        runCatching { vibrator?.cancel() }
    }

    companion object {
        private const val DEFAULT_AMPLITUDE = VibrationEffect.DEFAULT_AMPLITUDE
    }
}
