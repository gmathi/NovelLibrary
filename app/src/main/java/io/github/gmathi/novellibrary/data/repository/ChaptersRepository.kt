package io.github.gmathi.novellibrary.data.repository

import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.getAllWebPages
import io.github.gmathi.novellibrary.database.getAllWebPageSettings
import io.github.gmathi.novellibrary.database.deleteWebPages
import io.github.gmathi.novellibrary.database.deleteWebPage
import io.github.gmathi.novellibrary.database.updateChaptersCount
import io.github.gmathi.novellibrary.database.updateNovelMetaData
import io.github.gmathi.novellibrary.database.createWebPage
import io.github.gmathi.novellibrary.database.createWebPageSettings
import io.github.gmathi.novellibrary.database.updateNewReleasesCount
import io.github.gmathi.novellibrary.database.getNovel
import io.github.gmathi.novellibrary.database.updateWebPageSettingsReadStatus
import io.github.gmathi.novellibrary.database.updateWebPageSettings
import io.github.gmathi.novellibrary.database.deleteWebPageSettings
import io.github.gmathi.novellibrary.database.getWebPageSettings
import io.github.gmathi.novellibrary.database.runTransaction
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.Exceptions.MISSING_SOURCE_ID
import io.github.gmathi.novellibrary.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.injectLazy

class ChaptersRepository {
    
    companion object {
        const val TAG = "ChaptersRepository"
    }

    private val dbHelper: DBHelper by injectLazy()
    private val sourceManager: SourceManager by injectLazy()
    private val networkHelper: NetworkHelper by injectLazy()

    suspend fun getChaptersFromDatabase(novelId: Long): Pair<List<WebPage>, List<WebPageSettings>> = withContext(Dispatchers.IO) {
        val chapters = dbHelper.getAllWebPages(novelId)
        val settings = dbHelper.getAllWebPageSettings(novelId)
        Pair(chapters, settings)
    }

    suspend fun getChaptersFromSource(novel: Novel, showSources: Boolean): List<WebPage> = withContext(Dispatchers.IO) {
        try {
            val source = sourceManager.get(novel.sourceId) ?: throw Exception(MISSING_SOURCE_ID)
            val fetchedChapters = source.getChapterList(novel)
            if (novel.id != -1L) {
                fetchedChapters.forEach { it.novelId = novel.id }
            }
            fetchedChapters
        } catch (e: Exception) {
            Logs.error(TAG, "Error fetching chapters from source", e)
            emptyList()
        }
    }

    suspend fun saveChaptersToDatabase(
        novel: Novel, 
        chapters: List<WebPage>, 
        forceUpdate: Boolean = false,
        progressCallback: ((String) -> Unit)? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            progressCallback?.invoke("Adding/Updating Cache…")

            // DB transaction for faster insertions
            dbHelper.writableDatabase.runTransaction { writableDatabase ->
                if (forceUpdate) {
                    // Delete the current data
                    dbHelper.deleteWebPages(novel.id, writableDatabase)
                    chapters.forEach { dbHelper.deleteWebPage(it.url, writableDatabase) }
                }

                val chaptersCount = chapters.size
                dbHelper.updateChaptersCount(novel.id, chaptersCount.toLong(), writableDatabase)

                val chaptersHash = chapters.sumBy { it.hashCode() }
                novel.metadata[Constants.MetaDataKeys.HASH_CODE] = chaptersHash.toString()
                dbHelper.updateNovelMetaData(novel)

                for (i in 0 until chaptersCount) {
                    progressCallback?.invoke("Caching Chapters: $i/$chaptersCount")
                    dbHelper.createWebPage(chapters[i], writableDatabase)
                    dbHelper.createWebPageSettings(WebPageSettings(chapters[i].url, novel.id), writableDatabase)
                }
            }
            true
        } catch (e: Exception) {
            Logs.error(TAG, "Error saving chapters to database", e)
            false
        }
    }

    suspend fun updateNovelMetadata(novel: Novel) = withContext(Dispatchers.IO) {
        novel.metadata[Constants.MetaDataKeys.LAST_UPDATED_DATE] = Utils.getCurrentFormattedDate()
        dbHelper.updateNovelMetaData(novel)
    }

    suspend fun updateNewReleasesCount(novelId: Long, count: Long) = withContext(Dispatchers.IO) {
        dbHelper.updateNewReleasesCount(novelId, count)
    }

    suspend fun getNovel(novelId: Long): Novel? = withContext(Dispatchers.IO) {
        dbHelper.getNovel(novelId)
    }

    fun isNetworkConnected(): Boolean {
        return networkHelper.isConnectedToNetwork()
    }

    suspend fun updateWebPageSettingsReadStatus(
        webPageSettings: WebPageSettings, 
        markRead: Boolean,
        writableDatabase: android.database.sqlite.SQLiteDatabase? = null
    ) = withContext(Dispatchers.IO) {
        dbHelper.updateWebPageSettingsReadStatus(webPageSettings, markRead, writableDatabase)
    }

    suspend fun updateWebPageSettings(
        webPageSettings: WebPageSettings,
        writableDatabase: android.database.sqlite.SQLiteDatabase? = null
    ) = withContext(Dispatchers.IO) {
        dbHelper.updateWebPageSettings(webPageSettings, writableDatabase)
    }

    suspend fun deleteWebPageSettings(url: String) = withContext(Dispatchers.IO) {
        dbHelper.deleteWebPageSettings(url)
    }

    suspend fun getWebPageSettings(url: String): WebPageSettings? = withContext(Dispatchers.IO) {
        dbHelper.getWebPageSettings(url)
    }
} 