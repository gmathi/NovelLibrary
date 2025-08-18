package io.github.gmathi.novellibrary.viewmodel

import android.content.Context
import android.webkit.MimeTypeMap
import androidx.lifecycle.*
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.util.storage.getReadableSize
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class GoogleBackupViewModel @Inject constructor(
    private val state: SavedStateHandle,
    private val dataCenter: DataCenter
) : ViewModel(), LifecycleObserver {
    companion object {
        const val TAG = "GoogleBackupViewModel"
        const val KEY_NOVEL = "novelKey"
        const val BACKUP_FILENAME = "NovelLibraryBackup.zip"
        private val ZIP_MIME_TYPE = MimeTypeMap.getSingleton().getMimeTypeFromExtension("zip") ?: "application/zip"
    }

    //Objects to be part of the savedState
    lateinit var context: Context

    //last back variables
    var backupStatus = MutableLiveData<String>()
    var lastUpdatedTimestamp = MutableLiveData<String>()

    //google settings variables
    var backupInterval = MutableLiveData<String>()
    var googleAccountEmail = MutableLiveData<String>()
    var internetType = MutableLiveData<String>()

    //Backup Options
    var shouldBackupSimpleText: Boolean = false
    var shouldBackupDatabase: Boolean = false
    var shouldBackupPreferences: Boolean = false
    var shouldBackupFiles: Boolean = false

    fun init(lifecycleOwner: LifecycleOwner, context: Context) {
        lifecycleOwner.lifecycle.addObserver(this)
        this.context = context
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun onResume() {
        getBackupStatusData()
        getGoogleSettingsData()
    }

    private fun getBackupStatusData() {
        val pattern = "E - dd MMM, yyyy - hh:mm aa"
        val simpleDateFormat = SimpleDateFormat(pattern, Locale.getDefault())

        var localTimeStamp = "N/A"
        var localSize = "N/A"

        val externalPath = context.getExternalFilesDir(null)
        val backupFile = File(externalPath, BACKUP_FILENAME)
        if (backupFile.exists()) {
            localTimeStamp = simpleDateFormat.format(Date(backupFile.lastModified()))
            localSize = backupFile.getReadableSize()
        }

        val lastUpdatedText =
            "Local: $localTimeStamp" +
                    "\nGoogle Drive: ${dataCenter.lastCloudBackupTimestamp}" +
                    "\nSize: $localSize"
        lastUpdatedTimestamp.value = lastUpdatedText
    }

    private fun getGoogleSettingsData() {
        backupInterval.value = dataCenter.gdBackupInterval
        googleAccountEmail.value = dataCenter.gdAccountEmail
        internetType.value = dataCenter.gdInternetType
    }

    fun onBackupIntervalClicked() {}
    fun onGoogleAccountClicked() {}
    fun onInternetTypeClicked() {}

}
