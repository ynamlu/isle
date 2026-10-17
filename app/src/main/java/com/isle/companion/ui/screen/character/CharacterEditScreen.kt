package com.isle.companion.ui.screen.character

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.sp
import com.isle.companion.ui.theme.IsleRadius
import com.isle.companion.ui.theme.liquidGlass
import com.isle.companion.ui.theme.liquidGlassSource
import com.isle.companion.ui.theme.rememberLiquidGlassState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.isle.companion.ui.theme.isleBackground
import com.isle.companion.di.appContainer
import com.isle.companion.ui.component.CharacterAvatar
import com.isle.companion.ui.component.rememberImagePicker

private val RELATION_OPTIONS = listOf(
    "love" to "恋人",
    "bestie" to "闺蜜",
    "friend" to "朋友",
    "family" to "家人",
    "custom" to "自定义"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CharacterEditScreen(
    characterId: Long,
    onBack: () -> Unit
) {
    val container = LocalContext.current.appContainer
    val viewModel: CharacterEditViewModel = viewModel(
        key = "edit_$characterId",
        factory = viewModelFactory { initializer { CharacterEditViewModel(container, characterId) } }
    )
    val state by viewModel.state.collectAsState()
    val profile by container.userProfileRepository.profile
        .collectAsState(initial = com.isle.companion.data.prefs.UserProfile())
    // 青少年模式不开放「恋人」「自定义」定位
    val relationOptions = if (profile.isMinor) {
        RELATION_OPTIONS.filter { it.first != "love" && it.first != "custom" }
    } else {
        RELATION_OPTIONS
    }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val pickImage = rememberImagePicker { uri -> viewModel.importAvatar(uri) }



    LaunchedEffect(state.saved, state.deleted) {
        if (state.saved || state.deleted) onBack()
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除角色") },
            text = { Text("删除后该角色的聊天记录与记忆将一并清除，且无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete()
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") }
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
                title = { Text(if (state.isNew) "创建角色" else "编辑角色") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (state.canDelete) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(Icons.Filled.Delete, contentDescription = "删除")
                        }
                    }
                    IconButton(onClick = viewModel::save, enabled = state.canSave) {
                        Icon(Icons.Filled.Check, contentDescription = "保存")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .isleBackground()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CharacterAvatar(
                        avatarPath = state.avatarPath,
                        name = state.name.ifBlank { "屿" },
                        size = 96.dp,
                        modifier = Modifier.clickable { pickImage() }
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "点击上传头像",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            OutlinedTextField(
                shape = IsleRadius.field,
                value = state.name,
                onValueChange = viewModel::updateName,
                label = { Text("角色名称 *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("关系类型", style = MaterialTheme.typography.labelLarge)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    relationOptions.take(3).forEach { (value, label) ->
                        FilterChip(
                            selected = state.relationType == value,
                            onClick = { viewModel.updateRelationType(value) },
                            label = { Text(label) }
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    relationOptions.drop(3).forEach { (value, label) ->
                        FilterChip(
                            selected = state.relationType == value,
                            onClick = { viewModel.updateRelationType(value) },
                            label = { Text(label) }
                        )
                    }
                }
            }

            OutlinedTextField(
                shape = IsleRadius.field,
                value = state.personality,
                onValueChange = viewModel::updatePersonality,
                label = { Text("性格描述") },
                placeholder = { Text("例如：温柔、幽默、有点小傲娇") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                shape = IsleRadius.field,
                value = state.bio,
                onValueChange = viewModel::updateBio,
                label = { Text("简介 / 背景故事") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                shape = IsleRadius.field,
                value = state.speakingStyle,
                onValueChange = viewModel::updateSpeakingStyle,
                label = { Text("说话风格") },
                placeholder = { Text("例如：喜欢叫我「笨蛋」，语气轻快") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                shape = IsleRadius.field,
                value = state.greeting,
                onValueChange = viewModel::updateGreeting,
                label = { Text("开场白") },
                placeholder = { Text("例如：嗨，今天过得怎么样呀～") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = viewModel::save,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("保存")
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
