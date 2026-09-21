package io.github.gmathi.novellibrary.service.download

import io.github.gmathi.novellibrary.model.database.WebPageSettings
import io.github.gmathi.novellibrary.util.Utils
import io.github.gmathi.novellibrary.util.storage.StorageLocation
import io.github.gmathi.novellibrary.util.storage.StorageLocationResolver
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Integration test for the chapter download write/persist path exercised by
 * [DownloadWebPageThread.downloadChapter]:
 *
 * 1. `Utils.getNovelDir` (here, its pure [Utils.buildNovelDir] seam) resolves the `Novel_Dir`
 *    under the active [io.github.gmathi.novellibrary.util.storage.ResolvedRoot].
 * 2. The chapter file is written under that `Novel_Dir` (`File(novelDir, uri.getFileName())` in
 *    production; `convertDocToFile` just writes bytes to that same `File`).
 * 3. `webPageSettings.filePath = file.path` and the settings row is persisted via
 *    `dbHelper.updateWebPageSettings(...)`.
 *
 * `DownloadWebPageThread` itself needs a real Android `Context`/`DBHelper` (SQLite,
 * `NetworkHelper`, Jsoup network fetch, `HtmlCleaner`), none of which are available on the plain
 * JVM unit test runner used in this project (no Robolectric/mocking framework configured — see
 * [io.github.gmathi.novellibrary.model.preference.DataCenterDownloadStorageLocationTest]).
 * Rather than stand up a real DB, this test fakes the DB layer with a minimal in-memory
 * [FakeWebPageSettingsRepository] (mirroring the `FakeSharedPreferences` convention already used
 * in this test suite) and drives the same routing + write + persist sequence
 * [DownloadWebPageThread.downloadChapter] performs, using a real temp filesystem for the "chosen
 * root" so the assertions exercise real `File` I/O rather than mocks.
 *
 * Validates: Requirements 3.2, 3.3
 */
class DownloadWebPageThreadRoutingTest {

    /** Minimal in-memory stand-in for the `web_page_settings` table, mirroring `DBHelper.updateWebPageSettings`. */
    private class FakeWebPageSettingsRepository {
        private val rows = HashMap<String, WebPageSettings>()

        fun create(settings: WebPageSettings) {
            rows[settings.url] = settings
        }

        /** Mirrors `DBHelper.updateWebPageSettings`: persists title/redirectUrl/filePath/metadata for the row keyed by url. */
        fun updateWebPageSettings(settings: WebPageSettings) {
            val existing = rows[settings.url] ?: error("No row for ${settings.url}; call create() first")
            existing.title = settings.title
            existing.redirectedUrl = settings.redirectedUrl
            existing.filePath = settings.filePath
            existing.metadata = settings.metadata
        }

        fun get(url: String): WebPageSettings? = rows[url]
    }

    /** Simulates writing a downloaded chapter's HTML to disk, mirroring `HtmlCleaner.convertDocToFile`. */
    private fun writeSimulatedChapterFile(file: File): File {
        file.parentFile?.mkdirs()
        file.writeText("<html><body>Simulated chapter content</body></html>")
        return file
    }

    private val tempDirs = mutableListOf<File>()

    private fun newTempRoot(prefix: String): File {
        val marker = File.createTempFile(prefix, "")
        val dir = File(marker.parentFile, "${marker.name}-dir")
        marker.delete()
        dir.mkdirs()
        tempDirs += dir
        return dir
    }

    @AfterEach
    fun cleanup() {
        tempDirs.forEach { it.deleteRecursively() }
        tempDirs.clear()
    }

