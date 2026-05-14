package com.duckblast.game.ui.menu

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duckblast.game.audio.SoundId
import com.duckblast.game.audio.SoundManager
import com.duckblast.game.data.model.Profile
import com.duckblast.game.data.repository.GameRecordRepository
import com.duckblast.game.data.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MenuUiState(
    val profile: Profile? = null,
    val totalGames: Int = 0,
    val lastScore: Long = 0L,
    val ready: Boolean = false
)

class MenuViewModel(
    private val profileRepo: ProfileRepository,
    private val recordRepo: GameRecordRepository,
    private val soundManager: SoundManager
) : ViewModel() {

    private val _state = MutableStateFlow(MenuUiState())
    val state: StateFlow<MenuUiState> = _state.asStateFlow()

    init { refresh() }

    fun startMenuMusic() {
        soundManager.playLoop(SoundId.MENU_THEME)
    }

    fun stopMenuMusic() {
        soundManager.stopLoop()
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val profile = profileRepo.getSelectedProfile()
            val records = profile?.let { recordRepo.getRecordsForProfile(it.id) } ?: emptyList()
            _state.update {
                it.copy(
                    profile = profile,
                    totalGames = records.size,
                    lastScore = records.firstOrNull()?.score ?: 0L,
                    ready = true
                )
            }
        }
    }

    fun click() {
        soundManager.play(SoundId.MENU_SELECT)
    }

    fun onPlayPressed() {
        click()
        viewModelScope.launch(Dispatchers.IO) {
            val id = profileRepo.getSelectedProfileId()
            if (id != 0L) profileRepo.updateStreak(id)
        }
    }
}
