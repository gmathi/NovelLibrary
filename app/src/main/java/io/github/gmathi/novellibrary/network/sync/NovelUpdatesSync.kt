package io.github.gmathi.novellibrary.network.sync

import co.metalab.asyncawait.async
import io.github.gmathi.novellibrary.database.getWebPage
import io.github.gmathi.novellibrary.dbHelper
import io.github.gmathi.novellibrary.model.Novel
import io.github.gmathi.novellibrary.model.NovelGenre
import io.github.gmathi.novellibrary.model.NovelSection
import io.github.gmathi.novellibrary.model.WebPage
import io.github.gmathi.novellibrary.network.CloudFlareByPasser
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.network.NovelApi
import org.jsoup.select.Elements
import java.io.IOException
import java.net.CookieManager
import java.net.URL

class NovelUpdatesSync : NovelSync() {

    companion object {
        const val READINGLIST_UPDATE_URL = "https://" + HostNames.NOVEL_UPDATES + "/readinglist_update.php?sid=%s&rid=%s&checked=%s"
        const val UPDATE_NOVEL_URL = "https://" + HostNames.NOVEL_UPDATES + "/updatelist.php?sid=%s&lid=%d&act=%s"
        const val CATEGORY_LIST_URL = "https://" + HostNames.NOVEL_UPDATES + "/create-reading-lists/"
        const val LOGIN_URL = "https://" + HostNames.NOVEL_UPDATES + "/login/"
        const val COOKIE_REGEX = "wordpress_(logged_in|sec|user_sw)_"

        @Suppress("NOTHING_TO_INLINE")
        inline fun validateNovel(novel : Novel) {
            if (!novel.metaData.containsKey("PostId")) throw Exception("No PostId Found!")
            //if (!novel.url.contains(HostNames.NOVEL_UPDATES)) throw Exception("Not a NU novel!")
        }
    }

    override val host: String = HostNames.NOVEL_UPDATES

    override fun loggedIn(): Boolean {
        val testReg = COOKIE_REGEX.toRegex()
        return CloudFlareByPasser.getCookieMap(URL("https://" + HostNames.NOVEL_UPDATES)).any {
            testReg.containsMatchIn(it.key)
        }
    }

    override fun getLoginURL(): String {
        return LOGIN_URL
    }

    override fun getCookieLookupRegex(): String {
        return COOKIE_REGEX
    }

    override fun addNovel(novel: Novel, section : NovelSection?) : Boolean {
        validateNovel(novel)

        val category = getSectionIndex(section)
        return try {
            NovelApi.getString(UPDATE_NOVEL_URL.format(novel.metaData["PostId"], category, "move"), ignoreHttpErrors = false)
            true;
        } catch (e : IOException) {
            false;
        }
    }

    override fun removeNovel(novel: Novel) : Boolean {
        validateNovel(novel)

        return try {
            NovelApi.getString(READINGLIST_UPDATE_URL.format(novel.metaData["PostId"], "0", "noo"), ignoreHttpErrors = false)
            true;
        } catch (e : IOException) {
            false;
        }
    }

    override fun updateNovel(novel: Novel, section : NovelSection?) : Boolean {
        return addNovel(novel, section)
    }

    override fun batchAdd(novels: List<Novel>, sections: List<NovelSection>, predicate: ((Novel, Boolean) -> Unit)?): Boolean {
        async {

            val categories = ArrayList(await { fetchCategories() })
            val categoryMap = HashMap<Long, Int>()
            val initialCategoryCount = categories.count()

            sections.forEach { section ->
                val name = section.name ?: "NL Section ${section.id}"
                var index = categories.indexOfFirst { cat -> cat.name == name }
                if (index == -1) {
                    index = categories.count()
                    categories.add(CategoryInfo(name))
                }
                categoryMap[section.id] = index
            }

            if (initialCategoryCount != categories.count()) {
                if (!await { setCategories(categories) }) return@async
            }

            novels.forEach { novel ->
                try {
                    if (predicate != null) predicate(novel, false)
                    await { NovelApi.getString(UPDATE_NOVEL_URL.format(novel.metaData["PostId"], categoryMap[novel.novelSectionId] ?: 0, "move"), ignoreHttpErrors = false) }
                    novel.currentWebPageUrl?.let {
                        await { setBookmark(novel, dbHelper.getWebPage(it)?:return@await) }
                    }
                } catch (e: IOException) {
                    return@async
                }
            }
            if (predicate != null) predicate(novels.first(), true)
        }
        return true
    }

