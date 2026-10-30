package com.tomclaw.appsend_rb.screen.install

import com.tomclaw.appsend_rb.domain.InstallConfirmation
import com.tomclaw.appsend_rb.domain.InstallEvent
import com.tomclaw.appsend_rb.domain.InstallFailure
import com.tomclaw.appsend_rb.domain.InstallIssue
import com.tomclaw.appsend_rb.domain.InstalledVersion
import com.tomclaw.appsend_rb.domain.PackageCandidate
import com.tomclaw.appsend_rb.domain.PackageInstallerGateway
import com.tomclaw.appsend_rb.domain.ValueFormatter
import com.tomclaw.appsend_rb.domain.evaluateInstall
import com.tomclaw.appsend_rb.domain.Analytics
import com.tomclaw.appsend_rb.domain.SchedulersFactory
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.disposables.Disposable
import io.reactivex.rxjava3.kotlin.plusAssign

sealed interface InstallViewModel {

    data object Loading : InstallViewModel

    data object Unreadable : InstallViewModel

    data class Ready(
        val packageName: String?,
        val label: String,
        val update: Boolean,
        val fromVersion: String?,
        val toVersion: String,
        val size: String,
        val splitCount: Int,
        val signatureChecked: Boolean,
        val minSdk: Int,
        val needsPermission: Boolean,
    ) : InstallViewModel

    data class Blocked(
        val packageName: String?,
        val label: String,
        val fileVersion: String,
        val installedVersion: String?,
        val issue: InstallIssue,
        val canReplace: Boolean,
    ) : InstallViewModel

    data class Installing(
        val packageName: String?,
        val label: String,
        val percent: Int,
        val awaitingConfirmation: Boolean,
    ) : InstallViewModel

    data class Installed(
        val packageName: String?,
        val label: String,
        val version: String,
    ) : InstallViewModel

    data class Failed(
        val packageName: String?,
        val label: String,
        val failure: InstallFailure,
    ) : InstallViewModel

}

interface InstallInteractor {

    fun inspect(): Single<PackageCandidate>

    fun ongoing(): PackageCandidate?

    fun installedVersion(packageName: String): InstalledVersion?

    fun issue(candidate: PackageCandidate, installed: InstalledVersion?): InstallIssue?

    fun canInstall(): Boolean

    fun install(candidate: PackageCandidate): Observable<InstallEvent>

    fun confirmationShown()

    fun cancel()

}

class InstallInteractorImpl(
    private val uri: String,
    private val gateway: PackageInstallerGateway,
    private val sdk: Int,
    private val supportedAbis: List<String>,
) : InstallInteractor {

    override fun inspect(): Single<PackageCandidate> = gateway.inspect(uri)

    override fun ongoing(): PackageCandidate? = gateway.ongoing(uri)

    override fun installedVersion(packageName: String): InstalledVersion? =
        gateway.installedVersion(packageName)

    override fun issue(candidate: PackageCandidate, installed: InstalledVersion?): InstallIssue? =
        evaluateInstall(candidate, installed, sdk, supportedAbis)

    override fun canInstall(): Boolean = gateway.canInstall()

    override fun install(candidate: PackageCandidate): Observable<InstallEvent> =
        gateway.install(candidate)

    override fun confirmationShown() = gateway.confirmationShown(uri)

    override fun cancel() = gateway.cancel(uri)

}

interface InstallRouter {

    fun requestInstallPermission()

    fun confirmInstall(confirmation: InstallConfirmation)

    fun uninstall(packageName: String)

    fun launch(packageName: String)

    fun leave()

}

interface InstallPresenter {

    fun attachView(view: InstallView)

    fun detachView()

    fun attachRouter(router: InstallRouter)

    fun detachRouter()

    fun onResume()

    fun saveState(): InstallState

    fun destroy()

}

