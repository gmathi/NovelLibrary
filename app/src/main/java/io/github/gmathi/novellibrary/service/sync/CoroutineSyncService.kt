package io.github.gmathi.novellibrary.service.sync

import android.content.Context
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.service.database.ServiceDatabaseManager
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.Utils
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

/**
 * Coroutine-based sync service that replaces blocking sync operations
 */
class CoroutineSyncService(
    private val context: Context,
    private val dbHelper: DBHelper,
    private val networkHelper: NetworkHelper
) {
    
    private val serviceDatabaseManager = ServiceDatabaseManager(dbHelper)
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        private const val TAG = "CoroutineSyncService"
        private const val MAX_CONCURRENT_SYNCS = 3
    }

    /**
     * Perform background novel sync with structured concurrency
     */
    suspend fun performBackgroundSync(): SyncResult = withContext(Dispatchers.IO) {
        if (!networkHelper.isConnectedToNetwork()) {
            return@withContext SyncResult.NetworkError
        }

        try {
            val novels = serviceDatabaseManager.getAllNovels()
            val sourceManager = SourceManager(context)
            val syncResults = mutableMapOf<Novel, SyncNovelResult>()
            
            // Process novels with limited concurrency
            val semaphore = Semaphore(MAX_CONCURRENT_SYNCS)
            val jobs = novels.map { novel ->
                async {
                    semaphore.withPermit {
                        syncNovel(novel, sourceManager)
                    }
                }
            }
            
            // Wait for all sync operations to complete
            val results = jobs.awaitAll()
            results.forEachIndexed { index, result ->
                syncResults[novels[index]] = result
            }
            
            // Process successful syncs
            val updatedNovels = syncResults.filter { it.value is SyncNovelResult.Success }
                .map { it.key to (it.value as SyncNovelResult.Success) }
            
            if (updatedNovels.isNotEmpty()) {
                updateNovelsInDatabase(updatedNovels)
                return@withContext SyncResult.Success(updatedNovels.map { it.first })
            }
            
            return@withContext SyncResult.NoUpdates
            
        } catch (e: Exception) {
            Logs.error(TAG, "Background sync failed", e)
            return@withContext SyncResult.Error(e)
        }
    }

    /**
     * Sync a single novel with proper error handling
     */
    private suspend fun syncNovel(novel: Novel, sourceManager: SourceManager): SyncNovelResult {
        return try {
            val source = sourceManager.get(novel.sourceId)
            if (source == null) {
                return SyncNovelResult.Error(Exception("Source not found for novel: ${novel.name}"))
            }
            
            val newChaptersList = source.getChapterList(novel) ?: ArrayList()
            
            // Calculate hash codes for comparison
            var currentChaptersHashCode = (novel.metadata[Constants.MetaDataKeys.HASH_CODE] ?: "0").toInt()
            if (currentChaptersHashCode == 0) {
                currentChaptersHashCode = serviceDatabaseManager.getAllWebPages(novel.id).sumOf { it.hashCode() }
            }
            val newChaptersHashCode = newChaptersList.sumOf { it.hashCode() }
            
            if (newChaptersList.isNotEmpty() && newChaptersHashCode != currentChaptersHashCode) {
                novel.metadata[Constants.MetaDataKeys.HASH_CODE] = newChaptersHashCode.toString()
                novel.metadata[Constants.MetaDataKeys.LAST_UPDATED_DATE] = Utils.getCurrentFormattedDate()
                
                SyncNovelResult.Success(novel, newChaptersList)
            } else {
                SyncNovelResult.NoChanges
            }
            
        } catch (e: Exception) {
            Logs.error(TAG, "Failed to sync novel: ${novel.name}", e)
            SyncNovelResult.Error(e)
        }
    }

    /**
     * Update novels in database with proper transaction handling
     */
    private suspend fun updateNovelsInDatabase(updatedNovels: List<Pair<Novel, SyncNovelResult.Success>>) {
        updatedNovels.forEach { (novel, syncResult) ->
            try {
                val chapters = syncResult.chapters
                val webPageSettings = chapters.map { WebPageSettings(it.url, novel.id) }
                
                serviceDatabaseManager.performSyncTransaction(novel, chapters, webPageSettings)
                
            } catch (e: Exception) {
                Logs.error(TAG, "Failed to update novel in database: ${novel.name}", e)
            }
        }
    }

    /**
     * Get sync progress as a Flow for reactive UI updates
     */
    fun getSyncProgressFlow(): Flow<SyncProgress> = flow {
        val novels = serviceDatabaseManager.getAllNovels()
        emit(SyncProgress(0, novels.size, "Starting sync..."))
        
        novels.forEachIndexed { index, novel ->
            emit(SyncProgress(index, novels.size, "Syncing ${novel.name}..."))
            // Actual sync would happen here in a real implementation
            delay(100) // Simulate work
        }
        
        emit(SyncProgress(novels.size, novels.size, "Sync completed"))
    }.flowOn(Dispatchers.IO)

    /**
     * Cancel all ongoing sync operations
     */
    fun cancelSync() {
        serviceScope.cancel()
    }

    /**
     * Check if sync is currently running
     */
    fun isSyncing(): Boolean {
        return serviceScope.isActive
    }

    /**
     * Clean up resources
     */
    fun cleanup() {
        serviceScope.cancel()
    }
}

/**
 * Result of a sync operation
 */
sealed class SyncResult {
    object NetworkError : SyncResult()
    object NoUpdates : SyncResult()
    data class Success(val updatedNovels: List<Novel>) : SyncResult()
    data class Error(val exception: Exception) : SyncResult()
}

/**
 * Result of syncing a single novel
 */
sealed class SyncNovelResult {
    object NoChanges : SyncNovelResult()
    data class Success(val novel: Novel, val chapters: List<WebPage>) : SyncNovelResult()
    data class Error(val exception: Exception) : SyncNovelResult()
}

/**
 * Progress information for sync operations
 */
data class SyncProgress(
    val current: Int,
    val total: Int,
    val message: String
) {
    val percentage: Int get() = if (total > 0) (current * 100) / total else 0
}