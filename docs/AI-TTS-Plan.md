Context
NovelLibrary has an existing native-Android TTS implementation (TTSService / TTSPlayer / TTSWrapper) that uses android.speech.tts.TextToSpeech. The request is to add a parallel AI TTS engine (on-device neural synthesis via SherpaOnnx + Piper ONNX models) selectable by a feature flag, without modifying the existing TTS code at all.
On first use, the model (~63 MB) is downloaded and cached. The feature flag in settings controls which TTS engine starts when the reader's "Read Aloud" menu item is tapped.

Engine Choice: SherpaOnnx + Piper

Library: com.github.k2-fsa:sherpa-onnx-android (Maven / JitPack)
Models: Piper ONNX voices (e.g. en_US-ryan-high, en_US-amy-medium, en_GB-alan-medium)
Why: Fully offline, high-quality neural TTS, multiple voices, well-maintained Android AAR, clear download mechanism
Model storage: context.filesDir/ai_tts/models/<voiceId>/model.onnx + model.onnx.json
Default model CDN: GitHub Releases on k2-fsa/sherpa-onnx or Hugging Face mirrors


Files to Create / Modify
New Files
FilePurposemodel/preference/AiTtsPreferences.ktSharedPrefs wrapper for AI TTS settings (voice, speed, pitch, etc.)service/ai_tts/AiTtsModelManager.ktDownload, verify, load/unload Piper model filesservice/ai_tts/AiTtsPlayer.ktCoroutine-based playback: SherpaOnnx inference + AudioTrack outputservice/ai_tts/AiTtsService.ktMediaBrowserServiceCompat foreground service (mirrors TTSService structure)service/ai_tts/AiTtsNotificationBuilder.ktMedia-style playback notificationservice/ai_tts/AiTtsEventListener.ktCallback interface for service → UI eventsworker/AiTtsModelDownloadWorker.ktCoroutineWorker for model download with progress notificationsactivity/AiTtsControlsActivity.ktThin ComponentActivity — enableEdgeToEdge() + setContent { NovelLibraryTheme { AiTtsControlsScreen() } }activity/settings/AiTtsSettingsActivity.ktThin ComponentActivity — enableEdgeToEdge() + setContent { NovelLibraryTheme { AiTtsSettingsScreen() } }compose/ai_tts/AiTtsControlsScreen.ktFull Compose UI: ModalNavigationDrawer + Scaffold + TopAppBar + playback controls barcompose/ai_tts/AiTtsSettingsScreen.ktFull Compose UI: Scaffold + TopAppBar + LazyColumn settings rows
Modified Files
FileChangegradle/libs.versions.tomlAdd sherpa-onnx version entryapp/build.gradleAdd sherpa-onnx dependency + JitPack repo if neededmodel/preference/DataCenter.ktAdd useAiTts: Boolean property (key "useAiTts", default false) and val aiTtsPreferences instanceutil/notification/Notifications.ktAdd CHANNEL_AI_TTS_PLAYBACK, CHANNEL_AI_TTS_DOWNLOAD, ID_AI_TTS_PLAYBACK, ID_AI_TTS_DOWNLOADNovelLibraryApplication.ktNo change needed — existing Notifications.createChannels() picks up new entries automaticallyutil/system/StartIntentExt.ktAdd startAiTtsService(...) and startAiTtsActivity() extension functionsactivity/ReaderDBPagerActivity.ktIn onNavigationItemSelected(R.id.title_read_aloud): check dataCenter.useAiTts, branch to AI or legacy TTSactivity/settings/GeneralSettingsActivity.ktAdd POSITION_USE_AI_TTS row (switch) + update string arraysAndroidManifest.xmlRegister AiTtsService and AiTtsControlsActivity and AiTtsSettingsActivity

