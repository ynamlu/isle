package com.isle.companion.ui.screen.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isle.companion.data.local.entity.CharacterEntity
import com.isle.companion.data.local.entity.MessageEntity
import com.isle.companion.data.prefs.UserProfile
import com.isle.companion.di.AppContainer
import com.isle.companion.domain.prompt.PromptBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.sse.EventSource
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.atomic.AtomicBoolean

class ChatViewModel(
    private val container: AppContainer,
    private val characterId: Long
) : ViewModel() {

    private val _character = MutableStateFlow<CharacterEntity?>(null)
    val character: StateFlow<CharacterEntity?> = _character.asStateFlow()

    private val _messages = MutableStateFlow<List<MessageEntity>>(emptyList())
    val messages: StateFlow<List<MessageEntity>> = _messages.asStateFlow()

    private val _input = MutableStateFlow("")
    val input: StateFlow<String> = _input.asStateFlow()

    private val _attachedImage = MutableStateFlow<String?>(null)
    val attachedImage: StateFlow<String?> = _attachedImage.asStateFlow()

    private val _isStreaming = MutableStateFlow(false)
    val isStreaming: StateFlow<Boolean> = _isStreaming.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // 聊天图片保留较高分辨率，避免点开大图时糊
    private val chatImages = com.isle.companion.util.AvatarStorage(
        context = container.context,
        folder = "chat_images",
        maxSize = CHAT_IMAGE_MAX_SIZE
    )

    private var eventSource: EventSource? = null
    private var streamingAssistantId: Long? = null
    private var streamingText: String = ""
    private var streamingFinished: AtomicBoolean? = null

    init {
        viewModelScope.launch {
            val entity = container.characterRepository.get(characterId)
            _character.value = entity
            _messages.value = container.chatRepository.recent(characterId, MESSAGE_LIMIT)

            // 首次进入时发送角色开场白
            if (_messages.value.isEmpty() && entity != null && entity.greeting.isNotBlank()) {
                container.chatRepository.addAssistantMessage(characterId, entity.greeting)
                _messages.value = container.chatRepository.recent(characterId, MESSAGE_LIMIT)
            }
        }
    }

    fun updateInput(value: String) = _input.update { value }

    fun attachImage(uri: Uri) {
        viewModelScope.launch {
            chatImages.importFromUri(uri)?.let { path ->
                _attachedImage.update { old ->
                    if (old != null && old != path) chatImages.delete(old)
                    path
                }
            }
        }
    }

    fun clearAttachment() {
        chatImages.delete(_attachedImage.value)
        _attachedImage.value = null
    }

    fun dismissError() {
        _error.value = null
    }

    fun send() {
        val text = _input.value.trim()
        val imagePath = _attachedImage.value
        if (text.isEmpty() && imagePath == null) return
        if (_isStreaming.value) return
        viewModelScope.launch { sendInternal(text, imagePath, appendUser = true) }
    }

    /** 失败后重试最后一条用户消息，不重复写入用户消息。 */
    fun retryLast() {
        if (_isStreaming.value) return
        val lastUser = _messages.value.lastOrNull {
            it.role == "user" && (it.content.isNotBlank() || it.imagePath != null)
        } ?: return
        viewModelScope.launch {
            sendInternal(lastUser.content, lastUser.imagePath, appendUser = false)
        }
    }

    /** 主动停止生成：保留已经收到的部分内容。 */
    fun stopStreaming() {
        if (!_isStreaming.value) return
        streamingFinished?.set(true)
        eventSource?.cancel()
        eventSource = null
        val id = streamingAssistantId
        val text = streamingText
        streamingAssistantId = null
        _isStreaming.value = false
        viewModelScope.launch {
            if (id != null) {
                if (text.isBlank()) {
                    container.chatRepository.deleteMessage(id)
                } else {
                    container.chatRepository.updateContent(id, text)
                }
                _messages.value = container.chatRepository.recent(characterId, MESSAGE_LIMIT)
            }
        }
    }

    /** 清空当前角色的会话与聊天图片。 */
    fun clearChat() {
        if (_isStreaming.value) stopStreaming()
        viewModelScope.launch {
            container.chatRepository.imagePaths(characterId).forEach { path ->
                runCatching { File(path).takeIf { it.exists() }?.delete() }
            }
            container.chatRepository.clear(characterId)
            _messages.value = emptyList()
        }
    }

    private suspend fun sendInternal(text: String, imagePath: String?, appendUser: Boolean) {
        val character = _character.value ?: return
        val settings = container.settingsRepository.settings.first()
        if (!settings.isConfigured) {
            _error.value = "请先在设置中填写 DeepSeek API Key"
            return
        }
        if (_isStreaming.value) return

        // 重试时把上一轮失败的用户消息及其后的残留一起截掉，避免 Prompt 重复
        val history = container.chatRepository.recent(characterId, HISTORY_LIMIT).let { list ->
            if (appendUser) {
                list
            } else {
                val retryIndex = list.indexOfLast {
                    it.role == "user" && it.content == text && it.imagePath == imagePath
                }
                if (retryIndex >= 0) list.take(retryIndex) else list
            }
        }

        if (appendUser) {
            container.chatRepository.addUserMessage(characterId, text, imagePath)
            _input.value = ""
            _attachedImage.value = null
        }
        val assistantId = container.chatRepository.addAssistantMessage(characterId)
        _messages.value = container.chatRepository.recent(characterId, MESSAGE_LIMIT)

        _error.value = null
        _isStreaming.value = true
        streamingAssistantId = assistantId
        streamingText = ""
        val finished = AtomicBoolean(false)
        streamingFinished = finished

        val memories = container.memoryRepository.retrieveRelevant(characterId, text)
        val userProfile = container.userProfileRepository.profile.first()
        val conversation = PromptBuilder.buildConversation(
            character = character,
            memories = memories,
            history = history,
            userText = text.ifBlank { "（用户发来了一张图片）" },
            imageFile = imagePath?.let { File(it) },
            userProfile = userProfile,
            isMinor = userProfile.isMinor
        )

        eventSource = container.deepSeekClient.stream(
            settings = settings,
            messages = conversation,
            onDelta = { delta ->
                if (!finished.get()) {
                    streamingText += delta
                    updateAssistant(assistantId, streamingText)
                }
            },
            onComplete = {
                // [DONE] 与 onClosed 可能先后各触发一次，只允许结算一次
                if (!finished.compareAndSet(false, true)) return@stream
                viewModelScope.launch {
                    if (streamingText.isBlank()) {
                        container.chatRepository.deleteMessage(assistantId)
                    } else {
                        container.chatRepository.updateContent(assistantId, streamingText)
                    }
                    _messages.value = container.chatRepository.recent(characterId, MESSAGE_LIMIT)
                    _isStreaming.value = false
                    eventSource = null
                    streamingAssistantId = null
                    if (streamingText.isNotBlank()) {
                        extractMemories(text, streamingText, userProfile)
                    }
                }
            },
            onError = { throwable ->
                if (!finished.compareAndSet(false, true)) return@stream
                viewModelScope.launch {
                    // 不把错误文案写成助手回复；删除占位消息，由用户点“重试”
                    container.chatRepository.deleteMessage(assistantId)
                    _messages.value = container.chatRepository.recent(characterId, MESSAGE_LIMIT)
                    _isStreaming.value = false
                    eventSource = null
                    streamingAssistantId = null
                    _error.value = friendlyError(throwable)
                }
            }
        )
    }

    private fun updateAssistant(id: Long, content: String) {
        _messages.update { list ->
            val index = list.indexOfFirst { it.id == id }
            if (index < 0) {
                list
            } else {
                list.toMutableList().apply { this[index] = this[index].copy(content = content) }
            }
        }
    }

    /**
     * 一轮对话结束后，异步抽取长期记忆并写入本地（含端侧向量）。
     */
    private suspend fun extractMemories(
        userText: String,
        assistantText: String,
        userProfile: UserProfile
    ) {
        val character = _character.value ?: return
        if (userText.isBlank() || assistantText.isBlank()) return
        val settings = container.settingsRepository.settings.first()
        if (!settings.isConfigured) return

        runCatching {
            val extracted = container.memoryExtractor.extract(
                settings = settings,
                characterName = character.name,
                userText = userText,
                assistantText = assistantText,
                userProfile = userProfile,
                isMinor = userProfile.isMinor
            )
            extracted.forEach { memory ->
                container.memoryRepository.add(
                    characterId = characterId,
                    content = memory.content,
                    category = memory.category,
                    importance = memory.importance
                )
            }
        }
    }

    private fun friendlyError(t: Throwable): String {
        val raw = t.message.orEmpty()
        return when {
            t is UnknownHostException -> "网络连接失败，请检查网络后重试"
            t is SocketTimeoutException || t is InterruptedIOException -> "连接超时，请重试"
            raw.contains("401") -> "API Key 无效，请到设置页检查"
            raw.contains("402") -> "账户余额不足，请充值后重试"
            raw.contains("429") -> "请求过于频繁，请稍后再试"
            raw.contains("500") || raw.contains("502") || raw.contains("503") ->
                "服务暂时不可用，请稍后重试"
            t is IOException -> "网络异常，请重试"
            else -> "请求失败：${raw.ifBlank { "未知错误" }}"
        }
    }

    override fun onCleared() {
        streamingFinished?.set(true)
        eventSource?.cancel()
        eventSource = null
        super.onCleared()
    }

    companion object {
        private const val MESSAGE_LIMIT = 200
        private const val HISTORY_LIMIT = 12
        private const val CHAT_IMAGE_MAX_SIZE = 1600
    }
}
