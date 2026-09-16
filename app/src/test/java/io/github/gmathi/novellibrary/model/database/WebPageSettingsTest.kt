package io.github.gmathi.novellibrary.model.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the equals()/hashCode() contract of [WebPageSettings]: equality is defined by
 * (url, novelId) only, so the hash must not depend on the mutable fields. Before the fix,
 * two settings for the same chapter with different read state or metadata hashed differently,
 * which breaks HashSet/HashMap lookups and sumOf { it.hashCode() } change detection.
 */
class WebPageSettingsTest {

    private fun settings(url: String = "https://example.com/ch-1", novelId: Long = 7L) =
        WebPageSettings(url, novelId)

    @Test
    fun equalObjectsHaveEqualHashCodes_regardlessOfMutableFields() {
        val a = settings()
        val b = settings().apply {
            title = "Chapter 1"
            isRead = true
            filePath = "/data/novel/ch-1.html"
            redirectedUrl = "https://example.com/ch-1?redirected"
            metadata["key"] = "value"
        }

        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun hashCodeIsStableWhenMutableFieldsChangeAfterInsertion() {
        val item = settings()
        val set = HashSet<WebPageSettings>()
        set.add(item)

        item.isRead = true
        item.metadata["other"] = "changed"

        assertTrue("item must still be found after mutating non-key fields", set.contains(item))
        assertTrue(set.contains(settings()))
    }

    @Test
    fun differentKeysAreNotEqual() {
        assertNotEquals(settings(), settings(url = "https://example.com/ch-2"))
        assertNotEquals(settings(), settings(novelId = 8L))
        assertFalse(settings() == settings(novelId = 8L))
    }
}
