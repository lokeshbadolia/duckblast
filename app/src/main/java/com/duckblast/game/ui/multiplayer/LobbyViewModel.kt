package com.duckblast.game.ui.multiplayer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duckblast.game.data.model.Profile
import com.duckblast.game.data.repository.ProfileRepository
import com.duckblast.game.multiplayer.MultiplayerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class LobbyMode { IDLE, HOSTING, CLIENT, COUNTDOWN }

data class LobbyUiState(
    val selfProfile: Profile? = null,
    val players: List<MultiplayerManager.RemotePlayer> = emptyList(),
    val mode: LobbyMode = LobbyMode.IDLE,
    val countdown: Int = 0
)

class LobbyViewModel(
    private val profileRepo: ProfileRepository,
    private val multiplayer: MultiplayerManager
) : ViewModel() {

    private val _state = MutableStateFlow(LobbyUiState())
    val state: StateFlow<LobbyUiState> = _state.asStateFlow()

    private val _navigation = MutableSharedFlow<Long>(extraBufferCapacity = 2)
    val navigation: SharedFlow<Long> = _navigation.asSharedFlow()

    private var navigatingToGame = false

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val profile = profileRepo.getSelectedProfile()
            _state.update { it.copy(selfProfile = profile) }
        }
        viewModelScope.launch {
            multiplayer.players.collect { list ->
                _state.update { it.copy(players = list) }
            }
        }
        viewModelScope.launch {
            multiplayer.gameStart.collect { seed ->
                // Triggered both for self-host (echo of own start) and remote-host
                if (_state.value.mode != LobbyMode.COUNTDOWN) startCountdown(seed)
            }
        }
    }

    fun host() {
        val profile = _state.value.selfProfile ?: return
        multiplayer.startHost(profile.id, profile.name)
        _state.update { it.copy(mode = LobbyMode.HOSTING) }
    }

    fun join() {
        val profile = _state.value.selfProfile ?: return
        multiplayer.startClient(profile.id, profile.name)
        _state.update { it.copy(mode = LobbyMode.CLIENT) }
    }

    fun startGame() {
        if (_state.value.mode != LobbyMode.HOSTING) return
        val seed = nextSeed()
        multiplayer.startGame(seed)
        startCountdown(seed)
    }

    private fun startCountdown(seed: Long) {
        viewModelScope.launch {
            for (i in 3 downTo 1) {
                _state.update { it.copy(mode = LobbyMode.COUNTDOWN, countdown = i) }
                delay(1000L)
            }
            _state.update { it.copy(countdown = 0) }
            navigatingToGame = true
            _navigation.emit(seed)
        }
    }

    fun leave() {
        multiplayer.leaveSession()
        _state.update {
            it.copy(mode = LobbyMode.IDLE, players = emptyList(), countdown = 0)
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (!navigatingToGame) multiplayer.leaveSession()
    }

    private fun nextSeed(): Long {
        var s: Long
        do { s = Random.nextLong() } while (s == 0L)
        return s
    }
}
