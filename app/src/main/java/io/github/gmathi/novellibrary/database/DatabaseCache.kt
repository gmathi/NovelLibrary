package io.github.gmathi.novellibrary.database

import android.util.LruCache
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import io.github.gmathi.novellibrary.util.Logs
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Database query result cache for improved performance.
 * Caches frequently accessed data to reduce database queries.
 * Optimized for novels with large numbers of chapters.
 */
class DatabaseCache private constructor() {
    
    companion object {
        private const val TAG = "DatabaseCache"
        
        // Novel cache - store novel metadata (small objects)
        private const val NOVEL_CACHE_SIZE = 200
        
        // Chapter cache - store individual chapter lists (larger objects)
        private const val CHAPTER_CACHE_SIZE = 20 // Reduced due to large chapter lists
        
        // Settings cache - store chapter settings (small objects)
        private const val SETTINGS_CACHE_SIZE = 500
        
        // Chapter chunk cache - store chunks of chapters for large novels
        private const val CHAPTER_CHUNK_CACHE_SIZE = 50
        private const val CHAPTERS_PER_CHUNK = 100 // Cache chapters in chunks of 100
        
        // Cache cleanup interval
        private const val CACHE_CLEANUP_INTERVAL_MINUTES = 30L
        
        // Memory threshold for large novels (in bytes)
        private const val LARGE_NOVEL_THRESHOLD = 10 * 1024 * 1024 // 10MB
        
        @Volatile
        private var instance: DatabaseCache? = null
        
        @Synchronized
        fun getInstance(): DatabaseCache {
            return instance ?: DatabaseCache().also {
                instance = it
            }
        }
    }
    
    // Novel cache with LRU eviction (metadata only)
    private val novelCache = LruCache<Long, Novel>(NOVEL_CACHE_SIZE)
    
    // Chapter cache with LRU eviction (full chapter lists for small novels)
    private val chapterCache = LruCache<String, List<WebPage>>(CHAPTER_CACHE_SIZE)
    
    // WebPage settings cache with LRU eviction
    private val settingsCache = LruCache<String, WebPageSettings>(SETTINGS_CACHE_SIZE)
    
    // Novel chapters cache (novelId -> chapters) - for small novels only
    private val novelChaptersCache = LruCache<Long, List<WebPage>>(CHAPTER_CACHE_SIZE)
    
    // Chapter chunks cache for large novels (novelId_chunkIndex -> chapters)
    private val chapterChunksCache = LruCache<String, List<WebPage>>(CHAPTER_CHUNK_CACHE_SIZE)
    
    // Track which novels are large and need chunked caching
    private val largeNovels = ConcurrentHashMap<Long, Int>() // novelId -> total chapters
    
    // Cache statistics
    private val cacheStats = ConcurrentHashMap<String, CacheStats>()
    
    // Scheduled executor for cache cleanup
    private val cleanupExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    
    init {
        setupCacheCleanup()
        setupCacheListeners()
    }
    
    // Novel cache operations
    fun getNovel(novelId: Long): Novel? {
        val novel = novelCache.get(novelId)
        updateStats("novel", novel != null)
        return novel
    }
    
    fun putNovel(novel: Novel) {
        novelCache.put(novel.id, novel)
        Logs.debug(TAG, "Cached novel: ${novel.name} (ID: ${novel.id})")
    }
    
    fun invalidateNovel(novelId: Long) {
        novelCache.remove(novelId)
        novelChaptersCache.remove(novelId)
        largeNovels.remove(novelId)
        
        // Remove all chunks for this novel
        val chunksToRemove = mutableListOf<String>()
        chapterChunksCache.snapshot().keys.forEach { key ->
            if (key.startsWith("${novelId}_")) {
                chunksToRemove.add(key)
            }
        }
        chunksToRemove.forEach { chapterChunksCache.remove(it) }
        
        Logs.debug(TAG, "Invalidated novel cache for ID: $novelId (removed ${chunksToRemove.size} chunks)")
    }
    
    // Chapter cache operations with chunking for large novels
    fun getChapters(novelId: Long): List<WebPage>? {
        // Check if this is a large novel
        val totalChapters = largeNovels[novelId]
        
        return if (totalChapters != null && totalChapters > CHAPTERS_PER_CHUNK) {
            // Large novel - return first chunk only (most commonly accessed)
            getChapterChunk(novelId, 0)
        } else {
            // Small novel - return full list
            val chapters = novelChaptersCache.get(novelId)
            updateStats("chapters", chapters != null)
            chapters
        }
    }
    
