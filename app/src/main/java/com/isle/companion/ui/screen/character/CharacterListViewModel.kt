package com.isle.companion.ui.screen.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isle.companion.data.local.entity.CharacterEntity
import com.isle.companion.domain.character.visibleFor
import com.isle.companion.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CharacterListViewModel(private val container: AppContainer) : ViewModel() {

    val characters: StateFlow<List<CharacterEntity>> =
        container.characterRepository.observeAll()
            .map { it.visibleFor() }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())


    fun delete(character: CharacterEntity) {
        viewModelScope.launch {
            container.characterRepository.delete(character)
        }
    }
}
