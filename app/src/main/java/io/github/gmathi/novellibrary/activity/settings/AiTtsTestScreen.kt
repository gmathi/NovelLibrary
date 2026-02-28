package io.github.gmathi.novellibrary.activity.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTtsTestScreen(
    viewModel: AiTtsTestViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI TTS Test") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = uiState.statusText,
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = uiState.inputText,
                onValueChange = { viewModel.updateInputText(it) },
                label = { Text("Enter text to speak") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Column {
                Text(
                    text = "Speed: ${"%.1f".format(uiState.speed)}x",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = uiState.speed,
                    onValueChange = { viewModel.updateSpeed(it) },
                    valueRange = 0.5f..3.0f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Column {
                Text(
                    text = "Pitch: ${"%.1f".format(uiState.pitch)}x",
                    style = MaterialTheme.typography.bodyMedium
                )
                Slider(
                    value = uiState.pitch,
                    onValueChange = { viewModel.updatePitch(it) },
                    valueRange = 0.5f..2.0f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.speak() },
                    enabled = uiState.isSpeakEnabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Speak")
                }

                Button(
                    onClick = { viewModel.stopSpeaking() },
                    enabled = uiState.isStopEnabled,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Stop")
                }
            }
        }
    }
}
