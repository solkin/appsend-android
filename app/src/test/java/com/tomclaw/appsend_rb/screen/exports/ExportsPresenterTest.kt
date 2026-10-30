package com.tomclaw.appsend_rb.screen.exports

import com.jakewharton.rxrelay3.PublishRelay
import com.tomclaw.appsend_rb.ImmediateSchedulers
import com.tomclaw.appsend_rb.NoAnalytics
import com.tomclaw.appsend_rb.domain.ArchiveInfo
import com.tomclaw.appsend_rb.domain.ExportedFile
import com.tomclaw.appsend_rb.domain.ValueFormatter
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExportsPresenterTest {

    private val now = 10L * 24 * 60 * 60 * 1000
    private val a = ExportedFile("uri:a", "a.apk", 10, now)
    private val b = ExportedFile("uri:b", "b.apks", 20, now - 2L * 24 * 60 * 60 * 1000)
    private val info = ArchiveInfo("pkg", "A", "1", 1)

    private val interactor = FakeInteractor()
    private val view = FakeView()
    private val router = FakeRouter()
    private val presenter = ExportsPresenterImpl(interactor, Bytes, ImmediateSchedulers, NoAnalytics, null) { now }.also {
        it.attachView(view)
        it.attachRouter(router)
    }

    private fun load(vararg entries: ExportEntry) {
        interactor.next = Observable.just(entries.toList())
        presenter.onResume()
    }

    private fun bare(file: ExportedFile) = ExportEntry(file, null, InstallState.UNKNOWN)

    private fun row(file: ExportedFile) = view.model.rows
        .filterIsInstance<ExportsRow.File>()
        .first { it.entry.file == file }

    @Test
    fun `files are grouped by age`() {
        load(bare(a), bare(b))
        assertEquals(
            listOf(ExportAge.TODAY, ExportAge.THIS_WEEK),
            view.model.rows.filterIsInstance<ExportsRow.Section>().map { it.age },
        )
    }

    @Test
    fun `a reload keeps what is known about a file until it is read again`() {
        load(ExportEntry(a, info, InstallState.CURRENT))
        load(bare(a))
        assertEquals(InstallState.CURRENT, row(a).entry.state)
    }

    @Test
    fun `tapping shows the menu until selection starts, then toggles`() {
        load(bare(a), bare(b))
        view.clicks.accept(row(a))
        assertEquals(1, view.menus)

        view.longClicks.accept(row(a))
        assertTrue(view.model.selectionMode)
        assertEquals(1, view.model.selectedCount)
        view.clicks.accept(row(b))
        assertEquals(2, view.model.selectedCount)
        assertEquals("30 B", view.model.selectedSize)
        assertEquals(1, view.menus)
    }

    @Test
    fun `select all takes the visible files and a second press clears them`() {
        load(bare(a), bare(b))
        view.selectRelay.accept(Unit)
        view.selectAllRelay.accept(Unit)
        assertEquals(2, view.model.selectedCount)
        view.selectAllRelay.accept(Unit)
        assertEquals(0, view.model.selectedCount)
    }

    @Test
    fun `sharing the selection hands over the files and leaves selection mode`() {
        load(bare(a), bare(b))
        view.longClicks.accept(row(b))
        view.shareSelectedRelay.accept(Unit)
        assertEquals(listOf(listOf(b)), router.shared)
        assertFalse(view.model.selectionMode)
    }

    @Test
    fun `an empty selection only says so`() {
        load(bare(a))
        view.selectRelay.accept(Unit)
        view.shareSelectedRelay.accept(Unit)
        view.deleteSelectedRelay.accept(Unit)
        assertEquals(2, view.nothingSelected)
        assertTrue(router.shared.isEmpty())
        assertEquals(0, view.confirmations)
    }

    @Test
    fun `deleting asks first, then removes the selection and reloads`() {
        load(bare(a), bare(b))
        view.longClicks.accept(row(a))
        view.deleteSelectedRelay.accept(Unit)
        assertEquals(1, view.confirmations)
        assertTrue(interactor.deleted.isEmpty())

        interactor.next = Observable.just(listOf(bare(b)))
        view.deleteConfirmRelay.accept(Unit)
        assertEquals(listOf(listOf(a)), interactor.deleted)
        assertEquals(1, view.model.count)
        assertFalse(view.model.selectionMode)
    }

    @Test
    fun `a failed delete reports it and still shows what is left`() {
        load(bare(a))
        interactor.deleteResult = Completable.error(IllegalStateException())
        interactor.next = Observable.just(emptyList())
        view.deleteRelay.accept(a)
        assertEquals(1, view.deleteFailures)
        assertEquals(0, view.model.count)
    }

    @Test
    fun `a selected file that disappears leaves the selection`() {
        load(bare(a), bare(b))
        view.longClicks.accept(row(a))
        load(bare(b))
        assertEquals(0, view.model.selectedCount)
    }

    @Test
    fun `a selected file that left the filter is not acted on`() {
        load(ExportEntry(a, info, InstallState.NEWER_INSTALLED), ExportEntry(b, info, InstallState.NEWER_INSTALLED))
        view.filterRelay.accept(ExportsFilter.OUTDATED)
        view.longClicks.accept(row(a))
        load(ExportEntry(a, info, InstallState.NOT_INSTALLED), ExportEntry(b, info, InstallState.NEWER_INSTALLED))
        assertEquals(0, view.model.selectedCount)
        view.deleteSelectedRelay.accept(Unit)
        assertEquals(0, view.confirmations)
    }

    @Test
    fun `select all with nothing on screen keeps a restored selection`() {
        val restored = ExportsPresenterImpl(
            interactor, Bytes, ImmediateSchedulers, NoAnalytics, ExportsState(true, arrayListOf(a.uri)),
        ) { now }
        restored.attachView(view)
        view.selectAllRelay.accept(Unit)
        interactor.next = Observable.just(listOf(bare(a)))
        restored.onResume()
        assertEquals(1, view.model.selectedCount)
    }

    @Test
    fun `leaving the tab ends selection`() {
        load(bare(a))
        view.longClicks.accept(row(a))
        presenter.onHidden()
        assertFalse(view.model.selectionMode)
    }

    private class FakeInteractor : ExportsInteractor {

        var next: Observable<List<ExportEntry>> = Observable.never()
        var deleteResult: Completable = Completable.complete()
        val deleted = ArrayList<List<ExportedFile>>()

        override fun entries() = next

        override fun invalidations(): Observable<Unit> = Observable.never()

        override fun delete(files: List<ExportedFile>): Completable {
            deleted += files
            return deleteResult
        }

        override fun clear(): Completable = Completable.complete()

        override fun folder() = "folder"

        override fun saveFolder(uri: String) = Unit

    }

    private class FakeView : ExportsView {

        lateinit var model: ExportsViewModel
        var menus = 0
        var deleteFailures = 0
        var nothingSelected = 0
        var confirmations = 0
        val filterRelay = PublishRelay.create<ExportsFilter>()
        val clicks = PublishRelay.create<ExportsRow.File>()
        val longClicks = PublishRelay.create<ExportsRow.File>()
        val deleteRelay = PublishRelay.create<ExportedFile>()
        val selectRelay = PublishRelay.create<Unit>()
        val selectAllRelay = PublishRelay.create<Unit>()
        val shareSelectedRelay = PublishRelay.create<Unit>()
        val deleteSelectedRelay = PublishRelay.create<Unit>()
        val deleteConfirmRelay = PublishRelay.create<Unit>()

        override fun render(model: ExportsViewModel) {
            this.model = model
        }

        override fun showFileMenu(row: ExportsRow.File) {
            menus++
        }

        override fun showDeleteFailed() {
            deleteFailures++
        }

        override fun showNothingSelected() {
            nothingSelected++
        }

        override fun showDeleteSelectedConfirmation(count: Int) {
            confirmations++
        }

        override fun filterChanges(): Observable<ExportsFilter> = filterRelay

        override fun rowClicks(): Observable<ExportsRow.File> = clicks

        override fun rowLongClicks(): Observable<ExportsRow.File> = longClicks

        override fun installClicks(): Observable<ExportedFile> = Observable.never()

        override fun shareClicks(): Observable<ExportedFile> = Observable.never()

        override fun deleteConfirmations(): Observable<ExportedFile> = deleteRelay

        override fun clearConfirmations(): Observable<Unit> = Observable.never()

        override fun changeFolderClicks(): Observable<Unit> = Observable.never()

        override fun installFromFileClicks(): Observable<Unit> = Observable.never()

        override fun selectClicks(): Observable<Unit> = selectRelay

        override fun cancelSelectionClicks(): Observable<Unit> = Observable.never()

        override fun selectAllClicks(): Observable<Unit> = selectAllRelay

        override fun shareSelectedClicks(): Observable<Unit> = shareSelectedRelay

        override fun deleteSelectedClicks(): Observable<Unit> = deleteSelectedRelay

        override fun deleteSelectedConfirmations(): Observable<Unit> = deleteConfirmRelay

    }

    private class FakeRouter : ExportsRouter {

        val shared = ArrayList<List<ExportedFile>>()

        override fun shareFiles(files: List<ExportedFile>) {
            shared += files
        }

        override fun install(uri: String) = Unit

        override fun pickPackageFile() = Unit

        override fun pickFolder() = Unit

    }

    private object Bytes : ValueFormatter {

        override fun bytes(value: Long) = "$value B"

        override fun relativeTime(time: Long) = "$time"

        override fun date(time: Long) = "$time"

    }

}
