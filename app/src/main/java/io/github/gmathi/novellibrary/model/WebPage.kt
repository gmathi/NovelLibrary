package io.github.gmathi.novellibrary.model

import android.os.Parcel
import android.os.Parcelable

data class WebPage(var url: String, var chapter: String) : Parcelable {

    var novelId: Long = -1L
    var sourceId: Long = -1L
    var orderId: Long = -1L

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
        result = 31 * result + sourceId.hashCode()
        result = 31 * result + orderId.hashCode()
        return result
    }

    override fun toString(): String {
        return "WebPage(url='$url', chapter='$chapter', novelId=$novelId, sourceId=$sourceId, orderId=$orderId)"
    }

    // region Parcelable
    constructor(parcel: Parcel) : this(parcel.readString()!!, parcel.readString()!!) {
        novelId = parcel.readLong()
        sourceId = parcel.readLong()
        orderId = parcel.readLong()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(url)
        parcel.writeString(chapter)
        parcel.writeLong(novelId)
        parcel.writeLong(sourceId)
        parcel.writeLong(orderId)
    }

    override fun describeContents(): Int {
        return hashCode()
    }

    companion object CREATOR : Parcelable.Creator<WebPage> {
        override fun createFromParcel(parcel: Parcel): WebPage {
            return WebPage(parcel)
        }

        override fun newArray(size: Int): Array<WebPage?> {
            return arrayOfNulls(size)
        }
    }
    // endregion

}
