package com.lifetrack.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lifetrack.app.domain.repository.ProfileRepository
import com.lifetrack.app.domain.repository.ProfileResult
import com.lifetrack.app.domain.repository.ProfileUpdate
import com.lifetrack.app.domain.repository.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(val profile: UserProfile? = null, val loading: Boolean = false, val error: String? = null, val message: String? = null)

@HiltViewModel
class ProfileViewModel @Inject constructor(private val repository: ProfileRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = mutableState.asStateFlow()

    fun load() = viewModelScope.launch {
        mutableState.value = ProfileUiState(loading = true)
        when (val result = repository.get()) {
            is ProfileResult.Success -> mutableState.value = ProfileUiState(profile = result.value)
            is ProfileResult.Error -> mutableState.update { it.copy(loading = false, error = result.message) }
        }
    }

    fun save(update: ProfileUpdate) = viewModelScope.launch {
        mutableState.update { it.copy(loading = true, error = null, message = null) }
        when (val result = repository.update(update)) {
            is ProfileResult.Success -> mutableState.value = ProfileUiState(profile = result.value, message = "Perfil actualizado.")
            is ProfileResult.Error -> mutableState.update { it.copy(loading = false, error = result.message) }
        }
    }
}
