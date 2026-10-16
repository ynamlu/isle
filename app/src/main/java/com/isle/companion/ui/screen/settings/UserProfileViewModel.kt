package com.isle.companion.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isle.companion.data.prefs.UserProfile
import com.isle.companion.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UserProfileViewModel(private val container: AppContainer) : ViewModel() {

    val profile: StateFlow<UserProfile> =
        container.userProfileRepository.profile
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UserProfile())

    fun updateName(value: String) = viewModelScope.launch {
        container.userProfileRepository.updateName(value)
    }

    fun updatePersonality(value: String) = viewModelScope.launch {
        container.userProfileRepository.updatePersonality(value)
    }

    fun updateIntro(value: String) = viewModelScope.launch {
        container.userProfileRepository.updateIntro(value)
    }
}
