package io.github.gmathi.novellibrary.util

import com.google.firebase.analytics.FirebaseAnalytics
import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.extension.ExtensionManager
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.database.WebPage
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.Runs

/**
 * Utility class for creating and configuring mocks for Hilt dependencies.
 * Provides common mock configurations to reduce test setup boilerplate.
 */
object HiltMockingUtils {

    /**
     * Creates a mock DBHelper with common test behaviors configured.
     */
    fun createMockDBHelper(): DBHelper {
        return mockk<DBHelper>(relaxed = true) {
            every { getNovel(any()) } returns createTestNovel()
            every { getAllWebPages(any()) } returns arrayListOf()
            every { getAllWebPageSettings(any()) } returns arrayListOf()
            every { insertNovel(any()) } returns TestConfiguration.TestData.TEST_NOVEL_ID
            every { updateNewReleasesCount(any(), any()) } just Runs
            every { updateNovelMetaData(any()) } just Runs
            every { createDownload(any(), any()) } just Runs
            every { updateWebPageSettingsReadStatus(any(), any(), any()) } just Runs
            every { writableDatabase } returns mockk(relaxed = true)
        }
    }

    /**
     * Creates a mock DataCenter with common test behaviors configured.
     */
    fun createMockDataCenter(): DataCenter {
        return mockk<DataCenter>(relaxed = true) {
            every { loadLibraryScreen } returns 0
            every { enableCloudFlare } returns false
            every { enableJavaScript } returns false
        }
    }

    /**
     * Creates a mock NetworkHelper with common test behaviors configured.
     */
    fun createMockNetworkHelper(): NetworkHelper {
        return mockk<NetworkHelper>(relaxed = true) {
            every { isConnectedToNetwork() } returns true
            every { getDocumentWithUserAgent(any()) } returns mockk(relaxed = true)
        }
    }

    /**
     * Creates a mock SourceManager with common test behaviors configured.
     */
    fun createMockSourceManager(): SourceManager {
        return mockk<SourceManager>(relaxed = true) {
            every { getSource(any()) } returns mockk(relaxed = true)
            every { isSourceEnabled(any()) } returns true
        }
    }

    /**
     * Creates a mock ExtensionManager with common test behaviors configured.
     */
    fun createMockExtensionManager(): ExtensionManager {
        return mockk<ExtensionManager>(relaxed = true) {
            every { init(any()) } just Runs
            every { getExtensions() } returns arrayListOf()
        }
    }

    /**
     * Creates a mock FirebaseAnalytics with common test behaviors configured.
     */
    fun createMockFirebaseAnalytics(): FirebaseAnalytics {
        return mockk<FirebaseAnalytics>(relaxed = true) {
            every { logEvent(any(), any()) } just Runs
            every { setUserProperty(any(), any()) } just Runs
        }
    }

    /**
     * Creates a test Novel instance with default values.
     */
    fun createTestNovel(): Novel {
        return Novel(
            id = TestConfiguration.TestData.TEST_NOVEL_ID,
            name = TestConfiguration.TestData.TEST_NOVEL_NAME,
            url = TestConfiguration.TestData.TEST_URL,
            sourceId = 1L
        )
    }

    /**
     * Creates a test WebPage instance with default values.
     */
    fun createTestWebPage(): WebPage {
        return WebPage(
            url = TestConfiguration.TestData.TEST_URL,
            chapterName = TestConfiguration.TestData.TEST_CHAPTER_NAME,
            novelId = TestConfiguration.TestData.TEST_NOVEL_ID
        )
    }

    /**
     * Creates a list of test WebPages for testing bulk operations.
     */
    fun createTestWebPages(count: Int = 3): ArrayList<WebPage> {
        return arrayListOf<WebPage>().apply {
            repeat(count) { index ->
                add(
                    WebPage(
                        url = "${TestConfiguration.TestData.TEST_URL}/chapter-${index + 1}",
                        chapterName = "Chapter ${index + 1}",
                        novelId = TestConfiguration.TestData.TEST_NOVEL_ID
                    )
                )
            }
        }
    }

    /**
     * Configures a mock DBHelper for offline scenarios.
     */
    fun configureMockDBHelperForOffline(mockDBHelper: DBHelper) {
        every { mockDBHelper.getNovel(any()) } returns createTestNovel()
        every { mockDBHelper.getAllWebPages(any()) } returns createTestWebPages()
        every { mockDBHelper.getAllWebPageSettings(any()) } returns arrayListOf()
    }

    /**
     * Configures a mock NetworkHelper for offline scenarios.
     */
    fun configureMockNetworkHelperForOffline(mockNetworkHelper: NetworkHelper) {
        every { mockNetworkHelper.isConnectedToNetwork() } returns false
    }

    /**
     * Configures mocks for a successful novel loading scenario.
     */
    fun configureForSuccessfulNovelLoading(
        mockDBHelper: DBHelper,
        mockNetworkHelper: NetworkHelper
    ) {
        every { mockNetworkHelper.isConnectedToNetwork() } returns true
        every { mockDBHelper.getNovel(any()) } returns createTestNovel()
        every { mockDBHelper.getAllWebPages(any()) } returns createTestWebPages()
        every { mockDBHelper.getAllWebPageSettings(any()) } returns arrayListOf()
        every { mockDBHelper.updateNewReleasesCount(any(), any()) } just Runs
    }
}