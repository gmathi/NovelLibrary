package io.github.gmathi.novellibrary.model

import android.os.Parcel
import android.os.Parcelable

data class NovelSection(var id: Long = 0, var name: String? = null, var orderId: Long = 999L) : Parcelable {

    // region Parcelable
    constructor(parcel: Parcel) : this() {
        id = parcel.readLong()
        name = parcel.readString()
        orderId = parcel.readLong()
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(name)
        parcel.writeLong(orderId)
    }

    override fun describeContents(): Int {
        return id.toInt()
    }

    companion object CREATOR : Parcelable.Creator<NovelSection> {
        override fun createFromParcel(parcel: Parcel): NovelSection {
            return NovelSection(parcel)
        }

        override fun newArray(size: Int): Array<NovelSection?> {
            return arrayOfNulls(size)
        }
    }
    // endregion

}