package com.isle.companion.ui.screen.chat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.isle.companion.BuildConfig
import com.isle.companion.data.local.entity.CharacterEntity
import com.isle.companion.data.local.entity.MessageEntity
import com.isle.companion.data.prefs.UserProfile
import com.isle.companion.data.remote.OpenAiException
import com.isle.companion.di.AppContainer
import com.isle.companion.domain.chat.AgentAction
import com.isle.companion.domain.chat.ConversationOrchestrator
import com.isle.companion.domain.journal.JournalEntry
import com.isle.companion.domain.prompt.PromptBuilder
import com.isle.companion.domain.safety.ConversationMode
import com.isle.companion.domain.safety.SafetyAlert
import com.isle.companion.domain.safety.ResponseValidator
import com.isle.companion.domain.safety.SafetyAssessment
import com.isle.companion.domain.safety.SafetyEngine
import com.isle.companion.domain.safety.ValidationIssue
import com.isle.companion.domain.scenario.Scenario
import com.isle.companion.domain.scenario.ScenarioCatalog
import com.isle.companion.ui.journal.JournalImageGenerator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.sse.EventSource
import java.io.File
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.atomic.AtomicBoolean

class ChatViewModel(
    private val container: AppContainer,
    private val characterId: Long,
    scenarioId: String? = null
) : ViewModel() {

    private val scenario: Scenario? = ScenarioCatalog.byId(scenarioId)

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

    private val _mode = MutableStateFlow(
        if (scenario != null) ConversationMode.SCENARIO else ConversationMode.FREE
    )
    val mode: StateFlow<ConversationMode> = _mode.asStateFlow()

    private val _assessment = MutableStateFlow<SafetyAssessment?>(null)
    val assessment: StateFlow<SafetyAssessment?> = _assessment.asStateFlow()

    private val _safetyAlert = MutableStateFlow<SafetyAlert?>(null)
    val safetyAlert: StateFlow<SafetyAlert?> = _safetyAlert.asStateFlow()

    private val _agentTrace = MutableStateFlow<String?>(null)
    val agentTrace: StateFlow<String?> = _agentTrace.asStateFlow()

    private val _journalGenerating = MutableStateFlow(false)
    val journalGenerating: StateFlow<Boolean> = _journalGenerating.asStateFlow()

    private val _journalImage = MutableStateFlow<File?>(null)
    val journalImage: StateFlow<File?> = _journalImage.asStateFlow()

    private val _journalError = MutableStateFlow<String?>(null)
    val journalError: StateFlow<String?> = _journalError.asStateFlow()

    val activeScenario: Scenario? get() = scenario

    // 聊天图片保留较高分辨率，避免点开大图时糊
    private val chatImages = com.isle.companion.util.AvatarStorage(
        context = container.context,
        folder = "chat_images",
        maxSize = CHAT_IMAGE_MAX_SIZE
    )

    private var eventSource: EventSource? = null
    private var scenarioStep = 0
    private var streamingAssistantId: Long? = null
    private var streamingText: String = ""
    private var streamingFinished: AtomicBoolean? = null

    init {
        viewModelScope.launch {
            val entity = container.characterRepository.get(characterId)
            _character.value = entity
            _messages.value = container.chatRepository.recent(characterId, MESSAGE_LIMIT)

            // 场景优先：直接由 AI 开口，用户不用先想“从哪说起”
            val hasScenarioOpening = scenario != null &&
                _messages.value.any { it.role == "assistant" && it.content == scenario.opening }
            if (scenario != null && !hasScenarioOpening) {
                container.chatRepository.addAssistantMessage(characterId, scenario.opening)
                _messages.value = container.chatRepository.recent(characterId, MESSAGE_LIMIT)
            } else if (_messages.value.isEmpty() && entity != null && entity.greeting.isNotBlank()) {
                // 首次进入时发送角色开场白
                container.chatRepository.addAssistantMessage(characterId, entity.greeting)
                _messages.value = container.chatRepository.recent(characterId, MESSAGE_LIMIT)
            }
            _agentTrace.value = "场景：${scenario?.title ?: "自由聊天"} · 等待输入"
        }
    }

    fun setMode(value: ConversationMode) {
        if (value == ConversationMode.CRISIS) return
        _mode.value = value
        _safetyAlert.value = null
    }

    fun dismissSafetyAlert() {
        _safetyAlert.value = null
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
                val validation = ResponseValidator.validate(text, _mode.value)
                val finalText = if (validation.hasSevereIssue) {
                    ResponseValidator.fallback(validation.issues)
                } else {
                    text
                }
                if (finalText.isBlank()) {
                    container.chatRepository.deleteMessage(id)
                } else {
                    container.chatRepository.updateContent(id, finalText)
                }
                _messages.value = container.chatRepository.recent(characterId, MESSAGE_LIMIT)
            }
        }
    }

    /**
     * 结束一段对话，提炼金句并生成情绪手账图。
     * 有网络时由 LLM 提炼；失败或未配置模型时回退到最后一条助手回复。
     */
    fun generateJournal() {
        if (_isStreaming.value || _journalGenerating.value) return
        viewModelScope.launch {
            val messages = _messages.value
            val userTurns = messages.count { it.role == "user" && it.content.isNotBlank() }
            if (userTurns < 2) {
                _journalError.value = "再多聊几句，才能提炼出属于你的金句"
                return@launch
            }
            if (messages.takeLast(20).any {
                    it.role == "user" && SafetyEngine.assess(it.content).needsSafetyIntervention
                }
            ) {
                _journalError.value = "这段对话涉及高风险内容，先照顾好自己，暂不生成手账"
                return@launch
            }

            _journalGenerating.value = true
            _journalError.value = null
            try {
                val character = _character.value ?: return@launch
                val settings = container.settingsRepository.settings.first()
                val entry = withContext(Dispatchers.IO) {
                    if (settings.isConfigured) {
                        runCatching {
                            container.journalExtractor.extract(
                                settings = settings,
                                characterName = character.name,
                                messages = messages
                            )
                        }.getOrNull() ?: container.journalExtractor.fallback(messages)
                    } else {
                        container.journalExtractor.fallback(messages)
                    }
                }
                val file = withContext(Dispatchers.IO) {
                    JournalImageGenerator.generate(
                        context = container.context,
                        entry = entry,
                        characterName = character.name,
                        qrUrl = BuildConfig.SHARE_QR_URL
                    )
                }
                _journalImage.value = file
            } catch (t: Throwable) {
                _journalError.value = "手账生成失败，请稍后重试"
            } finally {
                _journalGenerating.value = false
            }
        }
    }

    fun dismissJournal() {
        _journalImage.value?.delete()
        _journalImage.value = null
    }

    fun dismissJournalError() {
        _journalError.value = null
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
        val plan = ConversationOrchestrator.plan(
            text = text,
            requestedMode = _mode.value,
            scenario = scenario
        )
        val currentAssessment = plan.assessment
        _assessment.value = currentAssessment

        // 高危/紧急：不调用模型，不用角色扮演掩盖风险
        if (plan.action != AgentAction.NORMAL) {
            if (_isStreaming.value) return
            if (appendUser) {
                container.chatRepository.addUserMessage(characterId, text, imagePath)
                _input.value = ""
                _attachedImage.value = null
            }
            val crisis = plan.action == AgentAction.CRISIS
            val reply = if (crisis) SafetyEngine.crisisMessage() else SafetyEngine.supportiveMessage()
            container.chatRepository.addAssistantMessage(characterId, reply)
            _messages.value = container.chatRepository.recent(characterId, MESSAGE_LIMIT)
            _mode.value = plan.mode
            _safetyAlert.value = SafetyAlert(
                title = if (crisis) "先确认你的安全" else "我很在意你现在的状态",
                message = reply,
                isCrisis = crisis
            )
            _agentTrace.value = plan.trace + " · 未调用模型"
            return
        }

        if (!settings.isConfigured) {
            _error.value = "请先在设置中填写模型服务的 API Key 与接口地址"
            return
        }
        if (_isStreaming.value) return
        _mode.value = plan.mode

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
            mode = _mode.value,
            scenario = scenario,
            assessment = currentAssessment,
            scenarioStep = scenarioStep
        )
        _agentTrace.value = plan.trace + " · 人设：${character.name}"

        eventSource = container.openAiClient.stream(
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
                    val validation = ResponseValidator.validate(streamingText, _mode.value)
                    val finalText = if (validation.hasSevereIssue) {
                        ResponseValidator.fallback(validation.issues)
                    } else {
                        streamingText
                    }
                    if (finalText.isBlank()) {
                        container.chatRepository.deleteMessage(assistantId)
                    } else {
                        container.chatRepository.updateContent(assistantId, finalText)
                    }
                    _messages.value = container.chatRepository.recent(characterId, MESSAGE_LIMIT)
                    _isStreaming.value = false
                    eventSource = null
                    streamingAssistantId = null
                    _agentTrace.value = listOfNotNull(_agentTrace.value, validation.summary).joinToString(" · ")
                    if (ValidationIssue.UNSAFE_METHOD in validation.issues) {
                        _safetyAlert.value = SafetyAlert(
                            title = "安全提示",
                            message = ResponseValidator.fallback(validation.issues),
                            isCrisis = true
                        )
                    }
                    if (finalText.isNotBlank() &&
                        _mode.value != ConversationMode.CRISIS &&
                        _mode.value != ConversationMode.SAFETY &&
                        !validation.hasSevereIssue
                    ) {
                        extractMemories(text, finalText, userProfile)
                    }
                    if (_mode.value == ConversationMode.SCENARIO && scenario != null &&
                        scenario.steps.isNotEmpty()
                    ) {
                        scenarioStep = (scenarioStep + 1) % scenario.steps.size
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
                userProfile = userProfile
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
            raw.contains("403") -> "没有访问权限，请检查 API Key 或模型权限"
            raw.contains("404") -> "接口地址或模型不存在，请到设置页检查"
            raw.contains("402") -> "账户余额不足，请充值后重试"
            raw.contains("429") -> "请求过于频繁，请稍后再试"
            raw.contains("500") || raw.contains("502") || raw.contains("503") ->
                "服务暂时不可用，请稍后重试"
            t is OpenAiException -> raw.ifBlank { "请求失败" }
            t is IllegalArgumentException -> raw.ifBlank { "API 地址格式不正确" }
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
