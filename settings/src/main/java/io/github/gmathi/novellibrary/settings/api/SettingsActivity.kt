package io.github.gmathi.novellibrary.settings.api

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.rememberNavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import io.github.gmathi.novellibrary.settings.data.repository.SettingsRepositoryDataStore
import io.github.gmathi.novellibrary.settings.network.GoogleDriveHelper
import io.github.gmathi.novellibrary.settings.ui.navigation.SettingsNavGraph
import io.github.gmathi.novellibrary.stubs.theme.NovelLibraryBaseTheme
import io.github.gmathi.novellibrary.settings.viewmodel.AdvancedSettingsViewModel
import io.github.gmathi.novellibrary.settings.viewmodel.BackupSettingsViewModel
import io.github.gmathi.novellibrary.settings.viewmodel.GeneralSettingsViewModel
import io.github.gmathi.novellibrary.settings.viewmodel.MainSettingsViewModel
import io.github.gmathi.novellibrary.settings.viewmodel.ReaderSettingsViewModel
import io.github.gmathi.novellibrary.settings.viewmodel.SyncSettingsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Standalone Activity for settings.
 *
 * Handles Google Sign-In and backup info directly. For backup/restore execution
 * (which requires WorkManager workers from the app module), subclasses can override
 * [onExecuteGoogleDriveBackup] and [onExecuteGoogleDriveRestore].
 */
open class SettingsActivity : ComponentActivity() {

    private lateinit var driveHelper: GoogleDriveHelper
    protected lateinit var backupSettingsViewModel: BackupSettingsViewModel

    private var pendingAction: PendingAction? = null
    private var pendingOptions: BooleanArray? = null

    private enum class PendingAction { BACKUP, RESTORE }

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            backupSettingsViewModel.setGdAccountEmail(account.email ?: "-")

            when (pendingAction) {
                PendingAction.BACKUP -> pendingOptions?.let {
                    onExecuteGoogleDriveBackup(it[0], it[1], it[2], it[3])
                }
                PendingAction.RESTORE -> pendingOptions?.let {
                    onExecuteGoogleDriveRestore(it[0], it[1], it[2], it[3])
                }
                null -> {}
            }
        } catch (_: ApiException) {
            // Sign-in failed or was cancelled
        } finally {
            pendingAction = null
            pendingOptions = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        driveHelper = GoogleDriveHelper(this)

        val settingsRepository = SettingsRepositoryDataStore(applicationContext)
        val mainSettingsViewModel = MainSettingsViewModel(settingsRepository)
        val readerSettingsViewModel = ReaderSettingsViewModel(settingsRepository)
        val generalSettingsViewModel = GeneralSettingsViewModel(settingsRepository)
        backupSettingsViewModel = BackupSettingsViewModel(settingsRepository)
        val syncSettingsViewModel = SyncSettingsViewModel(settingsRepository)
        val advancedSettingsViewModel = AdvancedSettingsViewModel(settingsRepository)

        setContent {
            val isDarkTheme by generalSettingsViewModel.isDarkTheme.collectAsState()

            NovelLibraryBaseTheme(darkTheme = isDarkTheme) {
                SettingsActivityContent(
                    mainSettingsViewModel = mainSettingsViewModel,
                    readerSettingsViewModel = readerSettingsViewModel,
                    generalSettingsViewModel = generalSettingsViewModel,
                    backupSettingsViewModel = backupSettingsViewModel,
                    syncSettingsViewModel = syncSettingsViewModel,
                    advancedSettingsViewModel = advancedSettingsViewModel,
                    appVersionName = packageManager.getPackageInfo(packageName, 0).versionName ?: "Unknown",
                    appVersionCode = packageManager.getPackageInfo(packageName, 0).versionCode,
                    onNavigateBack = { finish() },
                    onNavigateToContributors = {
                        startActivity(Intent(this, Class.forName("io.github.gmathi.novellibrary.activity.settings.ContributionsActivity")))
                    },
                    onNavigateToCopyright = {
                        startActivity(Intent(this, Class.forName("io.github.gmathi.novellibrary.activity.settings.CopyrightActivity")))
                    },
                    onNavigateToLicenses = {
                        startActivity(Intent(this, Class.forName("io.github.gmathi.novellibrary.activity.settings.LibrariesUsedActivity")))
                    },
                    onOpenPrivacyPolicy = {
                        startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/gmathi/NovelLibrary/blob/master/Privacy-Policy")))
                    },
                    onOpenTermsOfService = {
                        startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://github.com/gmathi/NovelLibrary/blob/master/Privacy-Policy")))
                    },
                    onCheckForUpdates = {},
                    onCreateBackup = {
                        startActivity(Intent(this, Class.forName("io.github.gmathi.novellibrary.activity.settings.BackupSettingsActivity")))
                    },
                    onRestoreBackup = {
                        startActivity(Intent(this, Class.forName("io.github.gmathi.novellibrary.activity.settings.BackupSettingsActivity")))
                    },
                    onGoogleSignIn = {
                        signInLauncher.launch(driveHelper.getSignInClient().signInIntent)
                    },
                    onGoogleSignOut = {
                        driveHelper.getSignInClient().signOut().addOnCompleteListener {
                            backupSettingsViewModel.setGdAccountEmail("")
                        }
                    },
                    onGoogleDriveBackup = { s, d, p, f ->
                        if (!driveHelper.isSignedIn()) {
                            pendingAction = PendingAction.BACKUP
                            pendingOptions = booleanArrayOf(s, d, p, f)
                            signInLauncher.launch(driveHelper.getSignInClient().signInIntent)
                        } else {
                            onExecuteGoogleDriveBackup(s, d, p, f)
                        }
                    },
                    onGoogleDriveRestore = { s, d, p, f ->
                        if (!driveHelper.isSignedIn()) {
                            pendingAction = PendingAction.RESTORE
                            pendingOptions = booleanArrayOf(s, d, p, f)
                            signInLauncher.launch(driveHelper.getSignInClient().signInIntent)
                        } else {
                            onExecuteGoogleDriveRestore(s, d, p, f)
                        }
                    },
                    onRefreshBackupInfo = { refreshBackupInfo() },
                    onSyncLogin = {
                        try {
                            startActivity(Intent(this, Class.forName("io.github.gmathi.novellibrary.activity.settings.SyncSettingsActivity")))
                        } catch (_: ClassNotFoundException) { }
                    },
                    onClearCache = {
                        onCacheClearRequested()
                    },
                    onResetSettings = {
                        onResetSettingsRequested()
                    },
                    onCloudflareBypass = {
                        try {
                            startActivity(Intent(this, Class.forName("io.github.gmathi.novellibrary.activity.settings.CloudFlareBypassActivity")))
                        } catch (_: ClassNotFoundException) { }
                    }
                )
            }
        }
    }

    private fun refreshBackupInfo() {
        if (!driveHelper.isSignedIn()) return
        CoroutineScope(Dispatchers.IO).launch {
            val result = driveHelper.getBackupInfo()
            withContext(Dispatchers.Main) {
                val info = result.getOrNull()
                if (info != null) {
                    backupSettingsViewModel.setLastCloudBackupTimestamp(
                        "${info.getFormattedTime()} • ${info.getFormattedSize()}"
                    )
                }
            }
        }
    }

    /**
     * Called to execute a Google Drive backup with the selected options.
     * Override in app module subclass to enqueue the WorkManager backup worker.
     */
    protected open fun onExecuteGoogleDriveBackup(
        simpleText: Boolean, database: Boolean, preferences: Boolean, files: Boolean
    ) {}

    /**
     * Called to execute a Google Drive restore with the selected options.
     * Override in app module subclass to enqueue the WorkManager restore worker.
     */
    protected open fun onExecuteGoogleDriveRestore(
        simpleText: Boolean, database: Boolean, preferences: Boolean, files: Boolean
    ) {}

    /**
     * Called when the user requests to clear the app cache.
     * Override in app module subclass to perform actual cache clearing.
     */
    protected open fun onCacheClearRequested() {
        cacheDir.deleteRecursively()
    }

    /**
     * Called when the user requests to reset all settings to defaults.
     * Override in app module subclass to perform actual settings reset.
     */
    protected open fun onResetSettingsRequested() {}
}

