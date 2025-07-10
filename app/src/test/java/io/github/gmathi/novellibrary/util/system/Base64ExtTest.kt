package io.github.gmathi.novellibrary.util.system

import org.junit.Test
import org.junit.Assert.*

class Base64ExtTest {

    @Test
    fun `test encodeBase64ToString with simple string`() {
        val input = "Hello, World!"
        val result = input.encodeBase64ToString()
        
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        // Base64 encoded "Hello, World!" should be "SGVsbG8sIFdvcmxkIQ=="
        assertEquals("SGVsbG8sIFdvcmxkIQ==", result)
    }

    @Test
    fun `test encodeBase64ToString with empty string`() {
        val input = ""
        val result = input.encodeBase64ToString()
        
        assertNotNull(result)
        assertEquals("", result)
    }

    @Test
    fun `test encodeBase64ToString with unicode string`() {
        val input = "Hello 世界"
        val result = input.encodeBase64ToString()
        
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        // Should be valid base64
        assertTrue(result.matches(Regex("^[A-Za-z0-9+/]*={0,2}$")))
    }

    @Test
    fun `test encodeBase64ToByteArray with simple string`() {
        val input = "Hello, World!"
        val result = input.encodeBase64ToByteArray()
        
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        // Should be valid base64 bytes
        val decoded = String(result.decodeBase64())
        assertEquals(input, decoded)
    }

    @Test
    fun `test encodeBase64ToByteArray with empty string`() {
        val input = ""
        val result = input.encodeBase64ToByteArray()
        
        assertNotNull(result)
        assertEquals(0, result.size)
    }

    @Test
    fun `test encodeBase64ToString with byte array`() {
        val input = "Test data".toByteArray()
        val result = input.encodeBase64ToString()
        
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        // Should be valid base64
        assertTrue(result.matches(Regex("^[A-Za-z0-9+/]*={0,2}$")))
    }

    @Test
    fun `test encodeBase64ToString with empty byte array`() {
        val input = ByteArray(0)
        val result = input.encodeBase64ToString()
        
        assertNotNull(result)
        assertEquals("", result)
    }

    @Test
    fun `test decodeBase64 with simple string`() {
        val input = "SGVsbG8sIFdvcmxkIQ==" // Base64 encoded "Hello, World!"
        val result = input.decodeBase64()
        
        assertNotNull(result)
        assertEquals("Hello, World!", result)
    }

    @Test
    fun `test decodeBase64 with empty string`() {
        val input = ""
        val result = input.decodeBase64()
        
        assertNotNull(result)
        assertEquals("", result)
    }

    @Test
    fun `test decodeBase64 with unicode string`() {
        val original = "Hello 世界"
        val encoded = original.encodeBase64ToString()
        val result = encoded.decodeBase64()
        
        assertEquals(original, result)
    }

    @Test
    fun `test decodeBase64ToByteArray with simple string`() {
        val input = "SGVsbG8sIFdvcmxkIQ==" // Base64 encoded "Hello, World!"
        val result = input.decodeBase64ToByteArray()
        
        assertNotNull(result)
        assertEquals("Hello, World!", String(result))
    }

    @Test
    fun `test decodeBase64ToByteArray with empty string`() {
        val input = ""
        val result = input.decodeBase64ToByteArray()
        
        assertNotNull(result)
        assertEquals(0, result.size)
    }

    @Test
    fun `test decodeBase64ToString with byte array`() {
        val original = "Test data"
        val encoded = original.toByteArray().encodeBase64()
        val result = encoded.decodeBase64ToString()
        
        assertEquals(original, result)
    }

    @Test
    fun `test decodeBase64ToString with empty byte array`() {
        val input = ByteArray(0)
        val result = input.decodeBase64ToString()
        
        assertNotNull(result)
        assertEquals("", result)
    }

    @Test
    fun `test encodeBase64 with byte array`() {
        val input = "Hello, World!".toByteArray()
        val result = input.encodeBase64()
        
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        // Should be valid base64
        assertTrue(String(result).matches(Regex("^[A-Za-z0-9+/]*={0,2}$")))
    }

    @Test
    fun `test encodeBase64 with empty byte array`() {
        val input = ByteArray(0)
        val result = input.encodeBase64()
        
        assertNotNull(result)
        assertEquals(0, result.size)
    }

    @Test
    fun `test decodeBase64 with byte array`() {
        val original = "Test data"
        val encoded = original.toByteArray().encodeBase64()
        val result = encoded.decodeBase64()
        
        assertEquals(original, String(result))
    }

    @Test
    fun `test decodeBase64 with empty byte array`() {
        val input = ByteArray(0)
        val result = input.decodeBase64()
        
        assertNotNull(result)
        assertEquals(0, result.size)
    }

    @Test
    fun `test round trip encoding and decoding`() {
        val original = "This is a test string with special characters: !@#$%^&*()_+-=[]{}|;':\",./<>?"
        val encoded = original.encodeBase64ToString()
        val decoded = encoded.decodeBase64()
        
        assertEquals(original, decoded)
    }

    @Test
    fun `test round trip encoding and decoding with byte arrays`() {
        val original = "Test data with bytes".toByteArray()
        val encoded = original.encodeBase64()
        val decoded = encoded.decodeBase64()
        
        assertArrayEquals(original, decoded)
    }

    @Test
    fun `test encodeBase64 with padding`() {
        val input = "A".toByteArray() // Single byte should need padding
        val result = input.encodeBase64()
        
        assertNotNull(result)
        val resultString = String(result)
        assertTrue(resultString.endsWith("==")) // Should have padding
    }

    @Test
    fun `test encodeBase64 without padding`() {
        val input = "ABC".toByteArray() // 3 bytes should not need padding
        val result = input.encodeBase64()
        
        assertNotNull(result)
        val resultString = String(result)
        assertFalse(resultString.endsWith("=")) // Should not have padding
    }

    @Test
    fun `test decodeBase64 with padding`() {
        val input = "QQ==" // Base64 encoded "A" with padding
        val result = input.decodeBase64()
        
        assertEquals("A", result)
    }

    @Test
    fun `test decodeBase64 without padding`() {
        val input = "QUJD" // Base64 encoded "ABC" without padding
        val result = input.decodeBase64()
        
        assertEquals("ABC", result)
    }

    @Test
    fun `test encodeBase64 with large data`() {
        val input = "A".repeat(1000).toByteArray()
        val result = input.encodeBase64()
        
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        val decoded = result.decodeBase64()
        assertArrayEquals(input, decoded)
    }

    @Test
    fun `test decodeBase64 with invalid characters`() {
        val input = "SGVsbG8sIFdvcmxkIQ==invalid"
        val result = input.decodeBase64()
        
        // Should handle invalid characters gracefully
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `test encodeBase64 with special characters`() {
        val input = "Special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?".toByteArray()
        val result = input.encodeBase64()
        
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
        val decoded = result.decodeBase64()
        assertArrayEquals(input, decoded)
    }
} 