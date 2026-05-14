package com.duckblast.game.game.level

/**
 * Hand-tuned progression curve for levels 1..30. Levels past 30 loop back to
 * the 21..30 band so the game can run indefinitely.
 *
 * Every third level (3, 6, 9, …) flips to clay-plate mode.
 */
object LevelProgressionTable {

    const val MAX_LEVEL = 30
    const val LOOP_FROM = 21
    const val LOOP_TO = 30
    const val MAX_SPEED_DP = 400f

    fun configFor(level: Int): LevelConfig {
        val clamped = level.coerceAtLeast(1)
        val effective = if (clamped <= LOOP_TO) clamped
            else LOOP_FROM + ((clamped - LOOP_FROM) % (LOOP_TO - LOOP_FROM + 1))
        val isPlate = effective % 3 == 0

        val base = when {
            effective in 1..3 -> LevelConfig(
                level = clamped,
                mode = GameMode.DUCK,
                ducksPerRound = 1,
                duckSpeed = 140f,
                shotsPerRound = 3,
                ducksToPass = 2,
                pointsPerDuck = 100,
                bonusPoints = 300,
                timePerDuck = 5f
            )
            effective in 4..6 -> LevelConfig(
                level = clamped,
                mode = GameMode.DUCK,
                ducksPerRound = 2,
                duckSpeed = 200f,
                shotsPerRound = 3,
                ducksToPass = 3,
                pointsPerDuck = 200,
                bonusPoints = 500,
                timePerDuck = 4.5f
            )
            effective in 7..10 -> LevelConfig(
                level = clamped,
                mode = GameMode.DUCK,
                ducksPerRound = 2,
                duckSpeed = 260f,
                shotsPerRound = 3,
                ducksToPass = 4,
                pointsPerDuck = 250,
                bonusPoints = 800,
                timePerDuck = 4f
            )
            effective in 11..20 -> {
                val tier = (effective - 11) / 3
                val speed = (280f + tier * 20f).coerceAtMost(MAX_SPEED_DP)
                val time = (3.8f - tier * 0.2f).coerceAtLeast(2.8f)
                LevelConfig(
                    level = clamped,
                    mode = GameMode.DUCK,
                    ducksPerRound = 2,
                    duckSpeed = speed,
                    shotsPerRound = 3,
                    ducksToPass = 4,
                    pointsPerDuck = 300,
                    bonusPoints = 1000,
                    timePerDuck = time
                )
            }
            else -> LevelConfig(
                level = clamped,
                mode = GameMode.DUCK,
                ducksPerRound = 2,
                duckSpeed = MAX_SPEED_DP,
                shotsPerRound = 3,
                ducksToPass = 5,
                pointsPerDuck = 400,
                bonusPoints = 1500,
                timePerDuck = 2.5f
            )
        }
        return if (isPlate) base.copy(mode = GameMode.PLATE) else base
    }
}
