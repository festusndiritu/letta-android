package dev.mizzenmast.letta.core.motion

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Haptic feedback engine for tactile UI interactions.
 * Provides consistent, semantic haptic patterns across the app.
 */

class HapticFeedback(context: Context) {
    
    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    
    // ══════════════════════════════════════════════════════════════════════
    // SEMANTIC HAPTIC PATTERNS
    // ══════════════════════════════════════════════════════════════════════
    
    /** Light tick - for successful actions (message sent, reaction added) */
    fun tick() {
        vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
    }
    
    /** Light click - for button presses, toggles */
    fun click() {
        vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
    }
    
    /** Heavy click - for important actions (delete, leave group) */
    fun heavyClick() {
        vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK))
    }
    
    /** Double click - for errors or warnings */
    fun doubleClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_DOUBLE_CLICK))
        } else {
            vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        }
    }
    
    /** Long press - when long-press gesture is recognized */
    fun longPress() {
        vibrate(VibrationEffect.createOneShot(50, 128))
    }
    
    /** Swipe threshold - when swipe gesture passes threshold */
    fun swipeThreshold() {
        vibrate(VibrationEffect.createOneShot(30, 100))
    }
    
    /** Selection - when selecting/multi-selecting items */
    fun selection() {
        tick()
    }
    
    /** Error - for error states or failed actions */
    fun error() {
        val pattern = longArrayOf(0, 50, 50, 50)
        val amplitudes = intArrayOf(0, 100, 0, 150)
        vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
    }
    
    /** Success - for successful completion of long operations */
    fun success() {
        val pattern = longArrayOf(0, 30, 30, 50)
        val amplitudes = intArrayOf(0, 80, 0, 120)
        vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
    }
    
    /** Impact light - for subtle interactions (button hover, gesture start) */
    fun impactLight() {
        vibrate(VibrationEffect.createOneShot(20, 50))
    }
    
    /** Impact medium - for standard interactions */
    fun impactMedium() {
        vibrate(VibrationEffect.createOneShot(30, 100))
    }
    
    /** Impact heavy - for strong interactions */
    fun impactHeavy() {
        vibrate(VibrationEffect.createOneShot(50, 180))
    }
    
    /** Notification - for incoming messages/notifications */
    fun notification() {
        val pattern = longArrayOf(0, 30, 80, 30)
        val amplitudes = intArrayOf(0, 100, 0, 150)
        vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
    }
    
    // ══════════════════════════════════════════════════════════════════════
    // CUSTOM PATTERNS FOR SPECIFIC INTERACTIONS
    // ══════════════════════════════════════════════════════════════════════
    
    /** Voice recording start */
    fun recordingStart() {
        vibrate(VibrationEffect.createOneShot(40, 120))
    }
    
    /** Voice recording locked */
    fun recordingLocked() {
        val pattern = longArrayOf(0, 25, 25, 40)
        val amplitudes = intArrayOf(0, 80, 0, 140)
        vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1))
    }
    
    /** Voice recording cancelled */
    fun recordingCancelled() {
        vibrate(VibrationEffect.createOneShot(60, 100))
    }
    
    /** Pull-to-refresh triggered */
    fun refreshTriggered() {
        vibrate(VibrationEffect.createOneShot(35, 110))
    }
    
    /** Page/tab changed */
    fun pageChanged() {
        vibrate(VibrationEffect.createOneShot(25, 80))
    }
    
    // ══════════════════════════════════════════════════════════════════════
    // INTERNAL HELPERS
    // ══════════════════════════════════════════════════════════════════════
    
    private fun vibrate(effect: VibrationEffect) {
        if (!vibrator.hasVibrator()) return
        
        try {
            vibrator.vibrate(effect)
        } catch (e: Exception) {
            // Silently fail if vibration not supported
        }
    }
    
    /**  Cancels any ongoing vibration */
    fun cancel() {
        vibrator.cancel()
    }
}

/**
 * Composable function to get a remembered HapticFeedback instance.
 * Usage: val haptics = rememberHapticFeedback()
 */
@Composable
fun rememberHapticFeedback(): HapticFeedback {
    val context = LocalContext.current
    return remember(context) {
        HapticFeedback(context)
    }
}
