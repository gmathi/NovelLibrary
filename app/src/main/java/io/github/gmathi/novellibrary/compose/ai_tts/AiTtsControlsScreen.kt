package io.github.gmathi.novellibrary.compose.ai_tts

import android.content.ComponentName
import android.content.Context
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.gmathi.novellibrary.compose.theme.NovelLibraryTheme
import io.github.gmathi.novellibrary.service.ai_tts.AiTtsPlaybackState
import io.github.gmathi.novellibrary.service.ai_tts.AiTtsService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTtsControlsScreen(
    novelTitle: String = "",
    chapterTitle: String = "",
    sentences: List<String> = emptyList(),
    playbackState: AiTtsPlaybackState = AiTtsPlaybackState.Idle,
    currentSentenceIndex: Int = 0,
    speechRate: Float = 1.0f,
    pitch: Float = 1.0f,
    autoNextChapter: Boolean = true,
    keepScreenOn: Boolean = false,
    onSpeechRateChange: (Float) -> Unit = {},
    onPitchChange: (Float) -> Unit = {},
    onAutoNextChapterChange: (Boolean) -> Unit = {},
    onKeepScreenOnChange: (Boolean) -> Unit = {},
    onPlayPause: () -> Unit = {},
    onStop: () -> Unit = {},
    onNextSentence: () -> Unit = {},
    onPrevSentence: () -> Unit = {},
    onNextChapter: () -> Unit = {},
    onPrevChapter: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    LaunchedEffect(currentSentenceIndex) {
        if (sentences.isNotEmpty() && currentSentenceIndex < sentences.size) {
            listState.animateScrollToItem(currentSentenceIndex)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Text(
                    text = "Quick Settings",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
                HorizontalDivider()

                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Speech Rate: ${"%.2f".format(speechRate)}x", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = speechRate,
                        onValueChange = onSpeechRateChange,
                        valueRange = 0.5f..2.0f,
                        steps = 29,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    Text("Pitch: ${"%.2f".format(pitch)}x", style = MaterialTheme.typography.bodyMedium)
                    Slider(
                        value = pitch,
                        onValueChange = onPitchChange,
                        valueRange = 0.5f..2.0f,
                        steps = 29,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Auto-read Next Chapter", modifier = Modifier.weight(1f))
                        Switch(checked = autoNextChapter, onCheckedChange = onAutoNextChapterChange)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Keep Screen On", modifier = Modifier.weight(1f))
                        Switch(checked = keepScreenOn, onCheckedChange = onKeepScreenOnChange)
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(novelTitle, style = MaterialTheme.typography.titleMedium)
                            if (chapterTitle.isNotEmpty()) {
                                Text(chapterTitle, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    }
                )
            },
            bottomBar = {
                PlaybackControlsBar(
                    playbackState = playbackState,
                    onPlayPause = onPlayPause,
                    onStop = onStop,
                    onNextSentence = onNextSentence,
                    onPrevSentence = onPrevSentence,
                    onNextChapter = onNextChapter,
                    onPrevChapter = onPrevChapter
                )
            }
        ) { paddingValues ->
            if (sentences.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    when (playbackState) {
                        is AiTtsPlaybackState.DownloadingModel -> Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        ) {
                            val progress = playbackState.progress
                            if (progress < 0) {
                                CircularProgressIndicator()
                            } else {
                                LinearProgressIndicator(
                                    progress = { progress / 100f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (progress < 0) "Downloading AI TTS model…"
                                else "Downloading AI TTS model… $progress%"
                            )
                        }
                        is AiTtsPlaybackState.LoadingModel -> CircularProgressIndicator()
                        is AiTtsPlaybackState.Error -> Text("Error: ${playbackState.message}")
                        else -> Text("No content loaded")
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(sentences) { index, sentence ->
                        val isActive = index == currentSentenceIndex
                        Text(
                            text = sentence,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                            ),
                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    else Color.Transparent,
                                    MaterialTheme.shapes.small
                                )
                                .padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PlaybackControlsBar(
    playbackState: AiTtsPlaybackState,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onNextSentence: () -> Unit,
    onPrevSentence: () -> Unit,
    onNextChapter: () -> Unit,
    onPrevChapter: () -> Unit
) {
    val isPlaying = playbackState is AiTtsPlaybackState.Playing
    val isLoading = playbackState is AiTtsPlaybackState.LoadingModel

    Surface(
        tonalElevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevChapter) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Previous Chapter")
            }
            IconButton(onClick = onPrevSentence) {
                Icon(Icons.Default.FastRewind, contentDescription = "Previous Sentence")
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(48.dp))
            } else {
                FilledIconButton(onClick = onPlayPause) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play"
                    )
                }
            }
            IconButton(onClick = onNextSentence) {
                Icon(Icons.Default.FastForward, contentDescription = "Next Sentence")
            }
            IconButton(onClick = onNextChapter) {
                Icon(Icons.Default.SkipNext, contentDescription = "Next Chapter")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AiTtsControlsScreenPreview() {
    NovelLibraryTheme {
        AiTtsControlsScreen(
            novelTitle = "Sample Novel",
            chapterTitle = "Chapter 1",
            sentences = listOf(
                "This is the first sentence of the chapter.",
                "This is the second sentence, which is currently being read aloud.",
                "And here is the third sentence of the chapter."
            ),
            playbackState = AiTtsPlaybackState.Playing,
            currentSentenceIndex = 1
        )
    }
}