Detailed Design
1. AiTtsPreferences.kt
kotlindata class AiTtsPreferences(val context: Context, val prefs: SharedPreferences) {
    var voiceId: String           // key "aiTtsVoiceId", default "en_US-ryan-high"
    var speechRate: Float         // key "aiTtsSpeechRate", range 0.5–2.0, default 1.0
    var pitch: Float              // key "aiTtsPitch", range 0.5–2.0, default 1.0
    var volumeNormalization: Boolean  // key "aiTtsVolumeNorm", default true
    var autoReadNextChapter: Boolean  // key "aiTtsAutoNext", default true
    var keepScreenOn: Boolean     // key "aiTtsKeepScreenOn", default false
}
Add val aiTtsPreferences = AiTtsPreferences(context, prefs) to DataCenter.
2. AiTtsModelManager.kt
kotlinclass AiTtsModelManager(private val context: Context) {
    // Model directory: context.filesDir/ai_tts/models/<voiceId>/
    fun isModelDownloaded(voiceId: String): Boolean
    fun getModelDir(voiceId: String): File
    fun deleteModel(voiceId: String)
    fun loadModel(voiceId: String): OfflineTts  // SherpaOnnx object
    fun unloadModel()
    fun availableVoices(): List<AiTtsVoiceInfo>  // bundled metadata
    
    // Called by AiTtsModelDownloadWorker to verify integrity
    fun verifyModel(voiceId: String): Boolean  // checksum or file existence check
}

data class AiTtsVoiceInfo(val id: String, val name: String, val language: String, val sizeBytes: Long, val downloadUrl: String, val checksumMd5: String)
3. AiTtsModelDownloadWorker.kt
kotlinclass AiTtsModelDownloadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    // Input: KEY_VOICE_ID (String)
    // Output: KEY_RESULT (success/failure String)
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        // Use ProgressNotificationManager for download progress
        // Download model.onnx + model.onnx.json to temp dir
        // Verify integrity (file size / MD5)
        // Move to final location atomically
        // Post completion notification
        // On failure: clean up temp, return Result.failure()
    }
    
    companion object {
        const val KEY_VOICE_ID = "voiceId"
        
        fun enqueue(context: Context, voiceId: String): UUID {
            val request = OneTimeWorkRequestBuilder<AiTtsModelDownloadWorker>()
                .setInputData(workDataOf(KEY_VOICE_ID to voiceId))
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork("ai_tts_download_$voiceId", ExistingWorkPolicy.KEEP, request)
            return request.id
        }
    }
}
4. AiTtsPlayer.kt
kotlinclass AiTtsPlayer(
    private val context: Context,
    private val preferences: AiTtsPreferences,
    private val modelManager: AiTtsModelManager,
    private val listener: AiTtsEventListener
) {
    // State
    val playbackState: StateFlow<AiTtsPlaybackState>  // IDLE, LOADING, PLAYING, PAUSED, STOPPED, ERROR
    val currentSentenceIndex: StateFlow<Int>
    val sentences: List<String>
    
    // Coroutine scope tied to lifecycle
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // SherpaOnnx TTS (loaded lazily per voice)
    private var offlineTts: OfflineTts? = null
    
    // AudioTrack for PCM playback on dedicated thread
    private var audioTrack: AudioTrack? = null
    private val audioThread = HandlerThread("ai_tts_audio", Process.THREAD_PRIORITY_AUDIO)
    
    fun setData(text: String, title: String, linkedPages: ArrayList<LinkedPage>, chapterIndex: Int)
    fun start()
    fun pause()
    fun stop()
    fun nextSentence()
    fun prevSentence()
    fun nextChapter()
    fun prevChapter()
    fun destroy()
    
    // Private
    private suspend fun synthesizeAndPlay(sentence: String)  // Dispatchers.IO inference → audio thread
    private fun splitIntoSentences(text: String): List<String>
    private fun initAudioTrack(sampleRate: Int)
}

