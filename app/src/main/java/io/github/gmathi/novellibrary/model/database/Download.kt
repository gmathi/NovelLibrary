package io.github.gmathi.novellibrary.model.database

import androidx.room.*
import io.github.gmathi.novellibrary.database.DBKeys

@Entity(tableName = DBKeys.TABLE_DOWNLOAD)
data class Download(@PrimaryKey
                    @ColumnInfo(name = DBKeys.KEY_WEB_PAGE_URL)
                    val webPageUrl: String,
                    @ColumnInfo(name = DBKeys.KEY_NAME)
                    var novelName: String,
                    @ColumnInfo(name = DBKeys.KEY_CHAPTER)
                    var chapter: String) {

    companion object {
        const val STATUS_IN_QUEUE = 0
        const val STATUS_PAUSED = 1
        const val STATUS_RUNNING = 2
    }

    /**
     * Either STATUS_IN_QUEUE, link STATUS_PAUSED or link STATUS_RUNNING
     */
    @ColumnInfo(name = DBKeys.KEY_STATUS)
    var status: Int = 0

    @ColumnInfo(name = DBKeys.KEY_METADATA, typeAffinity = ColumnInfo.TEXT, defaultValue = "{}")
    var metadata: HashMap<String, String> = HashMap<String, String>()

    @ColumnInfo(name = DBKeys.KEY_ORDER_ID)
    var orderId: Int = 0

    fun equals(other: Download): Boolean = (this.webPageUrl == other.webPageUrl)

}