package com.duckblast.game.game.entities

enum class DogMode {
    HIDDEN,
    INTRO_RUN,
    INTRO_SNIFF,
    INTRO_JUMP,
    SUCCESS_CARRY,
    LAUGH,
    FLINCH        // easter-egg reaction when player shoots the dog
}

/**
 * Hunting-dog companion. Its job is purely cosmetic — it animates during
 * round intros, post-round results, and the laugh-at-the-player easter egg.
 */
class DuckDog {

    var x: Float = 0f
    var y: Float = 0f

    var mode: DogMode = DogMode.HIDDEN
        private set
    var modeTimer: Float = 0f
        private set
    var modeDuration: Float = 0f
        private set

    val progress: Float
        get() = if (modeDuration > 0f) (modeTimer / modeDuration).coerceIn(0f, 1f) else 1f

    val visible: Boolean
        get() = mode != DogMode.HIDDEN

    fun setMode(newMode: DogMode, duration: Float) {
        mode = newMode
        modeTimer = 0f
        modeDuration = duration
    }

    fun update(dt: Float) {
        if (mode == DogMode.HIDDEN) return
        modeTimer += dt
        if (modeTimer >= modeDuration) {
            mode = DogMode.HIDDEN
            modeTimer = 0f
            modeDuration = 0f
        }
    }

    fun isLaughing(): Boolean = mode == DogMode.LAUGH
}
