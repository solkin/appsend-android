package com.tomclaw.appsend_rb.screen.apps

import android.net.Uri
import android.os.Build
import android.os.Bundle
import com.avito.konveyor.adapter.AdapterPresenter
import com.avito.konveyor.blueprint.Item
import com.avito.konveyor.data_source.ListDataSource
import com.tomclaw.appsend.util.Analytics
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

        fun showBackupLimitationsWarning(onConfirmed: () -> Unit)

        fun requestPermissions(onGranted: () -> Unit, onDenied: () -> Unit)

    }

}

class AppsPresenterImpl(
    private val interactor: AppsInteractor,
    private val adapterPresenter: Lazy<AdapterPresenter>,
    private val appEntityConverter: AppEntityConverter,
    private val preferences: PreferencesProvider,
    private val schedulers: SchedulersFactory,
    private val analytics: Analytics,
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
    private var screenTracked: Boolean = state?.getBoolean(KEY_SCREEN_TRACKED) ?: false
    private var searchActive: Boolean = state?.getBoolean(KEY_SEARCH_ACTIVE) ?: false
    private var selectedPackages: Set<String> = state?.getStringArrayList(KEY_SELECTED_PACKAGES)
        ?.toSet()
        ?: emptySet()

    private val subscriptions = CompositeDisposable()

    override fun attachView(view: AppsView) {
        this.view = view
        trackScreenOpen()
        subscriptions += view.refreshClicks().subscribe { onRefreshClicked() }
        subscriptions += view.prefsClicks().subscribe { onPrefsClicked() }
        subscriptions += view.infoClicks().subscribe { onInfoClicked() }
        subscriptions += view.appMenuClicks().subscribe { onAppMenuClicked(it) }
        subscriptions += view.selectionClicks().subscribe { enterSelectionMode() }
        subscriptions += view.batchShareClicks().subscribe { onBatchShareClicked() }
        subscriptions += view.batchExtractClicks().subscribe { onBatchExtractClicked() }
        subscriptions += view.cancelSelectionClicks().subscribe { leaveSelectionMode("toolbar") }
        subscriptions += view.searchTextChanged().subscribe { text -> onSearchTextChanged(text) }
        subscriptions += view.searchCloseChanged().subscribe { onSearchClosed() }

        entities.takeIf { it != null }
            ?.run { applyAppEntities(this) }
            ?: loadAppItems()
    }

    private fun onRefreshClicked() {
        analytics.trackEvent("apps_refresh")
        loadAppItems()
    }

    private fun onPrefsClicked() {
        analytics.trackEvent("navigation_opened", mapOf("screen" to "settings"))
        router?.showPrefsScreen()
    }

    private fun onInfoClicked() {
        analytics.trackEvent("navigation_opened", mapOf("screen" to "about"))
        router?.showInfoScreen()
    }

    private fun onAppMenuClicked(pair: Pair<Int, AppItem>) {
        val item = pair.second
        when (pair.first) {
            ACTION_RUN_APP -> {
                trackAppAction("run_app", item)
                router?.runApp(item.packageName)
                    ?.let { result ->
                        analytics.trackEvent(
                            "app_action_result",
                            mapOf("action" to "run_app", "result" to result.analyticsValue())
                        )
                        if (!result) view?.showAppLaunchError()
                    }
            }
            ACTION_FIND_IN_GP -> {
                trackAppAction("open_google_play", item)
                packageMayBeDeleted = item.packageName
                router?.openGooglePlay(item.packageName)
            }
            ACTION_SHARE_APP -> {
                trackAppAction("share_apk", item)
                shareApp(item)
            }
            ACTION_EXTRACT_APP -> {
                trackAppAction("extract_apk", item)
                extractApp(item)
            }
            ACTION_SHOW_PERMISSIONS -> {
                trackAppAction("open_permissions", item)
                showPermissions(item)
            }
            ACTION_SHOW_DETAILS -> {
                trackAppAction("open_details", item)
                showAppDetails(item)
            }
            ACTION_REMOVE_APP -> {
                trackAppAction("uninstall", item)
                packageMayBeDeleted = item.packageName
                router?.runAppUninstall(item.packageName)
            }
        }
    }

    private fun trackScreenOpen() {
        if (screenTracked) {
            return
        }
        screenTracked = true
        analytics.trackEvent(
            "screen_open",
            mapOf(
                "screen" to "apps",
                "show_system" to preferences.isShowSystemApps().analyticsValue(),
                "runnable_only" to preferences.isRunnableOnly().analyticsValue(),
                "sort_order" to preferences.getSortOrder().analyticsValue()
            )
        )
    }

    private fun trackAppAction(action: String, item: AppItem) {
        analytics.trackEvent(
            "app_action_selected",
            mapOf("action" to action) + item.analyticsTags()
        )
    }

    private fun AppItem.analyticsTags(): Map<String, String> {
        val entity = entities?.find { it.packageName == packageName }
        return mapOf(
            "system" to (entity?.system ?: false).analyticsValue(),
            "split" to (entity?.split ?: false).analyticsValue(),
            "has_permissions" to (!entity?.requestedPermissions.isNullOrEmpty()).analyticsValue()
        )
    }

    private fun AppEntity.analyticsTags(): Map<String, String> {
        return mapOf(
            "system" to system.analyticsValue(),
            "split" to split.analyticsValue(),
            "has_permissions" to (!requestedPermissions.isNullOrEmpty()).analyticsValue()
        )
    }

    private fun List<AppEntity>.analyticsFields(): Map<String, Double> {
        return mapOf(
            "count" to size.toDouble(),
            "system_count" to count { it.system }.toDouble(),
            "split_count" to count { it.split }.toDouble()
        )
    }

    private fun Int.analyticsValue(): String {
        return when (this) {
            NAME_ASCENDING -> "name_ascending"
            NAME_DESCENDING -> "name_descending"
            APP_SIZE -> "app_size"
            INSTALL_TIME -> "install_time"
            UPDATE_TIME -> "update_time"
            else -> "unknown"
        }
    }

    private fun Boolean.analyticsValue(): String = toString()

    private fun Throwable.trackNonFatal(event: String, tags: Map<String, String> = emptyMap()) {
        analytics.trackException(this, mapOf("event" to event) + tags)
    }

    private fun String.searchLengthBucket(): String {
        return when (length) {
            in 0..2 -> "short"
            in 3..10 -> "medium"
            else -> "long"
        }
    }

    private fun onSearchTextChanged(query: String) {
        if (query.isNotBlank() && !searchActive) {
            searchActive = true
            analytics.trackEvent(
                "apps_search_used",
                mapOf("query_length" to query.searchLengthBucket())
            )
        }
        filterApps(query)
    }

    private fun onSearchClosed() {
        if (searchActive) {
            searchActive = false
            analytics.trackEvent("apps_search_closed")
        }
        filterApps("")
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
                analytics.trackEvent(
                    "apps_loaded",
                    mapOf(
                        "show_system" to preferences.isShowSystemApps().analyticsValue(),
                        "runnable_only" to preferences.isRunnableOnly().analyticsValue(),
                        "sort_order" to preferences.getSortOrder().analyticsValue()
                    ),
                    entities.analyticsFields()
                )
                applyAppEntities(entities)
            }, { error ->
                error.trackNonFatal("apps_load_failed")
                analytics.trackEvent("apps_load_failed")
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
        confirmBackupLimitationsIfNeeded(listOf(entity), "share_apk") {
            router?.requestPermissions(
                onGranted = { shareApp(entity) },
                onDenied = {
                    analytics.trackEvent("app_action_denied", mapOf("action" to "share_apk", "reason" to "write_permission"))
                    view?.showWritePermissionsRequiredError()
                }
            )
        }
    }

    private fun extractApp(item: AppItem) {
        val entity = entities?.find { it.packageName == item.packageName } ?: return
        confirmBackupLimitationsIfNeeded(listOf(entity), "extract_apk") {
            router?.requestPermissions(
                onGranted = { extractApp(entity) },
                onDenied = {
                    analytics.trackEvent("app_action_denied", mapOf("action" to "extract_apk", "reason" to "write_permission"))
                    view?.showWritePermissionsRequiredError()
                }
            )
        }
    }

    private fun shareApp(entity: AppEntity) {
        analytics.trackEvent("apk_export_started", mapOf("action" to "share_apk") + entity.analyticsTags())
        subscriptions += interactor.exportApp(entity)
            .observeOn(schedulers.mainThread())
            .doOnSubscribe { view?.showProgress() }
            .doAfterTerminate { view?.showContent() }
            .subscribe({ file ->
                analytics.trackEvent("apk_export_succeeded", mapOf("action" to "share_apk") + entity.analyticsTags())
                router?.shareApk(file)
            }, { error ->
                error.trackNonFatal("apk_export_failed", mapOf("action" to "share_apk") + entity.analyticsTags())
                analytics.trackEvent("apk_export_failed", mapOf("action" to "share_apk") + entity.analyticsTags())
                view?.showAppExportError()
            })
    }

    private fun extractApp(entity: AppEntity) {
        analytics.trackEvent("apk_export_started", mapOf("action" to "extract_apk") + entity.analyticsTags())
        subscriptions += interactor.exportApp(entity)
            .observeOn(schedulers.mainThread())
            .doOnSubscribe { view?.showProgress() }
            .doAfterTerminate { view?.showContent() }
            .subscribe({
                analytics.trackEvent("apk_export_succeeded", mapOf("action" to "extract_apk") + entity.analyticsTags())
                view?.showExtractSuccess()
            }, { error ->
                error.trackNonFatal("apk_export_failed", mapOf("action" to "extract_apk") + entity.analyticsTags())
                analytics.trackEvent("apk_export_failed", mapOf("action" to "extract_apk") + entity.analyticsTags())
                view?.showAppExportError()
            })
    }

    private fun showPermissions(item: AppItem) {
        subscriptions += interactor.loadApp(item.packageName)
            .observeOn(schedulers.mainThread())
            .subscribe({ entity ->
                entity.requestedPermissions?.let {
                    analytics.trackEvent(
                        "permissions_opened",
                        entity.analyticsTags(),
                        mapOf("permissions_count" to entity.requestedPermissions.size.toDouble())
                    )
                    router?.showRequestedPermissions(entity.requestedPermissions)
                } ?: run {
                    analytics.trackEvent("permissions_empty", entity.analyticsTags())
                    view?.showNoRequestedPermissionsMessage()
                }
            }, { error ->
                error.trackNonFatal("permissions_load_failed")
                analytics.trackEvent("permissions_load_failed")
                view?.showUnableToGetPermissionsError()
            })
    }

    private fun showAppDetails(item: AppItem) {
        entities?.find { it.packageName == item.packageName }
            ?.let {
                analytics.trackEvent("app_details_opened", it.analyticsTags())
                router?.showAppDetails(it)
            }
    }

    override fun saveState() = Bundle().apply {
        entities?.let { putParcelableArrayList(KEY_ENTITIES, ArrayList(it)) }
        packageMayBeDeleted?.let { putString(KEY_PACKAGE_MAY_BE_DELETED, packageMayBeDeleted) }
        putBoolean(KEY_SELECTION_MODE, selectionMode)
        putBoolean(KEY_SCREEN_TRACKED, screenTracked)
        putBoolean(KEY_SEARCH_ACTIVE, searchActive)
        putStringArrayList(KEY_SELECTED_PACKAGES, ArrayList(selectedPackages))
    }

    override fun onBackPressed() {
        if (selectionMode) {
            leaveSelectionMode("back")
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
                    analytics.trackEvent("app_uninstalled")
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
                    analytics.trackEvent("app_menu_opened", item.analyticsTags())
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
                    analytics.trackEvent(
                        "selection_mode_entered",
                        mapOf("source" to "long_click"),
                        mapOf("selected_count" to selectedPackages.size.toDouble())
                    )
                    bindCurrentEntities()
                }
            }
        }
    }

    private fun enterSelectionMode() {
        selectionMode = true
        analytics.trackEvent(
            "selection_mode_entered",
            mapOf("source" to "toolbar"),
            mapOf("selected_count" to selectedPackages.size.toDouble())
        )
        bindCurrentEntities()
    }

    private fun leaveSelectionMode(reason: String) {
        analytics.trackEvent(
            "selection_mode_exited",
            mapOf("reason" to reason),
            mapOf("selected_count" to selectedPackages.size.toDouble())
        )
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
        analytics.trackEvent(
            "selection_changed",
            emptyMap(),
            mapOf("selected_count" to selectedPackages.size.toDouble())
        )
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
            analytics.trackEvent("batch_action_empty", mapOf("action" to "share_apks"))
            view?.showNoAppsSelectedMessage()
            return
        }
        confirmBackupLimitationsIfNeeded(selectedEntities, "share_apks") {
            router?.requestPermissions(
                onGranted = { shareApps(selectedEntities) },
                onDenied = {
                    analytics.trackEvent(
                        "batch_action_denied",
                        mapOf("action" to "share_apks", "reason" to "write_permission"),
                        selectedEntities.analyticsFields()
                    )
                    view?.showWritePermissionsRequiredError()
                }
            )
        }
    }

    private fun onBatchExtractClicked() {
        val selectedEntities = getSelectedEntities()
        if (selectedEntities.isEmpty()) {
            analytics.trackEvent("batch_action_empty", mapOf("action" to "extract_apks"))
            view?.showNoAppsSelectedMessage()
            return
        }
        confirmBackupLimitationsIfNeeded(selectedEntities, "extract_apks") {
            router?.requestPermissions(
                onGranted = { extractApps(selectedEntities) },
                onDenied = {
                    analytics.trackEvent(
                        "batch_action_denied",
                        mapOf("action" to "extract_apks", "reason" to "write_permission"),
                        selectedEntities.analyticsFields()
                    )
                    view?.showWritePermissionsRequiredError()
                }
            )
        }
    }

    private fun confirmBackupLimitationsIfNeeded(
        entities: List<AppEntity>,
        action: String,
        onConfirmed: () -> Unit
    ) {
        if (entities.hasBackupLimitations()) {
            analytics.trackEvent(
                "backup_limitations_shown",
                mapOf("action" to action),
                entities.analyticsFields()
            )
            router?.showBackupLimitationsWarning {
                analytics.trackEvent(
                    "backup_limitations_confirmed",
                    mapOf("action" to action),
                    entities.analyticsFields()
                )
                onConfirmed()
            }
        } else {
            onConfirmed()
        }
    }

    private fun List<AppEntity>.hasBackupLimitations(): Boolean {
        return any { it.system || it.split }
    }

    private fun shareApps(entities: List<AppEntity>) {
        analytics.trackEvent("batch_export_started", mapOf("action" to "share_apks"), entities.analyticsFields())
        subscriptions += io.reactivex.rxjava3.core.Observable.fromIterable(entities)
            .concatMap { interactor.exportApp(it) }
            .toList()
            .observeOn(schedulers.mainThread())
            .doOnSubscribe { view?.showProgress() }
            .doAfterTerminate { view?.showContent() }
            .subscribe({ uris ->
                analytics.trackEvent(
                    "batch_export_succeeded",
                    mapOf("action" to "share_apks"),
                    entities.analyticsFields() + mapOf("exported_count" to uris.size.toDouble())
                )
                leaveSelectionMode("completed")
                router?.shareApks(uris)
            }, { error ->
                error.trackNonFatal("batch_export_failed", mapOf("action" to "share_apks"))
                analytics.trackEvent("batch_export_failed", mapOf("action" to "share_apks"), entities.analyticsFields())
                view?.showAppExportError()
            })
    }

    private fun extractApps(entities: List<AppEntity>) {
        analytics.trackEvent("batch_export_started", mapOf("action" to "extract_apks"), entities.analyticsFields())
        subscriptions += io.reactivex.rxjava3.core.Observable.fromIterable(entities)
            .concatMap { interactor.exportApp(it) }
            .toList()
            .observeOn(schedulers.mainThread())
            .doOnSubscribe { view?.showProgress() }
            .doAfterTerminate { view?.showContent() }
            .subscribe({ uris ->
                val count = uris.size
                analytics.trackEvent(
                    "batch_export_succeeded",
                    mapOf("action" to "extract_apks"),
                    entities.analyticsFields() + mapOf("exported_count" to count.toDouble())
                )
                leaveSelectionMode("completed")
                view?.showBatchExtractSuccess(count)
            }, { error ->
                error.trackNonFatal("batch_export_failed", mapOf("action" to "extract_apks"))
                analytics.trackEvent("batch_export_failed", mapOf("action" to "extract_apks"), entities.analyticsFields())
                view?.showAppExportError()
            })
    }

}

private const val KEY_ENTITIES = "entities"
private const val KEY_PACKAGE_MAY_BE_DELETED = "package_may_be_deleted"
private const val KEY_SELECTION_MODE = "selection_mode"
private const val KEY_SCREEN_TRACKED = "screen_tracked"
private const val KEY_SEARCH_ACTIVE = "search_active"
private const val KEY_SELECTED_PACKAGES = "selected_packages"
