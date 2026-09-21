package io.github.gmathi.novellibrary.reader

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Unit tests for the "does the reader load from its offline file, or fall back to the web"
 * branch shared by the three Reader_Components: [io.github.gmathi.novellibrary.service.tts.TTSPlayer.loadFromFile],
 * [io.github.gmathi.novellibrary.service.ai_tts.AiTtsService.loadDocument], and
 * [io.github.gmathi.novellibrary.fragment.WebPageDBFragment.loadFromFile].
 *
 * None of these three methods are directly unit-testable in isolation: `TTSPlayer` requires a
 * live `MediaSessionCompat`/`NetworkHelper`/`DBHelper`/Android `Context`; `AiTtsService` is an
 * Android `Service` wired to `MediaSessionCompat`, `Glide`, and `DBHelper`; `WebPageDBFragment`
 * requires a `Fragment`/`WebView`/view binding. None of these Android framework collaborators
 * are mockable in this project's plain-JVM unit test setup (no Robolectric, no mocking framework
 * configured — see the existing boundary note in `DataCenterDownloadStorageLocationTest`).
 *
 * What *is* pure and shared across all three is the branch itself: build a `File` from the
 * absolute Stored_File_Path (each component uses an equivalent construction — see
 * [buildFileLikeTtsPlayerAndWebPageDBFragment] and [buildFileLikeAiTtsService] below, copied
 * verbatim from the respective `loadFromFile`/`loadDocument` methods) and check `File.exists()`
 * to decide "load from file" vs. "fall back to the web". This test extracts exactly that
 * existence-check + branch decision and exercises it against a real temp directory (standing in
 * for an arbitrary absolute path, e.g. one rooted on an SD card) and a real non-existent path,
 * asserting the correct branch is taken without needing to throw or touch any Android class.
 *
 * Validates: Requirements 4.1, 4.2, 7.3
 */
class ReaderFilePathFallbackTest {

    private companion object {
        private const val FILE_PROTOCOL = "file://"
    }

    /**
     * Mirrors the exact `File` construction used by `TTSPlayer.loadFromFile` and
     * `WebPageDBFragment.loadFromFile`:
     * ```
     * val internalFilePath = "$FILE_PROTOCOL${webPageSettings.filePath}"
     * val input = File(internalFilePath.substring(FILE_PROTOCOL.length))
     * ```
     */
    private fun buildFileLikeTtsPlayerAndWebPageDBFragment(filePath: String): File {
        val internalFilePath = "$FILE_PROTOCOL$filePath"
        return File(internalFilePath.substring(FILE_PROTOCOL.length))
    }

    /**
     * Mirrors the exact `File` construction used by `AiTtsService.loadDocument`:
     * ```
     * val input = File(filePath)
     * ```
     */
    private fun buildFileLikeAiTtsService(filePath: String): File = File(filePath)

    /** The branch decision itself, identical in shape to all three readers: `!input.exists()`. */
    private fun takesWebFallbackBranch(input: File): Boolean = !input.exists()

    // --- TTSPlayer / WebPageDBFragment style construction (file:// prefix round trip) ---

    @Test
    fun `TTSPlayer style file construction loads from an arbitrary absolute path (temp dir simulating SD)`() {
        val tempDir = newTempDir()
        try {
            val chapterFile = File(tempDir, "chapter.html")
            chapterFile.writeText("<html><body>offline chapter</body></html>")

            val input = buildFileLikeTtsPlayerAndWebPageDBFragment(chapterFile.absolutePath)

            assertTrue(input.exists(), "Expected file to exist at the arbitrary absolute path $input")
            assertFalse(takesWebFallbackBranch(input), "Existing file must not take the web-fallback branch")
            assertEquals(chapterFile.absolutePath, input.absolutePath)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `TTSPlayer style file construction takes the web-fallback branch for a non-existent path`() {
        val tempDir = newTempDir()
        try {
            val missingFile = File(tempDir, "does-not-exist-${System.nanoTime()}.html")
            assertFalse(missingFile.exists(), "Precondition failed: file unexpectedly exists")

            val input = buildFileLikeTtsPlayerAndWebPageDBFragment(missingFile.absolutePath)

            assertTrue(takesWebFallbackBranch(input), "Missing file must take the web-fallback branch")
        } finally {
            tempDir.deleteRecursively()
        }
    }

    // --- AiTtsService style construction (plain File(filePath), no protocol round trip) ---

    @Test
    fun `AiTtsService style file construction loads from an arbitrary absolute path (temp dir simulating SD)`() {
        val tempDir = newTempDir()
        try {
            val chapterFile = File(tempDir, "chapter.html")
            chapterFile.writeText("<html><body>offline chapter</body></html>")

            val input = buildFileLikeAiTtsService(chapterFile.absolutePath)

            assertTrue(input.exists(), "Expected file to exist at the arbitrary absolute path $input")
            assertFalse(takesWebFallbackBranch(input), "Existing file must not take the web-fallback branch")
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `AiTtsService style file construction takes the web-fallback branch for a non-existent path`() {
        val tempDir = newTempDir()
        try {
            val missingFile = File(tempDir, "does-not-exist-${System.nanoTime()}.html")
            assertFalse(missingFile.exists(), "Precondition failed: file unexpectedly exists")

            val input = buildFileLikeAiTtsService(missingFile.absolutePath)

            assertTrue(takesWebFallbackBranch(input), "Missing file must take the web-fallback branch")
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun `an unavailable root directory (simulating a removed SD volume) also takes the web-fallback branch`() {
        // Simulates Requirement 7.3: the volume itself is gone, not just one file within it.
        val tempDir = newTempDir()
        val missingRoot = File(tempDir, "sd-XXXX-XXXX-${System.nanoTime()}")
        val chapterUnderMissingRoot = File(missingRoot, "novel-1/chapter.html")
        try {
            assertFalse(missingRoot.exists(), "Precondition failed: simulated SD root unexpectedly exists")

            val input = buildFileLikeAiTtsService(chapterUnderMissingRoot.absolutePath)

            assertTrue(takesWebFallbackBranch(input), "Chapter under an unavailable root must take the web-fallback branch")
        } finally {
            tempDir.deleteRecursively()
        }
    }

    /** Creates a fresh, real temp directory for the duration of a single test. */
    private fun newTempDir(): File {
        val marker = File.createTempFile("reader-fallback-test-", "")
        val dir = File(marker.parentFile, "${marker.name}-dir")
        marker.delete()
        dir.mkdirs()
        return dir
    }
}
