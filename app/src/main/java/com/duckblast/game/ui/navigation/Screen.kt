package com.duckblast.game.ui.navigation

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Profile : Screen("profile")
    data object Menu : Screen("menu")
    data object Game : Screen("game?multiplayer={multiplayer}&seed={seed}") {
        const val ARG_MULTIPLAYER = "multiplayer"
        const val ARG_SEED = "seed"
        fun build(multiplayer: Boolean = false, seed: Long = 0L): String =
            "game?multiplayer=$multiplayer&seed=$seed"
    }
    data object GameOver : Screen(
        "gameover/{score}/{level}/{ducksHit}/{shotsFired}/{durationSec}/{perfect}/{previousBest}"
    ) {
        const val ARG_SCORE = "score"
        const val ARG_LEVEL = "level"
        const val ARG_DUCKS_HIT = "ducksHit"
        const val ARG_SHOTS_FIRED = "shotsFired"
        const val ARG_DURATION = "durationSec"
        const val ARG_PERFECT = "perfect"
        const val ARG_PREVIOUS_BEST = "previousBest"
        fun build(
            score: Long,
            level: Int,
            ducksHit: Int,
            shotsFired: Int,
            durationSec: Int,
            perfect: Boolean,
            previousBest: Long
        ): String = "gameover/$score/$level/$ducksHit/$shotsFired/$durationSec/$perfect/$previousBest"
    }
    data object Scoreboard : Screen("scoreboard")
    data object Lobby : Screen("lobby")
}
