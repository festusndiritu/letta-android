# Android Client Spec (Source of Truth)

This document reflects the current backend behavior in this repository.
Use this as the contract for a fresh Android client implementation.

## Base

- REST base: `https://api.letta.mizzenmast.dev`
- WS base: `wss://api.letta.mizzenmast.dev/ws?token=<access_token>`
- Auth header: `Authorization: Bearer <access_token>`

## Auth and Session

### Public

- `POST /auth/request-otp`
  - Body: `{ "phone_number": "+2547..." }`
  - Rate limit: 5/minute
  - Returns: `{ "message": "OTP sent." }`

- `POST /auth/verify-otp`
  - Body: `{ "phone_number": "...", "code": "123456" }`
  - Returns either:
    - Existing user: `{ "needs_profile": false, "access_token": "...", "refresh_token": "..." }`
    - New user: `{ "needs_profile": true, "setup_token": "..." }`

- `POST /auth/complete-profile`
  - Body: `{ "setup_token": "...", "display_name": "...", "avatar_url": null }`
  - Returns token pair

- `POST /auth/refresh`
  - Body: `{ "refresh_token": "..." }`
  - Returns token pair

### Authenticated

- `GET /auth/users/me`
- `PATCH /auth/users/me`
  - Optional fields: `display_name`, `bio`, `presence_visible`, `receipts_visible`, `show_timestamps`
- `POST /auth/users/me/push-token`
  - Body: `{ "fcm_token": "..." }`

## Conversations

- `GET /conversations`
  - Returns conversation list with:
    - `last_message` (nullable)
    - `unread_count`
- `POST /conversations/direct`
  - Body: `{ "other_user_id": "uuid" }`
- `POST /conversations/group`
  - Body: `{ "name": "...", "member_ids": ["uuid", ...] }`
- `GET /conversations/{conversation_id}`
- `PATCH /conversations/{conversation_id}`
  - Body: `{ "name": "...", "avatar_url": "..." }`
- `POST /conversations/{conversation_id}/members`
  - Body: `{ "user_ids": ["uuid", ...] }`
- `DELETE /conversations/{conversation_id}/members`
  - Body: `{ "user_id": "uuid" }`

## Messaging REST

- `GET /conversations/{conversation_id}/messages?before_id=<uuid>&limit=<1..100>`
- `GET /messages/missed?since=<ISO8601>`
- `DELETE /messages/{message_id}` (delete for everyone, sender only, 60-minute window)

## Reactions / Search / Preview

- `POST /messages/{message_id}/react`
  - Body: `{ "emoji": "👍" }`
- `GET /conversations/{conversation_id}/messages/search?q=...&limit=...`
- `GET /users/search?q=...&limit=...`
- `GET /users/{user_id}`
- `GET /meta/preview?url=...`
  - Rate limit: 30/minute

## Pins

- `POST /conversations/{conversation_id}/pins`
  - Body: `{ "message_id": "uuid" }`
- `DELETE /conversations/{conversation_id}/pins/{message_id}`
- `GET /conversations/{conversation_id}/pins`

## Polls

- Message send supports `type = "poll"` with `poll_data` JSON string
- `POST /messages/{message_id}/vote`
  - Body: `{ "option_indices": [0] }`
  - Emits WS `poll.vote`

## Anxiety / Focus / Disappearing

- `PATCH /users/me/focus`
  - Body: `{ "profile": "normal|quiet|off" }`
- `POST /conversations/{conversation_id}/mute`
  - Body: `{ "duration": "1h|8h|1w|always" }`
- `DELETE /conversations/{conversation_id}/mute`
- `PATCH /conversations/{conversation_id}/disappear`
  - Body: `{ "seconds": null|3600|86400|604800 }`

## Statuses

- `POST /statuses`
  - Body: `{ "type": "text|image|video", "content": "...", "media_url": "...", "bg_color": "#..." }`
- `GET /statuses/feed`
- `GET /statuses/mine`
- `POST /statuses/{status_id}/view`
- `DELETE /statuses/{status_id}`

## Calls

- `GET /calls?limit=20&before_id=<uuid>`

Call signaling is WS-only (below).

## Media

- `POST /media/upload` multipart form with `file`
  - Rate limit: 20/minute
- `POST /media/avatar` multipart form with `file`

## WebSocket Inbound (Client -> Server)

- `ping`
  - Payload: `{}`
  - Expected every 30 seconds
- `message.send`
  - Payload:
    - `conversation_id` (uuid)
    - `type` (`text|image|video|audio|document|poll`)
    - `content` (nullable)
    - `media_url` (nullable)
    - `media_mime` (nullable)
    - `reply_to_id` (nullable uuid)
    - `poll_data` (nullable JSON string)
- `message.ack`
- `message.read`
- `typing.start`
- `typing.stop`
- `call.offer`
  - `{ call_id, conversation_id, callee_id, type, sdp }`
- `call.answer`
  - `{ call_id, sdp }`
- `call.ice_candidate` (also accepts `call.ice-candidate`)
  - `{ call_id, target_user_id, candidate }`
- `call.reject`
- `call.end`

## WebSocket Outbound (Server -> Client)

- `pong`
- `message.new`
- `message.sent`
- `message.delivered`
- `message.read`
- `message.deleted`
- `typing.start`
- `typing.stop`
- `presence.update`
- `reaction.add`
- `reaction.remove`
- `message.pinned`
- `message.unpinned`
- `poll.vote`
- `status.new`
- `call.offer`
- `call.answer`
- `call.ice-candidate`
- `call.rejected`
- `call.ended`
- `error`

## MessageOut Notes

`MessageOut` now includes:
- `deleted_at`
- `reactions` map (`emoji -> count`)
- `my_reaction`
- `poll_data`

Deleted message behavior:
- message remains in history
- `content` is null
- `media_url` is null
- `deleted_at` is set


Greenfield Android plan
Phase 1 — Contract freeze (must-do first)
Use this as the single source of truth.
Lock event names exactly (call.ice_candidate inbound; server may emit call.ice-candidate outbound).
Freeze DTOs and endpoint paths before UI coding.
Phase 2 — App architecture
Kotlin + Coroutines + Flow + Hilt + Retrofit/OkHttp + Room + WorkManager.
Modules: core, auth, contacts, conversations, messages, statuses, calls, settings.
Offline-first repositories (Room as source of truth; WS/REST sync into DB).
Phase 3 — Realtime core
Single WS manager with auth token, reconnect policy, exponential backoff.
Heartbeat sender every 30s (ping), timeout handler if no pong/messages.
Missed-message recovery on reconnect using GET /messages/missed?since=....
Phase 4 — Feature implementation (client-specific UX included)
auth: OTP flow, setup-token branching, token refresh interceptor.
chat: optimistic message.send, delivery/read state machine, delete tombstones.
reactions: any-emoji picker + aggregate counters + my_reaction.
disappearing: per-conversation timer selector with presets.
statuses: story tray rings, grouped feed, viewed/unviewed segmentation, progress UI.
polls: dynamic single/multi-choice UI; live tally updates from poll.vote.
pins: pinned banner in thread + pinned list screen.
calls: incoming-call full-screen notification, ringtone + vibration, answer/reject, WebRTC signaling relay.
focus/mute: clear UX toggles mapping to backend enums.
Phase 5 — Reliability/security polish
Foreground service policy for active calls.
FCM handling for incoming_call and message knocks.
Strict schema parsing with fallback logging (unknown event telemetry).
Crash-safe local queue for unsent messages when offline.