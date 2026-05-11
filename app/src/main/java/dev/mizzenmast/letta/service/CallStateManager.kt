package dev.mizzenmast.letta.service

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton that tracks the one active call (if any) across the entire app.
 *
 * Observes WebSocket call events and updates state accordingly.
 * Exposes methods to initiate, answer, reject, and end calls.
 */
@Singleton
class CallStateManager @Inject constructor(
    private val wsManager: WebSocketManager,
) {
    private val scope = CoroutineScope(Dispatchers.Main.immediate)

    private val _activeCall = MutableStateFlow<ActiveCall?>(null)
    val activeCall: StateFlow<ActiveCall?> = _activeCall.asStateFlow()

    init {
        scope.launch {
            wsManager.events.collect { event ->
                when (event) {
                    is WebSocketManager.WsInboundEvent.CallOffer -> {
                        _activeCall.value = ActiveCall(
                            callId = event.callId,
                            conversationId = event.conversationId,
                            callerName = event.callerName ?: event.callerId,
                            state = CallState.RINGING,
                            callType = event.type,
                            isIncoming = true,
                            remoteSdp = event.sdp,
                            peerId = event.callerId,
                        )
                    }
                    is WebSocketManager.WsInboundEvent.CallAnswer -> {
                        _activeCall.updateById(event.callId) { it.copy(state = CallState.ACTIVE, remoteSdp = event.sdp) }
                    }
                    is WebSocketManager.WsInboundEvent.CallRejected -> {
                        val current = _activeCall.value
                        if (current?.callId == event.callId) _activeCall.value = null
                    }
                    is WebSocketManager.WsInboundEvent.CallEnded -> {
                        val current = _activeCall.value
                        if (current?.callId == event.callId) _activeCall.value = null
                    }
                    else -> Unit
                }
            }
        }
    }

    // Called from FCM when an incoming call push arrives (before WS event arrives)
    fun onIncomingCall(callId: String, conversationId: String, callerName: String, callType: String = "audio", peerId: String = "") {
        // Only set if there's no existing call for this callId already (WS may beat FCM)
        if (_activeCall.value?.callId != callId) {
            _activeCall.value = ActiveCall(
                callId = callId,
                conversationId = conversationId,
                callerName = callerName,
                state = CallState.RINGING,
                callType = callType,
                isIncoming = true,
                peerId = peerId,
            )
        }
    }

    // Initiate an outgoing call
    fun startOutgoingCall(conversationId: String, calleeId: String, calleeName: String, callType: String = "audio") {
        val callId = UUID.randomUUID().toString()
        wsManager.sendCallOffer(callId, conversationId, calleeId, callType, sdp = "")
        _activeCall.value = ActiveCall(
            callId = callId,
            conversationId = conversationId,
            callerName = calleeName,
            state = CallState.RINGING,
            callType = callType,
            isIncoming = false,
            peerId = calleeId,
        )
    }

    // Accept incoming call
    fun answerCall() {
        val current = _activeCall.value ?: return
        wsManager.sendCallAnswer(current.callId, current.remoteSdp ?: "")
        _activeCall.value = current.copy(state = CallState.ACTIVE)
    }

    // Decline incoming call
    fun rejectCall() {
        val current = _activeCall.value ?: return
        wsManager.sendCallReject(current.callId)
        _activeCall.value = null
    }

    // End a connected call
    fun endCall() {
        val current = _activeCall.value ?: return
        wsManager.sendCallEnd(current.callId)
        _activeCall.value = null
    }

    // Called when call was accepted (state transition for outgoing call)
    fun onCallAccepted(callId: String) {
        _activeCall.updateById(callId) { it.copy(state = CallState.ACTIVE) }
    }

    // Called when call was ended from external source (FCM, banner hangup)
    fun onCallEnded() { endCall() }

    // Called when call was declined from external source
    fun onCallDeclined() { rejectCall() }

    private fun MutableStateFlow<ActiveCall?>.updateById(
        callId: String,
        transform: (ActiveCall) -> ActiveCall,
    ) {
        val current = value
        if (current?.callId == callId) value = transform(current)
    }
}

data class ActiveCall(
    val callId: String,
    val conversationId: String,
    val callerName: String,
    val state: CallState,
    val callType: String = "audio",
    val isIncoming: Boolean = false,
    val remoteSdp: String? = null,
    val peerId: String = "",
)

enum class CallState {
    RINGING,  // phone is ringing (incoming or outgoing dialling)
    ACTIVE,   // call is connected
    ON_HOLD,  // call is muted/held
}

