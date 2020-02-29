package com.tomclaw.appsend_rb.util

import android.os.Parcel
import android.os.Parcelable

class AppIconData(val packageName: String, val versionCode: Int) : Parcelable {

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(packageName)
        writeInt(versionCode)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<AppIconData> {
        override fun createFromParcel(parcel: Parcel): AppIconData {
            val packageName = parcel.readString().orEmpty()
            val versionCode = parcel.readInt()
            return AppIconData(packageName, versionCode)
        }

        override fun newArray(size: Int): Array<AppIconData?> {
            return arrayOfNulls(size)
        }
    }

}