package com.tomclaw.appsend_rb.dto

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppEntity(
    val label: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val requestedPermissions: List<String>?,
    val path: String,
    val size: Long,
    val firstInstallTime: Long,
    val lastUpdateTime: Long
) : Parcelable
