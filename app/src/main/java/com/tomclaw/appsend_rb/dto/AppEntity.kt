package com.tomclaw.appsend_rb.dto

import android.os.Parcel
import android.os.Parcelable

class AppEntity(
        val label: String,
        val packageName: String,
        val versionName: String,
        val versionCode: Long,
        val path: String,
        val size: Long,
        val firstInstallTime: Long,
        val lastUpdateTime: Long
) : Parcelable {

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeString(label)
        writeString(packageName)
        writeString(versionName)
        writeLong(versionCode)
        writeString(path)
        writeLong(size)
        writeLong(firstInstallTime)
        writeLong(lastUpdateTime)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<AppEntity> {
        override fun createFromParcel(parcel: Parcel): AppEntity {
            val label = parcel.readString().orEmpty()
            val packageName = parcel.readString().orEmpty()
            val versionName = parcel.readString().orEmpty()
            val versionCode = parcel.readLong()
            val path = parcel.readString().orEmpty()
            val size = parcel.readLong()
            val firstInstallTime = parcel.readLong()
            val lastUpdateTime = parcel.readLong()
            return AppEntity(label, packageName, versionName, versionCode, path, size, firstInstallTime, lastUpdateTime)
        }

        override fun newArray(size: Int): Array<AppEntity?> {
            return arrayOfNulls(size)
        }
    }
}
