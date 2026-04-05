package dev.mizzenmast.letta.service

import android.util.Log
import dev.mizzenmast.letta.BuildConfig
import dev.mizzenmast.letta.data.local.TokenStore
import dev.mizzenmast.letta.data.local.dao.ConversationDao
import dev.mizzenmast.letta.data.local.dao.MessageDao
import dev.mizzenmast.letta.data.local.entity.MessageEntity
import dev.mizzenmast.letta.data.remote.api.AuthApiService
import dev.mizzenmast.letta.data.remote.dto.MessageDto
import dev.mizzenmast.letta.data.remote.dto.RefreshRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketManager @Inject constructor(
    private val tokenStore: TokenStore,
    private val authApi: AuthApiService,
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao,
    private val json: Json,
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var webSocket: WebSocket? = null
    private var reconnectJob: Job? = null
    private var reconnectDelay = 1_000L
    private val maxReconnectDelay = 30_000L

    private val _events = MutableSharedFlow<WsInboundEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<WsInboundEvent> = _events

    private val client = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    sealed class WsInboundEvent {
        data class MessageNew(val message: MessageDto) : WsInboundEvent()
        data class MessageDelivered(val messageId: String, val userId: String, val deliveredAt: String) : WsInboundEvent()
        data class MessageRead(val messageId: String, val userId: String, val readAt: String) : WsInboundEvent()
        data class ReactionAdd(val messageId: String, val userId: String, val emoji: String) : WsInboundEvent()
        data class ReactionRemove(val messageId: String, val userId: String) : WsInboundEvent()
        data class TypingStart(val conversationId: String, val userId: String) : WsInboundEvent()
        data class TypingStop(val conversationId: String, val userId: String) : WsInboundEvent()
        data class PresenceUpdate(val userId: String, val online: Boolean) : WsInboundEvent()
        data class Error(val detail: String) : WsInboundEvent()
    }

    /**
     * Launch connect() inside the IO scope so token refresh never blocks the calling thread.
     * Safe to call from any thread (e.g. a Service or ViewModel).
     */
    fun connect() {
        scope.launch { connectSuspending() }
    }

    private suspend fun connectSuspending() {
        val token = resolveToken() ?: run {
            Log.w(TAG, "No token available — skipping WebSocket connect")
            return
        }

        val request = Request.Builder()
            .url("${BuildConfig.WS_URL}?token=$token")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected")
                reconnectDelay = 1_000L
            }

            override fun onMessage(ws: WebSocket, text: String) {
                scope.launch { handleMessage(text) }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WebSocket closing: $code $reason")
                webSocket.close(1000, null)
                // 4002 = token invalid/expired after connect, 4004 = user not found
                // Don't reconnect for auth failures — caller must re-login
                if (code != 4002 && code != 4004) {
                    scheduleReconnect()
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WebSocket failure: ${t.message}")
                scheduleReconnect()
            }
        })
    }

    /**
     * Tries to return a valid access token.
     * Refreshes using the refresh token first; falls back to the stored access token.
     * Returns null only if there are no tokens at all.
     */
    private suspend fun resolveToken(): String? {
        val refreshToken = tokenStore.refreshToken
        if (refreshToken != null) {
            try {
                val response = authApi.refresh(RefreshRequest(refreshToken))
                tokenStore.accessToken = response.accessToken
                tokenStore.refreshToken = response.refreshToken
                return response.accessToken
            } catch (e: Exception) {
                Log.w(TAG, "Token refresh failed: ${e.message} — falling back to stored access token")
            }
        }
        return tokenStore.accessToken
    }

    fun disconnect() {
        reconnectJob?.cancel()
        webSocket?.close(1000, "User disconnected")
        webSocket = null
    }

    fun send(type: String, payload: JsonObject) {
        val event = buildJsonObject {
            put("type", type)
            put("payload", payload)
        }
        webSocket?.send(json.encodeToString(event))
    }

    fun sendMessage(
        conversationId: String,
        type: String,
        content: String? = null,
        mediaUrl: String? = null,
        mediaMime: String? = null,
        replyToId: String? = null,
    ) {
        val payload = buildJsonObject {
            put("conversation_id", conversationId)
            put("type", type)
            content?.let { put("content", it) }
            mediaUrl?.let { put("media_url", it) }
            mediaMime?.let { put("media_mime", it) }
            replyToId?.let { put("reply_to_id", it) }
        }
        send("message.send", payload)
    }

    fun sendAck(messageId: String) {
        send("message.ack", buildJsonObject { put("message_id", messageId) })
    }

    fun sendRead(messageId: String, conversationId: String) {
        send("message.read", buildJsonObject {
            put("message_id", messageId)
            put("conversation_id", conversationId)
        })
    }

    fun sendTypingStart(conversationId: String) {
        send("typing.start", buildJsonObject { put("conversation_id", conversationId) })
    }

    fun sendTypingStop(conversationId: String) {
        send("typing.stop", buildJsonObject { put("conversation_id", conversationId) })
    }

    private suspend fun handleMessage(text: String) {
        try {
            val obj = json.parseToJsonElement(text).jsonObject
            val type = obj["type"]?.jsonPrimitive?.content ?: return
            val payload = obj["payload"]?.jsonObject ?: return

            when (type) {
                "message.new", "message.sent" -> {
                    val msg = json.decodeFromJsonElement(MessageDto.serializer(), payload)
                    val members = conversationDao.getMembers(msg.conversationId)
                    val member = members.firstOrNull { it.userId == msg.senderId }
                    messageDao.upsert(msg.toEntity(
                        isMine = type == "message.sent",
                        senderName = member?.displayName ?: "",
                        senderAvatar = member?.avatarUrl,
                    ))
                    conversationDao.updateLastMessage(
                        msg.conversationId,
                        msg.content,
                        msg.createdAt,
                        msg.senderId,
                    )
                    if (type == "message.new") {
                        conversationDao.incrementUnread(msg.conversationId)
                        sendAck(msg.id)
                    }
                    _events.emit(WsInboundEvent.MessageNew(msg))
                }
                "message.delivered" -> {
                    val messageId = payload["message_id"]?.jsonPrimitive?.content ?: return
                    val userId = payload["user_id"]?.jsonPrimitive?.content ?: return
                    val at = payload["delivered_at"]?.jsonPrimitive?.content ?: return
                    messageDao.markDelivered(messageId, at)
                    _events.emit(WsInboundEvent.MessageDelivered(messageId, userId, at))
                }
                "message.read" -> {
                    val messageId = payload["message_id"]?.jsonPrimitive?.content ?: return
                    val userId = payload["user_id"]?.jsonPrimitive?.content ?: return
                    val at = payload["read_at"]?.jsonPrimitive?.content ?: return
                    messageDao.markRead(messageId, at)
                    _events.emit(WsInboundEvent.MessageRead(messageId, userId, at))
                }
                "reaction.add" -> {
                    val messageId = payload["message_id"]?.jsonPrimitive?.content ?: return
                    val userId = payload["user_id"]?.jsonPrimitive?.content ?: return
                    val emoji = payload["emoji"]?.jsonPrimitive?.content ?: return
                    _events.emit(WsInboundEvent.ReactionAdd(messageId, userId, emoji))
                }
                "reaction.remove" -> {
                    val messageId = payload["message_id"]?.jsonPrimitive?.content ?: return
                    val userId = payload["user_id"]?.jsonPrimitive?.content ?: return
                    _events.emit(WsInboundEvent.ReactionRemove(messageId, userId))
                }
                "typing.start" -> {
                    val conversationId = payload["conversation_id"]?.jsonPrimitive?.content ?: return
                    val userId = payload["user_id"]?.jsonPrimitive?.content ?: return
                    _events.emit(WsInboundEvent.TypingStart(conversationId, userId))
                }
                "typing.stop" -> {
                    val conversationId = payload["conversation_id"]?.jsonPrimitive?.content ?: return
                    val userId = payload["user_id"]?.jsonPrimitive?.content ?: return
                    _events.emit(WsInboundEvent.TypingStop(conversationId, userId))
                }
                "presence.update" -> {
                    val userId = payload["user_id"]?.jsonPrimitive?.content ?: return
                    val online = payload["online"]?.jsonPrimitive?.content?.toBoolean() ?: return
                    _events.emit(WsInboundEvent.PresenceUpdate(userId, online))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling WS message: ${e.message}")
        }
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(reconnectDelay)
            reconnectDelay = minOf(reconnectDelay * 2, maxReconnectDelay)
            connectSuspending()
        }
    }

    companion object {
        private const val TAG = "WebSocketManager"
    }
}

fun MessageDto.toEntity(
    isMine: Boolean,
    senderName: String = "",
    senderAvatar: String? = null,
): MessageEntity = MessageEntity(
    id = id,
    conversationId = conversationId,
    senderId = senderId,
    senderName = senderName,
    senderAvatar = senderAvatar,
    type = type,
    content = content,
    mediaUrl = mediaUrl,
    mediaMime = mediaMime,
    replyToId = replyToId,
    replyToContent = null,
    replyToSenderName = null,
    createdAt = createdAt,
    isMine = isMine,
)