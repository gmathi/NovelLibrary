package io.github.gmathi.novellibrary.compose.ai_tts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.gmathi.novellibrary.compose.theme.NovelLibraryTheme
import io.github.gmathi.novellibrary.service.ai_tts.AiTtsVoiceInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTtsSettingsScreen(
    speechRate: Float = 1.0f,
    pitch: Float = 1.0f,
    autoReadNextChapter: Boolean = true,
    keepScreenOn: Boolean = false,
    volumeNormalization: Boolean = true,
    activeVoiceId: String = "en_US-ryan-high",
    availableVoices: List<AiTtsVoiceInfo> = emptyList(),
    onSpeechRateChange: (Float) -> Unit = {},
    onPitchChange: (Float) -> Unit = {},
    onAutoReadNextChapterChange: (Boolean) -> Unit = {},
    onKeepScreenOnChange: (Boolean) -> Unit = {},
    onVolumeNormalizationChange: (Boolean) -> Unit = {},
    onVoiceSelected: (AiTtsVoiceInfo) -> Unit = {},
    onManageModels: () -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    var showVoicePicker by remember { mutableStateOf(false) }
    val activeVoice = availableVoices.find { it.id == activeVoiceId }

    if (showVoicePicker) {
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
                    valueText = "${"%.1f".format(speechRate)}x",
                    valueRange = 0.5f..2.0f,
                    onValueChange = onSpeechRateChange
                )
            }
            item {
                SliderSettingRow(
                    title = "Pitch",
                    value = pitch,
                    valueText = "${"%.1f".format(pitch)}x",
                    valueRange = 0.5f..2.0f,
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

            item { SectionHeader(title = "Voice & Model") }
            item {
                ChevronSettingRow(
                    title = "Active Voice",
                    subtitle = activeVoice?.name ?: activeVoiceId,
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

@Preview(showBackground = true)
@Composable
fun AiTtsSettingsScreenPreview() {
    NovelLibraryTheme {
        AiTtsSettingsScreen(
            availableVoices = listOf(
                AiTtsVoiceInfo(
                    id = "en_US-ryan-high",
                    name = "Ryan (English US)",
                    language = "en-US",
                    sizeBytes = 60_000_000L,
                    downloadUrl = "",
                    tokensUrl = "",
                    checksumMd5 = ""
                )
            )
        )
    }
}
