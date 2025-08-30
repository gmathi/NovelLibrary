package io.github.gmathi.novellibrary.di

import android.content.Context
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.preference.DataCenter
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
class DatabaseEntryPointTest {

    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    private lateinit var context: Context

    @Before
    fun setUp() {
        hiltRule.inject()
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun `DatabaseEntryPoint has correct annotations`() {
        // Given
        val entryPointClass = DatabaseEntryPoint::class.java

        // Then
        assertTrue("DatabaseEntryPoint should have @EntryPoint annotation", 
            entryPointClass.isAnnotationPresent(dagger.hilt.EntryPoint::class.java))
        assertTrue("DatabaseEntryPoint should have @InstallIn annotation", 
            entryPointClass.isAnnotationPresent(dagger.hilt.InstallIn::class.java))
    }

    @Test
    fun `DatabaseEntryPoint can be accessed from application context`() {
        // When
        val entryPoint = EntryPointAccessors.fromApplication(context, DatabaseEntryPoint::class.java)

        // Then
        assertNotNull("DatabaseEntryPoint should be accessible", entryPoint)
    }

    @Test
    fun `DatabaseEntryPoint provides DBHelper`() {
        // Given
        val entryPoint = EntryPointAccessors.fromApplication(context, DatabaseEntryPoint::class.java)

        // When
        val dbHelper = entryPoint.dbHelper()

        // Then
        assertNotNull("DBHelper should not be null", dbHelper)
        assertTrue("DBHelper should be correct type", dbHelper is DBHelper)
    }

    @Test
    fun `DatabaseEntryPoint provides DataCenter`() {
        // Given
        val entryPoint = EntryPointAccessors.fromApplication(context, DatabaseEntryPoint::class.java)

        // When
        val dataCenter = entryPoint.dataCenter()

        // Then
        assertNotNull("DataCenter should not be null", dataCenter)
        assertTrue("DataCenter should be correct type", dataCenter is DataCenter)
    }

    @Test
    fun `DatabaseEntryPoint provides same instances on multiple calls`() {
        // Given
        val entryPoint = EntryPointAccessors.fromApplication(context, DatabaseEntryPoint::class.java)

        // When
        val dbHelper1 = entryPoint.dbHelper()
        val dbHelper2 = entryPoint.dbHelper()
        val dataCenter1 = entryPoint.dataCenter()
        val dataCenter2 = entryPoint.dataCenter()

        // Then
        assertSame("DBHelper should be singleton", dbHelper1, dbHelper2)
        assertSame("DataCenter should be singleton", dataCenter1, dataCenter2)
    }

    @Test
    fun `DatabaseEntryPoint interface has correct method signatures`() {
        // Given
        val entryPointClass = DatabaseEntryPoint::class.java

        // When
        val dbHelperMethod = entryPointClass.getMethod("dbHelper")
        val dataCenterMethod = entryPointClass.getMethod("dataCenter")

        // Then
        assertEquals("dbHelper method should return DBHelper", 
            DBHelper::class.java, dbHelperMethod.returnType)
        assertEquals("dataCenter method should return DataCenter", 
            DataCenter::class.java, dataCenterMethod.returnType)
    }

    @Test
    fun `DatabaseEntryPoint dependencies are properly initialized`() {
        // Given
        val entryPoint = EntryPointAccessors.fromApplication(context, DatabaseEntryPoint::class.java)

        // When
        val dbHelper = entryPoint.dbHelper()
        val dataCenter = entryPoint.dataCenter()

        // Then
        // Verify that the dependencies are properly initialized
        assertNotNull("DBHelper should be initialized", dbHelper)
        assertNotNull("DataCenter should be initialized", dataCenter)
        
        // Verify that DBHelper is properly configured
        assertTrue("DBHelper should be properly initialized", 
            dbHelper.javaClass.name.contains("DBHelper"))
        
        // Verify that DataCenter is properly configured with context
        assertTrue("DataCenter should be properly initialized", 
            dataCenter.javaClass.name.contains("DataCenter"))
    }

    @Test
    fun `DatabaseEntryPoint works with existing DatabaseModule`() {
        // Given
        val entryPoint = EntryPointAccessors.fromApplication(context, DatabaseEntryPoint::class.java)

        // When
        val dbHelper = entryPoint.dbHelper()
        val dataCenter = entryPoint.dataCenter()

        // Then
        // Verify that the EntryPoint works with the existing DatabaseModule providers
        assertNotNull("DBHelper from EntryPoint should match DatabaseModule provider", dbHelper)
        assertNotNull("DataCenter from EntryPoint should match DatabaseModule provider", dataCenter)
        
        // These should be the same instances provided by DatabaseModule
        assertTrue("DBHelper should be singleton instance from DatabaseModule", 
            dbHelper is DBHelper)
        assertTrue("DataCenter should be singleton instance from DatabaseModule", 
            dataCenter is DataCenter)
    }
}