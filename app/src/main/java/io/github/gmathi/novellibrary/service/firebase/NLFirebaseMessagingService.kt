package io.github.gmathi.novellibrary.service.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.NavDrawerActivity
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Logs
import kotlinx.coroutines.*

/**
 * Firebase messaging service using coroutines for background processing
 */
@AndroidEntryPoint
class NLFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        private const val TAG = "NLFirebaseMessagingService"
        private const val NOTIFICATION_CHANNEL_ID = "novel_library_notifications"
        private const val NOTIFICATION_CHANNEL_NAME = "Novel Library Notifications"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    /**
     * Called when a new token is generated
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Logs.debug(TAG, "New FCM token: $token")
        
        // Handle token refresh using coroutines
        serviceScope.launch {
            try {
                handleTokenRefresh(token)
            } catch (e: Exception) {
                Logs.error(TAG, "Failed to handle token refresh", e)
            }
        }
    }

    /**
     * Called when a message is received
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        Logs.debug(TAG, "Message received from: ${remoteMessage.from}")
        
        // Handle message processing using coroutines
        serviceScope.launch {
            try {
                processRemoteMessage(remoteMessage)
            } catch (e: Exception) {
                Logs.error(TAG, "Failed to process remote message", e)
            }
        }
    }

    /**
     * Handle token refresh with coroutines
     */
    private suspend fun handleTokenRefresh(token: String) = withContext(Dispatchers.IO) {
        // Store token in preferences or send to server
        // This is where you would typically update the token on your backend
        Logs.info(TAG, "Token refresh handled: $token")
        
        // Example: Send token to server
        // sendTokenToServer(token)
    }

    /**
     * Process remote message with coroutines
     */
    private suspend fun processRemoteMessage(remoteMessage: RemoteMessage) = withContext(Dispatchers.IO) {
        // Process data payload
        if (remoteMessage.data.isNotEmpty()) {
            Logs.debug(TAG, "Message data payload: ${remoteMessage.data}")
            handleDataMessage(remoteMessage.data)
        }

        // Process notification payload
        remoteMessage.notification?.let { notification ->
            Logs.debug(TAG, "Message notification: ${notification.title}")
            handleNotificationMessage(notification.title, notification.body)
        }
    }

    /**
     * Handle data message with coroutines
     */
    private suspend fun handleDataMessage(data: Map<String, String>) = withContext(Dispatchers.Main) {
        // Handle different types of data messages
        when (data["type"]) {
            "novel_update" -> {
                val novelId = data["novel_id"]?.toLongOrNull()
                val novelName = data["novel_name"]
                if (novelId != null && novelName != null) {
                    showNovelUpdateNotification(novelId, novelName)
                }
            }
            "app_update" -> {
                val version = data["version"]
                if (version != null) {
                    showAppUpdateNotification(version)
                }
            }
            else -> {
                Logs.debug(TAG, "Unknown data message type: ${data["type"]}")
            }
        }
    }

    /**
     * Handle notification message with coroutines
     */
    private suspend fun handleNotificationMessage(title: String?, body: String?) = withContext(Dispatchers.Main) {
        if (title != null && body != null) {
            showGeneralNotification(title, body)
        }
    }

    /**
     * Show novel update notification
     */
    private fun showNovelUpdateNotification(novelId: Long, novelName: String) {
        val intent = Intent(this, NavDrawerActivity::class.java).apply {
            action = Constants.Action.MAIN_ACTION
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("novel_id", novelId)
        }

        val pendingIntent = createPendingIntent(intent, novelId.toInt())

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_book_white_vector)
            .setContentTitle("Novel Update")
            .setContentText("New chapters available for $novelName")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(novelId.toInt(), notification)
    }

    /**
     * Show app update notification
     */
    private fun showAppUpdateNotification(version: String) {
        val intent = Intent(this, NavDrawerActivity::class.java).apply {
            action = Constants.Action.MAIN_ACTION
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = createPendingIntent(intent, 0)

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_book_white_vector)
            .setContentTitle("App Update Available")
            .setContentText("Version $version is now available")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(1000, notification)
    }

    /**
     * Show general notification
     */
    private fun showGeneralNotification(title: String, body: String) {
        val intent = Intent(this, NavDrawerActivity::class.java).apply {
            action = Constants.Action.MAIN_ACTION
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = createPendingIntent(intent, 0)

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_book_white_vector)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(this).notify(2000, notification)
    }

    /**
     * Create pending intent with proper flags
     */
    private fun createPendingIntent(intent: Intent, requestCode: Int): PendingIntent {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        return PendingIntent.getActivity(this, requestCode, intent, flags)
    }

    /**
     * Create notification channel for Android O and above
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for Novel Library app updates and novel updates"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}