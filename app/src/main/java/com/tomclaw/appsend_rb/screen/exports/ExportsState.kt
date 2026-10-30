package com.tomclaw.appsend_rb.screen.exports

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class ExportsState(
    val selectionMode: Boolean,
    val selected: ArrayList<String>,
) : Parcelable
