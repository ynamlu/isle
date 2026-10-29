package com.isle.companion.ui.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isle.companion.data.prefs.DeepSeekSettings
import com.isle.companion.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    fun updateMaxTokens(value: Int) = viewModelScope.launch {
        container.settingsRepository.updateMaxTokens(value)
    }

    private val _testing = MutableStateFlow(false)
    val testing: StateFlow<Boolean> = _testing.asStateFlow()

    private val _testMessage = MutableStateFlow<String?>(null)
    val testMessage: StateFlow<String?> = _testMessage.asStateFlow()

    fun testConnection() {
        if (_testing.value) return
        viewModelScope.launch {
            val current = settings.value
            if (current.effectiveApiKey.isBlank()) {
                _testMessage.value = "请先填写 API Key"
                return@launch
            }
            _testing.value = true
            _testMessage.value = null
            _testMessage.value = container.deepSeekClient.testConnection(current)
                .getOrElse { it.message ?: "连接失败" }
            _testing.value = false
        }
    }

    fun clearTestMessage() {
        _testMessage.value = null
    }

    /** 回到年龄确认页，重新选择年龄段。 */
    fun resetAgeGroup() = viewModelScope.launch {
        container.userProfileRepository.clearAgeGroup()
    }

    /** 演示数据：恢复演示角色、记忆与示例对话。 */
    fun resetDemoData() = viewModelScope.launch {
        container.demoDataSeeder.reset()
    }
}
