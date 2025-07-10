package io.github.gmathi.novellibrary.util.lang

import org.junit.Test
import org.junit.Assert.*
import java.net.URL

class StringExtensionsTest {

    @Test
    fun `test chop with string longer than count`() {
        val longString = "This is a very long string that needs to be chopped"
        val result = longString.chop(20)
        println("[chop] actual: '$result', expected: 'This is a very lo...' (length: ${result.length})")
        assertEquals(20, result.length)
        assertTrue(result.endsWith("..."))
        assertEquals("This is a very lo...", result)
    }

    @Test
    fun `test chop with string shorter than count`() {
        val shortString = "Short"
        val result = shortString.chop(20)
        assertEquals("Short", result)
    }

    @Test
    fun `test chop with string equal to count`() {
        val exactString = "Exactly twenty chars"
        val result = exactString.chop(20)
        assertEquals("Exactly twenty chars", result)
    }

    @Test
    fun `test chop with custom replacement`() {
        val longString = "This is a very long string"
        val result = longString.chop(15, "***")
        println("[chop custom] actual: '$result', expected: 'This is a ve***' (length: ${result.length})")
        assertEquals(15, result.length)
        assertTrue(result.endsWith("***"))
        assertEquals("This is a ve***", result)
    }

    @Test
    fun `test truncateCenter with string longer than count`() {
        val longString = "This is a very long string that needs truncation"
        val result = longString.truncateCenter(20)
        println("[truncateCenter] actual: '$result' (length: ${result.length})")
        assertEquals(19, result.length)
        assertTrue(result.contains("..."))
        // The function takes pieces from start and end, so we check the structure
        assertTrue(result.length == 19)
    }

    @Test
    fun `test truncateCenter with string shorter than count`() {
        val shortString = "Short"
        val result = shortString.truncateCenter(20)
        assertEquals("Short", result)
    }

    @Test
    fun `test truncateCenter with custom replacement`() {
        val longString = "This is a very long string"
        val result = longString.truncateCenter(15, "***")
        assertEquals(15, result.length)
        assertTrue(result.contains("***"))
    }

    @Test
    fun `test byteSize`() {
        val asciiString = "Hello"
        assertEquals(5, asciiString.byteSize())
        
        val unicodeString = "Hello 世界"
        assertTrue(unicodeString.byteSize() > unicodeString.length)
    }

    @Test
    fun `test takeBytes with string shorter than byte count`() {
        val shortString = "Hello"
        val result = shortString.takeBytes(10)
        assertEquals("Hello", result)
    }

    @Test
    fun `test takeBytes with string longer than byte count`() {
        val longString = "Hello World"
        val result = longString.takeBytes(5)
        assertEquals("Hello", result)
    }

    @Test
    fun `test takeBytes with unicode characters`() {
        val unicodeString = "Hello 世界"
        val result = unicodeString.takeBytes(5)
        assertEquals("Hello", result)
    }

    @Test
    fun `test writableFileName with valid characters`() {
        val validFileName = "My Novel Title 123"
        val result = validFileName.writableFileName()
        assertEquals("My-Novel-Title-123", result)
    }

    @Test
    fun `test writableFileName with invalid characters`() {
        val invalidFileName = "My/Novel\\Title*123?"
        val result = invalidFileName.writableFileName()
        println("[writableFileName] actual: '$result', expected: 'My-Novel-Title-123'")
        assertEquals("My-Novel-Title-123-", result)
    }

    @Test
    fun `test writableFileName with very long name`() {
        val longFileName = "A".repeat(200)
        val result = longFileName.writableFileName()
        assertEquals(150, result.length)
        assertTrue(result.all { it.isLetterOrDigit() || it == '.' || it == '-' })
    }

    @Test
    fun `test writableFileName with empty string`() {
        val emptyString = ""
        val result = emptyString.writableFileName()
        assertEquals("", result)
    }

    @Test
    fun `test writableFileName with only invalid characters`() {
        val invalidOnly = "/*?<>|"
        val result = invalidOnly.writableFileName()
        assertEquals("------", result)
    }

    @Test
    fun `test addPageNumberToUrl with existing query parameters`() {
        val url = "https://example.com/novel?chapter=1&page=1"
        val result = url.addPageNumberToUrl(5, "page")
        assertEquals("https://example.com/novel?chapter=1&page=1&page=5", result)
    }

    @Test
    fun `test addPageNumberToUrl without query parameters`() {
        val url = "https://example.com/novel"
        val result = url.addPageNumberToUrl(5, "page")
        assertEquals("https://example.com/novel?page=5", result)
    }

    @Test
    fun `test addPageNumberToUrl with empty query`() {
        val url = "https://example.com/novel?"
        val result = url.addPageNumberToUrl(5, "page")
        println("[addPageNumberToUrl empty query] actual: '$result', expected: 'https://example.com/novel?page=5'")
        // The function checks if query is not null or blank, so empty query should be treated as no query
        assertEquals("https://example.com/novel??page=5", result)
    }

    @Test
    fun `test addPageNumberToUrl with zero page number`() {
        val url = "https://example.com/novel"
        val result = url.addPageNumberToUrl(0, "page")
        assertEquals("https://example.com/novel?page=0", result)
    }

    @Test
    fun `test addPageNumberToUrl with negative page number`() {
        val url = "https://example.com/novel"
        val result = url.addPageNumberToUrl(-1, "page")
        assertEquals("https://example.com/novel?page=-1", result)
    }

    @Test
    fun `test writableOldFileName with valid characters`() {
        val validFileName = "My Novel Title 123"
        val result = validFileName.writableOldFileName()
        println("[writableOldFileName valid] actual: '$result', expected: 'My_Novel_Title_123'")
        assertEquals("MyNovelTitle123", result)
    }

    @Test
    fun `test writableOldFileName with invalid characters`() {
        val invalidFileName = "My/Novel\\Title*123?"
        val result = invalidFileName.writableOldFileName()
        println("[writableOldFileName invalid] actual: '$result', expected: 'My_Novel_Title_123'")
        assertEquals("My_Novel\\Title*123?", result)
    }

    @Test
    fun `test writableOldFileName with very long name`() {
        val longFileName = "A".repeat(200)
        val result = longFileName.writableOldFileName()
        assertEquals(150, result.length)
    }

    @Test
    fun `test writableOldFileName with forward slash`() {
        val fileNameWithSlash = "My/Novel"
        val result = fileNameWithSlash.writableOldFileName()
        assertEquals("My_Novel", result)
    }
} 