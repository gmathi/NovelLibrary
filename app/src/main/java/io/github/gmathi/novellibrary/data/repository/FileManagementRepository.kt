package io.github.gmathi.novellibrary.data.repository

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.os.EnvironmentCompat
import androidx.documentfile.provider.DocumentFile
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Logs
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.storage.DiskUtil
import io.github.gmathi.novellibrary.util.storage.getReadableSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.io.IOException
import java.util.*

class FileManagementRepository {
    
    companion object {
        const val TAG = "FileManagementRepository"
    }

    private val dbHelper: DBHelper by injectLazy()

    /**
     * Get novel directory for file storage
     */
    suspend fun getNovelDirectory(context: Context, novelName: String, novelId: Long): File = withContext(Dispatchers.IO) {
        Utils.getNovelDir(context, novelName, novelId)
    }

    /**
     * Get all external storage directories
     */
    suspend fun getExternalStorages(context: Context): Collection<File> = withContext(Dispatchers.IO) {
        DiskUtil.getExternalStorages(context)
    }

    /**
     * Get directory size in bytes
     */
    suspend fun getDirectorySize(directory: File): Long = withContext(Dispatchers.IO) {
        DiskUtil.getDirectorySize(directory)
    }

    /**
     * Get readable file size
     */
    suspend fun getReadableFileSize(file: File): String = withContext(Dispatchers.IO) {
        file.getReadableSize()
    }

    /**
     * Delete downloaded chapters for a novel
     */
    suspend fun deleteDownloadedChapters(context: Context, novel: Novel) = withContext(Dispatchers.IO) {
        try {
            Utils.deleteDownloadedChapters(context, novel)
            Logs.info(TAG, "Successfully deleted downloaded chapters for novel: ${novel.name}")
        } catch (e: Exception) {
            Logs.error(TAG, "Error deleting downloaded chapters for novel: ${novel.name}", e)
            throw e
        }
    }

    /**
     * Delete specific downloaded chapter
     */
    suspend fun deleteDownloadedChapter(webPageSettings: WebPageSettings) = withContext(Dispatchers.IO) {
        try {
            webPageSettings.filePath?.let { filePath ->
                val file = File(filePath)
                if (file.exists()) {
                    file.delete()
                    webPageSettings.filePath = null
                    
                    // Delete linked pages if they exist
                    if (webPageSettings.metadata.containsKey(Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES)) {
                        val linkedPages = webPageSettings.getLinkedPagesCompat()
                        linkedPages.forEach { linkedPage ->
                            val linkedWebPageSettings = dbHelper.getWebPageSettings(linkedPage.href)
                            linkedWebPageSettings?.filePath?.let { linkedFilePath ->
                                val linkedFile = File(linkedFilePath)
                                if (linkedFile.exists()) {
                                    linkedFile.delete()
                                    dbHelper.deleteWebPageSettings(linkedWebPageSettings.url)
                                }
                            }
                        }
                        webPageSettings.metadata[Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES] = "[]"
                    }
                    
                    dbHelper.updateWebPageSettings(webPageSettings)
                    Logs.info(TAG, "Successfully deleted downloaded chapter: $filePath")
                }
            }
        } catch (e: Exception) {
            Logs.error(TAG, "Error deleting downloaded chapter", e)
            throw e
        }
    }

    /**
     * Check if file exists
     */
    suspend fun fileExists(filePath: String): Boolean = withContext(Dispatchers.IO) {
        File(filePath).exists()
    }

    /**
     * Create file if it doesn't exist
     */
    suspend fun createFileIfNotExists(file: File) = withContext(Dispatchers.IO) {
        try {
            file.createFileIfNotExists()
        } catch (e: IOException) {
            Logs.error(TAG, "Error creating file: ${file.absolutePath}", e)
            throw e
        }
    }

    /**
     * Create directories if they don't exist
     */
    suspend fun createDirectoriesIfNotExist(file: File) = withContext(Dispatchers.IO) {
        try {
            file.createDirsIfNotExists()
        } catch (e: IOException) {
            Logs.error(TAG, "Error creating directories: ${file.absolutePath}", e)
            throw e
        }
    }

    /**
     * Get file URI with proper compatibility
     */
    suspend fun getFileUri(context: Context, file: File): Uri = withContext(Dispatchers.IO) {
        file.getUriCompat(context)
    }

