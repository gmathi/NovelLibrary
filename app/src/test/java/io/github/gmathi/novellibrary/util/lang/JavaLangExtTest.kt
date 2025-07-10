package io.github.gmathi.novellibrary.util.lang

import org.junit.Test
import org.junit.Assert.*
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonPrimitive

class JavaLangExtTest {

    @Test
    fun `test containsCaseInsensitive with both strings not null`() {
        assertTrue("Hello World".containsCaseInsensitive("hello"))
        assertTrue("Hello World".containsCaseInsensitive("WORLD"))
        assertTrue("Hello World".containsCaseInsensitive("o Wo"))
        assertFalse("Hello World".containsCaseInsensitive("xyz"))
    }

    @Test
    fun `test containsCaseInsensitive with both strings null`() {
        assertTrue(null.containsCaseInsensitive(null))
    }

    @Test
    fun `test containsCaseInsensitive with first string null`() {
        assertFalse(null.containsCaseInsensitive("hello"))
    }

    @Test
    fun `test containsCaseInsensitive with second string null`() {
        assertFalse("Hello World".containsCaseInsensitive(null))
    }

    @Test
    fun `test containsCaseInsensitive with empty strings`() {
        assertTrue("".containsCaseInsensitive(""))
        assertTrue("Hello".containsCaseInsensitive(""))
        assertFalse("".containsCaseInsensitive("Hello"))
    }

    @Test
    fun `test asJsonNullFreeString with non-null string`() {
        val jsonElement: JsonElement = JsonPrimitive("test")
        val result = jsonElement.asJsonNullFreeString
        assertEquals("test", result)
    }

    @Test
    fun `test asJsonNullFreeString with null string`() {
        val jsonElement: JsonElement = JsonNull.INSTANCE
        val result = jsonElement.asJsonNullFreeString
        assertNull(result)
    }

    @Test
    fun `test asJsonNullFreeString with empty string`() {
        val jsonElement: JsonElement = JsonPrimitive("")
        val result = jsonElement.asJsonNullFreeString
        assertNull(result)
    }

    @Test
    fun `test asJsonNullFreeString with blank string`() {
        val jsonElement: JsonElement = JsonPrimitive("   ")
        val result = jsonElement.asJsonNullFreeString
        assertNull(result)
    }

    @Test
    fun `test asJsonNullFreeString with JsonNull`() {
        val jsonElement: JsonElement = JsonNull.INSTANCE
        val result = jsonElement.asJsonNullFreeString
        assertNull(result)
    }

    @Test
    fun `test covertJsonNull with non-null JsonElement`() {
        val jsonElement: JsonElement = JsonPrimitive("test")
        val result = jsonElement.covertJsonNull
        assertEquals(jsonElement, result)
    }

    @Test
    fun `test covertJsonNull with JsonNull`() {
        val jsonElement: JsonElement = JsonNull.INSTANCE
        val result = jsonElement.covertJsonNull
        assertNull(result)
    }

    @Test
    fun `test urlEncoded with normal string`() {
        val input = "Hello World"
        val result = input.urlEncoded
        assertEquals("Hello+World", result)
    }

    @Test
    fun `test urlEncoded with special characters`() {
        val input = "Hello & World"
        val result = input.urlEncoded
        assertEquals("Hello+%26+World", result)
    }

    @Test
    fun `test urlEncoded with null string`() {
        val input: String? = null
        val result = input.urlEncoded
        assertEquals("", result)
    }

    @Test
    fun `test urlEncoded with empty string`() {
        val input = ""
        val result = input.urlEncoded
        assertEquals("", result)
    }

    @Test
    fun `test urlEncoded with unicode characters`() {
        val input = "Hello 世界"
        val result = input.urlEncoded
        assertTrue(result.contains("%E4%B8%96%E7%95%8C")) // URL encoded 世界
    }

    @Test
    fun `test fixMalformed with normal URL`() {
        val input = "https://example.com"
        val result = input.fixMalformed
        assertEquals("https://example.com", result)
    }

    @Test
    fun `test fixMalformed with protocol-relative URL`() {
        val input = "//example.com"
        val result = input.fixMalformed
        assertEquals("http://example.com", result)
    }

    @Test
    fun `test fixMalformed with already fixed URL`() {
        val input = "http://example.com"
        val result = input.fixMalformed
        assertEquals("http://example.com", result)
    }

    @Test
    fun `test fixMalformedWithHost with protocol-relative URL`() {
        val input = "//example.com"
        val result = input.fixMalformedWithHost("example.com", "https")
        assertEquals("https://example.com", result)
    }

    @Test
    fun `test fixMalformedWithHost with relative path`() {
        val input = "/path"
        val result = input.fixMalformedWithHost("example.com", "https")
        assertEquals("https://example.com/path", result)
    }

    @Test
    fun `test fixMalformedWithHost with null host`() {
        val input = "//example.com"
        val result = input.fixMalformedWithHost(null, "https")
        assertEquals("https://example.com", result)
    }

    @Test
    fun `test fixMalformedWithHost with null protocol`() {
        val input = "//example.com"
        val result = input.fixMalformedWithHost("example.com", null)
        assertEquals("http://example.com", result)
    }

    @Test
    fun `test fixMalformedWithHost with both null`() {
        val input = "//example.com"
        val result = input.fixMalformedWithHost(null, null)
        assertEquals("http://example.com", result)
    }

    @Test
    fun `test getUrlDomain with valid URL`() {
        val input = "https://www.example.com/path"
        val result = input.getUrlDomain
        assertEquals("example.com", result)
    }

    @Test
    fun `test getUrlDomain with subdomain`() {
        val input = "https://sub.example.com/path"
        val result = input.getUrlDomain
        assertEquals("example.com", result)
    }

    @Test
    fun `test getUrlDomain with null input`() {
        val input: String? = null
        val result = input.getUrlDomain
        assertNull(result)
    }

    @Test
    fun `test getUrlDomain with invalid URL`() {
        val input = "not-a-url"
        val result = input.getUrlDomain
        assertNull(result)
    }

    @Test
    fun `test getUrlDomain with IP address`() {
        val input = "https://192.168.1.1/path"
        val result = input.getUrlDomain
        assertNull(result) // IP addresses don't have top private domains
    }

    @Test
    fun `test containsCaseInsensitive with mixed case`() {
        assertTrue("MiXeD cAsE".containsCaseInsensitive("mixed"))
        assertTrue("MiXeD cAsE".containsCaseInsensitive("CASE"))
        assertTrue("MiXeD cAsE".containsCaseInsensitive("xEd"))
    }

    @Test
    fun `test urlEncoded with spaces`() {
        val input = "Hello World Test"
        val result = input.urlEncoded
        assertEquals("Hello+World+Test", result)
    }

    @Test
    fun `test urlEncoded with slashes`() {
        val input = "path/to/resource"
        val result = input.urlEncoded
        assertEquals("path%2Fto%2Fresource", result)
    }

    @Test
    fun `test fixMalformed with multiple slashes`() {
        val input = "///example.com"
        val result = input.fixMalformed
        println("[fixMalformed with multiple slashes] actual: '$result'")
        assertEquals("http:///example.com", result)
    }

    @Test
    fun `test fixMalformedWithHost with complex path`() {
        val input = "/api/v1/users"
        val result = input.fixMalformedWithHost("api.example.com", "https")
        assertEquals("https://api.example.com/api/v1/users", result)
    }
} 