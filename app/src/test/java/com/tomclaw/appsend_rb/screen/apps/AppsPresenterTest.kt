package com.tomclaw.appsend_rb.screen.apps

import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import com.tomclaw.appsend_rb.ImmediateSchedulers
import com.tomclaw.appsend_rb.NoAnalytics
import com.tomclaw.appsend_rb.app
import com.tomclaw.appsend_rb.domain.AppFilter
import com.tomclaw.appsend_rb.domain.ExportFormat
import com.tomclaw.appsend_rb.domain.InstalledApp
import com.tomclaw.appsend_rb.domain.SortOrder
import com.tomclaw.appsend_rb.domain.ValueFormatter
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppsPresenterTest {

    private val a = app("A")
    private val b = app("B")

    private val interactor = FakeInteractor()
    private val view = FakeView()
    private val router = FakeRouter()

    private fun presenter(state: AppsState? = null) =
        AppsPresenterImpl(interactor, Bytes, ImmediateSchedulers, NoAnalytics, state).also {
            it.attachView(view)
            it.attachRouter(router)
        }

    private fun item(app: InstalledApp) = view.model.items.first { it.packageName == app.packageName }

    @Test
    fun `tapping opens details outside selection and toggles inside it`() {
        presenter()
        interactor.apps.accept(listOf(a, b))
        view.clicks.accept(item(a))
        assertEquals(listOf(a.packageName), router.opened)

        view.longClicks.accept(item(b))
        assertTrue(view.model.selectionMode)
        assertEquals(1, view.model.selectedCount)
        view.clicks.accept(item(a))
        assertEquals(2, view.model.selectedCount)
        view.clicks.accept(item(a))
        assertEquals(1, view.model.selectedCount)
        assertEquals(1, router.opened.size)
    }

    @Test
    fun `a removed app leaves the selection`() {
        presenter()
        interactor.apps.accept(listOf(a, b))
        view.longClicks.accept(item(a))
        interactor.apps.accept(listOf(b))
        assertEquals(0, view.model.selectedCount)
    }

    @Test
    fun `sharing nothing only says so`() {
        presenter()
        interactor.apps.accept(listOf(a))
        view.shareRelay.accept(Unit)
        assertEquals(1, view.nothingSelected)
        assertTrue(interactor.shared.isEmpty())
    }

    @Test
    fun `a system app asks before sharing`() {
        val system = app("S", system = true)
        presenter()
        interactor.apps.accept(listOf(system))
        view.longClicks.accept(item(system))
        view.shareRelay.accept(Unit)
        assertEquals(1, view.warnings)
        assertTrue(interactor.shared.isEmpty())
    }

    @Test
    fun `sharing hands over the selection and leaves selection mode`() {
        presenter()
        interactor.apps.accept(listOf(a, b))
        view.longClicks.accept(item(a))
        view.shareRelay.accept(Unit)
        assertEquals(listOf(a), interactor.shared)
        assertEquals(1, router.progressOpened)
        assertFalse(view.model.selectionMode)
    }

    @Test
    fun `removals run one at a time as the screen comes back`() {
        val presenter = presenter()
        interactor.apps.accept(listOf(a, b))
        view.longClicks.accept(item(a))
        view.clicks.accept(item(b))
        view.removeRelay.accept(Unit)
        assertEquals(listOf(a.packageName), router.uninstalled)
        presenter.onResume()
        assertEquals(listOf(a.packageName, b.packageName), router.uninstalled)
        presenter.onResume()
        assertEquals(2, router.uninstalled.size)
    }

    @Test
    fun `leaving the tab ends selection and search`() {
        val presenter = presenter()
        interactor.apps.accept(listOf(a))
        view.longClicks.accept(item(a))
        presenter.onHidden()
        assertFalse(view.model.selectionMode)
        assertEquals(1, view.searchesClosed)
    }

    private class FakeInteractor : AppsInteractor {

        val apps = BehaviorRelay.create<List<InstalledApp>>()
        val shared = ArrayList<InstalledApp>()

        override fun apps(): Observable<List<InstalledApp>> = apps

        override fun refresh(): Completable = Completable.complete()

        override fun filter() = AppFilter(SortOrder.NAME, showSystem = true, splitOnly = false, runnableOnly = false)

        override fun saveFilter(filter: AppFilter) = Unit

        override fun arrange(apps: List<InstalledApp>, filter: AppFilter) = apps

        override fun search(apps: List<InstalledApp>, query: String) = emptyList<InstalledApp>()

        override fun exportFormat() = ExportFormat.BUNDLE

        override fun share(apps: List<InstalledApp>) {
            shared += apps
        }

    }

    private class FakeView : AppsView {

        lateinit var model: AppsViewModel
        var nothingSelected = 0
        var warnings = 0
        var searchesClosed = 0
        val clicks = PublishRelay.create<AppItem>()
        val longClicks = PublishRelay.create<AppItem>()
        val shareRelay = PublishRelay.create<Unit>()
        val removeRelay = PublishRelay.create<Unit>()

        override fun render(model: AppsViewModel) {
            this.model = model
        }

        override fun closeSearch() {
            searchesClosed++
        }

        override fun showNothingSelected() {
            nothingSelected++
        }

        override fun showBackupWarning() {
            warnings++
        }

        override fun refreshRequests(): Observable<Unit> = Observable.never()

        override fun itemClicks(): Observable<AppItem> = clicks

        override fun itemLongClicks(): Observable<AppItem> = longClicks

        override fun sortChanges(): Observable<SortOrder> = Observable.never()

        override fun systemFilterChanges(): Observable<Boolean> = Observable.never()

        override fun splitFilterChanges(): Observable<Boolean> = Observable.never()

        override fun runnableFilterChanges(): Observable<Boolean> = Observable.never()

        override fun queryChanges(): Observable<String> = Observable.never()

        override fun selectClicks(): Observable<Unit> = Observable.never()

        override fun cancelSelectionClicks(): Observable<Unit> = Observable.never()

        override fun selectAllClicks(): Observable<Unit> = Observable.never()

        override fun shareClicks(): Observable<Unit> = shareRelay

        override fun extractClicks(): Observable<Unit> = Observable.never()

        override fun removeClicks(): Observable<Unit> = removeRelay

        override fun backupWarningConfirmations(): Observable<Unit> = Observable.never()
    }

    private class FakeRouter : AppsRouter {

        val opened = ArrayList<String>()
        val uninstalled = ArrayList<String>()
        var progressOpened = 0

        override fun openDetails(packageName: String) {
            opened += packageName
        }

        override fun openExportOptions(packages: List<String>) = Unit

        override fun openExportProgress() {
            progressOpened++
        }

        override fun uninstall(packageName: String) {
            uninstalled += packageName
        }

    }

    private object Bytes : ValueFormatter {

        override fun bytes(value: Long) = "$value"

        override fun relativeTime(time: Long) = "$time"

        override fun date(time: Long) = "$time"

    }

}
