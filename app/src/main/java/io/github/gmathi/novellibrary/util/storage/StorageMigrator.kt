package io.github.gmathi.novellibrary.util.storage

import android.content.Context
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.getAllNovels
import io.github.gmathi.novellibrary.database.getAllWebPageSettings
import io.github.gmathi.novellibrary.database.updateWebPageSettings
import io.github.gmathi.novellibrary.model.preference.DataCenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/**
 * Coordinates moving downloaded chapters when the active [StorageLocation] changes.
 *
 * Migration moves whole `Novel_Dir`s (not individual files) from the previous root to the newly
 * selected one so that co-located HTML/CSS/image resources stay together (see design Decision 3).
 * Each chapter's stored `file_path` is only committed after its file is confirmed present at the
 * destination, so an interrupted migration never leaves a chapter unreadable (design Decision 4).
 */
class StorageMigrator(
    private val context: Context,
    private val dbHelper: DBHelper,
    private val dataCenter: DataCenter
) {

    /** Outcome of a [migrate] call. */
    sealed class MigrationResult {
        /**
         * All Novel_Dirs were moved and every chapter path rewritten. [skippedDirs] counts
         * directories under the source root that did not resolve to a novel row and were therefore
         * left untouched (e.g. AI TTS models, espeak data, legacy host-based folders).
         */
        data class Success(val movedNovels: Int, val movedFiles: Int, val skippedDirs: Int) : MigrationResult()

        /** The destination did not have enough free space; nothing was touched. */
        data class InsufficientSpace(val requiredBytes: Long, val availableBytes: Long) : MigrationResult()

        /** Migration was interrupted after some files were committed to the new location. */
        data class PartialFailure(val movedFiles: Int, val failedFiles: Int, val skippedDirs: Int) : MigrationResult()
    }

    /**
     * Move all existing Novel_Dirs from [from] to [to], rewriting each chapter's `file_path`.
     * Emits progress (0..total). Sets the active location to [to] on success.
     *
     * Caller must hold the download lock while this runs.
     */
    suspend fun migrate(
        from: ResolvedRoot,
        to: ResolvedRoot,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): MigrationResult = withContext(Dispatchers.IO) {
        // Space precheck (Requirements 5.6, 6): touch nothing when there isn't enough room.
        val validNovelIds = dbHelper.getAllNovels().map { it.id }.toHashSet()
        val required = estimateRequiredBytes(from.root, validNovelIds)
        val available = DiskUtil.getAvailableSpace(to.root)
        if (required > available) {
            return@withContext MigrationResult.InsufficientSpace(requiredBytes = required, availableBytes = available)
        }

        // Only directories that resolve to a known novel row are migrated. Everything else under
        // the source root (AI TTS models, espeak data, the shared images cache, and legacy
        // host-based folders whose names don't parse to a novelId) is LEFT IN PLACE — moving it
        // blind relocated 300+ MB of model data and deleted the source while the DB still pointed
        // at the old path. See issues #275 and #276.
        val allDirs = from.root.listFiles()?.filter { it.isDirectory } ?: emptyList()
        val sourceNovelDirs = allDirs.filter { dir ->
            val id = dir.name.substringAfterLast('-').toLongOrNull()
            id != null && validNovelIds.contains(id)
        }
        val skippedDirs = allDirs.size - sourceNovelDirs.size
        val total = sourceNovelDirs.size

        var movedNovels = 0
        var movedFiles = 0
        var failedFiles = 0
        var interrupted = false

        isMigrationInProgress = true
        try {
            for (sourceNovelDir in sourceNovelDirs) {
                try {
                    val destNovelDir = File(to.root, sourceNovelDir.name)
                    // Guaranteed non-null: sourceNovelDirs only holds dirs that parsed to a valid id.
                    val novelId = sourceNovelDir.name.substringAfterLast('-').toLong()

                    // Fast path: same-filesystem atomic rename.
                    val renamed = sourceNovelDir.renameTo(destNovelDir)

                    if (!renamed) {
                        // Cross-filesystem fallback: copy every file, then commit paths, then delete source.
                        destNovelDir.mkdirs()
                        copyDirectoryContents(sourceNovelDir, destNovelDir)
                    }

                    // Commit each affected chapter's file_path only after its destination file exists.
                    run {
                        val webPageSettingsList = dbHelper.getAllWebPageSettings(novelId)
                        for (webPageSettings in webPageSettingsList) {
                            val oldPath = webPageSettings.filePath ?: continue
                            val newPath = resolveMovedChapterPath(from.root, to.root, destNovelDir, oldPath)
                            if (newPath != null && File(newPath).exists()) {
                                webPageSettings.filePath = newPath
                                dbHelper.updateWebPageSettings(webPageSettings)
                                movedFiles++
                            } else {
                                // The file was physically relocated with its Novel_Dir but its stored
                                // path could not be rewritten to a file that now exists — leaving the
                                // DB pointing at a location the reader can no longer open. Count it as
                                // a failure so migrate() never reports Success while orphaning chapters.
                                failedFiles++
                            }
                        }
                    }

                    // Only after paths are committed, remove the now-empty source dir (already gone
                    // in the renameTo fast path).
                    if (!renamed && sourceNovelDir.exists()) {
                        sourceNovelDir.deleteRecursively()
                    }

                    movedNovels++
                } catch (e: Exception) {
                    // Stop processing further Novel_Dirs; already-committed chapters remain committed
                    // and untouched ones remain at the old location (Property 7 / Req 5 AC 5).
                    interrupted = true
                    break
                } finally {
                    onProgress(movedNovels, total)
                }
            }
        } finally {
            isMigrationInProgress = false
        }

        if (interrupted) {
            return@withContext MigrationResult.PartialFailure(movedFiles = movedFiles, failedFiles = failedFiles, skippedDirs = skippedDirs)
        }

        if (failedFiles > 0) {
            return@withContext MigrationResult.PartialFailure(movedFiles = movedFiles, failedFiles = failedFiles, skippedDirs = skippedDirs)
        }

        StorageLocationResolver.setConfiguredLocation(dataCenter, to.location)
        MigrationResult.Success(movedNovels = movedNovels, movedFiles = movedFiles, skippedDirs = skippedDirs)
    }

    /** Recursively copies every file under [sourceDir] into [destDir], preserving relative structure. */
    private fun copyDirectoryContents(sourceDir: File, destDir: File) {
        val children = sourceDir.listFiles() ?: return
        for (child in children) {
            val target = File(destDir, child.name)
            if (child.isDirectory) {
                target.mkdirs()
                copyDirectoryContents(child, target)
            } else {
                child.copyTo(target, overwrite = true)
                if (!target.exists() || target.length() != child.length()) {
                    throw IOException("Failed to copy ${child.absolutePath} to ${target.absolutePath}")
                }
            }
        }
    }

    /**
     * Bytes needed to move all current downloads from [from]. Only directories that resolve to a
     * known novel id in [validNovelIds] are counted, matching exactly what [migrate] relocates —
     * so the precheck never demands headroom for bytes that will not move (e.g. AI TTS models).
     */
    fun estimateRequiredBytes(from: File, validNovelIds: Set<Long>): Long {
        val novelDirs = from.listFiles() ?: return 0L
        var total = 0L
        for (novelDir in novelDirs) {
            if (!novelDir.isDirectory) continue
            val id = novelDir.name.substringAfterLast('-').toLongOrNull()
            if (id == null || !validNovelIds.contains(id)) continue
            total += DiskUtil.getDirectorySize(novelDir)
        }
        return total
    }

    companion object {
        /**
         * True while a [migrate] call is in progress. Checked by `DownloadWebPageThread`/the
         * download service (task 8.1) so no new download starts while files are being relocated
         * (Requirement 5.3). Always released in a `finally` block, including on cancellation.
         */
        @Volatile
        var isMigrationInProgress: Boolean = false
            private set

        /**
         * Compute the destination path for a chapter file currently located under [oldRoot],
         * replacing the [oldRoot] prefix with [newRoot] while preserving the
         * `"<novelDir>/<fileName>"` suffix, so co-located resources keep resolving relative to the
         * moved Novel_Dir.
         *
         * Returns null if [filePath] is not actually located under [oldRoot].
         */
        fun computeDestinationPath(oldRoot: File, newRoot: File, filePath: String): String? {
            val oldRootPath = oldRoot.absolutePath
            val file = File(filePath)
            val absoluteFilePath = file.absolutePath
            val oldRootWithSeparator = if (oldRootPath.endsWith(File.separator)) oldRootPath else oldRootPath + File.separator
            if (!absoluteFilePath.startsWith(oldRootWithSeparator)) return null
            val suffix = absoluteFilePath.substring(oldRootWithSeparator.length)
            return File(newRoot, suffix).absolutePath
        }

        /**
         * Resolve where a chapter file lives after its Novel_Dir was moved to [destNovelDir].
         *
         * The whole Novel_Dir was relocated to [destNovelDir] (see [migrate]), so the file's new
         * location is deterministic regardless of how its OLD [filePath] was rooted. This is what
         * makes migration correct for chapters that were downloaded BEFORE the storage-location
         * feature existed, whose stored paths do not string-prefix-match the current [oldRoot]
         * (legacy host-based layout, or `/data/data` vs `/data/user/0` filesDir drift). For those,
         * the fragile prefix swap in [computeDestinationPath] returns null and the DB row would be
         * left pointing at the now-empty old location.
         *
         * Strategy, first hit wins:
         *  1. `<destNovelDir>/<fileName>` when it exists — the file moved with its dir.
         *  2. The prefix-swap result of [computeDestinationPath] when it exists — same-root moves
         *     where the file sits in a subdirectory of the Novel_Dir.
         *  3. `<destNovelDir>/<fileName>` unconditionally — best-effort commit so the DB tracks the
         *     new root even if the existence probe raced; the caller still verifies existence.
         */
        fun resolveMovedChapterPath(oldRoot: File, newRoot: File, destNovelDir: File, filePath: String): String? {
            val fileName = File(filePath).name
            if (fileName.isEmpty()) return null

            val byMovedDir = File(destNovelDir, fileName)
            if (byMovedDir.exists()) return byMovedDir.absolutePath

            val byPrefixSwap = computeDestinationPath(oldRoot, newRoot, filePath)
            if (byPrefixSwap != null && File(byPrefixSwap).exists()) return byPrefixSwap

            return byMovedDir.absolutePath
        }
    }
}
