package io.github.gmathi.novellibrary.activity.settings

import androidx.work.WorkInfo.State
import androidx.work.WorkManager
import androidx.work.WorkRequest
import io.github.gmathi.novellibrary.settings.api.SettingsActivity
import io.github.gmathi.novellibrary.worker.oneTimeGoogleDriveBackupWorkRequest
import io.github.gmathi.novellibrary.worker.oneTimeGoogleDriveRestoreWorkRequest

/**
 * App-module subclass of SettingsActivity.
 *
 * Only overrides backup/restore execution to enqueue WorkManager workers,
 * which depend on app-module classes (DBHelper, DataCenter, notifications, etc.).
 * All other Google Drive logic (sign-in, sign-out, backup info) is handled
 * by the base SettingsActivity in the settings module.
 */
class AppSettingsActivity : SettingsActivity() {

    override fun onExecuteGoogleDriveBackup(
        simpleText: Boolean, database: Boolean, preferences: Boolean, files: Boolean
    ) {
        val workRequest = oneTimeGoogleDriveBackupWorkRequest(
            shouldBackupSimpleText = simpleText,
            shouldBackupDatabase = database,
            shouldBackupPreferences = preferences,
            shouldBackupFiles = files
        )
        observeWork(workRequest)
    }

    override fun onExecuteGoogleDriveRestore(
        simpleText: Boolean, database: Boolean, preferences: Boolean, files: Boolean
    ) {
        val workRequest = oneTimeGoogleDriveRestoreWorkRequest(
            shouldRestoreSimpleText = simpleText,
            shouldRestoreDatabase = database,
            shouldRestorePreferences = preferences,
            shouldRestoreFiles = files
        )
        observeWork(workRequest)
    }

    private fun observeWork(workRequest: WorkRequest) {
        val workManager = WorkManager.getInstance(applicationContext)
        workManager.enqueue(workRequest)
        val observable = workManager.getWorkInfoByIdLiveData(workRequest.id)
        observable.observe(this) { info ->
            if (info != null && info.state in arrayOf(State.SUCCEEDED, State.FAILED, State.CANCELLED)) {
                observable.removeObservers(this)
            }
        }
    }
}
