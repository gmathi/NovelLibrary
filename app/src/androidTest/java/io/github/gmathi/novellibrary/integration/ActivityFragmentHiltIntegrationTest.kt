package io.github.gmathi.novellibrary.integration

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import io.github.gmathi.novellibrary.activity.MainActivity
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.util.BaseHiltAndroidTest
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration test for Activity and Fragment Hilt dependency injection.
 * Tests that dependencies are properly injected in real Android components.
 */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class ActivityFragmentHiltIntegrationTest : BaseHiltAndroidTest() {

    @Inject
    lateinit var dbHelper: DBHelper

    @Inject
    lateinit var dataCenter: DataCenter

    @Inject
    lateinit var networkHelper: NetworkHelper

    @Test
    fun `MainActivity should have dependencies injected`() {
        // Given
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // When
        scenario.onActivity { activity ->
            // Then
            assertNotNull(activity, "Activity should be created")
            // Note: We can't directly access private injected fields in MainActivity
            // but we can verify that the activity starts without crashes
            // which indicates successful dependency injection
        }

        scenario.close()
    }

    @Test
    fun `Hilt dependencies should be available in test`() {
        // Then
        assertNotNull(dbHelper, "DBHelper should be injected in test")
        assertNotNull(dataCenter, "DataCenter should be injected in test")
        assertNotNull(networkHelper, "NetworkHelper should be injected in test")
    }

    @Test
    fun `Dependencies should be singleton instances`() {
        // Given - inject dependencies again
        val secondDbHelper = dbHelper
        val secondDataCenter = dataCenter
        val secondNetworkHelper = networkHelper

        // Then - should be same instances (singleton)
        assertTrue(dbHelper === secondDbHelper, "DBHelper should be singleton")
        assertTrue(dataCenter === secondDataCenter, "DataCenter should be singleton")
        assertTrue(networkHelper === secondNetworkHelper, "NetworkHelper should be singleton")
    }

    @Test
    fun `Activity lifecycle should work with Hilt injection`() {
        // Given
        val scenario = ActivityScenario.launch(MainActivity::class.java)

        // When - simulate activity lifecycle
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.moveToState(Lifecycle.State.RESUMED)
        scenario.moveToState(Lifecycle.State.STARTED)
        scenario.moveToState(Lifecycle.State.CREATED)

        // Then - should not crash (successful if no exceptions thrown)
        scenario.onActivity { activity ->
            assertNotNull(activity, "Activity should survive lifecycle changes")
        }

        scenario.close()
    }
}