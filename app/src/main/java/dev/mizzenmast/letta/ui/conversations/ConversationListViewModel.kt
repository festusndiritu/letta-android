package dev.mizzenmast.letta.ui.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mizzenmast.letta.data.repository.ConversationRepository
import dev.mizzenmast.letta.service.WebSocketManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConversationListViewModel @Inject constructor(
    private val repository: ConversationRepository,
    private val wsManager: WebSocketManager,
) : ViewModel() {

    val conversations = repository.observeConversations()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        wsManager.connect()
        viewModelScope.launch {
            repository.refreshConversations()
            repository.syncMissedMessages()
        }
    }
}