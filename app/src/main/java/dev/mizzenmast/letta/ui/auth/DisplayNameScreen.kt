package dev.mizzenmast.letta.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun DisplayNameScreen(
    uiState: AuthUiState,
    onSetDisplayName: (String) -> Unit,  // avatar uploaded separately post-login
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    var displayName by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    var visible by remember { mutableStateOf(false) }

    val error = uiState.error ?: localError
    val canSubmit = displayName.trim().length >= 2 && !uiState.isLoading

    LaunchedEffect(Unit) {
        delay(100)
        visible = true
        delay(300)
        focusRequester.requestFocus()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .padding(horizontal = dev.mizzenmast.letta.ui.components.LettaSpacing.huge),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
        ) {
            Column {
                Text(
                    text = "What's your\nname?",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-0.5).sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "This is how you'll appear\nto people you message.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp,
                )

                Spacer(modifier = Modifier.height(40.dp))


                // ── Display name field ───────────────────────────────────────
                OutlinedTextField(
                    value = displayName,
                    onValueChange = {
                        if (it.length <= 30) {
                            displayName = it
                            localError = null
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    label = { Text("Display name") },
                    placeholder = { Text("e.g. Festus") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (canSubmit) {
                                keyboard?.hide()
                                onSetDisplayName(displayName.trim())
                            }
                        }
                    ),
                    isError = error != null,
                    supportingText = {
                        if (error != null) {
                            Text(error, color = MaterialTheme.colorScheme.error)
                        } else {
                            Text(
                                "${displayName.trim().length}/30",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            )
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                    ),
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val trimmed = displayName.trim()
                        if (trimmed.length < 2) {
                            localError = "Name must be at least 2 characters."
                            return@Button
                        }
                        keyboard?.hide()
                        onSetDisplayName(trimmed)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    enabled = canSubmit,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                    ),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(22.dp),
                        )
                    } else {
                        Text("Continue", style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
    }
}