@file:Suppress("UNCHECKED_CAST")

package io.github.gmathi.novellibrary.model.database

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

//Used for Internal Database
data class WebPageSettings(var url: String, var novelId: Long) : Parcelable, Serializable {

    var title: String? = null
    var isRead: Boolean = false
    var filePath: String? = null
    var redirectedUrl: String? = null
    var metadata: HashMap<String, String?> = HashMap()

    constructor(parcel: Parcel) : this(
        parcel.readString().toString(),
        parcel.readLong()
    ) {
        title = parcel.readString()
        isRead = parcel.readByte() != 0.toByte()
        filePath = parcel.readString()
        redirectedUrl = parcel.readString()
        metadata = parcel.readHashMap(HashMap::class.java.classLoader) as HashMap<String, String?>
    }

    //region Parcel Boiler code
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(url)
        parcel.writeLong(novelId)
        parcel.writeString(title)
        parcel.writeByte(if (isRead) 1 else 0)
        parcel.writeString(filePath)
        parcel.writeString(redirectedUrl)
        parcel.writeMap(metadata as Map<*, *>?)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<WebPageSettings> {
        override fun createFromParcel(parcel: Parcel): WebPageSettings {
            return WebPageSettings(parcel)
        }

        override fun newArray(size: Int): Array<WebPageSettings?> {
            return arrayOfNulls(size)
        }
    }
    //endregion

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WebPageSettings

        if (url != other.url) return false
        if (novelId != other.novelId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + novelId.hashCode()
        result = 31 * result + (title?.hashCode() ?: 0)
        result = 31 * result + isRead.hashCode()
        result = 31 * result + (filePath?.hashCode() ?: 0)
        result = 31 * result + (redirectedUrl?.hashCode() ?: 0)
        result = 31 * result + metadata.hashCode()
        return result
    }

    override fun toString(): String {
        return "ChapterSettings(url='$url', novelId=$novelId, title=$title, isRead=$isRead, filePath=$filePath, redirectedUrl=$redirectedUrl, metadata=$metadata)"
    }


}
