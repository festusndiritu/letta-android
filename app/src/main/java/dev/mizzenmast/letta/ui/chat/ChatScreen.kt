package dev.mizzenmast.letta.ui.chat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.emoji2.emojipicker.EmojiPickerView
import androidx.emoji2.text.EmojiCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import dev.mizzenmast.letta.data.local.entity.ConversationMemberEntity
import dev.mizzenmast.letta.data.local.entity.MessageEntity
import dev.mizzenmast.letta.data.remote.dto.LinkPreviewDto
import dev.mizzenmast.letta.data.remote.dto.MessageDto
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    onBack: () -> Unit,
    onGroupInfoClick: () -> Unit,
    onProfileClick: (String) -> Unit,
    viewModel: ChatViewModel = hiltViewModel(),
) {
    LaunchedEffect(conversationId) {
        viewModel.init(conversationId)
    }

    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val conversation by viewModel.conversation.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val presence by viewModel.presence.collectAsStateWithLifecycle()
    val lastSeen by viewModel.lastSeen.collectAsStateWithLifecycle()
    val linkPreviews by viewModel.linkPreviews.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isSearching by viewModel.isSearching.collectAsStateWithLifecycle()
    val searchError by viewModel.searchError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val inputFocusRequester = remember { FocusRequester() }

    val currentUserId = viewModel.currentUserId
    val otherMember = remember(members, currentUserId) {
        members.firstOrNull { it.userId != currentUserId } ?: members.firstOrNull()
    }
    val titleText = if (conversation?.type == "direct") {
        otherMember?.displayName ?: "Direct message"
    } else {
        conversation?.name ?: "Chat"
    }
    val isOtherOnline = otherMember?.userId?.let { presence[it] } == true
    val lastSeenText = otherMember?.userId?.let { id -> lastSeen[id]?.let { formatLastSeen(it) } }
    val isOtherTyping = otherMember?.userId?.let { id -> uiState.typingUserIds.contains(id) } == true

    val listState = rememberLazyListState()
    var messageText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }

    val reactionsByMessage by viewModel.reactions.collectAsStateWithLifecycle()
    val messageMap = remember(messages) { messages.associateBy { it.id } }

    var replyToMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var activeReactionMessageId by remember { mutableStateOf<String?>(null) }
    var pendingMediaUri by remember { mutableStateOf<Uri?>(null) }
    var pendingMediaMime by remember { mutableStateOf<String?>(null) }
    var pendingMediaCaption by remember { mutableStateOf("") }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    val attachmentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSearchSheet by remember { mutableStateOf(false) }
    var showEmojiSheet by remember { mutableStateOf(false) }
    var expandedImageUrl by remember { mutableStateOf<String?>(null) }
    var expandedVideoUrl by remember { mutableStateOf<String?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var recordingElapsedMs by remember { mutableStateOf(0L) }
    val reactionPresets = remember { listOf("👍", "❤️", "😂", "😮", "😢", "🔥") }

    val swipeThresholdPx = with(LocalDensity.current) { 64.dp.toPx() }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    LaunchedEffect(messageText) {
        if (messageText.isEmpty()) {
            if (isTyping) {
                isTyping = false
                viewModel.sendTypingStop()
            }
            return@LaunchedEffect
        }

        if (!isTyping) {
            isTyping = true
            viewModel.sendTypingStart()
        }

        delay(1500)
        if (messageText.isNotEmpty()) {
            isTyping = false
            viewModel.sendTypingStop()
        }
    }

    val attachmentPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            viewModel.clearMediaError()
            pendingMediaUri = uri
            pendingMediaMime = context.contentResolver.getType(uri)
        }
        showAttachmentMenu = false
    }

    fun launchPicker(mimeType: String) {
        attachmentPicker.launch(mimeType)
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
    ) { bitmap ->
        if (bitmap != null) {
            val uri = cacheBitmap(bitmap, context.cacheDir)
            if (uri != null) {
                viewModel.clearMediaError()
                pendingMediaUri = uri
                pendingMediaMime = "image/jpeg"
            } else {
                viewModel.setMediaError("Unable to capture photo")
            }
        }
    }

    fun startRecording() {
        try {
            val file = File(context.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
            val newRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            recorder = newRecorder
            recordingFile = file
            isRecording = true
        } catch (e: Exception) {
            recorder?.release()
            recorder = null
            recordingFile = null
            isRecording = false
            viewModel.setMediaError(e.message ?: "Failed to start recording")
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            startRecording()
        } else {
            viewModel.setMediaError("Microphone permission denied")
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            recorder?.release()
            recorder = null
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            val startedAt = System.currentTimeMillis()
            while (isActive && isRecording) {
                recordingElapsedMs = System.currentTimeMillis() - startedAt
                delay(250)
            }
        } else {
            recordingElapsedMs = 0L
        }
    }

    fun cancelRecording() {
        if (!isRecording) return
        try {
            recorder?.stop()
        } catch (_: Exception) {
        } finally {
            recorder?.release()
            recorder = null
            isRecording = false
        }
        recordingFile?.delete()
        recordingFile = null
    }

    fun stopRecordingAndSend() {
        val file = recordingFile
        if (!isRecording || file == null) return
        try {
            recorder?.stop()
        } catch (_: Exception) {
        } finally {
            recorder?.release()
            recorder = null
            isRecording = false
            recordingFile = null
        }
        if (!file.exists() || file.length() == 0L) {
            viewModel.setMediaError("Recording failed. Please try again.")
            return
        }
        viewModel.clearMediaError()
        viewModel.uploadAndSendMedia(Uri.fromFile(file), "audio/mp4")
    }

    LaunchedEffect(showEmojiSheet) {
        if (showEmojiSheet) {
            keyboardController?.hide()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column(
                        modifier = Modifier.then(
                            if (conversation?.type == "direct" && otherMember != null) {
                                Modifier.clickable { onProfileClick(otherMember.userId) }
                            } else Modifier
                        )
                    ) {
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (conversation?.type == "direct") {
                            when {
                                isOtherTyping -> {
                                    Text(
                                        text = "Typing...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                isOtherOnline -> {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary),
                                        )
                                        Text(
                                            text = "Online",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                lastSeenText != null -> {
                                    Text(
                                        text = "Last seen $lastSeenText",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                },
                actions = {
                    if (conversation?.type == "group") {
                        IconButton(onClick = onGroupInfoClick) {
                            Icon(Icons.Rounded.Info, contentDescription = "Group info")
                        }
                    } else if (conversation?.type == "direct" && otherMember != null) {
                        IconButton(onClick = { onProfileClick(otherMember.userId) }) {
                            Icon(Icons.Rounded.Info, contentDescription = "Profile")
                        }
                    }
                    IconButton(onClick = { showSearchSheet = true }) {
                        Icon(Icons.Rounded.Search, contentDescription = "Search messages")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .imePadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (replyToMessage != null) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = "Replying to ${replyToMessage?.senderName?.ifBlank { "Message" } ?: "Message"}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    Text(
                                        text = replyToMessage?.content ?: "",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                IconButton(onClick = { replyToMessage = null }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Cancel reply")
                                }
                            }
                        }
                    }

                    if (isRecording) {
                        RecordingBar(
                            elapsedMs = recordingElapsedMs,
                            onCancel = { cancelRecording() },
                            onSend = { stopRecordingAndSend() },
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            val showCamera = messageText.isBlank() &&
                                !uiState.isUploadingMedia &&
                                pendingMediaUri == null
                            OutlinedTextField(
                                value = messageText,
                                onValueChange = { messageText = it },
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(inputFocusRequester),
                                placeholder = { Text("Message") },
                                leadingIcon = {
                                    IconButton(onClick = { showEmojiSheet = true }) {
                                        Icon(
                                            Icons.Rounded.EmojiEmotions,
                                            contentDescription = "Emoji picker",
                                        )
                                    }
                                },
                                trailingIcon = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        IconButton(
                                            onClick = { showAttachmentMenu = true },
                                            enabled = !uiState.isUploadingMedia,
                                        ) {
                                            Icon(Icons.Rounded.AttachFile, contentDescription = "Attach file")
                                        }
                                        if (showCamera) {
                                            IconButton(
                                                onClick = { cameraLauncher.launch(null) },
                                                enabled = !uiState.isUploadingMedia,
                                            ) {
                                                Icon(Icons.Rounded.PhotoCamera, contentDescription = "Camera")
                                            }
                                        }
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    capitalization = KeyboardCapitalization.Sentences,
                                    imeAction = ImeAction.Send,
                                ),
                                keyboardActions = KeyboardActions(
                                    onSend = {
                                        if (messageText.isNotBlank()) {
                                            viewModel.sendMessage(messageText, replyToMessage?.id)
                                            messageText = ""
                                            replyToMessage = null
                                            isTyping = false
                                            viewModel.sendTypingStop()
                                        }
                                    },
                                ),
                                shape = RoundedCornerShape(24.dp),
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                ),
                            )

                            val canSend = messageText.isNotBlank()
                            IconButton(
                                onClick = {
                                    if (canSend) {
                                        viewModel.sendMessage(messageText, replyToMessage?.id)
                                        messageText = ""
                                        replyToMessage = null
                                        isTyping = false
                                        viewModel.sendTypingStop()
                                    } else {
                                        val granted = ContextCompat.checkSelfPermission(
                                            context,
                                            Manifest.permission.RECORD_AUDIO,
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (granted) {
                                            startRecording()
                                        } else {
                                            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                        }
                                    }
                                },
                                enabled = !uiState.isUploadingMedia,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (canSend) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant,
                                    ),
                            ) {
                                Icon(
                                    imageVector = if (canSend) Icons.AutoMirrored.Rounded.Send else Icons.Rounded.Mic,
                                    contentDescription = if (canSend) "Send" else "Record voice",
                                    tint = if (canSend) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }

                    if (uiState.mediaError != null) {
                        Text(
                            text = uiState.mediaError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 12.dp),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            if (messages.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Chat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp),
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = "Start the conversation",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                text = "Send a message or attach a file.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            )
                        }
                    }
                }
            }

            item {
                if (uiState.isLoadingMore) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
            }

            items(messages, key = { it.id }) { message ->
                val replyMessage = message.replyToId?.let { messageMap[it] }
                val userReactions = reactionsByMessage[message.id].orEmpty()
                val reactionCounts = remember(userReactions) {
                    userReactions.values.groupingBy { it }.eachCount()
                }

                MessageItem(
                    message = message,
                    showSenderName = conversation?.type == "group" && !message.isMine,
                    replyMessage = replyMessage,
                    reactionCounts = reactionCounts,
                    showReactionPicker = activeReactionMessageId == message.id,
                    reactionPresets = reactionPresets,
                    onReact = { emoji ->
                        viewModel.reactToMessage(message.id, emoji)
                        activeReactionMessageId = null
                    },
                    onLongPress = {
                        activeReactionMessageId = if (activeReactionMessageId == message.id) null else message.id
                    },
                    onSwipeReply = {
                        replyToMessage = message
                        activeReactionMessageId = null
                    },
                    onImageClick = { url -> expandedImageUrl = url },
                    onVideoClick = { url -> expandedVideoUrl = url },
                    swipeThresholdPx = swipeThresholdPx,
                    linkPreviews = linkPreviews,
                    onRequestLinkPreview = { viewModel.requestLinkPreview(it) },
                )
            }
        }
    }

    if (expandedImageUrl != null) {
        Dialog(
            onDismissRequest = { expandedImageUrl = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)),
            ) {
                AsyncImage(
                    model = expandedImageUrl,
                    contentDescription = "Expanded image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .clip(RoundedCornerShape(16.dp)),
                )
                IconButton(
                    onClick = { expandedImageUrl = null },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close image")
                }
            }
        }
    }

    if (expandedVideoUrl != null) {
        FullscreenVideoDialog(
            url = expandedVideoUrl ?: "",
            onDismiss = { expandedVideoUrl = null },
        )
    }

    if (showAttachmentMenu) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentMenu = false },
            sheetState = attachmentSheetState,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Attach",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                val actions = listOf(
                    AttachmentItem(Icons.Rounded.Image, "Photos") { launchPicker("image/*") },
                    AttachmentItem(Icons.Rounded.Videocam, "Videos") { launchPicker("video/*") },
                    AttachmentItem(Icons.Rounded.Audiotrack, "Audio") { launchPicker("audio/*") },
                    AttachmentItem(Icons.Rounded.Description, "Docs") { launchPicker("*/*") },
                    AttachmentItem(Icons.Rounded.AttachFile, "All files") { launchPicker("*/*") },
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 200.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    gridItems(actions) { item ->
                        AttachmentActionCard(
                            icon = item.icon,
                            label = item.label,
                            onClick = item.onClick,
                        )
                    }
                }
            }
        }
    }

    if (pendingMediaUri != null) {
        val isImage = pendingMediaMime?.startsWith("image/") == true
        AlertDialog(
            onDismissRequest = { pendingMediaUri = null },
            title = { Text("Send attachment?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (isImage) {
                        AsyncImage(
                            model = pendingMediaUri,
                            contentDescription = "Attachment preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .clip(RoundedCornerShape(12.dp)),
                        )
                    } else {
                        Text("This file will be sent to the chat.")
                    }
                    OutlinedTextField(
                        value = pendingMediaCaption,
                        onValueChange = { pendingMediaCaption = it },
                        label = { Text("Caption") },
                        placeholder = { Text("Add a caption") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = pendingMediaUri
                        if (uri != null) {
                            viewModel.uploadAndSendMedia(uri, pendingMediaMime, pendingMediaCaption)
                        }
                        pendingMediaUri = null
                        pendingMediaMime = null
                        pendingMediaCaption = ""
                    },
                ) { Text("Send") }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingMediaUri = null
                    pendingMediaMime = null
                    pendingMediaCaption = ""
                }) { Text("Cancel") }
            },
        )
    }

    if (showSearchSheet) {
        MessageSearchSheet(
            members = members,
            results = searchResults,
            isSearching = isSearching,
            errorMessage = searchError,
            onDismiss = {
                showSearchSheet = false
                viewModel.clearSearchResults()
            },
            onSearch = { query -> viewModel.searchMessages(query) },
        )
    }

    if (showEmojiSheet) {
        EmojiPickerSheet(
            onDismiss = { showEmojiSheet = false },
            onEmojiSelected = { emoji ->
                messageText += emojiForInsert(emoji)
            },
            onBackspace = {
                messageText = removeLastGlyph(messageText)
            },
            onShowKeyboard = {
                showEmojiSheet = false
                inputFocusRequester.requestFocus()
                keyboardController?.show()
            },
        )
    }

    val firstVisibleIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    LaunchedEffect(firstVisibleIndex) {
        if (firstVisibleIndex == 0 && messages.isNotEmpty()) {
            viewModel.loadMoreMessages()
        }
    }
}

