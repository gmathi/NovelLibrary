package io.github.gmathi.novellibrary.network.api

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Url

/**
 * GitHub API service interface using Retrofit with coroutines support
 */
interface GitHubApiService {

    /**
     * Get latest release information from GitHub
     * @param owner Repository owner
     * @param repo Repository name
     * @return Release information as raw JSON string
     */
    @GET("repos/{owner}/{repo}/releases/latest")
    suspend fun getLatestRelease(
        @Path("owner") owner: String,
        @Path("repo") repo: String
    ): okhttp3.ResponseBody

    /**
     * Get content from a raw GitHub URL
     * @param url Full URL to the raw content
     * @return Raw content as ResponseBody
     */
    @GET
    suspend fun getRawContent(@Url url: String): okhttp3.ResponseBody
}