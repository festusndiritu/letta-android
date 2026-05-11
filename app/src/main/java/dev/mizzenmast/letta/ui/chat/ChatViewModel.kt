package dev.mizzenmast.letta.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mizzenmast.letta.data.local.TokenStore
import dev.mizzenmast.letta.data.local.SettingsStore
import dev.mizzenmast.letta.data.local.dao.MessageDao
import dev.mizzenmast.letta.data.repository.ConversationRepository
import dev.mizzenmast.letta.data.repository.MetaRepository
import dev.mizzenmast.letta.service.CallStateManager
import dev.mizzenmast.letta.service.WebSocketManager
import dev.mizzenmast.letta.data.remote.dto.LinkPreviewDto
import dev.mizzenmast.letta.data.remote.dto.MessageDto
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
    private val callStateManager: CallStateManager,
    private val tokenStore: TokenStore,
    private val messageDao: MessageDao,
    private val mediaRepository: dev.mizzenmast.letta.data.repository.MediaRepository,
    private val metaRepository: MetaRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    private val _conversationId = MutableStateFlow("")
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState

    private val _reactions = MutableStateFlow<Map<String, Map<String, String>>>(emptyMap())
    val reactions: StateFlow<Map<String, Map<String, String>>> = _reactions

    private val _linkPreviews = MutableStateFlow<Map<String, LinkPreviewDto>>(emptyMap())
    val linkPreviews: StateFlow<Map<String, LinkPreviewDto>> = _linkPreviews

    private val _previewRequests = MutableStateFlow<Set<String>>(emptySet())

    private val _searchResults = MutableStateFlow<List<MessageDto>>(emptyList())
    val searchResults: StateFlow<List<MessageDto>> = _searchResults

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError

    val presence: StateFlow<Map<String, Boolean>> = wsManager.presence
    val lastSeen: StateFlow<Map<String, Long>> = wsManager.lastSeen

    val currentUserId: String? = tokenStore.userId

    val wallpaperType: StateFlow<String> = settingsStore.wallpaperTypeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "NONE")

    val wallpaperValue: StateFlow<String> = settingsStore.wallpaperValueFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

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

    fun uploadAndSendMedia(
        uri: android.net.Uri,
        mimeTypeOverride: String? = null,
        caption: String? = null,
    ) {
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
                        content = caption?.takeIf { it.isNotBlank() },
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

    fun requestLinkPreview(url: String) {
        if (_linkPreviews.value.containsKey(url) || _previewRequests.value.contains(url)) return
        _previewRequests.update { it + url }
        viewModelScope.launch {
            metaRepository.getPreview(url)
                .onSuccess { preview ->
                    _linkPreviews.update { it + (url to preview) }
                }
                .also {
                    _previewRequests.update { it - url }
                }
        }
    }

    fun searchMessages(query: String, limit: Int = 30) {
        val conversationId = _conversationId.value
        if (conversationId.isEmpty()) return
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            _searchError.value = null
            _isSearching.value = false
            return
        }
        _isSearching.value = true
        _searchError.value = null
        viewModelScope.launch {
            repository.searchMessages(conversationId, query.trim(), limit)
                .onSuccess { _searchResults.value = it }
                .onFailure { _searchError.value = it.message ?: "Search failed" }
            _isSearching.value = false
        }
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
        _searchError.value = null
    }

    fun muteConversation(duration: String) {
        val conversationId = _conversationId.value
        if (conversationId.isEmpty()) return
        viewModelScope.launch { repository.muteConversation(conversationId, duration) }
    }

    fun unmuteConversation() {
        val conversationId = _conversationId.value
        if (conversationId.isEmpty()) return
        viewModelScope.launch { repository.unmuteConversation(conversationId) }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            repository.deleteMessage(messageId)
        }
    }

    fun pinMessage(messageId: String) {
        val conversationId = _conversationId.value
        if (conversationId.isEmpty()) return
        viewModelScope.launch { repository.pinMessage(conversationId, messageId) }
    }

    fun unpinMessage(messageId: String) {
        val conversationId = _conversationId.value
        if (conversationId.isEmpty()) return
        viewModelScope.launch { repository.unpinMessage(conversationId, messageId) }
    }

    /** Initiate a voice or video call with the peer in a direct conversation. */
    fun startCall(callType: String) {
        val conversationId = _conversationId.value
        if (conversationId.isEmpty()) return
        val myId = currentUserId ?: return
        val peer = members.value.firstOrNull { it.userId != myId } ?: return
        callStateManager.startOutgoingCall(
            conversationId = conversationId,
            calleeId = peer.userId,
            calleeName = peer.displayName,
            callType = callType,
        )
    }
}