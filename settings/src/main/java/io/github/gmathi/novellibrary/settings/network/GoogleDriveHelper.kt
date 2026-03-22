package io.github.gmathi.novellibrary.settings.network

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.InputStreamContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.Locale

class GoogleDriveHelper(private val context: Context) {

    companion object {
        private const val APP_NAME = "Novel Library"
        private const val BACKUP_FILE_NAME = "NovelLibrary.backup.zip"
        private const val BACKUP_MIME_TYPE = "application/zip"
        private val REQUIRED_SCOPES = listOf(Scope(DriveScopes.DRIVE_APPDATA))
    }

    fun getSignInClient(): GoogleSignInClient {
        val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(REQUIRED_SCOPES[0])
            .build()
        return GoogleSignIn.getClient(context, signInOptions)
    }

    fun getSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    fun isSignedIn(): Boolean {
        val account = getSignedInAccount()
        return account != null && GoogleSignIn.hasPermissions(account, *REQUIRED_SCOPES.toTypedArray())
    }

    private fun getDriveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context, Collections.singleton(DriveScopes.DRIVE_APPDATA)
        )
        credential.selectedAccount = account.account
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName(APP_NAME).build()
    }

    suspend fun uploadBackup(localFile: File): Result<BackupInfo> = withContext(Dispatchers.IO) {
        try {
            val account = getSignedInAccount() ?: return@withContext Result.failure(IOException("Not signed in"))
            val driveService = getDriveService(account)

            val existingFiles = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$BACKUP_FILE_NAME'")
                .setFields("files(id, name)")
                .execute()

            existingFiles.files?.forEach { file ->
                driveService.files().delete(file.id).execute()
            }

            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = BACKUP_FILE_NAME
                parents = listOf("appDataFolder")
            }

            val mediaContent = InputStreamContent(BACKUP_MIME_TYPE, FileInputStream(localFile))
            mediaContent.length = localFile.length()

            val uploadedFile = driveService.files().create(fileMetadata, mediaContent)
                .setFields("id, name, size, modifiedTime")
                .execute()

            val info = BackupInfo(
                fileId = uploadedFile.id,
                fileName = uploadedFile.name,
                fileSize = uploadedFile.getSize()?.toLong() ?: localFile.length(),
                modifiedTime = Date()
            )
            Result.success(info)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadBackup(destinationFile: File): Result<File> = withContext(Dispatchers.IO) {
        try {
            val account = getSignedInAccount() ?: return@withContext Result.failure(IOException("Not signed in"))
            val driveService = getDriveService(account)

            val files = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$BACKUP_FILE_NAME'")
                .setFields("files(id, name, size, modifiedTime)")
                .setOrderBy("modifiedTime desc")
                .setPageSize(1)
                .execute()

            val driveFile = files.files?.firstOrNull()
                ?: return@withContext Result.failure(IOException("No backup found on Google Drive"))

            FileOutputStream(destinationFile).use { outputStream ->
                driveService.files().get(driveFile.id).executeMediaAndDownloadTo(outputStream)
            }

            Result.success(destinationFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBackupInfo(): Result<BackupInfo?> = withContext(Dispatchers.IO) {
        try {
            val account = getSignedInAccount() ?: return@withContext Result.failure(IOException("Not signed in"))
            val driveService = getDriveService(account)

            val files = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$BACKUP_FILE_NAME'")
                .setFields("files(id, name, size, modifiedTime)")
                .setOrderBy("modifiedTime desc")
                .setPageSize(1)
                .execute()

            val driveFile = files.files?.firstOrNull()
            if (driveFile == null) {
                Result.success(null)
            } else {
                val info = BackupInfo(
                    fileId = driveFile.id,
                    fileName = driveFile.name,
                    fileSize = driveFile.getSize()?.toLong() ?: 0L,
                    modifiedTime = driveFile.modifiedTime?.let { Date(it.value) } ?: Date()
                )
                Result.success(info)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteBackup(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val account = getSignedInAccount() ?: return@withContext Result.failure(IOException("Not signed in"))
            val driveService = getDriveService(account)

            val files = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$BACKUP_FILE_NAME'")
                .setFields("files(id)")
                .execute()

            files.files?.forEach { file ->
                driveService.files().delete(file.id).execute()
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class BackupInfo(
        val fileId: String,
        val fileName: String,
        val fileSize: Long,
        val modifiedTime: Date
    ) {
        fun getFormattedTime(): String {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            return sdf.format(modifiedTime)
        }

        fun getFormattedSize(): String {
            val kb = fileSize / 1024.0
            return if (kb > 1024) {
                String.format(Locale.getDefault(), "%.2f MB", kb / 1024.0)
            } else {
                String.format(Locale.getDefault(), "%.2f KB", kb)
            }
        }
    }
}
