package dev.mizzenmast.letta.ui.chat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import dev.mizzenmast.letta.data.local.entity.MessageEntity
import java.io.File
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
    val context = LocalContext.current

    val currentUserId = viewModel.currentUserId
    val otherMember = remember(members, currentUserId) {
        members.firstOrNull { it.userId != currentUserId } ?: members.firstOrNull()
    }
    val titleText = if (conversation?.type == "direct") {
        otherMember?.displayName ?: "Direct message"
    } else {
        conversation?.name ?: "Chat"
    }

    val listState = rememberLazyListState()
    var messageText by remember { mutableStateOf("") }
    var isTyping by remember { mutableStateOf(false) }

    val reactionsByMessage by viewModel.reactions.collectAsStateWithLifecycle()
    val messageMap = remember(messages) { messages.associateBy { it.id } }

    var replyToMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var activeReactionMessageId by remember { mutableStateOf<String?>(null) }
    var pendingMediaUri by remember { mutableStateOf<Uri?>(null) }
    var pendingMediaMime by remember { mutableStateOf<String?>(null) }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var expandedImageUrl by remember { mutableStateOf<String?>(null) }
    var expandedVideoUrl by remember { mutableStateOf<String?>(null) }
    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var recordingElapsedMs by remember { mutableStateOf(0L) }
    val reactionPresets = remember {
        listOf("👍", "❤️", "😂", "😮", "😢", "🔥")
    }

    val swipeThresholdPx = with(LocalDensity.current) { 64.dp.toPx() }

    // Scroll to bottom on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Typing indicator debounce
    LaunchedEffect(messageText) {
        if (messageText.isNotEmpty() && !isTyping) {
            isTyping = true
            viewModel.sendTypingStart()
        } else if (messageText.isEmpty() && isTyping) {
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
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        AnimatedVisibility(
                            visible = uiState.typingUserIds.isNotEmpty(),
                            enter = fadeIn(),
                            exit = fadeOut(),
                        ) {
                            Text(
                                text = "typing...",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
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

                    AnimatedVisibility(
                        visible = showAttachmentMenu && !isRecording,
                        enter = fadeIn(),
                        exit = fadeOut(),
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AttachmentAction(
                                    icon = Icons.Rounded.Image,
                                    label = "Images",
                                    onClick = { launchPicker("image/*") },
                                )
                                AttachmentAction(
                                    icon = Icons.Rounded.Videocam,
                                    label = "Videos",
                                    onClick = { launchPicker("video/*") },
                                )
                                AttachmentAction(
                                    icon = Icons.Rounded.Audiotrack,
                                    label = "Audio",
                                    onClick = { launchPicker("audio/*") },
                                )
                                AttachmentAction(
                                    icon = Icons.Rounded.Description,
                                    label = "Docs",
                                    onClick = { launchPicker("*/*") },
                                )
                                AttachmentAction(
                                    icon = Icons.Rounded.AttachFile,
                                    label = "All",
                                    onClick = { launchPicker("*/*") },
                                )
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
                            IconButton(
                                onClick = { showAttachmentMenu = !showAttachmentMenu },
                                enabled = !uiState.isUploadingMedia,
                            ) {
                                Icon(Icons.Rounded.AttachFile, contentDescription = "Attach file")
                            }

                            IconButton(
                                onClick = {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO,
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) {
                                        startRecording()
                                    } else {
                                        audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                },
                                enabled = !uiState.isUploadingMedia,
                            ) {
                                Icon(
                                    Icons.Rounded.Mic,
                                    contentDescription = "Record voice",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                )
                            }

                            OutlinedTextField(
                                value = messageText,
                                onValueChange = { messageText = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("Message") },
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
                                    }
                                ),
                                shape = RoundedCornerShape(24.dp),
                                maxLines = 4,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                ),
                            )

                            if (uiState.isUploadingMedia) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                IconButton(
                                    onClick = {
                                        if (messageText.isNotBlank()) {
                                            viewModel.sendMessage(messageText, replyToMessage?.id)
                                            messageText = ""
                                            replyToMessage = null
                                            isTyping = false
                                            viewModel.sendTypingStop()
                                        }
                                    },
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (messageText.isNotBlank()) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.Send,
                                        contentDescription = "Send",
                                        tint = if (messageText.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
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
            // Load more trigger
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

    if (pendingMediaUri != null) {
        val isImage = pendingMediaMime?.startsWith("image/") == true
        AlertDialog(
            onDismissRequest = { pendingMediaUri = null },
            title = { Text("Send attachment?") },
            text = {
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
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val uri = pendingMediaUri
                        if (uri != null) {
                            viewModel.uploadAndSendMedia(uri, pendingMediaMime)
                        }
                        pendingMediaUri = null
                        pendingMediaMime = null
                    }
                ) { Text("Send") }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingMediaUri = null
                    pendingMediaMime = null
                }) { Text("Cancel") }
            }
        )
    }

    // Load more on scroll to top
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
) {
    var dragOffset by remember(message.id) { mutableStateOf(0f) }
    val clampedOffset = dragOffset.coerceIn(0f, swipeThresholdPx * 1.2f)

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
                        modifier = Modifier
                            .clickable { onReact(emoji) }
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                    ) {
                        Text(text = emoji, style = MaterialTheme.typography.bodyMedium)
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
                data = Uri.parse(url)
                if (mime != null) setDataAndType(data, mime)
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

        Box(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomStart = if (message.isMine) 18.dp else 4.dp,
                        bottomEnd = if (message.isMine) 4.dp else 18.dp,
                    )
                )
                .background(bubbleColor)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
                    "text" -> Text(
                        text = message.content ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                    )
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

        // Status indicators for own messages
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

