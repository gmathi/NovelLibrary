package io.github.gmathi.novellibrary.database

import android.content.Context
import androidx.room.*
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gmathi.novellibrary.database.dao.NovelDao
import io.github.gmathi.novellibrary.database.dao.WebPageDao
import io.github.gmathi.novellibrary.model.database.WebPage
import org.hamcrest.CoreMatchers.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import kotlin.jvm.Throws

@RunWith(AndroidJUnit4::class)
class WebPageTest {
    companion object {
        fun createWebPage(novelId: Long): WebPage {
            return WebPage("https://www.example.org/chapter/0", "0", novelId)
        }
    }
    private lateinit var novelDao: NovelDao
    private lateinit var webPageDao: WebPageDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        db.insertDefaults()
        novelDao = db.novelDao()
        webPageDao = db.webPageDao()

        io.github.gmathi.novellibrary.db = db
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }
    
    @Test
    fun storeAndReadWebPage() {
        val novel = NovelTest.createNovel()
        val novelId = novelDao.insertNovel(novel)

        val webPage = createWebPage(novelId)
        webPageDao.insert(webPage)

        val byId = webPageDao.findOneByUrl(webPage.url)
        assertNotNull(byId)
        byId ?: return

        assertThat(byId.url, equalTo(webPage.url))
        assertThat(byId.chapter, equalTo(webPage.chapter))
        assertThat(byId.novelId, equalTo(novelId))
    }
}