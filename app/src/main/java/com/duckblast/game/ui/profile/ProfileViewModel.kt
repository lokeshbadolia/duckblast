package com.duckblast.game.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duckblast.game.data.model.Profile
import com.duckblast.game.data.repository.ProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val profiles: List<Profile> = emptyList(),
    val selectedId: Long = 0L,
    val loading: Boolean = true,
    val showCreateDialog: Boolean = false,
    val pendingDeleteId: Long? = null,
    val errorMessage: String? = null
)

class ProfileViewModel(
    private val profileRepo: ProfileRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            val profiles = profileRepo.getAllProfiles()
            val selectedId = profileRepo.getSelectedProfileId()
            _state.update {
                it.copy(profiles = profiles, selectedId = selectedId, loading = false)
            }
        }
    }

    fun openCreateDialog() {
        _state.update { it.copy(showCreateDialog = true, errorMessage = null) }
    }

    fun dismissCreateDialog() {
        _state.update { it.copy(showCreateDialog = false, errorMessage = null) }
    }

    fun createProfile(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            _state.update { it.copy(errorMessage = "Name can't be empty") }
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val existing = profileRepo.findByName(trimmed)
            if (existing != null) {
                _state.update { it.copy(errorMessage = "Hunter \"$trimmed\" already exists") }
                return@launch
            }
            val color = AvatarPalette.random()
            val id = profileRepo.saveProfile(Profile(name = trimmed, avatarColorHex = color))
            profileRepo.setSelectedProfileId(id)
            _state.update { it.copy(showCreateDialog = false, errorMessage = null) }
            refresh()
        }
    }

    fun selectProfile(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            profileRepo.setSelectedProfileId(id)
            refresh()
        }
    }

    fun requestDelete(id: Long) {
        _state.update { it.copy(pendingDeleteId = id) }
    }

    fun cancelDelete() {
        _state.update { it.copy(pendingDeleteId = null) }
    }

    fun confirmDelete() {
        val id = _state.value.pendingDeleteId ?: return
        viewModelScope.launch(Dispatchers.IO) {
            profileRepo.deleteProfile(id)
            _state.update { it.copy(pendingDeleteId = null) }
            refresh()
        }
    }
}

object AvatarPalette {
    private val palette = listOf(
        "#FF6B6B", "#FFA600", "#FCFC00", "#00CC00",
        "#1AA7EC", "#5C94FC", "#B14CFF", "#FF6FB5",
        "#884400", "#007700"
    )

    fun random(): String = palette.random()
    fun all(): List<String> = palette
}
