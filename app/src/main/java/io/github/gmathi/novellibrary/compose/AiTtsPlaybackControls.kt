package io.github.gmathi.novellibrary.compose

import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import io.github.gmathi.novellibrary.model.preference.TTSPreferences
import io.github.gmathi.novellibrary.service.tts.AiVoicePreset
import io.github.gmathi.novellibrary.service.tts.TTSService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * AI TTS Playback Controls UI component.
 * Provides real-time voice and speed controls for AI TTS during playback.
 * 
 * This composable follows MVVM architecture with AiTtsPlaybackViewModel managing state
 * and communicating with TTSService via MediaController commands.
 */
@Composable
fun AiTtsPlaybackControls(
    viewModel: AiTtsPlaybackViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Voice Selector
        VoiceSelector(
            selectedVoice = uiState.selectedVoice,
            voices = uiState.availableVoices,
            onVoiceSelected = { viewModel.onVoiceChanged(it) }
        )
        
        // Speed Slider
        SpeedSlider(
            speed = uiState.speed,
            onSpeedChanged = { viewModel.onSpeedChanged(it) }
        )
    }
}

/**
 * Voice selector dropdown displaying all available AI voice presets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSelector(
    selectedVoice: AiVoicePreset,
    voices: List<AiVoicePreset>,
    onVoiceSelected: (AiVoicePreset) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "AI Voice",
            style = MaterialTheme.typography.labelMedium
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = "${selectedVoice.displayName} (${selectedVoice.gender})",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                voices.forEach { voice ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(voice.displayName)
                                Text(
                                    text = voice.gender,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        onClick = {
                            onVoiceSelected(voice)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

/**
 * Speed slider with range 0.5x to 3.0x.
 */
@Composable
fun SpeedSlider(
    speed: Float,
    onSpeedChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AI Speed",
                style = MaterialTheme.typography.labelMedium
            )
            
            Text(
                text = String.format("%.1fx", speed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Slider(
            value = speed,
            onValueChange = onSpeedChanged,
            valueRange = 0.5f..3.0f,
            steps = 24, // 0.1 increments: (3.0 - 0.5) / 0.1 - 1 = 24
            modifier = Modifier.fillMaxWidth()
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "0.5x",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "3.0x",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * ViewModel for AI TTS Playback Controls.
 * Manages UI state and communicates with TTSService via MediaController.
 */
class AiTtsPlaybackViewModel(
    private val mediaController: MediaControllerCompat?,
    private val ttsPreferences: TTSPreferences
) : ViewModel() {
    
    data class UiState(
        val selectedVoice: AiVoicePreset = AiVoicePreset.EXPR_VOICE_2_M,
        val availableVoices: List<AiVoicePreset> = AiVoicePreset.entries,
        val speed: Float = 1.0f,
        val isAiTtsActive: Boolean = false
    )
    
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()
    
    init {
        loadPreferences()
    }
    
    /**
     * Handle voice preset change.
     * Updates preferences and sends command to TTSService.
     */
    fun onVoiceChanged(voice: AiVoicePreset) {
        _uiState.update { it.copy(selectedVoice = voice) }
        ttsPreferences.aiVoicePreset = voice.sid
        
        // Send command to TTSService to update voice in real-time
        mediaController?.sendCommand(
            TTSService.ACTION_UPDATE_AI_VOICE,
            Bundle().apply {
                putInt("voiceId", voice.sid)
            },
            null
        )
    }
    
    /**
     * Handle speed change.
     * Updates preferences and sends command to TTSService.
     */
    fun onSpeedChanged(speed: Float) {
        _uiState.update { it.copy(speed = speed) }
        ttsPreferences.aiSpeed = speed
        
        // Send command to TTSService to update speed in real-time
        mediaController?.sendCommand(
            TTSService.ACTION_UPDATE_AI_SPEED,
            Bundle().apply {
                putFloat("speed", speed)
            },
            null
        )
    }
    
    /**
     * Load current preferences into UI state.
     */
    private fun loadPreferences() {
        _uiState.update {
            it.copy(
                selectedVoice = AiVoicePreset.fromSid(ttsPreferences.aiVoicePreset),
                speed = ttsPreferences.aiSpeed,
                isAiTtsActive = ttsPreferences.ttsEngine == "ai_vits"
            )
        }
    }
}
