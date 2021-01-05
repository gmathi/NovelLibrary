package io.github.gmathi.novellibrary.model.database

import androidx.room.*
import io.github.gmathi.novellibrary.database.DBKeys
import java.io.Serializable

@Entity(tableName = DBKeys.TABLE_WEB_PAGE_SETTINGS,
    foreignKeys = [ForeignKey(entity = Novel::class,
            parentColumns = [DBKeys.KEY_ID],
            childColumns = [DBKeys.KEY_NOVEL_ID],
            onDelete = ForeignKey.CASCADE)],
    indices = [Index(name = DBKeys.INDEX_WEB_PAGE_SETTINGS,
            value = [DBKeys.KEY_URL, DBKeys.KEY_NOVEL_ID]),
        Index(value = [DBKeys.KEY_NOVEL_ID])])
data class WebPageSettings(@PrimaryKey
                           @ColumnInfo(name = DBKeys.KEY_URL)
                           var url: String,
                           @ColumnInfo(name = DBKeys.KEY_NOVEL_ID)
                           var novelId: Long) : Serializable {

    @ColumnInfo(name = DBKeys.KEY_TITLE)
    var title: String? = null
    @ColumnInfo(name = DBKeys.KEY_IS_READ)
    var isRead: Int = 0
    @ColumnInfo(name = DBKeys.KEY_FILE_PATH)
    var filePath: String? = null
    @ColumnInfo(name = DBKeys.KEY_REDIRECT_URL)
    var redirectedUrl: String? = null
    @ColumnInfo(name = DBKeys.KEY_METADATA, typeAffinity = ColumnInfo.TEXT, defaultValue = "{}")
    var metadata: HashMap<String, String?> = HashMap()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val webPage = other as WebPageSettings?
        return if (webPage != null) url == webPage.url else false
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + novelId.hashCode()
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + isRead
        result = 31 * result + (filePath?.hashCode() ?: 0)
        result = 31 * result + (redirectedUrl?.hashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }

    override fun toString(): String {
        return "WebPageSettings(url='$url', novelId=$novelId, title=$title, isRead=$isRead, filePath=$filePath, redirectedUrl=$redirectedUrl, metadata=$metadata)"
    }

    fun setIsRead(isRead: Int) {
        this.isRead = isRead
    }
}
