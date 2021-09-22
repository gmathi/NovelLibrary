package io.github.gmathi.novellibrary.service.tts

interface TTSEventListener {
    fun onReadingStart()
    fun onSentenceChange(sentenceIndex:Int)
    fun onReadingStop()
    fun onPlaybackStateChange()
    fun onChapterLoadStart()
    fun onChapterLoadStop()
}