package io.github.gmathi.novellibrary.service.tts

import android.content.Context
import android.os.Bundle
import android.support.v4.media.session.MediaControllerCompat
import android.util.Log
import io.github.gmathi.novellibrary.model.preference.DataCenter

/**
 * Helper class for switching AI TTS models.
 * Handles both the preference update and the service notification.
 */
object AiModelSwitcher {
    private const val TAG = "AiModelSwitcher"
    
    /**
     * Switch to a different AI TTS model.
     * This will:
     * 1. Update the preference
     * 2. Notify the TTS service to switch models
     * 3. Preload the new model in the background
     * 
     * @param context Application context
     * @param dataCenter DataCenter instance
     * @param newModelId The new model ID (e.g., "vits-piper-en_US-lessac-medium")
     * @param mediaController Optional media controller for active TTS session
     */
    fun switchModel(
        context: Context,
        dataCenter: DataCenter,
        newModelId: String,
        mediaController: MediaControllerCompat? = null
    ) {
        Log.i(TAG, "Switching AI model to: $newModelId")
        
        // Update preference
        dataCenter.ttsPreferences.aiModel = newModelId
        
        // If TTS is currently active, notify the service
        if (mediaController != null) {
            val extras = Bundle().apply {
                putString("modelId", newModelId)
            }
            mediaController.sendCommand(TTSService.ACTION_SWITCH_AI_MODEL, extras, null)
        } else {
            // If TTS is not active, just preload the new model
            val preloader = AiTtsPreloader.getInstance(context, dataCenter)
            preloader.switchModel(newModelId)
        }
    }
    
    /**
     * Get the currently selected model.
     * 
     * @param dataCenter DataCenter instance
     * @return Current model ID
     */
    fun getCurrentModel(dataCenter: DataCenter): String {
        return dataCenter.ttsPreferences.aiModel
    }
    
    /**
     * Get the display name for a model.
     * 
     * @param modelId Model ID
     * @return Display name
     */
    fun getModelDisplayName(modelId: String): String {
        return AiTtsModel.fromId(modelId).displayName
    }
    
    /**
     * Get all available models.
     * 
     * @return List of available models
     */
    fun getAvailableModels(): List<AiTtsModel> {
        return AiTtsModel.entries
    }
}
