package io.github.gmathi.novellibrary.model.source

import android.content.Context
import android.graphics.drawable.Drawable
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.gmathi.novellibrary.extension.ExtensionManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Unit tests for Source extension functions with Hilt dependency injection
 * Validates that extension functions work correctly with EntryPoint access
 */
@HiltAndroidTest
class SourceExtensionsHiltTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var extensionManager: ExtensionManager

    private lateinit var mockContext: Context
    private lateinit var testSource: Source

    @Before
    fun setup() {
        hiltRule.inject()
        
        mockContext = mockk(relaxed = true) {
            every { applicationContext } returns this
        }
        
        testSource = object : Source {
            override val id: Long = 1L
            override val name: String = "Test Source"
            
            override suspend fun fetchNovelDetails(novel: io.github.gmathi.novellibrary.model.database.Novel): io.github.gmathi.novellibrary.model.database.Novel {
                return novel
            }
            
            override suspend fun getChapterList(novel: io.github.gmathi.novellibrary.model.database.Novel): List<io.github.gmathi.novellibrary.model.database.WebPage> {
                return emptyList()
            }
        }
    }

    @Test
    fun `extensionManager is properly injected`() {
        assertNotNull(extensionManager)
    }

    @Test
    fun `icon extension function uses EntryPoint to access ExtensionManager`() {
        // Mock the drawable that would be returned
        val mockDrawable = mockk<Drawable>()
        every { extensionManager.getAppIconForSource(testSource) } returns mockDrawable

        // Call the extension function
        val result = testSource.icon(mockContext)

        // Verify the result
        assertEquals(mockDrawable, result)
        
        // Verify that the context's applicationContext was accessed
        verify { mockContext.applicationContext }
    }

    @Test
    fun `icon extension function returns null when no icon available`() {
        // Mock ExtensionManager to return null
        every { extensionManager.getAppIconForSource(testSource) } returns null

        // Call the extension function
        val result = testSource.icon(mockContext)

        // Verify null is returned
        assertEquals(null, result)
    }

    @Test
    fun `getPreferenceKey extension function works correctly`() {
        val preferenceKey = testSource.getPreferenceKey()
        assertEquals("source_1", preferenceKey)
    }

    @Test
    fun `getPreferenceKey handles different source IDs`() {
        val anotherSource = object : Source {
            override val id: Long = 999L
            override val name: String = "Another Source"
            
            override suspend fun fetchNovelDetails(novel: io.github.gmathi.novellibrary.model.database.Novel): io.github.gmathi.novellibrary.model.database.Novel {
                return novel
            }
            
            override suspend fun getChapterList(novel: io.github.gmathi.novellibrary.model.database.Novel): List<io.github.gmathi.novellibrary.model.database.WebPage> {
                return emptyList()
            }
        }
        
        val preferenceKey = anotherSource.getPreferenceKey()
        assertEquals("source_999", preferenceKey)
    }
}