class InstallPresenterImpl(
    private val interactor: InstallInteractor,
    private val formatter: ValueFormatter,
    private val schedulers: SchedulersFactory,
    private val analytics: Analytics,
    state: InstallState? = null,
) : InstallPresenter {

    private var view: InstallView? = null
    private var router: InstallRouter? = null
    private val viewSubscriptions = CompositeDisposable()
    private val dataSubscriptions = CompositeDisposable()
    private var installSubscription: Disposable? = null

    private var model: InstallViewModel = InstallViewModel.Loading
    private var candidate: PackageCandidate? = null
    private var pendingConfirmation: InstallConfirmation? = null
    private var restored = state

    init {
        analytics.trackEvent("screen_open", mapOf("screen" to "install"))
        val ongoing = interactor.ongoing()
        if (ongoing != null) {
            candidate = ongoing
            show(installingModel(ongoing, percent = 0, awaiting = false))
            followInstall(ongoing)
        } else {
            inspect()
        }
    }

    override fun attachView(view: InstallView) {
        this.view = view
        viewSubscriptions += view.installClicks().subscribe { onInstallClicked() }
        viewSubscriptions += view.replaceClicks().subscribe {
            candidate?.let { router?.uninstall(it.packageName) }
        }
        viewSubscriptions += view.openClicks().subscribe {
            candidate?.let { router?.launch(it.packageName) }
            router?.leave()
        }
        viewSubscriptions += view.dismissClicks().subscribe { onDismiss() }
        view.render(model)
    }

    override fun detachView() {
        viewSubscriptions.clear()
        view = null
    }

    override fun attachRouter(router: InstallRouter) {
        this.router = router
        launchPendingConfirmation()
    }

    override fun detachRouter() {
        router = null
    }

    /** The user may be back from settings or from uninstalling: re-check what stands now. */
    override fun onResume() {
        val current = candidate ?: return
        if (model is InstallViewModel.Ready || model is InstallViewModel.Blocked) evaluate(current)
    }

    override fun saveState() = InstallState(
        installed = model is InstallViewModel.Installed,
        failure = (model as? InstallViewModel.Failed)?.failure,
    )

    override fun destroy() {
        dataSubscriptions.clear()
        installSubscription?.dispose()
    }

    private fun inspect() {
        dataSubscriptions += interactor.inspect()
            .observeOn(schedulers.mainThread())
            .subscribe({
                candidate = it
                val outcome = restored
                restored = null
                when {
                    outcome?.installed == true -> show(InstallViewModel.Installed(it.packageName, it.label, it.versionName))
                    outcome?.failure != null -> show(InstallViewModel.Failed(it.packageName, it.label, outcome.failure))
                    else -> evaluate(it)
                }
            }, { error ->
                analytics.trackException(error, mapOf("event" to "install_inspect_failed"))
                show(InstallViewModel.Unreadable)
            })
    }

    private fun evaluate(candidate: PackageCandidate) {
        val installed = interactor.installedVersion(candidate.packageName)
        val issue = interactor.issue(candidate, installed)
        show(
            if (issue != null) {
                InstallViewModel.Blocked(
                    packageName = candidate.packageName.takeIf { installed != null },
                    label = candidate.label,
                    fileVersion = candidate.versionName,
                    installedVersion = installed?.versionName,
                    issue = issue,
                    canReplace = issue == InstallIssue.SIGNATURE_MISMATCH || issue == InstallIssue.DOWNGRADE,
                )
            } else {
                InstallViewModel.Ready(
                    packageName = candidate.packageName.takeIf { installed != null },
                    label = candidate.label,
                    update = installed != null,
                    fromVersion = installed?.versionName,
                    toVersion = candidate.versionName,
                    size = formatter.bytes(candidate.totalBytes),
                    splitCount = candidate.splitCount,
                    signatureChecked = !installed?.signatures.isNullOrEmpty() && candidate.signatures.isNotEmpty(),
                    minSdk = candidate.minSdk,
                    needsPermission = !interactor.canInstall(),
                )
            }
        )
    }

    private fun onInstallClicked() {
        val current = candidate ?: return
        if (!interactor.canInstall()) {
            router?.requestInstallPermission()
            return
        }
        analytics.trackEvent("install_started", mapOf("splits" to current.splitCount.toString()))
        show(installingModel(current, percent = 0, awaiting = false))
        followInstall(current)
    }

    private fun followInstall(current: PackageCandidate) {
        installSubscription?.dispose()
        installSubscription = interactor.install(current)
            .observeOn(schedulers.mainThread())
            .subscribe { onInstallEvent(current, it) }
    }

    private fun onInstallEvent(current: PackageCandidate, event: InstallEvent) {
        when (event) {
            is InstallEvent.Writing -> show(
                installingModel(
                    current,
                    percent = if (event.totalBytes > 0) (event.writtenBytes * 100 / event.totalBytes).toInt() else 0,
                    awaiting = false,
                )
            )
            is InstallEvent.ConfirmationRequired -> {
                show(installingModel(current, percent = 100, awaiting = true))
                pendingConfirmation = event.confirmation
                launchPendingConfirmation()
            }
            InstallEvent.AwaitingConfirmation -> {
                pendingConfirmation = null
                show(installingModel(current, percent = 100, awaiting = true))
            }
            InstallEvent.Installed -> {
                pendingConfirmation = null
                interactor.cancel()
                analytics.trackEvent("install_succeeded")
                show(InstallViewModel.Installed(current.packageName, current.label, current.versionName))
            }
            is InstallEvent.Failed -> onFinished(current, event.failure)
        }
    }

    private fun onFinished(current: PackageCandidate, failure: InstallFailure) {
        pendingConfirmation = null
        interactor.cancel()
        analytics.trackEvent("install_failed", mapOf("reason" to failure.name))
        if (failure == InstallFailure.ABORTED) {
            evaluate(current)
        } else {
            show(InstallViewModel.Failed(current.packageName, current.label, failure))
        }
    }

    private fun installingModel(current: PackageCandidate, percent: Int, awaiting: Boolean) =
        InstallViewModel.Installing(current.packageName, current.label, percent.coerceIn(0, 100), awaiting)

    /** The system's dialog can only start from a visible screen, so it may have to wait. */
    private fun launchPendingConfirmation() {
        val router = router ?: return
        val confirmation = pendingConfirmation ?: return
        pendingConfirmation = null
        router.confirmInstall(confirmation)
        interactor.confirmationShown()
    }

    private fun onDismiss() {
        installSubscription?.dispose()
        interactor.cancel()
        router?.leave()
    }

    private fun show(value: InstallViewModel) {
        if (value == model) return
        model = value
        view?.render(value)
    }

}
