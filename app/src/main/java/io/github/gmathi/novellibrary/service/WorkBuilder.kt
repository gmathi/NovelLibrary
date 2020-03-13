package io.github.gmathi.novellibrary.service

import androidx.work.*
import io.github.gmathi.novellibrary.dataCenter
import io.github.gmathi.novellibrary.service.backup.BackupWorker
import io.github.gmathi.novellibrary.service.restore.RestoreWorker
import java.util.concurrent.TimeUnit

const val ONE_TIME_BACKUP_WORK_TAG = "backupOnce"
const val PERIODIC_BACKUP_WORK_TAG = "backupOnce"
const val ONE_TIME_RESTORE_WORK_TAG = "restoreOnce"

fun oneTimeBackupWorkRequest(shouldBackupSimpleText: Boolean = true,
                             shouldBackupDatabase: Boolean = true,
                             shouldBackupPreferences: Boolean = true,
                             shouldBackupFiles: Boolean = true): OneTimeWorkRequest {

    val data =
        workDataOf(
            BackupWorker.KEY_SHOULD_BACKUP_SIMPLE_TEX to shouldBackupSimpleText,
            BackupWorker.KEY_SHOULD_BACKUP_DATA_BASE to shouldBackupDatabase,
            BackupWorker.KEY_SHOULD_BACKUP_PREFERENCES to shouldBackupPreferences,
            BackupWorker.KEY_SHOULD_BACKUP_FILES to shouldBackupFiles
        )

    return OneTimeWorkRequestBuilder<BackupWorker>()
            .addTag(ONE_TIME_BACKUP_WORK_TAG)
            .setInputData(data)
            .build()
}

fun periodicBackupWorkRequest(): PeriodicWorkRequest {
    val data =
        workDataOf(
            BackupWorker.KEY_SHOULD_BACKUP_SIMPLE_TEX to true,
            BackupWorker.KEY_SHOULD_BACKUP_DATA_BASE to true,
            BackupWorker.KEY_SHOULD_BACKUP_PREFERENCES to true,
            BackupWorker.KEY_SHOULD_BACKUP_FILES to false
        )

    var delay = dataCenter.backupFrequency - TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - dataCenter.lastBackup)
    if (delay < 0) delay = 0

    return PeriodicWorkRequestBuilder<BackupWorker>(15, TimeUnit.MINUTES)//(dataCenter.backupFrequency.toLong(), TimeUnit.HOURS)
        .addTag(PERIODIC_BACKUP_WORK_TAG)
        .setInputData(data)
        //.setInitialDelay(delay, TimeUnit.HOURS)
        .setInitialDelay(5, TimeUnit.SECONDS)
        .build()
}

fun oneTimeRestoreWorkRequest(shouldSimpleTextRestore: Boolean = true,
                              shouldRestoreDatabase: Boolean = true,
                              shouldRestorePreferences: Boolean = true,
                              shouldRestoreFiles: Boolean = true): OneTimeWorkRequest {

    val data = workDataOf(RestoreWorker.KEY_SHOULD_RESTORE_SIMPLE_TEX to shouldSimpleTextRestore,
            RestoreWorker.KEY_SHOULD_RESTORE_DATA_BASE to shouldRestoreDatabase,
            RestoreWorker.KEY_SHOULD_RESTORE_PREFERENCES to shouldRestorePreferences,
            RestoreWorker.KEY_SHOULD_RESTORE_FILES to shouldRestoreFiles)

    return OneTimeWorkRequestBuilder<RestoreWorker>()
            .addTag(ONE_TIME_RESTORE_WORK_TAG)
            .setInputData(data)
            .build()
}