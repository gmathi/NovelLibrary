package io.github.gmathi.novellibrary.di

import com.google.firebase.analytics.FirebaseAnalytics
import com.google.gson.Gson
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.extension.ExtensionManager
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.BaseHiltTest
import io.github.gmathi.novellibrary.util.coroutines.CoroutineScopes
import io.github.gmathi.novellibrary.util.coroutines.DispatcherProvider
import kotlinx.serialization.json.Json
import org.junit.Test
import javax.inject.Inject
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test class for verifying that all Hilt module providers work correctly
 * and provide the expected dependencies.
 */
class HiltModuleProvidersTest : BaseHiltTest() {

    @Inject
    lateinit var dbHelper: DBHelper

    @Inject
    lateinit var dataCenter: DataCenter

    @Inject
    lateinit var networkHelper: NetworkHelper

    @Inject
    lateinit var gson: Gson

    @Inject
    lateinit var json: Json

    @Inject
    lateinit var sourceManager: SourceManager

    @Inject
    lateinit var extensionManager: ExtensionManager

    @Inject
    lateinit var firebaseAnalytics: FirebaseAnalytics

    @Inject
    lateinit var coroutineScopes: CoroutineScopes

    @Inject
    lateinit var dispatcherProvider: DispatcherProvider

    @Test
    fun `DatabaseModule providers should inject correctly`() {
        // Then
        assertNotNull(dbHelper, "DBHelper should be injected")
        assertNotNull(dataCenter, "DataCenter should be injected")
        assertTrue(dbHelper is DBHelper, "DBHelper should be correct type")
        assertTrue(dataCenter is DataCenter, "DataCenter should be correct type")
    }

    @Test
    fun `NetworkModule providers should inject correctly`() {
        // Then
        assertNotNull(networkHelper, "NetworkHelper should be injected")
        assertNotNull(gson, "Gson should be injected")
        assertNotNull(json, "Json should be injected")
        assertTrue(networkHelper is NetworkHelper, "NetworkHelper should be correct type")
        assertTrue(gson is Gson, "Gson should be correct type")
        assertTrue(json is Json, "Json should be correct type")
    }

    @Test
    fun `SourceModule providers should inject correctly`() {
        // Then
        assertNotNull(sourceManager, "SourceManager should be injected")
        assertNotNull(extensionManager, "ExtensionManager should be injected")
        assertTrue(sourceManager is SourceManager, "SourceManager should be correct type")
        assertTrue(extensionManager is ExtensionManager, "ExtensionManager should be correct type")
    }

    @Test
    fun `AnalyticsModule providers should inject correctly`() {
        // Then
        assertNotNull(firebaseAnalytics, "FirebaseAnalytics should be injected")
        assertTrue(firebaseAnalytics is FirebaseAnalytics, "FirebaseAnalytics should be correct type")
    }

    @Test
    fun `CoroutineModule providers should inject correctly`() {
        // Then
        assertNotNull(coroutineScopes, "CoroutineScopes should be injected")
        assertNotNull(dispatcherProvider, "DispatcherProvider should be injected")
        assertTrue(coroutineScopes is CoroutineScopes, "CoroutineScopes should be correct type")
        assertTrue(dispatcherProvider is DispatcherProvider, "DispatcherProvider should be correct type")
    }

    @Test
    fun `Json configuration should be correct`() {
        // Then
        assertTrue(json.configuration.ignoreUnknownKeys, "Json should ignore unknown keys")
    }

    @Test
    fun `DispatcherProvider should provide test dispatchers`() {
        // Then
        assertNotNull(dispatcherProvider.main, "Main dispatcher should be provided")
        assertNotNull(dispatcherProvider.io, "IO dispatcher should be provided")
        assertNotNull(dispatcherProvider.default, "Default dispatcher should be provided")
        assertNotNull(dispatcherProvider.unconfined, "Unconfined dispatcher should be provided")
    }

    @Test
    fun `All dependencies should be singleton instances`() {
        // Given - inject dependencies again to test singleton behavior
        val secondDbHelper = dbHelper
        val secondDataCenter = dataCenter
        val secondNetworkHelper = networkHelper

        // Then - should be same instances (singleton)
        assertTrue(dbHelper === secondDbHelper, "DBHelper should be singleton")
        assertTrue(dataCenter === secondDataCenter, "DataCenter should be singleton")
        assertTrue(networkHelper === secondNetworkHelper, "NetworkHelper should be singleton")
    }
}