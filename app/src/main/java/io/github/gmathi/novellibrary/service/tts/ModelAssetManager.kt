package io.github.gmathi.novellibrary.service.tts

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Manages AI TTS model assets bundled in the APK.
 * 
 * Models are bundled in APK assets and copied to filesDir on first use.
 * Contains VITS-Piper model files: *.onnx, tokens.txt, espeak-ng-data/
 * 
 * Native JNI libraries (libonnxruntime.so, libsherpa-onnx-jni.so) are bundled
 * in the APK via jniLibs/.
 */
class ModelAssetManager(private val context: Context) {

    companion object {
        // Model file paths - using Kusal as default
        private const val MODEL_DIR = "vits-piper-en_US-kusal-medium"
        
        // Required model files (VITS-Piper doesn't need voices.bin)
        private val REQUIRED_MODEL_FILES = listOf(
            "tokens.txt"
        )
        
        // Required directories
        private const val ESPEAK_DATA_DIR = "espeak-ng-data"
    }

    enum class AssetStatus {
        NOT_DOWNLOADED,
        READY,
        CORRUPT
    }

    /**
     * Check if all model files are present and valid.
     * 
     * @return Current status of the asset pack
     */
    fun getAssetStatus(): AssetStatus {
        val modelDir = getModelDirectoryFile()
        
        if (!modelDir.exists()) {
            return AssetStatus.NOT_DOWNLOADED
        }
        
        // Check for any .onnx file (VITS-Piper model)
        val onnxFiles = modelDir.listFiles { file -> file.name.endsWith(".onnx") }
        val hasOnnxModel = onnxFiles?.isNotEmpty() == true
        
        // Check all required model files
        val allFilesPresent = REQUIRED_MODEL_FILES.all { fileName ->
            val file = File(modelDir, fileName)
            file.exists() && file.length() > 0
        }
        
        // Check espeak-ng-data directory
        val espeakDir = File(modelDir, ESPEAK_DATA_DIR)
        val espeakDataPresent = espeakDir.exists() && espeakDir.isDirectory && 
            (espeakDir.listFiles()?.isNotEmpty() == true)
        
        return if (hasOnnxModel && allFilesPresent && espeakDataPresent) {
            AssetStatus.READY
        } else {
            AssetStatus.CORRUPT
        }
    }

    /**
     * Get the directory path where model files are stored.
     * 
     * @return Absolute path to the model directory
     */
    fun getModelDirectory(): String {
        return getModelDirectoryFile().absolutePath
    }

    private fun getModelDirectoryFile(): File {
        return File(context.filesDir, MODEL_DIR)
    }
    
    /**
     * Copy model files from APK assets to filesDir if not already present.
     * This is needed because Sherpa-ONNX requires filesystem paths, not asset paths.
     * 
     * @return Result indicating success or failure
     */
    suspend fun copyModelsFromAssets(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val modelDir = getModelDirectoryFile()
            
            // If models already exist and are valid, skip copying
            if (getAssetStatus() == AssetStatus.READY) {
                android.util.Log.d("ModelAssetManager", "Models already present and valid, skipping copy")
                return@withContext Result.success(Unit)
            }
            
            android.util.Log.d("ModelAssetManager", "Starting model copy from assets to ${modelDir.absolutePath}")
            
            // Create model directory
            if (!modelDir.exists()) {
                val created = modelDir.mkdirs()
                android.util.Log.d("ModelAssetManager", "Created model directory: $created")
            }
            
            // Copy all files from assets/vits-piper-en_US-kusal-medium/ to filesDir
            val assetManager = context.assets
            val assetPath = MODEL_DIR
            
            // Check if asset path exists
            val rootFiles = assetManager.list(assetPath)
            if (rootFiles == null || rootFiles.isEmpty()) {
                throw IllegalStateException("Asset path '$assetPath' not found or empty in APK")
            }
            
            android.util.Log.d("ModelAssetManager", "Found ${rootFiles.size} items in assets/$assetPath")
            
            fun copyAssetFolder(assetPath: String, targetDir: File) {
                try {
                    val files = assetManager.list(assetPath) ?: emptyArray()
                    
                    for (filename in files) {
                        try {
                            val fullAssetPath = "$assetPath/$filename"
                            val targetFile = File(targetDir, filename)
                            
                            // Check if it's a directory by trying to list its contents
                            val subFiles = assetManager.list(fullAssetPath)
                            if (subFiles != null && subFiles.isNotEmpty()) {
                                // It's a directory
                                android.util.Log.d("ModelAssetManager", "Creating directory: ${targetFile.absolutePath}")
                                targetFile.mkdirs()
                                copyAssetFolder(fullAssetPath, targetFile)
                            } else {
                                // It's a file
                                android.util.Log.d("ModelAssetManager", "Copying file: $fullAssetPath -> ${targetFile.absolutePath}")
                                assetManager.open(fullAssetPath).use { input ->
                                    FileOutputStream(targetFile).use { output ->
                                        input.copyTo(output)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("ModelAssetManager", "Error copying $filename: ${e.message}", e)
                            throw e
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ModelAssetManager", "Error in copyAssetFolder for $assetPath: ${e.message}", e)
                    throw e
                }
            }
            
            copyAssetFolder(assetPath, modelDir)
            
            android.util.Log.d("ModelAssetManager", "Model copy completed, verifying...")
            
            // Verify the copy was successful
            val status = getAssetStatus()
            if (status != AssetStatus.READY) {
                throw IllegalStateException("Model files not valid after copying from assets. Status: $status")
            }
            
            android.util.Log.d("ModelAssetManager", "Model copy and verification successful")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("ModelAssetManager", "Failed to copy models from assets", e)
            Result.failure(e)
        }
    }

    /**
     * Get total disk space used by model files in bytes.
     * 
     * @return Total size in bytes
     */
    fun getAssetSizeBytes(): Long {
        return calculateDirectorySize(getModelDirectoryFile())
    }

    private fun calculateDirectorySize(directory: File): Long {
        if (!directory.exists()) return 0L
        
        var size = 0L
        directory.walkTopDown().forEach { file ->
            if (file.isFile) {
                size += file.length()
            }
        }
        return size
    }

    /**
     * Verify integrity of model files.
     * Checks that all required files are present with non-zero sizes.
     * 
     * @return true if all files are valid, false otherwise
     */
    fun verifyIntegrity(): Boolean {
        return getAssetStatus() == AssetStatus.READY
    }

    /**
     * Delete all model files from storage.
     * Models will be re-copied from APK assets on next use.
     * 
     * @return true if deletion was successful, false otherwise
     */
    fun deleteAssets(): Boolean {
        val modelDir = getModelDirectoryFile()
        
        if (modelDir.exists()) {
            return modelDir.deleteRecursively()
        }
        
        return true
    }
}
