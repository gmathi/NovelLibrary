package io.github.gmathi.novellibrary.database.dao

import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.database.dao.impl.GenreDaoImpl
import io.github.gmathi.novellibrary.model.database.Genre
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class GenreDaoTest {

    private lateinit var dbHelper: DBHelper
    private lateinit var genreDao: GenreDao

    @Before
    fun setUp() {
        dbHelper = mockk(relaxed = true)
        genreDao = GenreDaoImpl(dbHelper)
    }

    @After
    fun tearDown() {
        clearAllMocks()
    }

    @Test
    fun `createGenre should return existing genre id when genre exists`() = runTest {
        // Given
        val genreName = "Fantasy"
        val existingGenre = Genre().apply {
            id = 123L
            name = genreName
        }

        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        every { dbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns true
        every { mockCursor.getColumnIndex(any()) } returns 0
        every { mockCursor.getLong(0) } returns existingGenre.id
        every { mockCursor.getString(0) } returns existingGenre.name

        // When
        val result = genreDao.createGenre(genreName)

        // Then
        assertEquals(existingGenre.id, result)
        verify(exactly = 0) { dbHelper.writableDatabase.insert(any(), any(), any()) }
    }

    @Test
    fun `createGenre should create new genre when not exists`() = runTest {
        // Given
        val genreName = "Adventure"
        val newGenreId = 456L

        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        every { dbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns false
        every { dbHelper.writableDatabase.insert(any(), any(), any()) } returns newGenreId

        // When
        val result = genreDao.createGenre(genreName)

        // Then
        assertEquals(newGenreId, result)
        verify { dbHelper.writableDatabase.insert(any(), any(), any()) }
    }

    @Test
    fun `getGenre by name should return genre when found`() = runTest {
        // Given
        val genreName = "Fantasy"
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        
        every { dbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns true
        every { mockCursor.getColumnIndex(any()) } returns 0
        every { mockCursor.getLong(0) } returns 123L
        every { mockCursor.getString(0) } returns genreName

        // When
        val result = genreDao.getGenre(genreName)

        // Then
        assertNotNull(result)
        assertEquals(123L, result?.id)
        assertEquals(genreName, result?.name)
    }

    @Test
    fun `getGenre by name should return null when not found`() = runTest {
        // Given
        val genreName = "NonExistent"
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        
        every { dbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns false

        // When
        val result = genreDao.getGenre(genreName)

        // Then
        assertNull(result)
    }

    @Test
    fun `getGenre by id should return genre when found`() = runTest {
        // Given
        val genreId = 123L
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        
        every { dbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns true
        every { mockCursor.getColumnIndex(any()) } returns 0
        every { mockCursor.getLong(0) } returns genreId
        every { mockCursor.getString(0) } returns "Fantasy"

        // When
        val result = genreDao.getGenre(genreId)

        // Then
        assertNotNull(result)
        assertEquals(genreId, result?.id)
        assertEquals("Fantasy", result?.name)
    }

    @Test
    fun `getGenres should return list of genre names for novel`() = runTest {
        // Given
        val novelId = 123L
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        
        every { dbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns true
        every { mockCursor.getColumnIndex("Genres") } returns 0
        every { mockCursor.getString(0) } returns "Fantasy,Adventure,Romance"

        // When
        val result = genreDao.getGenres(novelId)

        // Then
        assertNotNull(result)
        assertEquals(3, result?.size)
        assertTrue(result?.contains("Fantasy") == true)
        assertTrue(result?.contains("Adventure") == true)
        assertTrue(result?.contains("Romance") == true)
    }

    @Test
    fun `getGenres should return null when no genres found`() = runTest {
        // Given
        val novelId = 123L
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        
        every { dbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns false

        // When
        val result = genreDao.getGenres(novelId)

        // Then
        assertNull(result)
    }

    @Test
    fun `getAllGenres should return list of all genres`() = runTest {
        // Given
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        
        every { dbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns true
        every { mockCursor.moveToNext() } returns true andThen false
        every { mockCursor.getColumnIndex(any()) } returns 0
        every { mockCursor.getLong(0) } returns 1L andThen 2L
        every { mockCursor.getString(0) } returns "Fantasy" andThen "Adventure"

        // When
        val result = genreDao.getAllGenres()

        // Then
        assertEquals(2, result.size)
        assertEquals("Fantasy", result[0].name)
        assertEquals("Adventure", result[1].name)
    }

    @Test
    fun `getAllGenresFlow should emit list of genres`() = runTest {
        // Given
        val mockCursor = mockk<android.database.Cursor>(relaxed = true)
        
        every { dbHelper.readableDatabase.rawQuery(any(), any()) } returns mockCursor
        every { mockCursor.moveToFirst() } returns true
        every { mockCursor.moveToNext() } returns false
        every { mockCursor.getColumnIndex(any()) } returns 0
        every { mockCursor.getLong(0) } returns 1L
        every { mockCursor.getString(0) } returns "Fantasy"

        // When
        val result = genreDao.getAllGenresFlow().first()

        // Then
        assertEquals(1, result.size)
        assertEquals("Fantasy", result[0].name)
    }

    @Test
    fun `updateGenre should update genre data`() = runTest {
        // Given
        val genre = Genre().apply {
            id = 123L
            name = "Updated Fantasy"
        }

        every { dbHelper.writableDatabase.update(any(), any(), any(), any()) } returns 1

        // When
        val result = genreDao.updateGenre(genre)

        // Then
        assertEquals(1L, result)
        verify { dbHelper.writableDatabase.update(any(), any(), any(), arrayOf(genre.id.toString())) }
    }

    @Test
    fun `deleteGenre should delete genre by id`() = runTest {
        // Given
        val genreId = 123L

        every { dbHelper.writableDatabase.delete(any(), any(), any()) } returns 1

        // When
        genreDao.deleteGenre(genreId)

        // Then
        verify { dbHelper.writableDatabase.delete(any(), any(), arrayOf(genreId.toString())) }
    }
}