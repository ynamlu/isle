package com.isle.companion.ui.navigation

object Routes {
    const val CHARACTERS = "characters"
    const val SCENARIOS = "scenarios"
    const val MEMORY_OVERVIEW = "memory_overview"
    const val ME = "me"
    const val CHAT = "chat/{characterId}?scenario={scenarioId}"
    const val CHARACTER_EDIT = "character_edit/{characterId}"
    const val SETTINGS = "settings"
    const val ACCOUNT = "settings/account"
    const val USER_PROFILE = "settings/profile"
    const val MODEL_CONFIG = "settings/model"
    const val STYLE_CONFIG = "settings/style"
    const val TECH_INFO = "settings/tech"
    const val MEMORY = "memory/{characterId}"

    fun chat(characterId: Long, scenarioId: String? = null): String =
        if (scenarioId.isNullOrBlank()) "chat/$characterId" else "chat/$characterId?scenario=$scenarioId"
    fun characterEdit(characterId: Long) = "character_edit/$characterId"
    fun memory(characterId: Long) = "memory/$characterId"
}