@Composable
private fun AttachmentAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
    var waveformWidthPx by remember(url) { mutableStateOf(0) }
    val waveformHeights = remember(url) { waveformHeights(url, 24) }

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
    val trackColor = if (isMine) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val activeColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary

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
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 28.dp)
                    .padding(horizontal = 6.dp)
                    .onSizeChanged { waveformWidthPx = it.width }
                    .pointerInput(durationMs, waveformWidthPx) {
                        detectTapGestures { offset ->
                            if (durationMs > 0L && waveformWidthPx > 0) {
                                val ratio = (offset.x / waveformWidthPx).coerceIn(0f, 1f)
                                player.seekTo((durationMs * ratio).toLong())
                            }
                        }
                    },
            ) {
                WaveformProgressBar(
                    progress = progress.coerceIn(0f, 1f),
                    barHeights = waveformHeights,
                    activeColor = activeColor,
                    inactiveColor = trackColor,
                )
            }
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
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onOpen() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(info.icon, contentDescription = info.label)
        Text(
            text = info.label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private data class DocumentInfo(
    val label: String,
    val icon: ImageVector,
)

private fun documentInfo(mimeType: String?, url: String?): DocumentInfo {
    val mime = mimeType?.lowercase().orEmpty()
    return when {
        mime == "application/pdf" || url?.endsWith(".pdf", true) == true -> {
            DocumentInfo("PDF", Icons.Rounded.PictureAsPdf)
        }
        mime == "application/msword" || mime.contains("wordprocessingml") || url?.endsWith(".doc", true) == true || url?.endsWith(".docx", true) == true -> {
            DocumentInfo("Word", Icons.AutoMirrored.Rounded.Article)
        }
        mime == "text/plain" || url?.endsWith(".txt", true) == true -> {
            DocumentInfo("Text", Icons.Rounded.Description)
        }
        else -> DocumentInfo("Document", Icons.AutoMirrored.Rounded.InsertDriveFile)
    }
}

@Composable
private fun WaveformProgressBar(
    progress: Float,
    barHeights: List<Float>,
    activeColor: Color,
    inactiveColor: Color,
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        if (barHeights.isEmpty()) return@Canvas
        val barCount = barHeights.size
        val totalBars = barCount.toFloat()
        val barWidth = size.width / (totalBars * 1.6f)
        val gap = barWidth * 0.6f
        val maxHeight = size.height
        barHeights.forEachIndexed { index, heightFactor ->
            val left = index * (barWidth + gap)
            val barHeight = (maxHeight * heightFactor).coerceAtLeast(maxHeight * 0.25f)
            val top = (maxHeight - barHeight) / 2f
            val color = if (index / totalBars <= progress) activeColor else inactiveColor
            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f),
            )
        }
    }
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
