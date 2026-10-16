package com.isle.companion.ui.screen.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isle.companion.data.local.entity.MemoryEntity
import com.isle.companion.di.AppContainer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MemoryViewModel(
    private val container: AppContainer,
    private val characterId: Long
) : ViewModel() {

    val memories: StateFlow<List<MemoryEntity>> =
        container.memoryRepository.observe(characterId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun delete(memory: MemoryEntity) {
        viewModelScope.launch { container.memoryRepository.delete(memory) }
    }

    fun clearAll() {
        viewModelScope.launch { container.memoryRepository.clear(characterId) }
    }
}
