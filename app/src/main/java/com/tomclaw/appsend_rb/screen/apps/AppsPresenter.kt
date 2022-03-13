package com.tomclaw.appsend_rb.screen.apps

import android.os.Bundle
import com.avito.konveyor.adapter.AdapterPresenter
import com.avito.konveyor.blueprint.Item
import com.avito.konveyor.data_source.ListDataSource
import com.tomclaw.appsend_rb.dto.AppEntity
import com.tomclaw.appsend_rb.screen.apps.adapter.ItemClickListener
import com.tomclaw.appsend_rb.screen.apps.adapter.app.AppItem
import com.tomclaw.appsend_rb.util.SchedulersFactory
import dagger.Lazy
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import java.io.File

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

        fun showAppDetails(packageName: String)

        fun runAppUninstall(packageName: String)

        fun shareApk(file: File)

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

    private var entities: List<AppEntity>? = state?.getParcelableArrayList(KEY_ENTITIES)

    private var packageMayBeDeleted: String? = state?.getString(KEY_PACKAGE_MAY_BE_DELETED)

    private val subscriptions = CompositeDisposable()

    override fun attachView(view: AppsView) {
        this.view = view
        subscriptions += view.refreshClicks().subscribe { loadAppItems() }
        subscriptions += view.prefsClicks().subscribe { onPrefsClicked() }
        subscriptions += view.infoClicks().subscribe { onInfoClicked() }
        subscriptions += view.appMenuClicks().subscribe { onAppMenuClicked(it) }
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
                packageMayBeDeleted = item.packageName
                router?.showAppDetails(item.packageName)
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
            }, {})
    }

    private fun filterApps(query: String) {
        entities.takeIf { it != null }
            ?.filter { it.label.contains(query, true) }
            ?.run { bindAppEntities(this) }
            ?: loadAppItems()
    }

    private fun applyAppEntities(entities: List<AppEntity>) {
        this.entities = entities
        bindAppEntities(entities)
    }

    private fun bindAppEntities(entities: List<AppEntity>) {
        var id: Long = 0
        val items = entities
            .map { appEntityConverter.convert(id++, it) }
        val dataSource = ListDataSource(items)
        adapterPresenter.get().onDataSourceChanged(dataSource)
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
                view?.showAppExportError(it.message.toString())
            })
    }

    private fun extractApp(item: AppItem) {
        val entity = entities?.find { it.packageName == item.packageName } ?: return
        subscriptions += interactor.exportApp(entity)
            .observeOn(schedulers.mainThread())
            .doOnSubscribe { view?.showProgress() }
            .doAfterTerminate { view?.showContent() }
            .subscribe({ file ->
                view?.showExtractSuccess(file.path)
            }, {
                view?.showAppExportError(it.message.toString())
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

    override fun saveState() = Bundle().apply {
        entities?.let { putParcelableArrayList(KEY_ENTITIES, ArrayList(it)) }
        packageMayBeDeleted?.let { putString(KEY_PACKAGE_MAY_BE_DELETED, packageMayBeDeleted) }
    }

    override fun onBackPressed() {
        router?.leaveScreen()
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
            is AppItem -> view?.showAppMenu(item)
        }
    }

}

private const val KEY_ENTITIES = "entities"
private const val KEY_PACKAGE_MAY_BE_DELETED = "package_may_be_deleted"
