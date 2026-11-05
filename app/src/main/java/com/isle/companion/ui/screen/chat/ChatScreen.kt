package com.isle.companion.ui.screen.chat

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.isle.companion.ui.theme.IsleRadius
import com.isle.companion.ui.theme.liquidGlass
import com.isle.companion.ui.theme.liquidGlassSource
import com.isle.companion.ui.theme.rememberLiquidGlassState
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import coil3.compose.AsyncImage
import com.isle.companion.data.local.entity.MessageEntity
import com.isle.companion.di.appContainer
import com.isle.companion.domain.safety.ConversationMode
import com.isle.companion.domain.safety.SafetyEngine
import com.isle.companion.ui.component.CharacterAvatar
import com.isle.companion.ui.component.keyboardRetryOnFocus
import com.isle.companion.ui.component.ImageViewerDialog
import com.isle.companion.ui.component.rememberImagePicker
import com.isle.companion.ui.journal.JournalSaver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.isle.companion.ui.theme.IsleUserBubbleBrush
import com.isle.companion.ui.theme.isleBackground
import com.isle.companion.ui.theme.rememberGlassColors
import com.isle.companion.ui.theme.relationColor
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    characterId: Long,
    scenarioId: String?,
    onBack: () -> Unit,
    onEditCharacter: () -> Unit
) {
    val container = LocalContext.current.appContainer
    val context = LocalContext.current
    val viewModel: ChatViewModel = viewModel(
        key = "chat_${characterId}_${scenarioId ?: "free"}",
        factory = viewModelFactory {
            initializer { ChatViewModel(container, characterId, scenarioId) }
        }
    )

    val character by viewModel.character.collectAsState()
    val messages by viewModel.messages.collectAsState()
    val input by viewModel.input.collectAsState()
    val attachedImage by viewModel.attachedImage.collectAsState()
    val isStreaming by viewModel.isStreaming.collectAsState()
    val error by viewModel.error.collectAsState()
    val mode by viewModel.mode.collectAsState()
    val assessment by viewModel.assessment.collectAsState()
    val safetyAlert by viewModel.safetyAlert.collectAsState()
    val agentTrace by viewModel.agentTrace.collectAsState()
    val journalGenerating by viewModel.journalGenerating.collectAsState()
    val journalImage by viewModel.journalImage.collectAsState()
    val journalError by viewModel.journalError.collectAsState()
    val scenario = viewModel.activeScenario
    val scope = rememberCoroutineScope()

    fun saveJournal(file: File) {
        scope.launch {
            val ok = withContext(Dispatchers.IO) { JournalSaver.saveToGallery(context, file) }
            Toast.makeText(
                context,
                if (ok) "已保存到相册：Pictures/心屿" else "保存失败，请稍后重试",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val file = journalImage
        if (granted && file != null) {
            saveJournal(file)
        } else if (!granted) {
            Toast.makeText(context, "需要存储权限才能保存图片", Toast.LENGTH_SHORT).show()
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val pickImage = rememberImagePicker(title = "发送图片") { uri -> viewModel.attachImage(uri) }

    var viewerImage by remember { mutableStateOf<String?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }
    var showTraceDialog by remember { mutableStateOf(false) }
    val hazeState = rememberLiquidGlassState()

    val accent = relationColor(character?.relationType ?: "custom")

    LaunchedEffect(journalError) {
        journalError?.let {
            snackbarHostState.showSnackbar(it, withDismissAction = true)
            viewModel.dismissJournalError()
        }
    }

    LaunchedEffect(error) {
        error?.let {
            val result = snackbarHostState.showSnackbar(
                message = it,
                actionLabel = "重试",
                withDismissAction = true,
                duration = SnackbarDuration.Long
            )
            viewModel.dismissError()
            if (result == SnackbarResult.ActionPerformed) viewModel.retryLast()
        }
    }

    // 只有用户本来就在底部时才跟随流式输出，避免把正在上滑看历史的人强行拉回
    val shouldFollow by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index
                ?: messages.lastIndex
            messages.isEmpty() || lastVisible >= messages.lastIndex - 1
        }
    }
    LaunchedEffect(messages.size) {
        // 新增消息（用户发送 / 助手占位）时总是滚到底部
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }
    LaunchedEffect(messages.lastOrNull()?.content) {
        if (isStreaming && shouldFollow && messages.isNotEmpty()) {
            listState.scrollToItem(messages.lastIndex)
        }
    }

    ImageViewerDialog(imagePath = viewerImage, onDismiss = { viewerImage = null })

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("清空会话") },
            text = { Text("将删除当前角色的全部聊天记录和图片，且无法恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    viewModel.clearChat()
                }) { Text("清空") }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("取消") }
            }
        )
    }

    safetyAlert?.let { alert ->
        AlertDialog(
            onDismissRequest = viewModel::dismissSafetyAlert,
            title = { Text(alert.title) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(alert.message)
                    Text(
                        text = SafetyEngine.disclaimer(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissSafetyAlert) { Text("我知道了") }
            },
            dismissButton = if (alert.isCrisis) {
                {
                    TextButton(onClick = {
                        runCatching {
                            context.startActivity(
                                android.content.Intent(
                                    android.content.Intent.ACTION_DIAL,
                                    android.net.Uri.parse("tel:12356")
                                )
                            )
                        }
                    }) { Text("拨打 12356") }
                }
            } else {
                null
            }
        )
    }

    if (showTraceDialog) {
        AlertDialog(
            onDismissRequest = { showTraceDialog = false },
            title = { Text("这次回复是怎么来的") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(agentTrace ?: "还没有生成记录")
                    Text(
                        text = "本机只展示结构化决策，不展示模型内部推理。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showTraceDialog = false }) { Text("知道了") }
            }
        )
    }

    if (journalGenerating) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("正在生成情绪手账") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(12.dp))
                    Text("把这段对话里值得留下的那句话，收进一张手账。")
                }
            },
            confirmButton = {}
        )
    }

    journalImage?.let { file ->
        Dialog(
            onDismissRequest = viewModel::dismissJournal,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AsyncImage(
                        model = file,
                        contentDescription = "情绪手账",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 620.dp)
                            .clip(RoundedCornerShape(18.dp))
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(
                            onClick = viewModel::dismissJournal,
                            modifier = Modifier.weight(1f)
                        ) { Text("关闭") }
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    storagePermissionLauncher.launch(
                                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                                    )
                                } else {
                                    saveJournal(file)
                                }
                            },
                            modifier = Modifier.weight(1.4f)
                        ) { Text("保存图片") }
                    }
                }
            }
        }
    }

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
                                text = when {
                                    isStreaming -> "正在输入…"
                                    scenario != null && mode == ConversationMode.SCENARIO ->
                                        "场景 · ${scenario.title}"
                                    mode == ConversationMode.CRISIS || mode == ConversationMode.SAFETY ->
                                        "安全模式 · ${mode.label}"
                                    else -> relationLabelOf(character?.relationType)
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = when {
                                    isStreaming -> MaterialTheme.colorScheme.primary
                                    mode == ConversationMode.SAFETY || mode == ConversationMode.CRISIS ->
                                        MaterialTheme.colorScheme.error
                                    else -> accent
                                }
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = viewModel::generateJournal,
                        enabled = !isStreaming && !journalGenerating
                    ) {
                        Icon(Icons.Filled.AutoAwesome, contentDescription = "生成情绪手账")
                    }
                    IconButton(onClick = { showTraceDialog = true }) {
                        Icon(Icons.Filled.Shield, contentDescription = "安全与生成说明")
                    }
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = "清空会话")
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

            // 快捷模式：把“想被倾听 / 想被分析 / 想慢下来”变成一键切换
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val modesEnabled = mode != ConversationMode.CRISIS && mode != ConversationMode.SAFETY
                    ModeChip("自由聊", mode == ConversationMode.FREE, modesEnabled) {
                        viewModel.setMode(ConversationMode.FREE)
                    }
                    ModeChip("听我吐槽", mode == ConversationMode.LISTEN, modesEnabled) {
                        viewModel.setMode(ConversationMode.LISTEN)
                    }
                    ModeChip("帮我分析", mode == ConversationMode.ANALYZE, modesEnabled) {
                        viewModel.setMode(ConversationMode.ANALYZE)
                    }
                    ModeChip("陪我呼吸", mode == ConversationMode.BREATHE, modesEnabled) {
                        viewModel.setMode(ConversationMode.BREATHE)
                    }
                    if (scenario != null) {
                        AssistChip(
                            onClick = {
                                if (modesEnabled) viewModel.setMode(ConversationMode.SCENARIO)
                            },
                            enabled = modesEnabled,
                            label = { Text("场景 · ${scenario.title}") }
                        )
                    }
                    AssistChip(
                        onClick = viewModel::generateJournal,
                        enabled = !isStreaming && !journalGenerating,
                        label = { Text("生成手账") }
                    )
                }
                Text(
                    text = when (mode) {
                        ConversationMode.LISTEN -> "树洞模式：只倾听，不打断，不评价"
                        ConversationMode.ANALYZE -> "分析模式：先接住情绪，再一起拆问题"
                        ConversationMode.BREATHE -> "呼吸模式：先花 30 秒让身体慢下来"
                        ConversationMode.SCENARIO -> "场景模式：跟着一个小步骤走"
                        ConversationMode.SAFETY, ConversationMode.CRISIS ->
                            "安全模式：角色扮演已暂停，先确认你的安全"
                        ConversationMode.FREE -> "自由聊：想说什么都可以"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (mode == ConversationMode.SAFETY || mode == ConversationMode.CRISIS) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                )
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
                        placeholder = {
                            Text(
                                when (mode) {
                                    ConversationMode.LISTEN -> "慢慢说，我在听…"
                                    ConversationMode.BREATHE -> "准备好了就告诉我"
                                    ConversationMode.CRISIS -> "先告诉我，你现在安全吗？"
                                    else -> "说点什么…"
                                }
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .keyboardRetryOnFocus(),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accent,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    )

                    Spacer(Modifier.width(8.dp))

                    if (isStreaming) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { viewModel.stopStreaming() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Stop,
                                contentDescription = "停止生成",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        val canSend = input.isNotBlank() || attachedImage != null
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
}

@Composable
private fun ModeChip(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        enabled = enabled,
        label = { Text(label) }
    )
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
    "love" -> "旧版角色"
    "bestie" -> "闺蜜"
    "teacher" -> "老师"
    "friend" -> "朋友"
    "family" -> "家人"
    else -> "自定义关系"
}
