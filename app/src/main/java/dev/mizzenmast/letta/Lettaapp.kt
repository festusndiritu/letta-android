package dev.mizzenmast.letta

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.emoji2.bundled.BundledEmojiCompatConfig
import androidx.emoji2.text.EmojiCompat
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class LettaApp : Application(), ImageLoaderFactory {

    @Inject
    lateinit var okHttpClient: OkHttpClient

    override fun onCreate() {
        super.onCreate()
        EmojiCompat.init(BundledEmojiCompatConfig(this))

        val manager = getSystemService(NotificationManager::class.java)

        // ── Messages channel ──────────────────────────────────────────────────
        manager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_MESSAGES,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH, // Changed to HIGH for heads-up notifications
            ).apply {
                description = "Message notifications"
                enableLights(true)
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            },
        )

        // ── Direct Messages channel ───────────────────────────────────────────
        manager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_DIRECT_MESSAGES,
                "Direct Messages",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Notifications for direct messages"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            },
        )

        // ── Group Messages channel ────────────────────────────────────────────
        manager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_GROUP_MESSAGES,
                "Group Messages",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Notifications for group messages"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            },
        )

        // ── Mentions channel ──────────────────────────────────────────────────
        manager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_MENTIONS,
                "Mentions",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Notifications when you are mentioned"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
                vibrationPattern = longArrayOf(0, 250, 250, 250) // Custom vibration for mentions
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            },
        )

        // ── Calls channel ────────────────────────────────────────────────────
        // HIGH importance required for incoming call heads-up / full-screen intent
        manager.createNotificationChannel(
            NotificationChannel(
                NOTIFICATION_CHANNEL_CALLS,
                "Calls",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Incoming and ongoing call notifications"
                setBypassDnd(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            },
        )
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25) // Use 25% of available memory
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(100L * 1024 * 1024) // 100 MB disk cache
                    .build()
            }
            .respectCacheHeaders(false)
            .apply {
                if (BuildConfig.DEBUG) {
                    logger(DebugLogger())
                }
            }
            .build()
    }

    companion object {
        const val NOTIFICATION_CHANNEL_MESSAGES = "messages"
        const val NOTIFICATION_CHANNEL_DIRECT_MESSAGES = "direct_messages"
        const val NOTIFICATION_CHANNEL_GROUP_MESSAGES = "group_messages"
        const val NOTIFICATION_CHANNEL_MENTIONS = "mentions"
        const val NOTIFICATION_CHANNEL_CALLS = "calls"
    }
}