    fun putChapters(novelId: Long, chapters: List<WebPage>) {
        if (chapters.size > CHAPTERS_PER_CHUNK) {
            // Large novel - use chunked caching
            largeNovels[novelId] = chapters.size
            putChapterChunks(novelId, chapters)
            Logs.debug(TAG, "Cached ${chapters.size} chapters for large novel ID: $novelId in chunks")
        } else {
            // Small novel - cache full list
            novelChaptersCache.put(novelId, chapters)
            Logs.debug(TAG, "Cached ${chapters.size} chapters for novel ID: $novelId")
        }
    }
    
    // Chapter chunk operations for large novels
    private fun getChapterChunk(novelId: Long, chunkIndex: Int): List<WebPage>? {
        val chunkKey = "${novelId}_$chunkIndex"
        val chapters = chapterChunksCache.get(chunkKey)
        updateStats("chapter_chunks", chapters != null)
        return chapters
    }
    
    private fun putChapterChunks(novelId: Long, chapters: List<WebPage>) {
        val chunks = chapters.chunked(CHAPTERS_PER_CHUNK)
        chunks.forEachIndexed { index, chunk ->
            val chunkKey = "${novelId}_$index"
            chapterChunksCache.put(chunkKey, chunk)
        }
        Logs.debug(TAG, "Cached ${chapters.size} chapters in ${chunks.size} chunks for novel ID: $novelId")
    }
    
    // Get specific chapter range for large novels
    fun getChapterRange(novelId: Long, startIndex: Int, endIndex: Int): List<WebPage>? {
        val totalChapters = largeNovels[novelId]
        if (totalChapters == null || totalChapters <= CHAPTERS_PER_CHUNK) {
            // Small novel - get full list and slice
            val chapters = novelChaptersCache.get(novelId)
            return chapters?.let { it.subList(startIndex, minOf(endIndex, it.size)) }
        }
        
        // Large novel - get from chunks
        val startChunk = startIndex / CHAPTERS_PER_CHUNK
        val endChunk = endIndex / CHAPTERS_PER_CHUNK
        
        val result = mutableListOf<WebPage>()
        for (chunkIndex in startChunk..endChunk) {
            val chunk = getChapterChunk(novelId, chunkIndex)
            if (chunk != null) {
                val chunkStart = if (chunkIndex == startChunk) startIndex % CHAPTERS_PER_CHUNK else 0
                val chunkEnd = if (chunkIndex == endChunk) endIndex % CHAPTERS_PER_CHUNK else chunk.size
                result.addAll(chunk.subList(chunkStart, chunkEnd))
            }
        }
        
        return if (result.isNotEmpty()) result else null
    }
    
    fun getChaptersByUrl(url: String): List<WebPage>? {
        val chapters = chapterCache.get(url)
        updateStats("chapters_by_url", chapters != null)
        return chapters
    }
    
    fun putChaptersByUrl(url: String, chapters: List<WebPage>) {
        chapterCache.put(url, chapters)
        Logs.debug(TAG, "Cached chapters by URL: $url")
    }
    
    // WebPage settings cache operations
    fun getWebPageSettings(url: String): WebPageSettings? {
        val settings = settingsCache.get(url)
        updateStats("settings", settings != null)
        return settings
    }
    
    fun putWebPageSettings(settings: WebPageSettings) {
        settingsCache.put(settings.url, settings)
        Logs.debug(TAG, "Cached settings for URL: ${settings.url}")
    }
    
    fun invalidateWebPageSettings(url: String) {
        settingsCache.remove(url)
        Logs.debug(TAG, "Invalidated settings cache for URL: $url")
    }
    
    // Bulk operations
    fun putNovels(novels: List<Novel>) {
        novels.forEach { putNovel(it) }
        Logs.debug(TAG, "Bulk cached ${novels.size} novels")
    }
    
    fun putChaptersForNovels(chaptersMap: Map<Long, List<WebPage>>) {
        chaptersMap.forEach { (novelId, chapters) ->
            putChapters(novelId, chapters)
        }
        Logs.debug(TAG, "Bulk cached chapters for ${chaptersMap.size} novels")
    }
    
    // Cache invalidation
    fun invalidateAll() {
        novelCache.evictAll()
        chapterCache.evictAll()
        settingsCache.evictAll()
        novelChaptersCache.evictAll()
        chapterChunksCache.evictAll()
        largeNovels.clear()
        Logs.debug(TAG, "All caches invalidated")
    }
    
    fun invalidateNovelRelated(novelId: Long) {
        invalidateNovel(novelId)
        // Also invalidate any chapters that might be related
        val chapters = novelChaptersCache.get(novelId)
        chapters?.forEach { chapter ->
            settingsCache.remove(chapter.url)
        }
        Logs.debug(TAG, "Invalidated all cache entries related to novel ID: $novelId")
    }
    
    // Cache statistics
    fun getCacheStats(): Map<String, CacheStats> {
        return cacheStats.toMap()
    }
    
