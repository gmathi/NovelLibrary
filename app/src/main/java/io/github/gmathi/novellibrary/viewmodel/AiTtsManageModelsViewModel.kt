package io.github.gmathi.novellibrary.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import androidx.work.WorkManager
import io.github.gmathi.novellibrary.service.ai_tts.AiTtsModelManager
import io.github.gmathi.novellibrary.service.ai_tts.AiTtsVoiceInfo
import io.github.gmathi.novellibrary.service.ai_tts.ModelDownloadState
import io.github.gmathi.novellibrary.worker.AiTtsModelDownloadWorker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AiTtsManageModelsViewModel(application: Application) : AndroidViewModel(application) {

    val modelManager = AiTtsModelManager(application)
    private val workManager = WorkManager.getInstance(application)

    val allVoices: List<AiTtsVoiceInfo> = modelManager.availableVoices()

    private val _downloadStates = MutableStateFlow<Map<String, ModelDownloadState>>(emptyMap())
    val downloadStates: StateFlow<Map<String, ModelDownloadState>> = _downloadStates.asStateFlow()

    init {
        // Initialize states from disk
        val initial = mutableMapOf<String, ModelDownloadState>()
        allVoices.forEach { voice ->
            initial[voice.id] = if (modelManager.isModelDownloaded(voice.id))
                ModelDownloadState.Downloaded
            else
                ModelDownloadState.NotDownloaded
        }
        _downloadStates.value = initial

        // Observe WorkManager for each voice to pick up in-progress / completed downloads
        allVoices.forEach { voice ->
            observeWorker(voice.id)
        }
    }

    private fun observeWorker(voiceId: String) {
        val liveData = workManager.getWorkInfosForUniqueWorkLiveData("ai_tts_download_$voiceId")
        // observeForever is safe here — ViewModel outlives config changes and we never
        // remove the observer because the VM is scoped to the Activity.
        liveData.observeForever { infos ->
            val info = infos?.firstOrNull() ?: return@observeForever
            val current = _downloadStates.value.toMutableMap()
            when (info.state) {
                WorkInfo.State.RUNNING -> {
                    val pct = info.progress.getInt(AiTtsModelDownloadWorker.KEY_PROGRESS, 0)
                    current[voiceId] = ModelDownloadState.Downloading(pct / 100f)
                }
                WorkInfo.State.ENQUEUED -> {
                    // Only show downloading if we're not already in a terminal state
                    val prev = current[voiceId]
                    if (prev is ModelDownloadState.NotDownloaded || prev is ModelDownloadState.Downloading) {
                        current[voiceId] = ModelDownloadState.Downloading(0f)
                    }
                }
                WorkInfo.State.SUCCEEDED -> {
                    // Verify files actually exist (guards against stale completed work)
                    if (modelManager.isModelDownloaded(voiceId)) {
                        current[voiceId] = ModelDownloadState.Downloaded
                    }
                }
                WorkInfo.State.FAILED -> {
                    val prev = current[voiceId]
                    if (prev is ModelDownloadState.Downloading) {
                        val error = info.outputData.getString("error") ?: "Download failed"
                        current[voiceId] = ModelDownloadState.Error(error)
                    }
                }
                else -> { /* BLOCKED, CANCELLED — no-op */ }
            }
            _downloadStates.value = current
        }
    }

    fun downloadModel(voiceId: String) {
        val current = _downloadStates.value.toMutableMap()
        current[voiceId] = ModelDownloadState.Downloading(0f)
        _downloadStates.value = current
        AiTtsModelDownloadWorker.enqueue(getApplication(), voiceId)
    }

    fun cancelDownload(voiceId: String) {
        workManager.cancelUniqueWork("ai_tts_download_$voiceId")
        val current = _downloadStates.value.toMutableMap()
        current[voiceId] = ModelDownloadState.NotDownloaded
        _downloadStates.value = current
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            modelManager.getModelDir(voiceId).deleteRecursively()
        }
    }

    fun deleteModel(voiceId: String) {
        modelManager.deleteModel(voiceId)
        val current = _downloadStates.value.toMutableMap()
        current[voiceId] = ModelDownloadState.NotDownloaded
        _downloadStates.value = current
    }
}
