package com.duckblast.game.ui.gameover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duckblast.game.data.repository.GameRecordRepository
import com.duckblast.game.data.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GameOverUiState(
    val profileName: String = "HUNTER",
    val avatarColorHex: String = "#884400",
    val avatarInitial: String = "?",
    val bestScore: Long = 0L,
    val totalGames: Int = 0,
    val streakDays: Int = 0,
    val loading: Boolean = true
)

class GameOverViewModel(
    private val profileRepo: ProfileRepository,
    private val recordRepo: GameRecordRepository
) : ViewModel() {

    private val _state = MutableStateFlow(GameOverUiState())
    val state: StateFlow<GameOverUiState> = _state.asStateFlow()

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            val profile = profileRepo.getSelectedProfile()
            val records = profile?.let { recordRepo.getRecordsForProfile(it.id) } ?: emptyList()
            _state.update {
                it.copy(
                    profileName = profile?.name?.uppercase() ?: "HUNTER",
                    avatarColorHex = profile?.avatarColorHex ?: "#884400",
                    avatarInitial = profile?.initial ?: "?",
                    bestScore = profile?.highScore ?: 0L,
                    totalGames = records.size,
                    streakDays = profile?.streakDays ?: 0,
                    loading = false
                )
            }
        }
    }
}
