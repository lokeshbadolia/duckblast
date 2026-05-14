package com.duckblast.game.game.engine

/**
 * High-level state machine emitted by the engine. Renderer + ViewModel switch
 * on this to choose what to draw and which overlay to display.
 */
sealed class GameState {
    data object Idle : GameState()
    data class DogIntro(val progress: Float) : GameState()
    data class DuckLaunch(val progress: Float) : GameState()
    data object Playing : GameState()
    data class RoundResult(
        val hitsThisRound: Int,
        val targetsThisRound: Int,
        val dogReaction: DogReaction,
        val progress: Float
    ) : GameState()
    data class LevelClear(val level: Int, val progress: Float) : GameState()
    data object GameOver : GameState()
    data object Paused : GameState()
}

enum class DogReaction { CARRY, LAUGH }

/**
 * One-shot occurrences the ViewModel/UI react to (navigation, sound bursts that
 * the renderer can't infer from the snapshot alone).
 */
sealed class GameEvent {
    data class Shot(val x: Float, val y: Float) : GameEvent()
    data class TargetHit(val x: Float, val y: Float, val points: Int) : GameEvent()
    data class TargetMissed(val x: Float, val y: Float) : GameEvent()
    data class TargetEscaped(val x: Float, val y: Float) : GameEvent()
    data class RoundEnded(val hits: Int, val total: Int, val reaction: DogReaction) : GameEvent()
    data class LevelCleared(val level: Int, val bonus: Int) : GameEvent()
    data object GameEnded : GameEvent()
    data object DogShotEasterEgg : GameEvent()
    data object PerfectRound : GameEvent()
}
