package io.github.gmathi.novellibrary.activity.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.afollestad.materialdialogs.MaterialDialog
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.activity.settings.reader.ReaderBackgroundSettingsActivity
import io.github.gmathi.novellibrary.compose.settings.AboutHelpSettingsScreen
import io.github.gmathi.novellibrary.compose.settings.AppearanceSettingsScreen
import io.github.gmathi.novellibrary.compose.settings.NetworkPrivacySettingsScreen
import io.github.gmathi.novellibrary.compose.settings.NewRootSettingsScreen
import io.github.gmathi.novellibrary.compose.settings.StorageBackupSettingsScreen
import io.github.gmathi.novellibrary.compose.settings.TextToSpeechSettingsScreen
import io.github.gmathi.novellibrary.compose.theme.NovelLibraryTheme
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.network.AppUpdateChecker
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.network.cloudflare.CloudflareCookieManager
import io.github.gmathi.novellibrary.service.sync.BackgroundNovelSyncTask
import io.github.gmathi.novellibrary.util.lang.LocaleManager
import io.github.gmathi.novellibrary.util.system.openInBrowser
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import uy.kohesive.injekt.injectLazy

/**
 * NEW Compose-only settings entry point implementing the restructuring plan.
 *
 * This is a parallel surface: the legacy [MainSettingsActivity] and every legacy
 * settings activity are left completely untouched and remain fully functional.
 * All toggles here read/write the SAME [DataCenter] keys the legacy screens use,
 * so no preference schema changes and no data migration are involved. Rows that
 * open detail screens launch the EXISTING activities.
 */
class NewSettingsActivity : ComponentActivity() {

    private val dataCenter: DataCenter by injectLazy()
    private val networkHelper: NetworkHelper by injectLazy()

    private enum class Screen { ROOT, APPEARANCE, NETWORK, STORAGE, TTS, ABOUT }

    // Hoisted state so onResume (returning from a sub-activity) can refresh it.
    private var nightMode by mutableStateOf(false)
    private var scrollingText by mutableStateOf(false)
    private var showChaptersLeftBadge by mutableStateOf(false)
    private var loadLibraryScreen by mutableStateOf(false)
    private var enableNotifications by mutableStateOf(false)
    private var dohProviderIndex by mutableStateOf(0)
    private var useAiTts by mutableStateOf(false)
    private var autoAppUpdate by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        refreshState()

        val dohNames = resources.getStringArray(R.array.dns_over_https_list).toList()

