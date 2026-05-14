package com.duckblast.game.ui.game

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duckblast.game.data.model.GameRecord
import com.duckblast.game.data.repository.GameRecordRepository
import com.duckblast.game.data.repository.ProfileRepository
import com.duckblast.game.game.engine.GameEngine
import com.duckblast.game.game.engine.GameEvent
import com.duckblast.game.game.engine.GameState
import com.duckblast.game.multiplayer.MultiplayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class GameOverlayState {
    data object None : GameOverlayState()
    data object Paused : GameOverlayState()
}

sealed class GameNavigation {
    data object ToMenu : GameNavigation()
    data class ToGameOver(
        val score: Long,
        val level: Int,
        val ducksHit: Int,
        val shotsFired: Int,
        val durationSec: Int,
        val perfect: Boolean,
        val previousBest: Long
    ) : GameNavigation()
}

class GameViewModel(
    val engine: GameEngine,
    private val profileRepo: ProfileRepository,
    private val recordRepo: GameRecordRepository,
    private val multiplayer: MultiplayerManager
) : ViewModel() {

    private val _overlay = MutableStateFlow<GameOverlayState>(GameOverlayState.None)
    val overlay: StateFlow<GameOverlayState> = _overlay.asStateFlow()

    private val _navigation = MutableSharedFlow<GameNavigation>(extraBufferCapacity = 4)
    val navigation: SharedFlow<GameNavigation> = _navigation.asSharedFlow()

    val remotePlayers: StateFlow<List<MultiplayerManager.RemotePlayer>> = multiplayer.players

    var isMultiplayer: Boolean = false
        private set
    private var seed: Long = 0L
    private var initialized = false
    private var surfaceReady = false
    private var started = false
    private var profileId: Long = 0L
    private var finalized = false
    private var scoreSyncJob: Job? = null

    init {
        viewModelScope.launch {
            engine.events.collect { event ->
                if (event is GameEvent.GameEnded) finalizeGame()
            }
        }
    }

    fun initialize(seed: Long, multiplayer: Boolean) {
        if (initialized) return
        this.seed = seed
        this.isMultiplayer = multiplayer
        initialized = true
        viewModelScope.launch(Dispatchers.IO) {
            val profile = profileRepo.getSelectedProfile()
            profileId = profile?.id ?: 0L
            engine.hiScore = profile?.highScore ?: 0L
        }
        if (multiplayer) startScoreBroadcasts()
        maybeStart()
    }

    fun onSurfaceReady() {
        surfaceReady = true
        maybeStart()
    }

    private fun maybeStart() {
        if (!initialized || !surfaceReady || started) return
        started = true
        engine.start(seed)
    }

    private fun startScoreBroadcasts() {
        scoreSyncJob?.cancel()
        scoreSyncJob = viewModelScope.launch {
            while (isActive) {
                if (engine.state is GameState.Playing
                    || engine.state is GameState.RoundResult
                    || engine.state is GameState.LevelClear
                ) {
                    multiplayer.publishScore(engine.score, engine.currentLevel, alive = true)
                }
                delay(500L)
            }
        }
    }

    fun togglePause() {
        if (engine.state is GameState.Paused) {
            engine.resume()
            _overlay.value = GameOverlayState.None
        } else {
            engine.pause()
            _overlay.value = GameOverlayState.Paused
        }
    }

    fun quit() {
        engine.pause()
        if (isMultiplayer) multiplayer.publishGameEnd(engine.score)
        viewModelScope.launch { _navigation.emit(GameNavigation.ToMenu) }
    }

    private fun finalizeGame() {
        if (finalized) return
        finalized = true
        val finalScore = engine.score
        val finalLevel = engine.currentLevel
        val finalDucksHit = engine.ducksHit
        val finalShotsFired = engine.shotsFired
        val accuracy = engine.accuracy
        val duration = engine.elapsedSeconds
        val perfect = finalShotsFired > 0 && finalDucksHit == finalShotsFired
        if (isMultiplayer) multiplayer.publishGameEnd(finalScore)
        viewModelScope.launch {
            val previousBest = withContext(Dispatchers.IO) {
                val prev = profileRepo.getSelectedProfile()?.highScore ?: 0L
                persistRecord(finalScore, finalLevel, finalDucksHit, finalShotsFired, accuracy, duration, perfect)
                prev
            }
            _navigation.emit(
                GameNavigation.ToGameOver(
                    score = finalScore,
                    level = finalLevel,
                    ducksHit = finalDucksHit,
                    shotsFired = finalShotsFired,
                    durationSec = duration,
                    perfect = perfect,
                    previousBest = previousBest
                )
            )
        }
    }

    private fun persistRecord(
        score: Long, level: Int, ducksHit: Int, shotsFired: Int,
        accuracy: Float, duration: Int, perfect: Boolean
    ) {
        if (profileId == 0L) return
        profileRepo.updateStats(profileId, ducksHit, shotsFired, level)
        profileRepo.updateHighScore(profileId, score)
        recordRepo.insertRecord(
            GameRecord(
                profileId = profileId,
                score = score,
                levelReached = level,
                ducksHit = ducksHit,
                shotsFired = shotsFired,
                accuracy = accuracy,
                durationSeconds = duration,
                isPerfectGame = perfect
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        scoreSyncJob?.cancel()
        if (isMultiplayer) multiplayer.leaveSession()
    }
}
