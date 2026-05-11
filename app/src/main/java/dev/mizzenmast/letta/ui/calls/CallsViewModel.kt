package dev.mizzenmast.letta.ui.calls

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mizzenmast.letta.data.local.TokenStore
import dev.mizzenmast.letta.data.local.entity.CallEntity
import dev.mizzenmast.letta.data.repository.CallRepository
import dev.mizzenmast.letta.data.repository.ConversationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CallItem(
    val call: CallEntity,
    val conversationName: String,
    val conversationId: String?,
    val isIncoming: Boolean,
    val isMissed: Boolean,
)

@HiltViewModel
class CallsViewModel @Inject constructor(
    private val callRepository: CallRepository,
    private val conversationRepository: ConversationRepository,
    private val tokenStore: TokenStore,
) : ViewModel() {

    val currentUserId: String = tokenStore.userId ?: ""

    val callItems: StateFlow<List<CallItem>> = combine(
        callRepository.observeCalls(),
        conversationRepository.observeConversations(),
    ) { calls, conversations ->
        val convMap = conversations.associateBy { it.id }
        calls.map { call ->
            val conv = call.conversationId?.let { convMap[it] }
            CallItem(
                call = call,
                conversationName = conv?.name ?: "Unknown",
                conversationId = call.conversationId,
                isIncoming = call.calleeId == currentUserId,
                isMissed = call.status == "missed" || call.status == "declined",
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { callRepository.refreshCalls() }
    }

    fun clearCalls() {
        viewModelScope.launch { callRepository.clearCalls() }
    }

    fun deleteCall(id: String) {
        viewModelScope.launch { callRepository.deleteCall(id) }
    }
}