@Composable
private fun MessageItem(
    message: MessageEntity,
    showSenderName: Boolean,
    replyMessage: MessageEntity?,
    reactionCounts: Map<String, Int>,
    showReactionPicker: Boolean,
    reactionPresets: List<String>,
    onReact: (String) -> Unit,
    onLongPress: () -> Unit,
    onSwipeReply: () -> Unit,
    onImageClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    swipeThresholdPx: Float,
    linkPreviews: Map<String, LinkPreviewDto>,
    onRequestLinkPreview: (String) -> Unit,
) {
    var dragOffset by remember(message.id) { mutableStateOf(0f) }
    val clampedOffset = dragOffset.coerceIn(0f, swipeThresholdPx * 1.2f)
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(message.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        if (dragOffset >= swipeThresholdPx) {
                            onSwipeReply()
                        }
                        dragOffset = 0f
                    },
                    onDragCancel = { dragOffset = 0f },
                ) { change, dragAmount ->
                    if (dragAmount > 0) {
                        dragOffset = (dragOffset + dragAmount).coerceIn(0f, swipeThresholdPx * 1.2f)
                        change.consume()
                    }
                }
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {},
                onLongClick = onLongPress,
            ),
    ) {
        MessageBubble(
            message = message,
            showSenderName = showSenderName,
            replyMessage = replyMessage,
            reactionCounts = reactionCounts,
            onImageClick = onImageClick,
            onVideoClick = onVideoClick,
            linkPreviews = linkPreviews,
            onRequestLinkPreview = onRequestLinkPreview,
            modifier = Modifier.graphicsLayer { translationX = clampedOffset },
        )

        if (showReactionPicker) {
            Row(
                modifier = Modifier
                    .padding(start = if (message.isMine) 0.dp else 12.dp, top = 4.dp)
                    .align(if (message.isMine) Alignment.End else Alignment.Start),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                reactionPresets.forEach { emoji ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable { onReact(emoji) },
                    ) {
                        Text(
                            text = emoji,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: MessageEntity,
    showSenderName: Boolean,
    replyMessage: MessageEntity?,
    reactionCounts: Map<String, Int>,
    onImageClick: (String) -> Unit,
    onVideoClick: (String) -> Unit,
    linkPreviews: Map<String, LinkPreviewDto>,
    onRequestLinkPreview: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val bubbleColor = if (message.isMine) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor = if (message.isMine) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val replyBackground = if (message.isMine) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val replyTextColor = if (message.isMine) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val replyLabelColor = if (message.isMine) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val context = LocalContext.current
    fun openMedia(url: String, mime: String?) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(Uri.parse(url), mime ?: "*/*")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    fun openUrl(url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isMine) Alignment.End else Alignment.Start,
    ) {
        if (showSenderName) {
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
            )
        }

        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if (message.isMine) 18.dp else 4.dp,
                bottomEnd = if (message.isMine) 4.dp else 18.dp,
            ),
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (replyMessage != null) {
                    Surface(
                        color = replyBackground,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Column(Modifier.padding(8.dp)) {
                            Text(
                                text = replyMessage.senderName.ifBlank { "Message" },
                                style = MaterialTheme.typography.labelSmall,
                                color = replyLabelColor,
                            )
                            Text(
                                text = replyMessage.content ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = replyTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                when (message.type) {
                    "text" -> {
                        Text(
                            text = message.content ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                        )
                        val candidate = remember(message.content) { extractFirstUrl(message.content) }
                        val normalized = remember(candidate) { candidate?.let { normalizeUrl(it) } }
                        if (candidate != null && normalized != null && shouldPreviewLink(candidate, message.content)) {
                            LaunchedEffect(normalized) { onRequestLinkPreview(normalized) }
                            val preview = linkPreviews[normalized]
                            if (preview != null) {
                                LinkPreviewCard(
                                    preview = preview,
                                    isMine = message.isMine,
                                    onClick = { openUrl(preview.url) },
                                )
                            }
                        }
                    }
                    "image" -> {
                        if (message.mediaUrl != null) {
                            AsyncImage(
                                model = message.mediaUrl,
                                contentDescription = "Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onImageClick(message.mediaUrl) },
                            )
                        } else {
                            Text(
                                text = "Image",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor,
                            )
                        }
                    }
                    "audio" -> {
                        if (message.mediaUrl != null) {
                            AudioPlayerBubble(
                                url = message.mediaUrl,
                                mimeType = message.mediaMime,
                                isMine = message.isMine,
                            )
                        } else {
                            Text(
                                text = "Audio",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor,
                            )
                        }
                    }
                    "video" -> {
                        if (message.mediaUrl != null) {
                            VideoPlayerBubble(
                                url = message.mediaUrl,
                                onExpand = { onVideoClick(message.mediaUrl) },
                            )
                        } else {
                            Text(
                                text = "Video",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor,
                            )
                        }
                    }
                    "document" -> {
                        if (message.mediaUrl != null) {
                            DocumentAttachment(
                                url = message.mediaUrl,
                                mimeType = message.mediaMime,
                                onOpen = { openMedia(message.mediaUrl, message.mediaMime) },
                            )
                        } else {
                            Text(
                                text = "Document",
                                style = MaterialTheme.typography.bodyMedium,
                                color = textColor,
                            )
                        }
                    }
                    else -> {
                        Text(
                            text = message.content ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                        )
                    }
                }
                val caption = message.content?.takeIf { it.isNotBlank() }
                if (message.type != "text" && !caption.isNullOrBlank()) {
                    Text(
                        text = caption,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                    )
                }
            }
        }

        if (reactionCounts.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .align(if (message.isMine) Alignment.End else Alignment.Start),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                reactionCounts.forEach { (emoji, count) ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {
                        Text(
                            text = "$emoji $count",
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }

        if (message.isMine) {
            val (icon, tint) = when {
                message.readAt != null -> Icons.Rounded.DoneAll to MaterialTheme.colorScheme.primary
                message.deliveredAt != null -> Icons.Rounded.DoneAll to MaterialTheme.colorScheme.onSurfaceVariant
                message.isPending -> null to MaterialTheme.colorScheme.onSurfaceVariant
                else -> Icons.Rounded.Done to MaterialTheme.colorScheme.onSurfaceVariant
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 4.dp, top = 2.dp),
            ) {
                if (icon != null) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(14.dp),
                    )
                }
                if (message.isPending) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "Sending...",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun RecordingBar(
    elapsedMs: Long,
    onCancel: () -> Unit,
    onSend: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error),
                )
                Text(
                    text = "Recording ${formatDuration(elapsedMs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onCancel) {
                    Icon(Icons.Rounded.Close, contentDescription = "Cancel recording")
                }
                IconButton(onClick = onSend) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send recording")
                }
            }
        }
    }
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

private data class AttachmentItem(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
)

@Composable
private fun AttachmentActionCard(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp)
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(30.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AudioPlayerBubble(
    url: String,
    mimeType: String?,
    isMine: Boolean,
) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
        }
    }
    var isPlaying by remember(url) { mutableStateOf(false) }
    var durationMs by remember(url) { mutableStateOf(0L) }
    var positionMs by remember(url) { mutableStateOf(0L) }
    var sliderValue by remember(url) { mutableStateOf(0f) }
    var isSeeking by remember(url) { mutableStateOf(false) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    durationMs = max(player.duration, 0L)
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player) {
        while (isActive) {
            if (player.playbackState == Player.STATE_READY) {
                positionMs = player.currentPosition
                durationMs = max(player.duration, 0L)
            }
            delay(250)
        }
    }

    val progress = if (durationMs > 0L) positionMs / durationMs.toFloat() else 0f
    val onSurfaceColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    LaunchedEffect(progress) {
        if (!isSeeking) {
            sliderValue = progress.coerceIn(0f, 1f)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = audioLabel(mimeType),
            style = MaterialTheme.typography.labelSmall,
            color = onSurfaceColor.copy(alpha = 0.7f),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = {
                    if (isPlaying) player.pause() else player.play()
                },
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = onSurfaceColor,
                )
            }
            Slider(
                value = sliderValue,
                onValueChange = {
                    isSeeking = true
                    sliderValue = it
                },
                onValueChangeFinished = {
                    isSeeking = false
                    if (durationMs > 0L) {
                        player.seekTo((durationMs * sliderValue).toLong())
                    }
                },
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatPlaybackTime(positionMs),
                style = MaterialTheme.typography.labelSmall,
                color = onSurfaceColor.copy(alpha = 0.7f),
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun VideoPlayerBubble(
    url: String,
    onExpand: () -> Unit,
) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 220.dp)
            .clip(RoundedCornerShape(12.dp)),
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                }
            },
            modifier = Modifier.matchParentSize(),
        )
        IconButton(
            onClick = onExpand,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp),
        ) {
            Icon(Icons.Rounded.OpenInFull, contentDescription = "Open video")
        }
    }
}

