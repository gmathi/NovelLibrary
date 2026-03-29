package io.github.gmathi.novellibrary.service.tts

import android.content.Context
import android.util.Log
import io.github.gmathi.novellibrary.model.preference.DataCenter
import kotlinx.coroutines.*

/**
 * Preloads AI TTS models in the background to eliminate first-use delay.
 * 
 * This class handles:
 * - Copying model files from assets to filesystem on first run
 * - Pre-initializing the TTS engine in the background
 * - Caching the initialized engine for immediate use
 */
class AiTtsPreloader(private val context: Context, private val dataCenter: DataCenter) {

    companion object {
        private const val TAG = "AiTtsPreloader"
        
        @Volatile
        private var instance: AiTtsPreloader? = null
        
        fun getInstance(context: Context, dataCenter: DataCenter): AiTtsPreloader {
            return instance ?: synchronized(this) {
                instance ?: AiTtsPreloader(context.applicationContext, dataCenter).also { instance = it }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /** Guards all mutations of preloadedPlayer / preloadedModelId / isPreloading. */
    private val stateLock = Any()
    private var preloadedPlayer: AiAudioPlayer? = null
    private var preloadedModelId: String? = null
    private var isPreloading = false

    /**
     * Switch to a different model and preload it.
     * This will:
     * 1. Clear the current preloaded player
     * 2. Start preloading the new model in the background
     * 
     * @param newModelId The new model to switch to
     */
    fun switchModel(newModelId: String) {
        Log.i(TAG, "Switching model from $preloadedModelId to $newModelId")
        
        // Clear old preloaded player
        clearPreloadedPlayer()
        
        // Start preloading the new model
        preloadModel(newModelId)
    }
    
    /**
     * Start preloading the AI TTS model in the background.
     * This should be called early in the app lifecycle (e.g., Application.onCreate).
     * 
     * @param modelId Model to preload (defaults to user's selected model)
     */
    fun preloadModel(modelId: String? = null) {
        val targetModelId = modelId ?: dataCenter.ttsPreferences.aiModel

        // Atomically check-and-set: skip if already preloading or already cached for this model.
        val shouldStart = synchronized(stateLock) {
            when {
                isPreloading -> { Log.d(TAG, "Preload already in progress, skipping"); false }
                preloadedPlayer != null && preloadedModelId == targetModelId -> { Log.d(TAG, "Model $targetModelId already preloaded"); false }
                else -> { isPreloading = true; true }
            }
        }
        if (!shouldStart) return

        Log.i(TAG, "Starting background preload of model: $targetModelId")

        scope.launch {
            try {
                val modelAssetManager = ModelAssetManager(context)

                // Step 1: Copy model files from assets if needed
                Log.d(TAG, "Checking if model files need to be copied...")
                val copyResult = modelAssetManager.copyModelsFromAssets(targetModelId)
                if (copyResult.isFailure) {
                    Log.e(TAG, "Failed to copy model files", copyResult.exceptionOrNull())
                    return@launch
                }

                // Step 2: Verify model files are ready
                val status = modelAssetManager.getAssetStatus(targetModelId)
                if (status != ModelAssetManager.AssetStatus.READY) {
                    Log.e(TAG, "Model files not ready after copy. Status: $status")
                    return@launch
                }

                // Step 3: Initialize the TTS engine
                Log.d(TAG, "Initializing TTS engine...")
                val modelDir = modelAssetManager.getModelDirectory(targetModelId)
                val player = AiAudioPlayer(modelDir)

                val error = player.init()
                if (error != null) {
                    Log.e(TAG, "Failed to initialize TTS engine: $error")
                    player.destroy()
                    return@launch
                }

                // Step 4: Atomically swap in the new player and destroy the old one.
                val oldPlayer = synchronized(stateLock) {
                    val old = preloadedPlayer
                    player.speed = dataCenter.ttsPreferences.aiSpeed
                    preloadedPlayer = player
                    preloadedModelId = targetModelId
                    old
                }
                oldPlayer?.destroy()

                Log.i(TAG, "Successfully preloaded model: $targetModelId")

            } catch (e: Exception) {
                Log.e(TAG, "Error during model preload", e)
            } finally {
                synchronized(stateLock) { isPreloading = false }
            }
        }
    }
    
    /**
     * Get the preloaded player if available and matches the requested model.
     * Returns null if not preloaded or model doesn't match.
     * 
     * IMPORTANT: The caller takes ownership of the returned player and must call destroy() when done.
     * 
     * @param modelId Model ID to retrieve (defaults to user's selected model)
     * @return Preloaded AiAudioPlayer or null
     */
    fun getPreloadedPlayer(modelId: String? = null): AiAudioPlayer? {
        val targetModelId = modelId ?: dataCenter.ttsPreferences.aiModel
        val player = synchronized(stateLock) {
            if (preloadedModelId == targetModelId && preloadedPlayer != null) {
                val p = preloadedPlayer
                preloadedPlayer = null
                preloadedModelId = null
                p
            } else null
        }
        if (player != null) Log.i(TAG, "Returning preloaded player for model: $targetModelId")
        else Log.d(TAG, "No preloaded player available for model: $targetModelId")
        return player
    }
    
    /**
     * Check if a model is currently preloaded and ready.
     * 
     * @param modelId Model ID to check (defaults to user's selected model)
     * @return true if preloaded and ready
     */
    fun isModelPreloaded(modelId: String? = null): Boolean {
        val targetModelId = modelId ?: dataCenter.ttsPreferences.aiModel
        return synchronized(stateLock) {
            preloadedModelId == targetModelId && preloadedPlayer != null
        }
    }
    
    /**
     * Clear the preloaded player and free resources.
     * Should be called when switching models or when the app is being destroyed.
     */
    fun clearPreloadedPlayer() {
        Log.d(TAG, "Clearing preloaded player")
        val player = synchronized(stateLock) {
            val p = preloadedPlayer
            preloadedPlayer = null
            preloadedModelId = null
            p
        }
        player?.destroy()
    }
    
    /**
     * Cancel any ongoing preload operation and clean up resources.
     */
    fun shutdown() {
        Log.d(TAG, "Shutting down preloader")
        scope.cancel()
        clearPreloadedPlayer()
    }
}
