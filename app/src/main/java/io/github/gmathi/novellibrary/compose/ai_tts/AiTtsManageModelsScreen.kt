package io.github.gmathi.novellibrary.compose.ai_tts

import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.gmathi.novellibrary.compose.theme.NovelLibraryTheme
import io.github.gmathi.novellibrary.service.ai_tts.AiTtsModelManager
import io.github.gmathi.novellibrary.service.ai_tts.AiTtsVoiceInfo
import io.github.gmathi.novellibrary.service.ai_tts.TtsEngineType
import io.github.gmathi.novellibrary.service.ai_tts.ModelDownloadState
import kotlinx.coroutines.launch

// Filter options mirroring the reference project's sort/filter bottom sheet
private enum class ModelFilter { ALL, DOWNLOAD, INSTALLED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTtsManageModelsScreen(
    modelManager: AiTtsModelManager,
    activeVoiceId: String = "",
    onVoiceSelected: (String) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val allVoices = remember { modelManager.availableVoices() }
    val downloadStates = remember {
        mutableStateMapOf<String, ModelDownloadState>().also { map ->
            allVoices.forEach { voice ->
                map[voice.id] = if (modelManager.isModelDownloaded(voice.id))
                    ModelDownloadState.Downloaded
                else
                    ModelDownloadState.NotDownloaded
            }
        }
    }

    var activeFilter by remember { mutableStateOf(ModelFilter.ALL) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var deleteConfirmVoice by remember { mutableStateOf<AiTtsVoiceInfo?>(null) }
    val scope = rememberCoroutineScope()

    // Storage info
    val (storageUsedGb, storageTotalGb, storagePercent) = remember {
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            val total = stat.blockSizeLong * stat.blockCountLong
            val avail = stat.blockSizeLong * stat.availableBlocksLong
            val used = total - avail
            Triple(
                used / 1_073_741_824.0,
                total / 1_073_741_824.0,
                if (total > 0) (used * 100 / total).toInt() else 0
            )
        } catch (e: Exception) {
            Triple(0.0, 0.0, 0)
        }
    }

    val filteredVoices = remember(allVoices, activeFilter, downloadStates.toMap()) {
        allVoices.filter { voice ->
            when (activeFilter) {
                ModelFilter.DOWNLOAD -> downloadStates[voice.id] !is ModelDownloadState.Downloaded
                ModelFilter.INSTALLED -> downloadStates[voice.id] is ModelDownloadState.Downloaded
                ModelFilter.ALL -> true
            }
        }
    }

    // Delete confirmation dialog
    deleteConfirmVoice?.let { voice ->
        AlertDialog(
            onDismissRequest = { deleteConfirmVoice = null },
            title = { Text("Delete Model") },
            text = { Text("Delete \"${voice.name}\"? You will need to re-download it to use this voice.") },
            confirmButton = {
                TextButton(onClick = {
                    modelManager.deleteModel(voice.id)
                    downloadStates[voice.id] = ModelDownloadState.NotDownloaded
                    if (activeVoiceId == voice.id) onVoiceSelected("")
                    deleteConfirmVoice = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmVoice = null }) { Text("Cancel") }
            }
        )
    }

    // Filter bottom sheet
    if (showFilterSheet) {
        ModalBottomSheet(onDismissRequest = { showFilterSheet = false }) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(
                    "Filter Models",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
                HorizontalDivider()
                ModelFilter.entries.forEach { filter ->
                    val label = when (filter) {
                        ModelFilter.ALL -> "All Models"
                        ModelFilter.DOWNLOAD -> "Not Downloaded"
                        ModelFilter.INSTALLED -> "Installed"
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = activeFilter == filter,
                            onClick = { activeFilter = filter; showFilterSheet = false }
                        )
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Voice Models") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            // Placeholder FAB — local import can be wired up later
            SmallFloatingActionButton(onClick = { /* TODO: local import */ }) {
                Icon(Icons.Default.Add, contentDescription = "Import local model")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // ── Storage card ──────────────────────────────────────────────────────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Storage,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Local Storage",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (storageTotalGb > 0)
                                "${"%.1f".format(storageUsedGb)} GB of ${"%.1f".format(storageTotalGb)} GB used"
                            else
                                "Storage info unavailable",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (storageTotalGb > 0) {
                        LinearProgressIndicator(
                            progress = { storagePercent / 100f },
                            modifier = Modifier
                                .width(80.dp)
                                .height(5.dp)
                        )
                    }
                }
            }

            // ── List header + filter ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "MODELS LIST (${filteredVoices.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { showFilterSheet = true }) {
                    val filterLabel = when (activeFilter) {
                        ModelFilter.ALL -> "All Models"
                        ModelFilter.DOWNLOAD -> "Not Downloaded"
                        ModelFilter.INSTALLED -> "Installed"
                    }
                    Text(filterLabel, style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.Sort, contentDescription = "Filter", modifier = Modifier.size(16.dp))
                }
            }

            // ── Model list ────────────────────────────────────────────────────────
            if (filteredVoices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No models found",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        Text(
                            "Change the filter or tap + to import",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredVoices, key = { it.id }) { voice ->
                        VoiceModelCard(
                            voice = voice,
                            state = downloadStates[voice.id] ?: ModelDownloadState.NotDownloaded,
                            isActive = voice.id == activeVoiceId,
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
                            onCancelDownload = {
                                // Cancellation is handled by coroutine scope cancellation;
                                // reset state to NotDownloaded
                                downloadStates[voice.id] = ModelDownloadState.NotDownloaded
                                modelManager.getModelDir(voice.id).deleteRecursively()
                            },
                            onUseVoice = { onVoiceSelected(voice.id) },
                            onRemoveActive = { onVoiceSelected("") },
                            onDelete = { deleteConfirmVoice = voice }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceModelCard(
    voice: AiTtsVoiceInfo,
    state: ModelDownloadState,
    isActive: Boolean,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onUseVoice: () -> Unit,
    onRemoveActive: () -> Unit,
    onDelete: () -> Unit
) {
    val isDownloaded = state is ModelDownloadState.Downloaded
    val isDownloading = state is ModelDownloadState.Downloading

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ── Left: info + controls ─────────────────────────────────────────────
            Column(modifier = Modifier.weight(1f)) {
                // Tags row
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SurfaceChip("ONNX")
                    SurfaceChip("Token", tinted = true)
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    voice.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )

                // Sub-text: language · size · progress
                val subText = when {
                    isDownloading -> {
                        val prog = (state as ModelDownloadState.Downloading).progress
                        "${voice.language} · ${formatBytes(voice.sizeBytes)} · ${(prog * 100).toInt()}%"
                    }
                    else -> "${voice.language} · ${formatBytes(voice.sizeBytes)}"
                }
                Text(
                    subText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Progress bar (only while downloading)
                if (isDownloading) {
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (state as ModelDownloadState.Downloading).progress },
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(5.dp)
                    )
                }

                if (state is ModelDownloadState.Error) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Error: ${state.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Action buttons row
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when {
                        isDownloading -> {
                            // Cancel button
                            Button(
                                onClick = onCancelDownload,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    "Cancel",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        isDownloaded -> {
                            // Use Voice / Remove button
                            Button(
                                onClick = if (isActive) onRemoveActive else onUseVoice,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isActive)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text(
                                    if (isActive) "Remove" else "Use Voice",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                            // Delete button
                            FilledTonalIconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                        else -> {
                            // Download button
                            Button(
                                onClick = onDownload,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                modifier = Modifier.height(32.dp)
                            ) {
                                Text("Download", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            // ── Right: mic icon card ──────────────────────────────────────────────
            Spacer(Modifier.width(10.dp))
            Card(
                enabled = !isDownloading,
                onClick = when {
                    isDownloaded -> if (isActive) onRemoveActive else onUseVoice
                    else -> onDownload
                },
                modifier = Modifier.size(width = 72.dp, height = 80.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isDownloaded) Icons.Default.Mic else Icons.Default.Download,
                        contentDescription = when {
                            isDownloaded && isActive -> "Remove active voice"
                            isDownloaded -> "Use this voice"
                            else -> "Download model"
                        },
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    if (isActive) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Active",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier
                                .size(18.dp)
                                .align(Alignment.BottomEnd)
                                .padding(end = 6.dp, bottom = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SurfaceChip(label: String, tinted: Boolean = false) {
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = if (tinted)
            MaterialTheme.colorScheme.tertiaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (tinted)
                MaterialTheme.colorScheme.onTertiaryContainer
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

private fun formatBytes(bytes: Long): String = when {
    bytes >= 1_000_000L -> "${"%.0f".format(bytes / 1_000_000.0)} MB"
    bytes >= 1_000L -> "${"%.0f".format(bytes / 1_000.0)} KB"
    else -> "$bytes B"
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true)
@Composable
private fun VoiceModelCardPreview() {
    NovelLibraryTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            VoiceModelCard(
                voice = AiTtsVoiceInfo(id = "id1", name = "Kokoro Multi-Lang", language = "Multi", sizeBytes = 337_000_000L, engineType = TtsEngineType.KOKORO, downloadUrl = "", tokensUrl = ""),
                state = ModelDownloadState.NotDownloaded,
                isActive = false,
                onDownload = {}, onCancelDownload = {}, onUseVoice = {}, onRemoveActive = {}, onDelete = {}
            )
            VoiceModelCard(
                voice = AiTtsVoiceInfo(id = "id2", name = "Ryan (English, High)", language = "en-US", sizeBytes = 115_000_000L, engineType = TtsEngineType.VITS, downloadUrl = "", tokensUrl = ""),
                state = ModelDownloadState.Downloaded,
                isActive = true,
                onDownload = {}, onCancelDownload = {}, onUseVoice = {}, onRemoveActive = {}, onDelete = {}
            )
            VoiceModelCard(
                voice = AiTtsVoiceInfo(id = "id3", name = "Pratham (Hindi, Medium)", language = "hi-IN", sizeBytes = 60_000_000L, engineType = TtsEngineType.VITS, downloadUrl = "", tokensUrl = ""),
                state = ModelDownloadState.Downloading(0.45f),
                isActive = false,
                onDownload = {}, onCancelDownload = {}, onUseVoice = {}, onRemoveActive = {}, onDelete = {}
            )
            VoiceModelCard(
                voice = AiTtsVoiceInfo(id = "id4", name = "Test (Error State)", language = "en-US", sizeBytes = 10_000_000L, engineType = TtsEngineType.VITS, downloadUrl = "", tokensUrl = ""),
                state = ModelDownloadState.Error("Connection timeout"),
                isActive = false,
                onDownload = {}, onCancelDownload = {}, onUseVoice = {}, onRemoveActive = {}, onDelete = {}
            )
        }
    }
}