@Composable
private fun FullscreenVideoDialog(
    url: String,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.7f)),
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
            ) {
                Icon(Icons.Rounded.Close, contentDescription = "Close video")
            }
        }
    }
}

@Composable
private fun DocumentAttachment(
    url: String,
    mimeType: String?,
    onOpen: () -> Unit,
) {
    val info = documentInfo(mimeType, url)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onOpen)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp),
        ) {
            Icon(
                imageVector = info.icon,
                contentDescription = null,
                modifier = Modifier.padding(8.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = info.label,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = url,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private data class DocumentMeta(val label: String, val icon: ImageVector)

private fun documentInfo(mimeType: String?, url: String?): DocumentMeta {
    val mime = mimeType?.lowercase().orEmpty()
    return when {
        mime.contains("pdf") || url?.endsWith(".pdf", true) == true ->
            DocumentMeta("PDF", Icons.Rounded.PictureAsPdf)
        mime == "application/msword" || mime.contains("wordprocessingml") ||
            url?.endsWith(".doc", true) == true || url?.endsWith(".docx", true) == true ->
            DocumentMeta("Word", Icons.AutoMirrored.Rounded.Article)
        mime == "text/plain" || url?.endsWith(".txt", true) == true ->
            DocumentMeta("Text", Icons.Rounded.Description)
        else -> DocumentMeta("Document", Icons.AutoMirrored.Rounded.InsertDriveFile)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MessageSearchSheet(
    members: List<ConversationMemberEntity>,
    results: List<MessageDto>,
    isSearching: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSearch: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Search messages",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search") },
                placeholder = { Text("Type a keyword") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(
                    onSearch = { onSearch(query) },
                ),
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { onSearch(query) }) {
                        Icon(Icons.Rounded.Search, contentDescription = "Search")
                    }
                },
            )

            when {
                isSearching -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
                errorMessage != null -> {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                results.isEmpty() -> {
                    Text(
                        text = if (query.isBlank()) "Enter a keyword to search." else "No results.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 360.dp),
                    ) {
                        items(results, key = { it.id }) { result ->
                            val senderName = members.firstOrNull { it.userId == result.senderId }?.displayName
                                ?: "Unknown"
                            val preview = searchPreview(result)
                            val time = formatCreatedAt(result.createdAt)
                            ListItem(
                                headlineContent = {
                                    Text(preview, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                },
                                supportingContent = {
                                    Text(
                                        text = listOfNotNull(senderName, time).joinToString(" • "),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmojiPickerSheet(
    onDismiss: () -> Unit,
    onEmojiSelected: (String) -> Unit,
    onBackspace: () -> Unit,
    onShowKeyboard: () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }
    var pickerView by remember { mutableStateOf<EmojiPickerView?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 360.dp, max = 520.dp)
                .navigationBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Emoji",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                IconButton(onClick = onShowKeyboard) {
                    Icon(Icons.Rounded.Keyboard, contentDescription = "Show keyboard")
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { value ->
                        searchQuery = value
                        pickerView?.let { updateEmojiSearch(it, value) }
                    },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search emoji") },
                    leadingIcon = {
                        Icon(Icons.Rounded.Search, contentDescription = null)
                    },
                    singleLine = true,
                )
                IconButton(onClick = onBackspace) {
                    Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = "Backspace")
                }
            }
            AndroidView(
                factory = { ctx ->
                    EmojiPickerView(ctx).apply {
                        setOnEmojiPickedListener { item ->
                            onEmojiSelected(item.emoji)
                        }
                        pickerView = this
                        updateEmojiSearch(this, searchQuery)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = true),
            )
        }
    }
}

private fun updateEmojiSearch(view: EmojiPickerView, query: String) {
    val method = view.javaClass.methods.firstOrNull { method ->
        val name = method.name
        val paramTypes = method.parameterTypes
        val acceptsQuery = paramTypes.size == 1 &&
            (paramTypes[0] == String::class.java || CharSequence::class.java.isAssignableFrom(paramTypes[0]))
        acceptsQuery && name in setOf("setSearchQuery", "setSearchText", "setQuery", "setSearchTerm")
    }
    if (method != null) {
        runCatching { method.invoke(view, query) }
    }
}

private fun removeLastGlyph(text: String): String {
    if (text.isEmpty()) return text
    val end = text.length
    val count = text.codePointCount(0, end)
    if (count <= 0) return ""
    val newEnd = text.offsetByCodePoints(0, count - 1)
    return text.substring(0, newEnd)
}

private fun waveformHeights(seed: String, count: Int): List<Float> {
    if (count <= 0) return emptyList()
    val base = (seed.hashCode() % 1000) / 1000.0
    return List(count) { index ->
        val value = abs(sin((index + 1) * 0.7 + base))
        (0.35 + (value * 0.65)).toFloat()
    }
}

private fun audioLabel(mimeType: String?): String {
    val mime = mimeType?.lowercase().orEmpty()
    return when {
        mime.contains("mpeg") -> "Audio • MP3"
        mime.contains("ogg") -> "Audio • OGG"
        mime.contains("mp4") -> "Audio • MP4"
        mime.contains("webm") -> "Audio • WebM"
        else -> "Audio"
    }
}

private fun formatPlaybackTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format(Locale.US, "%d:%02d", minutes, seconds)
}

private fun formatLastSeen(epochMs: Long): String {
    val zoned = Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault())
    val today = java.time.LocalDate.now(zoned.zone)
    return if (zoned.toLocalDate() == today) {
        zoned.format(DateTimeFormatter.ofPattern("HH:mm"))
    } else {
        zoned.format(DateTimeFormatter.ofPattern("MMM d, HH:mm"))
    }
}

private fun formatCreatedAt(createdAt: String): String {
    return runCatching {
        val zoned = Instant.parse(createdAt).atZone(ZoneId.systemDefault())
        zoned.format(DateTimeFormatter.ofPattern("MMM d, HH:mm"))
    }.getOrElse { "" }
}

@Composable
private fun LinkPreviewCard(
    preview: LinkPreviewDto,
    isMine: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (isMine) {
        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val textColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val title = preview.title?.trim().orEmpty()
    val description = preview.description?.trim().orEmpty()
    val siteName = preview.siteName?.trim().orEmpty()

    Surface(
        color = containerColor,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!preview.imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = preview.imageUrl,
                    contentDescription = preview.title ?: "Link preview",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                if (title.isNotEmpty()) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (description.isNotEmpty()) {
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = textColor.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (siteName.isNotEmpty()) {
                    Text(
                        text = siteName,
                        style = MaterialTheme.typography.labelSmall,
                        color = textColor.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

private fun searchPreview(message: MessageDto): String {
    val content = message.content?.trim().orEmpty()
    if (content.isNotEmpty()) return content
    return when (message.type) {
        "image" -> "Photo"
        "video" -> "Video"
        "audio" -> "Audio"
        "document" -> "Document"
        else -> "Message"
    }
}

private fun emojiForInsert(emoji: String): String {
    return EmojiCompat.get().process(emoji).toString()
}

private fun cacheBitmap(bitmap: Bitmap, cacheDir: File): Uri? {
    return try {
        val file = File(cacheDir, "camera_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }
        Uri.fromFile(file)
    } catch (_: Exception) {
        null
    }
}

private fun extractFirstUrl(text: String?): String? {
    if (text.isNullOrBlank()) return null
    val regex = Regex("""((https?://)?([A-Za-z0-9-]+\.)+[A-Za-z]{2,}[^\s]*)""")
    val match = regex.find(text) ?: return null
    return match.value.trim().trimEnd('.', ',', '!', '?', ')', ']', '}', '"', '\'')
}

private fun normalizeUrl(raw: String): String {
    return if (raw.startsWith("http://", true) || raw.startsWith("https://", true)) {
        raw
    } else {
        "https://$raw"
    }
}

private fun shouldPreviewLink(url: String, fullText: String?): Boolean {
    val trimmed = fullText?.trim().orEmpty()
    val cleaned = url.trim().trimEnd('.', ',', '!', '?', ')', ']', '}', '"', '\'')
    val hasScheme = cleaned.startsWith("http://", true) || cleaned.startsWith("https://", true)
    val hasWww = cleaned.startsWith("www.", true)
    return hasScheme || hasWww || trimmed.equals(cleaned, ignoreCase = false)
}

