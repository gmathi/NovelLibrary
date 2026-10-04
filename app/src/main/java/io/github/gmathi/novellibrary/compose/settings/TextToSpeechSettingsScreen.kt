package io.github.gmathi.novellibrary.compose.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Text-to-Speech parent — removes the top-level duplication of two separate TTS
 * entries. Exposes a single Engine selector (bound to dataCenter.useAiTts, the
 * same source of truth the AI TTS screen already uses) plus chevrons to the two
 * EXISTING engine sub-screens. The preference backends are intentionally NOT
 * merged (plan §7 decision 3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextToSpeechSettingsScreen(
    useAiTts: Boolean,
    onUseAiTtsChange: (Boolean) -> Unit,
    onOpenClassicTts: () -> Unit,
    onOpenAiTts: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Text-to-Speech") },
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
            item { SettingsSectionHeader(title = "Engine") }
            item {
                SwitchSettingRow(
                    title = "Use AI TTS engine",
                    subtitle = if (useAiTts)
                        "AI TTS (SherpaOnnx) is the active engine"
                    else
                        "Classic device TTS is the active engine",
                    checked = useAiTts,
                    onCheckedChange = onUseAiTtsChange
                )
            }
            item { HorizontalDivider() }

            item { SettingsSectionHeader(title = "Engine settings") }
            item {
                ChevronSettingRow(
                    title = "Classic TTS",
                    subtitle = "Device Read-Aloud settings",
                    onClick = onOpenClassicTts
                )
            }
            item {
                ChevronSettingRow(
                    title = "AI TTS",
                    subtitle = "Neural voice settings and models",
                    onClick = onOpenAiTts
                )
            }
        }
    }
}
