package io.github.gmathi.novellibrary.viewmodel

import android.content.Context
import androidx.lifecycle.*
import io.github.gmathi.novellibrary.data.repository.FileManagementRepository
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import io.github.gmathi.novellibrary.util.Constants
import io.github.gmathi.novellibrary.util.Logs
import kotlinx.coroutines.launch
import uy.kohesive.injekt.injectLazy
import java.io.File
import java.util.*

class FileManagementViewModel : ViewModel() {
    
    companion object {
        const val TAG = "FileManagementViewModel"
    }

    private val fileRepository: FileManagementRepository by injectLazy()

    // UI State
    private val _uiState = MutableLiveData<FileManagementUiState>()
    val uiState: LiveData<FileManagementUiState> = _uiState

    // File operations state
    private val _fileOperationsState = MutableLiveData<FileOperationsState>()
    val fileOperationsState: LiveData<FileOperationsState> = _fileOperationsState

    // Storage information
    private val _storageInfo = MutableLiveData<StorageInfo>()
    val storageInfo: LiveData<StorageInfo> = _storageInfo

    // File metadata
    private val _fileMetadata = MutableLiveData<Map<String, Any>>()
    val fileMetadata: LiveData<Map<String, Any>> = _fileMetadata

    // Directory size
    private val _directorySize = MutableLiveData<Long>()
    val directorySize: LiveData<Long> = _directorySize

    // External storages
    private val _externalStorages = MutableLiveData<Collection<File>>()
    val externalStorages: LiveData<Collection<File>> = _externalStorages

    /**
     * Initialize the ViewModel
     */
    fun init(context: Context) {
        viewModelScope.launch {
            try {
                _uiState.value = FileManagementUiState.Loading
                
                // Load storage information
                loadStorageInfo(context)
                
                // Load external storages
                loadExternalStorages(context)
                
                _uiState.value = FileManagementUiState.Success
            } catch (e: Exception) {
                Logs.error(TAG, "Error initializing FileManagementViewModel", e)
                _uiState.value = FileManagementUiState.Error(e.message ?: "Unknown error occurred")
            }
        }
    }

    /**
     * Load storage information
     */
    fun loadStorageInfo(context: Context) {
        viewModelScope.launch {
            try {
                _fileOperationsState.value = FileOperationsState.Loading
                
                val isStorageAvailable = fileRepository.isStorageAvailable(context)
                val availableSpace = fileRepository.getAvailableStorageSpace(context)
                val totalSpace = fileRepository.getTotalStorageSpace(context)
                
                val storageInfo = StorageInfo(
                    isAvailable = isStorageAvailable,
                    availableSpace = availableSpace,
                    totalSpace = totalSpace,
                    usedSpace = totalSpace - availableSpace
                )
                
                _storageInfo.value = storageInfo
                _fileOperationsState.value = FileOperationsState.Success
                
            } catch (e: Exception) {
                Logs.error(TAG, "Error loading storage info", e)
                _fileOperationsState.value = FileOperationsState.Error(e.message ?: "Failed to load storage info")
            }
        }
    }

    /**
     * Load external storages
     */
    fun loadExternalStorages(context: Context) {
        viewModelScope.launch {
            try {
                val storages = fileRepository.getExternalStorages(context)
                _externalStorages.value = storages
            } catch (e: Exception) {
                Logs.error(TAG, "Error loading external storages", e)
            }
        }
    }

    /**
     * Get directory size
     */
    fun getDirectorySize(directory: File) {
        viewModelScope.launch {
            try {
                val size = fileRepository.getDirectorySize(directory)
                _directorySize.value = size
            } catch (e: Exception) {
                Logs.error(TAG, "Error getting directory size", e)
            }
        }
    }

    /**
     * Get file metadata
     */
    fun getFileMetadata(file: File) {
        viewModelScope.launch {
            try {
                val metadata = fileRepository.getFileMetadata(file)
                _fileMetadata.value = metadata
            } catch (e: Exception) {
                Logs.error(TAG, "Error getting file metadata", e)
            }
        }
    }

    /**
     * Delete downloaded chapters for a novel
     */
    fun deleteDownloadedChapters(context: Context, novel: Novel) {
        viewModelScope.launch {
            try {
                _fileOperationsState.value = FileOperationsState.Loading
                
                fileRepository.deleteDownloadedChapters(context, novel)
                
                _fileOperationsState.value = FileOperationsState.Success
                Logs.info(TAG, "Successfully deleted downloaded chapters for novel: ${novel.name}")
                
            } catch (e: Exception) {
                Logs.error(TAG, "Error deleting downloaded chapters", e)
                _fileOperationsState.value = FileOperationsState.Error(e.message ?: "Failed to delete downloaded chapters")
            }
        }
    }

    /**
     * Delete specific downloaded chapter
     */
    fun deleteDownloadedChapter(webPageSettings: WebPageSettings) {
        viewModelScope.launch {
            try {
                _fileOperationsState.value = FileOperationsState.Loading
                
                fileRepository.deleteDownloadedChapter(webPageSettings)
                
                _fileOperationsState.value = FileOperationsState.Success
                Logs.info(TAG, "Successfully deleted downloaded chapter")
                
            } catch (e: Exception) {
                Logs.error(TAG, "Error deleting downloaded chapter", e)
                _fileOperationsState.value = FileOperationsState.Error(e.message ?: "Failed to delete downloaded chapter")
            }
        }
    }

