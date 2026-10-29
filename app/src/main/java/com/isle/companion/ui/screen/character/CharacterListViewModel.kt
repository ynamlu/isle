package com.isle.companion.ui.screen.character

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isle.companion.data.local.entity.CharacterEntity
import com.isle.companion.domain.character.visibleFor
import com.isle.companion.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CharacterListViewModel(private val container: AppContainer) : ViewModel() {

    val characters: StateFlow<List<CharacterEntity>> = combine(
        container.characterRepository.observeAll(),
        container.userProfileRepository.profile
    ) { list, profile ->
        // 青少年模式不开放亲密/自定义关系角色，已有角色也隐藏（数据保留）
        list.visibleFor(profile.isMinor)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())


    fun delete(character: CharacterEntity) {
        viewModelScope.launch {
            container.characterRepository.delete(character)
        }
    }
}