sealed class AiTtsPlaybackState {
    object Idle : AiTtsPlaybackState()
    object LoadingModel : AiTtsPlaybackState()
    data class Playing(val sentence: Int, val total: Int) : AiTtsPlaybackState()
    object Paused : AiTtsPlaybackState()
    object Stopped : AiTtsPlaybackState()
    data class Error(val message: String) : AiTtsPlaybackState()
}
5. AiTtsService.kt
kotlinclass AiTtsService : MediaBrowserServiceCompat(), AudioManager.OnAudioFocusChangeListener {
    companion object {
        const val ACTION_STARTUP = "ai_tts_startup"
        const val ACTION_PLAY_PAUSE = "ai_tts_play_pause"
        const val ACTION_STOP = "ai_tts_stop"
        const val ACTION_NEXT = "ai_tts_next"
        const val ACTION_PREVIOUS = "ai_tts_previous"
        const val ACTION_OPEN_CONTROLS = "ai_tts_open_controls"
        const val AUDIO_TEXT_KEY = "audioTextKey"
        const val TITLE = "title"
        const val NOVEL_ID = "novelId"
        const val LINKED_PAGES = "linkedPages"
        const val CHAPTER_INDEX = "chapterIndex"
        const val TRANSLATOR_SOURCE_NAME = "translatorSourceName"
    }
    
    private lateinit var player: AiTtsPlayer
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var notificationBuilder: AiTtsNotificationBuilder
    private var audioFocusRequest: AudioFocusRequest? = null  // API 26+
    private val noisyReceiver = NoisyReceiver()
    private var isForeground = false
    
    override fun onStartCommand(intent: Intent?, ...): Int  // dispatch actions
    private fun actionStartup(extras: Bundle)
    private fun hookSystem(): Boolean  // audio focus + startForeground
    private fun unhookSystem()         // abandon focus + stopForeground + stopSelf
    override fun onAudioFocusChange(focusChange: Int)
    
    // MediaSession callback
    inner class AiTtsSessionCallback : MediaSessionCompat.Callback()
    
    // Notification controller observes player.playbackState
    inner class NotificationController
    
    // Headphone disconnect
    private inner class NoisyReceiver : BroadcastReceiver()
}
6. AiTtsNotificationBuilder.kt
Media-style notification matching the existing TTSNotificationBuilder pattern:

Channel: "io.github.gmathi.novellibrary.ai_tts" (add to Notifications.kt)
NotificationID: from Utils.getUniqueNotificationId()
Actions: Previous, Play/Pause, Next, Open Controls
Uses MediaSessionCompat.Token

7. AiTtsControlsActivity.kt + compose/ai_tts/AiTtsControlsScreen.kt
Activity — pure Compose, ComponentActivity:
kotlinclass AiTtsControlsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()                        // edge-to-edge content
        setContent {
            NovelLibraryTheme {                   // auto-sets statusBarColor + light/dark bar icons
                AiTtsControlsScreen(
                    onSettingsClick = { startActivity<AiTtsSettingsActivity>() },
                    onBackClick    = { finish() }
                )
            }
        }
    }
}
Screen composable — compose/ai_tts/AiTtsControlsScreen.kt:
ModalNavigationDrawer (drawerContent = QuickSettingsPanel) {
  Scaffold(
    topBar = TopAppBar(
      title       = novel title + chapter name,
      navigationIcon = back arrow,
      actions     = settings icon → AiTtsSettingsActivity
    ),
    bottomBar = PlaybackControlsBar (prev-chapter / prev-sentence / play-pause / next-sentence / next-chapter)
  ) { paddingValues ->
    Column(Modifier.padding(paddingValues)) {
      SentenceListView(sentences, currentIndex)   // LazyColumn, auto-scrolls to active
    }
  }
}

