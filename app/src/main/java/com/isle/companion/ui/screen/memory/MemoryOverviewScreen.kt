package com.isle.companion.ui.screen.memory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.isle.companion.di.appContainer
import com.isle.companion.ui.component.CharacterAvatar
import com.isle.companion.ui.theme.GlassCard
import com.isle.companion.ui.theme.IsleRadius
import com.isle.companion.ui.theme.isleBackground
import com.isle.companion.ui.theme.liquidGlass
import com.isle.companion.ui.theme.liquidGlassSource
import com.isle.companion.ui.theme.relationColor
import com.isle.companion.ui.theme.rememberLiquidGlassState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryOverviewScreen(
    onOpenCharacterMemory: (Long) -> Unit
) {
    val container = LocalContext.current.appContainer
    val viewModel: MemoryOverviewViewModel = viewModel(
        factory = viewModelFactory { initializer { MemoryOverviewViewModel(container) } }
    )
    val groups by viewModel.groups.collectAsState()
    val allItems by viewModel.allItems.collectAsState()
    val query by viewModel.query.collectAsState()
    val expandedIds by viewModel.expandedIds.collectAsState()
    val hazeState = rememberLiquidGlassState()

    val weekStart = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
    val weekNew = allItems.count { it.memory.createdAt >= weekStart }
    val topCategory = allItems
        .filter { it.memory.createdAt >= weekStart }
        .groupingBy { overviewCategoryLabel(it.memory.category) }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                modifier = Modifier.liquidGlass(hazeState),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text("记忆") }
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text("这一周，TA 记住了什么", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = if (weekNew == 0) {
                                "这周还没有新的记忆，多聊几句吧"
                            } else {
                                "本周新记住 $weekNew 件事" +
                                    (topCategory?.let { " · 最常出现：$it" } ?: "")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::updateQuery,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text("搜索记忆或角色") },
                    singleLine = true,
                    shape = IsleRadius.field,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (groups.isEmpty()) {
                item {
                    Text(
                        text = if (query.isBlank()) "还没有长期记忆" else "没有找到相关记忆",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            items(groups, key = { it.characterId }) { group ->
                val searching = query.isNotBlank()
                val expanded = searching || group.characterId in expandedIds
                val rotation by animateFloatAsState(
                    targetValue = if (expanded) 180f else 0f,
                    label = "memoryGroupArrow"
                )

                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.toggleExpanded(group.characterId)
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CharacterAvatar(
                                avatarPath = group.avatarPath,
                                name = group.characterName,
                                size = 44.dp,
                                ringColor = relationColor(group.relationType)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = group.characterName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(IsleRadius.chip)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = relationLabel(group.relationType),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "${group.count} 条记忆 · 点击${if (expanded) "收起" else "展开"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            TextButton(
                                onClick = { onOpenCharacterMemory(group.characterId) }
                            ) { Text("管理") }
                            Icon(
                                imageVector = Icons.Filled.KeyboardArrowDown,
                                contentDescription = if (expanded) "收起" else "展开",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(24.dp)
                                    .rotate(rotation)
                            )
                        }

                        AnimatedVisibility(visible = expanded) {
                            Column(
                                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                HorizontalDivider()
                                group.items.forEach { item ->
                                    MemoryGroupRow(item = item)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MemoryGroupRow(item: MemoryOverviewItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = overviewCategoryLabel(item.memory.category),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = item.memory.content,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

private fun relationLabel(type: String): String = when (type) {
    "bestie" -> "闺蜜"
    "teacher" -> "老师"
    "friend" -> "朋友"
    "family" -> "家人"
    else -> "自定义"
}
