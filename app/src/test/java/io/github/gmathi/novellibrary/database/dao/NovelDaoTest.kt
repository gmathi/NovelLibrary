package io.github.gmathi.novellibrary.database.dao

import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.dao.impl.NovelDaoImpl
import io.github.gmathi.novellibrary.model.database.Novel
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class NovelDaoTest {

    private lateinit var dbHelper: DBHelper
    private lateinit var novelDao: NovelDao

    @Before
    fun setUp() {
        dbHelper = mockk(relaxed = true)
        novelDao = NovelDaoImpl(dbHelper)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `insertNovel should create novel and return id`() = runTest {
        // Given
        val novel = Novel("Test Novel", "https://test.com/novel", 1L)
        novel.genres = listOf("Fantasy", "Adventure")
        val expectedId = 123L

        every { dbHelper.createGenre(any()) } returns 1L
        every { dbHelper.createNovelGenre(any()) } returns 1L
        coEvery { novelDao.createNovel(novel) } returns expectedId

        // When
        val result = novelDao.insertNovel(novel)

        // Then
        assertEquals(expectedId, result)
        verify { dbHelper.createGenre("Fantasy") }
        verify { dbHelper.createGenre("Adventure") }
    }

    @Test
    fun `getNovelByUrl should return novel when found`() = runTest {
        // Given
        val novelUrl = "https://test.com/novel"
        val expectedNovel = Novel("Test Novel", novelUrl, 1L)
        expectedNovel.id = 123L

        // Mock the database query behavior
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        every { dbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns true
        every { mockCursor.getColumnIndex(any()) } returns 0
        every { mockCursor.getString(0) } returns "Test Novel" andThen novelUrl
        every { mockCursor.getLong(0) } returns 123L andThen 1L
        every { dbHelper.getGenres(123L) } returns listOf("Fantasy")

        // When
        val result = novelDao.getNovelByUrl(novelUrl)

        // Then
        assertNotNull(result)
        assertEquals(expectedNovel.name, result?.name)
        assertEquals(expectedNovel.url, result?.url)
        assertEquals(expectedNovel.id, result?.id)
    }

    @Test
    fun `getNovelByUrl should return null when not found`() = runTest {
        // Given
        val novelUrl = "https://test.com/nonexistent"
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        every { dbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns false

        // When
        val result = novelDao.getNovelByUrl(novelUrl)

        // Then
        assertNull(result)
    }

    @Test
    fun `getAllNovels should return list of novels`() = runTest {
        // Given
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        every { dbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns true
        every { mockCursor.moveToNext() } returns true andThen false
        every { mockCursor.getColumnIndex(any()) } returns 0
        every { mockCursor.getString(0) } returns "Novel 1" andThen "https://test1.com" andThen "Novel 2" andThen "https://test2.com"
        every { mockCursor.getLong(0) } returns 1L andThen 1L andThen 2L andThen 1L

        // When
        val result = novelDao.getAllNovels()

        // Then
        assertEquals(2, result.size)
        assertEquals("Novel 1", result[0].name)
        assertEquals("Novel 2", result[1].name)
    }

    @Test
    fun `getAllNovelsFlow should emit list of novels`() = runTest {
        // Given
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        every { dbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns true
        every { mockCursor.moveToNext() } returns false
        every { mockCursor.getColumnIndex(any()) } returns 0
        every { mockCursor.getString(0) } returns "Test Novel" andThen "https://test.com"
        every { mockCursor.getLong(0) } returns 1L andThen 1L

        // When
        val result = novelDao.getAllNovelsFlow().first()

        // Then
        assertEquals(1, result.size)
        assertEquals("Test Novel", result[0].name)
    }

    @Test
    fun `updateNovel should update novel data`() = runTest {
        // Given
        val novel = Novel("Updated Novel", "https://test.com/novel", 1L)
        novel.id = 123L
        novel.rating = "4.5"
        novel.shortDescription = "Updated description"

        every { dbHelper.writableDatabase.update(any(), any(), any(), any()) } returns 1

        // When
        val result = novelDao.updateNovel(novel)

        // Then
        assertEquals(1L, result)
        verify { dbHelper.writableDatabase.update(any(), any(), any(), any()) }
    }

    @Test
    fun `deleteNovel should delete novel by id`() = runTest {
        // Given
        val novelId = 123L

        every { dbHelper.writableDatabase.delete(any(), any(), any()) } returns 1

        // When
        novelDao.deleteNovel(novelId)

        // Then
        verify { dbHelper.writableDatabase.delete(any(), any(), arrayOf(novelId.toString())) }
    }

    @Test
    fun `updateNovelOrderId should update order id`() = runTest {
        // Given
        val novelId = 123L
        val orderId = 456L

        every { dbHelper.writableDatabase.update(any(), any(), any(), any()) } returns 1

        // When
        novelDao.updateNovelOrderId(novelId, orderId)

        // Then
        verify { dbHelper.writableDatabase.update(any(), any(), any(), arrayOf(novelId.toString())) }
    }

    @Test
    fun `updateNewReleasesCount should update releases count`() = runTest {
        // Given
        val novelId = 123L
        val newReleasesCount = 5L

        every { dbHelper.writableDatabase.update(any(), any(), any(), any()) } returns 1

        // When
        novelDao.updateNewReleasesCount(novelId, newReleasesCount)

        // Then
        verify { dbHelper.writableDatabase.update(any(), any(), any(), arrayOf(novelId.toString())) }
    }

    @Test
    fun `getNovelId should return novel id when found`() = runTest {
        // Given
        val novelUrl = "https://test.com/novel"
        val expectedId = 123L
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        
        every { dbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns true
        every { mockCursor.getColumnIndex(any()) } returns 0
        every { mockCursor.getLong(0) } returns expectedId

        // When
        val result = novelDao.getNovelId(novelUrl)

        // Then
        assertEquals(expectedId, result)
    }

    @Test
    fun `getNovelId should return -1 when not found`() = runTest {
        // Given
        val novelUrl = "https://test.com/nonexistent"
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        
        every { dbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns false

        // When
        val result = novelDao.getNovelId(novelUrl)

        // Then
        assertEquals(-1L, result)
    }
}