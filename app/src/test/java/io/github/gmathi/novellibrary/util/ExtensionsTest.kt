package io.github.gmathi.novellibrary.util

import org.junit.Test
import org.junit.Assert.*
import io.github.gmathi.novellibrary.model.database.WebPageSettings
import io.github.gmathi.novellibrary.model.other.LinkedPage

class ExtensionsTest {

    @Test
    fun `test Float toHumanPercentage`() {
        assertEquals(50.0f, 0.5f.toHumanPercentage(), 0.001f)
        assertEquals(0.0f, 0.0f.toHumanPercentage(), 0.001f)
        assertEquals(100.0f, 1.0f.toHumanPercentage(), 0.001f)
        assertEquals(25.5f, 0.255f.toHumanPercentage(), 0.001f)
    }

    @Test
    fun `test Float fromHumanPercentage`() {
        assertEquals(0.5f, 50.0f.fromHumanPercentage(), 0.001f)
        assertEquals(0.0f, 0.0f.fromHumanPercentage(), 0.001f)
        assertEquals(1.0f, 100.0f.fromHumanPercentage(), 0.001f)
        assertEquals(0.255f, 25.5f.fromHumanPercentage(), 0.001f)
    }

    @Test
    fun `test Double toHumanPercentage`() {
        assertEquals(50.0, 0.5.toHumanPercentage(), 0.001)
        assertEquals(0.0, 0.0.toHumanPercentage(), 0.001)
        assertEquals(100.0, 1.0.toHumanPercentage(), 0.001)
        assertEquals(25.5, 0.255.toHumanPercentage(), 0.001)
    }

    @Test
    fun `test Double fromHumanPercentage`() {
        assertEquals(0.5, 50.0.fromHumanPercentage(), 0.001)
        assertEquals(0.0, 0.0.fromHumanPercentage(), 0.001)
        assertEquals(1.0, 100.0.fromHumanPercentage(), 0.001)
        assertEquals(0.255, 25.5.fromHumanPercentage(), 0.001)
    }

    @Test
    fun `test WebPageSettings getLinkedPagesCompat with new format`() {
        val webPageSettings = WebPageSettings("https://example.com", 1L)
        val linkedPagesJson = """[{"href":"https://example.com","label":"Example","isMainContent":true}]"""
        webPageSettings.metadata[Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES] = linkedPagesJson
        
        val result = webPageSettings.getLinkedPagesCompat()
        
        assertEquals(1, result.size)
        assertEquals("https://example.com", result[0].href)
        assertEquals("Example", result[0].label)
        assertTrue(result[0].isMainContent)
    }

    @Test
    fun `test WebPageSettings getLinkedPagesCompat with old format`() {
        val webPageSettings = WebPageSettings("https://example.com", 1L)
        val oldFormatJson = """["https://example1.com","https://example2.com"]"""
        webPageSettings.metadata[Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES] = oldFormatJson
        
        val result = webPageSettings.getLinkedPagesCompat()
        
        assertEquals(2, result.size)
        assertEquals("https://example1.com", result[0].href)
        assertEquals("legacy 0", result[0].label)
        assertFalse(result[0].isMainContent)
        assertEquals("https://example2.com", result[1].href)
        assertEquals("legacy 1", result[1].label)
        assertFalse(result[1].isMainContent)
    }

    @Test
    fun `test WebPageSettings getLinkedPagesCompat with empty metadata`() {
        val webPageSettings = WebPageSettings("https://example.com", 1L)
        
        val result = webPageSettings.getLinkedPagesCompat()
        
        assertEquals(0, result.size)
    }

    @Test
    fun `test WebPageSettings getLinkedPagesCompat with null metadata`() {
        val webPageSettings = WebPageSettings("https://example.com", 1L)
        webPageSettings.metadata[Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES] = null
        
        val result = webPageSettings.getLinkedPagesCompat()
        
        assertEquals(0, result.size)
    }

    @Test(expected = com.google.gson.JsonSyntaxException::class)
    fun `test WebPageSettings getLinkedPagesCompat with invalid JSON`() {
        val webPageSettings = WebPageSettings("https://example.com", 1L)
        webPageSettings.metadata[Constants.MetaDataKeys.OTHER_LINKED_WEB_PAGES] = "invalid json"
        
        webPageSettings.getLinkedPagesCompat()
    }
} 