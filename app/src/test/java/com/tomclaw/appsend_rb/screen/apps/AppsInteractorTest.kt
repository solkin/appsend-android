package com.tomclaw.appsend_rb.screen.apps

import com.tomclaw.appsend_rb.FakePreferences
import com.tomclaw.appsend_rb.app
import com.tomclaw.appsend_rb.domain.AppFilter
import com.tomclaw.appsend_rb.domain.AppDetails
import com.tomclaw.appsend_rb.domain.ExportAction
import com.tomclaw.appsend_rb.domain.ExportFormat
import com.tomclaw.appsend_rb.domain.ExportManager
import com.tomclaw.appsend_rb.domain.ExportSession
import com.tomclaw.appsend_rb.domain.InstalledApp
import com.tomclaw.appsend_rb.domain.InstalledAppsRepository
import com.tomclaw.appsend_rb.domain.SortOrder
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class AppsInteractorTest {

    private val interactor = AppsInteractorImpl(NoRepository, FakePreferences(), NoExports, Locale.ROOT)

    private val filter = AppFilter(SortOrder.NAME, showSystem = true, splitOnly = false, runnableOnly = false)

    private fun labels(apps: List<InstalledApp>) = apps.map { it.label }

    @Test
    fun `sorts by name ignoring case`() {
        val apps = listOf(app("beta"), app("Alpha"), app("Gamma"))
        assertEquals(listOf("Alpha", "beta", "Gamma"), labels(interactor.arrange(apps, filter)))
    }

    @Test
    fun `sorts by total size including splits`() {
        val apps = listOf(app("Small", baseSize = 40), app("Big", baseSize = 10, totalSize = 90, splits = 2))
        assertEquals(listOf("Big", "Small"), labels(interactor.arrange(apps, filter.copy(sort = SortOrder.SIZE))))
    }

    @Test
    fun `sorts by dates newest first`() {
        val apps = listOf(app("Old", installed = 1, updated = 5), app("New", installed = 2, updated = 3))
        assertEquals(listOf("New", "Old"), labels(interactor.arrange(apps, filter.copy(sort = SortOrder.INSTALLED))))
        assertEquals(listOf("Old", "New"), labels(interactor.arrange(apps, filter.copy(sort = SortOrder.UPDATED))))
    }

    @Test
    fun `filters apply together`() {
        val apps = listOf(
            app("System", system = true, splits = 1),
            app("Split", splits = 1),
            app("Service", splits = 1, launchable = false),
            app("Plain"),
        )
        val strict = filter.copy(showSystem = false, splitOnly = true, runnableOnly = true)
        assertEquals(listOf("Split"), labels(interactor.arrange(apps, strict)))
    }

    @Test
    fun `search matches name and package and ignores blank queries`() {
        val apps = listOf(app("Telegram", packageName = "org.telegram"), app("Maps", packageName = "app.organic"))
        assertEquals(listOf("Telegram"), labels(interactor.search(apps, "TELE")))
        assertEquals(listOf("Maps"), labels(interactor.search(apps, "organic")))
        assertEquals(emptyList<String>(), labels(interactor.search(apps, "  ")))
    }

    private object NoRepository : InstalledAppsRepository {

        override fun apps(): Observable<List<InstalledApp>> = Observable.never()

        override fun refresh(): Completable = Completable.complete()

        override fun details(packageName: String): Single<AppDetails> = Single.never()

    }

    private object NoExports : ExportManager {

        override fun session(): Observable<ExportSession> = Observable.never()

        override val current = ExportSession()

        override var observed = false

        override fun start(apps: List<InstalledApp>, action: ExportAction, format: ExportFormat) = Unit

        override fun cancel() = Unit

        override fun retry(packageName: String) = Unit

        override fun dismiss() = Unit

    }

}
