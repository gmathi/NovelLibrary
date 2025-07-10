package io.github.gmathi.novellibrary.util.lang

import org.junit.Test
import org.junit.Assert.*

class HashTest {

    @Test
    fun `test sha256 with string`() {
        val input = "Hello, World!"
        val result = Hash.sha256(input)
        
        assertNotNull(result)
        assertEquals(64, result.length) // SHA-256 produces 64 hex characters
        assertTrue(result.matches(Regex("[a-f0-9]{64}"))) // Should be hex string
    }

    @Test
    fun `test sha256 with empty string`() {
        val input = ""
        val result = Hash.sha256(input)
        
        assertNotNull(result)
        assertEquals(64, result.length)
        // Empty string SHA-256 hash
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", result)
    }

    @Test
    fun `test sha256 with byte array`() {
        val input = "Test data".toByteArray()
        val result = Hash.sha256(input)
        
        assertNotNull(result)
        assertEquals(64, result.length)
        assertTrue(result.matches(Regex("[a-f0-9]{64}")))
    }

    @Test
    fun `test sha256 with empty byte array`() {
        val input = ByteArray(0)
        val result = Hash.sha256(input)
        
        assertNotNull(result)
        assertEquals(64, result.length)
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", result)
    }

    @Test
    fun `test md5 with string`() {
        val input = "Hello, World!"
        val result = Hash.md5(input)
        
        assertNotNull(result)
        assertEquals(32, result.length) // MD5 produces 32 hex characters
        assertTrue(result.matches(Regex("[a-f0-9]{32}"))) // Should be hex string
    }

    @Test
    fun `test md5 with empty string`() {
        val input = ""
        val result = Hash.md5(input)
        
        assertNotNull(result)
        assertEquals(32, result.length)
        // Empty string MD5 hash
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", result)
    }

    @Test
    fun `test md5 with byte array`() {
        val input = "Test data".toByteArray()
        val result = Hash.md5(input)
        
        assertNotNull(result)
        assertEquals(32, result.length)
        assertTrue(result.matches(Regex("[a-f0-9]{32}")))
    }

    @Test
    fun `test md5 with empty byte array`() {
        val input = ByteArray(0)
        val result = Hash.md5(input)
        
        assertNotNull(result)
        assertEquals(32, result.length)
        assertEquals("d41d8cd98f00b204e9800998ecf8427e", result)
    }

    @Test
    fun `test sha256 consistency`() {
        val input = "Consistent test input"
        val result1 = Hash.sha256(input)
        val result2 = Hash.sha256(input)
        
        assertEquals(result1, result2)
    }

    @Test
    fun `test md5 consistency`() {
        val input = "Consistent test input"
        val result1 = Hash.md5(input)
        val result2 = Hash.md5(input)
        
        assertEquals(result1, result2)
    }

    @Test
    fun `test sha256 with unicode string`() {
        val input = "Hello 世界"
        val result = Hash.sha256(input)
        
        assertNotNull(result)
        assertEquals(64, result.length)
        assertTrue(result.matches(Regex("[a-f0-9]{64}")))
    }

    @Test
    fun `test md5 with unicode string`() {
        val input = "Hello 世界"
        val result = Hash.md5(input)
        
        assertNotNull(result)
        assertEquals(32, result.length)
        assertTrue(result.matches(Regex("[a-f0-9]{32}")))
    }

    @Test
    fun `test sha256 with special characters`() {
        val input = "!@#$%^&*()_+-=[]{}|;':\",./<>?"
        val result = Hash.sha256(input)
        
        assertNotNull(result)
        assertEquals(64, result.length)
        assertTrue(result.matches(Regex("[a-f0-9]{64}")))
    }

    @Test
    fun `test md5 with special characters`() {
        val input = "!@#$%^&*()_+-=[]{}|;':\",./<>?"
        val result = Hash.md5(input)
        
        assertNotNull(result)
        assertEquals(32, result.length)
        assertTrue(result.matches(Regex("[a-f0-9]{32}")))
    }

    @Test
    fun `test sha256 with long string`() {
        val input = "A".repeat(1000)
        val result = Hash.sha256(input)
        
        assertNotNull(result)
        assertEquals(64, result.length)
        assertTrue(result.matches(Regex("[a-f0-9]{64}")))
    }

    @Test
    fun `test md5 with long string`() {
        val input = "A".repeat(1000)
        val result = Hash.md5(input)
        
        assertNotNull(result)
        assertEquals(32, result.length)
        assertTrue(result.matches(Regex("[a-f0-9]{32}")))
    }
} 