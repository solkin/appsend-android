package com.tomclaw.appsend_rb.domain

import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

data class InstalledApp(
    val label: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val baseSize: Long,
    val totalSize: Long,
    val splitCount: Int,
    val firstInstallTime: Long,
    val lastUpdateTime: Long,
    val system: Boolean,
    val launchable: Boolean,
) {
    val split: Boolean get() = splitCount > 0
}

data class RequestedPermission(
    val name: String,
    val description: String?,
    val dangerous: Boolean,
    val granted: Boolean,
)

data class AppDetails(
    val app: InstalledApp,
    val basePath: String,
    val installerLabel: String?,
    val minSdk: Int,
    val targetSdk: Int,
    val signatureSha256: String?,
    val permissions: List<RequestedPermission>,
)

enum class SortOrder { NAME, SIZE, UPDATED, INSTALLED }

data class AppFilter(
    val sort: SortOrder,
    val showSystem: Boolean,
    val splitOnly: Boolean,
    val runnableOnly: Boolean,
)

interface InstalledAppsRepository {

    /** The latest snapshot and every later one. Empty until the first [refresh] ends. */
    fun apps(): Observable<List<InstalledApp>>

    /** Re-reads the package list. Concurrent calls share one read. */
    fun refresh(): Completable

    fun details(packageName: String): Single<AppDetails>

}
