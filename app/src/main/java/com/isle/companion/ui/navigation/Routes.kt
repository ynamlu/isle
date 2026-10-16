package com.isle.companion.ui.navigation

object Routes {
    const val CHARACTERS = "characters"
    const val CHAT = "chat/{characterId}"
    const val CHARACTER_EDIT = "character_edit/{characterId}"
    const val SETTINGS = "settings"
    const val USER_PROFILE = "settings/profile"
    const val MODEL_CONFIG = "settings/model"
    const val STYLE_CONFIG = "settings/style"
    const val TECH_INFO = "settings/tech"
    const val MEMORY = "memory/{characterId}"

    fun chat(characterId: Long) = "chat/$characterId"
    fun characterEdit(characterId: Long) = "character_edit/$characterId"
    fun memory(characterId: Long) = "memory/$characterId"
}
