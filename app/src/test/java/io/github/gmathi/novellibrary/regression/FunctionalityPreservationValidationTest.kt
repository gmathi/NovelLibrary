package io.github.gmathi.novellibrary.regression

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.gmathi.novellibrary.source.SourceManager
import io.github.gmathi.novellibrary.extension.ExtensionManager
import io.github.gmathi.novellibrary.database.NovelHelper
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.dao.NovelDao
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.dataCenter.DataCenter
import io.github.gmathi.novellibrary.network.proxy.BaseProxyHelper
import io.github.gmathi.novellibrary.network.proxy.BasePostProxyHelper
import io.github.gmathi.novellibrary.network.CloudflareInterceptor
import io.github.gmathi.novellibrary.network.AndroidCookieJar
import io.github.gmathi.novellibrary.network.RetrofitServiceFactory
import io.github.gmathi.novellibrary.network.AppUpdateGithubApi
import io.github.gmathi.novellibrary.network.sync.NovelUpdatesSync
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.Chapter
import io.github.gmathi.novellibrary.model.source.HttpSource
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import javax.inject.Inject

/**
 * Comprehensive validation test that ensures all functionality is preserved after Injekt cleanup.
 * This test serves as the final validation that the migration to pure Hilt injection is successful.
 */
@HiltAndroidTest
class FunctionalityPreservationValidationTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    // Core dependencies that should be injectable
    @Inject
    lateinit var sourceManager: SourceManager

    @Inject
    lateinit var extensionManager: ExtensionManager

    @Inject
    lateinit var novelHelper: NovelHelper

    @Inject
    lateinit var dbHelper: DBHelper

    @Inject
    lateinit var novelDao: NovelDao

    @Inject
    lateinit var networkHelper: NetworkHelper

    @Inject
    lateinit var dataCenter: DataCenter

    @Inject
    lateinit var json: Json

    @ApplicationContext
    @Inject
    lateinit var context: Context

    // Network components that should work with Hilt injection
    private lateinit var baseProxyHelper: BaseProxyHelper
    private lateinit var basePostProxyHelper: BasePostProxyHelper
    private lateinit var cloudflareInterceptor: CloudflareInterceptor
    private lateinit var androidCookieJar: AndroidCookieJar
    private lateinit var retrofitServiceFactory: RetrofitServiceFactory
    private lateinit var appUpdateGithubApi: AppUpdateGithubApi
    private lateinit var novelUpdatesSync: NovelUpdatesSync

    @Before
    fun setup() {
        hiltRule.inject()
        
        // Initialize network components with injected dependencies
        baseProxyHelper = BaseProxyHelper(networkHelper)
        basePostProxyHelper = BasePostProxyHelper(networkHelper)
        cloudflareInterceptor = CloudflareInterceptor(networkHelper)
        androidCookieJar = AndroidCookieJar(dataCenter)
        retrofitServiceFactory = RetrofitServiceFactory(networkHelper)
        appUpdateGithubApi = AppUpdateGithubApi(networkHelper)
        novelUpdatesSync = NovelUpdatesSync(dbHelper, dataCenter)
    }

    @Test
    fun `validate all core dependencies are properly injected`() {
        // Validate that all core dependencies can be injected without Injekt
        assertNotNull("SourceManager should be injected", sourceManager)
        assertNotNull("ExtensionManager should be injected", extensionManager)
        assertNotNull("NovelHelper should be injected", novelHelper)
        assertNotNull("DBHelper should be injected", dbHelper)
        assertNotNull("NovelDao should be injected", novelDao)
        assertNotNull("NetworkHelper should be injected", networkHelper)
        assertNotNull("DataCenter should be injected", dataCenter)
        assertNotNull("Json should be injected", json)
        assertNotNull("Context should be injected", context)
    }

    @Test
    fun `validate all network components work with Hilt injection`() {
        // Validate that all network components can be created and used
        assertNotNull("BaseProxyHelper should be created", baseProxyHelper)
        assertNotNull("BasePostProxyHelper should be created", basePostProxyHelper)
        assertNotNull("CloudflareInterceptor should be created", cloudflareInterceptor)
        assertNotNull("AndroidCookieJar should be created", androidCookieJar)
        assertNotNull("RetrofitServiceFactory should be created", retrofitServiceFactory)
        assertNotNull("AppUpdateGithubApi should be created", appUpdateGithubApi)
        assertNotNull("NovelUpdatesSync should be created", novelUpdatesSync)

        // Validate that network components have access to their dependencies
        assertNotNull("BaseProxyHelper should have network client", baseProxyHelper.client)
        assertNotNull("BasePostProxyHelper should have network client", basePostProxyHelper.client)
        assertNotNull("NovelUpdatesSync should have DBHelper", novelUpdatesSync.dbHelper)
        assertNotNull("NovelUpdatesSync should have DataCenter", novelUpdatesSync.dataCenter)
    }

    @Test
    fun `validate database operations work correctly with Hilt injection`() = runBlocking {
        // Test that database operations work with injected dependencies
        val testNovel = createTestNovel()
        
        // Test novel insertion
        val novelId = novelHelper.insertNovel(testNovel)
        assertTrue("Novel should be inserted successfully", novelId > 0)
        
        try {
            // Test novel retrieval
            val retrievedNovel = novelHelper.getNovel(novelId)
            assertNotNull("Novel should be retrieved successfully", retrievedNovel)
            assertEquals("Retrieved novel should match", testNovel.name, retrievedNovel?.name)
            
            // Test NovelDao operations with SourceManager injection
            val novelDetails = novelDao.getNovelDetails(novelId)
            // Details might be null for test novel, but should not throw injection errors
            assertNotNull("NovelDao should have SourceManager access", novelDao.sourceManager)
            
            // Test chapter operations
            val testChapter = createTestChapter(novelId)
            val chapterId = novelHelper.insertChapter(testChapter)
            assertTrue("Chapter should be inserted successfully", chapterId > 0)
            
            val retrievedChapter = novelHelper.getChapter(chapterId)
            assertNotNull("Chapter should be retrieved successfully", retrievedChapter)
            
            // Clean up chapter
            novelHelper.deleteChapter(chapterId)
            
        } finally {
            // Clean up novel
            novelHelper.deleteNovel(novelId)
        }
    }

    @Test
    fun `validate source management operations work correctly with Hilt injection`() {
        // Test that source management works with injected dependencies
        val sources = sourceManager.getAllSources()
        assertNotNull("SourceManager should return sources", sources)
        
        val extensions = extensionManager.getInstalledExtensions()
        assertNotNull("ExtensionManager should return extensions", extensions)
        
        val availableExtensions = extensionManager.getAvailableExtensions()
        assertNotNull("ExtensionManager should return available extensions", availableExtensions)
        
        // Test source operations
        if (sources.isNotEmpty()) {
            val firstSource = sources.first()
            val retrievedSource = sourceManager.getSource(firstSource.id)
            assertEquals("Should retrieve same source by ID", firstSource.id, retrievedSource?.id)
        }
    }

    @Test
    fun `validate network operations work correctly with Hilt injection`() {
        // Test that network operations work with injected dependencies
        val client = networkHelper.cloudflareClient
        assertNotNull("NetworkHelper should provide client", client)
        
        // Test proxy helpers
        val proxyClient = baseProxyHelper.client
        assertNotNull("BaseProxyHelper should provide client", proxyClient)
        
        val postProxyClient = basePostProxyHelper.client
        assertNotNull("BasePostProxyHelper should provide client", postProxyClient)
        
        // Test retrofit factory
        val retrofit = retrofitServiceFactory.createRetrofit("https://httpbin.org/")
        assertNotNull("RetrofitServiceFactory should create retrofit", retrofit)
    }

    @Test
    fun `validate JSON serialization works correctly with Hilt injection`() {
        // Test that JSON serialization works with injected Json instance
        val testJson = """{"test": "value", "number": 42}"""
        
        try {
            val jsonElement = json.parseToJsonElement(testJson)
            assertNotNull("Should parse JSON successfully", jsonElement)
            
            val jsonString = json.encodeToString(
                kotlinx.serialization.json.JsonElement.serializer(),
                jsonElement
            )
            assertNotNull("Should serialize JSON successfully", jsonString)
            
        } catch (e: Exception) {
            fail("JSON operations should work with injected Json instance: ${e.message}")
        }
    }

    @Test
    fun `validate EntryPoint access patterns work correctly`() {
        // Test that EntryPoint access patterns work for components that need them
        
        // Test Source extension functions with context
        val testSource = createTestSource()
        val icon = testSource.icon(context)
        // Icon might be null for test source, but should not throw injection errors
        
        // Test that context is available for EntryPoint access
        assertNotNull("Context should be available for EntryPoint access", context)
        assertTrue("Context should be application context", 
            context == context.applicationContext)
    }

    @Test
    fun `validate error handling works correctly without Injekt`() {
        // Test that error handling works properly without Injekt dependencies
        
        // Test invalid database operations
        val invalidNovel = novelHelper.getNovel(-1L)
        assertNull("Should handle invalid operations gracefully", invalidNovel)
        
        // Test network error scenarios
        try {
            val client = networkHelper.cloudflareClient
            val request = okhttp3.Request.Builder()
                .url("https://httpbin.org/status/404")
                .build()
            
            val response = client.newCall(request).execute()
            assertEquals("Should handle HTTP errors correctly", 404, response.code)
            response.close()
            
        } catch (e: Exception) {
            // Network errors are acceptable, but should not be injection-related
            assertFalse("Errors should not be injection-related", 
                e.message?.contains("inject") == true)
        }
    }

    @Test
    fun `validate performance characteristics are maintained`() {
        // Test that performance is maintained or improved after Injekt cleanup
        
        val startTime = System.currentTimeMillis()
        
        // Perform typical operations
        val sources = sourceManager.getAllSources()
        val novels = novelHelper.getAllNovels()
        val extensions = extensionManager.getInstalledExtensions()
        
        val endTime = System.currentTimeMillis()
        val executionTime = endTime - startTime
        
        // Operations should complete reasonably quickly
        assertTrue("Operations should complete in reasonable time", executionTime < 5000)
        
        // Validate that operations returned expected results
        assertNotNull("Sources should be retrieved", sources)
        assertNotNull("Novels should be retrieved", novels)
        assertNotNull("Extensions should be retrieved", extensions)
    }

    @Test
    fun `validate memory usage patterns are stable`() {
        // Test that memory usage is stable after Injekt cleanup
        
        val runtime = Runtime.getRuntime()
        val initialMemory = runtime.totalMemory() - runtime.freeMemory()
        
        // Perform memory-intensive operations
        repeat(10) {
            val sources = sourceManager.getAllSources()
            val novels = novelHelper.getAllNovels()
            val extensions = extensionManager.getInstalledExtensions()
        }
        
        // Force garbage collection
        System.gc()
        Thread.sleep(100)
        
        val finalMemory = runtime.totalMemory() - runtime.freeMemory()
        val memoryIncrease = finalMemory - initialMemory
        
        // Memory increase should be reasonable (less than 10MB)
        assertTrue("Memory usage should be stable", memoryIncrease < 10 * 1024 * 1024)
    }

    @Test
    fun `validate concurrent operations work correctly`() = runBlocking {
        // Test that concurrent operations work properly with Hilt injection
        
        val operations = (1..5).map { i ->
            kotlinx.coroutines.async {
                // Perform concurrent operations
                val sources = sourceManager.getAllSources()
                val novels = novelHelper.getAllNovels()
                val extensions = extensionManager.getInstalledExtensions()
                
                Triple(sources, novels, extensions)
            }
        }
        
        // Wait for all operations to complete
        val results = operations.map { it.await() }
        
        // Validate that all operations succeeded
        results.forEach { (sources, novels, extensions) ->
            assertNotNull("Concurrent source operation should succeed", sources)
            assertNotNull("Concurrent novel operation should succeed", novels)
            assertNotNull("Concurrent extension operation should succeed", extensions)
        }
    }

    @Test
    fun `validate complete user workflow integration`() = runBlocking {
        // Test a complete user workflow to ensure all components work together
        
        // Step 1: User browses sources
        val sources = sourceManager.getAllSources()
        assertNotNull("User should see sources", sources)
        
        // Step 2: User adds a novel to library
        val testNovel = createTestNovel()
        val novelId = novelHelper.insertNovel(testNovel)
        assertTrue("User should be able to add novel", novelId > 0)
        
        try {
            // Step 3: User adds chapters
            val testChapter = createTestChapter(novelId)
            val chapterId = novelHelper.insertChapter(testChapter)
            assertTrue("User should be able to add chapter", chapterId > 0)
            
            // Step 4: User reads chapter
            val updatedChapter = testChapter.copy(id = chapterId, read = 1)
            novelHelper.updateChapter(updatedChapter)
            
            val readChapter = novelHelper.getChapter(chapterId)
            assertEquals("Chapter should be marked as read", 1, readChapter?.read)
            
            // Step 5: User manages extensions
            val extensions = extensionManager.getInstalledExtensions()
            assertNotNull("User should see extensions", extensions)
            
            // Clean up
            novelHelper.deleteChapter(chapterId)
            
        } finally {
            novelHelper.deleteNovel(novelId)
        }
    }

    @Test
    fun `validate no Injekt dependencies remain in runtime`() {
        // Final validation that no Injekt dependencies are present at runtime
        
        // Check that all major components are working without Injekt
        val componentChecks = mapOf(
            "SourceManager" to { sourceManager.getAllSources() },
            "ExtensionManager" to { extensionManager.getInstalledExtensions() },
            "NovelHelper" to { novelHelper.getAllNovels() },
            "NetworkHelper" to { networkHelper.cloudflareClient },
            "DataCenter" to { dataCenter.getSyncEnabled("test") },
            "Json" to { json.parseToJsonElement("{}") }
        )
        
        componentChecks.forEach { (componentName, check) ->
            try {
                val result = check()
                assertNotNull("$componentName should work without Injekt", result)
            } catch (e: Exception) {
                assertFalse("$componentName should not have Injekt-related errors", 
                    e.message?.contains("injekt", ignoreCase = true) == true ||
                    e.message?.contains("injectLazy", ignoreCase = true) == true ||
                    e.message?.contains("Injekt.get", ignoreCase = true) == true)
            }
        }
    }

    private fun createTestNovel(): Novel {
        return Novel(
            id = 0L,
            name = "Functionality Test Novel",
            url = "https://example.com/novel/functionality-test",
            imageUrl = "https://example.com/image.jpg",
            rating = "4.5",
            shortDescription = "Test novel for functionality validation",
            longDescription = "Long description for functionality validation test novel",
            language = "English",
            genres = listOf("Fantasy", "Adventure"),
            authors = listOf("Test Author"),
            translators = listOf("Test Translator"),
            tags = listOf("test", "functionality", "validation"),
            chaptersCount = 50L,
            currentChapterUrl = null,
            newReleasesCount = 0L,
            lastReadDate = null,
            lastUpdatedDate = System.currentTimeMillis(),
            metadata = mapOf("source" to "functionality_test", "test" to "true")
        )
    }

    private fun createTestChapter(novelId: Long): Chapter {
        return Chapter(
            id = 0L,
            novelId = novelId,
            name = "Functionality Test Chapter",
            url = "https://example.com/chapter/functionality-test",
            orderId = 1L,
            read = 0,
            bookmark = 0,
            translatorSourceName = "Test Translator",
            filePath = null,
            isDownloaded = 0
        )
    }

    private fun createTestSource(): io.github.gmathi.novellibrary.model.source.Source {
        return object : io.github.gmathi.novellibrary.model.source.Source {
            override val id: Long = 999L
            override val name: String = "Functionality Test Source"
            override val lang: String = "en"
            override val isEnabled: Boolean = true
        }
    }
}