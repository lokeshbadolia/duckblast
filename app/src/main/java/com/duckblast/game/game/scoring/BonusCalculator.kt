package com.duckblast.game.game.scoring

import com.duckblast.game.game.level.LevelConfig

object BonusCalculator {

    const val EASTER_EGG_DOG_SHOT = 50

    /** Awarded each time the player clears a full round without a miss. */
    fun perfectRoundBonus(config: LevelConfig): Int = config.bonusPoints / 3

    /** Awarded when the player passes the level. Doubled on a flawless run. */
    fun levelClearBonus(config: LevelConfig, totalHits: Int): Int {
        val perfect = totalHits >= config.targetsPerLevel
        return if (perfect) config.bonusPoints * 2 else config.bonusPoints
    }
}
