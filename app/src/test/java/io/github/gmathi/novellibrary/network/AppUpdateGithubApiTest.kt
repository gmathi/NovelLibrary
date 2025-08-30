package io.github.gmathi.novellibrary.network

import com.google.gson.Gson
import io.github.gmathi.novellibrary.model.other.LatestUpdate
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppUpdateGithubApiTest {

    private lateinit var networkHelper: NetworkHelper
    private lateinit var dataCenter: DataCenter
    private lateinit var gson: Gson
    private lateinit var appUpdateGithubApi: AppUpdateGithubApi

    @Before
    fun setUp() {
        networkHelper = mockk(relaxed = true)
        dataCenter = mockk(relaxed = true)
        gson = mockk(relaxed = true)
        appUpdateGithubApi = AppUpdateGithubApi(networkHelper, dataCenter, gson)
    }

    @Test
    fun `constructor injection works correctly`() {
        // Given
        val api = AppUpdateGithubApi(networkHelper, dataCenter, gson)
        
        // Then
        assertNotNull(api)
    }

    @Test
    fun `getApkUrl returns correct URL format`() {
        // Given
        val latestUpdate = LatestUpdate().apply {
            apk = "app-release.apk"
        }
        
        // When
        val url = appUpdateGithubApi.getApkUrl(latestUpdate)
        
        // Then
        assertEquals("${AppUpdateGithubApi.RELEASES_URL_PREFIX}app-release.apk", url)
    }

    @Test
    fun `companion object constants are correct`() {
        // Then
        assertEquals("https://raw.githubusercontent.com/", AppUpdateGithubApi.BASE_URL)
        assertEquals("${AppUpdateGithubApi.BASE_URL}gmathi/NovelLibrary/releases/", AppUpdateGithubApi.RELEASES_URL_PREFIX)
    }
}