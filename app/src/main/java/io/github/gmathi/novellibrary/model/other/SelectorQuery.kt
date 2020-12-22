package io.github.gmathi.novellibrary.model.other

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

data class SelectorQuery(val query: String, val appendTitleHeader: Boolean = true) : Serializable, Parcelable {

    //region Parcelable Implementation
    constructor(parcel: Parcel) : this(parcel.readString().toString(), parcel.readByte() != 0.toByte())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(query)
        parcel.writeByte(if (appendTitleHeader) 1 else 0)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<SelectorQuery> {
        override fun createFromParcel(parcel: Parcel): SelectorQuery {
            return SelectorQuery(parcel)
        }

        override fun newArray(size: Int): Array<SelectorQuery?> {
            return arrayOfNulls(size)
        }
    }
    //endregion

    //region equals(), hashcode(), toString()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SelectorQuery

        if (query != other.query) return false
        if (appendTitleHeader != other.appendTitleHeader) return false

        return true
    }

    override fun hashCode(): Int {
        var result = query.hashCode()
        result = 31 * result + appendTitleHeader.hashCode()
        return result
    }

    override fun toString(): String {
        return "SelectorQuery(query='$query', appendTitleHeader=$appendTitleHeader)"
    }
    //endregion

}