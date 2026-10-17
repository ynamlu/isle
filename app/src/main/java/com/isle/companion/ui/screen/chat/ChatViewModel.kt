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

        viewModelScope.launch {
            val character = _character.value ?: return@launch
            val settings = container.settingsRepository.settings.first()
            if (!settings.isConfigured) {
                _error.value = "请先在设置中填写 DeepSeek API Key"
                return@launch
            }

            val history = container.chatRepository.recent(characterId, HISTORY_LIMIT)
            container.chatRepository.addUserMessage(characterId, text, imagePath)
            // 直接用插入返回的 id 回填，避免依赖同一毫秒内的时间戳排序
            val assistantId = container.chatRepository.addAssistantMessage(characterId)

            _input.value = ""
            _attachedImage.value = null
            _isStreaming.value = true
            _messages.value = container.chatRepository.recent(characterId, MESSAGE_LIMIT)
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

            // [DONE] 与 onClosed 可能先后各触发一次，只允许结算一次
            val finished = AtomicBoolean(false)
            var accumulated = ""
            eventSource = container.deepSeekClient.stream(
                settings = settings,
                messages = conversation,
                onDelta = { delta ->
                    accumulated += delta
                    updateAssistant(assistantId, accumulated)
                },
                onComplete = {
                    if (!finished.compareAndSet(false, true)) return@stream
                    viewModelScope.launch {
                        container.chatRepository.updateContent(assistantId, accumulated)
                        _isStreaming.value = false
                        eventSource = null
                        extractMemories(text, accumulated, userProfile)
                    }
                },
                onError = { throwable ->
                    if (!finished.compareAndSet(false, true)) return@stream
                    viewModelScope.launch {
                        val message = accumulated.ifBlank { "抱歉，回复失败了：${throwable.message}" }
                        container.chatRepository.updateContent(assistantId, message)
                        updateAssistant(assistantId, message)
                        _isStreaming.value = false
                        _error.value = throwable.message ?: "网络请求失败"
                        eventSource = null
                    }
                }
            )
        }
    }

    private fun updateAssistant(id: Long, content: String) {
        _messages.update { list ->
            list.map { if (it.id == id) it.copy(content = content) else it }
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

    override fun onCleared() {
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
