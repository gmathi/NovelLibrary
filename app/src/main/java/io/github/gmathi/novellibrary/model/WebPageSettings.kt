package io.github.gmathi.novellibrary.model

import android.os.Parcel
import android.os.Parcelable

data class WebPageSettings(var url: String, var novelId: Long) : Parcelable {

    var title: String? = null
    var isRead: Int = 0
    var filePath: String? = null
    var redirectedUrl: String? = null
    var metaData: HashMap<String, String?> = HashMap()

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
        result = 31 * result + metaData.hashCode()
        return result
    }

    override fun toString(): String {
        return "WebPageSettings(url='$url', novelId=$novelId, title=$title, isRead=$isRead, filePath=$filePath, redirectedUrl=$redirectedUrl, metaData=$metaData)"
    }

    // region Parcelable
    constructor(parcel: Parcel) : this(parcel.readString()!!, parcel.readLong()) {
        title = parcel.readString()
        isRead = parcel.readInt()
        filePath = parcel.readString()
        redirectedUrl = parcel.readString()
        @Suppress("UNCHECKED_CAST")
        metaData = parcel.readSerializable() as HashMap<String, String?>
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(url)
        parcel.writeLong(novelId)
        parcel.writeString(title)
        parcel.writeInt(isRead)
        parcel.writeString(filePath)
        parcel.writeString(redirectedUrl)
        parcel.writeSerializable(metaData)
    }

    override fun describeContents(): Int {
        return hashCode()
    }

    companion object CREATOR : Parcelable.Creator<WebPageSettings> {
        override fun createFromParcel(parcel: Parcel): WebPageSettings {
            return WebPageSettings(parcel)
        }

        override fun newArray(size: Int): Array<WebPageSettings?> {
            return arrayOfNulls(size)
        }
    }
    // endregion

}
