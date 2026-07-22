package com.tomclaw.appsend_rb.screen.permissions

import android.os.Bundle
import com.tomclaw.appsend.util.Analytics
import com.tomclaw.appsend_rb.util.SchedulersFactory
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign

interface PermissionsPresenter {

    fun attachView(view: PermissionsView)

    fun detachView()

    fun attachRouter(router: PermissionsRouter)

    fun detachRouter()

    fun saveState(): Bundle

    fun onBackPressed()

    interface PermissionsRouter {

        fun leaveScreen()

    }

}

class PermissionsPresenterImpl(
    private val permissions: List<String>,
    private val converter: PermissionsConverter,
    private val schedulers: SchedulersFactory,
    private val analytics: Analytics,
    state: Bundle?
) : PermissionsPresenter {

    private var view: PermissionsView? = null
    private var router: PermissionsPresenter.PermissionsRouter? = null

    private val subscriptions = CompositeDisposable()

    override fun attachView(view: PermissionsView) {
        this.view = view

        analytics.trackEvent(
            "screen_open",
            mapOf("screen" to "permissions"),
            mapOf("permissions_count" to permissions.size.toDouble())
        )
        subscriptions += view.navigationClicks().subscribe {
            onBackPressed()
        }

        bindPermissions()
    }

    override fun detachView() {
        subscriptions.clear()
        this.view = null
    }

    override fun attachRouter(router: PermissionsPresenter.PermissionsRouter) {
        this.router = router
    }

    override fun detachRouter() {
        this.router = null
    }

    override fun saveState() = Bundle()

    override fun onBackPressed() {
        analytics.trackEvent("navigation_back", mapOf("screen" to "permissions"))
        router?.leaveScreen()
    }

    private fun bindPermissions() {
        view?.setPermissions(permissions.map { converter.convert(it) })
    }

}
