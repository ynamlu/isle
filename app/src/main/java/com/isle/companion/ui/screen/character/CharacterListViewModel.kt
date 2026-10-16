package com.isle.companion.ui.screen.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isle.companion.data.local.entity.CharacterEntity
import com.isle.companion.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CharacterListViewModel(private val container: AppContainer) : ViewModel() {

    val characters: StateFlow<List<CharacterEntity>> =
        container.characterRepository.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            container.characterRepository.ensureDefaultCharacter()
        }
    }

    fun delete(character: CharacterEntity) {
        viewModelScope.launch {
            container.characterRepository.delete(character)
        }
    }
}
