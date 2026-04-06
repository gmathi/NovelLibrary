package io.github.gmathi.novellibrary.compose.ai_tts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.gmathi.novellibrary.compose.theme.NovelLibraryTheme
import io.github.gmathi.novellibrary.service.ai_tts.AiTtsModelManager
import io.github.gmathi.novellibrary.service.ai_tts.AiTtsVoiceInfo
import io.github.gmathi.novellibrary.service.ai_tts.ModelDownloadState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTtsManageModelsScreen(
    modelManager: AiTtsModelManager,
    onNavigateBack: () -> Unit = {}
) {
    val voices = remember { modelManager.availableVoices() }
    val downloadStates = remember {
        mutableStateMapOf<String, ModelDownloadState>().also { map ->
            voices.forEach { voice ->
                map[voice.id] = if (modelManager.isModelDownloaded(voice.id))
                    ModelDownloadState.Downloaded
                else
                    ModelDownloadState.NotDownloaded
            }
        }
    }
    val scope = rememberCoroutineScope()
    var deleteConfirmVoice by remember { mutableStateOf<AiTtsVoiceInfo?>(null) }

    deleteConfirmVoice?.let { voice ->
        AlertDialog(
            onDismissRequest = { deleteConfirmVoice = null },
            title = { Text("Delete Model") },
            text = { Text("Delete \"${voice.name}\"? You will need to re-download it to use this voice.") },
            confirmButton = {
                TextButton(onClick = {
                    modelManager.deleteModel(voice.id)
                    downloadStates[voice.id] = ModelDownloadState.NotDownloaded
                    deleteConfirmVoice = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmVoice = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Models") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(voices, key = { it.id }) { voice ->
                VoiceModelCard(
                    voice = voice,
                    state = downloadStates[voice.id] ?: ModelDownloadState.NotDownloaded,
                    onDownload = {
                        scope.launch {
                            modelManager.downloadModel(
                                voice = voice,
                                onProgress = { progress ->
                                    downloadStates[voice.id] = ModelDownloadState.Downloading(progress)
                                },
                                onComplete = {
                                    downloadStates[voice.id] = ModelDownloadState.Downloaded
                                },
                                onError = { error ->
                                    downloadStates[voice.id] = ModelDownloadState.Error(error)
                                }
                            )
                        }
                    },
                    onDelete = { deleteConfirmVoice = voice }
                )
            }
        }
    }
}

@Composable
private fun VoiceModelCard(
    voice: AiTtsVoiceInfo,
    state: ModelDownloadState,
    onDownload: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(voice.name, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "${voice.language} · ${formatBytes(voice.sizeBytes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                when (state) {
                    is ModelDownloadState.Downloaded -> {
                        IconButton(onClick = onDelete) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    is ModelDownloadState.NotDownloaded, is ModelDownloadState.Error -> {
                        TextButton(onClick = onDownload) { Text("Download") }
                    }
                    is ModelDownloadState.Downloading -> {
                        // button hidden during download
                    }
                }
            }

            when (state) {
                is ModelDownloadState.Downloading -> {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "${(state.progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                is ModelDownloadState.Error -> {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Error: ${state.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                is ModelDownloadState.Downloaded -> {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Downloaded",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                else -> {}
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    return when {
        bytes >= 1_000_000L -> "${"%.0f".format(bytes / 1_000_000.0)} MB"
        bytes >= 1_000L -> "${"%.0f".format(bytes / 1_000.0)} KB"
        else -> "$bytes B"
    }
}

@Preview(showBackground = true)
@Composable
private fun VoiceModelCardPreview() {
    NovelLibraryTheme {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            VoiceModelCard(
                voice = AiTtsVoiceInfo("id1", "Ryan (US English, High)", "en-US", 63_000_000L, "", ""),
                state = ModelDownloadState.NotDownloaded,
                onDownload = {}, onDelete = {}
            )
            VoiceModelCard(
                voice = AiTtsVoiceInfo("id2", "Amy (US English, Medium)", "en-US", 30_000_000L, "", ""),
                state = ModelDownloadState.Downloaded,
                onDownload = {}, onDelete = {}
            )
            VoiceModelCard(
                voice = AiTtsVoiceInfo("id3", "Kristin (US English, Low)", "en-US", 10_000_000L, "", ""),
                state = ModelDownloadState.Downloading(0.45f),
                onDownload = {}, onDelete = {}
            )
        }
    }
}
