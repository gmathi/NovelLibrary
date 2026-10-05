package io.github.gmathi.novellibrary.network

import android.content.Context
import io.github.gmathi.novellibrary.BuildConfig
import io.github.gmathi.novellibrary.model.other.LatestUpdate
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.util.lang.withIOContext
import io.github.gmathi.novellibrary.util.logging.Logs
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import uy.kohesive.injekt.injectLazy
import java.util.Date

internal class AppUpdateGithubApi {

    private val networkService: NetworkHelper by injectLazy()
    private val dataCenter: DataCenter by injectLazy()

    companion object {
        private const val TAG = "AppUpdateGithubApi"

        // GitHub Contents API to list files on the releases branch
        private const val CONTENTS_API_URL =
            "https://api.github.com/repos/gmathi/NovelLibrary/contents/?ref=releases"

        // The releases branch's latest.json, which raw.githubusercontent.com serves without the
        // API's limit of 60 requests an hour per address (phones on one mobile network share one)
        private const val LATEST_JSON_URL =
            "https://raw.githubusercontent.com/gmathi/NovelLibrary/releases/latest.json"

        // Raw download prefix for the releases branch
        private const val RAW_DOWNLOAD_PREFIX =
            "https://github.com/gmathi/NovelLibrary/raw/releases/"

        // Pattern: Novel_Library.ver.<versionName>.build.<versionCode>.apk
        private val APK_REGEX =
            Regex("""Novel_Library\.ver\.(.+?)\.build\.(\d+)\.apk""")
    }

    /**
     * The latest release, from the releases branch: its file listing, or its latest.json when the
     * GitHub API refuses the listing (such as when the address has used up its hourly requests).
     */
    private suspend fun getLatestUpdate(): LatestUpdate? {
        return try {
            getLatestFromListing()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Logs.warning(TAG, "Release listing failed, reading latest.json instead: ${e.localizedMessage}")
            getLatestFromJson()
        }
    }

    /**
     * Fetches the file listing from the releases branch via the GitHub Contents API,
     * finds all APK files, parses version info from filenames, and returns the latest.
     */
    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun getLatestFromListing(): LatestUpdate? {
        return withIOContext {
            val response = networkService.client
                .newCall(GET(CONTENTS_API_URL))
                .await()

            val jsonArray = response.parseAs<JsonArray>()

            // Collect all APK entries with parsed version info
            val apkEntries = jsonArray.mapNotNull { element ->
                val name = element.jsonObject["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val match = APK_REGEX.matchEntire(name) ?: return@mapNotNull null
                val versionName = match.groupValues[1]
                val versionCode = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
                LatestUpdate(versionCode = versionCode, versionName = versionName, apk = name)
            }

            // Return the one with the highest versionCode
            apkEntries.maxByOrNull { it.versionCode }
        }
    }

    /** Reads the release named in the releases branch's latest.json. */
    @OptIn(ExperimentalSerializationApi::class)
    private suspend fun getLatestFromJson(): LatestUpdate? {
        return withIOContext {
            val json = networkService.client
                .newCall(GET(LATEST_JSON_URL))
                .await()
                .parseAs<JsonObject>()
            val versionCode = json["versionCode"]?.jsonPrimitive?.intOrNull ?: return@withIOContext null
            val versionName = json["versionName"]?.jsonPrimitive?.contentOrNull ?: return@withIOContext null
            val apk = json["apk"]?.jsonPrimitive?.contentOrNull ?: return@withIOContext null
            LatestUpdate(versionCode = versionCode, versionName = versionName, apk = apk)
        }
    }

    @ExperimentalSerializationApi
    suspend fun checkForUpdates(context: Context): LatestUpdate {
        val latestUpdate = getLatestUpdate()
            ?: return LatestUpdate(versionCode = 0, versionName = "", apk = "")

        dataCenter.lastExtCheck = Date().time

        if (latestUpdate.versionCode > BuildConfig.VERSION_CODE) {
            latestUpdate.hasUpdate = true
            Logs.info(TAG, "Update found: ${latestUpdate.versionName} (build ${latestUpdate.versionCode})")
        }

        return latestUpdate
    }

    fun getApkUrl(latestUpdate: LatestUpdate): String {
        return "$RAW_DOWNLOAD_PREFIX${latestUpdate.apk}"
    }
}
