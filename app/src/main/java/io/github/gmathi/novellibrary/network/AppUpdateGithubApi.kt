package io.github.gmathi.novellibrary.network

import android.content.Context
import com.google.gson.Gson
import io.github.gmathi.novellibrary.BuildConfig
import io.github.gmathi.novellibrary.model.other.LatestUpdate
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.util.lang.withIOContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.*
import uy.kohesive.injekt.injectLazy
import java.util.Date

/**
 * Exception thrown when app update operations fail
 */
class AppUpdateException(message: String, cause: Throwable? = null) : Exception(message, cause)

internal class AppUpdateGithubApi {

    private val networkService: NetworkHelper by injectLazy()
    private val dataCenter: DataCenter by injectLazy()
    private val gson: Gson by injectLazy()

    private suspend fun getLatestUpdate(): LatestUpdate {
        return withIOContext {
            try {
                networkService.client
                    .newCall(GET("${RELEASES_URL_PREFIX}latest.json"))
                    .awaitSuccess()
                    .parseAs<JsonObject>()
                    .let { parseResponse(it) }
            } catch (e: Exception) {
                throw AppUpdateException("Failed to fetch latest update information", e)
            }
        }
    }

    @ExperimentalSerializationApi
    suspend fun checkForUpdates(context: Context): LatestUpdate {
        return try {
            val latestUpdate = getLatestUpdate()
            dataCenter.lastExtCheck = Date().time

            if (latestUpdate.versionCode > BuildConfig.VERSION_CODE) {
                latestUpdate.hasUpdate = true
            }

            latestUpdate
        } catch (e: Exception) {
            throw AppUpdateException("Failed to check for app updates", e)
        }
    }

    private fun parseResponse(json: JsonObject): LatestUpdate {
        return gson.fromJson(json.toString(), LatestUpdate::class.java)
    }

    fun getApkUrl(latestUpdate: LatestUpdate): String {
        return "${RELEASES_URL_PREFIX}${latestUpdate.apk}"
    }

    companion object {
        const val BASE_URL = "https://raw.githubusercontent.com/"
        const val RELEASES_URL_PREFIX = "${BASE_URL}gmathi/NovelLibrary/releases/"
    }

}
