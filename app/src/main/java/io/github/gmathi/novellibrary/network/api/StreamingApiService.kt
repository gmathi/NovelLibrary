package io.github.gmathi.novellibrary.network.api

import kotlinx.coroutines.flow.Flow
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Url

/**
 * Streaming API service interface demonstrating Flow-based reactive patterns.
 * This template shows how to convert RxJava Observable patterns to Kotlin Flow.
 */
interface StreamingApiService {

    /**
     * Example: Convert RxJava Observable<String> to Flow<String> for streaming content
     * This would be implemented in the repository layer using Flow.flow { } builder
     * 
     * Usage in repository:
     * fun getStreamingContent(url: String): Flow<String> = flow {
     *     val response = apiService.getStreamingContent(url)
     *     emit(response.body()?.string() ?: "")
     * }.flowOn(Dispatchers.IO)
     */
    @GET
    suspend fun getStreamingContent(@Url url: String): ResponseBody

    /**
     * Example: Paginated data that can be converted to Flow
     * Repository would implement Flow that handles pagination automatically
     * 
     * Usage in repository:
     * fun getPaginatedNovels(query: String): Flow<List<Novel>> = flow {
     *     var page = 1
     *     do {
     *         val response = apiService.getPaginatedData(query, page)
     *         if (response.isSuccessful) {
     *             response.body()?.let { novels ->
     *                 emit(novels)
     *                 page++
     *             }
     *         }
     *     } while (response.body()?.isNotEmpty() == true)
     * }.flowOn(Dispatchers.IO)
     */
    @GET("paginated")
    suspend fun getPaginatedData(
        @Query("q") query: String,
        @Query("page") page: Int
    ): Response<List<String>>

    /**
     * Example: Real-time updates that would be polled and converted to Flow
     * Repository would implement polling mechanism using Flow
     * 
     * Usage in repository:
     * fun getRealtimeUpdates(id: String): Flow<UpdateData> = flow {
     *     while (currentCoroutineContext().isActive) {
     *         try {
     *             val response = apiService.getUpdates(id)
     *             if (response.isSuccessful) {
     *                 response.body()?.let { emit(it) }
     *             }
     *             delay(5000) // Poll every 5 seconds
     *         } catch (e: Exception) {
     *             // Handle error or emit error state
     *         }
     *     }
     * }.flowOn(Dispatchers.IO)
     */
    @GET("updates/{id}")
    suspend fun getUpdates(@Path("id") id: String): Response<String>
}