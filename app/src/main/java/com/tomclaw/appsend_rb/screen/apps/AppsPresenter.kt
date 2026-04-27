package com.tomclaw.appsend_rb.screen.apps

import android.net.Uri
import android.os.Build
import android.os.Bundle
import com.avito.konveyor.adapter.AdapterPresenter
import com.avito.konveyor.blueprint.Item
import com.avito.konveyor.data_source.ListDataSource
import com.tomclaw.appsend_rb.dto.AppEntity
import com.tomclaw.appsend_rb.screen.apps.adapter.ItemClickListener
import com.tomclaw.appsend_rb.screen.apps.adapter.app.AppItem
import com.tomclaw.appsend_rb.util.SchedulersFactory
import dagger.Lazy
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign

interface AppsPresenter : ItemClickListener {

    fun attachView(view: AppsView)

    fun detachView()

    fun attachRouter(router: AppsRouter)

    fun detachRouter()

    fun saveState(): Bundle

    fun onBackPressed()

    fun onResume()

    fun invalidateAppsList()

    interface AppsRouter {

        fun showPrefsScreen()

        fun showInfoScreen()

        fun leaveScreen()

        fun runApp(packageName: String): Boolean

        fun openGooglePlay(packageName: String)

        fun showRequestedPermissions(permissions: List<String>)

        fun showAppDetails(entity: AppEntity)

        fun runAppUninstall(packageName: String)

        fun shareApk(uri: Uri)

        fun shareApks(uris: List<Uri>)

        fun requestPermissions(onGranted: () -> Unit, onDenied: () -> Unit)

    }

}

