package com.tomclaw.appsend_rb.screen.exportoptions

import com.tomclaw.appsend_rb.domain.AppPreferences
import com.tomclaw.appsend_rb.domain.ExportAction
import com.tomclaw.appsend_rb.domain.ExportFormat
import com.tomclaw.appsend_rb.domain.ExportManager
import com.tomclaw.appsend_rb.domain.ExportStorage
import com.tomclaw.appsend_rb.domain.InstalledApp
import com.tomclaw.appsend_rb.domain.InstalledAppsRepository
import com.tomclaw.appsend_rb.domain.StorageAccess
import com.tomclaw.appsend_rb.domain.ValueFormatter
import com.tomclaw.appsend_rb.domain.exportFileName
import com.tomclaw.appsend_rb.domain.SchedulersFactory
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign

enum class ExportNotice { NONE, BUNDLE_INSTALL, BASE_ONLY, SYSTEM_APP }

data class ExportOptionsViewModel(
    val title: String,
    val subtitle: String,
    val showFormats: Boolean,
    val format: ExportFormat,
    val bundleSize: String,
    val baseSize: String,
    val notice: ExportNotice,
    val folder: String,
    val fileName: String?,
    val template: String,
    val ready: Boolean,
)

interface ExportOptionsInteractor {

    fun apps(): Single<List<InstalledApp>>

    fun format(): ExportFormat

    fun template(): String

    fun saveTemplate(template: String)

    fun folder(): String

    fun saveFolder(uri: String)

    fun needsStorageAccess(): Boolean

    fun start(apps: List<InstalledApp>, action: ExportAction, format: ExportFormat)

}

class ExportOptionsInteractorImpl(
    private val packages: List<String>,
    private val repository: InstalledAppsRepository,
    private val preferences: AppPreferences,
    private val storage: ExportStorage,
    private val exports: ExportManager,
) : ExportOptionsInteractor {

    override fun apps(): Single<List<InstalledApp>> = repository.apps()
        .firstOrError()
        .map { apps -> apps.filter { it.packageName in packages } }

    override fun format(): ExportFormat = preferences.exportFormat

    override fun template(): String = preferences.fileNameTemplate

    override fun saveTemplate(template: String) {
        preferences.fileNameTemplate = template
    }

    override fun folder(): String = storage.location()

    override fun saveFolder(uri: String) {
        preferences.exportFolder = uri
    }

    override fun needsStorageAccess(): Boolean = storage.requiresPermission()

    override fun start(apps: List<InstalledApp>, action: ExportAction, format: ExportFormat) {
        exports.start(apps, action, format)
    }

}

interface ExportOptionsResources {

    fun appsCount(count: Int): String

    fun filesCount(count: Int): String

}

interface ExportOptionsRouter {

    fun pickFolder()

    fun requestStorageAccess()

    fun openOwnSettings()

    fun openExportProgress()

    fun leave()

}

interface ExportOptionsPresenter {

    fun attachView(view: ExportOptionsView)

    fun detachView()

    fun attachRouter(router: ExportOptionsRouter)

    fun detachRouter()

    fun onFolderPicked(uri: String)

    fun onStorageAccess(access: StorageAccess)

    fun saveState(): ExportOptionsState

}

class ExportOptionsPresenterImpl(
    private val interactor: ExportOptionsInteractor,
    private val resources: ExportOptionsResources,
    private val formatter: ValueFormatter,
    private val schedulers: SchedulersFactory,
    state: ExportOptionsState? = null,
) : ExportOptionsPresenter {

    private var view: ExportOptionsView? = null
    private var router: ExportOptionsRouter? = null
    private val subscriptions = CompositeDisposable()
    private var apps: List<InstalledApp> = emptyList()
    private var format: ExportFormat = state?.format ?: interactor.format()
    private var pendingAction: ExportAction? = state?.pendingAction
    private var leavePending = false
    private var launchWhenLoaded = false

    override fun attachView(view: ExportOptionsView) {
        this.view = view
        subscriptions += view.formatChanges().subscribe {
            format = it
            render()
        }
        subscriptions += view.folderClicks().subscribe { router?.pickFolder() }
        subscriptions += view.ownSettingsClicks().subscribe { router?.openOwnSettings() }
        subscriptions += view.templateChanges().subscribe {
            interactor.saveTemplate(it)
            render()
        }
        subscriptions += view.extractClicks().subscribe { start(ExportAction.EXTRACT) }
        subscriptions += view.shareClicks().subscribe { start(ExportAction.SHARE) }
        render()
        subscriptions += interactor.apps()
            .observeOn(schedulers.mainThread())
            .subscribe({
                apps = it
                render()
                if (launchWhenLoaded) resumePendingAction()
            }, { leave() })
    }

    override fun detachView() {
        subscriptions.clear()
        view = null
    }

    override fun attachRouter(router: ExportOptionsRouter) {
        this.router = router
        if (leavePending) router.leave()
    }

    override fun detachRouter() {
        router = null
    }

    override fun onFolderPicked(uri: String) {
        interactor.saveFolder(uri)
        render()
    }

    override fun saveState() = ExportOptionsState(format, pendingAction)

    private fun start(action: ExportAction) {
        if (action == ExportAction.EXTRACT && interactor.needsStorageAccess()) {
            pendingAction = action
            router?.requestStorageAccess()
        } else {
            launch(action)
        }
    }

    override fun onStorageAccess(access: StorageAccess) {
        if (pendingAction == null) return
        if (access != StorageAccess.GRANTED) {
            pendingAction = null
            view?.showStorageAccessRequired(canOpenSettings = access == StorageAccess.PERMANENTLY_DENIED)
        } else if (apps.isEmpty()) {
            // After a re-creation the answer can get here before the list does.
            launchWhenLoaded = true
        } else {
            resumePendingAction()
        }
    }

    private fun resumePendingAction() {
        launchWhenLoaded = false
        val action = pendingAction ?: return
        pendingAction = null
        launch(action)
    }

    private fun launch(action: ExportAction) {
        interactor.start(apps, action, format)
        router?.openExportProgress()
        leave()
    }

    private fun leave() {
        leavePending = true
        router?.leave()
    }

    private fun render() {
        val single = apps.singleOrNull()
        val anySplit = apps.any { it.split }
        val total = apps.sumOf { it.totalSize }
        val base = apps.sumOf { it.baseSize }
        view?.render(
            ExportOptionsViewModel(
                title = single?.label ?: resources.appsCount(apps.size),
                subtitle = single?.let {
                    "${it.versionName} · ${resources.filesCount(it.splitCount + 1)}"
                } ?: formatter.bytes(total),
                showFormats = anySplit,
                format = format,
                bundleSize = formatter.bytes(total),
                baseSize = formatter.bytes(base),
                notice = when {
                    apps.any { it.system } -> ExportNotice.SYSTEM_APP
                    anySplit && format == ExportFormat.BASE -> ExportNotice.BASE_ONLY
                    anySplit -> ExportNotice.BUNDLE_INSTALL
                    else -> ExportNotice.NONE
                },
                folder = interactor.folder(),
                fileName = single?.let { exportFileName(interactor.template(), it, format) },
                template = interactor.template(),
                ready = apps.isNotEmpty(),
            )
        )
    }

}
