package io.github.gmathi.novellibrary.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.gmathi.novellibrary.R
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class FragmentLifecycleHiltTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun `test fragment lifecycle with Hilt dependency injection`() {
        val scenario = launchFragmentInContainer<TestLifecycleFragment>()

        scenario.onFragment { fragment ->
            // Verify fragment is in RESUMED state
            assertEquals("Fragment should be in RESUMED state", Lifecycle.State.RESUMED, fragment.lifecycle.currentState)

            // Verify dependencies are available throughout lifecycle
            assertTrue("Dependencies should be available in onCreate", fragment.dependenciesAvailableInOnCreate)
            assertTrue("Dependencies should be available in onCreateView", fragment.dependenciesAvailableInOnCreateView)
            assertTrue("Dependencies should be available in onViewCreated", fragment.dependenciesAvailableInOnViewCreated)
            assertTrue("Dependencies should be available in onActivityCreated", fragment.dependenciesAvailableInOnActivityCreated)

            // Verify all dependencies are properly injected
            assertNotNull("FirebaseAnalytics should be injected", fragment.firebaseAnalytics)
            assertNotNull("DataCenter should be injected", fragment.dataCenter)
            assertNotNull("DBHelper should be injected", fragment.dbHelper)
            assertNotNull("SourceManager should be injected", fragment.sourceManager)
            assertNotNull("NetworkHelper should be injected", fragment.networkHelper)
        }
    }

    @Test
    fun `test fragment recreation preserves dependency injection`() {
        val scenario = launchFragmentInContainer<TestLifecycleFragment>()

        // Simulate configuration change
        scenario.recreate()

        scenario.onFragment { fragment ->
            // Verify dependencies are still available after recreation
            assertNotNull("FirebaseAnalytics should be injected after recreation", fragment.firebaseAnalytics)
            assertNotNull("DataCenter should be injected after recreation", fragment.dataCenter)
            assertNotNull("DBHelper should be injected after recreation", fragment.dbHelper)
            assertNotNull("SourceManager should be injected after recreation", fragment.sourceManager)
            assertNotNull("NetworkHelper should be injected after recreation", fragment.networkHelper)

            // Verify lifecycle callbacks were called with dependencies available
            assertTrue("Dependencies should be available in onCreate after recreation", fragment.dependenciesAvailableInOnCreate)
            assertTrue("Dependencies should be available in onCreateView after recreation", fragment.dependenciesAvailableInOnCreateView)
            assertTrue("Dependencies should be available in onViewCreated after recreation", fragment.dependenciesAvailableInOnViewCreated)
        }
    }

    @Test
    fun `test fragment with arguments preserves dependency injection`() {
        val bundle = Bundle().apply {
            putString("test_key", "test_value")
            putLong("test_long", 12345L)
        }

        val scenario = launchFragmentInContainer<TestLifecycleFragment>(fragmentArgs = bundle)

        scenario.onFragment { fragment ->
            // Verify arguments are preserved
            assertEquals("String argument should be preserved", "test_value", fragment.arguments?.getString("test_key"))
            assertEquals("Long argument should be preserved", 12345L, fragment.arguments?.getLong("test_long"))

            // Verify dependencies are still injected
            assertNotNull("FirebaseAnalytics should be injected with arguments", fragment.firebaseAnalytics)
            assertNotNull("DataCenter should be injected with arguments", fragment.dataCenter)
            assertNotNull("DBHelper should be injected with arguments", fragment.dbHelper)
            assertNotNull("SourceManager should be injected with arguments", fragment.sourceManager)
            assertNotNull("NetworkHelper should be injected with arguments", fragment.networkHelper)
        }
    }

    // Test fragment to verify dependency injection timing throughout lifecycle
    class TestLifecycleFragment : BaseFragment() {
        var dependenciesAvailableInOnCreate = false
        var dependenciesAvailableInOnCreateView = false
        var dependenciesAvailableInOnViewCreated = false
        var dependenciesAvailableInOnActivityCreated = false

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            dependenciesAvailableInOnCreate = checkDependenciesAvailable()
        }

        override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
            dependenciesAvailableInOnCreateView = checkDependenciesAvailable()
            return inflater.inflate(R.layout.content_recycler_view, container, false)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            dependenciesAvailableInOnViewCreated = checkDependenciesAvailable()
        }

        override fun onActivityCreated(savedInstanceState: Bundle?) {
            super.onActivityCreated(savedInstanceState)
            dependenciesAvailableInOnActivityCreated = checkDependenciesAvailable()
        }

        private fun checkDependenciesAvailable(): Boolean {
            return try {
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