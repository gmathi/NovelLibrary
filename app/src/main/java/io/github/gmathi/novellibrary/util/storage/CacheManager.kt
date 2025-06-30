package io.github.gmathi.novellibrary.util.storage

import android.content.Context
import android.os.Environment
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.util.Logs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking

/**
 * Smart cache manager for improved storage performance and resource management.
 * Manages file caches, image caches, and database caches with intelligent cleanup.
 */
class CacheManager private constructor(private val context: Context) {
    
    companion object {
        private const val TAG = "CacheManager"
        private const val MAX_CACHE_SIZE = 100L * 1024 * 1024 // 100MB
        private const val MAX_IMAGE_CACHE_SIZE = 50L * 1024 * 1024 // 50MB
        private const val MAX_FILE_CACHE_SIZE = 30L * 1024 * 1024 // 30MB
        private const val CACHE_CLEANUP_INTERVAL_HOURS = 6L
        private const val FILE_CLEANUP_DAYS = 7L
        
        @Volatile
        private var instance: CacheManager? = null
        
        @Synchronized
        fun getInstance(context: Context): CacheManager {
            return instance ?: CacheManager(context.applicationContext).also {
                instance = it
            }
        }
    }
    
    private val dataCenter: DataCenter by injectLazy()
    private val cacheDir = File(context.cacheDir, "smart_cache")
    private val imageCacheDir = File(cacheDir, "images")
    private val fileCacheDir = File(cacheDir, "files")
    private val databaseCacheDir = File(cacheDir, "database")
    
    private val cleanupExecutor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    
    init {
        initializeCacheDirectories()
        setupScheduledCleanup()
    }
    
    private fun initializeCacheDirectories() {
        try {
            cacheDir.mkdirs()
            imageCacheDir.mkdirs()
            fileCacheDir.mkdirs()
            databaseCacheDir.mkdirs()
            
            Logs.debug(TAG, "Cache directories initialized: ${cacheDir.absolutePath}")
        } catch (e: Exception) {
            Logs.error(TAG, "Error initializing cache directories", e)
        }
    }
    
    private fun setupScheduledCleanup() {
        cleanupExecutor.scheduleAtFixedRate({
            try {
                // Use non-suspend version for scheduled cleanup
                cleanupCacheSync()
            } catch (e: Exception) {
                Logs.error(TAG, "Error during scheduled cache cleanup", e)
            }
        }, CACHE_CLEANUP_INTERVAL_HOURS, CACHE_CLEANUP_INTERVAL_HOURS, TimeUnit.HOURS)
    }
    
    /**
     * Non-suspend version of cache cleanup for scheduled tasks.
     */
    private fun cleanupCacheSync() {
        Logs.debug(TAG, "Starting scheduled cache cleanup...")
        
        try {
            // Clean up old files based on age
            val cutoffTime = System.currentTimeMillis() - (FILE_CLEANUP_DAYS * 24 * 60 * 60 * 1000L)
            
            listOf(imageCacheDir, fileCacheDir, databaseCacheDir).forEach { dir ->
                dir.walkTopDown()
                    .filter { it.isFile && it.lastModified() < cutoffTime }
                    .forEach { file ->
                        if (file.delete()) {
                            Logs.debug(TAG, "Deleted old file: ${file.name}")
                        }
                    }
            }
            
            Logs.debug(TAG, "Scheduled cache cleanup completed")
        } catch (e: Exception) {
            Logs.error(TAG, "Error during scheduled cache cleanup", e)
        }
    }
    
    /**
     * Clean up all caches based on size and age.
     */
    suspend fun cleanupCache() = withContext(Dispatchers.IO) {
        Logs.debug(TAG, "Starting cache cleanup...")
        
        val beforeStats = getCacheStats()
        
        cleanupImageCache()
        cleanupFileCache()
        cleanupDatabaseCache()
        cleanupOldFiles()
        
        val afterStats = getCacheStats()
        
        Logs.debug(TAG, "Cache cleanup completed: ${beforeStats.totalSize} -> ${afterStats.totalSize} bytes")
    }
    
    /**
     * Clean up image cache based on size and access time.
     */
    private suspend fun cleanupImageCache() = withContext(Dispatchers.IO) {
        val currentSize = getDirectorySize(imageCacheDir)
        if (currentSize <= MAX_IMAGE_CACHE_SIZE) return@withContext
        
        val files = imageCacheDir.listFiles()?.sortedBy { it.lastModified() } ?: return@withContext
        var sizeToFree = currentSize - MAX_IMAGE_CACHE_SIZE
        
        for (file in files) {
            if (sizeToFree <= 0) break
            val fileSize = file.length()
            if (file.delete()) {
                sizeToFree -= fileSize
                Logs.debug(TAG, "Deleted image cache file: ${file.name}")
            }
        }
    }
    
