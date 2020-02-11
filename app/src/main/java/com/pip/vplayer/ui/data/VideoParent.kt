package com.pip.vplayer.ui.data

import android.os.Parcel
import android.os.Parcelable

data class VideoParent(
    val name: String?, var videoList: MutableList<VideoItem>?
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString(),
        mutableListOf<VideoItem>().apply {
            parcel.readArrayList(VideoItem::class.java.classLoader)
        }
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<VideoParent> {
        override fun createFromParcel(parcel: Parcel): VideoParent {
            return VideoParent(parcel)
        }

        override fun newArray(size: Int): Array<VideoParent?> {
            return arrayOfNulls(size)
        }
    }
}