package com.tomclaw.appsend_rb.screen.permissions

import android.os.Bundle
import com.avito.konveyor.adapter.AdapterPresenter
import com.avito.konveyor.data_source.ListDataSource
import com.tomclaw.appsend_rb.util.SchedulersFactory
import dagger.Lazy
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
    private val adapterPresenter: Lazy<AdapterPresenter>,
    private val converter: PermissionsConverter,
    private val schedulers: SchedulersFactory,
    state: Bundle?
) : PermissionsPresenter {

    private var view: PermissionsView? = null
    private var router: PermissionsPresenter.PermissionsRouter? = null

    private val subscriptions = CompositeDisposable()

    override fun attachView(view: PermissionsView) {
        this.view = view

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

    override fun saveState() = Bundle().apply {
    }

    override fun onBackPressed() {
        router?.leaveScreen()
    }

    private fun bindPermissions() {
        val items = permissions.map { converter.convert(it) }
        val dataSource = ListDataSource(items)
        adapterPresenter.get().onDataSourceChanged(dataSource)
        view?.contentUpdated()
    }

}
