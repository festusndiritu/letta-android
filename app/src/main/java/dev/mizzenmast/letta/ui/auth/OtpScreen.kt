package dev.mizzenmast.letta.ui.auth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.google.android.gms.auth.api.phone.SmsRetriever
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OtpScreen(
    phoneNumber: String,
    uiState: AuthUiState,
    onVerifyOtp: (String) -> Unit,
    onResendOtp: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    var otpValue by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    var countdown by remember { mutableIntStateOf(30) }
    var canResend by remember { mutableStateOf(false) }

    val error = uiState.error ?: localError

    // ── SMS Retriever ─────────────────────────────────────────────────────────
    DisposableEffect(Unit) {
        val client = SmsRetriever.getClient(context)
        client.startSmsRetriever()

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (SmsRetriever.SMS_RETRIEVED_ACTION != intent.action) return

                val extras = intent.extras ?: return

                // On API 30+ the extras bundle may contain a consent intent instead of
                // the raw message when User Consent API is active — handle both.
                @Suppress("DEPRECATION")
                val status = extras.get(SmsRetriever.EXTRA_STATUS) as? Status ?: return

                when (status.statusCode) {
                    CommonStatusCodes.SUCCESS -> {
                        val smsMessage =
                            extras.getString(SmsRetriever.EXTRA_SMS_MESSAGE) ?: return
                        val code = Regex("\\b(\\d{6})\\b").find(smsMessage)?.value ?: return
                        otpValue = code
                    }
                    CommonStatusCodes.TIMEOUT -> {
                        // 5-minute retriever window expired — nothing to do, user types manually
                    }
                }
            }
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(SmsRetriever.SMS_RETRIEVED_ACTION),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        onDispose {
            try { context.unregisterReceiver(receiver) } catch (_: Exception) { }
        }
    }

    // ── Countdown ─────────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        while (countdown > 0) {
            delay(1000)
            countdown--
        }
        canResend = true
    }

    // ── Auto-focus ────────────────────────────────────────────────────────────
    LaunchedEffect(Unit) {
        delay(200)
        focusRequester.requestFocus()
    }

    // ── Auto-submit on 6 digits ───────────────────────────────────────────────
    LaunchedEffect(otpValue) {
        if (otpValue.length == 6 && !uiState.isLoading) {
            onVerifyOtp(otpValue)
        }
    }

    // ── Clear and refocus on error ────────────────────────────────────────────
    LaunchedEffect(uiState.error) {
        if (uiState.error != null) {
            otpValue = ""
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    // Hidden real text field — drives keyboard and captures input
    BasicTextField(
        value = otpValue,
        onValueChange = { new ->
            if (new.length <= 6 && new.all { it.isDigit() } && !uiState.isLoading) {
                otpValue = new
                localError = null
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier
            .size(1.dp)
            .focusRequester(focusRequester),
        cursorBrush = SolidColor(Color.Transparent),
    )

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) {
                focusRequester.requestFocus()
                keyboard?.show()
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding(),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(start = 4.dp),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface,
                )
            }

            Spacer(modifier = Modifier.height(40.dp))

            Column(modifier = Modifier.padding(horizontal = 32.dp)) {
                Text(
                    text = "Check your\nmessages",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "We sent a 6-digit code to\n$phoneNumber",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp,
                )

                Spacer(modifier = Modifier.height(48.dp))

                // ── Digit boxes ───────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    repeat(6) { index ->
                        val digit = otpValue.getOrNull(index)?.toString() ?: ""
                        val isFocused = index == otpValue.length && !uiState.isLoading

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = if (isFocused) 2.dp else 1.dp,
                                    color = when {
                                        error != null && digit.isNotEmpty() -> MaterialTheme.colorScheme.error
                                        isFocused -> MaterialTheme.colorScheme.primary
                                        digit.isNotEmpty() -> MaterialTheme.colorScheme.outline
                                        else -> Color.Transparent
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) {
                                    focusRequester.requestFocus()
                                    keyboard?.show()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = digit,
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                ),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedVisibility(visible = uiState.isLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 2.5.dp,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                AnimatedVisibility(visible = error != null && !uiState.isLoading) {
                    Text(
                        text = error ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Resend row ────────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (!canResend) {
                        Text(
                            text = "Resend in ${countdown}s",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        )
                    } else {
                        TextButton(
                            onClick = {
                                // Reset local UI state immediately; ViewModel drives the actual request
                                canResend = false
                                countdown = 30
                                otpValue = ""
                                localError = null
                                onResendOtp()

                                // Restart countdown independently of the network call
                                scope.launch {
                                    while (countdown > 0) {
                                        delay(1000)
                                        countdown--
                                    }
                                    canResend = true
                                }
                            },
                            enabled = !uiState.isLoading,
                        ) {
                            Text(
                                "Resend code",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            }
        }
    }
}