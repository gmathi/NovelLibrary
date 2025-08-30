package io.github.gmathi.novellibrary.network.sync

import io.github.gmathi.novellibrary.database.DBHelper
import io.github.gmathi.novellibrary.model.database.Novel
import io.github.gmathi.novellibrary.model.preference.DataCenter
import io.github.gmathi.novellibrary.model.source.SourceManager
import io.github.gmathi.novellibrary.network.NetworkHelper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory for creating NovelSync instances with Hilt dependency injection
 */
@Singleton
class NovelSyncFactory @Inject constructor(
    private val dbHelper: DBHelper,
    private val dataCenter: DataCenter,
    private val networkHelper: NetworkHelper,
    private val sourceManager: SourceManager
) {

    fun getInstance(novel: Novel, ignoreEnabled: Boolean = false): NovelSync? {
        return NovelSync.getInstance(novel, dbHelper, dataCenter, networkHelper, sourceManager, ignoreEnabled)
    }

    fun getInstance(url: String, ignoreEnabled: Boolean = false): NovelSync? {
        return NovelSync.getInstance(url, dbHelper, dataCenter, networkHelper, sourceManager, ignoreEnabled)
    }

    fun getAllInstances(ignoreEnabled: Boolean = false): List<NovelSync> {
        return NovelSync.getAllInstances(dbHelper, dataCenter, networkHelper, sourceManager, ignoreEnabled)
    }
}