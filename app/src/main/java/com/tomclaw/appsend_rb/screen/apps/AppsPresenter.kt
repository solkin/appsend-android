package com.tomclaw.appsend_rb.screen.apps

import com.tomclaw.appsend_rb.domain.AppFilter
import com.tomclaw.appsend_rb.domain.ExportFormat
import com.tomclaw.appsend_rb.domain.InstalledApp
import com.tomclaw.appsend_rb.domain.ValueFormatter
import com.tomclaw.appsend_rb.domain.Analytics
import com.tomclaw.appsend_rb.domain.SchedulersFactory
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign

interface AppsPresenter {

    fun attachView(view: AppsView)

    fun detachView()

    fun attachRouter(router: AppsRouter)

    fun detachRouter()

    fun onResume()

    fun onHidden()

    fun saveState(): AppsState

    fun destroy()

}

class AppsPresenterImpl(
    private val interactor: AppsInteractor,
    private val formatter: ValueFormatter,
    private val schedulers: SchedulersFactory,
    private val analytics: Analytics,
    state: AppsState?,
    private val clock: () -> Long = System::currentTimeMillis,
) : AppsPresenter {

    private var view: AppsView? = null
    private var router: AppsRouter? = null
    private val viewSubscriptions = CompositeDisposable()
    private val dataSubscriptions = CompositeDisposable()

    private var apps: List<InstalledApp>? = null
    private var filter: AppFilter = interactor.filter()
    private var query: String = ""
    private var refreshing = false
    private var failed = false
    private var selectionMode = state?.selectionMode ?: false
    private var selected: Set<String> = state?.selected?.toSet().orEmpty()
    private var pendingUninstalls: List<String> = state?.pendingUninstalls.orEmpty()

    init {
        dataSubscriptions += interactor.apps()
            .observeOn(schedulers.mainThread())
            .subscribe { snapshot ->
                apps = snapshot
                val installed = snapshot.mapTo(HashSet()) { it.packageName }
                selected = selected.intersect(installed)
                render()
            }
        analytics.trackEvent("screen_open", mapOf("screen" to "apps"))
        load(pull = false)
    }

    override fun attachView(view: AppsView) {
        this.view = view
        viewSubscriptions += view.refreshRequests().subscribe {
            analytics.trackEvent("apps_refresh")
            load(pull = true)
        }
        viewSubscriptions += view.itemClicks().subscribe(::onItemClick)
        viewSubscriptions += view.itemLongClicks().subscribe(::onItemLongClick)
        viewSubscriptions += view.sortChanges().subscribe { updateFilter(filter.copy(sort = it)) }
        viewSubscriptions += view.systemFilterChanges().subscribe { updateFilter(filter.copy(showSystem = it)) }
        viewSubscriptions += view.splitFilterChanges().subscribe { updateFilter(filter.copy(splitOnly = it)) }
        viewSubscriptions += view.runnableFilterChanges().subscribe { updateFilter(filter.copy(runnableOnly = it)) }
        viewSubscriptions += view.queryChanges().subscribe {
            query = it
            render()
        }
        viewSubscriptions += view.selectClicks().subscribe { setSelectionMode(true) }
        viewSubscriptions += view.cancelSelectionClicks().subscribe { setSelectionMode(false) }
        viewSubscriptions += view.selectAllClicks().subscribe { toggleSelectAll() }
        viewSubscriptions += view.shareClicks().subscribe { onShareClicked() }
        viewSubscriptions += view.extractClicks().subscribe { onExtractClicked() }
        viewSubscriptions += view.removeClicks().subscribe { onRemoveClicked() }
        viewSubscriptions += view.backupWarningConfirmations().subscribe { share() }
        render()
    }

    override fun detachView() {
        viewSubscriptions.clear()
        view = null
    }

    override fun attachRouter(router: AppsRouter) {
        this.router = router
    }

    override fun detachRouter() {
        router = null
    }

    override fun onResume() {
        val next = pendingUninstalls.firstOrNull() ?: return
        pendingUninstalls = pendingUninstalls.drop(1)
        router?.uninstall(next)
    }

    /** Another tab took over: nothing here may keep holding the bottom bar or Back. */
    override fun onHidden() {
        if (selectionMode) setSelectionMode(false)
        view?.closeSearch()
    }

    override fun destroy() {
        dataSubscriptions.clear()
    }

    override fun saveState() = AppsState(
        selectionMode = selectionMode,
        selected = ArrayList(selected),
        pendingUninstalls = ArrayList(pendingUninstalls),
    )

    private fun load(pull: Boolean) {
        refreshing = pull
        failed = false
        render()
        dataSubscriptions += interactor.refresh()
            .observeOn(schedulers.mainThread())
            .subscribe({
                refreshing = false
                render()
            }, { error ->
                refreshing = false
                failed = true
                analytics.trackException(error, mapOf("event" to "apps_load_failed"))
                render()
            })
    }

    private fun updateFilter(value: AppFilter) {
        if (filter == value) return
        filter = value
        interactor.saveFilter(value)
        render()
    }

    private fun onItemClick(item: AppItem) {
        if (selectionMode) {
            selected = if (item.packageName in selected) {
                selected - item.packageName
            } else {
                selected + item.packageName
            }
            render()
        } else {
            router?.openDetails(item.packageName)
        }
    }

    private fun onItemLongClick(item: AppItem) {
        if (selectionMode) return
        selected = setOf(item.packageName)
        setSelectionMode(true)
    }

    private fun setSelectionMode(enabled: Boolean) {
        selectionMode = enabled
        if (!enabled) selected = emptySet()
        render()
    }

    private fun toggleSelectAll() {
        val visible = visibleApps().map { it.packageName }
        selected = if (selected.containsAll(visible)) emptySet() else visible.toSet()
        render()
    }

    private fun selectedApps(): List<InstalledApp> =
        apps.orEmpty().filter { it.packageName in selected }

    private fun onShareClicked() {
        val chosen = selectedApps()
        if (chosen.isEmpty()) {
            view?.showNothingSelected()
            return
        }
        // A system app, or a split one reduced to its base, may not reinstall from the file.
        val limited = chosen.any {
            it.system || (it.split && interactor.exportFormat() == ExportFormat.BASE)
        }
        if (limited) view?.showBackupWarning() else share()
    }

    private fun share() {
        interactor.share(selectedApps())
        setSelectionMode(false)
        router?.openExportProgress()
    }

    private fun onExtractClicked() {
        val chosen = selectedApps().map { it.packageName }
        if (chosen.isEmpty()) {
            view?.showNothingSelected()
            return
        }
        router?.openExportOptions(chosen)
        setSelectionMode(false)
    }

    private fun onRemoveClicked() {
        val chosen = selectedApps()
        if (chosen.isEmpty()) {
            view?.showNothingSelected()
            return
        }
        // Each removal is one system dialog, so they run one at a time, on resume.
        pendingUninstalls = chosen.map { it.packageName }
        setSelectionMode(false)
        onResume()
    }

    private fun visibleApps(): List<InstalledApp> =
        interactor.arrange(apps.orEmpty(), filter)

    private fun render() {
        val view = view ?: return
        val loaded = apps
        val visible = visibleApps()
        view.render(
            AppsViewModel(
                items = visible.map(::toItem),
                searchResults = interactor.search(apps.orEmpty(), query).map(::toItem),
                loading = loaded == null && !failed,
                refreshing = refreshing,
                failed = failed && loaded == null,
                empty = loaded != null && visible.isEmpty(),
                total = loaded?.size ?: 0,
                filter = filter,
                selectionMode = selectionMode,
                selectedCount = selected.size,
                selectedSize = formatter.bytes(selectedApps().sumOf { it.totalSize }),
            )
        )
    }

    private fun toItem(app: InstalledApp) = AppItem(
        packageName = app.packageName,
        name = app.label,
        version = app.versionName,
        size = formatter.bytes(app.totalSize),
        updated = formatter.relativeTime(app.lastUpdateTime),
        split = app.split,
        fresh = clock() - app.lastUpdateTime < NEW_APP_WINDOW_MS,
        selected = selectionMode && app.packageName in selected,
    )

}

private const val NEW_APP_WINDOW_MS = 24L * 60 * 60 * 1000
