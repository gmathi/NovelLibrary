package io.github.gmathi.novellibrary.util

import net.jqwik.api.Arbitraries
import net.jqwik.api.Arbitrary
import net.jqwik.api.ForAll
import net.jqwik.api.Property
import net.jqwik.api.Provide
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import io.github.gmathi.novellibrary.util.lang.writableFileName
import java.io.File

/**
 * Property-based tests for [Utils.buildNovelDir], the pure naming/rooting overload that
 * [Utils.getNovelDir] delegates to after resolving the active storage root.
 *
 * [Utils.getNovelDir] itself requires an Android `Context` (to resolve the active
 * [io.github.gmathi.novellibrary.util.storage.StorageLocationResolver] root), which is not
 * available under the plain JVM unit test runner. `buildNovelDir` was extracted as its own
 * overload taking the already-resolved root directly, mirroring the
 * Context-overload/pure-overload seam already used by `StorageLocationResolver`, so the naming
 * and rooting convention can be exercised here without Android.
 */
class UtilsGetNovelDirPropertyTest {

    /** Arbitrary novel name, including the empty string (exercises the generated-id substitution). */
    @Provide
    fun novelNames(): Arbitrary<String> =
        Arbitraries.oneOf(
            Arbitraries.just(""),
            Arbitraries.strings().withCharRange('a', 'z').ofMinLength(0).ofMaxLength(20),
            Arbitraries.strings().ofMinLength(0).ofMaxLength(20)
        )

    @Provide
    fun novelIds(): Arbitrary<Long> = Arbitraries.longs().between(0L, Long.MAX_VALUE / 2)

    // Feature: download-storage-location, Property 3: Novel directory is rooted under the active location
    @Property(tries = 100)
    fun novelDirIsRootedUnderTheGivenRootWithTheExpectedName(
        @ForAll("novelNames") novelName: String,
        @ForAll("novelIds") novelId: Long
    ) {
        val root = newTempDir()
        try {
            val novelDir = Utils.buildNovelDir(root, novelName, novelId)

            // The returned directory is rooted directly under the given root.
            assertEquals(root, novelDir.parentFile, "expected novelDir's parent to be the given root")

            val writableNovelName = novelName.writableFileName()
            if (writableNovelName.isNotEmpty()) {
                // Non-empty writable names produce the deterministic "<writableNovelName>-<novelId>" name.
                assertEquals(
                    "$writableNovelName-$novelId",
                    novelDir.name,
                    "expected dir name to follow \"<writableNovelName>-<novelId>\""
                )
            } else {
                // Empty (or fully-stripped) names are substituted with a generated id, but the
                // "-<novelId>" suffix convention still holds.
                assertTrue(
                    novelDir.name.endsWith("-$novelId"),
                    "expected generated-id dir name to still end with \"-$novelId\", got \"${novelDir.name}\""
                )
                assertTrue(
                    novelDir.name.length > "-$novelId".length,
                    "expected a non-empty generated name before the \"-$novelId\" suffix, got \"${novelDir.name}\""
                )
            }
        } finally {
            root.deleteRecursively()
        }
    }

    /** Creates a fresh, real temp directory for the duration of a single property iteration. */
    private fun newTempDir(): File {
        val marker = File.createTempFile("get-novel-dir-test-", "")
        val dir = File(marker.parentFile, "${marker.name}-dir")
        marker.delete()
        dir.mkdirs()
        return dir
    }
}
