package io.github.gmathi.novellibrary.activity.settings

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.util.Constants.WORK_KEY_RESULT
import io.github.gmathi.novellibrary.worker.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uy.kohesive.injekt.injectLazy
import java.io.File

data class BackupRestoreUiState(
    val isOperationRunning: Boolean = false,
    val operationType: OperationType = OperationType.NONE,
    val progress: Float = 0f,
    val currentStep: String = "",
    val resultMessage: String? = null,
    val isSuccess: Boolean? = null,
    val isDeletingFiles: Boolean = false,

    // Checkbox states for backup/restore options
    val simpleText: Boolean = true,
    val database: Boolean = true,
    val preferences: Boolean = true,
    val files: Boolean = true,

    // Dialogs
    val showBackupOptionsDialog: Boolean = false,
    val showRestoreOptionsDialog: Boolean = false,
    val showFrequencyDialog: Boolean = false,
    val showClearDataDialog: Boolean = false,
    val showResultDialog: Boolean = false
)

enum class OperationType { NONE, BACKUP, RESTORE, CLEAR_DATA }

class BackupRestoreViewModel(application: Application) : AndroidViewModel(application) {

    private val dataCenter: DataCenter by injectLazy()
    private val dbHelper: DBHelper by injectLazy()

    private val _uiState = MutableStateFlow(BackupRestoreUiState())
    val uiState: StateFlow<BackupRestoreUiState> = _uiState.asStateFlow()

    val backupFrequency: Int get() = dataCenter.backupFrequency
    val showBackupHint: Boolean get() = dataCenter.showBackupHint
    val showRestoreHint: Boolean get() = dataCenter.showRestoreHint

    fun dismissBackupHint() { dataCenter.showBackupHint = false }
    fun dismissRestoreHint() { dataCenter.showRestoreHint = false }

    fun showBackupOptions() = _uiState.update { it.copy(showBackupOptionsDialog = true) }
    fun showRestoreOptions() = _uiState.update { it.copy(showRestoreOptionsDialog = true) }
    fun showFrequencyDialog() = _uiState.update { it.copy(showFrequencyDialog = true) }
    fun showClearDataDialog() = _uiState.update { it.copy(showClearDataDialog = true) }

    fun dismissDialog() = _uiState.update {
        it.copy(
            showBackupOptionsDialog = false,
            showRestoreOptionsDialog = false,
            showFrequencyDialog = false,
            showClearDataDialog = false,
            showResultDialog = false
        )
    }

    fun dismissResult() = _uiState.update {
        it.copy(showResultDialog = false, resultMessage = null, isSuccess = null)
    }

    fun updateOption(simpleText: Boolean? = null, database: Boolean? = null, preferences: Boolean? = null, files: Boolean? = null) {
        _uiState.update {
            it.copy(
                simpleText = simpleText ?: it.simpleText,
                database = database ?: it.database,
                preferences = preferences ?: it.preferences,
                files = files ?: it.files
            )
        }
    }

    fun startBackup(uri: Uri) {
        val state = _uiState.value
        val context = getApplication<Application>()

        val readWriteFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        val array = dataCenter.backupData
        val oldUri = if (array != null) Uri.parse(Data.fromByteArray(array).getString(BackupWorker.KEY_URI)) else null

        if (uri != oldUri) {
            try { if (oldUri != null) context.contentResolver.releasePersistableUriPermission(oldUri, readWriteFlags) } catch (_: Exception) {}
            context.contentResolver.takePersistableUriPermission(uri, readWriteFlags)
        }

        val workRequest = oneTimeBackupWorkRequest(uri, state.simpleText, state.database, state.preferences, state.files)
        observeWork(workRequest, OperationType.BACKUP)
    }

    fun startRestore(uri: Uri) {
        val state = _uiState.value
        val workRequest = oneTimeRestoreWorkRequest(uri, state.simpleText, state.database, state.preferences, state.files)
        observeWork(workRequest, OperationType.RESTORE)
    }

    private fun observeWork(workRequest: OneTimeWorkRequest, type: OperationType) {
        val context = getApplication<Application>()
        val workManager = WorkManager.getInstance(context)
        workManager.enqueue(workRequest)

        _uiState.update {
            it.copy(
                isOperationRunning = true,
                operationType = type,
                progress = 0f,
                currentStep = if (type == OperationType.BACKUP) context.getString(R.string.backup) else context.getString(R.string.restore),
                resultMessage = null,
                isSuccess = null
            )
        }

        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(workRequest.id).collect { info ->
                if (info == null) return@collect

                // Read progress data from worker
                val progressValue = info.progress.getFloat(PROGRESS_KEY, -1f)
                val stepName = info.progress.getString(STEP_KEY) ?: ""

                if (progressValue >= 0f) {
                    _uiState.update { it.copy(progress = progressValue, currentStep = stepName) }
                }

                when (info.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        val msg = info.outputData.getString(WORK_KEY_RESULT) ?: ""
                        _uiState.update {
                            it.copy(
                                isOperationRunning = false,
                                operationType = OperationType.NONE,
                                progress = 1f,
                                resultMessage = msg,
                                isSuccess = true,
                                showResultDialog = true
                            )
                        }
                    }
                    WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                        val msg = info.outputData.getString(WORK_KEY_RESULT) ?: ""
                        _uiState.update {
                            it.copy(
                                isOperationRunning = false,
                                operationType = OperationType.NONE,
                                progress = 0f,
                                resultMessage = msg,
                                isSuccess = false,
                                showResultDialog = true
                            )
                        }
                    }
                    else -> { /* ENQUEUED, RUNNING, BLOCKED */ }
                }
            }
        }
    }

    fun updateBackupFrequency(frequency: Int) {
        val context = getApplication<Application>()
        if (dataCenter.backupFrequency != frequency) {
            val workRequest = if (frequency != 0) periodicBackupWorkRequest(frequency) else null
            WorkManager.getInstance(context).apply {
                if (workRequest == null) {
                    cancelUniqueWork(BackupWorker.UNIQUE_WORK_NAME)
                } else {
                    enqueueUniquePeriodicWork(BackupWorker.UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, workRequest)
                }
            }
            dataCenter.backupFrequency = if (workRequest != null) frequency else 0
        }
        dismissDialog()
    }

    fun clearData() {
        _uiState.update { it.copy(isDeletingFiles = true, showClearDataDialog = false) }
        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                deleteDir(context.cacheDir)
                deleteDir(context.filesDir)
                dbHelper.removeAll()
                dataCenter.saveNovelSearchHistory(ArrayList())
            } catch (_: Exception) {
            } finally {
                _uiState.update { it.copy(isDeletingFiles = false) }
            }
        }
    }

    private fun deleteDir(dir: File?): Boolean {
        return if (dir != null && dir.isDirectory) {
            dir.listFiles()?.forEach { deleteDir(it) }
            dir.delete()
        } else dir?.delete() ?: false
    }

    companion object {
        const val PROGRESS_KEY = "backup_restore_progress"
        const val STEP_KEY = "backup_restore_step"
    }
}
