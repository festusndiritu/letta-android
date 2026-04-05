package dev.mizzenmast.letta.data.remote.api

import dev.mizzenmast.letta.data.remote.dto.ContactSyncRequest
import dev.mizzenmast.letta.data.remote.dto.ContactSyncResponse
import dev.mizzenmast.letta.data.remote.dto.ConversationDto
import dev.mizzenmast.letta.data.remote.dto.CreateDirectRequest
import dev.mizzenmast.letta.data.remote.dto.CreateGroupRequest
import dev.mizzenmast.letta.data.remote.dto.FocusProfileRequest
import dev.mizzenmast.letta.data.remote.dto.MessageDto
import dev.mizzenmast.letta.data.remote.dto.MuteRequest
import dev.mizzenmast.letta.data.remote.dto.PublicUserDto
import dev.mizzenmast.letta.data.remote.dto.ReactionRequest
import dev.mizzenmast.letta.data.remote.dto.SessionDto
import dev.mizzenmast.letta.data.remote.dto.AddMembersRequest
import dev.mizzenmast.letta.data.remote.dto.RemoveMemberRequest
import dev.mizzenmast.letta.data.remote.dto.UpdateConversationRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ConversationApiService {
    @GET("conversations")
    suspend fun getConversations(): List<ConversationDto>

    @POST("conversations/direct")
    suspend fun createDirect(@Body body: CreateDirectRequest): ConversationDto

    @POST("conversations/group")
    suspend fun createGroup(@Body body: CreateGroupRequest): ConversationDto

    @GET("conversations/{id}/messages")
    suspend fun getMessages(
        @Path("id") conversationId: String,
        @Query("before_id") beforeId: String? = null,
        @Query("limit") limit: Int = 30,
    ): List<MessageDto>

    @GET("messages/missed")
    suspend fun getMissedMessages(@Query("since") since: String): List<MessageDto>

    @POST("contacts/sync")
    suspend fun syncContacts(@Body body: ContactSyncRequest): ContactSyncResponse

    @GET("users/search")
    suspend fun searchUsers(@Query("q") query: String): List<PublicUserDto>

    @GET("users/{id}")
    suspend fun getUser(@Path("id") userId: String): PublicUserDto

    @PATCH("users/me/focus")
    suspend fun setFocusProfile(@Body body: FocusProfileRequest)

    @POST("conversations/{id}/mute")
    suspend fun muteConversation(@Path("id") conversationId: String, @Body body: MuteRequest)

    @DELETE("conversations/{id}/mute")
    suspend fun unmuteConversation(@Path("id") conversationId: String)

    @GET("sessions")
    suspend fun getSessions(): List<SessionDto>

    @DELETE("sessions/{id}")
    suspend fun revokeSession(@Path("id") sessionId: String)

    @POST("messages/{id}/react")
    suspend fun reactToMessage(
        @Path("id") messageId: String,
        @Body body: ReactionRequest,
    )

    @GET("conversations/{id}")
    suspend fun getConversation(@Path("id") conversationId: String): ConversationDto

    @PATCH("conversations/{id}")
    suspend fun updateConversation(
        @Path("id") conversationId: String,
        @Body body: UpdateConversationRequest,
    ): ConversationDto

    @POST("conversations/{id}/members")
    suspend fun addMembers(
        @Path("id") conversationId: String,
        @Body body: AddMembersRequest,
    )

    @HTTP(method = "DELETE", path = "conversations/{id}/members", hasBody = true)
    suspend fun removeMember(
        @Path("id") conversationId: String,
        @Body body: RemoveMemberRequest,
    )
}