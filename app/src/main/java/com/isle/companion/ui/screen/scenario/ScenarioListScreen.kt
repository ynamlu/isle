package com.isle.companion.ui.screen.scenario

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.isle.companion.di.appContainer
import com.isle.companion.domain.scenario.Scenario
import com.isle.companion.domain.scenario.ScenarioCatalog
import com.isle.companion.ui.screen.character.CharacterListViewModel
import com.isle.companion.ui.theme.GlassCard
import com.isle.companion.ui.theme.IsleRadius
import com.isle.companion.ui.theme.isleBackground
import com.isle.companion.ui.theme.liquidGlass
import com.isle.companion.ui.theme.liquidGlassSource
import com.isle.companion.ui.theme.rememberLiquidGlassState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScenarioListScreen(
    onOpenScenario: (Long, String) -> Unit
) {
    val container = LocalContext.current.appContainer
    val viewModel: CharacterListViewModel = viewModel(
        factory = viewModelFactory { initializer { CharacterListViewModel(container) } }
    )
    val characters by viewModel.characters.collectAsState()
    val hazeState = rememberLiquidGlassState()
    var pendingScenario by remember { mutableStateOf<Scenario?>(null) }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                modifier = Modifier.liquidGlass(hazeState),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text("场景") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .isleBackground()
                .liquidGlassSource(hazeState),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding() + 10.dp,
                bottom = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("今天想从哪里开始？", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "选一个最接近此刻的场景，我会先开口，不用你组织语言。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            items(ScenarioCatalog.all, key = { it.id }) { scenario ->
                ScenarioCard(scenario = scenario, onClick = { pendingScenario = scenario })
            }
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
                    if (characters.isEmpty()) {
                        Text("先创建一个角色，再开始场景。")
                    } else {
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
                }
            },
            confirmButton = {}
        )
    }
}

@Composable
private fun ScenarioCard(scenario: Scenario, onClick: () -> Unit) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
        shape = IsleRadius.card,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = scenario.emoji, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = scenario.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = scenario.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

private fun relationLabel(type: String): String = when (type) {
    "bestie" -> "闺蜜"
    "teacher" -> "老师"
    "friend" -> "朋友"
    "family" -> "家人"
    else -> "自定义"
}
