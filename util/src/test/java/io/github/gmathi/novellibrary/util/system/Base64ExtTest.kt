package io.github.gmathi.novellibrary.util.system

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Unit tests for Base64 extension functions
 */
class Base64ExtTest : StringSpec({
    
    "encodeBase64ToString should encode string correctly" {
        val input = "Hello, World!"
        val encoded = input.encodeBase64ToString()
        
        // Verify it's not empty and different from input
        encoded.isNotEmpty() shouldBe true
        encoded shouldBe "SGVsbG8sIFdvcmxkIQ=="
    }
    
    "decodeBase64 should decode encoded string correctly" {
        val original = "Hello, World!"
        val encoded = original.encodeBase64ToString()
        val decoded = encoded.decodeBase64()
        
        decoded shouldBe original
    }
    
    "encodeBase64ToByteArray should encode to byte array" {
        val input = "test"
        val encoded = input.encodeBase64ToByteArray()
        
        encoded.isNotEmpty() shouldBe true
    }
    
    "decodeBase64ToByteArray should decode to byte array" {
        val original = "test"
        val encoded = original.encodeBase64ToString()
        val decoded = encoded.decodeBase64ToByteArray()
        
        String(decoded) shouldBe original
    }
    
    "ByteArray encodeBase64ToString should work" {
        val input = "test".toByteArray()
        val encoded = input.encodeBase64ToString()
        
        encoded shouldBe "dGVzdA=="
    }
    
    "ByteArray decodeBase64ToString should work" {
        val encoded = "dGVzdA==".toByteArray()
        val decoded = encoded.decodeBase64ToString()
        
        decoded shouldBe "test"
    }
    
    "encode and decode should be reversible" {
        val original = "The quick brown fox jumps over the lazy dog"
        val encoded = original.encodeBase64ToString()
        val decoded = encoded.decodeBase64()
        
        decoded shouldBe original
    }
    
    "should handle empty string" {
        val original = ""
        val encoded = original.encodeBase64ToString()
        val decoded = encoded.decodeBase64()
        
        decoded shouldBe original
    }
    
    "should handle special characters" {
        val original = "!@#$%^&*()_+-=[]{}|;':\",./<>?"
        val encoded = original.encodeBase64ToString()
        val decoded = encoded.decodeBase64()
        
        decoded shouldBe original
    }
})
