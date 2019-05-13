package io.github.gmathi.novellibrary.service.tts

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.getNovel
import io.github.gmathi.novellibrary.extensions.albumArt
import io.github.gmathi.novellibrary.extensions.displaySubtitle
import io.github.gmathi.novellibrary.extensions.displayTitle
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.getGlideUrl
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class TTSService : Service(), TextToSpeech.OnInitListener {

    companion object {
        const val TAG = "TTSService"

        const val AUDIO_TEXT_KEY = "audioTextKey"
        const val TITLE = "title"
        const val NOVEL_ID = "webPageId"

        const val ACTION_STOP = "actionStop"
        const val ACTION_PAUSE = "actionPause"
        const val ACTION_PLAY = "actionPlay"
        const val ACTION_NEXT = "actionNext"
        const val ACTION_PREVIOUS = "actionPrevious"
    }

    private lateinit var tts: TextToSpeech
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaController: MediaControllerCompat
    private lateinit var TTSNotificationBuilder: TTSNotificationBuilder
    private lateinit var notificationManager: NotificationManagerCompat
    private lateinit var dbHelper: DBHelper

    private var novel: Novel ? = null
    private var audioText: String? = null
    private var title: String? = null

    private var lines: ArrayList<String> = ArrayList()
    private var lineNumber: Int = 0
    private var isForegroundService: Boolean = false
    private val metadataCompat = MediaMetadataCompat.Builder()


//    private var isPlaying: Boolean = false


    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        //android.os.Debug.waitForDebugger()

        // Build a PendingIntent that can be used to launch the UI.
        val sessionActivityPendingIntent = PendingIntent.getActivity(this, 0, Intent(), 0)

        // Create a new MediaSession.
        mediaSession = MediaSessionCompat(this, "MusicService")
                .apply {
                    setSessionActivity(sessionActivityPendingIntent)
                    isActive = true
                }
        mediaController = MediaControllerCompat(this, mediaSession).also {
            it.registerCallback(MediaControllerCallback())
        }
        val pendingIntents = HashMap<String, PendingIntent>()
        pendingIntents[ACTION_PLAY] = createPendingIntent(ACTION_PLAY)
        pendingIntents[ACTION_PAUSE] = createPendingIntent(ACTION_PAUSE)
        pendingIntents[ACTION_STOP] = createPendingIntent(ACTION_STOP)
        pendingIntents[ACTION_NEXT] = createPendingIntent(ACTION_NEXT)
        pendingIntents[ACTION_PREVIOUS] = createPendingIntent(ACTION_PREVIOUS)

        TTSNotificationBuilder = TTSNotificationBuilder(this, pendingIntents)
        notificationManager = NotificationManagerCompat.from(this)

        dbHelper = DBHelper.getInstance(this)

        tts = TextToSpeech(this, this)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onDone(utteranceId: String?) {
                utteranceId?.let {
                    if (it == "KEEP_READING") {
                        lineNumber++
                        speakNexLine()
                    } else if (utteranceId == "STOP_READING") {

                    }
                }

            }

            override fun onError(utteranceId: String?) {
            }

            override fun onStart(utteranceId: String?) {
            }
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e(TAG, "OnStartCommand")
        val action = intent?.action
        if (action != null) {
            Log.e(TAG, "Action: $action")
            when (action) {
                ACTION_PLAY -> {
                    speakNexLine()
                    mediaSession.setPlaybackState(PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_PLAYING, 1L, 1F).build())
                }
                ACTION_PAUSE -> {
                    sendToTTS("", TextToSpeech.QUEUE_FLUSH, "STOP_READING")
                    mediaSession.setPlaybackState(PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_PAUSED, 1L, 1F).build())
                }
                ACTION_STOP -> {
                    sendToTTS("", TextToSpeech.QUEUE_FLUSH, "STOP_COMPLETELY")
                    mediaSession.setPlaybackState(PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_NONE, 1L, 1F).build())
                }
                ACTION_NEXT, ACTION_PREVIOUS -> {
                    Toast.makeText(this, "Not Supported Yet!", Toast.LENGTH_LONG).show()
//                    currentOrderId++
//                    val webPage = dbHelper.getWeb
//                    if ((currentOrderId+1) < novel.chaptersCount) {
//                        currentOrderId++
//                    } else {
//                        sendToTTS("No More Chapters. You are up-to-date", TextToSpeech.QUEUE_FLUSH, "STOP_COMPLETELY")
//                        mediaSession.setPlaybackState(PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_NONE, 1L, 1F).build())
//                    }
                }
            }
        } else {
            //Get values from intent
            audioText = intent?.extras?.getString(AUDIO_TEXT_KEY, null) ?: ""
            title = intent?.extras?.getString(TITLE, null) ?: ""
            val novelId = intent?.extras?.getLong(NOVEL_ID, -1L) ?: -1L
            novel = dbHelper.getNovel(novelId)

            metadataCompat.displayTitle = novel?.name ?: "Novel Name Not Found"
            metadataCompat.displaySubtitle = title

            if (!novel?.imageUrl.isNullOrBlank() && Utils.isConnectedToNetwork(this)) {
                Glide.with(this).asBitmap().load(novel!!.imageUrl!!.getGlideUrl()).into(object : CustomTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        metadataCompat.albumArt = resource
                        mediaSession.setMetadata(metadataCompat.build())
                        startReading()
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                    }
                })
            } else {
                mediaSession.setMetadata(metadataCompat.build())
                startReading()
            }

        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "English language is not available.", Toast.LENGTH_SHORT).show()
            } else {
                startReading()
            }
        } else {
            Toast.makeText(this, "Could not initialize TextToSpeech.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startReading() {
        //Stop reading
        sendToTTS("", TextToSpeech.QUEUE_FLUSH)

        //Reset old data
        lines.clear()
        lineNumber = 0

        //Set new data & start reading
        audioText?.split(".")?.mapTo(lines) { it }
        mediaSession.setPlaybackState(PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_PLAYING, 1L, 1F).build())
        speakNexLine()
    }

    private fun speakNexLine() {
        if (lines.isNotEmpty() && lineNumber < lines.size) {
            sendToTTS(lines[lineNumber], TextToSpeech.QUEUE_ADD, "KEEP_READING")
        } else {
            mediaSession.setPlaybackState(PlaybackStateCompat.Builder().setState(PlaybackStateCompat.STATE_NONE, 1L, 1F).build())
        }
    }

    @Suppress("DEPRECATION")
    private fun sendToTTS(text: String, queueMode: Int, utteranceId: String = "DO_NOTHING") {
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId) // Define if you need it
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, queueMode, params, utteranceId)
        } else {
            tts.speak(text, queueMode, null)
        }
    }

    private fun createPendingIntent(action: String): PendingIntent {
        val actionIntent = Intent(this, TTSService::class.java)
        actionIntent.action = action
        return PendingIntent.getService(this, 0, actionIntent, 0)
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            //mediaController.playbackState?.let { updateNotification(it) }
            Log.e(TAG, "MetaData Changed: $metadata")
        }

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            state?.let { updateNotification(it) }
            Log.e(TAG, "State Changed: $state")
        }

        private fun updateNotification(state: PlaybackStateCompat) {
            val updatedState = state.state
            if (mediaController.metadata == null) {
                return
            }

            // Skip building a notification when state is "none".
            val notification = if (updatedState != PlaybackStateCompat.STATE_NONE) {
                TTSNotificationBuilder.buildNotification(mediaSession.sessionToken)
            } else {
                null
            }

            when (updatedState) {
                PlaybackStateCompat.STATE_BUFFERING,
                PlaybackStateCompat.STATE_PLAYING -> {

                    if (!isForegroundService) {
                        //startService(Intent(applicationContext, this@TTSService.javaClass))
                        startForeground(NOW_PLAYING_NOTIFICATION, notification)
                        isForegroundService = true
                    } else if (notification != null) {
                        notificationManager.notify(NOW_PLAYING_NOTIFICATION, notification)
                    }
                }
                else -> {
                    if (isForegroundService) {
                        stopForeground(false)
                        isForegroundService = false
                        tts.stop()
                        // If playback has ended, also stop the service.
                        if (updatedState == PlaybackStateCompat.STATE_NONE) {
                            stopSelf()
                            tts.shutdown()
                        }

                        if (notification != null) {
                            notificationManager.notify(NOW_PLAYING_NOTIFICATION, notification)
                        } else {
                            stopForeground(true)
                        }
                    }
                }
            }
        }
    }


}