QuickSettingsPanel composable: speed Slider (0.5–2.0), pitch Slider, auto-next Switch, keep-screen-on Switch
Observes AiTtsPlayer.playbackState: StateFlow<AiTtsPlaybackState> and currentSentenceIndex: StateFlow<Int>
Connects to AiTtsService via MediaBrowserCompat; transport commands go through MediaControllerCompat
NovelLibraryTheme SideEffect sets window.statusBarColor = colorScheme.primary.toArgb() and isAppearanceLightStatusBars = !isDark — no manual window manipulation needed

8. AiTtsSettingsActivity.kt + compose/ai_tts/AiTtsSettingsScreen.kt
Activity — pure Compose, ComponentActivity:
kotlinclass AiTtsSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NovelLibraryTheme {
                AiTtsSettingsScreen(onBackClick = { finish() })
            }
        }
    }
}
Screen composable — compose/ai_tts/AiTtsSettingsScreen.kt:
Scaffold(
  topBar = TopAppBar(title="AI TTS Settings", navigationIcon=back arrow)
) { paddingValues ->
  LazyColumn(Modifier.padding(paddingValues)) {

    // ── Playback section header ──────────────────────────────────
    item { SectionHeader("Playback") }
    item { SliderSettingRow("Speech Rate", value, 0.5f, 2.0f, "%") }
    item { SliderSettingRow("Pitch", value, 0.5f, 2.0f, "%") }
    item { SwitchSettingRow("Auto-read next chapter", checked) }
    item { SwitchSettingRow("Keep screen on", checked) }
    item { SwitchSettingRow("Volume normalization", checked) }

    // ── Voice / Model section header ─────────────────────────────
    item { SectionHeader("Voice / Model") }
    item { ChevronSettingRow("Active voice", currentVoiceName) }  // opens VoicePickerDialog
    item { ChevronSettingRow("Download additional voices") }
    item { ChevronSettingRow("Manage downloaded models") }   // shows delete dialog
  }
}
Private composables SliderSettingRow, SwitchSettingRow, SectionHeader, ChevronSettingRow live in the same file — no separate utility file needed (single use).
9. ReaderDBPagerActivity.kt modification
kotlinR.id.title_read_aloud -> {
    if (dataCenter.readerMode) {
        val webPageDBFragment = ...
        val audioText = webPageDBFragment?.doc?.getFormattedText() ?: return true
        val title = webPageDBFragment.doc?.title() ?: ""
        val chapterIndex = ...
        
        if (dataCenter.useAiTts) {
            startAiTtsService(audioText, webPageDBFragment.linkedPages, title, novel.id, translatorSourceName, chapterIndex)
            firebaseAnalytics.logNovelEvent(FAC.Event.LISTEN_NOVEL, novel)
            startAiTtsActivity()
        } else {
            startTTSService(audioText, webPageDBFragment.linkedPages, title, novel.id, translatorSourceName, chapterIndex)
            firebaseAnalytics.logNovelEvent(FAC.Event.LISTEN_NOVEL, novel)
            startTTSActivity()
        }
    } else {
        showAlertDialog(title = "Read Aloud", message = "Only supported in Reader Mode!")
    }
}
10. First-Use Download Flow
When startAiTtsService(...) is called:

AiTtsService.actionStartup() calls modelManager.isModelDownloaded(voiceId)
If downloaded: load model, start playback immediately
If not downloaded: post a sticky foreground notification "Downloading AI voice model (~63 MB)…", enqueue AiTtsModelDownloadWorker
Worker downloads with ProgressNotificationManager (progress notification)
On success: worker sends broadcast AiTtsService.ACTION_MODEL_READY → service loads model → starts playback
On failure: show error notification with Retry action, service stops

If the Activity is in foreground when called, show a MaterialDialog confirmation before downloading (uses WorkManager to keep download alive if user leaves).

