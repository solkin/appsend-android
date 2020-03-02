package com.tomclaw.appsend_rb.screen.apps

import android.os.Bundle
import com.avito.konveyor.adapter.AdapterPresenter
import com.avito.konveyor.blueprint.Item
import com.avito.konveyor.data_source.ListDataSource
import com.tomclaw.appsend_rb.screen.apps.adapter.ItemClickListener
import com.tomclaw.appsend_rb.screen.apps.adapter.app.AppItem
import com.tomclaw.appsend_rb.util.AppIconData
import com.tomclaw.appsend_rb.util.SchedulersFactory
import dagger.Lazy
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign

interface AppsPresenter : ItemClickListener {

    fun attachView(view: AppsView)

    fun detachView()

    fun attachRouter(router: AppsRouter)

    fun detachRouter()

    fun saveState(): Bundle

    fun onBackPressed()

    interface AppsRouter {

        fun showPrefsScreen()

        fun showInfoScreen()

        fun leaveScreen()

    }

}

class AppsPresenterImpl(
        private val interactor: AppsInteractor,
        private val adapterPresenter: Lazy<AdapterPresenter>,
        private val appInfoConverter: AppInfoConverter,
        private val schedulers: SchedulersFactory,
        state: Bundle?
) : AppsPresenter {

    private var view: AppsView? = null
    private var router: AppsPresenter.AppsRouter? = null

    private val subscriptions = CompositeDisposable()

    override fun attachView(view: AppsView) {
        this.view = view
        subscriptions += view.refreshClicks().subscribe { }
        subscriptions += view.prefsClicks().subscribe { onPrefsClicked() }
        subscriptions += view.infoClicks().subscribe { onInfoClicked() }

        val items = listOf(AppItem(
                id = 1,
                icon = AppIconData("com.tomclaw.appsend_rb", 1),
                name = "AppSend",
                versionName = "1.0",
                versionCode = 1
        ))
        val dataSource = ListDataSource(items)
        adapterPresenter.get().onDataSourceChanged(dataSource)
        view.contentUpdated()
    }

    private fun onPrefsClicked() {
        router?.showPrefsScreen()
    }

    private fun onInfoClicked() {
        router?.showInfoScreen()
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

    override fun saveState() = Bundle().apply {
    }

    override fun onBackPressed() {
    }

    override fun onItemClick(item: Item) {
    }

}
