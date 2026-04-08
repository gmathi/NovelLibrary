package io.github.gmathi.novellibrary.service.ai_tts

sealed class AiTtsPlaybackState {
    object Idle : AiTtsPlaybackState()
    /** Model files are being downloaded from the network before playback can begin. */
    object DownloadingModel : AiTtsPlaybackState()
    object LoadingModel : AiTtsPlaybackState()
    object Playing : AiTtsPlaybackState()
    object Paused : AiTtsPlaybackState()
    object Stopped : AiTtsPlaybackState()
    data class Error(val message: String) : AiTtsPlaybackState()
}

interface AiTtsEventListener {
    fun onSentenceChanged(sentenceIndex: Int, sentence: String)
    fun onPlaybackStateChanged(state: AiTtsPlaybackState)
    fun onChapterChanged(chapterIndex: Int)
    fun onError(message: String)
}
