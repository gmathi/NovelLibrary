package io.github.gmathi.novellibrary.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.network.GoogleDriveHelper
import kotlinx.coroutines.launch
import uy.kohesive.injekt.injectLazy

class GoogleBackupViewModel : ViewModel() {

    private val dataCenter: DataCenter by injectLazy()

    val backupInfo = MutableLiveData<GoogleDriveHelper.BackupInfo?>()
    val isLoading = MutableLiveData(false)
    val errorMessage = MutableLiveData<String?>()

    fun fetchBackupInfo(context: Context) {
        isLoading.value = true
        errorMessage.value = null
        val driveHelper = GoogleDriveHelper(context)
        viewModelScope.launch {
            val result = driveHelper.getBackupInfo()
            isLoading.postValue(false)
            if (result.isSuccess) {
                backupInfo.postValue(result.getOrNull())
            } else {
                errorMessage.postValue(result.exceptionOrNull()?.localizedMessage)
            }
        }
    }
}
