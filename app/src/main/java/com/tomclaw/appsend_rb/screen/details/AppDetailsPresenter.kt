package com.tomclaw.appsend_rb.screen.details

import com.tomclaw.appsend_rb.domain.AppDetails
import com.tomclaw.appsend_rb.domain.ExportFormat
import com.tomclaw.appsend_rb.domain.Analytics
import com.tomclaw.appsend_rb.domain.SchedulersFactory
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign

interface AppDetailsPresenter {

    fun attachView(view: AppDetailsView)

    fun detachView()

    fun attachRouter(router: AppDetailsRouter)

    fun detachRouter()

    fun destroy()

}

interface AppDetailsRouter {

    fun launch(packageName: String): Boolean

    fun openExportOptions(packageName: String)

    fun openExportProgress()

    fun openPermissions(packageName: String)

    fun openStorePage(packageName: String)

    fun openSystemDetails(packageName: String)

    fun uninstall(packageName: String)

    fun leave()

}

class AppDetailsPresenterImpl(
    private val packageName: String,
    private val interactor: AppDetailsInteractor,
    private val resources: AppDetailsResources,
    private val schedulers: SchedulersFactory,
    private val analytics: Analytics,
) : AppDetailsPresenter {

    private var view: AppDetailsView? = null
    private var router: AppDetailsRouter? = null
    private val viewSubscriptions = CompositeDisposable()
    private val dataSubscriptions = CompositeDisposable()
    private var details: AppDetails? = null
    private var knownInRepository = false
    private var leavePending = false

    init {
        analytics.trackEvent("screen_open", mapOf("screen" to "app_details"))
        dataSubscriptions += interactor.changes(packageName)
            .observeOn(schedulers.mainThread())
            .subscribe { matches ->
                if (matches.isEmpty() && knownInRepository) {
                    leave()
                } else {
                    knownInRepository = matches.isNotEmpty()
                    load()
                }
            }
        load()
    }

    override fun attachView(view: AppDetailsView) {
        this.view = view
        viewSubscriptions += view.backClicks().subscribe { leave() }
        viewSubscriptions += view.openClicks().subscribe { onOpen() }
        viewSubscriptions += view.extractClicks().subscribe { router?.openExportOptions(packageName) }
        viewSubscriptions += view.shareClicks().subscribe { onShare() }
        viewSubscriptions += view.copyPackageClicks().subscribe { onCopyPackage() }
        viewSubscriptions += view.permissionsClicks().subscribe { router?.openPermissions(packageName) }
        viewSubscriptions += view.storeClicks().subscribe { router?.openStorePage(packageName) }
        viewSubscriptions += view.systemDetailsClicks().subscribe { router?.openSystemDetails(packageName) }
        viewSubscriptions += view.removeClicks().subscribe { router?.uninstall(packageName) }
        viewSubscriptions += view.backupWarningConfirmations().subscribe { share() }
        details?.let(::render)
    }

    override fun detachView() {
        viewSubscriptions.clear()
        view = null
    }

    override fun attachRouter(router: AppDetailsRouter) {
        this.router = router
        if (leavePending) router.leave()
    }

    override fun detachRouter() {
        router = null
    }

    override fun destroy() {
        dataSubscriptions.clear()
    }

    /** The app may be removed from the system screen on top: leave once back in front. */
    private fun leave() {
        leavePending = true
        router?.leave()
    }

    private fun load() {
        dataSubscriptions += interactor.details(packageName)
            .observeOn(schedulers.mainThread())
            .subscribe({
                details = it
                render(it)
            }, { error ->
                analytics.trackException(error, mapOf("event" to "app_details_failed"))
                leave()
            })
    }

    private fun render(details: AppDetails) {
        val app = details.app
        view?.render(
            AppDetailsViewModel(
                packageName = app.packageName,
                label = app.label,
                canOpen = app.launchable,
                rows = listOfNotNull(
                    InfoRow(resources.versionLabel(), resources.version(app)),
                    InfoRow(resources.sizeLabel(), resources.size(app)),
                    InfoRow(resources.statusLabel(), resources.status(app)),
                    details.installerLabel?.let { InfoRow(resources.installerLabel(), it) },
                    InfoRow(resources.installedLabel(), resources.date(app.firstInstallTime)),
                    InfoRow(resources.updatedLabel(), resources.date(app.lastUpdateTime)),
                    InfoRow(resources.sdkLabel(), resources.sdk(details.minSdk, details.targetSdk)),
                    details.signatureSha256?.let { InfoRow(resources.signatureLabel(), it) },
                    InfoRow(resources.pathLabel(), details.basePath),
                ),
                permissions = resources.permissionsSummary(
                    details.permissions.size,
                    details.permissions.count { it.dangerous },
                ),
                hasPermissions = details.permissions.isNotEmpty(),
            )
        )
    }

    private fun onOpen() {
        if (router?.launch(packageName) == false) view?.showNotRunnable()
    }

    private fun onCopyPackage() {
        interactor.copyPackageName(packageName)
        view?.showPackageCopied()
    }

    private fun onShare() {
        val app = details?.app ?: return
        val limited = app.system || (app.split && interactor.exportFormat() == ExportFormat.BASE)
        if (limited) view?.showBackupWarning() else share()
    }

    private fun share() {
        val app = details?.app ?: return
        analytics.trackEvent("app_action_selected", mapOf("action" to "share", "split" to app.split.toString()))
        interactor.share(app)
        router?.openExportProgress()
    }

}
