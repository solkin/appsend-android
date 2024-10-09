package com.tomclaw.appsend_rb.util

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppIconData(
    val packageName: String,
    val versionCode: Long
) : Parcelable