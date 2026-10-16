package com.isle.companion.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isle.companion.data.prefs.DeepSeekSettings
import com.isle.companion.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    val settings: StateFlow<DeepSeekSettings> =
        container.settingsRepository.settings
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                DeepSeekSettings()
            )

    fun updateApiKey(value: String) = viewModelScope.launch {
        container.settingsRepository.updateApiKey(value.trim())
    }

    fun updateBaseUrl(value: String) = viewModelScope.launch {
        container.settingsRepository.updateBaseUrl(value.trim())
    }

    fun updateModel(value: String) = viewModelScope.launch {
        container.settingsRepository.updateModel(value.trim())
    }

    fun updateTemperature(value: Double) = viewModelScope.launch {
        container.settingsRepository.updateTemperature(value)
    }
}
