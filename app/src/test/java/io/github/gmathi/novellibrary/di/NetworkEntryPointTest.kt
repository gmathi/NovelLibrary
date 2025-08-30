package io.github.gmathi.novellibrary.di

import android.content.Context
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.gmathi.novellibrary.network.NetworkHelper
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class, sdk = [28])
class NetworkEntryPointTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private lateinit var context: Context

    @Before
    fun setUp() {
        hiltRule.inject()
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun `NetworkEntryPoint has correct annotations`() {
        // Given
        val entryPointClass = NetworkEntryPoint::class.java

        // Then
        assertTrue("NetworkEntryPoint should have @EntryPoint annotation", 
            entryPointClass.isAnnotationPresent(dagger.hilt.EntryPoint::class.java))
        assertTrue("NetworkEntryPoint should have @InstallIn annotation", 
            entryPointClass.isAnnotationPresent(dagger.hilt.InstallIn::class.java))
    }

    @Test
    fun `NetworkEntryPoint can be accessed from application context`() {
        // When
        val entryPoint = EntryPointAccessors.fromApplication(context, NetworkEntryPoint::class.java)

        // Then
        assertNotNull("NetworkEntryPoint should be accessible", entryPoint)
    }

    @Test
    fun `NetworkEntryPoint provides NetworkHelper`() {
        // Given
        val entryPoint = EntryPointAccessors.fromApplication(context, NetworkEntryPoint::class.java)

        // When
        val networkHelper = entryPoint.networkHelper()

        // Then
        assertNotNull("NetworkHelper should not be null", networkHelper)
        assertTrue("NetworkHelper should be correct type", networkHelper is NetworkHelper)
    }

    @Test
    fun `NetworkEntryPoint provides Json`() {
        // Given
        val entryPoint = EntryPointAccessors.fromApplication(context, NetworkEntryPoint::class.java)

        // When
        val json = entryPoint.json()

        // Then
        assertNotNull("Json should not be null", json)
        assertTrue("Json should be correct type", json is Json)
        assertTrue("Json should ignore unknown keys", json.configuration.ignoreUnknownKeys)
    }

    @Test
    fun `NetworkEntryPoint provides same instances on multiple calls`() {
        // Given
        val entryPoint = EntryPointAccessors.fromApplication(context, NetworkEntryPoint::class.java)

        // When
        val networkHelper1 = entryPoint.networkHelper()
        val networkHelper2 = entryPoint.networkHelper()
        val json1 = entryPoint.json()
        val json2 = entryPoint.json()

        // Then
        assertSame("NetworkHelper should be singleton", networkHelper1, networkHelper2)
        assertSame("Json should be singleton", json1, json2)
    }

    @Test
    fun `NetworkEntryPoint interface has correct method signatures`() {
        // Given
        val entryPointClass = NetworkEntryPoint::class.java

        // When
        val networkHelperMethod = entryPointClass.getMethod("networkHelper")
        val jsonMethod = entryPointClass.getMethod("json")

        // Then
        assertEquals("networkHelper method should return NetworkHelper", 
            NetworkHelper::class.java, networkHelperMethod.returnType)
        assertEquals("json method should return Json", 
            Json::class.java, jsonMethod.returnType)
    }
}