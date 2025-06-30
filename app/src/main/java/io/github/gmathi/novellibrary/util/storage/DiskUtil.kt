package io.github.gmathi.novellibrary.util.storage

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.os.EnvironmentCompat
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.lang.Hash
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import android.media.MediaScannerConnection

object DiskUtil {
    private const val TAG = "DiskUtil"

    fun hashKeyForDisk(key: String): String {
        return Hash.md5(key)
    }

    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    fun getDirectorySize(f: File): Long {
        var size: Long = 0
        if (f.isDirectory) {
            for (file in f.listFiles()) {
                size += getDirectorySize(file)
            }
        } else {
            size = f.length()
        }
        return size
    }

    /**
     * Async version of getDirectorySize for better performance.
     */
    suspend fun getDirectorySizeAsync(dir: File): Long = withContext(Dispatchers.IO) {
        if (!dir.exists()) return@withContext 0L
        
        dir.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    /**
     * Clean up old files based on age.
     */
    suspend fun cleanupOldFiles(directory: File, maxAgeDays: Int) = withContext(Dispatchers.IO) {
        if (!directory.exists() || !directory.isDirectory) return@withContext
        
        val cutoffTime = System.currentTimeMillis() - (maxAgeDays * 24 * 60 * 60 * 1000L)
        var deletedCount = 0
        var freedSpace = 0L
        
        directory.walkTopDown()
            .filter { it.isFile && it.lastModified() < cutoffTime }
            .forEach { file ->
                val fileSize = file.length()
                if (file.delete()) {
                    deletedCount++
                    freedSpace += fileSize
                    Logs.debug(TAG, "Deleted old file: ${file.name} (${fileSize} bytes)")
                }
            }
        
        Logs.debug(TAG, "Cleanup completed: deleted $deletedCount files, freed ${freedSpace} bytes")
    }

    /**
     * Optimized file copy with progress tracking.
     */
    suspend fun copyFileAsync(source: File, destination: File, progressCallback: ((Float) -> Unit)? = null): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!source.exists()) {
                Logs.error(TAG, "Source file does not exist: ${source.absolutePath}")
                return@withContext false
            }
            
            destination.parentFile?.mkdirs()
            
            val bufferSize = 8192
            val buffer = ByteArray(bufferSize)
            val totalBytes = source.length()
            var copiedBytes = 0L
            
