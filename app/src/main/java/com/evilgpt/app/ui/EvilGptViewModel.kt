package com.evilgpt.app.ui

import android.app.Application
import android.net.Uri
import com.evilgpt.app.data.ChatAttachment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.evilgpt.app.data.AiModel
import com.evilgpt.app.data.ChatMessage
import com.evilgpt.app.data.ChatSession
import com.evilgpt.app.data.EvilGptApi
import com.evilgpt.app.data.EvilGptModels
import com.evilgpt.app.data.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

 data class AppState(
    val user: UserSession? = null,
    val loading: Boolean = true,
    val busy: Boolean = false,
    val sessions: List<ChatSession> = emptyList(),
    val activeSessionId: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val pendingAttachments: List<ChatAttachment> = emptyList(),
    val models: List<AiModel> = EvilGptModels.fallback,
    val selectedModel: String = "model-22",
    val webSearch: Boolean = false,
    val deepThink: Boolean = false,
    val privateMode: Boolean = false,
    val status: String = "",
    val error: String? = null
)

class EvilGptViewModel(application: Application) : AndroidViewModel(application) {
    private val api = EvilGptApi(application)
    private val _state = MutableStateFlow(AppState(user = api.currentSession()))
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        val user = api.currentSession()
        if (user == null) _state.update { it.copy(loading = false) }
        else refreshHome()
    }

    fun signIn(email: String, password: String) = viewModelScope.launch {
        _state.update { it.copy(busy = true, error = null) }
        try {
            val user = api.signIn(email, password)
            _state.update { it.copy(user = user, busy = false, loading = true) }
            refreshHome()
        } catch (e: Exception) {
            _state.update { it.copy(busy = false, loading = false, error = e.message ?: "Sign in failed.") }
        }
    }

    fun signUp(email: String, password: String) = viewModelScope.launch {
        _state.update { it.copy(busy = true, error = null) }
        try {
            val user = api.signUp(email, password)
            if (user == null) {
                _state.update { it.copy(busy = false, error = "Account created. Check your email to confirm it, then sign in.") }
            } else {
                _state.update { it.copy(user = user, busy = false, loading = true) }
                refreshHome()
            }
        } catch (e: Exception) {
            _state.update { it.copy(busy = false, error = e.message ?: "Sign up failed.") }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    fun signOut() {
        api.signOut()
        _state.value = AppState(loading = false)
    }

    fun refreshHome() = viewModelScope.launch {
        _state.update { it.copy(loading = true, error = null) }
        try {
            val sessions = api.loadSessions()
            val models = runCatching { api.loadModels() }.getOrDefault(EvilGptModels.fallback)
            _state.update { current ->
                current.copy(
                    sessions = sessions,
                    models = models,
                    selectedModel = if (models.any { model -> model.id == current.selectedModel }) current.selectedModel else models.firstOrNull()?.id ?: current.selectedModel,
                    loading = false
                )
            }
            val active = _state.value.activeSessionId
            if (active == null && sessions.isNotEmpty()) openSession(sessions.first().id)
            else if (active == null) newChat()
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, error = e.message ?: "Could not load EvilGPT.") }
        }
    }

    fun newChat(private: Boolean = _state.value.privateMode) = viewModelScope.launch {
        _state.update { it.copy(messages = emptyList(), pendingAttachments = emptyList(), status = "", error = null, privateMode = private) }
        if (private) {
            _state.update { it.copy(activeSessionId = "private-${UUID.randomUUID()}") }
            return@launch
        }
        try {
            val session = api.createSession()
            _state.update { it.copy(activeSessionId = session.id, sessions = listOf(session) + it.sessions.filterNot { old -> old.id == session.id }) }
        } catch (e: Exception) {
            _state.update { it.copy(error = e.message ?: "Could not start a new chat.") }
        }
    }

    fun openSession(id: String) = viewModelScope.launch {
        _state.update { it.copy(activeSessionId = id, privateMode = false, messages = emptyList(), pendingAttachments = emptyList(), loading = true, error = null) }
        try {
            val messages = api.loadMessages(id)
            _state.update { it.copy(messages = messages, loading = false) }
        } catch (e: Exception) {
            _state.update { it.copy(loading = false, error = e.message ?: "Could not load this conversation.") }
        }
    }

    fun setModel(id: String) = _state.update { it.copy(selectedModel = id) }
    fun setWebSearch(enabled: Boolean) = _state.update { it.copy(webSearch = enabled) }
    fun setDeepThink(enabled: Boolean) = _state.update { it.copy(deepThink = enabled) }

    fun attachImage(uri: Uri) = viewModelScope.launch {
        val context = getApplication<Application>()
        _state.update { it.copy(busy = true, error = null, status = "Uploading image…") }
        try {
            val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
            if (mime !in listOf("image/jpeg", "image/png", "image/webp")) throw IllegalArgumentException("Choose a JPG, PNG, or WEBP image.")
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: throw IllegalArgumentException("Could not read that image.")
            if (bytes.size > 15 * 1024 * 1024) throw IllegalArgumentException("Images must be smaller than 15 MB.")
            var sessionId = _state.value.activeSessionId
            if (sessionId == null) {
                if (_state.value.privateMode) {
                    val privateId = "private-${UUID.randomUUID()}"
                    sessionId = privateId
                    _state.update { it.copy(activeSessionId = privateId) }
                } else {
                    val session = api.createSession()
                    sessionId = session.id
                    _state.update { it.copy(activeSessionId = session.id, sessions = listOf(session) + it.sessions) }
                }
            }
            val name = uri.lastPathSegment?.substringAfterLast('/') ?: "image.jpg"
            val attachment = api.uploadImage(sessionId, name, mime, bytes)
            _state.update { it.copy(pendingAttachments = it.pendingAttachments + attachment) }
        } catch (e: Exception) {
            _state.update { it.copy(error = e.message ?: "Image upload failed.") }
        } finally {
            _state.update { it.copy(busy = false, status = "") }
        }
    }

    fun removeAttachment(path: String) = _state.update { it.copy(pendingAttachments = it.pendingAttachments.filterNot { item -> item.path == path }) }

    fun sendMessage(raw: String) = viewModelScope.launch {
        val rawPrompt = raw.trim()
        if (_state.value.busy) return@launch
        val attachments = _state.value.pendingAttachments
        val prompt = rawPrompt.ifBlank { if (attachments.isNotEmpty()) "Please analyze the attached image." else "" }
        if (prompt.isEmpty()) return@launch
        var sessionId = _state.value.activeSessionId
        val isPrivate = _state.value.privateMode
        _state.update { it.copy(busy = true, error = null, status = "Thinking…") }
        try {
            if (!isPrivate && (sessionId == null || sessionId.startsWith("private-"))) {
                val session = api.createSession()
                sessionId = session.id
                _state.update { it.copy(activeSessionId = session.id, sessions = listOf(session) + it.sessions) }
            }
            val userMessage = ChatMessage(role = "user", content = prompt, images = attachments)
            val previous = _state.value.messages
            _state.update { it.copy(messages = previous + userMessage, pendingAttachments = emptyList()) }
            if (!isPrivate && sessionId != null) api.saveMessage(sessionId, "user", prompt, attachments)
            val assistant = StringBuilder()
            val requestMessages = previous + userMessage
            val response = api.streamChat(
                messages = requestMessages,
                modelId = _state.value.selectedModel,
                webSearch = _state.value.webSearch,
                deepThink = _state.value.deepThink,
                images = attachments,
                onDelta = { delta ->
                    assistant.append(delta)
                    val current = _state.value.messages
                    val existing = current.lastOrNull()?.takeIf { it.role == "assistant" }
                    val next = ChatMessage(id = existing?.id ?: UUID.randomUUID().toString(), role = "assistant", content = assistant.toString())
                    _state.update { it.copy(messages = if (existing == null) it.messages + next else it.messages.dropLast(1) + next, status = "") }
                },
                onStatus = { status -> _state.update { it.copy(status = status) } }
            )
            if (response.isNotBlank()) {
                if (!isPrivate && sessionId != null) api.saveMessage(sessionId, "assistant", response)
                val current = _state.value.messages
                val last = current.lastOrNull()
                val finalMessage = ChatMessage(id = last?.takeIf { it.role == "assistant" }?.id ?: UUID.randomUUID().toString(), role = "assistant", content = response)
                _state.update { it.copy(messages = if (last?.role == "assistant") it.messages.dropLast(1) + finalMessage else it.messages + finalMessage) }
            }
            if (!isPrivate) {
                try {
                    val refreshedSessions = api.loadSessions()
                    _state.update { it.copy(sessions = refreshedSessions) }
                } catch (_: Exception) { /* Keep the local conversation if history refresh is temporarily unavailable. */ }
            }
        } catch (e: Exception) {
            _state.update { it.copy(error = e.message ?: "Could not get a response.") }
        } finally {
            _state.update { it.copy(busy = false, status = "") }
        }
    }

    fun renameSession(sessionId: String, title: String) = viewModelScope.launch {
        try {
            api.renameSession(sessionId, title.trim())
            _state.update { state -> state.copy(sessions = state.sessions.map { if (it.id == sessionId) it.copy(customTitle = title.trim()) else it }) }
        } catch (e: Exception) { _state.update { it.copy(error = e.message ?: "Rename failed.") } }
    }

    fun deleteSession(sessionId: String) = viewModelScope.launch {
        try {
            api.deleteSession(sessionId)
            val remaining = _state.value.sessions.filterNot { it.id == sessionId }
            _state.update { it.copy(sessions = remaining) }
            if (_state.value.activeSessionId == sessionId) {
                _state.update { it.copy(activeSessionId = null, messages = emptyList()) }
                newChat()
            }
        } catch (e: Exception) { _state.update { it.copy(error = e.message ?: "Delete failed.") } }
    }

    fun toggleStar(session: ChatSession) = viewModelScope.launch {
        try {
            api.toggleStar(session.id, !session.starred)
            _state.update { state -> state.copy(sessions = state.sessions.map { if (it.id == session.id) it.copy(starred = !it.starred) else it }) }
        } catch (e: Exception) { _state.update { it.copy(error = e.message ?: "Could not update favourite.") } }
    }
}
