package com.tomclaw.appsend_rb.screen.apps

import android.os.Parcelable
import com.tomclaw.appsend_rb.domain.AppFilter
import kotlinx.parcelize.Parcelize

data class AppItem(
    val packageName: String,
    val name: String,
    val version: String,
    val size: String,
    val updated: String,
    val split: Boolean,
    val fresh: Boolean,
    val selected: Boolean,
)

data class AppsViewModel(
    val items: List<AppItem>,
    val searchResults: List<AppItem>,
    val loading: Boolean,
    val refreshing: Boolean,
    val failed: Boolean,
    val empty: Boolean,
    val total: Int,
    val filter: AppFilter,
    val selectionMode: Boolean,
    val selectedCount: Int,
    val selectedSize: String,
)

@Parcelize
data class AppsState(
    val selectionMode: Boolean,
    val selected: ArrayList<String>,
    val pendingUninstalls: ArrayList<String>,
) : Parcelable

interface AppsRouter {

    fun openDetails(packageName: String)

    fun openExportOptions(packages: List<String>)

    fun openExportProgress()

    fun uninstall(packageName: String)

}
