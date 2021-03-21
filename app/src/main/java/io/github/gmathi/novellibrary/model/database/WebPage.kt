package io.github.gmathi.novellibrary.model.database

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

data class WebPage(var url: String, var chapterName: String) : Parcelable, Serializable {

    /**
     * Novel this chapter belongs to.
     */
    var novelId: Long = -1L

    /**
     * Order of the chapter in the chapters list. The higher the order, the latest released chapter it is.
     */
    var orderId: Long = -1L

    /**
     * In-case the chapter is aggregated from multiple translator sources, this source id determines the source. Default is -1L
     */
    var translatorSourceName: String? = null

    //Parcelable Implementation

    constructor(parcel: Parcel) : this(
        parcel.readString().toString(),
        parcel.readString().toString()
    ) {
        novelId = parcel.readLong()
        orderId = parcel.readLong()
        translatorSourceName = parcel.readString()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(url)
        parcel.writeString(chapterName)
        parcel.writeLong(novelId)
        parcel.writeLong(orderId)
        parcel.writeString(translatorSourceName)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<WebPage> {
        override fun createFromParcel(parcel: Parcel): WebPage {
            return WebPage(parcel)
        }

        override fun newArray(size: Int): Array<WebPage?> {
            return arrayOfNulls(size)
        }
    }

    // Other Methods
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as WebPage

        if (url != other.url) return false
        if (chapterName != other.chapterName) return false
        if (novelId != other.novelId) return false
        if (orderId != other.orderId) return false
        if (translatorSourceName != other.translatorSourceName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + chapterName.hashCode()
        result = 31 * result + novelId.hashCode()
        result = 31 * result + orderId.hashCode()
        result = 31 * result + translatorSourceName.hashCode()
        return result
    }

    override fun toString(): String {
        return "Chapter(url='$url', chapterName='$chapterName', novelId=$novelId, orderId=$orderId, translatorSourceName=$translatorSourceName)"
    }


}
