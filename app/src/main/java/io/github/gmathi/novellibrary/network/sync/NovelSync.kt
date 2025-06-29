package io.github.gmathi.novellibrary.network.sync

import android.webkit.CookieManager
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.NovelSection
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.util.lang.containsCaseInsensitive
import kotlinx.coroutines.*
import uy.kohesive.injekt.injectLazy

abstract class NovelSync {

    companion object {

        fun getInstance(novel: Novel, ignoreEnabled: Boolean = false): NovelSync? {
            return getInstance(novel.url, ignoreEnabled)
        }

        fun getInstance(url: String, ignoreEnabled: Boolean = false): NovelSync? {
            val dataCenter: DataCenter by injectLazy()
            return when {
                url.containsCaseInsensitive(HostNames.NOVEL_UPDATES) && (ignoreEnabled || dataCenter.getSyncEnabled(HostNames.NOVEL_UPDATES)) -> return NovelUpdatesSync()
                else -> null
            }
        }

        fun getAllInstances(ignoreEnabled: Boolean = false): List<NovelSync> {
            val list = ArrayList<NovelSync>()
            val dataCenter: DataCenter by injectLazy()
            if (ignoreEnabled || dataCenter.getSyncEnabled(HostNames.NOVEL_UPDATES)) list.add(NovelUpdatesSync())
            return list
        }

    }

    abstract val host: String

    val dbHelper: DBHelper by injectLazy()
    val dataCenter: DataCenter by injectLazy()
    val networkHelper: NetworkHelper by injectLazy()
    val sourceManager: SourceManager by injectLazy()

    fun forget() {
        dataCenter.deleteLoginCookieString(host)
        dataCenter.deleteLoginCookieString("www.$host")
        dataCenter.deleteLoginCookieString("m.$host")
        val manager = CookieManager.getInstance()
        manager.setCookie("https://$host/", "")
        manager.setCookie("https://www.$host/", "")
        manager.setCookie("https://m.$host/", "")
    }

    fun applyAsync(scope: CoroutineScope? = null, block: (NovelSync) -> Unit) {
        if (scope == null) {
            // Use a supervised scope to prevent cancellation of other coroutines
            CoroutineScope(SupervisorJob() + Dispatchers.IO).launch { 
                block(this@NovelSync) 
            }
        } else {
            scope.launch { withContext(Dispatchers.IO) { block(this@NovelSync) } }
        }
    }

    // Login
    abstract fun loggedIn(): Boolean
    abstract fun getLoginURL(): String
    abstract fun getCookieLookupRegex(): String

    // App -> Remote
    fun addNovel(novel: Novel): Boolean {
        return addNovel(novel, null)
    }

    abstract fun addNovel(novel: Novel, section: NovelSection?): Boolean
    abstract fun removeNovel(novel: Novel): Boolean
    abstract fun updateNovel(novel: Novel, section: NovelSection?): Boolean

    fun batchAdd(novels: List<Novel>, sections: List<NovelSection>): Boolean {
        return batchAdd(novels, sections, null)
    }

    abstract fun batchAdd(novels: List<Novel>, sections: List<NovelSection>, progress: ((String) -> Unit)?): Boolean

    abstract fun setBookmark(novel: Novel, chapter: WebPage): Boolean
    abstract fun clearBookmark(novel: Novel, chapter: WebPage): Boolean

    abstract fun addSection(section: NovelSection): Boolean
    abstract fun removeSection(section: NovelSection): Boolean
    abstract fun renameSection(section: NovelSection, oldName: String?, newName: String?): Boolean

    // Remote -> App
    abstract fun getCategories(): List<String>
//abstract fun getNovelsInCategory(category : NovelGenre)
// TODO: Remote -> app sync
}