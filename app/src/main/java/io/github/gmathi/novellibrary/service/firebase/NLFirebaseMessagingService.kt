package io.github.gmathi.novellibrary.service.firebase

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.Utils

/**
 * This is a useless service. We are not using this service for anything right now.
 */

class NLFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG: String = "NLFirebaseMessagingService"
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage?) {
        Logs.debug(TAG, "From: " + remoteMessage!!.from)

        // Check if message contains a data payload.
        if (remoteMessage.data.isNotEmpty()) {
            Logs.debug(TAG, "Message data payload: " + remoteMessage.data)
        }

        // Check if message contains a notification payload.
        if (remoteMessage.notification != null) {
            Logs.debug(TAG, "Message Notification Body: " + remoteMessage.notification!!.body!!)
        }
    }

    /**
     * Called if InstanceID token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the InstanceID token
     * is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String?) {
        Logs.debug(TAG, "Refreshed token: " + token!!)
    }
}