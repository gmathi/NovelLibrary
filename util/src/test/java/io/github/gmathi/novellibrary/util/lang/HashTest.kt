package io.github.gmathi.novellibrary.util.lang

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldHaveLength

/**
 * Unit tests for Hash utility
 * 
 * Tests MD5 and SHA256 hashing functions
 */
class HashTest : StringSpec({
    
    "md5 should produce consistent hash for same input" {
        val input = "test string"
        val hash1 = Hash.md5(input)
        val hash2 = Hash.md5(input)
        
        hash1 shouldBe hash2
    }
    
    "md5 should produce different hashes for different inputs" {
        val hash1 = Hash.md5("test1")
        val hash2 = Hash.md5("test2")
        
        hash1 shouldNotBe hash2
    }
    
    "md5 should produce 32 character hex string" {
        val hash = Hash.md5("test")
        hash shouldHaveLength 32
        hash.all { it in '0'..'9' || it in 'a'..'f' } shouldBe true
    }
    
    "sha256 should produce consistent hash for same input" {
        val input = "test string"
        val hash1 = Hash.sha256(input)
        val hash2 = Hash.sha256(input)
        
        hash1 shouldBe hash2
    }
    
    "sha256 should produce different hashes for different inputs" {
        val hash1 = Hash.sha256("test1")
        val hash2 = Hash.sha256("test2")
        
        hash1 shouldNotBe hash2
    }
    
    "sha256 should produce 64 character hex string" {
        val hash = Hash.sha256("test")
        hash shouldHaveLength 64
        hash.all { it in '0'..'9' || it in 'a'..'f' } shouldBe true
    }
    
    "md5 should handle empty string" {
        val hash = Hash.md5("")
        hash shouldHaveLength 32
    }
    
    "sha256 should handle empty string" {
        val hash = Hash.sha256("")
        hash shouldHaveLength 64
    }
})
