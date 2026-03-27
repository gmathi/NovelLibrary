package io.github.gmathi.novellibrary.model.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "download")
data class Download(
    @PrimaryKey @ColumnInfo(name = "web_page_url") val webPageUrl: String,
    @ColumnInfo(name = "name") var novelName: String,
    @ColumnInfo(name = "novel_id") var novelId: Long,
    @ColumnInfo(name = "chapter") var chapter: String
) {

    companion object {
        const val STATUS_IN_QUEUE = 0
        const val STATUS_PAUSED = 1
        const val STATUS_RUNNING = 2
    }

    @ColumnInfo(name = "status")
    var status: Int = 0

    @ColumnInfo(name = "metadata")
    var metadata: HashMap<String, String>? = null

    @ColumnInfo(name = "order_id")
    var orderId: Int = 0

    fun equals(other: Download): Boolean = (this.webPageUrl == other.webPageUrl)

}