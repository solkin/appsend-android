package com.tomclaw.appsend_rb.screen.apps.adapter.app

import android.os.Parcelable
import com.avito.konveyor.blueprint.Item
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppItem(
    override val id: Long,
    val icon: String?,
    val packageName: String,
    val name: String,
    val size: String,
    val firstInstallTime: String,
    val lastUpdateTime: String,
    val versionName: String,
    val versionCode: Long,
    val newApp: Boolean
) : Item, Parcelable