package dev.mizzenmast.letta.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mizzenmast.letta.data.local.TokenStore
import dev.mizzenmast.letta.data.local.dao.MessageDao
import dev.mizzenmast.letta.data.repository.ConversationRepository
import dev.mizzenmast.letta.service.WebSocketManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val isLoadingMore: Boolean = false,
    val typingUserIds: Set<String> = emptySet(),
    val isUploadingMedia: Boolean = false,
    val mediaError: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val repository: ConversationRepository,
    private val wsManager: WebSocketManager,
    private val tokenStore: TokenStore,
    private val messageDao: MessageDao,
    private val mediaRepository: dev.mizzenmast.letta.data.repository.MediaRepository,
) : ViewModel() {

    private val _conversationId = MutableStateFlow("")
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    private val _reactions = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())
    val reactions: StateFlow<Map<String, Map<String, String>>> = _reactions

    val currentUserId: String? = tokenStore.userId

    val messages = _conversationId
        .flatMapLatest { id -> repository.observeMessages(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val conversation = _conversationId
        .flatMapLatest { id -> repository.observeConversation(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val members = _conversationId
        .flatMapLatest { id -> repository.observeMembers(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun init(conversationId: String) {
        _conversationId.value = conversationId
        viewModelScope.launch {
            repository.clearUnread(conversationId)
        }
        // Observe typing events
        viewModelScope.launch {
            wsManager.events.collect { event ->
                when (event) {
                    is WebSocketManager.WsInboundEvent.TypingStart -> {
                        if (event.conversationId == conversationId) {
                            _uiState.update { it.copy(typingUserIds = it.typingUserIds + event.userId) }
                        }
                    }
                    is WebSocketManager.WsInboundEvent.TypingStop -> {
                        if (event.conversationId == conversationId) {
                            _uiState.update { it.copy(typingUserIds = it.typingUserIds - event.userId) }
                        }
                    }
                    is WebSocketManager.WsInboundEvent.ReactionAdd -> {
                        _reactions.update { state ->
                            val updated = state[event.messageId]?.toMutableMap() ?: mutableMapOf()
                            updated[event.userId] = event.emoji
                            state + (event.messageId to updated)
                        }
                    }
                    is WebSocketManager.WsInboundEvent.ReactionRemove -> {
                        _reactions.update { state ->
                            val updated = state[event.messageId]?.toMutableMap() ?: mutableMapOf()
                            updated.remove(event.userId)
                            if (updated.isEmpty()) state - event.messageId
                            else state + (event.messageId to updated)
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    fun sendMessage(content: String, replyToId: String? = null) {
        val conversationId = _conversationId.value
        if (content.isBlank() || conversationId.isEmpty()) return
        wsManager.sendMessage(
            conversationId,
            "text",
            content = content.trim(),
            replyToId = replyToId,
        )
    }

    fun sendTypingStart() = wsManager.sendTypingStart(_conversationId.value)
    fun sendTypingStop() = wsManager.sendTypingStop(_conversationId.value)

    fun loadMoreMessages() {
        val conversationId = _conversationId.value
        val oldest = messages.value.firstOrNull()?.id ?: return
        _uiState.update { it.copy(isLoadingMore = true) }
        viewModelScope.launch {
            repository.loadMoreMessages(conversationId, oldest)
            _uiState.update { it.copy(isLoadingMore = false) }
        }
    }

    fun reactToMessage(messageId: String, emoji: String) {
        val userId = currentUserId ?: return
        _reactions.update { state ->
            val updated = state[messageId]?.toMutableMap() ?: mutableMapOf()
            val existing = updated[userId]
            if (existing == emoji) {
                updated.remove(userId)
            } else {
                updated[userId] = emoji
            }
            if (updated.isEmpty()) state - messageId
            else state + (messageId to updated)
        }
        viewModelScope.launch {
            repository.reactToMessage(messageId, emoji)
        }
    }

    fun uploadAndSendMedia(uri: android.net.Uri, mimeTypeOverride: String? = null) {
        val conversationId = _conversationId.value
        if (conversationId.isEmpty()) return
        _uiState.update { it.copy(isUploadingMedia = true, mediaError = null) }
        viewModelScope.launch {
            mediaRepository.upload(uri, mimeTypeOverride)
                .onSuccess { media ->
                    val type = when {
                        media.mimeType.startsWith("image/") -> "image"
                        media.mimeType.startsWith("video/") -> "video"
                        media.mimeType.startsWith("audio/") -> "audio"
                        else -> "document"
                    }
                    wsManager.sendMessage(
                        conversationId = conversationId,
                        type = type,
                        mediaUrl = media.url,
                        mediaMime = media.mimeType,
                    )
                    _uiState.update { it.copy(isUploadingMedia = false) }
                }
                .onFailure { e ->
                    _uiState.update {
                        it.copy(
                            isUploadingMedia = false,
                            mediaError = e.message ?: "Upload failed",
                        )
                    }
                }
        }
    }

    fun clearMediaError() {
        _uiState.update { it.copy(mediaError = null) }
    }

    fun setMediaError(message: String) {
        _uiState.update { it.copy(mediaError = message) }
    }
}