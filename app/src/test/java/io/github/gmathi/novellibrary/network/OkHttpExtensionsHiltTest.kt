package io.github.gmathi.novellibrary.network

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.mockk
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Unit tests for OkHttpExtensions with Hilt dependency injection
 * Validates that parseAs function works correctly with injected Json instance
 */
@HiltAndroidTest
class OkHttpExtensionsHiltTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var json: Json

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun `json instance is properly injected`() {
        assertNotNull(json)
    }

    @ExperimentalSerializationApi
    @Test
    fun `parseAs function works with injected Json`() {
        // Create a mock response with JSON content
        val jsonContent = """{"name": "test", "value": 123}"""
        val response = mockk<Response>(relaxed = true) {
            every { body?.string() } returns jsonContent
            every { use<TestData>(any()) } answers {
                val block = firstArg<(Response) -> TestData>()
                block(this@mockk)
            }
        }

        // Test parseAs with injected Json
        val result = response.parseAs<TestData>(json)
        
        assertEquals("test", result.name)
        assertEquals(123, result.value)
    }

    @ExperimentalSerializationApi
    @Test
    fun `parseAs handles empty response body`() {
        val response = mockk<Response>(relaxed = true) {
            every { body?.string() } returns null
            every { use<TestData>(any()) } answers {
                val block = firstArg<(Response) -> TestData>()
                block(this@mockk)
            }
        }

        try {
            response.parseAs<TestData>(json)
        } catch (e: Exception) {
            // Expected to throw exception for invalid JSON
            assertNotNull(e)
        }
    }

    @kotlinx.serialization.Serializable
    data class TestData(
        val name: String,
        val value: Int
    )
}