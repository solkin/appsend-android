package com.tomclaw.appsend_rb.screen.details

import com.tomclaw.appsend_rb.dto.AppEntity
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign

interface AppDetailsPresenter {

    fun attachView(view: AppDetailsView)

    fun detachView()

    fun attachRouter(router: AppDetailsRouter)

    fun detachRouter()

    fun onBackPressed()

    interface AppDetailsRouter {

        fun copyPackageName(packageName: String)

        fun showRequestedPermissions(permissions: List<String>)

        fun showSystemDetails(packageName: String)

        fun leaveScreen()

    }

}

class AppDetailsPresenterImpl(
    private val entity: AppEntity,
    private val resourceProvider: AppDetailsResourceProvider
) : AppDetailsPresenter {

    private var view: AppDetailsView? = null
    private var router: AppDetailsPresenter.AppDetailsRouter? = null

    private val subscriptions = CompositeDisposable()

    override fun attachView(view: AppDetailsView) {
        this.view = view

        subscriptions += view.navigationClicks().subscribe { onBackPressed() }
        subscriptions += view.copyPackageClicks().subscribe { copyPackageName() }
        subscriptions += view.permissionsClicks().subscribe { showPermissions() }
        subscriptions += view.systemDetailsClicks().subscribe { showSystemDetails() }

        bindDetails()
    }

    override fun detachView() {
        subscriptions.clear()
        this.view = null
    }

    override fun attachRouter(router: AppDetailsPresenter.AppDetailsRouter) {
        this.router = router
    }

    override fun detachRouter() {
        this.router = null
    }

    override fun onBackPressed() {
        router?.leaveScreen()
    }

    private fun bindDetails() {
        view?.setDetails(
            AppDetailsUi(
                label = entity.label,
                packageName = entity.packageName,
                version = resourceProvider.formatVersion(entity),
                installTime = resourceProvider.formatTime(entity.firstInstallTime),
                updateTime = resourceProvider.formatTime(entity.lastUpdateTime),
                size = resourceProvider.formatSize(entity.size),
                path = entity.path,
                permissions = resourceProvider.formatPermissionsCount(entity.requestedPermissions?.size ?: 0),
                status = resourceProvider.formatStatus(entity.system, entity.split),
                permissionsEnabled = !entity.requestedPermissions.isNullOrEmpty()
            )
        )
    }

    private fun copyPackageName() {
        router?.copyPackageName(entity.packageName)
        view?.showPackageNameCopied()
    }

    private fun showPermissions() {
        entity.requestedPermissions?.takeIf { it.isNotEmpty() }
            ?.let { router?.showRequestedPermissions(it) }
    }

    private fun showSystemDetails() {
        router?.showSystemDetails(entity.packageName)
    }

}

data class AppDetailsUi(
    val label: String,
    val packageName: String,
    val version: String,
    val installTime: String,
    val updateTime: String,
    val size: String,
    val path: String,
    val permissions: String,
    val status: String,
    val permissionsEnabled: Boolean
)