            source.inputStream().use { input ->
                destination.outputStream().use { output ->
                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        copiedBytes += bytesRead
                        
                        progressCallback?.invoke(copiedBytes.toFloat() / totalBytes)
                    }
                }
            }
            
            Logs.debug(TAG, "File copied successfully: ${source.name} -> ${destination.name}")
            true
        } catch (e: IOException) {
            Logs.error(TAG, "Error copying file: ${source.name}", e)
            false
        }
    }

    /**
     * Batch file operations for better performance.
     */
    suspend fun batchFileOperations(operations: List<FileOperation>): BatchResult = withContext(Dispatchers.IO) {
        val results = mutableListOf<FileOperationResult>()
        var totalProcessed = 0
        var totalSuccess = 0
        var totalFreedSpace = 0L
        
        operations.forEach { operation ->
            val result = when (operation) {
                is FileOperation.Delete -> {
                    val fileSize = operation.file.length()
                    val success = operation.file.delete()
                    if (success) {
                        totalFreedSpace += fileSize
                        totalSuccess++
                    }
                    FileOperationResult(operation, success, fileSize)
                }
                is FileOperation.Copy -> {
                    val success = copyFileAsync(operation.source, operation.destination)
                    if (success) totalSuccess++
                    FileOperationResult(operation, success, operation.source.length())
                }
                is FileOperation.Move -> {
                    val fileSize = operation.source.length()
                    val success = operation.source.renameTo(operation.destination)
                    if (success) totalSuccess++
                    FileOperationResult(operation, success, fileSize)
                }
            }
            results.add(result)
            totalProcessed++
        }
        
        BatchResult(
            totalProcessed = totalProcessed,
            totalSuccess = totalSuccess,
            totalFreedSpace = totalFreedSpace,
            results = results
        )
    }

    /**
     * Get storage statistics for monitoring.
     */
    suspend fun getStorageStats(context: Context): StorageStats = withContext(Dispatchers.IO) {
        val externalStorage = Environment.getExternalStorageDirectory()
        val internalStorage = context.filesDir
        
        val externalTotal = externalStorage.totalSpace
        val externalAvailable = externalStorage.usableSpace
        val externalUsed = externalTotal - externalAvailable
        
        val internalTotal = internalStorage.totalSpace
        val internalAvailable = internalStorage.usableSpace
        val internalUsed = internalTotal - internalAvailable
        
        StorageStats(
            externalTotal = externalTotal,
            externalAvailable = externalAvailable,
            externalUsed = externalUsed,
            internalTotal = internalTotal,
            internalAvailable = internalAvailable,
            internalUsed = internalUsed
        )
    }

    /**
     * Check if storage is running low.
     */
    suspend fun isStorageLow(context: Context): Boolean = withContext(Dispatchers.IO) {
        val stats = getStorageStats(context)
        val lowStorageThreshold = 100L * 1024 * 1024 // 100MB
        
        stats.externalAvailable < lowStorageThreshold || stats.internalAvailable < lowStorageThreshold
    }

    /**
     * Returns the root folders of all the available external storages.
     */
    fun getExternalStorages(context: Context): Collection<File> {
        val directories = mutableSetOf<File>()
        directories += ContextCompat.getExternalFilesDirs(context, null)
            .filterNotNull()
            .mapNotNull {
                val file = File(it.absolutePath.substringBefore("/Android/"))
                val state = EnvironmentCompat.getStorageState(file)
                if (state == Environment.MEDIA_MOUNTED || state == Environment.MEDIA_MOUNTED_READ_ONLY) {
                    file
                } else {
                    null
                }
            }

        return directories
    }

    /**
     * Scans the given file so that it can be shown in gallery apps, for example.
     */
    fun scanMedia(context: Context, file: File) {
        scanMedia(context, file.toUri())
    }

    /**
     * Scans the given file so that it can be shown in gallery apps, for example.
     */
    fun scanMedia(context: Context, uri: Uri) {
        // Use MediaScannerConnection instead of deprecated ACTION_MEDIA_SCANNER_SCAN_FILE
        MediaScannerConnection.scanFile(
            context,
            arrayOf(uri.path ?: ""),
            null
        ) { path, uri ->
            // Scan completed
        }
    }

    /**
     * Mutate the given filename to make it valid for a FAT filesystem,
     * replacing any invalid characters with "_". This method doesn't allow hidden files (starting
     * with a dot), but you can manually add it later.
     */
    fun buildValidFilename(origName: String): String {
        val name = origName.trim('.', ' ')
        if (name.isEmpty()) {
            return "(invalid)"
        }
        val sb = StringBuilder(name.length)
        name.forEach { c ->
            if (isValidFatFilenameChar(c)) {
                sb.append(c)
            } else {
                sb.append('_')
            }
        }
        // Even though vfat allows 255 UCS-2 chars, we might eventually write to
        // ext4 through a FUSE layer, so use that limit minus 15 reserved characters.
        return sb.toString().take(240)
    }

    /**
     * Returns true if the given character is a valid filename character, false otherwise.
     */
    private fun isValidFatFilenameChar(c: Char): Boolean {
        if (0x00.toChar() <= c && c <= 0x1f.toChar()) {
            return false
        }
        return when (c) {
            '"', '*', '/', ':', '<', '>', '?', '\\', '|', 0x7f.toChar() -> false
            else -> true
        }
    }

    /**
     * File operations for batch processing.
     */
    sealed class FileOperation {
        data class Delete(val file: File) : FileOperation()
        data class Copy(val source: File, val destination: File) : FileOperation()
        data class Move(val source: File, val destination: File) : FileOperation()
    }

    /**
     * Result of a file operation.
     */
    data class FileOperationResult(
        val operation: FileOperation,
        val success: Boolean,
        val bytesProcessed: Long
    )

    /**
     * Result of batch file operations.
     */
    data class BatchResult(
        val totalProcessed: Int,
        val totalSuccess: Int,
        val totalFreedSpace: Long,
        val results: List<FileOperationResult>
    ) {
        val successRate: Float get() = if (totalProcessed > 0) totalSuccess.toFloat() / totalProcessed else 0f
    }

    /**
     * Storage statistics for monitoring.
     */
    data class StorageStats(
        val externalTotal: Long,
        val externalAvailable: Long,
        val externalUsed: Long,
        val internalTotal: Long,
        val internalAvailable: Long,
        val internalUsed: Long
    ) {
        val externalUtilization: Double get() = if (externalTotal > 0) externalUsed.toDouble() / externalTotal else 0.0
        val internalUtilization: Double get() = if (internalTotal > 0) internalUsed.toDouble() / internalTotal else 0.0
        val totalAvailable: Long get() = externalAvailable + internalAvailable
        val totalUsed: Long get() = externalUsed + internalUsed
        val totalSpace: Long get() = externalTotal + internalTotal
    }
}
