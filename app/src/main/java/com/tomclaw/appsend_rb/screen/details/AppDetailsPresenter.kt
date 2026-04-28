package com.tomclaw.appsend_rb.screen.details

import com.tomclaw.appsend.util.Analytics
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
    private val resourceProvider: AppDetailsResourceProvider,
    private val analytics: Analytics
) : AppDetailsPresenter {

    private var view: AppDetailsView? = null
    private var router: AppDetailsPresenter.AppDetailsRouter? = null

    private val subscriptions = CompositeDisposable()

    override fun attachView(view: AppDetailsView) {
        this.view = view

        analytics.trackEvent("screen_open", mapOf("screen" to "app_details") + entity.analyticsTags())
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
        analytics.trackEvent("navigation_back", mapOf("screen" to "app_details"))
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
        analytics.trackEvent("app_details_action_selected", mapOf("action" to "copy_package_name") + entity.analyticsTags())
        router?.copyPackageName(entity.packageName)
        view?.showPackageNameCopied()
    }

    private fun showPermissions() {
        entity.requestedPermissions?.takeIf { it.isNotEmpty() }
            ?.let {
                analytics.trackEvent(
                    "app_details_action_selected",
                    mapOf("action" to "open_permissions") + entity.analyticsTags(),
                    mapOf("permissions_count" to it.size.toDouble())
                )
                router?.showRequestedPermissions(it)
            }
    }

    private fun showSystemDetails() {
        analytics.trackEvent("app_details_action_selected", mapOf("action" to "open_system_details") + entity.analyticsTags())
        router?.showSystemDetails(entity.packageName)
    }

    private fun AppEntity.analyticsTags(): Map<String, String> {
        return mapOf(
            "system" to system.toString(),
            "split" to split.toString(),
            "has_permissions" to (!requestedPermissions.isNullOrEmpty()).toString()
        )
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
