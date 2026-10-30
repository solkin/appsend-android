package com.tomclaw.appsend_rb.screen.exports

import com.tomclaw.appsend_rb.domain.Analytics
import com.tomclaw.appsend_rb.domain.ExportedFile
import com.tomclaw.appsend_rb.domain.SchedulersFactory
import com.tomclaw.appsend_rb.domain.ValueFormatter
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.plusAssign

enum class ExportsFilter { ALL, INSTALLED, OUTDATED }

enum class ExportAge { TODAY, THIS_WEEK, EARLIER }

sealed interface ExportsRow {

    data class Section(val age: ExportAge) : ExportsRow

    data class File(
        val entry: ExportEntry,
        val size: String,
        val time: String,
        val selectable: Boolean,
        val selected: Boolean,
        val position: Int,
        val groupSize: Int,
    ) : ExportsRow

}

data class ExportsViewModel(
    val loading: Boolean,
    val folder: String,
    val count: Int,
    val totalSize: String,
    val rows: List<ExportsRow>,
    val selectionMode: Boolean,
    val selectedCount: Int,
    val selectedSize: String,
)

interface ExportsRouter {

    fun shareFiles(files: List<ExportedFile>)

    fun pickFolder()

}

interface ExportsPresenter {

    fun attachView(view: ExportsView)

    fun detachView()

    fun attachRouter(router: ExportsRouter)

    fun detachRouter()

    fun onResume()

    fun onHidden()

    fun onFolderPicked(uri: String)

    fun saveState(): ExportsState

    fun destroy()

}

