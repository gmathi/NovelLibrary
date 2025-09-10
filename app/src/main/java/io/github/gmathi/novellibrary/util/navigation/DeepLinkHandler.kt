package io.github.gmathi.novellibrary.util.navigation

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.navigation.NavDeepLinkRequest
import dagger.hilt.android.scopes.ActivityScoped
import io.github.gmathi.novellibrary.model.database.Novel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeepLinkHandler @Inject constructor() {

    /**
     * Handle deep links from intents and convert them to Navigation Component deep link requests
     */
    fun handleDeepLink(intent: Intent): NavDeepLinkRequest? {
        return when {
            // Handle novel deep links
            intent.hasExtra("novel") -> {
                val novel = intent.getSerializableExtra("novel") as? Novel
                novel?.let {
                    createNovelDeepLink(it.id)
                }
            }
            
            // Handle novel ID deep links
            intent.hasExtra("novelId") -> {
                val novelId = intent.getLongExtra("novelId", -1L)
                if (novelId != -1L) {
                    createNovelDeepLink(novelId)
                } else null
            }
            
            // Handle reader deep links
            intent.hasExtra("novelId") && intent.hasExtra("chapterId") -> {
                val novelId = intent.getLongExtra("novelId", -1L)
                val chapterId = intent.getLongExtra("chapterId", -1L)
                if (novelId != -1L && chapterId != -1L) {
                    createReaderDeepLink(novelId, chapterId)
                } else null
            }
            
            // Handle URI-based deep links
            intent.data != null -> {
                handleUriDeepLink(intent.data!!)
            }
            
            // Handle action-based intents
            intent.action != null -> {
                handleActionIntent(intent)
            }
            
            else -> null
        }
    }

    /**
     * Create a deep link request for novel details
     */
    private fun createNovelDeepLink(novelId: Long): NavDeepLinkRequest {
        return NavDeepLinkRequest.Builder
            .fromUri("app://novellibrary/novel/$novelId".toUri())
            .build()
    }

    /**
     * Create a deep link request for reader
     */
    private fun createReaderDeepLink(novelId: Long, chapterId: Long): NavDeepLinkRequest {
        return NavDeepLinkRequest.Builder
            .fromUri("app://novellibrary/reader/$novelId/$chapterId".toUri())
            .build()
    }

    /**
     * Handle URI-based deep links (e.g., from external apps or web links)
     */
    private fun handleUriDeepLink(uri: Uri): NavDeepLinkRequest? {
        return when {
            // Handle novel URLs from external sources
            uri.pathSegments.contains("novel") -> {
                // Extract novel ID from URI if possible
                val novelId = extractNovelIdFromUri(uri)
                if (novelId != -1L) {
                    createNovelDeepLink(novelId)
                } else null
            }
            
            // Handle reader URLs
            uri.pathSegments.contains("reader") -> {
                val novelId = extractNovelIdFromUri(uri)
                val chapterId = extractChapterIdFromUri(uri)
                if (novelId != -1L && chapterId != -1L) {
                    createReaderDeepLink(novelId, chapterId)
                } else null
            }
            
            else -> null
        }
    }

    /**
     * Handle action-based intents (e.g., sharing, file opening)
     */
    private fun handleActionIntent(intent: Intent): NavDeepLinkRequest? {
        return when (intent.action) {
            Intent.ACTION_SEND -> {
                // Handle shared content - navigate to search or import
                if (intent.type == "text/plain") {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    // Navigate to search fragment to handle shared text
                    NavDeepLinkRequest.Builder
                        .fromUri("app://novellibrary/search".toUri())
                        .build()
                } else null
            }
            
            Intent.ACTION_VIEW -> {
                // Handle file associations or web links
                intent.data?.let { uri ->
                    handleUriDeepLink(uri)
                }
            }
            
            // Handle novel library specific actions
            "io.github.gmathi.novellibrary.OPEN_NOVEL" -> {
                val novelId = intent.getLongExtra("novelId", -1L)
                if (novelId != -1L) {
                    createNovelDeepLink(novelId)
                } else null
            }
            
            "io.github.gmathi.novellibrary.OPEN_READER" -> {
                val novelId = intent.getLongExtra("novelId", -1L)
                val chapterId = intent.getLongExtra("chapterId", -1L)
                if (novelId != -1L && chapterId != -1L) {
                    createReaderDeepLink(novelId, chapterId)
                } else null
            }
            
            else -> null
        }
    }

    /**
     * Extract novel ID from URI
     */
    private fun extractNovelIdFromUri(uri: Uri): Long {
        return try {
            // Try to extract from path segments
            val segments = uri.pathSegments
            val novelIndex = segments.indexOf("novel")
            if (novelIndex != -1 && novelIndex + 1 < segments.size) {
                segments[novelIndex + 1].toLong()
            } else {
                // Try to extract from query parameters
                uri.getQueryParameter("novelId")?.toLong() ?: -1L
            }
        } catch (e: NumberFormatException) {
            -1L
        }
    }

    /**
     * Extract chapter ID from URI
     */
    private fun extractChapterIdFromUri(uri: Uri): Long {
        return try {
            // Try to extract from path segments
            val segments = uri.pathSegments
            val chapterIndex = segments.indexOf("chapter")
            if (chapterIndex != -1 && chapterIndex + 1 < segments.size) {
                segments[chapterIndex + 1].toLong()
            } else {
                // Try to extract from query parameters
                uri.getQueryParameter("chapterId")?.toLong() ?: -1L
            }
        } catch (e: NumberFormatException) {
            -1L
        }
    }

    /**
     * Create deep link for library fragment
     */
    fun createLibraryDeepLink(): NavDeepLinkRequest {
        return NavDeepLinkRequest.Builder
            .fromUri("app://novellibrary/library".toUri())
            .build()
    }

    /**
     * Create deep link for search fragment
     */
    fun createSearchDeepLink(): NavDeepLinkRequest {
        return NavDeepLinkRequest.Builder
            .fromUri("app://novellibrary/search".toUri())
            .build()
    }

    /**
     * Create deep link for extensions fragment
     */
    fun createExtensionsDeepLink(): NavDeepLinkRequest {
        return NavDeepLinkRequest.Builder
            .fromUri("app://novellibrary/extensions".toUri())
            .build()
    }

    /**
     * Create deep link for downloads fragment
     */
    fun createDownloadsDeepLink(): NavDeepLinkRequest {
        return NavDeepLinkRequest.Builder
            .fromUri("app://novellibrary/downloads".toUri())
            .build()
    }

    /**
     * Create deep link for settings fragment
     */
    fun createSettingsDeepLink(): NavDeepLinkRequest {
        return NavDeepLinkRequest.Builder
            .fromUri("app://novellibrary/settings".toUri())
            .build()
    }

    /**
     * Check if an intent contains a deep link that we can handle
     */
    fun canHandleIntent(intent: Intent): Boolean {
        return intent.hasExtra("novel") ||
                intent.hasExtra("novelId") ||
                intent.data != null ||
                intent.action in listOf(
                    Intent.ACTION_SEND, 
                    Intent.ACTION_VIEW,
                    "io.github.gmathi.novellibrary.OPEN_NOVEL",
                    "io.github.gmathi.novellibrary.OPEN_READER"
                )
    }
}