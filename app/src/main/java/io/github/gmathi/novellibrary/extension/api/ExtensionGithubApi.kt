package io.github.gmathi.novellibrary.extension.api

import android.content.Context
import io.github.gmathi.novellibrary.extension.model.Extension
import io.github.gmathi.novellibrary.extension.model.LoadResult
import io.github.gmathi.novellibrary.extension.util.ExtensionLoader
import io.github.gmathi.novellibrary.network.GET
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.network.RequestPriority
import io.github.gmathi.novellibrary.network.await
import io.github.gmathi.novellibrary.network.parseAs
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.util.lang.withIOContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import uy.kohesive.injekt.injectLazy
import java.util.Date

internal class ExtensionGithubApi {

    private val networkService: NetworkHelper by injectLazy()
    private val dataCenter: DataCenter by injectLazy()

    suspend fun findExtensions(): List<Extension.Available> {
        return withIOContext {
            networkService.newCallWithPriority(GET("${REPO_URL_PREFIX}index.json"), RequestPriority.NORMAL)
                .await()
                .parseAs<JsonArray>()
                .let { parseResponse(it) }
        }
    }

    suspend fun checkForUpdates(context: Context): List<Extension.Installed> {
        val extensions = findExtensions()
        dataCenter.lastExtCheck = Date().time

        val installedExtensions = ExtensionLoader.loadExtensions(context)
            .filterIsInstance<LoadResult.Success>()
            .map { it.extension }

        val extensionsWithUpdate = mutableListOf<Extension.Installed>()
        for (installedExt in installedExtensions) {
            val pkgName = installedExt.pkgName
            val availableExt = extensions.find { it.pkgName == pkgName } ?: continue

            val hasUpdate = availableExt.versionCode > installedExt.versionCode
            if (hasUpdate) {
                extensionsWithUpdate.add(installedExt)
            }
        }

        return extensionsWithUpdate
    }

    private fun parseResponse(json: JsonArray): List<Extension.Available> {
        return json
            .filter { element ->
                val versionName = element.jsonObject["version"]!!.jsonPrimitive.content
                val libVersion = versionName.substringBeforeLast('.').toDouble()
                libVersion >= ExtensionLoader.LIB_VERSION_MIN && libVersion <= ExtensionLoader.LIB_VERSION_MAX
            }
            .map { element ->
                val name = element.jsonObject["name"]!!.jsonPrimitive.content.substringAfter("NovelLibrary: ")
                val pkgName = element.jsonObject["pkg"]!!.jsonPrimitive.content
                val apkName = element.jsonObject["apk"]!!.jsonPrimitive.content
                val versionName = element.jsonObject["version"]!!.jsonPrimitive.content
                val versionCode = element.jsonObject["code"]!!.jsonPrimitive.int
                val lang = element.jsonObject["lang"]!!.jsonPrimitive.content
                val nsfw = element.jsonObject["nsfw"]!!.jsonPrimitive.int == 1
                val icon = "${REPO_URL_PREFIX}icon/${apkName.replace(".apk", ".png")}"

                Extension.Available(name, pkgName, versionName, versionCode, lang, nsfw, apkName, icon)
            }
    }

    fun getApkUrl(extension: Extension.Available): String {
        return "${REPO_URL_PREFIX}apk/${extension.apkName}"
    }

    companion object {
        const val BASE_URL = "https://raw.githubusercontent.com/"
        const val REPO_URL_PREFIX = "${BASE_URL}gmathi/NovelLibrary-Extensions/repo/"
    }

}
