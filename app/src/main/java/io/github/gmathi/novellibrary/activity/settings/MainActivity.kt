package io.github.gmathi.novellibrary.activity.settings

import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.drive.CreateFileActivityOptions
import com.google.android.gms.drive.Drive
import com.google.android.gms.drive.DriveClient
import com.google.android.gms.drive.DriveContents
import com.google.android.gms.drive.DriveResourceClient
import com.google.android.gms.drive.MetadataChangeSet
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.Task
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream

/**
 * Android Drive Quickstart activity. This activity takes a photo and saves it in Google Drive. The
 * user is prompted with a pre-made dialog which allows them to choose the file location.
 */
class MainActivity : Activity() {

    private var mGoogleSignInClient: GoogleSignInClient? = null
    private var mDriveClient: DriveClient? = null
    private var mDriveResourceClient: DriveResourceClient? = null
    private var mBitmapToSave: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        signIn()
    }

    /** Start sign in activity.  */
    private fun signIn() {
        Log.i(TAG, "Start sign in")
        mGoogleSignInClient = buildGoogleSignInClient()
        startActivityForResult(mGoogleSignInClient!!.signInIntent, REQUEST_CODE_SIGN_IN)
    }

    /** Build a Google SignIn client.  */
    private fun buildGoogleSignInClient(): GoogleSignInClient {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Drive.SCOPE_FILE)
            .build()
        return GoogleSignIn.getClient(this, signInOptions)
    }

    /** Create a new file and save it to Drive.  */
    private fun saveFileToDrive() {
        // Start by creating a new contents, and setting a callback.
        Log.i(TAG, "Creating new contents.")
        val image = mBitmapToSave

        mDriveResourceClient!!
            .createContents()
            .continueWithTask { task -> createFileIntentSender(task.result, image) }
            .addOnFailureListener { e -> Log.w(TAG, "Failed to create new contents.", e) }
    }

    /**
     * Creates an [IntentSender] to start a dialog activity with configured [ ] for user to create a new photo in Drive.
     */
    private fun createFileIntentSender(driveContents: DriveContents, image: Bitmap?): Task<Void> {
        Log.i(TAG, "New contents created.")
        // Get an output stream for the contents.
        val outputStream = driveContents.outputStream
        // Write the bitmap data from it.
        val bitmapStream = ByteArrayOutputStream()
        image!!.compress(Bitmap.CompressFormat.PNG, 100, bitmapStream)
        try {
            outputStream.write(bitmapStream.toByteArray())
        } catch (e: IOException) {
            Log.w(TAG, "Unable to write file contents.", e)
        }

        // Create the initial metadata - MIME type and title.
        // Note that the user will be able to change the title later.
        val metadataChangeSet = MetadataChangeSet.Builder()
            .setMimeType("image/jpeg")
            .setTitle("Android Photo.png")
            .build()
        // Set up options to configure and display the create file activity.
        val createFileActivityOptions = CreateFileActivityOptions.Builder()
            .setInitialMetadata(metadataChangeSet)
            .setInitialDriveContents(driveContents)
            .build()

        return mDriveClient!!
            .newCreateFileActivityIntentSender(createFileActivityOptions)
            .continueWith { task ->
                startIntentSenderForResult(task.result, REQUEST_CODE_CREATOR, null, 0, 0, 0)
                null
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_SIGN_IN -> {
                Log.i(TAG, "Sign in request code")
                // Called after user is signed in.
                if (resultCode == Activity.RESULT_OK) {
                    Log.i(TAG, "Signed in successfully.")
                    // Use the last signed in account here since it already have a Drive scope.
                    mDriveClient = Drive.getDriveClient(this, GoogleSignIn.getLastSignedInAccount(this)!!)
                    // Build a drive resource client.
                    mDriveResourceClient = Drive.getDriveResourceClient(this, GoogleSignIn.getLastSignedInAccount(this)!!)
                    // Start camera.
                    startActivityForResult(
                        Intent(MediaStore.ACTION_IMAGE_CAPTURE), REQUEST_CODE_CAPTURE_IMAGE)
                }
            }
            REQUEST_CODE_CAPTURE_IMAGE -> {
                Log.i(TAG, "capture image request code")
                // Called after a photo has been taken.
                if (resultCode == Activity.RESULT_OK) {
                    Log.i(TAG, "Image captured successfully.")
                    // Store the image data as a bitmap for writing later.
                    mBitmapToSave = data.extras!!.get("data") as Bitmap
                    saveFileToDrive()
                }
            }
            REQUEST_CODE_CREATOR -> {
                Log.i(TAG, "creator request code")
                // Called after a file is saved to Drive.
                if (resultCode == Activity.RESULT_OK) {
                    Log.i(TAG, "Image successfully saved.")
                    mBitmapToSave = null
                    // Just start the camera again for another photo.
                    startActivityForResult(
                        Intent(MediaStore.ACTION_IMAGE_CAPTURE), REQUEST_CODE_CAPTURE_IMAGE)
                }
            }
        }
    }

    companion object {

        private const val TAG = "drive-quickstart"
        private const val REQUEST_CODE_SIGN_IN = 0
        private const val REQUEST_CODE_CAPTURE_IMAGE = 1
        private const val REQUEST_CODE_CREATOR = 2
    }
}