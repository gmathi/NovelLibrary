package io.github.gmathi.novellibrary.service.firebase

import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.iid.FirebaseInstanceIdService
import io.github.gmathi.novellibrary.util.Utils


class NLFirebaseInstanceIdService : FirebaseInstanceIdService() {

    companion object {
        private const val TAG: String = "NLFInstanceIdService"
    }

    override fun onTokenRefresh() {
        // Get updated InstanceID token.
        val refreshedToken = FirebaseInstanceId.getInstance().token
        Utils.debug(TAG, "Refreshed token: " + refreshedToken!!)

        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, send the
        // Instance ID token to your app server.
        //sendRegistrationToServer(refreshedToken)
    }
}