    /**
     * Clean up file cache based on size and access time.
     */
    private suspend fun cleanupFileCache() = withContext(Dispatchers.IO) {
        val currentSize = getDirectorySize(fileCacheDir)
        if (currentSize <= MAX_FILE_CACHE_SIZE) return@withContext
        
        val files = fileCacheDir.listFiles()?.sortedBy { it.lastModified() } ?: return@withContext
        var sizeToFree = currentSize - MAX_FILE_CACHE_SIZE
        
        for (file in files) {
            if (sizeToFree <= 0) break
            val fileSize = file.length()
            if (file.delete()) {
                sizeToFree -= fileSize
                Logs.debug(TAG, "Deleted file cache: ${file.name}")
            }
        }
    }
    
    /**
     * Clean up database cache files.
     */
    private suspend fun cleanupDatabaseCache() = withContext(Dispatchers.IO) {
        val currentSize = getDirectorySize(databaseCacheDir)
        val maxDbCacheSize = MAX_CACHE_SIZE - MAX_IMAGE_CACHE_SIZE - MAX_FILE_CACHE_SIZE
        
        if (currentSize <= maxDbCacheSize) return@withContext
        
        val files = databaseCacheDir.listFiles()?.sortedBy { it.lastModified() } ?: return@withContext
        var sizeToFree = currentSize - maxDbCacheSize
        
        for (file in files) {
            if (sizeToFree <= 0) break
            val fileSize = file.length()
            if (file.delete()) {
                sizeToFree -= fileSize
                Logs.debug(TAG, "Deleted database cache file: ${file.name}")
            }
        }
    }
    
    /**
     * Clean up old files based on age.
     */
    private suspend fun cleanupOldFiles() = withContext(Dispatchers.IO) {
        val cutoffTime = System.currentTimeMillis() - (FILE_CLEANUP_DAYS * 24 * 60 * 60 * 1000L)
        
        listOf(imageCacheDir, fileCacheDir, databaseCacheDir).forEach { dir ->
            dir.walkTopDown()
                .filter { it.isFile && it.lastModified() < cutoffTime }
                .forEach { file ->
                    if (file.delete()) {
                        Logs.debug(TAG, "Deleted old file: ${file.name}")
                    }
                }
        }
    }
    
    /**
     * Get cache statistics.
     */
    suspend fun getCacheStats(): CacheStats = withContext(Dispatchers.IO) {
        val imageSize = getDirectorySize(imageCacheDir)
        val fileSize = getDirectorySize(fileCacheDir)
        val databaseSize = getDirectorySize(databaseCacheDir)
        val totalSize = imageSize + fileSize + databaseSize
        
        CacheStats(
            totalSize = totalSize,
            imageCacheSize = imageSize,
            fileCacheSize = fileSize,
            databaseCacheSize = databaseSize,
            maxCacheSize = MAX_CACHE_SIZE,
            imageCacheMaxSize = MAX_IMAGE_CACHE_SIZE,
            fileCacheMaxSize = MAX_FILE_CACHE_SIZE,
            databaseCacheMaxSize = MAX_CACHE_SIZE - MAX_IMAGE_CACHE_SIZE - MAX_FILE_CACHE_SIZE
        )
    }
    
    /**
     * Get directory size recursively.
     */
    private suspend fun getDirectorySize(directory: File): Long = withContext(Dispatchers.IO) {
        if (!directory.exists() || !directory.isDirectory) return@withContext 0L
        
        directory.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }
    
    /**
     * Store a file in the appropriate cache directory.
     */
    suspend fun storeFile(fileName: String, data: ByteArray, cacheType: CacheType): File = withContext(Dispatchers.IO) {
        val targetDir = when (cacheType) {
            CacheType.IMAGE -> imageCacheDir
            CacheType.FILE -> fileCacheDir
            CacheType.DATABASE -> databaseCacheDir
        }
        
        val file = File(targetDir, fileName)
        file.writeBytes(data)
        
        Logs.debug(TAG, "Stored file in ${cacheType.name.lowercase()} cache: $fileName (${data.size} bytes)")
        file
    }
    
    /**
     * Retrieve a file from cache.
     */
    suspend fun getFile(fileName: String, cacheType: CacheType): File? = withContext(Dispatchers.IO) {
        val targetDir = when (cacheType) {
            CacheType.IMAGE -> imageCacheDir
            CacheType.FILE -> fileCacheDir
            CacheType.DATABASE -> databaseCacheDir
        }
        
        val file = File(targetDir, fileName)
        if (file.exists()) {
            Logs.debug(TAG, "Retrieved file from ${cacheType.name.lowercase()} cache: $fileName")
            file
        } else {
            null
        }
    }
    
