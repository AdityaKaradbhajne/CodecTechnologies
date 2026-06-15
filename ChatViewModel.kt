package com.example.aichat.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.aichat.BuildConfig
import com.example.aichat.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

// ── UI State ──────────────────────────────────────────────────────────────────

sealed class ChatUiState {
    object Idle : ChatUiState()
    object Streaming : ChatUiState()           // AI is currently responding
    data class Error(val message: String) : ChatUiState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ChatDatabase.getInstance(application)
    private val repository = ChatRepository(
        dao = db.messageDao(),
        apiKey = BuildConfig.GEMINI_API_KEY   // Set in local.properties → BuildConfig
    )

    // Current session ID — new UUID on first launch, persists across sends
    private val _sessionId = MutableStateFlow(UUID.randomUUID().toString())
    val sessionId: StateFlow<String> = _sessionId.asStateFlow()

    // Live message list for the current session
    val messages: StateFlow<List<Message>> = _sessionId
        .flatMapLatest { repository.getMessages(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // All past sessions for the history screen
    val sessions: StateFlow<List<ChatSession>> = repository.getAllSessions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // UI state for showing loading / error indicators
    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Idle)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    // Tracks the currently streaming message's DB row ID so we can update it
    private var streamingMessageId: Long = -1L
    private var streamingJob: Job? = null

    // ── Public actions ────────────────────────────────────────────────────────

    /**
     * Call this when the user taps Send (or confirms voice input).
     */
    fun sendMessage(userText: String) {
        if (userText.isBlank() || _uiState.value is ChatUiState.Streaming) return

        streamingJob = viewModelScope.launch {
            val currentSessionId = _sessionId.value
            val isFirstMessage = messages.value.isEmpty()

            // 1. Create session row on the first message
            if (isFirstMessage) {
                repository.createSession(currentSessionId, userText)
            }

            // 2. Persist user message to Room
            repository.saveMessage(
                Message(
                    sessionId = currentSessionId,
                    role = MessageRole.USER,
                    content = userText
                )
            )
            repository.touchSession(currentSessionId)

            // 3. Insert a LOADING placeholder — shown as typing dots in the UI
            val loadingMsg = Message(
                sessionId = currentSessionId,
                role = MessageRole.LOADING,
                content = ""
            )
            streamingMessageId = repository.saveMessage(loadingMsg)
            _uiState.value = ChatUiState.Streaming

            // 4. Stream the response from Gemini
            val accumulated = StringBuilder()
            try {
                repository.sendMessage(userText, messages.value)
                    .collect { chunk ->
                        accumulated.append(chunk)

                        // Update the placeholder row in real time so the UI
                        // reflects the partial response as it arrives
                        repository.saveMessage(
                            loadingMsg.copy(
                                id = streamingMessageId,
                                role = MessageRole.ASSISTANT,
                                content = accumulated.toString()
                            )
                        )
                    }

                // 5. Final write — replace LOADING with completed ASSISTANT message
                repository.saveMessage(
                    loadingMsg.copy(
                        id = streamingMessageId,
                        role = MessageRole.ASSISTANT,
                        content = accumulated.toString()
                    )
                )
                repository.touchSession(currentSessionId)
                _uiState.value = ChatUiState.Idle

            } catch (e: Exception) {
                // Remove the broken placeholder and surface the error
                repository.saveMessage(
                    loadingMsg.copy(
                        id = streamingMessageId,
                        role = MessageRole.ASSISTANT,
                        content = "Sorry, something went wrong. Please try again."
                    )
                )
                _uiState.value = ChatUiState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Stop an in-progress streaming response.
     */
    fun cancelStreaming() {
        streamingJob?.cancel()
        _uiState.value = ChatUiState.Idle
    }

    /**
     * Switch to an existing session (called from history screen).
     */
    fun loadSession(sessionId: String) {
        _sessionId.value = sessionId
    }

    /**
     * Start a brand-new chat session.
     */
    fun newSession() {
        _sessionId.value = UUID.randomUUID().toString()
        _uiState.value = ChatUiState.Idle
    }

    /**
     * Delete a session and all its messages.
     */
    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            // If the deleted session was the active one, start fresh
            if (_sessionId.value == sessionId) newSession()
        }
    }
}