        setContent {
            NovelLibraryTheme {
                var screen by remember { mutableStateOf(Screen.ROOT) }

                // System Back pops a sub-screen to the root; from the root it exits the activity.
                androidx.activity.compose.BackHandler(enabled = screen != Screen.ROOT) {
                    screen = Screen.ROOT
                }

                when (screen) {
                    Screen.ROOT -> NewRootSettingsScreen(
                        onNavigateBack = { finish() },
                        onOpenAppearance = { screen = Screen.APPEARANCE },
                        onOpenReader = { startActivity(Intent(this, io.github.gmathi.novellibrary.activity.settings.reader.ReaderSettingsActivity::class.java)) },
                        onOpenTextToSpeech = { screen = Screen.TTS },
                        onOpenNetworkPrivacy = { screen = Screen.NETWORK },
                        onOpenStorageBackup = { screen = Screen.STORAGE },
                        onOpenSync = { startActivity(Intent(this, SyncSettingsSelectionActivity::class.java)) },
                        onOpenAboutHelp = { screen = Screen.ABOUT },
                        onSwitchToClassic = {
                            dataCenter.useNewSettingsUi = false
                            startActivity(Intent(this, MainSettingsActivity::class.java))
                            finish()
                        },
                    )

                    Screen.APPEARANCE -> AppearanceSettingsScreen(
                        nightMode = nightMode,
                        scrollingText = scrollingText,
                        showChaptersLeftBadge = showChaptersLeftBadge,
                        loadLibraryScreen = loadLibraryScreen,
                        enableNotifications = enableNotifications,
                        onNightModeChange = { value ->
                            nightMode = value
                            dataCenter.appNightMode = value
                            AppCompatDelegate.setDefaultNightMode(
                                if (value) AppCompatDelegate.MODE_NIGHT_YES
                                else AppCompatDelegate.MODE_NIGHT_NO
                            )
                        },
                        onScrollingTextChange = { value -> scrollingText = value; dataCenter.enableScrollingText = value },
                        onShowChaptersLeftBadgeChange = { value -> showChaptersLeftBadge = value; dataCenter.showChaptersLeftBadge = value },
                        onLoadLibraryScreenChange = { value -> loadLibraryScreen = value; dataCenter.loadLibraryScreen = value },
                        onEnableNotificationsChange = { value ->
                            enableNotifications = value
                            dataCenter.enableNotifications = value
                            if (value) BackgroundNovelSyncTask.scheduleRepeat(applicationContext)
                            else BackgroundNovelSyncTask.cancelAll(applicationContext)
                        },
                        onOpenReaderColors = { startActivity(Intent(this, ReaderBackgroundSettingsActivity::class.java)) },
                        onNavigateBack = { screen = Screen.ROOT },
                    )

                    Screen.NETWORK -> NetworkPrivacySettingsScreen(
                        dohProviderIndex = dohProviderIndex,
                        dohProviderNames = dohNames,
                        onDohProviderChange = { index -> dohProviderIndex = index; dataCenter.dohProvider = index },
                        onOpenForcedWebViewDomains = { startActivity(Intent(this, ForcedWebViewDomainsActivity::class.java)) },
                        onClearCloudflareCookies = { clearCloudflareCookies() },
                        onNavigateBack = { screen = Screen.ROOT },
                    )

                    Screen.STORAGE -> StorageBackupSettingsScreen(
                        onOpenDownloadStorage = { startActivity(Intent(this, StorageSettingsActivity::class.java)) },
                        onOpenBackupRestore = { startActivity(Intent(this, BackupRestoreActivity::class.java)) },
                        onNavigateBack = { screen = Screen.ROOT },
                    )

                    Screen.TTS -> TextToSpeechSettingsScreen(
                        useAiTts = useAiTts,
                        onUseAiTtsChange = { value -> useAiTts = value; dataCenter.useAiTts = value },
                        onOpenClassicTts = { startActivity(Intent(this, TTSSettingsActivity::class.java)) },
                        onOpenAiTts = { startActivity(Intent(this, AiTtsSettingsActivity::class.java)) },
                        onNavigateBack = { screen = Screen.ROOT },
                    )

                    Screen.ABOUT -> AboutHelpSettingsScreen(
                        autoAppUpdate = autoAppUpdate,
                        onAutoAppUpdateChange = { value -> autoAppUpdate = value; dataCenter.enableAutoAppUpdate = value },
                        onCheckForUpdates = { checkForUpdates() },
                        onReportBug = { reportBug() },
                        onDonate = { messageDialog(R.string.donate_developer, R.string.donations_description_new) },
                        onAboutUs = { messageDialog(R.string.about_us, R.string.about_us_content) },
                        onLanguagesSupported = { startActivity(Intent(this, LanguageActivity::class.java)) },
                        onLibrariesUsed = { startActivity(Intent(this, LibrariesUsedActivity::class.java)) },
                        onContributions = { startActivity(Intent(this, ContributionsActivity::class.java)) },
                        onCopyrightNotice = { startActivity(Intent(this, CopyrightActivity::class.java)) },
                        onWhatsChanged = { startActivity(Intent(this, WhatsChangedActivity::class.java)) },
                        onNavigateBack = { screen = Screen.ROOT },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshState()
    }

    private fun refreshState() {
        nightMode = dataCenter.appNightMode
        scrollingText = dataCenter.enableScrollingText
        showChaptersLeftBadge = dataCenter.showChaptersLeftBadge
        loadLibraryScreen = dataCenter.loadLibraryScreen
        enableNotifications = dataCenter.enableNotifications
        dohProviderIndex = dataCenter.dohProvider
        useAiTts = dataCenter.useAiTts
        autoAppUpdate = dataCenter.enableAutoAppUpdate
    }

    private fun clearCloudflareCookies() {
        networkHelper.cloudflareCookieManager.knownHosts().forEach { host ->
            "https://$host/".toHttpUrlOrNull()?.let { httpUrl ->
                networkHelper.cookieManager.remove(httpUrl, CloudflareCookieManager.CLOUDFLARE_COOKIE_NAMES, 0)
            }
        }
        networkHelper.cloudflareCookieManager.clearAllCookies()
        Toast.makeText(this, R.string.clear_cloudflare_cookies_success, Toast.LENGTH_SHORT).show()
    }

    private fun checkForUpdates() {
        Toast.makeText(this, "Checking for updates…", Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val checker = AppUpdateChecker(applicationContext)
            checker.checkAndPromptUpdate(force = true)
        }
    }

    private fun reportBug() {
        val systemInfo = systemInfo()
        fun copyDebugInfo() {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Debug-info", systemInfo))
            Toast.makeText(this, R.string.bug_report_copied_toast, Toast.LENGTH_LONG).show()
        }
        MaterialDialog(this).show {
            message(text = getString(R.string.bug_report_content, "\n\n" + systemInfo))
            positiveButton(R.string.bug_report_copy_and_post) {
                copyDebugInfo()
                openInBrowser(MainSettingsActivity.DISCORD_INVITE_URL)
                it.dismiss()
            }
            neutralButton(R.string.copy_to_clipboard) { copyDebugInfo() }
            negativeButton(R.string.close) { it.dismiss() }
        }
    }

    private fun systemInfo(): String {
        val dm = resources.displayMetrics
        return StringBuilder("Debug-info:")
            .append("\n\tApp Version: ").append(io.github.gmathi.novellibrary.BuildConfig.VERSION_NAME)
            .append('_').append(io.github.gmathi.novellibrary.BuildConfig.VERSION_CODE)
            .append("\n\tOS Version: ").append(System.getProperty("os.version"))
            .append('(').append(android.os.Build.VERSION.INCREMENTAL).append(')')
            .append("\n\tOS API Level: ").append(android.os.Build.VERSION.SDK_INT)
            .append("\n\tManufacturer: ").append(android.os.Build.MANUFACTURER)
            .append("\n\tDevice: ").append(android.os.Build.DEVICE)
            .append("\n\tModel (and Product): ").append(android.os.Build.MODEL)
            .append(" (").append(android.os.Build.PRODUCT).append(')')
            .append("\n\tDisplay: ").append(dm.widthPixels).append('x').append(dm.heightPixels)
            .toString()
    }

    private fun messageDialog(titleRes: Int, messageRes: Int) {
        MaterialDialog(this).show {
            title(titleRes)
            message(messageRes)
        }
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleManager.updateContextLocale(newBase))
    }
}
