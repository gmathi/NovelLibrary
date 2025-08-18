package io.github.gmathi.novellibrary.worker

import android.content.Context
import android.net.Uri
import androidx.work.*
import io.github.gmathi.novellibrary.model.preference.DataCenter
import dagger.hilt.android.EntryPointAccessors
import io.github.gmathi.novellibrary.di.WorkerEntryPoint
import java.util.concurrent.TimeUnit

const val ONE_TIME_BACKUP_WORK_TAG = "backupOnce"
const val PERIODIC_BACKUP_WORK_TAG = "backupOnce"
const val ONE_TIME_RESTORE_WORK_TAG = "restoreOnce"

fun oneTimeBackupWorkRequest(
    context: android.content.Context,
    uri: Uri,
    shouldBackupSimpleText: Boolean = true,
    shouldBackupDatabase: Boolean = true,
    shouldBackupPreferences: Boolean = true,
    shouldBackupFiles: Boolean = true
): OneTimeWorkRequest {

    val data =
        workDataOf(
            BackupWorker.KEY_URI to uri.toString(),
            BackupWorker.KEY_SHOULD_BACKUP_SIMPLE_TEX to shouldBackupSimpleText,
            BackupWorker.KEY_SHOULD_BACKUP_DATA_BASE to shouldBackupDatabase,
            BackupWorker.KEY_SHOULD_BACKUP_PREFERENCES to shouldBackupPreferences,
            BackupWorker.KEY_SHOULD_BACKUP_FILES to shouldBackupFiles
        )

    val entryPoint = EntryPointAccessors.fromApplication(context, WorkerEntryPoint::class.java)
    val dataCenter: DataCenter = entryPoint.dataCenter()
    dataCenter.backupData = data.toByteArray()

    return OneTimeWorkRequestBuilder<BackupWorker>()
        .addTag(ONE_TIME_BACKUP_WORK_TAG)
        .setInputData(data)
        .build()
}

fun periodicBackupWorkRequest(context: Context, backupFrequency: Int): PeriodicWorkRequest? {
    val entryPoint = EntryPointAccessors.fromApplication(context, WorkerEntryPoint::class.java)
    val dataCenter: DataCenter = entryPoint.dataCenter()
    val array = dataCenter.backupData ?: return null
    val data = Data.fromByteArray(array)

    var delay = backupFrequency - TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - dataCenter.lastBackup)
    if (delay < 0) delay = 0

    return PeriodicWorkRequestBuilder<BackupWorker>(backupFrequency.toLong(), TimeUnit.HOURS)
        .addTag(PERIODIC_BACKUP_WORK_TAG)
        .setInputData(data)
        .setInitialDelay(delay, TimeUnit.HOURS)
        .build()
}

fun oneTimeRestoreWorkRequest(
    context: Context,
    uri: Uri,
    shouldSimpleTextRestore: Boolean = true,
    shouldRestoreDatabase: Boolean = true,
    shouldRestorePreferences: Boolean = true,
    shouldRestoreFiles: Boolean = true
): OneTimeWorkRequest {

    val data =
        workDataOf(
            RestoreWorker.KEY_URI to uri.toString(),
            RestoreWorker.KEY_SHOULD_RESTORE_SIMPLE_TEX to shouldSimpleTextRestore,
            RestoreWorker.KEY_SHOULD_RESTORE_DATA_BASE to shouldRestoreDatabase,
            RestoreWorker.KEY_SHOULD_RESTORE_PREFERENCES to shouldRestorePreferences,
            RestoreWorker.KEY_SHOULD_RESTORE_FILES to shouldRestoreFiles
        )

    return OneTimeWorkRequestBuilder<RestoreWorker>()
        .addTag(ONE_TIME_RESTORE_WORK_TAG)
        .setInputData(data)
        .build()
}