class ExportsPresenterImpl(
    private val interactor: ExportsInteractor,
    private val formatter: ValueFormatter,
    private val schedulers: SchedulersFactory,
    private val analytics: Analytics,
    state: ExportsState? = null,
    private val clock: () -> Long = System::currentTimeMillis,
) : ExportsPresenter {

    private var view: ExportsView? = null
    private var router: ExportsRouter? = null
    private val viewSubscriptions = CompositeDisposable()
    private val dataSubscriptions = CompositeDisposable()
    private var loadSubscription: Disposable? = null
    private var entries: List<ExportEntry>? = null
    private var filter = ExportsFilter.ALL
    private var selectionMode = state?.selectionMode ?: false
    private var selected: Set<String> = state?.selected?.toSet().orEmpty()

    init {
        analytics.trackEvent("screen_open", mapOf("screen" to "exports"))
        dataSubscriptions += interactor.invalidations()
            .observeOn(schedulers.mainThread())
            .subscribe { load() }
    }

    override fun attachView(view: ExportsView) {
        this.view = view
        viewSubscriptions += view.filterChanges().subscribe {
            filter = it
            render()
        }
        viewSubscriptions += view.rowClicks().subscribe(::onRowClick)
        viewSubscriptions += view.rowLongClicks().subscribe(::onRowLongClick)
        viewSubscriptions += view.shareClicks().subscribe { router?.shareFiles(listOf(it)) }
        viewSubscriptions += view.deleteConfirmations().subscribe { delete(listOf(it)) }
        viewSubscriptions += view.clearConfirmations().subscribe { clear() }
        viewSubscriptions += view.changeFolderClicks().subscribe { router?.pickFolder() }
        viewSubscriptions += view.selectClicks().subscribe { setSelectionMode(true) }
        viewSubscriptions += view.cancelSelectionClicks().subscribe { setSelectionMode(false) }
        viewSubscriptions += view.selectAllClicks().subscribe { toggleSelectAll() }
        viewSubscriptions += view.shareSelectedClicks().subscribe { shareSelected() }
        viewSubscriptions += view.deleteSelectedClicks().subscribe { confirmDeleteSelected() }
        viewSubscriptions += view.deleteSelectedConfirmations().subscribe { deleteSelected() }
        render()
    }

    override fun detachView() {
        viewSubscriptions.clear()
        view = null
    }

    override fun attachRouter(router: ExportsRouter) {
        this.router = router
    }

    override fun detachRouter() {
        router = null
    }

    override fun onResume() {
        load()
    }

    /** Another tab took over: nothing here may keep holding the bottom bar or Back. */
    override fun onHidden() {
        if (selectionMode) setSelectionMode(false)
    }

    override fun saveState() = ExportsState(selectionMode, ArrayList(selected))

    override fun destroy() {
        dataSubscriptions.clear()
        loadSubscription?.dispose()
    }

    override fun onFolderPicked(uri: String) {
        interactor.saveFolder(uri)
        setSelectionMode(false)
        load()
    }

    private fun load() {
        loadSubscription?.dispose()
        loadSubscription = interactor.entries()
            .observeOn(schedulers.mainThread())
            .subscribe({
                entries = keepKnownIdentity(it)
                val existing = it.mapTo(HashSet()) { entry -> entry.file.uri }
                selected = selected.intersect(existing)
                render()
            }, { error ->
                analytics.trackException(error, mapOf("event" to "exports_load_failed"))
                entries = entries.orEmpty()
                selected = emptySet()
                render()
            })
    }

    /** A reload first lists bare files: rows must not lose what is known until it is re-read. */
    private fun keepKnownIdentity(fresh: List<ExportEntry>): List<ExportEntry> {
        val known = entries.orEmpty().associateBy { it.file.uri }
        return fresh.map { entry ->
            val previous = known[entry.file.uri]
            if (entry.info == null && previous != null) {
                entry.copy(info = previous.info, state = previous.state)
            } else {
                entry
            }
        }
    }

    private fun onRowClick(row: ExportsRow.File) {
        val uri = row.entry.file.uri
        if (selectionMode) {
            selected = if (uri in selected) selected - uri else selected + uri
            render()
        } else {
            view?.showFileMenu(row)
        }
    }

    private fun onRowLongClick(row: ExportsRow.File) {
        if (selectionMode) return
        selected = setOf(row.entry.file.uri)
        setSelectionMode(true)
    }

    private fun setSelectionMode(enabled: Boolean) {
        selectionMode = enabled
        if (!enabled) selected = emptySet()
        render()
    }

    private fun visibleEntries(): List<ExportEntry> = entries.orEmpty().filter {
        when (filter) {
            ExportsFilter.ALL -> true
            ExportsFilter.INSTALLED -> it.state == InstallState.CURRENT
            ExportsFilter.OUTDATED -> it.state == InstallState.NEWER_INSTALLED
        }
    }

    private fun toggleSelectAll() {
        val visible = visibleEntries().map { it.file.uri }
        if (visible.isEmpty()) return
        selected = if (selected.containsAll(visible)) emptySet() else visible.toSet()
        render()
    }

    /** A reload can move a selected file out of the filter: what is not shown is not acted on. */
    private fun selectedFiles(): List<ExportedFile> =
        visibleEntries().map { it.file }.filter { it.uri in selected }

    private fun shareSelected() {
        val files = selectedFiles()
        if (files.isEmpty()) {
            view?.showNothingSelected()
            return
        }
        router?.shareFiles(files)
        setSelectionMode(false)
    }

    private fun confirmDeleteSelected() {
        val count = selectedFiles().size
        if (count == 0) view?.showNothingSelected() else view?.showDeleteSelectedConfirmation(count)
    }

    private fun deleteSelected() {
        val files = selectedFiles()
        setSelectionMode(false)
        delete(files)
    }

    private fun delete(files: List<ExportedFile>) {
        dataSubscriptions += interactor.delete(files)
            .observeOn(schedulers.mainThread())
            .subscribe({ load() }, {
                view?.showDeleteFailed()
                load()
            })
    }

    private fun clear() {
        dataSubscriptions += interactor.clear()
            .observeOn(schedulers.mainThread())
            .subscribe({ load() }, {
                view?.showDeleteFailed()
                load()
            })
    }

    private fun render() {
        val all = entries
        view?.render(
            ExportsViewModel(
                loading = all == null,
                folder = interactor.folder(),
                count = all.orEmpty().size,
                totalSize = formatter.bytes(all.orEmpty().sumOf { it.file.size }),
                rows = group(visibleEntries()),
                selectionMode = selectionMode,
                selectedCount = selectedFiles().size,
                selectedSize = formatter.bytes(selectedFiles().sumOf { it.size }),
            )
        )
    }

    private fun group(entries: List<ExportEntry>): List<ExportsRow> {
        val now = clock()
        return entries.groupBy { ageOf(it.file.modified, now) }
            .toSortedMap()
            .flatMap { (age, group) ->
                listOf<ExportsRow>(ExportsRow.Section(age)) + group.mapIndexed { index, entry ->
                    ExportsRow.File(
                        entry = entry,
                        size = formatter.bytes(entry.file.size),
                        time = formatter.relativeTime(entry.file.modified),
                        selectable = selectionMode,
                        selected = entry.file.uri in selected,
                        position = index,
                        groupSize = group.size,
                    )
                }
            }
    }

    private fun ageOf(time: Long, now: Long): ExportAge = when {
        now - time < DAY_MS -> ExportAge.TODAY
        now - time < WEEK_MS -> ExportAge.THIS_WEEK
        else -> ExportAge.EARLIER
    }

}

private const val DAY_MS = 24L * 60 * 60 * 1000
private const val WEEK_MS = 7 * DAY_MS
