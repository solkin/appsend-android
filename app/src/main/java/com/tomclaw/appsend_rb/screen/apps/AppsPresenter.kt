package com.tomclaw.appsend_rb.screen.apps

import android.os.Bundle
import com.avito.konveyor.adapter.AdapterPresenter
import com.avito.konveyor.blueprint.Item
import com.avito.konveyor.data_source.ListDataSource
import com.tomclaw.appsend_rb.dto.AppEntity
import com.tomclaw.appsend_rb.screen.apps.adapter.ItemClickListener
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
        private val appEntityConverter: AppEntityConverter,
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

        loadAppItems()
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

    private fun loadAppItems() {
        subscriptions += interactor.loadApps(
                        systemApps = false,
                        runnableOnly = false,
                        sortOrder = NAME_ASCENDING
                )
                .observeOn(schedulers.mainThread())
                .doOnSubscribe { view?.showProgress() }
                .doAfterTerminate { view?.showContent() }
                .subscribe({ apps ->
                    bindAppItems(apps)
                }, {})
    }

    private fun bindAppItems(apps: List<AppEntity>) {
        var id: Long = 0
        val items = apps
                .sortedBy { it.lastUpdateTime }
                .reversed()
                .map { appEntityConverter.convert(id++, it) }
        val dataSource = ListDataSource(items)
        adapterPresenter.get().onDataSourceChanged(dataSource)
        view?.contentUpdated()
    }

    override fun saveState() = Bundle().apply {
    }

    override fun onBackPressed() {
    }

    override fun onItemClick(item: Item) {
    }

}