    fun getMemoryUsage(): MemoryUsage {
        return MemoryUsage(
            novelCacheSize = novelCache.size(),
            novelCacheMaxSize = novelCache.maxSize(),
            chapterCacheSize = chapterCache.size(),
            chapterCacheMaxSize = chapterCache.maxSize(),
            settingsCacheSize = settingsCache.size(),
            settingsCacheMaxSize = settingsCache.maxSize(),
            novelChaptersCacheSize = novelChaptersCache.size(),
            novelChaptersCacheMaxSize = novelChaptersCache.maxSize(),
            chapterChunksCacheSize = chapterChunksCache.size(),
            chapterChunksCacheMaxSize = chapterChunksCache.maxSize(),
            largeNovelsCount = largeNovels.size
        )
    }
    
    // Cache cleanup
    fun cleanup() {
        val beforeSize = getTotalCacheSize()
        novelCache.evictAll()
        chapterCache.evictAll()
        settingsCache.evictAll()
        novelChaptersCache.evictAll()
        chapterChunksCache.evictAll()
        largeNovels.clear()
        val afterSize = getTotalCacheSize()
        
        Logs.debug(TAG, "Cache cleanup completed: $beforeSize -> $afterSize entries")
    }
    
    // Shutdown
    fun shutdown() {
        Logs.debug(TAG, "Starting DatabaseCache shutdown process")
        
        // Log cache statistics before shutdown
        val stats = getCacheStats()
        val memoryUsage = getMemoryUsage()
        Logs.debug(TAG, "Cache statistics before shutdown: $stats")
        Logs.debug(TAG, "Memory usage before shutdown: $memoryUsage")
        
        // Shutdown scheduled executor
        cleanupExecutor.shutdown()
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow()
                Logs.debug(TAG, "Forced shutdown of cleanup executor")
            }
        } catch (e: InterruptedException) {
            cleanupExecutor.shutdownNow()
            Thread.currentThread().interrupt()
            Logs.debug(TAG, "Interrupted during executor shutdown")
        }
        
        // Invalidate all caches
        invalidateAll()
        
        Logs.debug(TAG, "DatabaseCache shutdown completed successfully")
    }
    
    private fun setupCacheCleanup() {
        cleanupExecutor.scheduleAtFixedRate({
            try {
                cleanup()
            } catch (e: Exception) {
                Logs.error(TAG, "Error during cache cleanup", e)
            }
        }, CACHE_CLEANUP_INTERVAL_MINUTES, CACHE_CLEANUP_INTERVAL_MINUTES, TimeUnit.MINUTES)
    }
    
    private fun setupCacheListeners() {
        // Note: Android's LruCache doesn't have setEntryRemovedListener method
        // We'll track evictions through the cleanup process instead
        Logs.debug(TAG, "Cache listeners setup completed")
    }
    
    private fun updateStats(cacheType: String, hit: Boolean) {
        val stats = cacheStats.getOrPut(cacheType) { CacheStats() }
        if (hit) {
            stats.hits++
        } else {
            stats.misses++
        }
    }
    
    private fun getTotalCacheSize(): Int {
        return novelCache.size() + chapterCache.size() + 
               settingsCache.size() + novelChaptersCache.size() + chapterChunksCache.size()
    }
    
    /**
     * Cache statistics for monitoring performance.
     */
    data class CacheStats(
        var hits: Long = 0,
        var misses: Long = 0
    ) {
        val totalRequests: Long get() = hits + misses
        val hitRate: Double get() = if (totalRequests > 0) hits.toDouble() / totalRequests else 0.0
    }
    
    /**
     * Memory usage statistics for all caches.
     */
    data class MemoryUsage(
        val novelCacheSize: Int,
        val novelCacheMaxSize: Int,
        val chapterCacheSize: Int,
        val chapterCacheMaxSize: Int,
        val settingsCacheSize: Int,
        val settingsCacheMaxSize: Int,
        val novelChaptersCacheSize: Int,
        val novelChaptersCacheMaxSize: Int,
        val chapterChunksCacheSize: Int,
        val chapterChunksCacheMaxSize: Int,
        val largeNovelsCount: Int
    ) {
        val totalSize: Int get() = novelCacheSize + chapterCacheSize + settingsCacheSize + 
                                  novelChaptersCacheSize + chapterChunksCacheSize
        val totalMaxSize: Int get() = novelCacheMaxSize + chapterCacheMaxSize + settingsCacheMaxSize + 
                                     novelChaptersCacheMaxSize + chapterChunksCacheMaxSize
        val utilization: Double get() = if (totalMaxSize > 0) totalSize.toDouble() / totalMaxSize else 0.0
    }
} 