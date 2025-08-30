package io.github.gmathi.novellibrary.database

import android.content.Context
import io.github.gmathi.novellibrary.database.dao.impl.NovelDaoImpl
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.mockk.mockk
import org.junit.Test

/**
 * Simple compilation test to verify Task 4.7 changes compile correctly
 */
class DatabaseTask47CompilationTest {

    @Test
    fun `NovelHelper should compile with Hilt constructor injection`() {
        // Given
        val mockContext = mockk<Context>(relaxed = true)
        val mockSourceManager = mockk<SourceManager>(relaxed = true)
        val mockDbHelper = mockk<DBHelper>(relaxed = true)

        // When - This should compile without errors
        val novelHelper = NovelHelper(mockContext, mockSourceManager, mockDbHelper)

        // Then
        assert(novelHelper is NovelHelper)
    }

    @Test
    fun `NovelDaoImpl should compile with constructor injection`() {
        // Given
        val mockDbHelper = mockk<DBHelper>(relaxed = true)
        val mockSourceManager = mockk<SourceManager>(relaxed = true)

        // When - This should compile without errors
        val novelDao = NovelDaoImpl(mockDbHelper, mockSourceManager)

        // Then
        assert(novelDao is NovelDaoImpl)
    }

    @Test
    fun `verify NovelHelper has required annotations`() {
        // This test verifies that the class has the correct annotations
        val novelHelperClass = NovelHelper::class.java
        
        // Check for @Singleton annotation
        val singletonAnnotation = novelHelperClass.getAnnotation(javax.inject.Singleton::class.java)
        assert(singletonAnnotation != null) { "NovelHelper should have @Singleton annotation" }
        
        // Check constructor has @Inject annotation
        val constructor = novelHelperClass.constructors.first()
        val injectAnnotation = constructor.getAnnotation(javax.inject.Inject::class.java)
        assert(injectAnnotation != null) { "NovelHelper constructor should have @Inject annotation" }
    }
}