Threading Model
OperationThread / DispatcherUI updates (playback state, sentences)Dispatchers.Main via StateFlowSherpaOnnx inference (offlineTts.generate())Dispatchers.IO (CPU-bound but use IO to avoid main blocking)AudioTrack write loopDedicated HandlerThread with THREAD_PRIORITY_AUDIOModel file I/O (load, verify)Dispatchers.IOModel download (WorkManager)Dispatchers.IO inside CoroutineWorker.doWork()MediaSession callbacksMain thread (MediaSession delivers on binder thread, dispatched to Main)Service coroutine scopeCoroutineScope(SupervisorJob() + Dispatchers.Main), cancelled in onDestroy()

Notification Channels (add to Notifications.kt)
kotlinconst val CHANNEL_AI_TTS_PLAYBACK = "ai_tts_playback_channel"
const val ID_AI_TTS_PLAYBACK = -701       // used by AiTtsNotificationBuilder

const val CHANNEL_AI_TTS_DOWNLOAD = "ai_tts_download_channel"
const val ID_AI_TTS_DOWNLOAD = -702       // used by AiTtsModelDownloadWorker
Both with IMPORTANCE_LOW, no sound, no vibration.

AndroidManifest.xml Additions
xml<service
    android:name=".service.ai_tts.AiTtsService"
    android:enabled="true"
    android:foregroundServiceType="mediaPlayback"
    android:exported="false">
    <intent-filter>
        <action android:name="android.media.browse.MediaBrowserService" />
    </intent-filter>
</service>

<activity
    android:name=".activity.AiTtsControlsActivity"
    android:screenOrientation="fullUser"
    android:theme="@style/AppTheme.NoActionBar" />
    <!-- NoActionBar: Compose Scaffold provides its own TopAppBar.
         NovelLibraryTheme SideEffect sets statusBarColor + light/dark icons automatically. -->

<activity
    android:name=".activity.settings.AiTtsSettingsActivity"
    android:label="@string/title_activity_ai_tts_settings"
    android:screenOrientation="fullUser"
    android:theme="@style/AppTheme.NoActionBar" />

Dependencies (libs.versions.toml + app/build.gradle)
toml# libs.versions.toml [versions]
sherpaOnnx = "1.10.40"   # latest stable

# [libraries]
sherpa-onnx-android = { group = "com.github.k2-fsa", name = "sherpa-onnx-android", version.ref = "sherpaOnnx" }
groovy// app/build.gradle repositories (settings.gradle)
maven { url "https://jitpack.io" }

// app/build.gradle dependencies
implementation libs.sherpa.onnx.android

Note: If JitPack is unavailable, the sherpa-onnx pre-built AAR can be placed in app/libs/ and referenced as implementation files('libs/sherpa-onnx-android-arm64-v8a-VERSION.aar').


GeneralSettingsActivity Feature Flag
Add one item to the settings list:
kotlinprivate const val POSITION_USE_AI_TTS = 8  // after existing items

// In bind():
POSITION_USE_AI_TTS -> {
    itemBinding.widgetSwitch.visibility = View.VISIBLE
    itemBinding.widgetSwitch.isChecked = dataCenter.useAiTts
    itemBinding.widgetSwitch.setOnCheckedChangeListener { _, value -> dataCenter.useAiTts = value }
}
Also add corresponding string resources in general_titles_list and general_subtitles_list arrays.
Add a navigation to AiTtsSettingsActivity from the TTS section in MainSettingsActivity (or create a link from GeneralSettingsActivity via chevron).

Implementation Sequence & Code Review Checkpoints
Phase 1 — Foundation

Add dependency (libs.versions.toml, build.gradle, settings.gradle)
Add Notifications.kt channels
Create AiTtsPreferences.kt
Add useAiTts flag + aiTtsPreferences to DataCenter.kt
Create AiTtsModelManager.kt
Create AiTtsModelDownloadWorker.kt
Register worker in AndroidManifest.xml (only services/activities needed, WorkManager auto-discovers)

[CODE REVIEW CHECKPOINT 1]: Review dependency setup, ModelManager, and Download Worker. Verify: correct threading, proper coroutine scoping, model file paths, progress notification pattern matches BackupWorker, error handling, retry logic.
Phase 2 — Core Playback Service