    override fun setBookmark(novel: Novel, chapter : WebPage) : Boolean {
        validateNovel(novel)

        return try {
            val chapterId = chapter.url.split("/").dropLastWhile { it.isEmpty() }.last()
            NovelApi.getString(READINGLIST_UPDATE_URL.format(novel.metaData["PostId"], chapterId, "yes"), ignoreHttpErrors = false)
            true;
        } catch (e : IOException) {
            false;
        }
    }

    override fun clearBookmark(novel: Novel, chapter : WebPage) : Boolean {
        validateNovel(novel)

        return try {
            NovelApi.getString(READINGLIST_UPDATE_URL.format(novel.metaData["PostId"], "0", "no"), ignoreHttpErrors = false)
            true;
        } catch (e : IOException) {
            false;
        }
    }

    override fun addSection(section: NovelSection): Boolean {
        return setCategories(fetchCategories() + CategoryInfo(section.name?:"NL Section ${section.id}"))
    }

    override fun removeSection(section: NovelSection): Boolean {
        val name = section.name?:"NL Section ${section.id}"
        return setCategories(fetchCategories().filter { it.name != name })
    }

    override fun renameSection(section: NovelSection, oldName : String?, newName : String?): Boolean {
        val categories = fetchCategories()
        val existing = categories.firstOrNull { it.name == oldName }
        return if (existing == null) {
//            setCategories(categories + CategoryInfo(section.name?:"NL Section ${section.id}"))
            false
        } else {
            existing.name = newName?:"NL Section ${section.id}"
            setCategories(categories)
        }
    }

    // getters

    override fun getCategories(): List<String> {
        return NovelApi.getDocument(CATEGORY_LIST_URL).body().select("#myTable_crl tbody tr input[name^=fname]").map {
            it.attr("value")
        }
    }

    // internal

    private fun getSectionIndex(section : NovelSection?) : Int {
        return if (section == null) {
            0
        } else {
            val categories = fetchCategories()
            val existingIndex = categories.firstOrNull { it.name == section.name }?.index
            if (existingIndex != null) existingIndex
            else {
                setCategories(categories + CategoryInfo(section.name?:"NL Section ${section.id}"))
                categories.count()
            }
        }
    }

    private fun setCategories(categories : List<CategoryInfo>) : Boolean {
        val toKeep = mutableListOf<Int>()
        val toDelete = mutableListOf<Int>()
        val data = HashMap<String, String>()

        val url = StringBuilder(CATEGORY_LIST_URL)

        categories.forEachIndexed { index, it ->
            it.index = index
            if (it.keep) toKeep.add(index)
            else toDelete.add(index)
            data["fname$index"] = it.name
            data["desc$index"] = it.description
            data["webmenu$index"] = it.icon
        }
        data["elements_st_unchecked"] = toKeep.joinToString(",")
        data["elements_st_checked"] = toDelete.joinToString(",")
        return try {
            NovelApi.getStringWithFormData(CATEGORY_LIST_URL, data, ignoreHttpErrors = false)
            true;
        } catch (e : IOException) {
            false;
        }
    }

    private fun fetchCategories() : List<CategoryInfo> {
        return NovelApi.getDocument(CATEGORY_LIST_URL).body().select("#myTable_crl tbody tr").map {
            CategoryInfo(
                it.select("input[name^=fname]").attr("value"),
                description = it.select("input[name^=desc]").attr("value"),
                icon = it.select("select.webmenu>option[selected]").attr("value"),
                index = it.select("input[name=check]").attr("value").toInt()
            )
        }
    }

    class CategoryInfo(var name : String, var description : String = "NovelLibrary section", var icon : String = "default_rl.png", var index : Int = 0) {

        var keep = true

        override fun toString() : String {
            return "fname$index=$name&description$index=$description&webmenu$index=$icon"
        }

    }
}