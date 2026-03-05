package io.github.gmathi.novellibrary.settings.util

/**
 * Common validation functions for settings values.
 * 
 * Provides reusable validation logic to ensure settings values are within
 * acceptable ranges and formats before being persisted.
 */
object SettingsValidation {
    
    /**
     * Validates text size is within acceptable range.
     * 
     * @param size Text size in sp
     * @return Validated text size clamped to [12, 32]
     */
    fun validateTextSize(size: Int): Int {
        return size.coerceIn(12, 32)
    }
    
    /**
     * Validates scroll length is within acceptable range.
     * 
     * @param length Scroll length in pixels
     * @return Validated scroll length clamped to [50, 500]
     */
    fun validateScrollLength(length: Int): Int {
        return length.coerceIn(50, 500)
    }
    
    /**
     * Validates scroll interval is within acceptable range.
     * 
     * @param interval Scroll interval in milliseconds
     * @return Validated interval clamped to [500, 5000]
     */
    fun validateScrollInterval(interval: Int): Int {
        return interval.coerceIn(500, 5000)
    }
    
    /**
     * Validates backup frequency is within acceptable range.
     * 
     * @param hours Backup frequency in hours
     * @return Validated frequency clamped to [1, 168] (1 hour to 1 week)
     */
    fun validateBackupFrequency(hours: Int): Int {
        return hours.coerceIn(1, 168)
    }
    
    /**
     * Validates language code is supported.
     * 
     * @param languageCode ISO 639-1 language code
     * @return Validated language code, defaults to "en" if unsupported
     */
    fun validateLanguageCode(languageCode: String): String {
        val supportedLanguages = setOf(
            "en", "es", "fr", "de", "it", "pt", "ru", "ja", "ko", "zh"
        )
        return if (languageCode in supportedLanguages) languageCode else "en"
    }
    
    /**
     * Validates email format.
     * 
     * @param email Email address
     * @return True if email format is valid
     */
    fun isValidEmail(email: String): Boolean {
        if (email.isBlank()) return false
        val emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
        return emailRegex.matches(email)
    }
    
    /**
     * Validates color value is a valid ARGB integer.
     * 
     * @param color Color as ARGB integer
     * @return True if color is valid (non-zero)
     */
    fun isValidColor(color: Int): Boolean {
        return color != 0
    }
    
    /**
     * Validates file path is not empty and doesn't contain invalid characters.
     * 
     * @param path File path
     * @return True if path is valid
     */
    fun isValidFilePath(path: String): Boolean {
        if (path.isBlank()) return true // Empty path is valid (means default)
        val invalidChars = setOf('<', '>', ':', '"', '|', '?', '*')
        return path.none { it in invalidChars }
    }
    
    /**
     * Validates backup interval string.
     * 
     * @param interval Backup interval ("daily", "weekly", "monthly")
     * @return Validated interval, defaults to "daily" if invalid
     */
    fun validateBackupInterval(interval: String): String {
        val validIntervals = setOf("daily", "weekly", "monthly")
        return if (interval in validIntervals) interval else "daily"
    }
    
    /**
     * Validates internet type preference.
     * 
     * @param type Internet type ("wifi", "any")
     * @return Validated type, defaults to "wifi" if invalid
     */
    fun validateInternetType(type: String): String {
        val validTypes = setOf("wifi", "any")
        return if (type in validTypes) type else "wifi"
    }
    
    /**
     * Validates timestamp is not in the future.
     * 
     * @param timestamp Timestamp in milliseconds
     * @return Validated timestamp, clamped to current time if in future
     */
    fun validateTimestamp(timestamp: Long): Long {
        val now = System.currentTimeMillis()
        return if (timestamp > now) now else timestamp
    }
}
