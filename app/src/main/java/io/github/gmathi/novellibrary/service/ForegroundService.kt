package io.github.gmathi.novellibrary.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.util.Log
import android.widget.RemoteViews
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.NavDrawerActivity
import io.github.gmathi.novellibrary.util.Constants

class ForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (intent.action == Constants.ACTION.STARTFOREGROUND_ACTION) {
            Log.i(LOG_TAG, "Received Start Foreground Intent ")
            val notificationIntent = Intent(this, NavDrawerActivity::class.java)
            notificationIntent.action = Constants.ACTION.MAIN_ACTION
            notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            val pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0)

            val previousIntent = Intent(this, ForegroundService::class.java)
            previousIntent.action = Constants.ACTION.PREV_ACTION
            val ppreviousIntent = PendingIntent.getService(this, 0,
                previousIntent, 0)

            val playIntent = Intent(this, ForegroundService::class.java)
            playIntent.action = Constants.ACTION.PLAY_ACTION
            val pplayIntent = PendingIntent.getService(this, 0,
                playIntent, 0)

            val nextIntent = Intent(this, ForegroundService::class.java)
            nextIntent.action = Constants.ACTION.NEXT_ACTION
            val pnextIntent = PendingIntent.getService(this, 0,
                nextIntent, 0)

            val closeIntent = Intent(this, ForegroundService::class.java)
            closeIntent.action = Constants.ACTION.STOPFOREGROUND_ACTION
            val pcloseIntent = PendingIntent.getService(this, 0,
                closeIntent, 0)

            val icon = BitmapFactory.decodeResource(resources,
                R.drawable.ic_cloud_download_white_vector)

            val notification = NotificationCompat.Builder(this)
                .setContentTitle("Truiton Music Player")
                .setTicker("Truiton Music Player")
                .setContentText("My Music")
                .setSmallIcon(R.drawable.ic_book_white_vector)
                //  .setLargeIcon(icon)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_media_previous,
                    "Previous", ppreviousIntent)
                .addAction(android.R.drawable.ic_media_play, "Play",
                    pplayIntent)
                .addAction(R.drawable.ic_close_white_vector, "close",
                    pcloseIntent)
                .addAction(android.R.drawable.ic_media_next, "Next",
                    pnextIntent).build()

            val remoteViews = RemoteViews(packageName,
                R.layout.download_notification)
            val mBuilder = Notification.Builder(this).setSmallIcon(R.drawable.ic_book_white_vector).setContent(remoteViews)


            startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE,
                mBuilder.build())
        } else if (intent.action == Constants.ACTION.PREV_ACTION) {
            Log.i(LOG_TAG, "Clicked Previous")
        } else if (intent.action == Constants.ACTION.PLAY_ACTION) {
            Log.i(LOG_TAG, "Clicked Play")
        } else if (intent.action == Constants.ACTION.NEXT_ACTION) {
            Log.i(LOG_TAG, "Clicked Next")
        } else if (intent.action == Constants.ACTION.STOPFOREGROUND_ACTION) {
            Log.i(LOG_TAG, "Received Stop Foreground Intent")
            stopForeground(true)
            stopSelf()
        }
        return Service.START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(LOG_TAG, "In onDestroy")
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        private val LOG_TAG = "ForegroundService"
    }
}