@Composable
private fun SettingsActivityContent(
    mainSettingsViewModel: MainSettingsViewModel,
    readerSettingsViewModel: ReaderSettingsViewModel,
    generalSettingsViewModel: GeneralSettingsViewModel,
    backupSettingsViewModel: BackupSettingsViewModel,
    syncSettingsViewModel: SyncSettingsViewModel,
    advancedSettingsViewModel: AdvancedSettingsViewModel,
    appVersionName: String,
    appVersionCode: Int,
    onNavigateBack: () -> Unit,
    onNavigateToContributors: () -> Unit,
    onNavigateToCopyright: () -> Unit,
    onNavigateToLicenses: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onOpenTermsOfService: () -> Unit,
    onCheckForUpdates: () -> Unit,
    onCreateBackup: () -> Unit = {},
    onRestoreBackup: () -> Unit = {},
    onGoogleSignIn: () -> Unit = {},
    onGoogleSignOut: () -> Unit = {},
    onGoogleDriveBackup: (Boolean, Boolean, Boolean, Boolean) -> Unit = { _, _, _, _ -> },
    onGoogleDriveRestore: (Boolean, Boolean, Boolean, Boolean) -> Unit = { _, _, _, _ -> },
    onRefreshBackupInfo: () -> Unit = {},
    onSyncLogin: () -> Unit = {},
    onClearCache: () -> Unit = {},
    onResetSettings: () -> Unit = {},
    onCloudflareBypass: () -> Unit = {}
) {
    val navController = rememberNavController()

    SettingsNavGraph(
        mainSettingsViewModel = mainSettingsViewModel,
        readerSettingsViewModel = readerSettingsViewModel,
        generalSettingsViewModel = generalSettingsViewModel,
        backupSettingsViewModel = backupSettingsViewModel,
        syncSettingsViewModel = syncSettingsViewModel,
        advancedSettingsViewModel = advancedSettingsViewModel,
        appVersionName = appVersionName,
        appVersionCode = appVersionCode,
        navController = navController,
        onNavigateBack = onNavigateBack,
        onNavigateToContributors = onNavigateToContributors,
        onNavigateToCopyright = onNavigateToCopyright,
        onNavigateToLicenses = onNavigateToLicenses,
        onOpenPrivacyPolicy = onOpenPrivacyPolicy,
        onOpenTermsOfService = onOpenTermsOfService,
        onCheckForUpdates = onCheckForUpdates,
        onCreateBackup = onCreateBackup,
        onRestoreBackup = onRestoreBackup,
        onGoogleSignIn = onGoogleSignIn,
        onGoogleSignOut = onGoogleSignOut,
        onGoogleDriveBackup = onGoogleDriveBackup,
        onGoogleDriveRestore = onGoogleDriveRestore,
        onRefreshBackupInfo = onRefreshBackupInfo,
        onSyncLogin = onSyncLogin,
        onClearCache = onClearCache,
        onResetSettings = onResetSettings,
        onCloudflareBypass = onCloudflareBypass
    )
}