class AppsPresenterImpl(
    private val interactor: AppsInteractor,
    private val adapterPresenter: Lazy<AdapterPresenter>,
    private val appEntityConverter: AppEntityConverter,
    private val preferences: PreferencesProvider,
    private val schedulers: SchedulersFactory,
    state: Bundle?
) : AppsPresenter {

    private var view: AppsView? = null
    private var router: AppsPresenter.AppsRouter? = null

    private var entities: List<AppEntity>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        state?.getParcelableArrayList(KEY_ENTITIES, AppEntity::class.java)
    } else {
        @Suppress("DEPRECATION")
        state?.getParcelableArrayList(KEY_ENTITIES)
    }

    private var packageMayBeDeleted: String? = state?.getString(KEY_PACKAGE_MAY_BE_DELETED)
    private var selectionMode: Boolean = state?.getBoolean(KEY_SELECTION_MODE) ?: false
    private var selectedPackages: Set<String> = state?.getStringArrayList(KEY_SELECTED_PACKAGES)
        ?.toSet()
        ?: emptySet()

    private val subscriptions = CompositeDisposable()

    override fun attachView(view: AppsView) {
        this.view = view
        subscriptions += view.refreshClicks().subscribe { loadAppItems() }
        subscriptions += view.prefsClicks().subscribe { onPrefsClicked() }
        subscriptions += view.infoClicks().subscribe { onInfoClicked() }
        subscriptions += view.appMenuClicks().subscribe { onAppMenuClicked(it) }
        subscriptions += view.selectionClicks().subscribe { enterSelectionMode() }
        subscriptions += view.batchShareClicks().subscribe { onBatchShareClicked() }
        subscriptions += view.batchExtractClicks().subscribe { onBatchExtractClicked() }
        subscriptions += view.cancelSelectionClicks().subscribe { leaveSelectionMode() }
        subscriptions += view.searchTextChanged().subscribe { text -> filterApps(text) }
        subscriptions += view.searchCloseChanged().subscribe { filterApps("") }

        entities.takeIf { it != null }
            ?.run { applyAppEntities(this) }
            ?: loadAppItems()
    }

    private fun onPrefsClicked() {
        router?.showPrefsScreen()
    }

    private fun onInfoClicked() {
        router?.showInfoScreen()
    }

    private fun onAppMenuClicked(pair: Pair<Int, AppItem>) {
        val item = pair.second
        when (pair.first) {
            ACTION_RUN_APP -> router?.runApp(item.packageName)
                ?.let { result ->
                    if (!result) view?.showAppLaunchError()
                }
            ACTION_FIND_IN_GP -> {
                packageMayBeDeleted = item.packageName
                router?.openGooglePlay(item.packageName)
            }
            ACTION_SHARE_APP -> router?.requestPermissions(
                onGranted = { shareApp(item) },
                onDenied = { view?.showWritePermissionsRequiredError() }
            )
            ACTION_EXTRACT_APP -> router?.requestPermissions(
                onGranted = { extractApp(item) },
                onDenied = { view?.showWritePermissionsRequiredError() }
            )
            ACTION_SHOW_PERMISSIONS -> showPermissions(item)
            ACTION_SHOW_DETAILS -> {
                showAppDetails(item)
            }
            ACTION_REMOVE_APP -> {
                packageMayBeDeleted = item.packageName
                router?.runAppUninstall(item.packageName)
            }
        }
    }

    override fun detachView() {
        subscriptions.clear()
        this.view = null
    }

    override fun attachRouter(router: AppsPresenter.AppsRouter) {
        this.router = router
    }

    override fun detachRouter() {
        this.router = null
    }

    override fun invalidateAppsList() {
        loadAppItems()
    }

    private fun loadAppItems() {
        subscriptions += interactor.loadApps(
            systemApps = preferences.isShowSystemApps(),
            runnableOnly = preferences.isRunnableOnly(),
            sortOrder = preferences.getSortOrder()
        )
            .observeOn(schedulers.mainThread())
            .doOnSubscribe { view?.showProgress() }
            .doAfterTerminate { view?.showContent() }
            .subscribe({ entities ->
                applyAppEntities(entities)
            }, {
                view?.showAppsLoadingError()
            })
    }

    private fun filterApps(query: String) {
        entities.takeIf { it != null }
            ?.filter { it.matchesSearchQuery(query) }
            ?.run { bindAppEntities(this) }
            ?: loadAppItems()
    }

    private fun AppEntity.matchesSearchQuery(query: String): Boolean {
        return label.contains(query, ignoreCase = true) ||
                packageName.contains(query, ignoreCase = true)
    }

    private fun applyAppEntities(entities: List<AppEntity>) {
        this.entities = entities
        selectedPackages = selectedPackages.intersect(entities.map { it.packageName }.toSet())
        bindAppEntities(entities)
    }

    private fun bindAppEntities(entities: List<AppEntity>) {
        var id: Long = 0
        val items = entities
            .map {
                appEntityConverter.convert(
                    id++,
                    it,
                    selectable = selectionMode,
                    selected = selectedPackages.contains(it.packageName)
                )
            }
        val dataSource = ListDataSource(items)
        adapterPresenter.get().onDataSourceChanged(dataSource)
        view?.showSelectionMode(selectionMode, selectedPackages.size)
        view?.contentUpdated()
    }

    private fun shareApp(item: AppItem) {
        val entity = entities?.find { it.packageName == item.packageName } ?: return
        subscriptions += interactor.exportApp(entity)
            .observeOn(schedulers.mainThread())
            .doOnSubscribe { view?.showProgress() }
            .doAfterTerminate { view?.showContent() }
            .subscribe({ file ->
                router?.shareApk(file)
            }, {
                view?.showAppExportError()
            })
    }

    private fun extractApp(item: AppItem) {
        val entity = entities?.find { it.packageName == item.packageName } ?: return
        subscriptions += interactor.exportApp(entity)
            .observeOn(schedulers.mainThread())
            .doOnSubscribe { view?.showProgress() }
            .doAfterTerminate { view?.showContent() }
            .subscribe({
                view?.showExtractSuccess()
            }, {
                view?.showAppExportError()
            })
    }

    private fun showPermissions(item: AppItem) {
        subscriptions += interactor.loadApp(item.packageName)
            .observeOn(schedulers.mainThread())
            .subscribe({ entity ->
                entity.requestedPermissions?.let {
                    router?.showRequestedPermissions(entity.requestedPermissions)
                } ?: view?.showNoRequestedPermissionsMessage()
            }, {
                view?.showUnableToGetPermissionsError()
            })
    }

    private fun showAppDetails(item: AppItem) {
        entities?.find { it.packageName == item.packageName }
            ?.let { router?.showAppDetails(it) }
    }

    override fun saveState() = Bundle().apply {
        entities?.let { putParcelableArrayList(KEY_ENTITIES, ArrayList(it)) }
        packageMayBeDeleted?.let { putString(KEY_PACKAGE_MAY_BE_DELETED, packageMayBeDeleted) }
        putBoolean(KEY_SELECTION_MODE, selectionMode)
        putStringArrayList(KEY_SELECTED_PACKAGES, ArrayList(selectedPackages))
    }

    override fun onBackPressed() {
        if (selectionMode) {
            leaveSelectionMode()
        } else {
            router?.leaveScreen()
        }
    }

    override fun onResume() {
        packageMayBeDeleted?.let { pkg ->
            checkAppExist(pkg)
            packageMayBeDeleted = null
        }
    }

    private fun checkAppExist(packageName: String) {
        subscriptions += interactor.loadApp(packageName)
            .observeOn(schedulers.mainThread())
            .subscribe({}, {
                entities?.let { actual ->
                    val entity = actual.find { it.packageName == packageName }
                        ?: return@subscribe
                    applyAppEntities(actual - entity)
                }
            })
    }

    override fun onItemClick(item: Item) {
        when (item) {
            is AppItem -> {
                if (selectionMode) {
                    toggleSelection(item.packageName)
                } else {
                    view?.showAppMenu(item)
                }
            }
        }
    }

    override fun onItemLongClick(item: Item) {
        when (item) {
            is AppItem -> {
                if (!selectionMode) {
                    selectionMode = true
                    selectedPackages = selectedPackages + item.packageName
                    bindCurrentEntities()
                }
            }
        }
    }

    private fun enterSelectionMode() {
        selectionMode = true
        bindCurrentEntities()
    }

    private fun leaveSelectionMode() {
        selectionMode = false
        selectedPackages = emptySet()
        bindCurrentEntities()
    }

    private fun toggleSelection(packageName: String) {
        selectedPackages = if (selectedPackages.contains(packageName)) {
            selectedPackages - packageName
        } else {
            selectedPackages + packageName
        }
        bindCurrentEntities()
    }

    private fun bindCurrentEntities() {
        entities?.let { bindAppEntities(it) }
    }

    private fun getSelectedEntities(): List<AppEntity> {
        return entities.orEmpty().filter { selectedPackages.contains(it.packageName) }
    }

    private fun onBatchShareClicked() {
        val selectedEntities = getSelectedEntities()
        if (selectedEntities.isEmpty()) {
            view?.showNoAppsSelectedMessage()
            return
        }
        router?.requestPermissions(
            onGranted = { shareApps(selectedEntities) },
            onDenied = { view?.showWritePermissionsRequiredError() }
        )
    }

    private fun onBatchExtractClicked() {
        val selectedEntities = getSelectedEntities()
        if (selectedEntities.isEmpty()) {
            view?.showNoAppsSelectedMessage()
            return
        }
        router?.requestPermissions(
            onGranted = { extractApps(selectedEntities) },
            onDenied = { view?.showWritePermissionsRequiredError() }
        )
    }

    private fun shareApps(entities: List<AppEntity>) {
        subscriptions += io.reactivex.rxjava3.core.Observable.fromIterable(entities)
            .concatMap { interactor.exportApp(it) }
            .toList()
            .observeOn(schedulers.mainThread())
            .doOnSubscribe { view?.showProgress() }
            .doAfterTerminate { view?.showContent() }
            .subscribe({ uris ->
                leaveSelectionMode()
                router?.shareApks(uris)
            }, {
                view?.showAppExportError()
            })
    }

    private fun extractApps(entities: List<AppEntity>) {
        subscriptions += io.reactivex.rxjava3.core.Observable.fromIterable(entities)
            .concatMap { interactor.exportApp(it) }
            .toList()
            .observeOn(schedulers.mainThread())
            .doOnSubscribe { view?.showProgress() }
            .doAfterTerminate { view?.showContent() }
            .subscribe({ uris ->
                val count = uris.size
                leaveSelectionMode()
                view?.showBatchExtractSuccess(count)
            }, {
                view?.showAppExportError()
            })
    }

}

private const val KEY_ENTITIES = "entities"
private const val KEY_PACKAGE_MAY_BE_DELETED = "package_may_be_deleted"
private const val KEY_SELECTION_MODE = "selection_mode"
private const val KEY_SELECTED_PACKAGES = "selected_packages"
