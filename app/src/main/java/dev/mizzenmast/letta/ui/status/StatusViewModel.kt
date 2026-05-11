package dev.mizzenmast.letta.ui.status

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.mizzenmast.letta.data.local.TokenStore
import dev.mizzenmast.letta.data.local.dao.ConversationDao
import dev.mizzenmast.letta.data.local.entity.StatusEntity
import dev.mizzenmast.letta.data.repository.StatusRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatusGroup(
    val userId: String,
    val displayName: String,
    val avatarUrl: String?,
    val statuses: List<StatusEntity>,
)

@HiltViewModel
class StatusViewModel @Inject constructor(
    private val statusRepository: StatusRepository,
    private val conversationDao: ConversationDao,
    private val tokenStore: TokenStore,
) : ViewModel() {

    val currentUserId: String = tokenStore.userId ?: ""

    val myStatuses: StateFlow<List<StatusEntity>> = statusRepository.observeMine()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Contacts' statuses grouped by userId, display name resolved via conversation members
    val feedGroups: StateFlow<List<StatusGroup>> = combine(
        statusRepository.observeFeed(),
        conversationDao.observeAllMembers(),
    ) { feed, members ->
        // Build a userId → (displayName, avatarUrl) map from conversation members
        val nameMap = members
            .groupBy { it.userId }
            .mapValues { (_, list) ->
                val m = list.first()
                Pair(m.displayName, m.avatarUrl)
            }
        feed
            .groupBy { it.userId }
            .map { (userId, statuses) ->
                val (name, avatar) = nameMap[userId] ?: Pair(userId.take(8) + "…", null)
                StatusGroup(
                    userId = userId,
                    displayName = name,
                    avatarUrl = avatar,
                    statuses = statuses.sortedBy { it.createdAt ?: "" },
                )
            }
            .sortedByDescending { it.statuses.maxOfOrNull { s -> s.createdAt ?: "" } ?: "" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            statusRepository.refreshFeed()
            statusRepository.refreshMine()
        }
    }

    fun viewStatus(statusId: String) {
        viewModelScope.launch { statusRepository.viewStatus(statusId) }
    }

    fun createTextStatus(content: String, bgColor: String?) {
        viewModelScope.launch {
            statusRepository.createStatus(
                type = "text",
                content = content,
                mediaUrl = null,
                bgColor = bgColor,
            )
        }
    }

    fun createMediaStatus(type: String, mediaUrl: String, content: String?) {
        viewModelScope.launch {
            statusRepository.createStatus(
                type = type,
                content = content,
                mediaUrl = mediaUrl,
                bgColor = null,
            )
        }
    }

    fun deleteStatus(statusId: String) {
        viewModelScope.launch { statusRepository.deleteStatus(statusId) }
    }
}
