package io.github.gmathi.novellibrary.service.firebase

import android.annotation.SuppressLint
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.Utils

/**
 * This is a useless service. We are not using this service for anything right now.
 */

@SuppressLint("MissingFirebaseInstanceTokenRefresh")
class NLFirebaseMessagingService : FirebaseMessagingService()