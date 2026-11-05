package com.isle.companion.ui.screen.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isle.companion.data.local.entity.MemoryEntity
import com.isle.companion.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MemoryViewModel(
    private val container: AppContainer,
    private val characterId: Long
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val allMemories: StateFlow<List<MemoryEntity>> =
        container.memoryRepository.observe(characterId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val memories: StateFlow<List<MemoryEntity>> = combine(allMemories, _query) { list, keyword ->
        val q = keyword.trim()
        if (q.isBlank()) list else list.filter {
            it.content.contains(q, ignoreCase = true) || categoryLabel(it.category).contains(q)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateQuery(value: String) {
        _query.value = value
    }

    fun update(memory: MemoryEntity, content: String) {
        viewModelScope.launch { container.memoryRepository.update(memory, content) }
    }

    fun delete(memory: MemoryEntity) {
        viewModelScope.launch { container.memoryRepository.delete(memory) }
    }

    fun clearAll() {
        viewModelScope.launch { container.memoryRepository.clear(characterId) }
    }
}

internal fun categoryLabel(category: String): String = when (category) {
    "preference" -> "喜好"
    "event" -> "经历"
    "relationship" -> "关系"
    "promise" -> "约定"
    else -> "信息"
}
