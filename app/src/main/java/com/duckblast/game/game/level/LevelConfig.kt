package com.duckblast.game.game.level

enum class GameMode { DUCK, PLATE }

/**
 * All distances and speeds are expressed in dp; the engine multiplies by the
 * runtime density to convert to pixels.
 */
data class LevelConfig(
    val level: Int,
    val mode: GameMode,
    val ducksPerRound: Int,        // targets launched simultaneously each round
    val duckSpeed: Float,          // horizontal speed in dp/s
    val shotsPerRound: Int,        // typically 3
    val ducksToPass: Int,          // total hits needed across the level to advance
    val pointsPerDuck: Int,        // base score per hit
    val bonusPoints: Int,          // perfect-round / level-clear bonus
    val timePerDuck: Float,        // seconds before a duck reaches the top
    val roundsPerLevel: Int = 3
) {
    val targetsPerLevel: Int
        get() = ducksPerRound * roundsPerLevel
}
