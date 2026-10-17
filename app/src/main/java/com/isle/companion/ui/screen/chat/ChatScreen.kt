package com.isle.companion.ui.screen.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.isle.companion.ui.theme.IsleRadius
import com.isle.companion.ui.theme.liquidGlass
import com.isle.companion.ui.theme.liquidGlassSource
import com.isle.companion.ui.theme.rememberLiquidGlassState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil3.compose.AsyncImage
import com.isle.companion.data.local.entity.MessageEntity
import com.isle.companion.di.appContainer
import com.isle.companion.ui.component.CharacterAvatar
import com.isle.companion.ui.component.ImageViewerDialog
import com.isle.companion.ui.component.rememberImagePicker
import com.isle.companion.ui.theme.IsleUserBubbleBrush
import com.isle.companion.ui.theme.isleBackground
import com.isle.companion.ui.theme.rememberGlassColors
import com.isle.companion.ui.theme.relationColor
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    characterId: Long,
    onBack: () -> Unit,
    onEditCharacter: () -> Unit,
    onOpenMemory: () -> Unit
) {
    val container = LocalContext.current.appContainer
    val viewModel: ChatViewModel = viewModel(
        key = "chat_$characterId",
        factory = viewModelFactory { initializer { ChatViewModel(container, characterId) } }
    )

    val character by viewModel.character.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val input by viewModel.input.collectAsState()
    val attachedImage by viewModel.attachedImage.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val error by viewModel.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val pickImage = rememberImagePicker(title = "发送图片") { uri -> viewModel.attachImage(uri) }

    var viewerImage by remember { mutableStateOf<String?>(null) }
    val hazeState = rememberLiquidGlassState()

    val accent = relationColor(character?.relationType ?: "custom")

    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    LaunchedEffect(messages.size, messages.lastOrNull()?.content) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    ImageViewerDialog(imagePath = viewerImage, onDismiss = { viewerImage = null })

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                modifier = Modifier.liquidGlass(hazeState),
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CharacterAvatar(
                            avatarPath = character?.avatarPath,
                            name = character?.name ?: "心屿",
                            size = 38.dp,
                            ringColor = accent
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = character?.name ?: "",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (isStreaming) "正在输入…" else relationLabelOf(character?.relationType),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isStreaming) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    accent
                                }
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onOpenMemory) {
                        Icon(Icons.Filled.Psychology, contentDescription = "长期记忆")
                    }
                    IconButton(onClick = onEditCharacter) {
                        Icon(Icons.Filled.Edit, contentDescription = "编辑角色")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .isleBackground()
                .imePadding()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .liquidGlassSource(hazeState),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = padding.calculateTopPadding() + 10.dp,
                    bottom = 16.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(messages, key = { it.id }) { message ->
                    MessageRow(
                        message = message,
                        avatarPath = character?.avatarPath,
                        characterName = character?.name ?: "",
                        accent = accent,
                        onImageClick = { path -> viewerImage = path }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // 待发送图片预览
            if (attachedImage != null) {
                Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                    AsyncImage(
                        model = File(attachedImage!!),
                        contentDescription = "待发送图片",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(76.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewerImage = attachedImage }
                    )
                    IconButton(
                        onClick = viewModel::clearAttachment,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "移除图片")
                    }
                }
            }

            // 输入区
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = pickImage) {
                        Icon(
                            Icons.Filled.AddPhotoAlternate,
                            contentDescription = "添加图片",
                            tint = accent
                        )
                    }

                    OutlinedTextField(
                        shape = IsleRadius.field,
                        value = input,
                        onValueChange = viewModel::updateInput,
                        placeholder = { Text("说点什么…") },
                        modifier = Modifier.weight(1f),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Spacer(Modifier.width(8.dp))

                    val canSend = !isStreaming && (input.isNotBlank() || attachedImage != null)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                if (canSend) {
                                    IsleUserBubbleBrush
                                } else {
                                    androidx.compose.ui.graphics.SolidColor(
                                        MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            )
                            .clickable(enabled = canSend) { viewModel.send() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "发送",
                            tint = if (canSend) {
                                Color.White
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageRow(
    message: MessageEntity,
    avatarPath: String?,
    characterName: String,
    accent: Color,
    onImageClick: (String) -> Unit
) {
    val isUser = message.role == "user"

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            CharacterAvatar(
                avatarPath = avatarPath,
                name = characterName,
                size = 36.dp,
                ringColor = accent
            )
            Spacer(Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.fillMaxWidth(0.78f)
        ) {
            if (isUser && message.imagePath != null) {
                AsyncImage(
                    model = File(message.imagePath),
                    contentDescription = "图片",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .height(150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onImageClick(message.imagePath) }
                )
                Spacer(Modifier.height(6.dp))
            }

            if (message.content.isNotBlank()) {
                if (isUser) {
                    Box(
                        modifier = Modifier
                            .clip(
                                RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 6.dp,
                                    bottomEnd = 18.dp,
                                    bottomStart = 18.dp
                                )
                            )
                            .background(IsleUserBubbleBrush)
                    ) {
                        Text(
                            text = message.content,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    val bubbleShape = RoundedCornerShape(
                        topStart = 6.dp,
                        topEnd = 18.dp,
                        bottomEnd = 18.dp,
                        bottomStart = 18.dp
                    )
                    val glass = rememberGlassColors()
                    Box(
                        modifier = Modifier
                            .clip(bubbleShape)
                            .background(brush = glass.fill)
                            .border(width = 1.dp, color = glass.border, shape = bubbleShape)
                    ) {
                        Text(
                            text = message.content,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 11.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

private fun relationLabelOf(type: String?): String = when (type) {
    "love" -> "恋人"
    "bestie" -> "闺蜜"
    "friend" -> "朋友"
    "family" -> "家人"
    else -> "自定义关系"
}
