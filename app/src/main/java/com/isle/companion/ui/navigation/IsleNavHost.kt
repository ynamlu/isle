package com.isle.companion.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.isle.companion.ui.screen.character.CharacterEditScreen
import com.isle.companion.ui.screen.character.CharacterListScreen
import com.isle.companion.ui.screen.chat.ChatScreen
import com.isle.companion.ui.screen.memory.MemoryOverviewScreen
import com.isle.companion.ui.screen.memory.MemoryScreen
import com.isle.companion.ui.screen.scenario.ScenarioListScreen
import com.isle.companion.ui.screen.settings.AccountScreen
import com.isle.companion.ui.screen.settings.ModelConfigScreen
import com.isle.companion.ui.screen.settings.SettingsScreen
import com.isle.companion.ui.screen.settings.StyleConfigScreen
import com.isle.companion.ui.screen.settings.TechInfoScreen
import com.isle.companion.ui.screen.settings.UserProfileScreen

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.CHARACTERS, "首页", Icons.Filled.Home),
    BottomNavItem(Routes.SCENARIOS, "场景", Icons.Filled.Explore),
    BottomNavItem(Routes.MEMORY_OVERVIEW, "记忆", Icons.Filled.Psychology),
    BottomNavItem(Routes.ME, "我的", Icons.Filled.Person)
)

@Composable
fun IsleNavHost(
    onLock: () -> Unit = {},
    onAccountDeleted: () -> Unit = {}
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        containerColor = Color.Transparent,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                IsleBottomBar(
                    currentRoute = currentRoute,
                    onSelect = { route -> navController.switchTab(route) }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.CHARACTERS,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding())
        ) {
            composable(Routes.CHARACTERS) {
                CharacterListScreen(
                    onOpenChat = { id -> navController.navigate(Routes.chat(id)) },
                    onOpenScenario = { id, scenarioId ->
                        navController.navigate(Routes.chat(id, scenarioId))
                    },
                    onCreate = { navController.navigate(Routes.characterEdit(0)) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }

            composable(Routes.SCENARIOS) {
                ScenarioListScreen(
                    onOpenScenario = { id, scenarioId ->
                        navController.navigate(Routes.chat(id, scenarioId))
                    }
                )
            }

            composable(Routes.MEMORY_OVERVIEW) {
                MemoryOverviewScreen(
                    onOpenCharacterMemory = { characterId ->
                        navController.navigate(Routes.memory(characterId))
                    }
                )
            }

            composable(Routes.ME) {
                SettingsScreen(
                    onBack = {},
                    onOpenUserProfile = { navController.navigate(Routes.USER_PROFILE) },
                    onOpenModelConfig = { navController.navigate(Routes.MODEL_CONFIG) },
                    onOpenStyleConfig = { navController.navigate(Routes.STYLE_CONFIG) },
                    onOpenTechInfo = { navController.navigate(Routes.TECH_INFO) },
                    onOpenAccount = { navController.navigate(Routes.ACCOUNT) },
                    isTab = true
                )
            }

            composable(
                route = Routes.CHAT,
                arguments = listOf(
                    navArgument("characterId") { type = NavType.LongType },
                    navArgument("scenarioId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { entry ->
                val characterId = entry.arguments?.getLong("characterId") ?: 0L
                val scenarioId = entry.arguments?.getString("scenarioId")
                ChatScreen(
                    characterId = characterId,
                    scenarioId = scenarioId,
                    onBack = { navController.popBackStack() },
                    onEditCharacter = { navController.navigate(Routes.characterEdit(characterId)) }
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
                    onOpenTechInfo = { navController.navigate(Routes.TECH_INFO) },
                    onOpenAccount = { navController.navigate(Routes.ACCOUNT) }
                )
            }

            composable(Routes.ACCOUNT) {
                AccountScreen(
                    onBack = { navController.popBackStack() },
                    onLock = onLock,
                    onAccountDeleted = onAccountDeleted
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
}

@Composable
private fun IsleBottomBar(currentRoute: String?, onSelect: (String) -> Unit) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        tonalElevation = 0.dp
    ) {
        bottomNavItems.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { onSelect(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}

private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
