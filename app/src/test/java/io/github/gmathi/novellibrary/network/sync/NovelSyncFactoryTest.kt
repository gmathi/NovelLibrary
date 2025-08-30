package io.github.gmathi.novellibrary.network.sync

import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.HostNames
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NovelSyncFactoryTest {

    private lateinit var dbHelper: DBHelper
    private lateinit var dataCenter: DataCenter
    private lateinit var networkHelper: NetworkHelper
    private lateinit var sourceManager: SourceManager
    private lateinit var novelSyncFactory: NovelSyncFactory

    @Before
    fun setUp() {
        dbHelper = mockk(relaxed = true)
        dataCenter = mockk(relaxed = true)
        networkHelper = mockk(relaxed = true)
        sourceManager = mockk(relaxed = true)
        novelSyncFactory = NovelSyncFactory(dbHelper, dataCenter, networkHelper, sourceManager)
    }

    @Test
    fun `constructor injection works correctly`() {
        // Given
        val factory = NovelSyncFactory(dbHelper, dataCenter, networkHelper, sourceManager)
        
        // Then
        assertNotNull(factory)
    }

    @Test
    fun `getInstance with novel returns NovelUpdatesSync for novel updates URL`() {
        // Given
        every { dataCenter.getSyncEnabled(HostNames.NOVEL_UPDATES) } returns true
        val novel = mockk<Novel> {
            every { url } returns "https://${HostNames.NOVEL_UPDATES}/series/test-novel"
        }
        
        // When
        val sync = novelSyncFactory.getInstance(novel)
        
        // Then
        assertNotNull(sync)
        assertTrue(sync is NovelUpdatesSync)
    }

    @Test
    fun `getInstance with URL returns NovelUpdatesSync for novel updates URL`() {
        // Given
        every { dataCenter.getSyncEnabled(HostNames.NOVEL_UPDATES) } returns true
        val url = "https://${HostNames.NOVEL_UPDATES}/series/test-novel"
        
        // When
        val sync = novelSyncFactory.getInstance(url)
        
        // Then
        assertNotNull(sync)
        assertTrue(sync is NovelUpdatesSync)
    }

    @Test
    fun `getInstance returns null for unknown URL`() {
        // Given
        val url = "https://unknown-site.com/novel"
        
        // When
        val sync = novelSyncFactory.getInstance(url)
        
        // Then
        assertNull(sync)
    }

    @Test
    fun `getInstance returns null when sync is disabled`() {
        // Given
        every { dataCenter.getSyncEnabled(HostNames.NOVEL_UPDATES) } returns false
        val url = "https://${HostNames.NOVEL_UPDATES}/series/test-novel"
        
        // When
        val sync = novelSyncFactory.getInstance(url)
        
        // Then
        assertNull(sync)
    }

    @Test
    fun `getAllInstances returns list with NovelUpdatesSync when enabled`() {
        // Given
        every { dataCenter.getSyncEnabled(HostNames.NOVEL_UPDATES) } returns true
        
        // When
        val syncs = novelSyncFactory.getAllInstances()
        
        // Then
        assertEquals(1, syncs.size)
        assertTrue(syncs[0] is NovelUpdatesSync)
    }

    @Test
    fun `getAllInstances returns empty list when sync is disabled`() {
        // Given
        every { dataCenter.getSyncEnabled(HostNames.NOVEL_UPDATES) } returns false
        
        // When
        val syncs = novelSyncFactory.getAllInstances()
        
        // Then
        assertTrue(syncs.isEmpty())
    }
}