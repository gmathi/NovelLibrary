package io.github.gmathi.novellibrary.database.dao

import io.github.gmathi.novellibrary.model.database.Genre
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Genre operations using Coroutines and Flow
 */
interface GenreDao {
    
    /**
     * Create a genre in the database
     * @param genreName The name of the genre
     * @return The ID of the created genre
     */
    suspend fun createGenre(genreName: String): Long
    
    /**
     * Get a genre by name
     * @param genreName The name of the genre
     * @return The genre if found, null otherwise
     */
    suspend fun getGenre(genreName: String): Genre?
    
    /**
     * Get a genre by ID
     * @param genreId The ID of the genre
     * @return The genre if found, null otherwise
     */
    suspend fun getGenre(genreId: Long): Genre?
    
    /**
     * Get all genres for a novel
     * @param novelId The novel ID
     * @return List of genre names for the novel
     */
    suspend fun getGenres(novelId: Long): List<String>?
    
    /**
     * Get all genres as a Flow for reactive updates
     * @return Flow of list of all genres
     */
    fun getAllGenresFlow(): Flow<List<Genre>>
    
    /**
     * Get all genres (one-time query)
     * @return List of all genres
     */
    suspend fun getAllGenres(): List<Genre>
    
    /**
     * Update a genre
     * @param genre The genre to update
     * @return Number of rows affected
     */
    suspend fun updateGenre(genre: Genre): Long
    
    /**
     * Delete a genre
     * @param id The genre ID to delete
     */
    suspend fun deleteGenre(id: Long)
}