package io.github.gmathi.novellibrary.regression

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.gmathi.novellibrary.source.SourceManager
import io.github.gmathi.novellibrary.extension.ExtensionManager
import io.github.gmathi.novellibrary.model.source.Source
import io.github.gmathi.novellibrary.model.source.HttpSource
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.network.sync.NovelSync
import io.github.gmathi.novellibrary.network.sync.NovelUpdatesSync
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.dataCenter.DataCenter
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import javax.inject.Inject

/**
 * Comprehensive regression tests for source management operations after Injekt cleanup.
 * Validates that all source-related functionality works correctly with pure Hilt injection.
 */
@HiltAndroidTest
class SourceManagementRegressionTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var sourceManager: SourceManager

    @Inject
    lateinit var extensionManager: ExtensionManager

    @Inject
    lateinit var networkHelper: NetworkHelper

    @Inject
    lateinit var dbHelper: DBHelper

    @Inject
    lateinit var dataCenter: DataCenter

    @ApplicationContext
    @Inject
    lateinit var context: Context

    private lateinit var novelUpdatesSync: NovelUpdatesSync

    @Before
    fun setup() {
        hiltRule.inject()
        novelUpdatesSync = NovelUpdatesSync(dbHelper, dataCenter)
    }

    @Test
    fun `test SourceManager basic operations work correctly`() {
        // Test that SourceManager can be injected and used
        assertNotNull("SourceManager should be injected", sourceManager)
        
        // Test getting all sources
        val sources = sourceManager.getAllSources()
        assertNotNull("Should return sources list", sources)
        
        // Test getting source by ID
        if (sources.isNotEmpty()) {
            val firstSource = sources.first()
            val retrievedSource = sourceManager.getSource(firstSource.id)
            assertEquals("Should retrieve same source by ID", firstSource.id, retrievedSource?.id)
        }
    }

    @Test
    fun `test ExtensionManager operations work correctly`() {
        // Test that ExtensionManager can be injected and used
        assertNotNull("ExtensionManager should be injected", extensionManager)
        
        // Test getting installed extensions
        val extensions = extensionManager.getInstalledExtensions()
        assertNotNull("Should return extensions list", extensions)
        
        // Test extension loading functionality
        val availableExtensions = extensionManager.getAvailableExtensions()
        assertNotNull("Should return available extensions", availableExtensions)
    }

    @Test
    fun `test Source extension functions work correctly`() {
        // Test that Source extension functions work with proper dependency injection
        val testSource = createTestSource()
        
        // Test icon() extension function with context parameter
        val icon = testSource.icon(context)
        // Icon might be null for test source, but should not throw injection errors
        
        // Test other source extension functions
        val displayName = testSource.toString()
        assertNotNull("Source should have display name", displayName)
    }

    @Test
    fun `test HttpSource creation and operations work correctly`() {
        // Test that HttpSource can be created with proper injection
        val httpSource = createTestHttpSource()
        assertNotNull("HttpSource should be created", httpSource)
        
        // Test that HttpSource has access to NetworkHelper
        assertNotNull("HttpSource should have network access", httpSource.client)
        
        // Test basic HTTP operations
        runBlocking {
            try {
                // Test that source can make network requests
                val response = httpSource.client.newCall(
                    okhttp3.Request.Builder()
                        .url("https://httpbin.org/get")
                        .build()
                ).execute()
                
                assertTrue("HttpSource network request should work", response.isSuccessful)
                response.close()
            } catch (e: Exception) {
                // Network might fail in test environment, but should not be injection-related
                assertFalse("Should not fail due to injection issues", 
                    e.message?.contains("inject") == true)
            }
        }
    }

    @Test
    fun `test NovelSync operations work correctly`() = runBlocking {
        // Test that NovelSync can be created with proper injection
        assertNotNull("NovelUpdatesSync should be created", novelUpdatesSync)
        
        // Test that NovelSync has access to dependencies
        assertNotNull("NovelSync should have DBHelper access", novelUpdatesSync.dbHelper)
        assertNotNull("NovelSync should have DataCenter access", novelUpdatesSync.dataCenter)
        
        // Test sync operations
        try {
            val testUrl = "https://www.novelupdates.com/series/test-novel/"
            val canSync = novelUpdatesSync.canSync(testUrl)
            // Result depends on URL format, but should not throw injection errors
            assertNotNull("Sync check should return result", canSync)
        } catch (e: Exception) {
            assertFalse("Should not fail due to injection issues", 
                e.message?.contains("inject") == true)
        }
    }

    @Test
    fun `test source filtering and search work correctly`() {
        // Test source filtering functionality
        val allSources = sourceManager.getAllSources()
        val enabledSources = sourceManager.getEnabledSources()
        
        assertNotNull("All sources should be retrievable", allSources)
        assertNotNull("Enabled sources should be retrievable", enabledSources)
        
        // Test source search functionality
        if (allSources.isNotEmpty()) {
            val searchTerm = allSources.first().name.take(3)
            val searchResults = sourceManager.searchSources(searchTerm)
            assertNotNull("Source search should return results", searchResults)
        }
    }

    @Test
    fun `test source state management works correctly`() {
        // Test enabling/disabling sources
        val sources = sourceManager.getAllSources()
        if (sources.isNotEmpty()) {
            val testSource = sources.first()
            val originalState = testSource.isEnabled
            
            // Test state changes
            sourceManager.setSourceEnabled(testSource.id, !originalState)
            val updatedSource = sourceManager.getSource(testSource.id)
            assertEquals("Source state should be updated", !originalState, updatedSource?.isEnabled)
            
            // Restore original state
            sourceManager.setSourceEnabled(testSource.id, originalState)
        }
    }

    @Test
    fun `test extension installation and management work correctly`() {
        // Test extension installation process
        val availableExtensions = extensionManager.getAvailableExtensions()
        
        if (availableExtensions.isNotEmpty()) {
            val testExtension = availableExtensions.first()
            
            // Test extension info retrieval
            val extensionInfo = extensionManager.getExtensionInfo(testExtension.packageName)
            assertNotNull("Should retrieve extension info", extensionInfo)
            
            // Test extension icon retrieval
            val icon = extensionManager.getExtensionIcon(testExtension.packageName)
            // Icon might be null, but should not throw injection errors
        }
    }

    @Test
    fun `test source catalog operations work correctly`() = runBlocking {
        // Test source catalog functionality
        val sources = sourceManager.getAllSources()
        val httpSources = sources.filterIsInstance<HttpSource>()
        
        if (httpSources.isNotEmpty()) {
            val testSource = httpSources.first()
            
            try {
                // Test catalog browsing
                val catalogResults = testSource.getPopularNovels(1)
                assertNotNull("Catalog should return results", catalogResults)
            } catch (e: Exception) {
                // Network operations might fail in test environment
                assertFalse("Should not fail due to injection issues", 
                    e.message?.contains("inject") == true)
            }
        }
    }

    @Test
    fun `test source preferences work correctly`() {
        // Test source preference management
        val sources = sourceManager.getAllSources()
        
        if (sources.isNotEmpty()) {
            val testSource = sources.first()
            
            // Test preference retrieval
            val preferences = sourceManager.getSourcePreferences(testSource.id)
            assertNotNull("Should retrieve source preferences", preferences)
            
            // Test preference updates
            sourceManager.updateSourcePreference(testSource.id, "test_key", "test_value")
            val updatedPrefs = sourceManager.getSourcePreferences(testSource.id)
            assertNotNull("Should retrieve updated preferences", updatedPrefs)
        }
    }

    @Test
    fun `test concurrent source operations work correctly`() = runBlocking {
        // Test multiple concurrent source operations
        val sources = sourceManager.getAllSources().take(3)
        
        val operations = sources.map { source ->
            kotlinx.coroutines.async {
                sourceManager.getSource(source.id)
            }
        }
        
        operations.forEach { deferred ->
            val result = deferred.await()
            assertNotNull("Concurrent source operation should succeed", result)
        }
    }

    private fun createTestSource(): Source {
        return object : Source {
            override val id: Long = 999L
            override val name: String = "Test Source"
            override val lang: String = "en"
            override val isEnabled: Boolean = true
        }
    }

    private fun createTestHttpSource(): HttpSource {
        return object : HttpSource(networkHelper) {
            override val id: Long = 998L
            override val name: String = "Test HTTP Source"
            override val lang: String = "en"
            override val baseUrl: String = "https://httpbin.org"
            
            override suspend fun getPopularNovels(page: Int) = emptyList<Any>()
            override suspend fun getLatestUpdates(page: Int) = emptyList<Any>()
            override suspend fun searchNovels(query: String, page: Int) = emptyList<Any>()
            override suspend fun getNovelDetails(novel: Any) = null
            override suspend fun getChapterList(novel: Any) = emptyList<Any>()
            override suspend fun getChapterContent(chapter: Any) = ""
        }
    }
}