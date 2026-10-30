package com.tomclaw.appsend_rb.screen.details

import com.tomclaw.appsend_rb.domain.AppDetails
import com.tomclaw.appsend_rb.domain.AppPreferences
import com.tomclaw.appsend_rb.domain.Clipboard
import com.tomclaw.appsend_rb.domain.ExportAction
import com.tomclaw.appsend_rb.domain.ExportFormat
import com.tomclaw.appsend_rb.domain.ExportManager
import com.tomclaw.appsend_rb.domain.InstalledApp
import com.tomclaw.appsend_rb.domain.InstalledAppsRepository
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

interface AppDetailsInteractor {

    fun details(packageName: String): Single<AppDetails>

    /** The app as the repository knows it now, or empty once it is gone. */
    fun changes(packageName: String): Observable<List<InstalledApp>>

    fun exportFormat(): ExportFormat

    fun share(app: InstalledApp)

    fun copyPackageName(packageName: String)

}

class AppDetailsInteractorImpl(
    private val repository: InstalledAppsRepository,
    private val preferences: AppPreferences,
    private val exports: ExportManager,
    private val clipboard: Clipboard,
) : AppDetailsInteractor {

    override fun details(packageName: String): Single<AppDetails> = repository.details(packageName)

    override fun changes(packageName: String): Observable<List<InstalledApp>> =
        repository.apps()
            .map { apps -> apps.filter { it.packageName == packageName } }
            .distinctUntilChanged()

    override fun exportFormat(): ExportFormat = preferences.exportFormat

    override fun share(app: InstalledApp) {
        exports.start(listOf(app), ExportAction.SHARE, preferences.exportFormat)
    }

    override fun copyPackageName(packageName: String) {
        clipboard.copy(packageName, packageName)
    }

}
