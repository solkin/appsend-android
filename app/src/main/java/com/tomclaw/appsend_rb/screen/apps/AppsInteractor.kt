package com.tomclaw.appsend_rb.screen.apps

import com.tomclaw.appsend_rb.domain.AppFilter
import com.tomclaw.appsend_rb.domain.AppPreferences
import com.tomclaw.appsend_rb.domain.ExportAction
import com.tomclaw.appsend_rb.domain.ExportFormat
import com.tomclaw.appsend_rb.domain.ExportManager
import com.tomclaw.appsend_rb.domain.InstalledApp
import com.tomclaw.appsend_rb.domain.InstalledAppsRepository
import com.tomclaw.appsend_rb.domain.SortOrder
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import java.util.Locale

interface AppsInteractor {

    fun apps(): Observable<List<InstalledApp>>

    fun refresh(): Completable

    fun filter(): AppFilter

    fun saveFilter(filter: AppFilter)

    /** Applies the chips. The search query narrows a separate list, so it stays out. */
    fun arrange(apps: List<InstalledApp>, filter: AppFilter): List<InstalledApp>

    fun search(apps: List<InstalledApp>, query: String): List<InstalledApp>

    fun exportFormat(): ExportFormat

    fun share(apps: List<InstalledApp>)

}

class AppsInteractorImpl(
    private val repository: InstalledAppsRepository,
    private val preferences: AppPreferences,
    private val exports: ExportManager,
    private val locale: Locale,
) : AppsInteractor {

    override fun apps(): Observable<List<InstalledApp>> = repository.apps()

    override fun refresh(): Completable = repository.refresh()

    override fun filter(): AppFilter = preferences.appFilter

    override fun saveFilter(filter: AppFilter) {
        preferences.appFilter = filter
    }

    override fun arrange(apps: List<InstalledApp>, filter: AppFilter): List<InstalledApp> {
        val visible = apps.filter { app ->
            (filter.showSystem || !app.system) &&
                (!filter.splitOnly || app.split) &&
                (!filter.runnableOnly || app.launchable)
        }
        return when (filter.sort) {
            SortOrder.NAME -> visible.sortedBy { it.label.uppercase(locale) }
            SortOrder.SIZE -> visible.sortedByDescending { it.totalSize }
            SortOrder.UPDATED -> visible.sortedByDescending { it.lastUpdateTime }
            SortOrder.INSTALLED -> visible.sortedByDescending { it.firstInstallTime }
        }
    }

    override fun search(apps: List<InstalledApp>, query: String): List<InstalledApp> {
        if (query.isBlank()) return emptyList()
        return apps
            .filter {
                it.label.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }
            .sortedBy { it.label.uppercase(locale) }
    }

    override fun exportFormat(): ExportFormat = preferences.exportFormat

    override fun share(apps: List<InstalledApp>) {
        exports.start(apps, ExportAction.SHARE, preferences.exportFormat)
    }

}
