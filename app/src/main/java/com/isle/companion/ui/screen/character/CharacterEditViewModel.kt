package com.isle.companion.ui.screen.character

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isle.companion.data.local.entity.CharacterEntity
import com.isle.companion.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CharacterEditState(
    val id: Long = 0,
    val name: String = "",
    // 默认使用中性关系类型；「恋人」等亲密定位在青少年模式下不出现
    val relationType: String = "bestie",
    val personality: String = "",
    val bio: String = "",
    val speakingStyle: String = "",
    val greeting: String = "",
    val avatarPath: String? = null,
    val isDefault: Boolean = false,
    val saved: Boolean = false,
    val deleted: Boolean = false
) {
    val isNew: Boolean get() = id == 0L
    val canSave: Boolean get() = name.isNotBlank()
    val canDelete: Boolean get() = id > 0 && !isDefault
}

class CharacterEditViewModel(
    private val container: AppContainer,
    private val characterId: Long
) : ViewModel() {

    private val _state = MutableStateFlow(CharacterEditState())
    val state: StateFlow<CharacterEditState> = _state.asStateFlow()

    init {
        if (characterId > 0) {
            viewModelScope.launch {
                container.characterRepository.get(characterId)?.let { entity ->
                    _state.value = CharacterEditState(
                        id = entity.id,
                        name = entity.name,
                        relationType = entity.relationType,
                        personality = entity.personality,
                        bio = entity.bio,
                        speakingStyle = entity.speakingStyle,
                        greeting = entity.greeting,
                        avatarPath = entity.avatarPath,
                        isDefault = entity.isDefault
                    )
                }
            }
        }
    }

    fun updateName(value: String) = _state.update { it.copy(name = value) }
    fun updateRelationType(value: String) = _state.update { it.copy(relationType = value) }
    fun updatePersonality(value: String) = _state.update { it.copy(personality = value) }
    fun updateBio(value: String) = _state.update { it.copy(bio = value) }
    fun updateSpeakingStyle(value: String) = _state.update { it.copy(speakingStyle = value) }
    fun updateGreeting(value: String) = _state.update { it.copy(greeting = value) }

    fun importAvatar(uri: Uri) {
        viewModelScope.launch {
            val path = container.avatarStorage.importFromUri(uri) ?: return@launch
            val old = _state.value.avatarPath
            if (old != null && old != path) container.avatarStorage.delete(old)
            _state.update { it.copy(avatarPath = path) }
        }
    }

    fun save() {
        val current = _state.value
        if (!current.canSave) return
        viewModelScope.launch {
            container.characterRepository.save(
                CharacterEntity(
                    id = current.id,
                    name = current.name.trim(),
                    relationType = current.relationType,
                    personality = current.personality.trim(),
                    bio = current.bio.trim(),
                    speakingStyle = current.speakingStyle.trim(),
                    greeting = current.greeting.trim(),
                    avatarPath = current.avatarPath,
                    isDefault = current.isDefault
                )
            )
            _state.update { it.copy(saved = true) }
        }
    }

    fun delete() {
        val current = _state.value
        if (!current.canDelete) return
        viewModelScope.launch {
            val entity = container.characterRepository.get(current.id) ?: return@launch
            container.characterRepository.delete(entity)
            _state.update { it.copy(deleted = true) }
        }
    }
}