Create AiTtsEventListener.kt
Create AiTtsPlayer.kt (inference + AudioTrack)
Create AiTtsNotificationBuilder.kt
Create AiTtsService.kt (MediaBrowserServiceCompat, audio focus, foreground service)
Register AiTtsService in AndroidManifest.xml

[CODE REVIEW CHECKPOINT 2]: Review service lifecycle (hookSystem/unhookSystem), audio focus handling (API 26+ AudioFocusRequest + legacy fallback), headphone disconnect receiver, coroutine scope cancellation in onDestroy, sentence-by-sentence playback, AudioTrack setup, MediaSession integration.
Phase 3 — UI

Create compose/ai_tts/AiTtsControlsScreen.kt (Compose screen with ModalNavigationDrawer + Scaffold)
Create activity/AiTtsControlsActivity.kt (thin ComponentActivity host)
Create compose/ai_tts/AiTtsSettingsScreen.kt (Compose screen with LazyColumn settings rows)
Create activity/settings/AiTtsSettingsActivity.kt (thin ComponentActivity host)
Add startAiTtsService() and startAiTtsActivity() to StartIntentExt.kt
Register activities in AndroidManifest.xml
Register both activities in AndroidManifest.xml
Add AI TTS settings navigation (from GeneralSettingsActivity or MainSettingsActivity)

[CODE REVIEW CHECKPOINT 3]: Review Compose screens and activity hosts. Verify:

Both activities use ComponentActivity + enableEdgeToEdge() + NovelLibraryTheme
NovelLibraryTheme SideEffect correctly controls statusBarColor and isAppearanceLightStatusBars — no manual window manipulation in activities
Scaffold padding is passed down (paddingValues) so content clears status bar and nav bar
ModalNavigationDrawer opens/closes correctly for quick settings
TopAppBar back arrow and settings icon navigate correctly
MediaBrowserCompat connected/disconnected aligned with Activity.onStart()/onStop()
Voice picker populates from AiTtsModelManager.availableVoices()
Sliders write back to AiTtsPreferences immediately (no save button needed)
Dark/light switching (via dataCenter.isDarkTheme) reflects correctly in both screens

Phase 4 — Integration

Add POSITION_USE_AI_TTS toggle in GeneralSettingsActivity
Modify ReaderDBPagerActivity.onNavigationItemSelected to branch on dataCenter.useAiTts
Add string resources for all new UI strings
End-to-end smoke test path

[CODE REVIEW CHECKPOINT 4]: Final integration review. Verify: feature flag correctly routes to new vs. old service, no regressions in existing TTS path, strings present, manifest complete, no memory leaks (scope cancellation), ProGuard/R8 rules for SherpaOnnx if needed.

Key Existing Utilities to Reuse
UtilityLocationUsed ForProgressNotificationManagerutil/view/ProgressNotificationManager.ktModel download progress notificationUtils.getUniqueNotificationId()util/Utils.ktNotification ID generationDataCenter.ttsPreferences patternmodel/preference/TTSPreferences.ktAiTtsPreferences follows same getter/setter patternBaseSettingsActivity<V, T>activity/settings/BaseSettingsActivity.ktAiTtsSettingsActivity extends thisinjectLazy<T>()InjektDI in Worker and ServiceNotificationManagerCompat permission checkDownloadNovelService.kt:284All notification posts check POST_NOTIFICATIONSServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACKTTSService.ktSame foreground type for AiTtsServiceMediaButtonReceiverTTSService.ktMedia button handling in AiTtsService

Constraints Summary

Min SDK 23: use AudioFocusRequest only on API 26+, with legacy requestAudioFocus() fallback
Kotlin Coroutines only (no RxJava)
Injekt DI (no Hilt)
No Room (no changes to DB layer needed)
POST_NOTIFICATIONS permission check before every notification post
Existing TTSService and all related files are read-only — no changes permitted