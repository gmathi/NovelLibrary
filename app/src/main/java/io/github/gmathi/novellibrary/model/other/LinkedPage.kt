package io.github.gmathi.novellibrary.model.other

import android.os.Parcel
import android.os.Parcelable

/**
 * @param href The URL leading to the linked page
 * @param label The text that was used to display the linked page when applicable.
 * @param isMainContent Whether this linked page is actual main page content leading from a "click here for real chapter" page.
 */
data class LinkedPage(val href: String, val label: String, val isMainContent: Boolean = false) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString()?:"",
        parcel.readString()?:"",
        parcel.readByte() != 0.toByte()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(href)
        parcel.writeString(label)
        parcel.writeByte(if (isMainContent) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<LinkedPage> {
        override fun createFromParcel(parcel: Parcel): LinkedPage {
            return LinkedPage(parcel)
        }

        override fun newArray(size: Int): Array<LinkedPage?> {
            return arrayOfNulls(size)
        }
    }
}
