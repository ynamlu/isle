package com.isle.companion.ui.screen.character

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.isle.companion.data.local.entity.CharacterEntity
import com.isle.companion.di.appContainer
import com.isle.companion.domain.scenario.Scenario
import com.isle.companion.domain.scenario.ScenarioCatalog
import com.isle.companion.ui.component.CharacterAvatar
import com.isle.companion.ui.theme.GlassCard
import com.isle.companion.ui.theme.IsleBrandBrush
import com.isle.companion.ui.theme.IslePurple
import com.isle.companion.ui.theme.IsleRadius
import com.isle.companion.ui.theme.IsleSpacing
import com.isle.companion.ui.theme.liquidGlass
import com.isle.companion.ui.theme.liquidGlassSource
import com.isle.companion.ui.theme.rememberLiquidGlassState
import com.isle.companion.ui.theme.isleBackground
import com.isle.companion.ui.theme.relationColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterListScreen(
    onOpenChat: (Long) -> Unit,
    onOpenScenario: (Long, String) -> Unit,
    onCreate: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val container = LocalContext.current.appContainer
    val viewModel: CharacterListViewModel = viewModel(
        factory = viewModelFactory { initializer { CharacterListViewModel(container) } }
    )
    val characters by viewModel.characters.collectAsState()
    val hazeState = rememberLiquidGlassState()
    var pendingScenario by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<Scenario?>(null)
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                modifier = Modifier.liquidGlass(hazeState),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = {
                    Text(
                        text = "心屿",
                        style = MaterialTheme.typography.titleLarge.copy(brush = IsleBrandBrush),
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "设置")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .isleBackground()
                .liquidGlassSource(hazeState),
            contentPadding = PaddingValues(
                start = IsleSpacing.lg,
                end = IsleSpacing.lg,
                top = padding.calculateTopPadding() + IsleSpacing.md,
                bottom = padding.calculateBottomPadding() + IsleSpacing.xxl
            ),
            verticalArrangement = Arrangement.spacedBy(IsleSpacing.md)
        ) {
            item { HeroCard() }

            item {
                Text(
                    text = "今天想聊什么",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = IsleSpacing.xs, top = IsleSpacing.sm)
                )
            }

            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(IsleSpacing.sm),
                    contentPadding = PaddingValues(horizontal = IsleSpacing.xs)
                ) {
                    items(ScenarioCatalog.all, key = { it.id }) { scenario ->
                        ScenarioChip(scenario = scenario, onClick = { pendingScenario = scenario })
                    }
                }
            }

            item {
                Text(
                    text = "我的陪伴者 · ${characters.size}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = IsleSpacing.xs,
                        top = IsleSpacing.sm
                    )
                )
            }

            items(characters, key = { it.id }) { character ->
                CharacterCard(
                    character = character,
                    onClick = { onOpenChat(character.id) }
                )
            }

            item { AddCharacterCard(onClick = onCreate) }
        }
    }

    pendingScenario?.let { scenario ->
        AlertDialog(
            onDismissRequest = { pendingScenario = null },
            title = { Text("让谁来陪你？") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = scenario.title + " · " + scenario.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    characters.forEach { character ->
                        TextButton(
                            onClick = {
                                pendingScenario = null
                                onOpenScenario(character.id, scenario.id)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("${character.name} · ${relationLabel(character.relationType)}")
                        }
                    }
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun ScenarioChip(scenario: Scenario, onClick: () -> Unit) {
    Surface(
        shape = IsleRadius.card,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        shadowElevation = 0.dp,
        modifier = Modifier.width(168.dp),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = scenario.emoji, style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                text = scenario.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = scenario.subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HeroCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 12.dp,
                shape = IsleRadius.cardLarge,
                clip = false,
                ambientColor = IslePurple.copy(alpha = 0.35f),
                spotColor = IslePurple.copy(alpha = 0.35f)
            )
            .clip(IsleRadius.cardLarge)
            .background(IsleBrandBrush)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.35f),
                shape = IsleRadius.cardLarge
            )
            .padding(horizontal = IsleSpacing.xl, vertical = IsleSpacing.lg)
    ) {
        Column {
            Text(
                text = "一座属于你的心灵小岛",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(IsleSpacing.xs))
            Text(
                text = "在这里，总有人愿意听你说完",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f)
            )
        }
    }
}

@Composable
private fun CharacterCard(
    character: CharacterEntity,
    onClick: () -> Unit
) {
    val accent = relationColor(character.relationType)

    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = IsleRadius.card,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(IsleSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CharacterAvatar(
                avatarPath = character.avatarPath,
                name = character.name,
                size = 54.dp,
                ringColor = accent
            )

            Spacer(Modifier.width(IsleSpacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = character.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(IsleSpacing.sm))
                    Box(
                        modifier = Modifier
                            .clip(IsleRadius.chip)
                            .background(accent.copy(alpha = 0.14f))
                            .padding(horizontal = IsleSpacing.sm, vertical = 2.dp)
                    ) {
                        Text(
                            text = relationLabel(character.relationType),
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = character.bio.ifBlank { character.personality }.ifBlank { "还没有填写简介" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun AddCharacterCard(onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = IsleRadius.card,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(IsleSpacing.sm))
            Text(
                text = "创建新角色",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun relationLabel(type: String): String = when (type) {
    "love" -> "旧版角色"
    "bestie" -> "闺蜜"
    "teacher" -> "老师"
    "friend" -> "朋友"
    "family" -> "家人"
    else -> "自定义"
}
