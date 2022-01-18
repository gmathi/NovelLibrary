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

internal class AppUpdateGithubApi {

    private val networkService: NetworkHelper by injectLazy()
    private val dataCenter: DataCenter by injectLazy()
    private val gson: Gson by injectLazy()

    private suspend fun getLatestUpdate(): LatestUpdate {
        return withIOContext {
            networkService.client
                .newCall(GET("${RELEASES_URL_PREFIX}latest.json"))
                .await()
                .parseAs<JsonObject>()
                .let { parseResponse(it) }
        }
    }

    @ExperimentalSerializationApi
    suspend fun checkForUpdates(context: Context): LatestUpdate {
        val latestUpdate = getLatestUpdate()
        dataCenter.lastExtCheck = Date().time

        if (latestUpdate.versionCode > BuildConfig.VERSION_CODE) {
            latestUpdate.hasUpdate = true
        }

        return latestUpdate
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
