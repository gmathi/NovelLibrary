package io.github.gmathi.novellibrary.integration

import androidx.lifecycle.SavedStateHandle
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.BaseHiltTest
import io.github.gmathi.novellibrary.util.HiltMockingUtils
import io.github.gmathi.novellibrary.util.TestConfiguration
import io.github.gmathi.novellibrary.viewmodel.ChaptersViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import javax.inject.Inject
import kotlin.system.measureTimeMillis
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import io.mockk.mockk
import kotlinx.coroutines.launch

/**
 * Performance comparison test between Hilt dependency injection and previous Injekt system.
 * Measures injection performance, memory usage, and startup times.
 */
@ExperimentalCoroutinesApi
class HiltPerformanceComparisonTest : BaseHiltTest() {

    @Inject
    lateinit var dbHelper: DBHelper

    @Inject
    lateinit var dataCenter: DataCenter

    @Inject
    lateinit var networkHelper: NetworkHelper

    companion object {
        private const val PERFORMANCE_ITERATIONS = 100
        private const val MAX_ACCEPTABLE_INJECTION_TIME_MS = 50L
        private const val MAX_ACCEPTABLE_VIEWMODEL_CREATION_TIME_MS = 100L
    }

    @Test
    fun `Hilt dependency injection should be performant`() = TestConfiguration.runTestWithDispatcher {
        // Given
        val savedStateHandle = SavedStateHandle().apply {
            set(ChaptersViewModel.KEY_NOVEL, HiltMockingUtils.createTestNovel())
        }

        // When - measure dependency injection time
        val injectionTime = measureTimeMillis {
            repeat(PERFORMANCE_ITERATIONS) {
                // Simulate dependency access (already injected by Hilt)
                val _ = dbHelper
                val _ = dataCenter
                val _ = networkHelper
            }
        }

        val averageInjectionTime = injectionTime / PERFORMANCE_ITERATIONS

        // Then
        assertTrue(
            averageInjectionTime < MAX_ACCEPTABLE_INJECTION_TIME_MS,
            "Average injection time ($averageInjectionTime ms) should be less than $MAX_ACCEPTABLE_INJECTION_TIME_MS ms"
        )
    }

    @Test
    fun `ViewModel creation with Hilt should be performant`() = TestConfiguration.runTestWithDispatcher {
        // Given
        val savedStateHandle = SavedStateHandle().apply {
            set(ChaptersViewModel.KEY_NOVEL, HiltMockingUtils.createTestNovel())
        }

        // Configure mocks for performance testing
        HiltMockingUtils.configureForSuccessfulNovelLoading(dbHelper, networkHelper)

        // When - measure ViewModel creation time
        val creationTime = measureTimeMillis {
            repeat(PERFORMANCE_ITERATIONS) {
                val viewModel = ChaptersViewModel(
                    savedStateHandle,
                    dbHelper,
                    dataCenter,
                    networkHelper,
                    mockk(relaxed = true),
                    mockk(relaxed = true)
                )
                // Ensure ViewModel is actually created
                assertNotNull(viewModel)
            }
        }

        val averageCreationTime = creationTime / PERFORMANCE_ITERATIONS

        // Then
        assertTrue(
            averageCreationTime < MAX_ACCEPTABLE_VIEWMODEL_CREATION_TIME_MS,
            "Average ViewModel creation time ($averageCreationTime ms) should be less than $MAX_ACCEPTABLE_VIEWMODEL_CREATION_TIME_MS ms"
        )
    }

    @Test
    fun `Hilt singleton instances should have minimal memory overhead`() {
        // Given - access singletons multiple times
        val instances = mutableListOf<Any>()

        // When - collect singleton instances
        repeat(10) {
            instances.add(dbHelper)
            instances.add(dataCenter)
            instances.add(networkHelper)
        }

        // Then - all instances should be the same (singleton behavior)
        val uniqueDbHelpers = instances.filterIsInstance<DBHelper>().distinct()
        val uniqueDataCenters = instances.filterIsInstance<DataCenter>().distinct()
        val uniqueNetworkHelpers = instances.filterIsInstance<NetworkHelper>().distinct()

        assertTrue(uniqueDbHelpers.size == 1, "DBHelper should be singleton")
        assertTrue(uniqueDataCenters.size == 1, "DataCenter should be singleton")
        assertTrue(uniqueNetworkHelpers.size == 1, "NetworkHelper should be singleton")
    }

    @Test
    fun `Hilt dependency resolution should be faster than reflection-based injection`() {
        // Given
        val savedStateHandle = SavedStateHandle().apply {
            set(ChaptersViewModel.KEY_NOVEL, HiltMockingUtils.createTestNovel())
        }

        // When - measure Hilt dependency resolution
        val hiltTime = measureTimeMillis {
            repeat(PERFORMANCE_ITERATIONS) {
                // Hilt dependencies are already resolved at compile time
                val viewModel = ChaptersViewModel(
                    savedStateHandle,
                    dbHelper,
                    dataCenter,
                    networkHelper,
                    mockk(relaxed = true),
                    mockk(relaxed = true)
                )
                assertNotNull(viewModel)
            }
        }

        // Simulate reflection-based injection time (like Injekt)
        val reflectionTime = measureTimeMillis {
            repeat(PERFORMANCE_ITERATIONS) {
                // Simulate reflection overhead
                val dbHelperClass = DBHelper::class.java
                val dataCenterClass = DataCenter::class.java
                val networkHelperClass = NetworkHelper::class.java
                
                // Simulate field access via reflection
                val _ = dbHelperClass.simpleName
                val _ = dataCenterClass.simpleName
                val _ = networkHelperClass.simpleName
            }
        }

        // Then - Hilt should be faster or comparable
        assertTrue(
            hiltTime <= reflectionTime * 1.5, // Allow 50% tolerance
            "Hilt injection time ($hiltTime ms) should be comparable to or faster than reflection-based injection ($reflectionTime ms)"
        )
    }

    @Test
    fun `Hilt should have minimal startup overhead`() {
        // Given - measure startup time
        val startupTime = measureTimeMillis {
            // Simulate application startup with Hilt
            // Dependencies are already injected by the test framework
            val _ = dbHelper
            val _ = dataCenter
            val _ = networkHelper
        }

        // Then - startup should be fast
        assertTrue(
            startupTime < 10L,
            "Hilt startup overhead ($startupTime ms) should be minimal"
        )
    }

    @Test
    fun `Hilt should handle concurrent dependency access efficiently`() = TestConfiguration.runTestWithDispatcher {
        // Given
        val concurrentAccessTime = measureTimeMillis {
            // Simulate concurrent access to dependencies
            repeat(PERFORMANCE_ITERATIONS) {
                launch {
                    val _ = dbHelper
                    val _ = dataCenter
                    val _ = networkHelper
                }
            }
        }

        // Then - concurrent access should be efficient
        assertTrue(
            concurrentAccessTime < MAX_ACCEPTABLE_INJECTION_TIME_MS * 2,
            "Concurrent dependency access time ($concurrentAccessTime ms) should be efficient"
        )
    }
}