    /**
     * Scan media file for gallery visibility
     */
    suspend fun scanMediaFile(context: Context, file: File) = withContext(Dispatchers.IO) {
        DiskUtil.scanMedia(context, file)
    }

    /**
     * Get hash key for disk storage
     */
    suspend fun getHashKeyForDisk(key: String): String = withContext(Dispatchers.IO) {
        DiskUtil.hashKeyForDisk(key)
    }

    /**
     * Copy file from source to destination
     */
    suspend fun copyFile(src: File, dst: File) = withContext(Dispatchers.IO) {
        try {
            Utils.copyFile(src, dst)
        } catch (e: IOException) {
            Logs.error(TAG, "Error copying file from ${src.absolutePath} to ${dst.absolutePath}", e)
            throw e
        }
    }

    /**
     * Copy file from input stream to destination
     */
    suspend fun copyFile(inputStream: java.io.InputStream, dst: File) = withContext(Dispatchers.IO) {
        try {
            Utils.copyFile(inputStream, dst)
        } catch (e: IOException) {
            Logs.error(TAG, "Error copying file to ${dst.absolutePath}", e)
            throw e
        }
    }

    /**
     * Copy file from source to DocumentFile
     */
    suspend fun copyFile(contentResolver: android.content.ContentResolver, src: File, dst: DocumentFile) = withContext(Dispatchers.IO) {
        try {
            Utils.copyFile(contentResolver, src, dst)
        } catch (e: IOException) {
            Logs.error(TAG, "Error copying file to DocumentFile", e)
            throw e
        }
    }

    /**
     * Get folder size
     */
    suspend fun getFolderSize(folder: File): Long = withContext(Dispatchers.IO) {
        Utils.getFolderSize(folder)
    }

    /**
     * Zip directory
     */
    suspend fun zipDirectory(sourceDir: File, zipFile: File) = withContext(Dispatchers.IO) {
        try {
            Utils.zip(sourceDir, zipFile)
        } catch (e: Exception) {
            Logs.error(TAG, "Error zipping directory: ${sourceDir.absolutePath}", e)
            throw e
        }
    }

    /**
     * Unzip file
     */
    suspend fun unzipFile(contentResolver: android.content.ContentResolver, zipUri: Uri, destinationDir: File) = withContext(Dispatchers.IO) {
        try {
            Utils.unzip(contentResolver, zipUri, destinationDir)
        } catch (e: Exception) {
            Logs.error(TAG, "Error unzipping file", e)
            throw e
        }
    }

    /**
     * Check if storage is available
     */
    suspend fun isStorageAvailable(context: Context): Boolean = withContext(Dispatchers.IO) {
        val externalStorages = getExternalStorages(context)
        externalStorages.isNotEmpty()
    }

    /**
     * Get available storage space
     */
    suspend fun getAvailableStorageSpace(context: Context): Long = withContext(Dispatchers.IO) {
        val externalStorages = getExternalStorages(context)
        externalStorages.sumOf { it.freeSpace }
    }

    /**
     * Get total storage space
     */
    suspend fun getTotalStorageSpace(context: Context): Long = withContext(Dispatchers.IO) {
        val externalStorages = getExternalStorages(context)
        externalStorages.sumOf { it.totalSpace }
    }

    /**
     * Clean up temporary files
     */
    suspend fun cleanupTempFiles(context: Context) = withContext(Dispatchers.IO) {
        try {
            val tempDir = File(context.cacheDir, "temp")
            if (tempDir.exists()) {
                tempDir.deleteRecursively()
            }
            Logs.info(TAG, "Successfully cleaned up temporary files")
        } catch (e: Exception) {
            Logs.error(TAG, "Error cleaning up temporary files", e)
            throw e
        }
    }

    /**
     * Get file metadata
     */
    suspend fun getFileMetadata(file: File): Map<String, Any> = withContext(Dispatchers.IO) {
        val metadata = mutableMapOf<String, Any>()
        
        if (file.exists()) {
            metadata["size"] = file.length()
            metadata["lastModified"] = Date(file.lastModified())
            metadata["isDirectory"] = file.isDirectory
            metadata["isFile"] = file.isFile
            metadata["canRead"] = file.canRead()
            metadata["canWrite"] = file.canWrite()
            metadata["absolutePath"] = file.absolutePath
            metadata["name"] = file.name
        }
        
        metadata
    }
} 