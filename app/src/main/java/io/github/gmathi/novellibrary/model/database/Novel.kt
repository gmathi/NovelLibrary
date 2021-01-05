@file:Suppress("UNCHECKED_CAST")

package io.github.gmathi.novellibrary.model.database

import android.os.Parcel
import android.os.Parcelable
import androidx.room.*
import com.google.gson.annotations.SerializedName
import io.github.gmathi.novellibrary.database.DBKeys
import io.github.gmathi.novellibrary.model.other.NovelSectionEvent
import java.io.Serializable

@Entity(tableName = DBKeys.TABLE_NOVEL,
    foreignKeys = [ForeignKey(entity = NovelSection::class,
            parentColumns = [DBKeys.KEY_ID],
            childColumns = [DBKeys.KEY_NOVEL_SECTION_ID],
            onDelete = ForeignKey.SET_DEFAULT)],
    indices = [Index(value = [DBKeys.KEY_CURRENT_WEB_PAGE_URL]),
               Index(value = [DBKeys.KEY_NOVEL_SECTION_ID])])
data class Novel(@ColumnInfo(name = DBKeys.KEY_URL)
                 var url: String) : Parcelable, Serializable {

    constructor(name: String?, url: String): this(url) {
        name?.let { this.name = name }
    }

    /**
     * Name of the novel
     */
    @ColumnInfo(name = DBKeys.KEY_NAME)
    var name: String = "Unknown - Not Found!"

    /**
     * Internal Id from the source
     */
    @Ignore
    var externalNovelId: String? = null

    /**
     * Novel's cover art image url
     */
    @ColumnInfo(name = DBKeys.KEY_IMAGE_URL)
    var imageUrl: String? = null

    /**
     * Rating of the novel in the decimal format and ranging between (min) 0.0 - 5.0 (max)
     */
    @ColumnInfo(name = DBKeys.KEY_RATING)
    var rating: String? = null

    /**
     * Short description of the novel that is shown at a glance
     */
    @ColumnInfo(name = DBKeys.KEY_SHORT_DESCRIPTION)
    var shortDescription: String? = null

    /**
     * Complete description of the novel
     */
    @ColumnInfo(name = DBKeys.KEY_LONG_DESCRIPTION)
    var longDescription: String? = null

    /**
     * List of genres this novel belongs to
     */
    @Ignore
    var genres: List<String>? = null

    /**
     * Author(s) of this novel
     */
    @Ignore
    var authors: List<String>? = null

    /**
     * Illustrator(s) of this novel
     */
    @Ignore
    var illustrator: List<String>? = null

    /**
     * Number of released chapters in this novel
     */
    @ColumnInfo(name = DBKeys.KEY_CHAPTERS_COUNT)
    var chaptersCount: Long = 0L

    /**
     * More metadata of the novel
     */
    @ColumnInfo(name = DBKeys.KEY_METADATA, typeAffinity = ColumnInfo.TEXT, defaultValue = "{}")
    @SerializedName("metaData") //This is support restoration from older backups.
    var metadata: HashMap<String, String?> = HashMap()

    //For Database and internal app tracking
    /**
     * Local downloaded novel image file path
     */
    @ColumnInfo(name = DBKeys.KEY_IMAGE_FILE_PATH)
    var imageFilePath: String? = null

    /**
     * Display order of the novel in the Library Screen
     */
    @ColumnInfo(name = DBKeys.KEY_ORDER_ID)
    var orderId: Long = -1L

    /**
     * The red bubble that shows the number of new releases since the novel was last opened.
     */
    @ColumnInfo(name = DBKeys.KEY_NEW_RELEASES_COUNT)
    var newReleasesCount = 0L

    /**
     * Novel Section Id for the novel section this belongs to.
     */
    @ColumnInfo(name = DBKeys.KEY_NOVEL_SECTION_ID, defaultValue = "-1")
    var novelSectionId: Long = -1L

    /**
     * Database id for the novel. Default = -1L which means it is not in database.
     * CAUTION: Set to 0 (not set) before inserting into database!!
     */
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = DBKeys.KEY_ID)
    var id: Long = -1L

    /**
     * The bookmark for the novel. This tracks the last chapter the user was reading.
     */
    @ColumnInfo(name =DBKeys.KEY_CURRENT_WEB_PAGE_URL)
    @SerializedName("currentlyReading")
    var currentChapterUrl: String? = null


    // Other Helper functions
    fun copyFrom(otherNovel: Novel?) {
        if (otherNovel != null) {
            id = if (otherNovel.id != -1L) otherNovel.id else id
            url = otherNovel.url
            name = otherNovel.name
            genres = if (otherNovel.genres != null) otherNovel.genres else genres
            rating = if (otherNovel.rating != null) otherNovel.rating else rating
            imageUrl = if (otherNovel.imageUrl != null) otherNovel.imageUrl else imageUrl
            imageFilePath = if (otherNovel.imageFilePath != null) otherNovel.imageFilePath else imageFilePath
            longDescription = if (otherNovel.longDescription != null) otherNovel.longDescription else longDescription
            shortDescription = if (otherNovel.shortDescription != null) otherNovel.shortDescription else shortDescription
            currentChapterUrl = if (otherNovel.currentChapterUrl != null) otherNovel.currentChapterUrl else currentChapterUrl
            newReleasesCount = if (otherNovel.newReleasesCount != 0L) otherNovel.newReleasesCount else newReleasesCount
            chaptersCount = if (otherNovel.chaptersCount != 0L) otherNovel.chaptersCount else chaptersCount
            orderId = if (otherNovel.orderId != -1L) otherNovel.orderId else orderId
            novelSectionId = if (otherNovel.novelSectionId != -1L) otherNovel.novelSectionId else novelSectionId

            otherNovel.metadata.keys.forEach { key ->
                metadata[key] = otherNovel.metadata[key]
            }
        }
    }

    //Parcelable Implementation
    constructor(parcel: Parcel) : this(parcel.readString().toString()) {
        name = parcel.readString().toString()
        externalNovelId = parcel.readString()
        imageUrl = parcel.readString()
        rating = parcel.readString()
        shortDescription = parcel.readString()
        longDescription = parcel.readString()
        genres = parcel.createStringArrayList()
        authors = parcel.createStringArrayList()
        illustrator = parcel.createStringArrayList()
        chaptersCount = parcel.readLong()
        imageFilePath = parcel.readString()
        orderId = parcel.readLong()
        newReleasesCount = parcel.readLong()
        novelSectionId = parcel.readLong()
        id = parcel.readLong()
        currentChapterUrl = parcel.readString()
        metadata = parcel.readHashMap(HashMap::class.java.classLoader) as HashMap<String, String?>
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(url)
        parcel.writeString(name)
        parcel.writeString(externalNovelId)
        parcel.writeString(imageUrl)
        parcel.writeString(rating)
        parcel.writeString(shortDescription)
        parcel.writeString(longDescription)
        parcel.writeStringList(genres)
        parcel.writeStringList(authors)
        parcel.writeStringList(illustrator)
        parcel.writeLong(chaptersCount)
        parcel.writeString(imageFilePath)
        parcel.writeLong(orderId)
        parcel.writeLong(newReleasesCount)
        parcel.writeLong(novelSectionId)
        parcel.writeLong(id)
        parcel.writeString(currentChapterUrl)
        parcel.writeMap(metadata as Map<*, *>?)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Novel> {
        override fun createFromParcel(parcel: Parcel): Novel {
            return Novel(parcel)
        }

        override fun newArray(size: Int): Array<Novel?> {
            return arrayOfNulls(size)
        }
    }

    // Other Methods
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Novel

        if (url != other.url) return false
        if (name != other.name) return false
        if (externalNovelId != other.externalNovelId) return false
        if (imageUrl != other.imageUrl) return false
        if (rating != other.rating) return false
        if (shortDescription != other.shortDescription) return false
        if (longDescription != other.longDescription) return false
        if (genres != other.genres) return false
        if (authors != other.authors) return false
        if (illustrator != other.illustrator) return false
        if (chaptersCount != other.chaptersCount) return false
        if (metadata != other.metadata) return false
        if (imageFilePath != other.imageFilePath) return false
        if (orderId != other.orderId) return false
        if (newReleasesCount != other.newReleasesCount) return false
        if (novelSectionId != other.novelSectionId) return false
        if (id != other.id) return false
        if (currentChapterUrl != other.currentChapterUrl) return false

        return true
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (externalNovelId?.hashCode() ?: 0)
        result = 31 * result + (imageUrl?.hashCode() ?: 0)
        result = 31 * result + (rating?.hashCode() ?: 0)
        result = 31 * result + (shortDescription?.hashCode() ?: 0)
        result = 31 * result + (longDescription?.hashCode() ?: 0)
        result = 31 * result + (genres?.hashCode() ?: 0)
        result = 31 * result + (authors?.hashCode() ?: 0)
        result = 31 * result + (illustrator?.hashCode() ?: 0)
        result = 31 * result + chaptersCount.hashCode()
        result = 31 * result + metadata.hashCode()
        result = 31 * result + (imageFilePath?.hashCode() ?: 0)
        result = 31 * result + orderId.hashCode()
        result = 31 * result + newReleasesCount.hashCode()
        result = 31 * result + novelSectionId.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + (currentChapterUrl?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String {
        return "Novel(url='$url', name='$name', novelId=$externalNovelId, imageUrl=$imageUrl, rating=$rating, shortDescription=$shortDescription, longDescription=$longDescription, genres=$genres, authors=$authors, illustrator=$illustrator, chaptersCount=$chaptersCount, metadata=$metadata, imageFilePath=$imageFilePath, orderId=$orderId, newReleasesCount=$newReleasesCount, novelSectionId=$novelSectionId, id=$id, currentChapterUrl=$currentChapterUrl)"
    }


}
