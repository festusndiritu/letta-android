package dev.mizzenmast.letta.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.InsertDriveFile
import androidx.compose.material.icons.automirrored.rounded.Reply
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Reply
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Videocam
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
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import dev.mizzenmast.letta.core.theme.LocalLettaColors
import dev.mizzenmast.letta.data.local.entity.MessageEntity
import dev.mizzenmast.letta.core.motion.HapticFeedback
import dev.mizzenmast.letta.core.motion.rememberHapticFeedback
import dev.mizzenmast.letta.ui.components.LettaInputBar
import dev.mizzenmast.letta.ui.components.ReplyPreviewBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.File
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.core.graphics.toColorInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    conversationId: String,
    onBack: () -> Unit,
    onGroupInfoClick: () -> Unit,
    onProfileClick: (String) -> Unit,
    onVoiceCall: () -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel(),
) {
    LaunchedEffect(conversationId) {
        viewModel.init(conversationId)
    }

    val haptics = rememberHapticFeedback()
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
    val wallpaperType by viewModel.wallpaperType.collectAsStateWithLifecycle()
    val wallpaperValue by viewModel.wallpaperValue.collectAsStateWithLifecycle()
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

    // Conversation-scoped state (reset when conversation changes)
    var replyToMessage by remember(conversationId) { mutableStateOf<MessageEntity?>(null) }
    var activeReactionMessageId by remember(conversationId) { mutableStateOf<String?>(null) }
    var contextMenuMessage by remember(conversationId) { mutableStateOf<MessageEntity?>(null) }
    var pendingMediaUri by remember(conversationId) { mutableStateOf<Uri?>(null) }
    var pendingMediaMime by remember(conversationId) { mutableStateOf<String?>(null) }
    var pendingMediaCaption by remember(conversationId) { mutableStateOf("") }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    val attachmentSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSearchSheet by remember { mutableStateOf(false) }
    var showEmojiSheet by remember { mutableStateOf(false) }
    var expandedImageUrl by remember { mutableStateOf<String?>(null) }
    var expandedVideoUrl by remember { mutableStateOf<String?>(null) }
    val mediaMessages = remember(messages) {
        messages.filter { (it.type == "image" || it.type == "video") && it.mediaUrl != null }
    }
    var expandedMediaIndex by remember { mutableStateOf<Int?>(null) }
    val clipboardManager = LocalClipboardManager.current
    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordingFile by remember { mutableStateOf<File?>(null) }
    var recordingElapsedMs by remember { mutableLongStateOf(0L) }
    val reactionPresets = remember { dev.mizzenmast.letta.ui.components.LettaEmojis.reactions }

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
        contentWindowInsets = WindowInsets(0),
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
                    if (conversation?.type == "direct" && otherMember != null) {
                        IconButton(onClick = { viewModel.startCall("audio") }) {
                            Icon(Icons.Rounded.Call, contentDescription = "Voice call")
                        }
                        IconButton(onClick = { viewModel.startCall("video") }) {
                            Icon(Icons.Rounded.Videocam, contentDescription = "Video call")
                        }
                        IconButton(onClick = { onProfileClick(otherMember.userId) }) {
                            Icon(Icons.Rounded.Info, contentDescription = "Profile")
                        }
                    } else if (conversation?.type == "group") {
                        IconButton(onClick = onGroupInfoClick) {
                            Icon(Icons.Rounded.Info, contentDescription = "Group info")
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Emoji panel (replaces keyboard when active)
                    AnimatedVisibility(
                        visible = showEmojiSheet,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        EmojiPickerPanel(
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
                            }
                        )
                    }

                    // Input bar section
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(
                                if (!showEmojiSheet) {
                                    Modifier
                                        .navigationBarsPadding()
                                        .imePadding()
                                } else {
                                    Modifier.navigationBarsPadding()
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        AnimatedVisibility(
                            visible = replyToMessage != null,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            replyToMessage?.let {
                                ReplyPreviewBar(
                                    replyToName = it.senderName.ifBlank { "Message" },
                                    replyToText = it.content ?: "",
                                    onCancel = { replyToMessage = null },
                                    hapticFeedback = haptics
                                )
                            }
                        }

                        if (isRecording) {
                            RecordingBar(
                                elapsedMs = recordingElapsedMs,
                                onCancel = { cancelRecording() },
                                onSend = { stopRecordingAndSend() },
                            )
                        } else {
                            LettaInputBar(
                                value = messageText,
                                onValueChange = { newText ->
                                    messageText = newText
                                    if (newText.isNotBlank() && !isTyping) {
                                        isTyping = true
                                        viewModel.sendTypingStart()
                                    } else if (newText.isBlank() && isTyping) {
                                        isTyping = false
                                        viewModel.sendTypingStop()
                                    }
                                },
                                onSend = {
                                    if (messageText.isNotBlank()) {
                                        isTyping = false
                                        viewModel.sendTypingStop()
                                        viewModel.sendMessage(messageText, replyToMessage?.id)
                                        messageText = ""
                                        replyToMessage = null
                                    }
                                },
                                onAttach = { showAttachmentMenu = true },
                                onCamera = { cameraLauncher.launch(null) },
                                onMic = {
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
                                onEmojiToggle = {
                                    if (showEmojiSheet) {
                                        showEmojiSheet = false
                                        inputFocusRequester.requestFocus()
                                        keyboardController?.show()
                                    } else {
                                        showEmojiSheet = true
                                        keyboardController?.hide()
                                    }
                                },
                                showEmojiPicker = showEmojiSheet,
                                showCamera = messageText.isBlank() && !uiState.isUploadingMedia && pendingMediaUri == null,
                                enabled = !uiState.isUploadingMedia,
                                isUploading = uiState.isUploadingMedia,
                                hapticFeedback = haptics
                            )
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
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // ── Wallpaper background ─────────────────────────────────────
            when (wallpaperType) {
                "COLOR" -> {
                    val color = runCatching {
                        Color(wallpaperValue.toColorInt())
                    }.getOrElse { MaterialTheme.colorScheme.background }
                    Box(Modifier.matchParentSize().background(color))
                }
                "IMAGE" -> {
                    AsyncImage(
                        model = wallpaperValue,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize(),
                    )
                }
                else -> Unit // "NONE" — no wallpaper
            }
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
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
                        contextMenuMessage = message
                        activeReactionMessageId = null
                    },
                    onSwipeReply = {
                        replyToMessage = message
                        activeReactionMessageId = null
                    },
                    onImageClick = { url ->
                        val idx = mediaMessages.indexOfFirst { it.mediaUrl == url }
                        if (idx >= 0) expandedMediaIndex = idx else expandedImageUrl = url
                    },
                    onVideoClick = { url ->
                        val idx = mediaMessages.indexOfFirst { it.mediaUrl == url }
                        if (idx >= 0) expandedMediaIndex = idx else expandedVideoUrl = url
                    },
                    swipeThresholdPx = swipeThresholdPx,
                    linkPreviews = linkPreviews,
                    onRequestLinkPreview = { viewModel.requestLinkPreview(it) },
                )
            }
        }
        } // end LazyColumn Box

    if (expandedMediaIndex != null) {
        val startIndex = expandedMediaIndex!!
        val pagerState = rememberPagerState(initialPage = startIndex) { mediaMessages.size }
        Dialog(
            onDismissRequest = { expandedMediaIndex = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.92f)),
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    val mediaMsg = mediaMessages[page]
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (mediaMsg.type == "image") {
                            AsyncImage(
                                model = mediaMsg.mediaUrl,
                                contentDescription = "Image ${page + 1} of ${mediaMessages.size}",
                                contentScale = ContentScale.Fit,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp),
                            )
                        } else {
                            FullscreenVideoDialog(
                                url = mediaMsg.mediaUrl ?: "",
                                onDismiss = { expandedMediaIndex = null },
                            )
                        }
                    }
                }
                // Close + counter overlay
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = { expandedMediaIndex = null }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White)
                    }
                    Text(
                        text = "${pagerState.currentPage + 1} / ${mediaMessages.size}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                    )
                    Spacer(Modifier.size(48.dp))
                }
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
                        .heightIn(min = 240.dp),
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
        Dialog(
            onDismissRequest = { pendingMediaUri = null; pendingMediaMime = null; pendingMediaCaption = "" },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Scaffold(
                contentWindowInsets = WindowInsets(0),
                topBar = {
                    Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.surface) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = {
                                pendingMediaUri = null; pendingMediaMime = null; pendingMediaCaption = ""
                            }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Cancel")
                            }
                            Text(
                                text = if (isImage) "Send photo" else "Send file",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f).padding(start = 8.dp),
                            )
                            TextButton(
                                onClick = {
                                    val uri = pendingMediaUri
                                    if (uri != null) {
                                        viewModel.uploadAndSendMedia(uri, pendingMediaMime, pendingMediaCaption.ifBlank { null })
                                    }
                                    pendingMediaUri = null; pendingMediaMime = null; pendingMediaCaption = ""
                                },
                            ) { Text("Send") }
                        }
                    }
                },
                bottomBar = {
                    Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.surface) {
                        OutlinedTextField(
                            value = pendingMediaCaption,
                            onValueChange = { pendingMediaCaption = it },
                            placeholder = { Text("Add a caption…") },
                            singleLine = false,
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .imePadding()
                                .padding(12.dp),
                        )
                    }
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black)
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isImage) {
                        AsyncImage(
                            model = pendingMediaUri,
                            contentDescription = "Preview",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.AutoMirrored.Rounded.InsertDriveFile,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(64.dp),
                            )
                            Spacer(Modifier.height(12.dp))
                            Text("File ready to send", color = Color.White)
                        }
                    }
                }
            }
        }
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

    // ── Message context menu ──────────────────────────────────────────────
    contextMenuMessage?.let { msg ->
        val menuSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { contextMenuMessage = null },
            sheetState = menuSheetState,
        ) {
            val lc = LocalLettaColors.current
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            ) {
                // Reaction quick-pick row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    reactionPresets.forEach { emoji ->
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable {
                                viewModel.reactToMessage(msg.id, emoji)
                                contextMenuMessage = null
                            },
                        ) {
                            Text(
                                text = emoji,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                // Reply
                ListItem(
                    headlineContent = { Text("Reply") },
                    leadingContent = { Icon(Icons.AutoMirrored.Rounded.Reply, contentDescription = null, tint = lc.accent) },
                    modifier = Modifier.clickable {
                        replyToMessage = msg
                        contextMenuMessage = null
                    },
                )
                // Copy text (only for text messages)
                if (!msg.content.isNullOrBlank()) {
                    ListItem(
                        headlineContent = { Text("Copy text") },
                        leadingContent = { Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = lc.accent) },
                        modifier = Modifier.clickable {
                            clipboardManager.setText(AnnotatedString(msg.content))
                            contextMenuMessage = null
                        },
                    )
                }
                // Pin / Unpin
                ListItem(
                    headlineContent = { Text("Pin message") },
                    leadingContent = { Icon(Icons.Rounded.PushPin, contentDescription = null, tint = lc.accent) },
                    modifier = Modifier.clickable {
                        viewModel.pinMessage(msg.id)
                        contextMenuMessage = null
                    },
                )
                // Forward placeholder
                ListItem(
                    headlineContent = { Text("Forward") },
                    leadingContent = { Icon(Icons.Rounded.Share, contentDescription = null, tint = lc.accent) },
                    modifier = Modifier.clickable {
                        contextMenuMessage = null
                    },
                )
                // Delete (own messages only)
                if (msg.isMine) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ListItem(
                        headlineContent = { Text("Delete", color = lc.destructive) },
                        leadingContent = {
                            Icon(Icons.Rounded.Delete, contentDescription = null, tint = lc.destructive)
                        },
                        modifier = Modifier.clickable {
                            viewModel.deleteMessage(msg.id)
                            contextMenuMessage = null
                        },
                    )
                }
            }
        }
    }

    val firstVisibleIndex by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    LaunchedEffect(firstVisibleIndex) {
        if (firstVisibleIndex == 0 && messages.isNotEmpty()) {
            viewModel.loadMoreMessages()
        }
    }
}
} // end ChatScreen
