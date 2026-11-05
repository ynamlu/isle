package com.isle.companion.ui.screen.memory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.isle.companion.ui.theme.liquidGlass
import com.isle.companion.ui.theme.liquidGlassSource
import com.isle.companion.ui.theme.rememberLiquidGlassState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.isle.companion.ui.theme.isleBackground
import com.isle.companion.ui.theme.GlassCard
import com.isle.companion.di.appContainer
import com.isle.companion.data.local.entity.MemoryEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryScreen(
    characterId: Long,
    onBack: () -> Unit
) {
    val container = LocalContext.current.appContainer
    val viewModel: MemoryViewModel = viewModel(
        key = "memory_$characterId",
        factory = viewModelFactory { initializer { MemoryViewModel(container, characterId) } }
    )
    val memories by viewModel.memories.collectAsState()
    val allMemories by viewModel.allMemories.collectAsState()
    val query by viewModel.query.collectAsState()
    var showClearConfirm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<MemoryEntity?>(null) }
    var editText by remember { mutableStateOf("") }

    val weekStart = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
    val weekNew = allMemories.count { it.createdAt >= weekStart }
    val topCategory = allMemories
        .groupingBy { categoryLabel(it.category) }
        .eachCount()
        .maxByOrNull { it.value }
        ?.key

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("清空记忆") },
            text = { Text("将删除该角色的全部长期记忆，且无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showClearConfirm = false
                    viewModel.clearAll()
                }) { Text("清空") }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("取消") }
            }
        )
    }

    editing?.let { memory ->
        AlertDialog(
            onDismissRequest = { editing = null },
            title = { Text("编辑记忆") },
            text = {
                OutlinedTextField(
                    value = editText,
                    onValueChange = { editText = it },
                    label = { Text("TA 记住的事") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.update(memory, editText)
                    editing = null
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { editing = null }) { Text("取消") }
            }
        )
    }

    val hazeState = rememberLiquidGlassState()

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                modifier = Modifier.liquidGlass(hazeState),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                title = { Text("记忆管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (memories.isNotEmpty()) {
                        IconButton(onClick = { showClearConfirm = true }) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "清空记忆")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (memories.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .isleBackground()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("还没有长期记忆", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "多聊几句，TA 就会慢慢记住你的事",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .isleBackground()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("本周回顾", style = MaterialTheme.typography.titleSmall)
                        Text(
                            text = if (weekNew == 0) {
                                "这周还没有新的记忆，多聊几句吧"
                            } else {
                                "这周新记住 $weekNew 件事" +
                                    (topCategory?.let { " · 最常出现：$it" } ?: "")
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = viewModel::updateQuery,
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text("搜索 TA 记住的事") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (memories.isEmpty()) {
                item {
                    Text(
                        text = if (query.isBlank()) "还没有长期记忆" else "没有找到相关记忆",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            items(memories, key = { it.id }) { memory ->
                GlassCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            AssistChip(
                                onClick = {},
                                label = { Text(categoryLabel(memory.category)) }
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = memory.content,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                        IconButton(onClick = {
                            editing = memory
                            editText = memory.content
                        }) {
                            Icon(Icons.Filled.Edit, contentDescription = "编辑")
                        }
                        IconButton(onClick = { viewModel.delete(memory) }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "删除")
                        }
                    }
                }
            }
        }
    }
}
