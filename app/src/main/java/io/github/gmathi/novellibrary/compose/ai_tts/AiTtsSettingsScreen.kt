package io.github.gmathi.novellibrary.compose.ai_tts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
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
import io.github.gmathi.novellibrary.service.ai_tts.KokoroVoice
import io.github.gmathi.novellibrary.service.ai_tts.KokoroVoiceHelper
import io.github.gmathi.novellibrary.service.ai_tts.TtsEngineType
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTtsSettingsScreen(
    speechRate: Float = 1.0f,
    pitch: Float = 1.0f,
    autoReadNextChapter: Boolean = true,
    keepScreenOn: Boolean = false,
    volumeNormalization: Boolean = true,
    smartPunctuation: Boolean = true,
    emotionTags: Boolean = false,
    activeVoiceId: String = "en_US-ryan-high",
    availableVoices: List<AiTtsVoiceInfo> = emptyList(),
    modelManager: AiTtsModelManager? = null,
    kokoroSpeakerId: Int = 0,
    onSpeechRateChange: (Float) -> Unit = {},
    onPitchChange: (Float) -> Unit = {},
    onAutoReadNextChapterChange: (Boolean) -> Unit = {},
    onKeepScreenOnChange: (Boolean) -> Unit = {},
    onVolumeNormalizationChange: (Boolean) -> Unit = {},
    onSmartPunctuationChange: (Boolean) -> Unit = {},
    onEmotionTagsChange: (Boolean) -> Unit = {},
    onVoiceSelected: (AiTtsVoiceInfo) -> Unit = {},
    onKokoroVoiceSelected: (KokoroVoice) -> Unit = {},
    onManageModels: () -> Unit = {},
    onClearCache: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    var showVoicePicker by remember { mutableStateOf(false) }
    var showEmotionBetaDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    val activeVoice = availableVoices.find { it.id == activeVoiceId }
    val isKokoroActive = activeVoice?.engineType == TtsEngineType.KOKORO
    val activeKokoroVoice = if (isKokoroActive) KokoroVoiceHelper.getById(kokoroSpeakerId) else null

    // Calculate storage info
    val downloadedCount = remember(activeVoiceId) {
        modelManager?.let { mm ->
            mm.availableVoices().count { mm.isModelDownloaded(it.id) }
        } ?: 0
    }
    val totalSizeText = remember(activeVoiceId) {
        modelManager?.let { mm ->
            val totalBytes = mm.availableVoices()
                .filter { mm.isModelDownloaded(it.id) }
                .sumOf { mm.getModelDir(it.id).walkTopDown().filter { f -> f.isFile }.sumOf { f -> f.length() } }
            val mb = totalBytes / (1024.0 * 1024.0)
            if (mb > 1024) String.format(Locale.US, "%.2f GB", mb / 1024.0)
            else String.format(Locale.US, "%.2f MB", mb)
        } ?: "0.00 MB"
    }

    if (showVoicePicker) {
        if (isKokoroActive) {
            KokoroVoicePickerDialog(
                voices = KokoroVoiceHelper.ALL_VOICES,
                selectedSpeakerId = kokoroSpeakerId,
                onVoiceSelected = { voice ->
                    onKokoroVoiceSelected(voice)
                    showVoicePicker = false
                },
                onDismiss = { showVoicePicker = false }
            )
        } else {
            VoicePickerDialog(
                voices = availableVoices,
                selectedVoiceId = activeVoiceId,
                onVoiceSelected = { voice ->
                    onVoiceSelected(voice)
                    showVoicePicker = false
                },
                onDismiss = { showVoicePicker = false }
            )
        }
    }

    if (showEmotionBetaDialog) {
        AlertDialog(
            onDismissRequest = { showEmotionBetaDialog = false },
            title = { Text("Beta Feature") },
            text = { Text("Emotion tagging is currently in beta. It may not perform perfectly with all voice models. Do you wish to continue?") },
            confirmButton = {
                TextButton(onClick = {
                    onEmotionTagsChange(true)
                    showEmotionBetaDialog = false
                }) { Text("Continue") }
            },
            dismissButton = {
                TextButton(onClick = { showEmotionBetaDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear All Cache") },
            text = { Text("This will delete all downloaded voice models and reset settings to defaults. Are you sure?") },
            confirmButton = {
                TextButton(onClick = {
                    onClearCache()
                    showClearCacheDialog = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI TTS Settings") },
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
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            item { SectionHeader(title = "Playback") }
            item {
                SliderSettingRow(
                    title = "Speech Rate",
                    value = speechRate,
                    valueText = "${"%.2f".format(speechRate)}x",
                    valueRange = 0.5f..2.0f,
                    steps = 29,
                    onValueChange = onSpeechRateChange
                )
            }
            item {
                SliderSettingRow(
                    title = "Pitch",
                    value = pitch,
                    valueText = "${"%.2f".format(pitch)}x",
                    valueRange = 0.5f..2.0f,
                    steps = 29,
                    onValueChange = onPitchChange
                )
            }
            item {
                SwitchSettingRow(
                    title = "Auto-read Next Chapter",
                    subtitle = "Automatically continue to the next chapter",
                    checked = autoReadNextChapter,
                    onCheckedChange = onAutoReadNextChapterChange
                )
            }
            item {
                SwitchSettingRow(
                    title = "Keep Screen On",
                    subtitle = "Prevent screen from turning off during playback",
                    checked = keepScreenOn,
                    onCheckedChange = onKeepScreenOnChange
                )
            }
            item {
                SwitchSettingRow(
                    title = "Volume Normalization",
                    subtitle = "Normalize audio volume across sentences",
                    checked = volumeNormalization,
                    onCheckedChange = onVolumeNormalizationChange
                )
            }

            item { SectionHeader(title = "Voice Processing") }
            item {
                SwitchSettingRow(
                    title = "Smart Punctuation",
                    subtitle = "Add natural pauses at commas, periods, and other punctuation",
                    checked = smartPunctuation,
                    onCheckedChange = onSmartPunctuationChange
                )
            }
            item {
                SwitchSettingRow(
                    title = "Emotion Tags (Beta)",
                    subtitle = "Process tags like [sad], [angry], [whisper] in text",
                    checked = emotionTags,
                    onCheckedChange = { enabled ->
                        if (enabled) showEmotionBetaDialog = true
                        else onEmotionTagsChange(false)
                    }
                )
            }

            item { SectionHeader(title = "Voice & Model") }
            item {
                ChevronSettingRow(
                    title = "Active Voice",
                    subtitle = if (isKokoroActive && activeKokoroVoice != null)
                        "${activeKokoroVoice.fullLabel}"
                    else
                        activeVoice?.name ?: activeVoiceId,
                    onClick = { showVoicePicker = true }
                )
            }
            item {
                ChevronSettingRow(
                    title = "Manage Models",
                    subtitle = "Download or delete voice models",
                    onClick = onManageModels
                )
            }

            item { SectionHeader(title = "Storage") }
            item {
                InfoRow(
                    title = "Downloaded Models",
                    value = "$downloadedCount local models"
                )
            }
            item {
                InfoRow(
                    title = "Total Size",
                    value = totalSizeText
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showClearCacheDialog = true }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Clear Cache", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error)
                        Text("Delete all models and reset settings", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp, end = 16.dp)
    )
    HorizontalDivider()
}

@Composable
private fun SliderSettingRow(
    title: String,
    value: Float,
    valueText: String,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(valueText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            steps = steps,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun SwitchSettingRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ChevronSettingRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoRow(
    title: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun VoicePickerDialog(
    voices: List<AiTtsVoiceInfo>,
    selectedVoiceId: String,
    onVoiceSelected: (AiTtsVoiceInfo) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Voice") },
        text = {
            LazyColumn {
                items(voices.size) { index ->
                    val voice = voices[index]
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onVoiceSelected(voice) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = voice.id == selectedVoiceId,
                            onClick = { onVoiceSelected(voice) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(voice.name, style = MaterialTheme.typography.bodyLarge)
                            Text(voice.language, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun KokoroVoicePickerDialog(
    voices: List<KokoroVoice>,
    selectedSpeakerId: Int,
    onVoiceSelected: (KokoroVoice) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedLanguage by remember { mutableStateOf<String?>(null) }
    val languages = remember { voices.map { it.language }.distinct().sorted() }

    val filteredVoices = remember(searchQuery, selectedLanguage) {
        voices.filter { voice ->
            val matchesSearch = searchQuery.isBlank() ||
                voice.displayName.contains(searchQuery, ignoreCase = true) ||
                voice.voiceKey.contains(searchQuery, ignoreCase = true)
            val matchesLang = selectedLanguage == null || voice.language == selectedLanguage
            matchesSearch && matchesLang
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Kokoro Voice (${filteredVoices.size})") },
        text = {
            Column {
                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search voices…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                // Language filter chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = selectedLanguage == null,
                        onClick = { selectedLanguage = null },
                        label = { Text("All", style = MaterialTheme.typography.labelSmall) }
                    )
                    languages.take(5).forEach { lang ->
                        FilterChip(
                            selected = selectedLanguage == lang,
                            onClick = { selectedLanguage = if (selectedLanguage == lang) null else lang },
                            label = { Text(lang, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
                if (languages.size > 5) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        languages.drop(5).forEach { lang ->
                            FilterChip(
                                selected = selectedLanguage == lang,
                                onClick = { selectedLanguage = if (selectedLanguage == lang) null else lang },
                                label = { Text(lang, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                // Voice list
                LazyColumn(modifier = Modifier.heightIn(max = 350.dp)) {
                    items(filteredVoices.size) { index ->
                        val voice = filteredVoices[index]
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onVoiceSelected(voice) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = voice.speakerId == selectedSpeakerId,
                                onClick = { onVoiceSelected(voice) }
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "${voice.flag} ${voice.displayName}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    voice.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun AiTtsSettingsScreenPreview() {
    NovelLibraryTheme {
        AiTtsSettingsScreen(
            availableVoices = listOf(
                AiTtsVoiceInfo(
                    id = "kokoro_multi_lang",
                    name = "Kokoro Multi-Lang",
                    language = "Multi-language",
                    sizeBytes = 337_000_000L,
                    engineType = TtsEngineType.KOKORO,
                    downloadUrl = "",
                    tokensUrl = "",
                )
            )
        )
    }
}
