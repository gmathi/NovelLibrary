package io.github.gmathi.novellibrary.service

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.content.Context
import android.os.Build
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.gmathi.novellibrary.util.Constants
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.Closeable
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.math.roundToInt

/**
 * A [NotificationManagerCompat] wrapper made to simplify handling of progress notifications.
 *
 * Key features:
 * - Creates notification channel on API over [Build.VERSION_CODES.O]
 * - Normalizes progress to a value between 0 and [normalizationLength], and discards updates
 *   that are to small to make a noticeable difference
 * - Limit progress updates to once every [updateRate] milliseconds
 * - Reduces boilerplate by providing easy way to update progress
 */
@Suppress("MemberVisibilityCanBePrivate", "unused")
class ProgressNotificationManager(context: Context,
                                  private val channelId: String = DEFAULT_CHANNEL_ID,
                                  private val channelName: String = DEFAULT_CHANNEL_NAME,
                                  @Importance private val importance: Int = DEFAULT_CHANNEL_IMPORTANCE,
                                  private val applyToChannel: NotificationChannel.() -> Unit = DEFAULT_APPLY_TO_CHANNEL) : Closeable {

    private val notificationManager: NotificationManagerCompat = NotificationManagerCompat.from(context)
    private val notificationQueue: NotificationQueue = NotificationQueue()

    val notificationId: Int = Constants.nextNotificationId
    val builder: Builder = Builder(context, channelId).DEFAULT_APPLY_TO_BUILDER() as Builder

    private val updateRate: Long = DEFAULT_UPDATE_RATE

    private var normalizationLength: Int
        get() = builder.max
        set(value) { builder.max = value }
    private var max: Int = 100
    private var progress: Int = 0

    private fun normalize(progress: Int): Int =
        (progress.toDouble() * normalizationLength / max).roundToInt()

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("WrongConstant")
    private fun createNotificationChannel() =
        notificationManager.createNotificationChannel(NotificationChannel(channelId, channelName, importance).apply(applyToChannel))

    fun newIndeterminateProgress() =
        notificationQueue.add(builder.buildIndeterminate(), true)

    fun newIndeterminateProgress(applyToBuilder: Builder.() -> NotificationCompat.Builder) {
        builder.applyToBuilder()
        newIndeterminateProgress()
    }

    fun newProgress(max: Int = 100, progress: Int = 0, normalizationLength: Int = 0) {
        this.max = max
        this.progress = normalize(progress)
        if (normalizationLength != 0)
            this.normalizationLength = normalizationLength
        notificationQueue.add(builder.buildProgress(this.progress), true)
    }

    fun newProgress(max: Int = 100, progress: Int = 0, normalizationLength: Int = 0, applyToBuilder: Builder.() -> NotificationCompat.Builder) {
        builder.applyToBuilder()
        newProgress(max, progress, normalizationLength)
    }

    fun updateProgress(progress: Int = 0, important: Boolean = false) {
        val normalized = normalize(progress)
        if (normalized == this.progress) return
        this.progress = normalized
        notificationQueue.add(builder.buildUpdatedProgress(this.progress), important/* || progress == max*/)
    }

    fun updateProgress(progress: Int = 0, important: Boolean = true, applyToBuilder: Builder.() -> NotificationCompat.Builder) {
        builder.applyToBuilder()
        updateProgress(progress, important)
    }

    fun closeProgress() =
        notificationQueue.add(builder.buildWithoutProgress(), true)

    fun closeProgress(applyToBuilder: Builder.() -> NotificationCompat.Builder) {
        builder.applyToBuilder()
        closeProgress()
    }

    private fun updateNotification(notification: Notification) {
        notificationManager.notify(notificationId, notification)
    }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            createNotificationChannel()
        GlobalScope.launch {
            while (!notificationQueue.isClosed()) {
                updateNotification(notificationQueue.take())
                delay(updateRate * NotificationQueue.globalChannelCount.get())
            }
        }
    }

    suspend fun waitForQueue() {
        while (notificationQueue.isNotEmpty)
            delay(1000)
        delay(200)
    }

    override fun close() {
        notificationQueue.close()
    }

    companion object {
        // region Helper Classes
        @IntDef(
            value = [
                NotificationManagerCompat.IMPORTANCE_UNSPECIFIED,
                NotificationManagerCompat.IMPORTANCE_NONE,
                NotificationManagerCompat.IMPORTANCE_MIN,
                NotificationManagerCompat.IMPORTANCE_LOW,
                NotificationManagerCompat.IMPORTANCE_DEFAULT,
                NotificationManagerCompat.IMPORTANCE_HIGH
            ]
        )
        @Retention(AnnotationRetention.SOURCE)
        annotation class Importance

        class Builder(context: Context, channelId: String) : NotificationCompat.Builder(context, channelId) {
            internal var max: Int = DEFAULT_NORMALIZATION_LENGTH

            @Deprecated("Change progress through [ProgressNotificationManager] instead",
                ReplaceWith(
                "updateProgress",
                "io.github.gmathi.novellibrary.service.ProgressNotificationManager"
                )
            )
            override fun setProgress(max: Int, progress: Int, indeterminate: Boolean): Builder =
                this.also {
                    android.util.Log.w(Builder::class.simpleName, "Change progress through [ProgressNotificationManager] instead!")
                }

            internal fun buildIndeterminate(): Notification =
                super.setProgress(0, 0, true)
                    .setOngoing(true)
                    .setAutoCancel(false)
                    .build()

            internal fun buildProgress(progress: Int): Notification =
                super.setProgress(max, progress, false)
                    .setOngoing(true)
                    .setAutoCancel(false)
                    .build()

            internal fun buildUpdatedProgress(progress: Int): Notification =
                super.setProgress(max, progress, false)
                    .build()

            internal fun buildWithoutProgress(): Notification =
                super.setProgress(0, 0, false)
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .build()
        }

        private class NotificationQueue(val sizeLimit: Int = DEFAULT_UPDATE_QUEUE_SIZE_LIMIT) : Closeable {
            private val updateChannel = Channel<Notification>(Channel.UNLIMITED)
            private var queueSize: Int = 0
            val isEmpty: Boolean
                get() = queueSize == 0
            val isNotEmpty: Boolean
                get() = queueSize != 0

            fun add(notification: Notification, ignoreSizeLimit: Boolean = false) {
                if (ignoreSizeLimit || globalQueueSize.get() < sizeLimit) {
                    globalQueueSize.getAndIncrement()
                    queueSize++
                    // Channel.UNLIMITED means send never suspends (blocks)
                    runBlocking { updateChannel.send(notification) }
                }
            }

            suspend fun take(): Notification {
                return updateChannel.receive().also {
                    globalQueueSize.getAndDecrement()
                    queueSize--
                }
            }

            override fun close() {
                globalQueueSize.getAndAdd(-queueSize)
                globalChannelCount.getAndDecrement()
                queueSize = 0
            }

            fun isClosed(): Boolean = updateChannel.isClosedForReceive

            init {
                globalChannelCount.getAndIncrement()
            }

            companion object {
                const val DEFAULT_UPDATE_QUEUE_SIZE_LIMIT = 25

                @JvmStatic
                private val globalQueueSize: AtomicInteger = AtomicInteger()

                @JvmStatic
                val globalChannelCount: AtomicLong = AtomicLong()
            }
        }
        // endregion

        // region Defaults
        private const val DEFAULT_CHANNEL_ID = "default_channel"
        private const val DEFAULT_CHANNEL_NAME = "Default"
        private const val DEFAULT_CHANNEL_DESCRIPTION = "Default notifications"
        private const val DEFAULT_CHANNEL_IMPORTANCE = NotificationManagerCompat.IMPORTANCE_LOW

        @TargetApi(Build.VERSION_CODES.O)
        @JvmStatic
        private val DEFAULT_APPLY_TO_CHANNEL: NotificationChannel.() -> Unit =  {
            description = DEFAULT_CHANNEL_DESCRIPTION
            setSound(null, null)
            enableVibration(false)
        }
        private val DEFAULT_APPLY_TO_BUILDER: Builder.() -> NotificationCompat.Builder = {
            setOnlyAlertOnce(true)
        }

        private const val DEFAULT_UPDATE_RATE = 50L
        private const val DEFAULT_NORMALIZATION_LENGTH = 100
        // endregion
    }

}