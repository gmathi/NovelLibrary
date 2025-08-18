package io.github.gmathi.novellibrary.di

import android.content.Context
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.preference.DataCenter
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DatabaseModuleTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
    }

    @Test
    fun `provideDBHelper returns singleton instance`() {
        // Given
        val module = DatabaseModule

        // When
        val dbHelper1 = module.provideDBHelper(context)
        val dbHelper2 = module.provideDBHelper(context)

        // Then
        assertNotNull(dbHelper1)
        assertNotNull(dbHelper2)
        assertSame("DBHelper should be singleton", dbHelper1, dbHelper2)
    }

    @Test
    fun `provideDataCenter returns valid instance`() {
        // Given
        val module = DatabaseModule

        // When
        val dataCenter = module.provideDataCenter(context)

        // Then
        assertNotNull(dataCenter)
        assertTrue("DataCenter should be properly initialized", dataCenter is DataCenter)
    }

    @Test
    fun `DBHelper getInstance pattern works correctly`() {
        // Given
        val instance1 = DBHelper.getInstance(context)
        val instance2 = DBHelper.getInstance(context)

        // Then
        assertNotNull(instance1)
        assertNotNull(instance2)
        assertSame("DBHelper.getInstance should return same instance", instance1, instance2)
    }

    @Test
    fun `DatabaseModule has correct annotations`() {
        // Given
        val moduleClass = DatabaseModule::class.java

        // Then
        assertTrue("DatabaseModule should have @Module annotation", 
            moduleClass.isAnnotationPresent(dagger.Module::class.java))
        assertTrue("DatabaseModule should have @InstallIn annotation", 
            moduleClass.isAnnotationPresent(dagger.hilt.InstallIn::class.java))
    }

    @Test
    fun `provideDBHelper method has correct annotations`() {
        // Given
        val method = DatabaseModule::class.java.getMethod("provideDBHelper", Context::class.java)

        // Then
        assertTrue("provideDBHelper should have @Provides annotation", 
            method.isAnnotationPresent(dagger.Provides::class.java))
        assertTrue("provideDBHelper should have @Singleton annotation", 
            method.isAnnotationPresent(javax.inject.Singleton::class.java))
    }

    @Test
    fun `provideDataCenter method has correct annotations`() {
        // Given
        val method = DatabaseModule::class.java.getMethod("provideDataCenter", Context::class.java)

        // Then
        assertTrue("provideDataCenter should have @Provides annotation", 
            method.isAnnotationPresent(dagger.Provides::class.java))
        assertTrue("provideDataCenter should have @Singleton annotation", 
            method.isAnnotationPresent(javax.inject.Singleton::class.java))
    }
}