    /**
     * Check if a file exists in cache.
     */
    suspend fun hasFile(fileName: String, cacheType: CacheType): Boolean = withContext(Dispatchers.IO) {
        val targetDir = when (cacheType) {
            CacheType.IMAGE -> imageCacheDir
            CacheType.FILE -> fileCacheDir
            CacheType.DATABASE -> databaseCacheDir
        }
        
        File(targetDir, fileName).exists()
    }
    
    /**
     * Delete a specific file from cache.
     */
    suspend fun deleteFile(fileName: String, cacheType: CacheType): Boolean = withContext(Dispatchers.IO) {
        val targetDir = when (cacheType) {
            CacheType.IMAGE -> imageCacheDir
            CacheType.FILE -> fileCacheDir
            CacheType.DATABASE -> databaseCacheDir
        }
        
        val file = File(targetDir, fileName)
        if (file.delete()) {
            Logs.debug(TAG, "Deleted file from ${cacheType.name.lowercase()} cache: $fileName")
            true
        } else {
            false
        }
    }
    
    /**
     * Clear all caches.
     */
    suspend fun clearAllCaches() = withContext(Dispatchers.IO) {
        Logs.debug(TAG, "Clearing all caches...")
        
        listOf(imageCacheDir, fileCacheDir, databaseCacheDir).forEach { dir ->
            dir.walkTopDown()
                .filter { it.isFile }
                .forEach { file ->
                    if (file.delete()) {
                        Logs.debug(TAG, "Deleted cache file: ${file.name}")
                    }
                }
        }
        
        Logs.debug(TAG, "All caches cleared")
    }
    
    /**
     * Get available storage space.
     */
    suspend fun getAvailableStorageSpace(): Long = withContext(Dispatchers.IO) {
        try {
            val externalStorage = Environment.getExternalStorageDirectory()
            externalStorage.usableSpace
        } catch (e: Exception) {
            Logs.error(TAG, "Error getting available storage space", e)
            0L
        }
    }
    
    /**
     * Check if storage is running low.
     */
    suspend fun isStorageLow(): Boolean = withContext(Dispatchers.IO) {
        val availableSpace = getAvailableStorageSpace()
        val lowStorageThreshold = 100L * 1024 * 1024 // 100MB
        
        availableSpace < lowStorageThreshold
    }
    
    /**
     * Shutdown the cache manager.
     */
    fun shutdown() {
        Logs.debug(TAG, "Starting CacheManager shutdown process")
        
        // Log cache statistics before shutdown
        runBlocking {
            try {
                val stats = getCacheStats()
                Logs.debug(TAG, "Cache statistics before shutdown: $stats")
            } catch (e: Exception) {
                Logs.error(TAG, "Error getting cache stats during shutdown", e)
            }
        }
        
        // Shutdown scheduled executor
        cleanupExecutor.shutdown()
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow()
                Logs.debug(TAG, "Forced shutdown of CacheManager cleanup executor")
            }
        } catch (e: InterruptedException) {
            cleanupExecutor.shutdownNow()
            Thread.currentThread().interrupt()
            Logs.debug(TAG, "Interrupted during CacheManager executor shutdown")
        }
        
        Logs.debug(TAG, "CacheManager shutdown completed successfully")
    }
    
    /**
     * Cache types for different file categories.
     */
    enum class CacheType {
        IMAGE, FILE, DATABASE
    }
    
    /**
     * Cache statistics for monitoring.
     */
    data class CacheStats(
        val totalSize: Long,
        val imageCacheSize: Long,
        val fileCacheSize: Long,
        val databaseCacheSize: Long,
        val maxCacheSize: Long,
        val imageCacheMaxSize: Long,
        val fileCacheMaxSize: Long,
        val databaseCacheMaxSize: Long
    ) {
        val totalUtilization: Double get() = if (maxCacheSize > 0) totalSize.toDouble() / maxCacheSize else 0.0
        val imageUtilization: Double get() = if (imageCacheMaxSize > 0) imageCacheSize.toDouble() / imageCacheMaxSize else 0.0
        val fileUtilization: Double get() = if (fileCacheMaxSize > 0) fileCacheSize.toDouble() / fileCacheMaxSize else 0.0
        val databaseUtilization: Double get() = if (databaseCacheMaxSize > 0) databaseCacheSize.toDouble() / databaseCacheMaxSize else 0.0
    }
} 