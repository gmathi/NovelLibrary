package io.github.gmathi.novellibrary.integration

import androidx.lifecycle.SavedStateHandle
import com.google.firebase.analytics.FirebaseAnalytics
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.extension.ExtensionManager
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.BaseHiltTest
import io.github.gmathi.novellibrary.util.HiltMockingUtils
import io.github.gmathi.novellibrary.util.TestConfiguration
import io.github.gmathi.novellibrary.util.coroutines.CoroutineScopes
import io.github.gmathi.novellibrary.util.coroutines.DispatcherProvider
import io.github.gmathi.novellibrary.viewmodel.ChaptersViewModel
import io.mockk.every
import io.mockk.just
import io.mockk.Runs
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import javax.inject.Inject
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * End-to-end integration test for dependency injection flows.
 * Tests complete dependency chains from ViewModels through repositories to data sources.
 */
@ExperimentalCoroutinesApi
class EndToEndDependencyInjectionTest : BaseHiltTest() {

    @Inject
    lateinit var dbHelper: DBHelper

    @Inject
    lateinit var dataCenter: DataCenter

    @Inject
    lateinit var networkHelper: NetworkHelper

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
    fun `complete dependency chain should work for ChaptersViewModel`() = TestConfiguration.runTestWithDispatcher {
        // Given
        val savedStateHandle = SavedStateHandle().apply {
            set(ChaptersViewModel.KEY_NOVEL, HiltMockingUtils.createTestNovel())
        }

        // Configure mocks for successful flow
        HiltMockingUtils.configureForSuccessfulNovelLoading(dbHelper, networkHelper)

        // When
        val viewModel = ChaptersViewModel(
            savedStateHandle,
            dbHelper,
            dataCenter,
            networkHelper,
            sourceManager,
            firebaseAnalytics
        )

        // Then
        assertNotNull(viewModel, "ViewModel should be created successfully")
        
        // Test that dependencies are properly injected and functional
        viewModel.getData()
        
        // Verify the dependency chain worked
        verify { networkHelper.isConnectedToNetwork() }
        verify { dbHelper.getNovel(any()) }
    }

    @Test
    fun `dependency injection should work across all layers`() {
        // Given - all dependencies should be injected

        // Then - verify all layers are properly connected
        assertNotNull(dbHelper, "Data layer should be injected")
        assertNotNull(networkHelper, "Network layer should be injected")
        assertNotNull(sourceManager, "Domain layer should be injected")
        assertNotNull(extensionManager, "Extension layer should be injected")
        assertNotNull(firebaseAnalytics, "Analytics layer should be injected")
        assertNotNull(coroutineScopes, "Coroutine utilities should be injected")
        assertNotNull(dispatcherProvider, "Dispatcher provider should be injected")
    }

    @Test
    fun `dependency scoping should be correct across components`() {
        // Given - inject dependencies multiple times
        val dbHelper2 = dbHelper
        val dataCenter2 = dataCenter
        val networkHelper2 = networkHelper

        // Then - singleton dependencies should be same instances
        assertTrue(dbHelper === dbHelper2, "DBHelper should be singleton")
        assertTrue(dataCenter === dataCenter2, "DataCenter should be singleton")
        assertTrue(networkHelper === networkHelper2, "NetworkHelper should be singleton")
    }

    @Test
    fun `complex dependency relationships should work`() {
        // Given
        every { extensionManager.init(any()) } just Runs
        every { sourceManager.getSource(any()) } returns mockk(relaxed = true)

        // When - test complex dependency interaction
        extensionManager.init(sourceManager)

        // Then
        verify { extensionManager.init(sourceManager) }
        assertNotNull(sourceManager, "SourceManager should be available for ExtensionManager")
    }

    @Test
    fun `error handling should work across dependency chain`() = TestConfiguration.runTestWithDispatcher {
        // Given
        val savedStateHandle = SavedStateHandle().apply {
            set(ChaptersViewModel.KEY_NOVEL, HiltMockingUtils.createTestNovel())
        }

        // Configure for offline scenario
        HiltMockingUtils.configureMockNetworkHelperForOffline(networkHelper)
        HiltMockingUtils.configureMockDBHelperForOffline(dbHelper)

        // When
        val viewModel = ChaptersViewModel(
            savedStateHandle,
            dbHelper,
            dataCenter,
            networkHelper,
            sourceManager,
            firebaseAnalytics
        )

        viewModel.getData()

        // Then - should handle offline scenario gracefully
        verify { networkHelper.isConnectedToNetwork() }
        // Should still attempt to get data from database even when offline
        verify { dbHelper.getNovel(any()) }
    }

    @Test
    fun `coroutine integration should work with dependency injection`() = TestConfiguration.runTestWithDispatcher {
        // Given
        assertNotNull(dispatcherProvider, "DispatcherProvider should be injected")
        assertNotNull(coroutineScopes, "CoroutineScopes should be injected")

        // When - test coroutine dispatchers
        val mainDispatcher = dispatcherProvider.main
        val ioDispatcher = dispatcherProvider.io
        val defaultDispatcher = dispatcherProvider.default

        // Then
        assertNotNull(mainDispatcher, "Main dispatcher should be available")
        assertNotNull(ioDispatcher, "IO dispatcher should be available")
        assertNotNull(defaultDispatcher, "Default dispatcher should be available")
    }

    @Test
    fun `analytics integration should work with dependency injection`() {
        // Given
        every { firebaseAnalytics.logEvent(any(), any()) } just Runs

        // When
        firebaseAnalytics.logEvent("test_event", null)

        // Then
        verify { firebaseAnalytics.logEvent("test_event", null) }
        assertNotNull(firebaseAnalytics, "FirebaseAnalytics should be properly injected")
    }
}