package com.isle.companion.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.isle.companion.ui.screen.character.CharacterEditScreen
import com.isle.companion.ui.screen.character.CharacterListScreen
import com.isle.companion.ui.screen.chat.ChatScreen
import com.isle.companion.ui.screen.memory.MemoryScreen
import com.isle.companion.ui.screen.settings.ModelConfigScreen
import com.isle.companion.ui.screen.settings.SettingsScreen
import com.isle.companion.ui.screen.settings.StyleConfigScreen
import com.isle.companion.ui.screen.settings.TechInfoScreen
import com.isle.companion.ui.screen.settings.UserProfileScreen

@Composable
fun IsleNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.CHARACTERS) {
        composable(Routes.CHARACTERS) {
            CharacterListScreen(
                onOpenChat = { id -> navController.navigate(Routes.chat(id)) },
                onCreate = { navController.navigate(Routes.characterEdit(0)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) }
            )
        }

        composable(Routes.CHAT) { entry ->
            val characterId = entry.arguments?.getString("characterId")?.toLongOrNull() ?: 0L
            ChatScreen(
                characterId = characterId,
                onBack = { navController.popBackStack() },
                onEditCharacter = { navController.navigate(Routes.characterEdit(characterId)) },
                onOpenMemory = { navController.navigate(Routes.memory(characterId)) }
            )
        }

        composable(Routes.MEMORY) { entry ->
            val characterId = entry.arguments?.getString("characterId")?.toLongOrNull() ?: 0L
            MemoryScreen(
                characterId = characterId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.CHARACTER_EDIT) { entry ->
            val characterId = entry.arguments?.getString("characterId")?.toLongOrNull() ?: 0L
            CharacterEditScreen(
                characterId = characterId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenUserProfile = { navController.navigate(Routes.USER_PROFILE) },
                onOpenModelConfig = { navController.navigate(Routes.MODEL_CONFIG) },
                onOpenStyleConfig = { navController.navigate(Routes.STYLE_CONFIG) },
                onOpenTechInfo = { navController.navigate(Routes.TECH_INFO) }
            )
        }

        composable(Routes.USER_PROFILE) {
            UserProfileScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.MODEL_CONFIG) {
            ModelConfigScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.STYLE_CONFIG) {
            StyleConfigScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.TECH_INFO) {
            TechInfoScreen(onBack = { navController.popBackStack() })
        }
    }
}
