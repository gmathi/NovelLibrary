package io.github.gmathi.novellibrary.fragment

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.extension.ExtensionManager
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class FragmentHiltIntegrationTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var dbHelper: DBHelper

    @Inject
    lateinit var dataCenter: DataCenter

    @Inject
    lateinit var networkHelper: NetworkHelper

    @Inject
    lateinit var sourceManager: SourceManager

    @Inject
    lateinit var firebaseAnalytics: FirebaseAnalytics

    @Inject
    lateinit var extensionManager: ExtensionManager

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun `test BaseFragment dependency injection`() {
        val scenario = launchFragmentInContainer<TestBaseFragment>()

        scenario.onFragment { fragment ->
            // Verify all dependencies are injected
            assertNotNull("FirebaseAnalytics should be injected", fragment.firebaseAnalytics)
            assertNotNull("DataCenter should be injected", fragment.dataCenter)
            assertNotNull("DBHelper should be injected", fragment.dbHelper)
            assertNotNull("SourceManager should be injected", fragment.sourceManager)
            assertNotNull("NetworkHelper should be injected", fragment.networkHelper)

            // Verify dependencies are the same instances as injected in test
            assertSame("FirebaseAnalytics should be same instance", firebaseAnalytics, fragment.firebaseAnalytics)
            assertSame("DataCenter should be same instance", dataCenter, fragment.dataCenter)
            assertSame("DBHelper should be same instance", dbHelper, fragment.dbHelper)
            assertSame("SourceManager should be same instance", sourceManager, fragment.sourceManager)
            assertSame("NetworkHelper should be same instance", networkHelper, fragment.networkHelper)
        }
    }

    @Test
    fun `test ExtensionsFragment dependency injection`() {
        val scenario = launchFragmentInContainer<ExtensionsFragment>()

        scenario.onFragment { fragment ->
            // Verify base dependencies are injected
            assertNotNull("FirebaseAnalytics should be injected", fragment.firebaseAnalytics)
            assertNotNull("DataCenter should be injected", fragment.dataCenter)
            assertNotNull("DBHelper should be injected", fragment.dbHelper)
            assertNotNull("SourceManager should be injected", fragment.sourceManager)
            assertNotNull("NetworkHelper should be injected", fragment.networkHelper)

            // Verify additional ExtensionManager dependency is injected
            assertNotNull("ExtensionManager should be injected", fragment.extensionManager)
            assertSame("ExtensionManager should be same instance", extensionManager, fragment.extensionManager)
        }
    }

    @Test
    fun `test SourcesFragment dependency injection`() {
        val scenario = launchFragmentInContainer<SourcesFragment>()

        scenario.onFragment { fragment ->
            // Verify base dependencies are injected
            assertNotNull("FirebaseAnalytics should be injected", fragment.firebaseAnalytics)
            assertNotNull("DataCenter should be injected", fragment.dataCenter)
            assertNotNull("DBHelper should be injected", fragment.dbHelper)
            assertNotNull("SourceManager should be injected", fragment.sourceManager)
            assertNotNull("NetworkHelper should be injected", fragment.networkHelper)

            // Verify additional ExtensionManager dependency is injected
            assertNotNull("ExtensionManager should be injected", fragment.extensionManager)
            assertSame("ExtensionManager should be same instance", extensionManager, fragment.extensionManager)
        }
    }

    @Test
    fun `test LibraryFragment dependency injection`() {
        val scenario = launchFragmentInContainer<LibraryFragment>()

        scenario.onFragment { fragment ->
            // Verify all base dependencies are injected
            assertNotNull("FirebaseAnalytics should be injected", fragment.firebaseAnalytics)
            assertNotNull("DataCenter should be injected", fragment.dataCenter)
            assertNotNull("DBHelper should be injected", fragment.dbHelper)
            assertNotNull("SourceManager should be injected", fragment.sourceManager)
            assertNotNull("NetworkHelper should be injected", fragment.networkHelper)
        }
    }

    @Test
    fun `test SearchFragment dependency injection`() {
        val scenario = launchFragmentInContainer<SearchFragment>()

        scenario.onFragment { fragment ->
            // Verify all base dependencies are injected
            assertNotNull("FirebaseAnalytics should be injected", fragment.firebaseAnalytics)
            assertNotNull("DataCenter should be injected", fragment.dataCenter)
            assertNotNull("DBHelper should be injected", fragment.dbHelper)
            assertNotNull("SourceManager should be injected", fragment.sourceManager)
            assertNotNull("NetworkHelper should be injected", fragment.networkHelper)
        }
    }

    @Test
    fun `test RecentlyUpdatedNovelsFragment dependency injection`() {
        val scenario = launchFragmentInContainer<RecentlyUpdatedNovelsFragment>()

        scenario.onFragment { fragment ->
            // Verify all base dependencies are injected
            assertNotNull("FirebaseAnalytics should be injected", fragment.firebaseAnalytics)
            assertNotNull("DataCenter should be injected", fragment.dataCenter)
            assertNotNull("DBHelper should be injected", fragment.dbHelper)
            assertNotNull("SourceManager should be injected", fragment.sourceManager)
            assertNotNull("NetworkHelper should be injected", fragment.networkHelper)
        }
    }

    @Test
    fun `test RecentlyViewedNovelsFragment dependency injection`() {
        val scenario = launchFragmentInContainer<RecentlyViewedNovelsFragment>()

        scenario.onFragment { fragment ->
            // Verify all base dependencies are injected
            assertNotNull("FirebaseAnalytics should be injected", fragment.firebaseAnalytics)
            assertNotNull("DataCenter should be injected", fragment.dataCenter)
            assertNotNull("DBHelper should be injected", fragment.dbHelper)
            assertNotNull("SourceManager should be injected", fragment.sourceManager)
            assertNotNull("NetworkHelper should be injected", fragment.networkHelper)
        }
    }

    @Test
    fun `test fragment lifecycle and dependency injection timing`() {
        val scenario = launchFragmentInContainer<TestLifecycleFragment>()

        scenario.onFragment { fragment ->
            // Verify dependencies are available in onViewCreated
            assertTrue("Dependencies should be injected before onViewCreated", fragment.dependenciesAvailableInOnViewCreated)
            
            // Verify all dependencies are properly injected
            assertNotNull("FirebaseAnalytics should be injected", fragment.firebaseAnalytics)
            assertNotNull("DataCenter should be injected", fragment.dataCenter)
            assertNotNull("DBHelper should be injected", fragment.dbHelper)
            assertNotNull("SourceManager should be injected", fragment.sourceManager)
            assertNotNull("NetworkHelper should be injected", fragment.networkHelper)
        }
    }

    // Test fragment that extends BaseFragment for testing purposes
    class TestBaseFragment : BaseFragment()

    // Test fragment to verify dependency injection timing
    class TestLifecycleFragment : BaseFragment() {
        var dependenciesAvailableInOnViewCreated = false

        override fun onViewCreated(view: android.view.View, savedInstanceState: android.os.Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            
            // Check if dependencies are available at this point
            dependenciesAvailableInOnViewCreated = try {
                firebaseAnalytics != null &&
                dataCenter != null &&
                dbHelper != null &&
                sourceManager != null &&
                networkHelper != null
            } catch (e: Exception) {
                false
            }
        }
    }
}