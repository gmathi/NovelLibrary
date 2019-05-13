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
import android.support.annotation.RequiresApi
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import android.support.v4.media.session.MediaButtonReceiver
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v7.content.res.AppCompatResources
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.extensions.isPlaying

const val NOW_PLAYING_CHANNEL: String = "io.github.gmathi.novellibrary.service.tts.NOW_PLAYING"
const val NOW_PLAYING_NOTIFICATION: Int = 1

/**
 * Helper class to encapsulate code for building notifications.
 */
class TTSNotificationBuilder(private val context: Context, private val pendingIntents: HashMap<String, PendingIntent>) {
    private val platformNotificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private val skipToPreviousAction = NotificationCompat.Action(
            R.drawable.ic_skip_previous_white,
            context.getString(R.string.previous_chapter),
            pendingIntents[TTSService.ACTION_PREVIOUS])
    private val playAction = NotificationCompat.Action(
            R.drawable.ic_play_white,
            context.getString(R.string.play),
            pendingIntents[TTSService.ACTION_PLAY])
    private val pauseAction = NotificationCompat.Action(
            R.drawable.ic_pause_white,
            context.getString(R.string.pause),
            pendingIntents[TTSService.ACTION_PAUSE])
    private val skipToNextAction = NotificationCompat.Action(
            R.drawable.ic_skip_next_white,
            context.getString(R.string.next_chapter),
            pendingIntents[TTSService.ACTION_NEXT])
    private val stopPendingIntent =
            pendingIntents[TTSService.ACTION_STOP]

    fun buildNotification(sessionToken: MediaSessionCompat.Token): Notification {
        if (shouldCreateNowPlayingChannel()) {
            createNowPlayingChannel()
        }

        val controller = MediaControllerCompat(context, sessionToken)
        val description = controller.metadata.description
        val playbackState = controller.playbackState

        val builder = NotificationCompat.Builder(context, NOW_PLAYING_CHANNEL)

        // Only add actions for skip back, play/pause, skip forward, based on what's enabled.
        // Only add actions for skip back, play/pause, skip forward, based on what's enabled.
//        var playPauseIndex = 0
//        if (playbackState.isSkipToPreviousEnabled) {
        builder.addAction(skipToPreviousAction)
//            ++playPauseIndex
//        }
        if (playbackState.isPlaying) {
            builder.addAction(pauseAction)
        } else {
            builder.addAction(playAction)
        }
//        if (playbackState.isSkipToNextEnabled) {
        builder.addAction(skipToNextAction)
//        }

        val drawable = AppCompatResources.getDrawable(context, R.mipmap.ic_launcher)
        if (drawable is BitmapDrawable) {
            builder.setLargeIcon(drawable.bitmap)
        }


        val mediaStyle = android.support.v4.media.app.NotificationCompat.MediaStyle()
                .setCancelButtonIntent(stopPendingIntent)
                .setMediaSession(sessionToken)
                .setShowActionsInCompactView(1)
                .setShowCancelButton(true)

        return builder.setContentIntent(controller.sessionActivity)
                .setContentText(description.subtitle)
                .setContentTitle(description.title)
                .setDeleteIntent(stopPendingIntent)
                .setLargeIcon(description.iconBitmap)
                .setOnlyAlertOnce(true)
                .setSmallIcon(R.drawable.ic_queue_music_white_vector)
                .setStyle(mediaStyle)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()


    }

    private fun shouldCreateNowPlayingChannel() =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !nowPlayingChannelExists()

    @RequiresApi(Build.VERSION_CODES.O)
    private fun nowPlayingChannelExists() =
            platformNotificationManager.getNotificationChannel(NOW_PLAYING_CHANNEL) != null

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNowPlayingChannel() {
        val notificationChannel = NotificationChannel(NOW_PLAYING_CHANNEL,
                "Now Playing",
                NotificationManager.IMPORTANCE_LOW)
                .apply {
                    description = "Show the TTS for the current novel/chapter"
                }

        platformNotificationManager.createNotificationChannel(notificationChannel)
    }
}


