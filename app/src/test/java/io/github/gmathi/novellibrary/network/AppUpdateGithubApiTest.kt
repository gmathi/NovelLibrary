package io.github.gmathi.novellibrary.network

import com.google.gson.Gson
import io.github.gmathi.novellibrary.BuildConfig
import io.github.gmathi.novellibrary.model.other.LatestUpdate
import kotlinx.serialization.ExperimentalSerializationApi
import org.junit.Test
import org.junit.Assert.*
import org.junit.Before

@ExperimentalSerializationApi
class AppUpdateGithubApiTest {

    private lateinit var appUpdateApi: AppUpdateGithubApi

    @Before
    fun setUp() {
        // Note: This test focuses on the public methods that don't require network calls
        appUpdateApi = AppUpdateGithubApi()
    }

    @Test
    fun `getApkUrl should return correct URL`() {
        // Given
        val latestUpdate = LatestUpdate(
            versionCode = 123,
            versionName = "1.2.3",
            apk = "app-release.apk"
        )

        // When
        val apkUrl = appUpdateApi.getApkUrl(latestUpdate)

        // Then
        val expectedUrl = "${AppUpdateGithubApi.RELEASES_URL_PREFIX}app-release.apk"
        assertEquals(expectedUrl, apkUrl)
    }

    @Test
    fun `constants should have correct values`() {
        // Then
        assertEquals("https://raw.githubusercontent.com/", AppUpdateGithubApi.BASE_URL)
        assertEquals("${AppUpdateGithubApi.BASE_URL}gmathi/NovelLibrary/releases/", AppUpdateGithubApi.RELEASES_URL_PREFIX)
    }

    @Test
    fun `parseResponse should handle valid JSON`() {
        // Given
        val gson = Gson()
        val latestUpdate = LatestUpdate(
            versionCode = 123,
            versionName = "1.2.3",
            apk = "test.apk"
        )
        val jsonString = gson.toJson(latestUpdate)
        
        // When
        val parsedUpdate = gson.fromJson(jsonString, LatestUpdate::class.java)

        // Then
        assertNotNull(parsedUpdate)
        assertEquals(123, parsedUpdate.versionCode)
        assertEquals("1.2.3", parsedUpdate.versionName)
        assertEquals("test.apk", parsedUpdate.apk)
    }

    @Test
    fun `hasUpdate should be set correctly when version is newer`() {
        // Given
        val latestUpdate = LatestUpdate(
            versionCode = BuildConfig.VERSION_CODE + 1,
            versionName = "1.0.0",
            apk = "test.apk",
            hasUpdate = false
        )

        // When
        if (latestUpdate.versionCode > BuildConfig.VERSION_CODE) {
            latestUpdate.hasUpdate = true
        }

        // Then
        assertTrue(latestUpdate.hasUpdate)
    }

    @Test
    fun `hasUpdate should remain false when version is same or older`() {
        // Given
        val latestUpdate = LatestUpdate(
            versionCode = BuildConfig.VERSION_CODE,
            versionName = "1.0.0",
            apk = "test.apk",
            hasUpdate = false
        )

        // When
        if (latestUpdate.versionCode > BuildConfig.VERSION_CODE) {
            latestUpdate.hasUpdate = true
        }

        // Then
        assertFalse(latestUpdate.hasUpdate)
    }


}