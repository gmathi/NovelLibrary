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

package io.github.gmathi.novellibrary.service.tts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.annotation.RequiresApi
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.extensions.isPlaying


/**
 * Helper class to encapsulate code for building notifications.
 */
class TTSNotificationBuilder(private val context: Context, private val pendingIntents: HashMap<String, PendingIntent>) {

    companion object {
        const val TTS_CHANNEL_ID: String = "io.github.gmathi.novellibrary.tts"

        @JvmStatic
        val TTS_NOTIFICATION_ID: Int = io.github.gmathi.novellibrary.util.Utils.getUniqueNotificationId()
    }

    private val notificationManager = NotificationManagerCompat.from(context)

    private val skipToPreviousAction = NotificationCompat.Action(
        R.drawable.ic_skip_previous_white,
        context.getString(R.string.previous_chapter),
        pendingIntents[TTSService.ACTION_PREVIOUS]
    )
    private val playAction = NotificationCompat.Action(
        R.drawable.ic_play_white,
        context.getString(R.string.play),
        pendingIntents[TTSService.ACTION_PLAY]
    )
    private val pauseAction = NotificationCompat.Action(
        R.drawable.ic_pause_white,
        context.getString(R.string.pause),
        pendingIntents[TTSService.ACTION_PAUSE]
    )
    private val skipToNextAction = NotificationCompat.Action(
        R.drawable.ic_skip_next_white,
        context.getString(R.string.next_chapter),
        pendingIntents[TTSService.ACTION_NEXT]
    )
    private val openControlsAction = NotificationCompat.Action(
        R.drawable.ic_open_in_browser_white_vector,
        context.getString(R.string.text_to_speech_controls),
        pendingIntents[TTSService.ACTION_OPEN_CONTROLS]
    )
    private val stopPendingIntent =
        pendingIntents[TTSService.ACTION_STOP]

    fun buildNotification(sessionToken: MediaSessionCompat.Token): Notification {
        if (shouldCreateNowPlayingChannel()) {
            createNowPlayingChannel(context)
        }

        val controller = MediaControllerCompat(context, sessionToken)
        val description = controller.metadata?.description
        val playbackState = controller.playbackState

        val builder = NotificationCompat.Builder(context, TTS_CHANNEL_ID)
        builder.addAction(skipToPreviousAction)
        if (playbackState.isPlaying) {
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

    private fun shouldCreateNowPlayingChannel() =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !nowPlayingChannelExists()

    @RequiresApi(Build.VERSION_CODES.O)
    private fun nowPlayingChannelExists() =
        notificationManager.getNotificationChannel(TTS_CHANNEL_ID) != null

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNowPlayingChannel(context: Context) {
        notificationManager.createNotificationChannel(
            NotificationChannel(
                TTS_CHANNEL_ID,
                context.getString(R.string.tts_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.tts_notification_channel_name)
                setSound(null, null)
                enableVibration(false)
            })
    }
}


