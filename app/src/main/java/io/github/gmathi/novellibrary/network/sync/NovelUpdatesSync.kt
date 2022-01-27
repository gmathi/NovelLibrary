package io.github.gmathi.novellibrary.network.sync

import io.github.gmathi.novellibrary.database.getWebPage
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.NovelSection
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.network.POST
import io.github.gmathi.novellibrary.network.WebPageDocumentFetcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import java.net.URL

class NovelUpdatesSync : NovelSync() {

    companion object {
        private const val BASE_URL = "https://${HostNames.NOVEL_UPDATES}"
        const val READING_LIST_UPDATE_URL = "$BASE_URL/readinglist_update.php?sid=%s&rid=%s&checked=%s"
        const val UPDATE_NOVEL_URL = "$BASE_URL/updatelist.php?sid=%s&lid=%d&act=%s"
        const val CATEGORY_LIST_URL = "$BASE_URL/create-reading-lists/"
        const val LOGIN_URL = "$BASE_URL/login/"
        const val COOKIE_REGEX = "wordpress_(logged_in|sec|user_sw)_"

        @Suppress("NOTHING_TO_INLINE")
        inline fun validateNovel(novel: Novel) {
            if (!novel.metadata.containsKey("PostId")) throw Exception("No PostId Found!")
        }
    }

    override val host: String = HostNames.NOVEL_UPDATES

    override fun loggedIn(): Boolean {
        val testReg = COOKIE_REGEX.toRegex()
        return networkHelper.cookieManager.getCookieMap(URL(BASE_URL)).any {
            testReg.containsMatchIn(it.key)
        }
    }

    override fun getLoginURL(): String {
        return LOGIN_URL
    }

    override fun getCookieLookupRegex(): String {
        return COOKIE_REGEX
    }

    override fun addNovel(novel: Novel, section: NovelSection?): Boolean {
        validateNovel(novel)

        val category = getSectionIndex(section)
        return try {
            WebPageDocumentFetcher.document(UPDATE_NOVEL_URL.format(novel.metadata["PostId"], category, "move"))
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun removeNovel(novel: Novel): Boolean {
        validateNovel(novel)

        return try {
            WebPageDocumentFetcher.document(READING_LIST_UPDATE_URL.format(novel.metadata["PostId"], "0", "noo"))
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun updateNovel(novel: Novel, section: NovelSection?): Boolean {
        return addNovel(novel, section)
    }

    override fun batchAdd(novels: List<Novel>, sections: List<NovelSection>, progress: ((String) -> Unit)?): Boolean {

        try {
            runBlocking {

                val tempCategoriesList = withContext(Dispatchers.IO) { fetchCategories() }
                if (tempCategoriesList.isEmpty()) return@runBlocking
                val categories = ArrayList(tempCategoriesList)
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
                    if (!withContext(Dispatchers.IO) { setCategories(categories) }) return@runBlocking
                }

                novels.forEach { novel ->
                    try {
                        progress?.let { it(novel.name) }
                        withContext(Dispatchers.IO) { WebPageDocumentFetcher.document(UPDATE_NOVEL_URL.format(novel.metadata["PostId"], categoryMap[novel.novelSectionId] ?: 0, "move")) }
                        novel.currentChapterUrl?.let {
                            withContext(Dispatchers.IO) { setBookmark(novel, dbHelper.getWebPage(it) ?: return@withContext) }
                        }
                    } catch (e: Exception) {
                        return@runBlocking
                    }
                }
            }
            return true
        } catch (e: Exception) {
            return false
        }

    }

    override fun setBookmark(novel: Novel, chapter: WebPage): Boolean {
        validateNovel(novel)
        return try {
            val chapterId = chapter.url.split("/").dropLastWhile { it.isEmpty() }.last()
            WebPageDocumentFetcher.document(READING_LIST_UPDATE_URL.format(novel.metadata["PostId"], chapterId, "yes"))
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun clearBookmark(novel: Novel, chapter: WebPage): Boolean {
        validateNovel(novel)

        return try {
            WebPageDocumentFetcher.document(READING_LIST_UPDATE_URL.format(novel.metadata["PostId"], "0", "no"))
            true
        } catch (e: Exception) {
            false
        }
    }

    override fun addSection(section: NovelSection): Boolean {
        return setCategories(fetchCategories() + CategoryInfo(section.name ?: "NL Section ${section.id}"))
    }

    override fun removeSection(section: NovelSection): Boolean {
        val name = section.name ?: "NL Section ${section.id}"
        return setCategories(fetchCategories().filter { it.name != name })
    }

    override fun renameSection(section: NovelSection, oldName: String?, newName: String?): Boolean {
        val categories = fetchCategories() ?: return false
        val existing = categories.firstOrNull { it.name == oldName }
        return if (existing == null) {
//            setCategories(categories + CategoryInfo(section.name?:"NL Section ${section.id}"))
            false
        } else {
            existing.name = newName ?: "NL Section ${section.id}"
            setCategories(categories)
        }
    }

    // getters

    override fun getCategories(): List<String> {
        return try {
            WebPageDocumentFetcher.document(CATEGORY_LIST_URL).body().select("#myTable_crl tbody tr input[name^=fname]").map {
                it.attr("value")
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // internal

    private fun getSectionIndex(section: NovelSection?): Int {
        return if (section == null) {
            0
        } else {
            val categories = fetchCategories() ?: return 0
            val existingIndex = categories.firstOrNull { it.name == section.name }?.index
            if (existingIndex != null) existingIndex
            else {
                setCategories(categories + CategoryInfo(section.name ?: "NL Section ${section.id}"))
                categories.count()
            }
        }
    }

    private fun setCategories(categories: List<CategoryInfo>): Boolean {
        val toKeep = mutableListOf<Int>()
        val toDelete = mutableListOf<Int>()
        val data = HashMap<String, String>()

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
            val formBodyBuilder = FormBody.Builder()
            data.forEach { formBodyBuilder.add(it.key, it.value) }
            val request = POST(CATEGORY_LIST_URL, body = formBodyBuilder.build())
            WebPageDocumentFetcher.connect(request)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun fetchCategories(): List<CategoryInfo> {
        return try {
            WebPageDocumentFetcher.document(CATEGORY_LIST_URL).body().select("#myTable_crl tbody tr").map {
                CategoryInfo(
                    it.select("input[name^=fname]").attr("value"),
                    description = it.select("input[name^=desc]").attr("value"),
                    icon = it.select("select.webmenu>option[selected]").attr("value"),
                    index = it.select("input[name=check]").attr("value").toInt()
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    class CategoryInfo(var name: String, var description: String = "NovelLibrary section", var icon: String = "default_rl.png", var index: Int = 0) {
        var keep = true
        override fun toString(): String {
            return "fname$index=$name&description$index=$description&webmenu$index=$icon"
        }
    }
}