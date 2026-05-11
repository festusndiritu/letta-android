package dev.mizzenmast.letta.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onComplete: () -> Unit) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(600),
        label = "splash_alpha",
    )

    LaunchedEffect(Unit) {
        delay(100)
        visible = true
        delay(1200)
        onComplete()
    }

    Surface {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Letta",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-1).sp,
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.alpha(alpha),
            )
        }
    }
}