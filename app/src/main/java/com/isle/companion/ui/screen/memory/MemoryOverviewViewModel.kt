package com.isle.companion.ui.screen.memory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isle.companion.data.local.entity.MemoryEntity
import com.isle.companion.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class MemoryOverviewItem(
    val memory: MemoryEntity,
    val characterName: String,
    val relationType: String,
    val avatarPath: String?
)

data class MemoryCharacterGroup(
    val characterId: Long,
    val characterName: String,
    val relationType: String,
    val avatarPath: String?,
    val items: List<MemoryOverviewItem>
) {
    val count: Int get() = items.size
    val latestAt: Long get() = items.maxOfOrNull { it.memory.createdAt } ?: 0L
}

class MemoryOverviewViewModel(container: AppContainer) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    private val _expandedIds = MutableStateFlow<Set<Long>>(emptySet())
    val expandedIds: StateFlow<Set<Long>> = _expandedIds

    val allItems: StateFlow<List<MemoryOverviewItem>> = combine(
        container.memoryRepository.observeAll(),
        container.characterRepository.observeAll()
    ) { memories, characters ->
        val characterMap = characters.associateBy { it.id }
        memories.mapNotNull { memory ->
            val character = characterMap[memory.characterId] ?: return@mapNotNull null
            if (character.relationType == "love") return@mapNotNull null
            MemoryOverviewItem(memory, character.name, character.relationType, character.avatarPath)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val items: StateFlow<List<MemoryOverviewItem>> = combine(allItems, _query) { list, keyword ->
        val q = keyword.trim()
        if (q.isBlank()) list else list.filter {
            it.memory.content.contains(q, ignoreCase = true) ||
                it.characterName.contains(q, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val groups: StateFlow<List<MemoryCharacterGroup>> = items.map { list ->
        list.groupBy { it.memory.characterId }
            .map { (characterId, memories) ->
                val first = memories.first()
                MemoryCharacterGroup(
                    characterId = characterId,
                    characterName = first.characterName,
                    relationType = first.relationType,
                    avatarPath = first.avatarPath,
                    items = memories.sortedByDescending { it.memory.createdAt }
                )
            }
            .sortedByDescending { it.latestAt }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun updateQuery(value: String) {
        _query.value = value
    }

    fun toggleExpanded(characterId: Long) {
        _expandedIds.update { ids ->
            if (characterId in ids) ids - characterId else ids + characterId
        }
    }
}

internal fun overviewCategoryLabel(category: String): String = when (category) {
    "preference" -> "喜好"
    "event" -> "经历"
    "relationship" -> "关系"
    "promise" -> "约定"
    else -> "信息"
}
