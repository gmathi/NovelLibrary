package io.github.gmathi.novellibrary.database.dao

import io.github.gmathi.novellibrary.model.database.WebPage
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for WebPage operations using Coroutines and Flow
 */
interface WebPageDao {
    
    /**
     * Create a web page in the database
     * @param webPage The web page to create
     * @return True if created successfully, false if already exists
     */
    suspend fun createWebPage(webPage: WebPage): Boolean
    
    /**
     * Get a web page by URL
     * @param url The URL of the web page
     * @return The web page if found, null otherwise
     */
    suspend fun getWebPage(url: String): WebPage?
    
    /**
     * Get all web pages for a novel as a Flow
     * @param novelId The novel ID
     * @return Flow of list of web pages
     */
    fun getAllWebPagesFlow(novelId: Long): Flow<List<WebPage>>
    
    /**
     * Get all web pages for a novel (one-time query)
     * @param novelId The novel ID
     * @return List of web pages
     */
    suspend fun getAllWebPages(novelId: Long): List<WebPage>
    
    /**
     * Get all web pages for a novel with specific translator source as a Flow
     * @param novelId The novel ID
     * @param translatorSourceName The translator source name
     * @return Flow of list of web pages
     */
    fun getAllWebPagesFlow(novelId: Long, translatorSourceName: String?): Flow<List<WebPage>>
    
    /**
     * Get all web pages for a novel with specific translator source (one-time query)
     * @param novelId The novel ID
     * @param translatorSourceName The translator source name
     * @return List of web pages
     */
    suspend fun getAllWebPages(novelId: Long, translatorSourceName: String?): List<WebPage>
    
    /**
     * Get a web page by novel ID and offset
     * @param novelId The novel ID
     * @param offset The offset
     * @return The web page if found, null otherwise
     */
    suspend fun getWebPage(novelId: Long, offset: Int): WebPage?
    
    /**
     * Get a web page by novel ID, translator source, and offset
     * @param novelId The novel ID
     * @param translatorSourceName The translator source name
     * @param offset The offset
     * @return The web page if found, null otherwise
     */
    suspend fun getWebPage(novelId: Long, translatorSourceName: String?, offset: Int): WebPage?
    
    /**
     * Delete all web pages for a novel
     * @param novelId The novel ID
     */
    suspend fun deleteWebPages(novelId: Long)
    
    /**
     * Delete a specific web page by URL
     * @param url The URL of the web page to delete
     */
    suspend fun deleteWebPage(url: String)
}