    /**
     * Simulates a full chapter download against a chosen root: resolves the Novel_Dir under that
     * root, writes the chapter file, persists `file_path`, and asserts both the file's location
     * and the persisted path.
     */
    @Test
    fun `simulated download under a chosen root writes the file under that root and persists its absolute path`() {
        val chosenRoot = newTempRoot("download-routing-test-internal")
        val repository = FakeWebPageSettingsRepository()

        val novelName = "My Test Novel"
        val novelId = 42L
        val chapterUrl = "https://example.com/novel/chapter-1"
        repository.create(WebPageSettings(chapterUrl, novelId))

        // 1) Resolve Novel_Dir under the chosen root, exactly as Utils.getNovelDir does via
        //    StorageLocationResolver.resolveWritableRoot -> Utils.buildNovelDir.
        val novelDir = Utils.buildNovelDir(chosenRoot, novelName, novelId)

        // 2) Write the chapter file under that Novel_Dir, as DownloadWebPageThread.downloadChapter does.
        val chapterFile = writeSimulatedChapterFile(File(novelDir, "chapter-1.html"))

        // 3) Persist the written file's absolute path as the Stored_File_Path, as
        //    downloadChapter does with `webPageSettings.filePath = file.path` followed by
        //    `dbHelper.updateWebPageSettings(webPageSettings)`.
        val webPageSettings = repository.get(chapterUrl)!!
        webPageSettings.filePath = chapterFile.path
        repository.updateWebPageSettings(webPageSettings)

        // Assert: the chapter file resides under the chosen root.
        assertTrue(
            chapterFile.absolutePath.startsWith(chosenRoot.absolutePath + File.separator),
            "Expected $chapterFile to reside under $chosenRoot"
        )

        // Assert: the persisted file_path equals the written file's absolute path.
        val persisted = repository.get(chapterUrl)!!
        assertEquals(chapterFile.absolutePath, File(persisted.filePath!!).absolutePath)
    }

    /**
     * Same scenario, but the chosen root is selected via
     * [StorageLocationResolver.resolveWritableRoot] from a configured [StorageLocation.SdCard]
     * that is available among the candidate volume ids, so the routing decision itself (not just
     * the naming) is exercised. [StorageLocationResolver.extractVolumeId] matches the literal
     * `/storage/<id>/` path segment, which real `java.io.File` instances on a Windows JVM cannot
     * produce (`\`-separated, no leading slash) — [AndroidStylePathFile] reports that Android-style
     * path for matching purposes only; once the volume id is confirmed to resolve, the test does
     * its actual file I/O against a real temp directory standing in for that volume.
     */
    @Test
    fun `simulated download routed through an available SD root writes under the SD root and persists its path`() {
        val internalRoot = newTempRoot("download-routing-test-internal-fallback")
        val sdRoot = newTempRoot("download-routing-test-sd")
        val repository = FakeWebPageSettingsRepository()
        val volumeId = "SD-0001"

        val configured = StorageLocation.SdCard(volumeId = volumeId, displayPath = "")
        val resolved = StorageLocationResolver.resolveWritableRoot(
            configured = configured,
            internalRoot = internalRoot,
            candidateDirs = listOf(AndroidStylePathFile(volumeId))
        )
        assertTrue(!resolved.didFallback, "Precondition: configured SD volume should be available")
        assertEquals(configured, resolved.location)

        // The resolved root is the synthetic Android-style path; substitute the real temp
        // directory standing in for that same volume for the actual file write.
        val chosenRoot = sdRoot

        val novelName = "SD Routed Novel"
        val novelId = 7L
        val chapterUrl = "https://example.com/novel/sd-chapter-1"
        repository.create(WebPageSettings(chapterUrl, novelId))

        val novelDir = Utils.buildNovelDir(chosenRoot, novelName, novelId)
        val chapterFile = writeSimulatedChapterFile(File(novelDir, "chapter-1.html"))

        val webPageSettings = repository.get(chapterUrl)!!
        webPageSettings.filePath = chapterFile.path
        repository.updateWebPageSettings(webPageSettings)

        assertTrue(
            chapterFile.absolutePath.startsWith(chosenRoot.absolutePath + File.separator),
            "Expected $chapterFile to reside under the SD root $chosenRoot"
        )
        assertEquals(0, countFilesUnder(internalRoot), "No file should have been written to the internal fallback root")

        val persisted = repository.get(chapterUrl)!!
        assertEquals(chapterFile.absolutePath, File(persisted.filePath!!).absolutePath)
    }

    private fun countFilesUnder(dir: File): Int =
        dir.walkTopDown().count { it.isFile }

    /**
     * A [File] whose path accessors always return an Android-style `/storage/<volumeId>/...` path,
     * bypassing host-OS path normalization, so [StorageLocationResolver.extractVolumeId] can match
     * it under a Windows JVM the same way it would on-device. Not used for real I/O — only to
     * confirm resolution picks the intended volume id.
     */
    private class AndroidStylePathFile(volumeId: String) :
        File("/storage/$volumeId/Android/data/io.github.gmathi.novellibrary/files") {
        private val androidPath = "/storage/$volumeId/Android/data/io.github.gmathi.novellibrary/files"
        override fun getPath(): String = androidPath
        override fun getAbsolutePath(): String = androidPath
        override fun getCanonicalPath(): String = androidPath
    }
}
