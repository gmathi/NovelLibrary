package io.github.gmathi.novellibrary.network.api

import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import kotlinx.coroutines.flow.Flow
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Novel API service interface template demonstrating coroutines conversion patterns.
 * This serves as a template for converting RxJava-based API interfaces to coroutines.
 */
interface NovelApiService {

    /**
     * Example: Convert RxJava Single<Novel> to suspend function
     * Before: fun getNovelDetails(@Url url: String): Single<Novel>
     * After: suspend function with proper error handling
     */
    @GET
    suspend fun getNovelDetails(@Url url: String): Response<Novel>

    /**
     * Example: Convert RxJava Observable<List<Novel>> to Flow for streaming data
     * Before: fun searchNovels(@Query("q") query: String): Observable<List<Novel>>
     * After: Flow-based interface for reactive data streams
     */
    @GET("search")
    suspend fun searchNovels(@Query("q") query: String): Response<List<Novel>>

    /**
     * Example: Convert RxJava Observable<WebPage> to Flow for streaming chapter data
     * Before: fun getChapters(@Path("novelId") novelId: String): Observable<List<WebPage>>
     * After: suspend function returning list
     */
    @GET("novels/{novelId}/chapters")
    suspend fun getChapters(@Path("novelId") novelId: String): Response<List<WebPage>>

    /**
     * Example: Raw content fetching with proper error handling
     * Before: fun getRawContent(@Url url: String): Single<ResponseBody>
     * After: suspend function with ResponseBody
     */
    @GET
    suspend fun getRawContent(@Url url: String): ResponseBody

    /**
     * Example: POST request with form data
     * Before: fun submitForm(@FieldMap data: Map<String, String>): Single<ResponseBody>
     * After: suspend function with form data
     */
    @FormUrlEncoded
    @POST("submit")
    suspend fun submitForm(@FieldMap data: Map<String, String>): Response<ResponseBody>

    /**
     * Example: Streaming data with Flow for real-time updates
     * This would be used for scenarios where you need continuous data streams
     * Note: This is a conceptual example - actual streaming would require Server-Sent Events or WebSocket
     */
    @GET("novels/{novelId}/updates")
    suspend fun getNovelUpdates(@Path("novelId") novelId: String): Response<List<Novel>>
}