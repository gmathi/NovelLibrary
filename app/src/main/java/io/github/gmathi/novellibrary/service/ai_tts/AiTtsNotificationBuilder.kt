/*
 * Copyright 2018 Google Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.gmathi.novellibrary.service.ai_tts

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.extensions.isPlaying
import io.github.gmathi.novellibrary.util.notification.Notifications


/**
 * Helper class to encapsulate code for building AI TTS playback notifications.
 */
class AiTtsNotificationBuilder(
    private val context: Context,
    private val pendingIntents: HashMap<String, PendingIntent>
) {

    companion object {
        // Action constants mirroring AiTtsService (defined here for use before AiTtsService exists)
        const val ACTION_PLAY = "ai_tts_play"
        const val ACTION_PAUSE = "ai_tts_pause"
        const val ACTION_STOP = "ai_tts_stop"
        const val ACTION_NEXT = "ai_tts_next"
        const val ACTION_PREVIOUS = "ai_tts_previous"
        const val ACTION_OPEN_CONTROLS = "ai_tts_open_controls"
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    private val skipToPreviousAction = NotificationCompat.Action(
        R.drawable.ic_skip_previous_white,
        context.getString(R.string.previous_chapter),
        pendingIntents[ACTION_PREVIOUS]
    )
    private val playAction = NotificationCompat.Action(
        R.drawable.ic_play_white,
        context.getString(R.string.play),
        pendingIntents[ACTION_PLAY]
    )
    private val pauseAction = NotificationCompat.Action(
        R.drawable.ic_pause_white,
        context.getString(R.string.pause),
        pendingIntents[ACTION_PAUSE]
    )
    private val skipToNextAction = NotificationCompat.Action(
        R.drawable.ic_skip_next_white,
        context.getString(R.string.next_chapter),
        pendingIntents[ACTION_NEXT]
    )
    private val openControlsAction = NotificationCompat.Action(
        R.drawable.ic_open_in_browser_white_vector,
        context.getString(R.string.text_to_speech_controls),
        pendingIntents[ACTION_OPEN_CONTROLS]
    )
    private val stopPendingIntent =
        pendingIntents[ACTION_STOP]

    fun buildNotification(sessionToken: MediaSessionCompat.Token): Notification {
        if (shouldCreateNowPlayingChannel()) {
            createNowPlayingChannel(context)
        }

        val controller = MediaControllerCompat(context, sessionToken)
        val description = controller.metadata?.description
        val playbackState = controller.playbackState

        val builder = NotificationCompat.Builder(context, Notifications.CHANNEL_AI_TTS_PLAYBACK)
        builder.addAction(skipToPreviousAction)
        if (playbackState?.isPlaying == true) {
            builder.addAction(pauseAction)
        } else {
            builder.addAction(playAction)
        }
        builder.addAction(skipToNextAction)
        builder.addAction(openControlsAction)

        val drawable = AppCompatResources.getDrawable(context, R.mipmap.ic_launcher)
        if (drawable is BitmapDrawable) {
            builder.setLargeIcon(drawable.bitmap)
        }

        val mediaStyle = androidx.media.app.NotificationCompat.DecoratedMediaCustomViewStyle()
            .setCancelButtonIntent(stopPendingIntent)
            .setMediaSession(sessionToken)
            .setShowActionsInCompactView(1)
            .setShowCancelButton(true)

        builder.setContentIntent(controller.sessionActivity)
            .setStyle(mediaStyle)
            .setSmallIcon(R.drawable.ic_queue_music_white_vector)

        description?.let {
            builder.setLargeIcon(it.iconBitmap)
                .setContentText(it.subtitle)
                .setContentTitle(it.title)
        }

        return builder.setDeleteIntent(stopPendingIntent)
            .setOnlyAlertOnce(true)
            .setColorized(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    fun notify(notification: Notification) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify(Notifications.ID_AI_TTS_PLAYBACK, notification)
        }
    }

    private fun shouldCreateNowPlayingChannel() =
        !nowPlayingChannelExists()

    private fun nowPlayingChannelExists() =
        notificationManager.getNotificationChannel(Notifications.CHANNEL_AI_TTS_PLAYBACK) != null

    private fun createNowPlayingChannel(context: Context) {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                Notifications.CHANNEL_AI_TTS_PLAYBACK,
                "AI TTS Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "AI TTS Playback"
                setSound(null, null)
                enableVibration(false)
            }
        )
    }
}
