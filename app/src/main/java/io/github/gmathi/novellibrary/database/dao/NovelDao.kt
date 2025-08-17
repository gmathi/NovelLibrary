package io.github.gmathi.novellibrary.database.dao

import io.github.gmathi.novellibrary.model.database.Novel
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Novel operations using Coroutines and Flow
 */
interface NovelDao {
    
    /**
     * Insert a novel into the database
     * @param novel The novel to insert
     * @return The ID of the inserted novel
     */
    suspend fun insertNovel(novel: Novel): Long
    
    /**
     * Create a novel in the database
     * @param novel The novel to create
     * @return The ID of the created novel
     */
    suspend fun createNovel(novel: Novel): Long
    
    /**
     * Get a novel by its URL
     * @param novelUrl The URL of the novel
     * @return The novel if found, null otherwise
     */
    suspend fun getNovelByUrl(novelUrl: String): Novel?
    
    /**
     * Get a novel by its ID
     * @param novelId The ID of the novel
     * @return The novel if found, null otherwise
     */
    suspend fun getNovel(novelId: Long): Novel?
    
    /**
     * Get all novels as a Flow for reactive updates
     * @return Flow of list of all novels
     */
    fun getAllNovelsFlow(): Flow<List<Novel>>
    
    /**
     * Get all novels (one-time query)
     * @return List of all novels
     */
    suspend fun getAllNovels(): List<Novel>
    
    /**
     * Get all novels in a specific section as a Flow
     * @param novelSectionId The section ID
     * @return Flow of list of novels in the section
     */
    fun getAllNovelsFlow(novelSectionId: Long): Flow<List<Novel>>
    
    /**
     * Get all novels in a specific section (one-time query)
     * @param novelSectionId The section ID
     * @return List of novels in the section
     */
    suspend fun getAllNovels(novelSectionId: Long): List<Novel>
    
    /**
     * Get novel ID by URL
     * @param novelUrl The URL of the novel
     * @return The novel ID or -1 if not found
     */
    suspend fun getNovelId(novelUrl: String): Long
    
    /**
     * Update a novel
     * @param novel The novel to update
     * @return Number of rows affected
     */
    suspend fun updateNovel(novel: Novel): Long
    
    /**
     * Update novel order ID
     * @param novelId The novel ID
     * @param orderId The new order ID
     */
    suspend fun updateNovelOrderId(novelId: Long, orderId: Long)
    
    /**
     * Update novel section ID
     * @param novelId The novel ID
     * @param novelSectionId The new section ID
     */
    suspend fun updateNovelSectionId(novelId: Long, novelSectionId: Long)
    
    /**
     * Update bookmark current web page URL
     * @param novelId The novel ID
     * @param currentChapterUrl The current chapter URL
     */
    suspend fun updateBookmarkCurrentWebPageUrl(novelId: Long, currentChapterUrl: String?)
    
    /**
     * Update total chapter count
     * @param novelId The novel ID
     * @param totalChaptersCount The total chapters count
     */
    suspend fun updateTotalChapterCount(novelId: Long, totalChaptersCount: Long)
    
    /**
     * Update new releases count
     * @param novelId The novel ID
     * @param newReleasesCount The new releases count
     */
    suspend fun updateNewReleasesCount(novelId: Long, newReleasesCount: Long)
    
    /**
     * Update novel metadata
     * @param novel The novel with updated metadata
     */
    suspend fun updateNovelMetaData(novel: Novel)
    
    /**
     * Update chapters and releases count
     * @param novelId The novel ID
     * @param totalChaptersCount The total chapters count
     * @param newReleasesCount The new releases count
     */
    suspend fun updateChaptersAndReleasesCount(novelId: Long, totalChaptersCount: Long, newReleasesCount: Long)
    
    /**
     * Update chapters count
     * @param novelId The novel ID
     * @param chaptersCount The chapters count
     */
    suspend fun updateChaptersCount(novelId: Long, chaptersCount: Long)
    
    /**
     * Delete a novel
     * @param id The novel ID to delete
     */
    suspend fun deleteNovel(id: Long)
    
    /**
     * Reset a novel (delete and recreate with fresh data)
     * @param novel The novel to reset
     */
    suspend fun resetNovel(novel: Novel)
}