package io.github.gmathi.novellibrary.activity.settings

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers.isDialog
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.gmathi.novellibrary.R
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.util.storage.StorageLocation
import io.github.gmathi.novellibrary.util.storage.StorageLocationResolver
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented test validating [StorageSettingsActivity.confirmLocationChange]:
 *
 *  - Tapping a non-active [StorageOption][io.github.gmathi.novellibrary.util.storage.StorageOption]
 *    row shows the confirmation `AlertDialog` (title + message) *before* any migration/progress UI
 *    appears, matching Requirement 2.5 ("WHEN the user selects a location different from the active
 *    one, THE system SHALL request confirmation before changing the Download_Storage_Location").
 *
 * Locating a non-active row without real SD card hardware:
 *
 * The test emulator/device has no removable SD volume, so [StorageLocationResolver.listOptions]
 * would normally only ever return a single, already-active `Internal` option — there would be no
 * non-active row to tap. To still exercise a genuine non-active option, this test persists a
 * *configured-but-absent* SD volume token (`"sd:TEST-0001"`) into [DataCenter] before launching the
 * activity. Per [StorageLocationResolver.listOptions]'s documented behavior, a configured SD volume
 * that isn't present in `getExternalFilesDirs()` still appears in the option list — marked
 * unavailable and *not* active (the active marker falls back to `Internal`, mirroring
 * [StorageLocationResolver.resolveWritableRoot]'s fallback). That configured-but-absent row is a
 * real non-active [StorageOption][io.github.gmathi.novellibrary.util.storage.StorageOption], so
 * tapping it exercises [StorageSettingsActivity.onItemClick] /
 * [StorageSettingsActivity.confirmLocationChange] exactly as tapping a real, present-but-inactive
 * SD card would, without requiring SD hardware in the test environment.
 *
 * The preference is restored to its prior value in [tearDown] so this test does not leak state
 * into other tests or the developer's own app data.
 */
@RunWith(AndroidJUnit4::class)
class StorageSettingsConfirmationDialogInstrumentedTest {

    private lateinit var dataCenter: DataCenter
    private var originalLocation: String = StorageLocation.INTERNAL_TOKEN

    private val fakeAbsentSdToken = "${StorageLocation.SD_PREFIX}TEST-0001"

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        dataCenter = DataCenter(context)
        originalLocation = dataCenter.downloadStorageLocation

        // Configure a location for a SD volume that is guaranteed absent on the test
        // device/emulator, so the settings list contains a genuine non-active, unavailable row
        // (see class doc) alongside the always-active Internal row.
        dataCenter.downloadStorageLocation = fakeAbsentSdToken
    }

    @After
    fun tearDown() {
        dataCenter.downloadStorageLocation = originalLocation
    }

    @Test
    fun selectingNonActiveOption_showsConfirmationDialog_beforeAnyChangeOccurs() {
        val scenario = ActivityScenario.launch(StorageSettingsActivity::class.java)
        try {
            // Sanity check on the fixture: the configured SD volume must actually be absent on
            // this test device, otherwise the row we're about to tap would be an available,
            // active SD option instead of the unavailable/non-active one this test requires.
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val options = StorageLocationResolver.listOptions(context, dataCenter)
            val configuredOption = options.firstOrNull { it.location == StorageLocation.SdCard("TEST-0001", "") }
            check(configuredOption != null) { "Expected the configured-but-absent SD option to be listed" }
            check(!configuredOption.isActive) { "Fixture invalid: configured option must not be active" }
            check(options.first { it.location is StorageLocation.Internal }.isActive) {
                "Fixture invalid: Internal must be the active fallback option"
            }

            // The configured-but-absent SD option is listed after Internal (position 1).
            val nonActiveRowPosition = 1

            onView(withId(R.id.recyclerView))
                .perform(RecyclerViewActions.actionOnItemAtPosition<androidx.recyclerview.widget.RecyclerView.ViewHolder>(nonActiveRowPosition, click()))

            // The confirmation dialog (title + message) must be showing now, before migration starts.
            onView(withText(R.string.storage_change_confirm_title))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))

            onView(withText(R.string.okay))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))

            onView(withText(R.string.cancel))
                .inRoot(isDialog())
                .check(matches(isDisplayed()))

            // No migration has started yet: the progress dialog must not be showing anywhere,
            // confirming confirmLocationChange() has not proceeded past confirmation.
            onView(withText(R.string.storage_migration_progress_title))
                .check(doesNotExist())
        } finally {
            scenario.close()
        }
    }
}
