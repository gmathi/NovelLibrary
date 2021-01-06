package io.github.gmathi.novellibrary.database

import android.content.Context
import androidx.room.*
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.gmathi.novellibrary.NovelLibraryApplication
import io.github.gmathi.novellibrary.database.dao.GenreDao
import io.github.gmathi.novellibrary.database.dao.NovelDao
import io.github.gmathi.novellibrary.database.dao.NovelGenreDao
import io.github.gmathi.novellibrary.model.database.Novel
import org.hamcrest.CoreMatchers.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import kotlin.jvm.Throws

@RunWith(AndroidJUnit4::class)
class NovelTest {
    companion object {
        fun createNovel(): Novel {
            val novel = Novel("https://www.example.com")
            novel.apply {
                name = "Novel Example"
                genres = listOf("Test", "Example", "Action")
            }

            return novel
        }
    }

    private lateinit var novelDao: NovelDao
    private lateinit var novelGenreDao: NovelGenreDao
    private lateinit var genreDao: GenreDao
    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        db.insertDefaults()
        novelDao = db.novelDao()
        novelGenreDao = db.novelGenreDao()
        genreDao = db.genreDao()

        io.github.gmathi.novellibrary.db = db
    }
    
    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun storeAndReadNovel() {
        val novel = createNovel()

        val id = novelDao.insertNovel(novel)
        val byId = novelDao.findOneById(id)
        assertNotNull(byId)
        assertEquals(byId?.url, "https://www.example.com")
        assertEquals(byId?.name, "Novel Example")

        assertTrue(novelGenreDao.getAll().isNotEmpty())
        assertTrue(genreDao.getAll().isNotEmpty())
        
        val novelGenreByNovel = novelGenreDao.findByNovelId(byId!!.id)
        assertTrue(novelGenreByNovel.isNotEmpty())
        val genreById = genreDao.findOneById(novelGenreByNovel[0].genreId)
        assertThat(genreById?.name, equalTo("Test"))
    }
    
    @Test
    fun storeAndDeleteNovel() {
        val id = novelDao.insertNovel(createNovel())
        novelDao.delete(novelDao.findOneById(id)!!)
        assertTrue(novelDao.getAll().isEmpty())
    }
}