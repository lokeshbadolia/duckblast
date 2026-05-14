package com.duckblast.game.ui.scoreboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duckblast.game.data.model.GameRecord
import com.duckblast.game.data.model.Profile
import com.duckblast.game.data.repository.GameRecordRepository
import com.duckblast.game.data.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScoreboardEntry(
    val rank: Int,
    val profile: Profile,
    val record: GameRecord
)

data class ScoreboardUiState(
    val entries: List<ScoreboardEntry> = emptyList(),
    val selectedProfileId: Long = 0L,
    val loading: Boolean = true
)

class ScoreboardViewModel(
    private val recordRepo: GameRecordRepository,
    private val profileRepo: ProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ScoreboardUiState())
    val state: StateFlow<ScoreboardUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val leaderboard = recordRepo.getGlobalLeaderboard(20)
            val selectedId = profileRepo.getSelectedProfileId()
            val entries = leaderboard.mapIndexed { index, (profile, record) ->
                ScoreboardEntry(rank = index + 1, profile = profile, record = record)
            }
            _state.update { it.copy(entries = entries, selectedProfileId = selectedId, loading = false) }
        }
    }
}
