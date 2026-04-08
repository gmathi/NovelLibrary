package io.github.gmathi.novellibrary.activity

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.session.MediaControllerCompat
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.gmathi.novellibrary.compose.ai_tts.AiTtsControlsScreen
import io.github.gmathi.novellibrary.compose.theme.NovelLibraryTheme
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.service.ai_tts.AiTtsPlaybackState
import io.github.gmathi.novellibrary.service.ai_tts.AiTtsService
import io.github.gmathi.novellibrary.util.logging.Logs
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import uy.kohesive.injekt.injectLazy

class AiTtsControlsActivity : ComponentActivity() {

    private val dataCenter: DataCenter by injectLazy()

    // Mirror of the service player's state — updated by collecting the service flows
    private val sentences = MutableStateFlow<List<String>>(emptyList())
    private val playbackState = MutableStateFlow<AiTtsPlaybackState>(AiTtsPlaybackState.Idle)
    private val currentSentenceIndex = MutableStateFlow(0)

    private var mediaBrowser: MediaBrowserCompat? = null
    private var mediaController: MediaControllerCompat? = null
    private var flowsSynced = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val novelTitle = intent.getStringExtra(EXTRA_NOVEL_TITLE) ?: ""
        val chapterTitle = intent.getStringExtra(EXTRA_CHAPTER_TITLE) ?: ""

        // Apply keep-screen-on preference
        val prefs = dataCenter.aiTtsPreferences
        Logs.debug("AiTtsControlsActivity", "onCreate: novelTitle='$novelTitle' chapterTitle='$chapterTitle' keepScreenOn=${prefs.keepScreenOn}")
        if (prefs.keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }

        // Connect to MediaBrowserService for MediaController (playback commands)
        mediaBrowser = MediaBrowserCompat(
            this,
            ComponentName(this, AiTtsService::class.java),
            object : MediaBrowserCompat.ConnectionCallback() {
                override fun onConnected() {
                    val token = mediaBrowser?.sessionToken ?: return
                    mediaController = MediaControllerCompat(this@AiTtsControlsActivity, token)
                    MediaControllerCompat.setMediaController(this@AiTtsControlsActivity, mediaController)
                }
            },
            null
        ).also { it.connect() }

        // Sync local state flows from the service player whenever it is available
        syncServiceFlows()

        setContent {
            NovelLibraryTheme {
                val sentenceList by sentences.collectAsState()
                val state by playbackState.collectAsState()
                val sentenceIndex by currentSentenceIndex.collectAsState()

                AiTtsControlsScreen(
                    novelTitle = novelTitle,
                    chapterTitle = chapterTitle,
                    sentences = sentenceList,
                    playbackState = state,
                    currentSentenceIndex = sentenceIndex,
                    onPlayPause = {
                        val ctrl = mediaController
                        if (ctrl != null) {
                            if (state is AiTtsPlaybackState.Playing) ctrl.transportControls.pause()
                            else ctrl.transportControls.play()
                        } else {
                            AiTtsService.instance?.player?.let { p ->
                                if (p.playbackState.value is AiTtsPlaybackState.Playing) p.pause()
                                else p.start()
                            }
                        }
                    },
                    onStop = {
                        mediaController?.transportControls?.stop()
                            ?: AiTtsService.instance?.player?.stop()
                    },
                    onNextSentence = {
                        mediaController?.transportControls?.skipToNext()
                            ?: AiTtsService.instance?.player?.nextSentence()
                    },
                    onPrevSentence = {
                        mediaController?.transportControls?.skipToPrevious()
                            ?: AiTtsService.instance?.player?.prevSentence()
                    },
                    onNextChapter = { AiTtsService.instance?.player?.nextChapter() },
                    onPrevChapter = { AiTtsService.instance?.player?.prevChapter() },
                    onNavigateBack = { finish() }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        syncServiceFlows()
    }

    override fun onResume() {
        super.onResume()
        // Service may have started between onCreate and onResume; re-sync if flows are still empty
        if (sentences.value.isEmpty()) syncServiceFlows()
    }

    override fun onDestroy() {
        Logs.debug("AiTtsControlsActivity", "onDestroy: disconnecting media browser")
        mediaBrowser?.disconnect()
        super.onDestroy()
    }

    /**
     * Launches coroutines (tied to the Activity lifecycle) to mirror the service player's
     * StateFlows into local flows consumed by Compose. Retries if service not ready yet.
     */
    private fun syncServiceFlows() {
        if (flowsSynced) {
            val player = AiTtsService.instance?.player
            if (player != null) {
                Logs.debug("AiTtsControlsActivity", "syncServiceFlows: already synced, sentences=${player.sentences.value.size}")
            }
            return
        }

        val player = AiTtsService.instance?.player
        if (player == null) {
            Logs.debug("AiTtsControlsActivity", "syncServiceFlows: player is null, service instance=${AiTtsService.instance}, will retry...")
            // Retry after a short delay - the service may still be starting
            lifecycleScope.launch {
                kotlinx.coroutines.delay(500)
                if (!flowsSynced) syncServiceFlows()
            }
            return
        }

        flowsSynced = true
        Logs.debug("AiTtsControlsActivity", "syncServiceFlows: starting collection, sentences=${player.sentences.value.size}")

        lifecycleScope.launch {
            player.sentences.collect {
                Logs.debug("AiTtsControlsActivity", "sentences collected: ${it.size} items")
                sentences.value = it
            }
        }
        lifecycleScope.launch {
            player.playbackState.collect {
                Logs.debug("AiTtsControlsActivity", "playbackState collected: $it")
                playbackState.value = it
            }
        }
        lifecycleScope.launch {
            player.currentSentenceIndex.collect { currentSentenceIndex.value = it }
        }
    }

    companion object {
        const val EXTRA_NOVEL_TITLE = "extra_novel_title"
        const val EXTRA_CHAPTER_TITLE = "extra_chapter_title"

        fun createIntent(context: Context, novelTitle: String = "", chapterTitle: String = ""): Intent {
            return Intent(context, AiTtsControlsActivity::class.java).apply {
                putExtra(EXTRA_NOVEL_TITLE, novelTitle)
                putExtra(EXTRA_CHAPTER_TITLE, chapterTitle)
            }
        }
    }
}
