package com.tomclaw.appsend_rb.screen.apps.adapter.app

import android.os.Parcel
import android.os.Parcelable
import com.avito.konveyor.blueprint.Item
import com.tomclaw.appsend_rb.util.AppIconData

class AppItem(
    override val id: Long,
    val icon: AppIconData,
    val packageName: String,
    val name: String,
    val size: String,
    val firstInstallTime: String,
    val lastUpdateTime: String,
    val versionName: String,
    val versionCode: Long,
    val newApp: Boolean
) : Item, Parcelable {

    override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
        writeLong(id)
        writeParcelable(icon, 0)
        writeString(packageName)
        writeString(name)
        writeString(size)
        writeString(firstInstallTime)
        writeString(lastUpdateTime)
        writeString(versionName)
        writeLong(versionCode)
        writeBoolean(newApp)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<AppItem> {
        override fun createFromParcel(parcel: Parcel): AppItem {
            val id = parcel.readLong()
            val icon = parcel.readParcelable(AppIconData::class.java.classLoader) as AppIconData?
            val name = parcel.readString().orEmpty()
            val packageName = parcel.readString().orEmpty()
            val size = parcel.readString().orEmpty()
            val firstInstallTime = parcel.readString().orEmpty()
            val lastUpdateTime = parcel.readString().orEmpty()
            val versionName = parcel.readString().orEmpty()
            val versionCode = parcel.readLong()
            val newApp = parcel.readBoolean()
            return AppItem(
                id,
                icon!!,
                packageName,
                name,
                size,
                firstInstallTime,
                lastUpdateTime,
                versionName,
                versionCode,
                newApp
            )
        }

        override fun newArray(size: Int): Array<AppItem?> {
            return arrayOfNulls(size)
        }
    }

}