package com.example.aichat.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ChatRepository(
    private val dao: MessageDao,
    private val apiKey: String
) {

    // Lazily created so we don't crash at startup if the key is missing
    private val model by lazy {
        GenerativeModel(
            modelName = "gemini-pro",
            apiKey = apiKey,
            generationConfig = generationConfig {
                temperature = 0.7f
                maxOutputTokens = 2048
            }
        )
    }

    // ── Room helpers ──────────────────────────────────────────────────────────

    fun getMessages(sessionId: String): Flow<List<Message>> =
        dao.getMessagesBySession(sessionId)

    fun getAllSessions(): Flow<List<ChatSession>> =
        dao.getAllSessions()

    suspend fun saveMessage(message: Message): Long =
        dao.insertMessage(message)

    suspend fun createSession(sessionId: String, firstMessage: String) {
        val title = firstMessage.take(40).trimEnd() + if (firstMessage.length > 40) "…" else ""
        dao.insertSession(ChatSession(sessionId = sessionId, title = title))
    }

    suspend fun touchSession(sessionId: String) =
        dao.updateSessionTimestamp(sessionId)

    suspend fun deleteSession(sessionId: String) =
        dao.deleteSessionWithMessages(sessionId)

    // ── Gemini API ────────────────────────────────────────────────────────────

    /**
     * Streams a reply from Gemini.
     * [history] is the list of prior messages (used to keep conversation context).
     * Emits chunks of text as they arrive.
     */
    fun sendMessage(userText: String, history: List<Message>): Flow<String> = flow {
        val chat = model.startChat(
            history = history
                .filter { it.role != MessageRole.LOADING }
                .map { msg ->
                    content(role = if (msg.role == MessageRole.USER) "user" else "model") {
                        text(msg.content)
                    }
                }
        )

        chat.sendMessageStream(userText).collect { chunk ->
            chunk.text?.let { emit(it) }
        }
    }
}
