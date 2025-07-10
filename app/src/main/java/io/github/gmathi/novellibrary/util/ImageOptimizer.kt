package io.github.gmathi.novellibrary.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import coil.ImageLoader
import coil.request.ImageRequest
import coil.size.Size
import io.github.gmathi.novellibrary.network.NetworkHelperOptimized
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Image optimization utility for better performance and memory management
 */
class ImageOptimizer(private val context: Context) {
    
    companion object {
        private const val CACHE_SIZE = 20 * 1024 * 1024 // 20MB cache
        private const val MAX_IMAGE_SIZE = 1024 // Max dimension for loaded images
        private const val COMPRESSION_QUALITY = 85 // JPEG compression quality
    }
    
    // Memory cache for bitmaps
    private val memoryCache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt() // 1/8 of available memory
    ) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }
    }
    
    private val networkHelper = NetworkHelperOptimized(context)
    
    /**
     * Load and optimize image with caching
     */
    suspend fun loadOptimizedImage(url: String, width: Int = MAX_IMAGE_SIZE, height: Int = MAX_IMAGE_SIZE): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                // Check memory cache first
                memoryCache.get(url)?.let { return@withContext it }
                
                // Check disk cache
                val cachedFile = getCachedImageFile(url)
                if (cachedFile.exists()) {
                    val bitmap = loadBitmapFromFile(cachedFile, width, height)
                    bitmap?.let { memoryCache.put(url, it) }
                    return@withContext bitmap
                }
                
                // Load from network with Coil
                val request = ImageRequest.Builder(context)
                    .data(url)
                    .size(Size(width, height))
                    .build()
                
                val result = networkHelper.imageLoader.execute(request)
                val bitmap = result.drawable?.toBitmap()
                
                // Cache the result
                bitmap?.let {
                    val optimizedBitmap = optimizeBitmap(it, width, height)
                    memoryCache.put(url, optimizedBitmap)
                    cacheImageToDisk(url, optimizedBitmap)
                    return@withContext optimizedBitmap
                }
                
                null
            } catch (e: Exception) {
                Logs.error("ImageOptimizer", "Error loading image: $url", e)
                null
            }
        }
    }
    
    /**
     * Optimize bitmap size and quality
     */
    private fun optimizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        // Calculate new dimensions maintaining aspect ratio
        val ratio = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        
        return if (ratio < 1.0f) {
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
    }
    
    /**
     * Load bitmap from file with size optimization
     */
    private fun loadBitmapFromFile(file: File, maxWidth: Int, maxHeight: Int): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, options)
            
            // Calculate sample size
            options.inSampleSize = calculateInSampleSize(options, maxWidth, maxHeight)
            options.inJustDecodeBounds = false
            
            BitmapFactory.decodeFile(file.absolutePath, options)
        } catch (e: Exception) {
            Logs.error("ImageOptimizer", "Error loading bitmap from file", e)
            null
        }
    }
    
    /**
     * Calculate sample size for bitmap loading
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        
        return inSampleSize
    }
    
    /**
     * Cache image to disk
     */
    private fun cacheImageToDisk(url: String, bitmap: Bitmap) {
        try {
            val file = getCachedImageFile(url)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, out)
            }
        } catch (e: Exception) {
            Logs.error("ImageOptimizer", "Error caching image to disk", e)
        }
    }
    
    /**
     * Get cached image file
     */
    private fun getCachedImageFile(url: String): File {
        val fileName = url.hashCode().toString()
        return File(context.cacheDir, "optimized_images/$fileName.jpg")
    }
    
    /**
     * Clear memory cache
     */
    fun clearMemoryCache() {
        memoryCache.evictAll()
    }
    
    /**
     * Clear disk cache
     */
    fun clearDiskCache() {
        val cacheDir = File(context.cacheDir, "optimized_images")
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
        }
    }
    
    /**
     * Preload images for better UX
     */
    suspend fun preloadImages(urls: List<String>) {
        withContext(Dispatchers.IO) {
            urls.take(5).forEach { url -> // Limit preloading to 5 images
                try {
                    loadOptimizedImage(url, 512, 512) // Smaller size for preloading
                } catch (e: Exception) {
                    // Ignore preload errors
                }
            }
        }
    }
}