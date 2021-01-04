package io.github.gmathi.novellibrary.model.database

import androidx.room.*
import io.github.gmathi.novellibrary.database.DBKeys
import java.io.Serializable

@Entity(tableName = DBKeys.TABLE_WEB_PAGE,
    foreignKeys = [ForeignKey(entity = Novel::class,
            parentColumns = [DBKeys.KEY_ID],
            childColumns = [DBKeys.KEY_NOVEL_ID],
            onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TranslatorSource::class,
            parentColumns = [DBKeys.KEY_ID],
            childColumns = [DBKeys.KEY_SOURCE_ID],
            onDelete = ForeignKey.SET_DEFAULT)],
    indices = [Index(name = DBKeys.TABLE_WEB_PAGE,
            value = [DBKeys.KEY_URL, DBKeys.KEY_NOVEL_ID],
            unique = true),
        Index(value = [DBKeys.KEY_NOVEL_ID]),
        Index(value = [DBKeys.KEY_SOURCE_ID])])
data class WebPage(@PrimaryKey
                   @ColumnInfo(name = DBKeys.KEY_URL)
                   var url: String,
                   @ColumnInfo(name = DBKeys.KEY_CHAPTER)
                   var chapter: String,
                   @ColumnInfo(name = DBKeys.KEY_NOVEL_ID)
                   var novelId: Long = -1L,
                   @ColumnInfo(name = DBKeys.KEY_ORDER_ID)
                   var orderId: Long = -1L,
                   @ColumnInfo(name = DBKeys.KEY_SOURCE_ID, defaultValue = "-1")
                   var translatorSourceId: Long = -1L) : Serializable {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val webPage = other as WebPage?
        return if (webPage != null) url == webPage.url else false
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + chapter.hashCode()
        result = 31 * result + novelId.hashCode()
        result = 31 * result + translatorSourceId.hashCode()
        result = 31 * result + orderId.hashCode()
        return result
    }

    override fun toString(): String {
        return "WebPage(url='$url', chapter='$chapter', novelId=$novelId, sourceId=$translatorSourceId, orderId=$orderId)"
    }

}
