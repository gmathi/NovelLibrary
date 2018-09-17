package io.github.gmathi.novellibrary.service

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.widget.Toast
import java.util.*
import android.app.PendingIntent
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.widget.RemoteViews
import io.github.gmathi.novellibrary.R


class TTSService : Service(), TextToSpeech.OnInitListener {

    companion object {
        const val AUDIO_TEXT_KEY = "audioTextKey"
        const val NOVEL_NAME_KEY = "novelNameKey"
        const val CHAPTER_NAME_KEY = "chapterNameKey"
        const val ACTION_STOP = "actionStop"
    }

    private lateinit var tts: TextToSpeech
    private var audioText: String? = null


    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        tts = TextToSpeech(this, this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action != null && action == ACTION_STOP) {
            sendToTTS("", 0)
            stopForegroundService()
            return super.onStartCommand(intent, flags, startId)
        }

        audioText = intent?.extras?.getString(AUDIO_TEXT_KEY, null) ?: ""
        val novelName = intent?.extras?.getString(NOVEL_NAME_KEY, null) ?: ""
        val chapterName = intent?.extras?.getString(CHAPTER_NAME_KEY, null) ?: ""

        sendToTTS("", 0)
        readCurrentAudioText()
        startForeground(1, getNotification(this, novelName, chapterName))
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts.setLanguage(Locale.getDefault())
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "English language is not available.", Toast.LENGTH_SHORT).show()
            } else {
                sendToTTS("", 0)
                readCurrentAudioText()
            }
        } else {
            Toast.makeText(this, "Could not initialize TextToSpeech.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun readCurrentAudioText() {
        audioText?.split(".")?.forEach { sendToTTS(it, 1) }
    }

    private fun sendToTTS(text: String, queueMode: Int) {
        if (queueMode == 1)
            tts.speak(text, TextToSpeech.QUEUE_ADD, null)
        else
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null)
    }


    private fun stopForegroundService() {
        tts.stop()
        tts.shutdown()
        // Stop foreground service and remove the notification.
        stopForeground(true)
         // Stop the foreground service.
        stopSelf()
    }

    private fun getNotification(context: Context, novelName: String, chapterName: String): Notification {

        // Add Pause button intent in notification.
        val stopIntent = Intent(this, TTSService::class.java)
        stopIntent.action = ACTION_STOP
        val pendingStopIntent = PendingIntent.getService(this, 0, stopIntent, 0)

        val view = RemoteViews(context.packageName, R.layout.notification_tts)
        view.setTextViewText(R.id.novelName, novelName)
        view.setTextViewText(R.id.chapterName, chapterName)
        view.setOnClickPendingIntent(R.id.close, pendingStopIntent)

        val pendingIntent = PendingIntent.getActivity(context, 0, Intent(), 0)
        val channelId = if(Build.VERSION.SDK_INT >=  Build.VERSION_CODES.O) getString(R.string.default_notification_channel_id) else ""

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, channelId)
                    .setSmallIcon(R.drawable.ic_queue_music_white_vector)
                    .setCustomContentView(view)
                    .setContentIntent(pendingIntent)
                    .build()
        } else {
            Notification.Builder(context)
                    .setSmallIcon(R.drawable.ic_queue_music_white_vector)
                    .setContent(view)
                    .setContentIntent(pendingIntent)
                    .build()
        }

    }


}