    /**
     * Copy file
     */
    fun copyFile(src: File, dst: File) {
        viewModelScope.launch {
            try {
                _fileOperationsState.value = FileOperationsState.Loading
                
                fileRepository.copyFile(src, dst)
                
                _fileOperationsState.value = FileOperationsState.Success
                Logs.info(TAG, "Successfully copied file from ${src.absolutePath} to ${dst.absolutePath}")
                
            } catch (e: Exception) {
                Logs.error(TAG, "Error copying file", e)
                _fileOperationsState.value = FileOperationsState.Error(e.message ?: "Failed to copy file")
            }
        }
    }

    /**
     * Create file if it doesn't exist
     */
    fun createFileIfNotExists(file: File) {
        viewModelScope.launch {
            try {
                _fileOperationsState.value = FileOperationsState.Loading
                
                fileRepository.createFileIfNotExists(file)
                
                _fileOperationsState.value = FileOperationsState.Success
                Logs.info(TAG, "Successfully created file: ${file.absolutePath}")
                
            } catch (e: Exception) {
                Logs.error(TAG, "Error creating file", e)
                _fileOperationsState.value = FileOperationsState.Error(e.message ?: "Failed to create file")
            }
        }
    }

    /**
     * Create directories if they don't exist
     */
    fun createDirectoriesIfNotExist(file: File) {
        viewModelScope.launch {
            try {
                _fileOperationsState.value = FileOperationsState.Loading
                
                fileRepository.createDirectoriesIfNotExist(file)
                
                _fileOperationsState.value = FileOperationsState.Success
                Logs.info(TAG, "Successfully created directories: ${file.absolutePath}")
                
            } catch (e: Exception) {
                Logs.error(TAG, "Error creating directories", e)
                _fileOperationsState.value = FileOperationsState.Error(e.message ?: "Failed to create directories")
            }
        }
    }

    /**
     * Clean up temporary files
     */
    fun cleanupTempFiles(context: Context) {
        viewModelScope.launch {
            try {
                _fileOperationsState.value = FileOperationsState.Loading
                
                fileRepository.cleanupTempFiles(context)
                
                _fileOperationsState.value = FileOperationsState.Success
                Logs.info(TAG, "Successfully cleaned up temporary files")
                
            } catch (e: Exception) {
                Logs.error(TAG, "Error cleaning up temporary files", e)
                _fileOperationsState.value = FileOperationsState.Error(e.message ?: "Failed to clean up temporary files")
            }
        }
    }

    /**
     * Zip directory
     */
    fun zipDirectory(sourceDir: File, zipFile: File) {
        viewModelScope.launch {
            try {
                _fileOperationsState.value = FileOperationsState.Loading
                
                fileRepository.zipDirectory(sourceDir, zipFile)
                
                _fileOperationsState.value = FileOperationsState.Success
                Logs.info(TAG, "Successfully zipped directory: ${sourceDir.absolutePath}")
                
            } catch (e: Exception) {
                Logs.error(TAG, "Error zipping directory", e)
                _fileOperationsState.value = FileOperationsState.Error(e.message ?: "Failed to zip directory")
            }
        }
    }

    /**
     * Unzip file
     */
    fun unzipFile(contentResolver: android.content.ContentResolver, zipUri: android.net.Uri, destinationDir: File) {
        viewModelScope.launch {
            try {
                _fileOperationsState.value = FileOperationsState.Loading
                
                fileRepository.unzipFile(contentResolver, zipUri, destinationDir)
                
                _fileOperationsState.value = FileOperationsState.Success
                Logs.info(TAG, "Successfully unzipped file")
                
            } catch (e: Exception) {
                Logs.error(TAG, "Error unzipping file", e)
                _fileOperationsState.value = FileOperationsState.Error(e.message ?: "Failed to unzip file")
            }
        }
    }

    /**
     * Check if file exists
     */
    fun checkFileExists(filePath: String) {
        viewModelScope.launch {
            try {
                val exists = fileRepository.fileExists(filePath)
                // You can add a LiveData for this if needed
                Logs.info(TAG, "File exists check: $filePath = $exists")
            } catch (e: Exception) {
                Logs.error(TAG, "Error checking file existence", e)
            }
        }
    }

    /**
     * Get novel directory
     */
    fun getNovelDirectory(context: Context, novelName: String, novelId: Long) {
        viewModelScope.launch {
            try {
                val directory = fileRepository.getNovelDirectory(context, novelName, novelId)
                // You can add a LiveData for this if needed
                Logs.info(TAG, "Novel directory: ${directory.absolutePath}")
            } catch (e: Exception) {
                Logs.error(TAG, "Error getting novel directory", e)
            }
        }
    }

    /**
     * Reset UI state
     */
    fun resetUiState() {
        _uiState.value = FileManagementUiState.Idle
        _fileOperationsState.value = FileOperationsState.Idle
    }

    /**
     * Reset file operations state
     */
    fun resetFileOperationsState() {
        _fileOperationsState.value = FileOperationsState.Idle
    }

    // UI State sealed classes
    sealed class FileManagementUiState {
        object Idle : FileManagementUiState()
        object Loading : FileManagementUiState()
        object Success : FileManagementUiState()
        data class Error(val message: String) : FileManagementUiState()
    }

    sealed class FileOperationsState {
        object Idle : FileOperationsState()
        object Loading : FileOperationsState()
        object Success : FileOperationsState()
        data class Error(val message: String) : FileOperationsState()
    }

    // Data classes
    data class StorageInfo(
        val isAvailable: Boolean,
        val availableSpace: Long,
        val totalSpace: Long,
        val usedSpace: Long
    ) {
        val usedPercentage: Float
            get() = if (totalSpace > 0) (usedSpace.toFloat() / totalSpace.toFloat()) * 100 else 0f
    }
} 