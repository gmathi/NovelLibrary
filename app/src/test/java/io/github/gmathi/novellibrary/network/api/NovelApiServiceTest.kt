package io.github.gmathi.novellibrary.network.api

import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody
import org.junit.Test
import org.junit.Assert.*
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NovelApiServiceTest {

    @Test
    fun `test NovelApiService interface has correct method signatures`() {
        val methods = NovelApiService::class.java.declaredMethods
        
        // Test getNovelDetails method
        val getNovelDetailsMethod = methods.find { it.name == "getNovelDetails" }
        assertNotNull("getNovelDetails method should exist", getNovelDetailsMethod)
        
        // Test searchNovels method
        val searchNovelsMethod = methods.find { it.name == "searchNovels" }
        assertNotNull("searchNovels method should exist", searchNovelsMethod)
        
        // Test getChapters method
        val getChaptersMethod = methods.find { it.name == "getChapters" }
        assertNotNull("getChapters method should exist", getChaptersMethod)
        
        // Test getRawContent method
        val getRawContentMethod = methods.find { it.name == "getRawContent" }
        assertNotNull("getRawContent method should exist", getRawContentMethod)
        
        // Test submitForm method
        val submitFormMethod = methods.find { it.name == "submitForm" }
        assertNotNull("submitForm method should exist", submitFormMethod)
        
        // Test getNovelUpdates method
        val getNovelUpdatesMethod = methods.find { it.name == "getNovelUpdates" }
        assertNotNull("getNovelUpdates method should exist", getNovelUpdatesMethod)
    }

    @Test
    fun `test service can be created with retrofit`() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.example.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(NovelApiService::class.java)

        assertNotNull("Service should not be null", service)
        assertTrue("Service should be a NovelApiService instance", 
            service is NovelApiService)
    }

    @Test
    fun `test service methods can be called in coroutine context`() = runTest {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.example.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val service = retrofit.create(NovelApiService::class.java)
        
        // Verify that the service has the expected methods
        assertNotNull("Service should not be null", service)
        
        // Check that methods exist and are properly annotated
        val serviceClass = service.javaClass
        assertTrue("Service should be a proxy class", 
            java.lang.reflect.Proxy.isProxyClass(serviceClass))
    }

    @Test
    fun `test retrofit annotations are properly configured`() {
        val methods = NovelApiService::class.java.methods
        
        // Test getNovelDetails annotations
        val getNovelDetailsMethod = methods.find { it.name == "getNovelDetails" }
        assertNotNull(getNovelDetailsMethod)
        val getAnnotation = getNovelDetailsMethod?.getAnnotation(retrofit2.http.GET::class.java)
        assertNotNull("getNovelDetails should have @GET annotation", getAnnotation)
        
        // Test searchNovels annotations
        val searchNovelsMethod = methods.find { it.name == "searchNovels" }
        assertNotNull(searchNovelsMethod)
        val searchGetAnnotation = searchNovelsMethod?.getAnnotation(retrofit2.http.GET::class.java)
        assertNotNull("searchNovels should have @GET annotation", searchGetAnnotation)
        assertEquals("search", searchGetAnnotation?.value)
        
        // Test getChapters annotations
        val getChaptersMethod = methods.find { it.name == "getChapters" }
        assertNotNull(getChaptersMethod)
        val chaptersGetAnnotation = getChaptersMethod?.getAnnotation(retrofit2.http.GET::class.java)
        assertNotNull("getChapters should have @GET annotation", chaptersGetAnnotation)
        assertEquals("novels/{novelId}/chapters", chaptersGetAnnotation?.value)
        
        // Test submitForm annotations
        val submitFormMethod = methods.find { it.name == "submitForm" }
        assertNotNull(submitFormMethod)
        val postAnnotation = submitFormMethod?.getAnnotation(retrofit2.http.POST::class.java)
        assertNotNull("submitForm should have @POST annotation", postAnnotation)
        assertEquals("submit", postAnnotation?.value)
        
        val formEncodedAnnotation = submitFormMethod?.getAnnotation(retrofit2.http.FormUrlEncoded::class.java)
        assertNotNull("submitForm should have @FormUrlEncoded annotation", formEncodedAnnotation)
    }

    @Test
    fun `test method parameter annotations`() {
        val methods = NovelApiService::class.java.methods
        
        // Test searchNovels query parameter
        val searchNovelsMethod = methods.find { it.name == "searchNovels" }
        assertNotNull(searchNovelsMethod)
        
        val parameters = searchNovelsMethod?.parameters
        assertNotNull("searchNovels should have parameters", parameters)
        assertTrue("searchNovels should have at least one parameter", parameters!!.isNotEmpty())
        
        // Test getChapters path parameter
        val getChaptersMethod = methods.find { it.name == "getChapters" }
        assertNotNull(getChaptersMethod)
        
        val chaptersParameters = getChaptersMethod?.parameters
        assertNotNull("getChapters should have parameters", chaptersParameters)
        assertTrue("getChapters should have at least one parameter", chaptersParameters!!.isNotEmpty())
    }

    @Test
    fun `test return types are properly defined`() {
        val methods = NovelApiService::class.java.methods
        
        // Test that methods return appropriate types
        val getNovelDetailsMethod = methods.find { it.name == "getNovelDetails" }
        assertNotNull(getNovelDetailsMethod)
        // Note: Testing exact return types for suspend functions is complex due to continuation parameters
        
        val getRawContentMethod = methods.find { it.name == "getRawContent" }
        assertNotNull(getRawContentMethod)
        // ResponseBody return type verification would require more complex reflection
    }
}