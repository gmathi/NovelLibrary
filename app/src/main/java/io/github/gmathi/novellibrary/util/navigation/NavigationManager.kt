package io.github.gmathi.novellibrary.util.navigation

import androidx.navigation.NavController
import androidx.navigation.NavOptions
import dagger.hilt.android.scopes.ActivityScoped
import io.github.gmathi.novellibrary.R
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NavigationManager @Inject constructor() {

    /**
     * Navigate to the Library fragment
     */
    fun navigateToLibrary(navController: NavController) {
        navController.navigate(
            R.id.libraryFragment,
            null,
            getNavOptions()
        )
    }

    /**
     * Navigate to the Search fragment
     */
    fun navigateToSearch(navController: NavController) {
        navController.navigate(
            R.id.searchFragment,
            null,
            getNavOptions()
        )
    }

    /**
     * Navigate to the Extensions fragment
     */
    fun navigateToExtensions(navController: NavController) {
        navController.navigate(
            R.id.extensionsFragment,
            null,
            getNavOptions()
        )
    }

    /**
     * Navigate to the Downloads fragment
     */
    fun navigateToDownloads(navController: NavController) {
        navController.navigate(
            R.id.downloadsFragment,
            null,
            getNavOptions()
        )
    }

    /**
     * Navigate to the Settings fragment
     */
    fun navigateToSettings(navController: NavController) {
        navController.navigate(
            R.id.mainSettingsFragment,
            null,
            getNavOptions()
        )
    }

    /**
     * Navigate to Novel Details from Library
     */
    fun navigateToNovelDetailsFromLibrary(navController: NavController, novelId: Long) {
        val bundle = android.os.Bundle().apply {
            putLong("novelId", novelId)
        }
        navController.navigate(R.id.action_library_to_novel_details, bundle)
    }

    /**
     * Navigate to Novel Details from Search
     */
    fun navigateToNovelDetailsFromSearch(navController: NavController, novelId: Long) {
        val bundle = android.os.Bundle().apply {
            putLong("novelId", novelId)
        }
        navController.navigate(R.id.action_search_to_novel_details, bundle)
    }

    /**
     * Navigate to Novel Details from Downloads
     */
    fun navigateToNovelDetailsFromDownloads(navController: NavController, novelId: Long) {
        val bundle = android.os.Bundle().apply {
            putLong("novelId", novelId)
        }
        navController.navigate(R.id.action_downloads_to_novel_details, bundle)
    }

    /**
     * Navigate to Chapters
     */
    fun navigateToChapters(navController: NavController, novelId: Long) {
        val bundle = android.os.Bundle().apply {
            putLong("novelId", novelId)
        }
        navController.navigate(R.id.action_novel_details_to_chapters, bundle)
    }

    /**
     * Navigate to Reader from Novel Details
     */
    fun navigateToReaderFromNovelDetails(navController: NavController, novelId: Long, chapterId: Long = -1L, translatorSourceName: String? = null) {
        try {
            val action = io.github.gmathi.novellibrary.fragment.NovelDetailsFragmentDirections
                .actionNovelDetailsToReader(novelId, chapterId, translatorSourceName)
            navController.navigate(action)
        } catch (e: Exception) {
            // Fallback to manual navigation if Safe Args fails
            val bundle = android.os.Bundle().apply {
                putLong("novelId", novelId)
                putLong("chapterId", chapterId)
                putString("translatorSourceName", translatorSourceName)
            }
            navController.navigate(R.id.action_novel_details_to_reader, bundle)
        }
    }

    /**
     * Navigate to Reader from Chapters
     */
    fun navigateToReaderFromChapters(navController: NavController, novelId: Long, chapterId: Long = -1L, translatorSourceName: String? = null) {
        try {
            val action = io.github.gmathi.novellibrary.fragment.ChaptersMainFragmentDirections
                .actionChaptersToReader(novelId, chapterId, translatorSourceName)
            navController.navigate(action)
        } catch (e: Exception) {
            // Fallback to manual navigation if Safe Args fails
            val bundle = android.os.Bundle().apply {
                putLong("novelId", novelId)
                putLong("chapterId", chapterId)
                putString("translatorSourceName", translatorSourceName)
            }
            navController.navigate(R.id.action_chapters_to_reader, bundle)
        }
    }

    /**
     * Navigate to Reader from Downloads
     */
    fun navigateToReaderFromDownloads(navController: NavController, novelId: Long, chapterId: Long = -1L, translatorSourceName: String? = null) {
        // Use manual navigation for now since DownloadsFragment might not have Safe Args generated yet
        val bundle = android.os.Bundle().apply {
            putLong("novelId", novelId)
            putLong("chapterId", chapterId)
            putString("translatorSourceName", translatorSourceName)
        }
        navController.navigate(R.id.action_downloads_to_reader, bundle)
    }

    /**
     * Navigate back in the navigation stack
     */
    fun navigateBack(navController: NavController): Boolean {
        return navController.popBackStack()
    }

    /**
     * Navigate to a specific destination and clear the back stack
     */
    fun navigateAndClearBackStack(navController: NavController, destinationId: Int) {
        navController.navigate(
            destinationId,
            null,
            NavOptions.Builder()
                .setPopUpTo(navController.graph.startDestinationId, true)
                .build()
        )
    }

    /**
     * Navigate to Reader Settings
     */
    fun navigateToReaderSettings(navController: NavController) {
        navController.navigate(R.id.action_main_settings_to_reader_settings)
    }

    /**
     * Navigate to Backup Settings
     */
    fun navigateToBackupSettings(navController: NavController) {
        navController.navigate(R.id.action_main_settings_to_backup_settings)
    }

    /**
     * Navigate to Sync Settings Selection
     */
    fun navigateToSyncSettingsSelection(navController: NavController) {
        navController.navigate(R.id.action_main_settings_to_sync_settings_selection)
    }

    /**
     * Navigate to Sync Settings with URL
     */
    fun navigateToSyncSettings(navController: NavController, url: String) {
        val bundle = android.os.Bundle().apply {
            putString("url", url)
        }
        navController.navigate(R.id.action_sync_selection_to_sync_settings, bundle)
    }

    /**
     * Navigate to Sync Login
     */
    fun navigateToSyncLogin(navController: NavController, url: String, lookup: String) {
        val bundle = android.os.Bundle().apply {
            putString("url", url)
            putString("lookup", lookup)
        }
        navController.navigate(R.id.action_sync_selection_to_sync_login, bundle)
    }

    /**
     * Navigate to General Settings
     */
    fun navigateToGeneralSettings(navController: NavController) {
        navController.navigate(R.id.action_main_settings_to_general_settings)
    }

    /**
     * Navigate to TTS Settings
     */
    fun navigateToTTSSettings(navController: NavController) {
        navController.navigate(R.id.action_main_settings_to_tts_settings)
    }

    /**
     * Navigate to Language Settings
     */
    fun navigateToLanguageSettings(navController: NavController, changeLanguage: Boolean = false) {
        val bundle = android.os.Bundle().apply {
            putBoolean("changeLanguage", changeLanguage)
        }
        navController.navigate(R.id.action_main_settings_to_language, bundle)
    }

    /**
     * Navigate to Mention Settings
     */
    fun navigateToMentionSettings(navController: NavController) {
        navController.navigate(R.id.action_main_settings_to_mention_settings)
    }

    /**
     * Navigate to Copyright
     */
    fun navigateToCopyright(navController: NavController) {
        navController.navigate(R.id.action_main_settings_to_copyright)
    }

    /**
     * Navigate to Contributions
     */
    fun navigateToContributions(navController: NavController) {
        navController.navigate(R.id.action_main_settings_to_contributions)
    }

    /**
     * Navigate to Libraries Used
     */
    fun navigateToLibrariesUsed(navController: NavController) {
        navController.navigate(R.id.action_main_settings_to_libraries_used)
    }

    /**
     * Navigate to Reader Background Settings
     */
    fun navigateToReaderBackgroundSettings(navController: NavController) {
        navController.navigate(R.id.action_reader_settings_to_background_settings)
    }

    /**
     * Navigate to Scroll Behaviour Settings
     */
    fun navigateToScrollBehaviourSettings(navController: NavController) {
        navController.navigate(R.id.action_reader_settings_to_scroll_behaviour)
    }

    /**
     * Navigate to Google Backup Settings
     */
    fun navigateToGoogleBackupSettings(navController: NavController) {
        navController.navigate(R.id.action_backup_settings_to_google_backup)
    }

    /**
     * Navigate to CloudFlare Bypass
     */
    fun navigateToCloudFlareBypass(navController: NavController, hostName: String) {
        val bundle = android.os.Bundle().apply {
            putString("hostName", hostName)
        }
        navController.navigate(R.id.cloudFlareBypassFragment, bundle)
    }

    /**
     * Navigate back to Chapters from Reader
     */
    fun navigateBackToChapters(navController: NavController) {
        navController.navigate(R.id.action_reader_to_chapters)
    }

    /**
     * Navigate back to Novel Details from Reader
     */
    fun navigateBackToNovelDetailsFromReader(navController: NavController) {
        navController.navigate(R.id.action_reader_to_novel_details)
    }

    /**
     * Navigate back to Novel Details from Chapters
     */
    fun navigateBackToNovelDetailsFromChapters(navController: NavController) {
        navController.navigate(R.id.action_chapters_to_novel_details)
    }

    /**
     * Get default navigation options for smooth transitions
     */
    private fun getNavOptions(): NavOptions {
        return NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setRestoreState(true)
            .build()
    }
}