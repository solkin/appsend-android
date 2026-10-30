package com.tomclaw.appsend_rb.screen.permissions

import com.tomclaw.appsend_rb.domain.AppDetails
import com.tomclaw.appsend_rb.domain.InstalledAppsRepository
import com.tomclaw.appsend_rb.domain.RequestedPermission
import com.tomclaw.appsend_rb.domain.Analytics
import com.tomclaw.appsend_rb.domain.SchedulersFactory
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign

enum class PermissionFilter { ALL, DANGEROUS, GRANTED }

sealed interface PermissionRow {

    data class Section(val dangerous: Boolean) : PermissionRow

    data class Item(
        val permission: RequestedPermission,
        val position: Int,
        val groupSize: Int,
    ) : PermissionRow

}

data class PermissionsViewModel(
    val appLabel: String,
    val total: Int,
    val dangerous: Int,
    val granted: Int,
    val rows: List<PermissionRow>,
)

interface PermissionsInteractor {

    fun details(): Single<AppDetails>

}

class PermissionsInteractorImpl(
    private val packageName: String,
    private val repository: InstalledAppsRepository,
) : PermissionsInteractor {

    override fun details(): Single<AppDetails> = repository.details(packageName)

}

interface PermissionsRouter {

    fun leave()

}

interface PermissionsPresenter {

    fun attachView(view: PermissionsView)

    fun detachView()

    fun attachRouter(router: PermissionsRouter)

    fun detachRouter()

}

class PermissionsPresenterImpl(
    private val interactor: PermissionsInteractor,
    private val schedulers: SchedulersFactory,
    private val analytics: Analytics,
) : PermissionsPresenter {

    private var view: PermissionsView? = null
    private var router: PermissionsRouter? = null
    private val subscriptions = CompositeDisposable()
    private var details: AppDetails? = null
    private var filter = PermissionFilter.ALL
    private var leavePending = false

    override fun attachView(view: PermissionsView) {
        this.view = view
        analytics.trackEvent("screen_open", mapOf("screen" to "permissions"))
        subscriptions += view.backClicks().subscribe { router?.leave() }
        subscriptions += view.filterChanges().subscribe {
            filter = it
            render()
        }
        subscriptions += interactor.details()
            .observeOn(schedulers.mainThread())
            .subscribe({
                details = it
                render()
            }, { error ->
                analytics.trackException(error, mapOf("event" to "permissions_load_failed"))
                leavePending = true
                router?.leave()
            })
    }

    override fun detachView() {
        subscriptions.clear()
        view = null
    }

    override fun attachRouter(router: PermissionsRouter) {
        this.router = router
        if (leavePending) router.leave()
    }

    override fun detachRouter() {
        router = null
    }

    private fun render() {
        val details = details ?: return
        val all = details.permissions
        val visible = when (filter) {
            PermissionFilter.ALL -> all
            PermissionFilter.DANGEROUS -> all.filter { it.dangerous }
            PermissionFilter.GRANTED -> all.filter { it.granted }
        }
        view?.render(
            PermissionsViewModel(
                appLabel = details.app.label,
                total = all.size,
                dangerous = all.count { it.dangerous },
                granted = all.count { it.granted },
                rows = section(visible, dangerous = true) + section(visible, dangerous = false),
            )
        )
    }

    private fun section(permissions: List<RequestedPermission>, dangerous: Boolean): List<PermissionRow> {
        val group = permissions.filter { it.dangerous == dangerous }
        if (group.isEmpty()) return emptyList()
        return listOf(PermissionRow.Section(dangerous)) +
            group.mapIndexed { index, permission -> PermissionRow.Item(permission, index, group.size) }
    }

}
