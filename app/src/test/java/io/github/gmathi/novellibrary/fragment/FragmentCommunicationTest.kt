package io.github.gmathi.novellibrary.fragment

import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.gmathi.novellibrary.model.other.NovelSectionEvent
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class FragmentCommunicationTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun init() {
        hiltRule.inject()
    }

    @Test
    fun `test fragment EventBus communication with Hilt injection`() {
        val scenario = launchFragmentInContainer<TestEventBusFragment>()
        val latch = CountDownLatch(1)
        var receivedEvent: NovelSectionEvent? = null

        scenario.onFragment { fragment ->
            // Set up event listener
            fragment.eventReceived = { event ->
                receivedEvent = event
                latch.countDown()
            }

            // Verify dependencies are injected
            assertNotNull("DataCenter should be injected", fragment.dataCenter)
            assertNotNull("DBHelper should be injected", fragment.dbHelper)

            // Post an event
            EventBus.getDefault().post(NovelSectionEvent(123L))
        }

        // Wait for event to be received
        assertTrue("Event should be received within 5 seconds", latch.await(5, TimeUnit.SECONDS))
        assertNotNull("Event should not be null", receivedEvent)
        assertEquals("Event novelSectionId should match", 123L, receivedEvent?.novelSectionId)
    }

    @Test
    fun `test fragment factory methods work with Hilt`() {
        // Test LibraryFragment.newInstance with parameters
        val libraryFragment = LibraryFragment.newInstance(456L)
        assertNotNull("LibraryFragment should be created", libraryFragment)
        assertEquals("Arguments should be set correctly", 456L, libraryFragment.arguments?.getLong("novelSectionId"))

        // Test ExtensionsFragment.newInstance
        val extensionsFragment = ExtensionsFragment.newInstance()
        assertNotNull("ExtensionsFragment should be created", extensionsFragment)

        // Test SourcesFragment.newInstance
        val sourcesFragment = SourcesFragment.newInstance()
        assertNotNull("SourcesFragment should be created", sourcesFragment)
    }

    @Test
    fun `test fragment dependency injection works with factory methods`() {
        val scenario = launchFragmentInContainer { LibraryFragment.newInstance(789L) }

        scenario.onFragment { fragment ->
            // Verify dependencies are injected even when using factory method
            assertNotNull("FirebaseAnalytics should be injected", fragment.firebaseAnalytics)
            assertNotNull("DataCenter should be injected", fragment.dataCenter)
            assertNotNull("DBHelper should be injected", fragment.dbHelper)
            assertNotNull("SourceManager should be injected", fragment.sourceManager)
            assertNotNull("NetworkHelper should be injected", fragment.networkHelper)

            // Verify arguments are preserved
            assertEquals("Arguments should be preserved", 789L, fragment.arguments?.getLong("novelSectionId"))
        }
    }

    // Test fragment for EventBus communication
    class TestEventBusFragment : BaseFragment() {
        var eventReceived: ((NovelSectionEvent) -> Unit)? = null

        override fun onStart() {
            super.onStart()
            EventBus.getDefault().register(this)
        }

        override fun onStop() {
            EventBus.getDefault().unregister(this)
            super.onStop()
        }

        @Subscribe(threadMode = ThreadMode.MAIN)
        fun onNovelSectionEvent(event: NovelSectionEvent) {
            eventReceived?.invoke(event)
        }
    }
}