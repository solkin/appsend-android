package com.tomclaw.appsend_rb.screen.apps

import android.os.Bundle
import com.avito.konveyor.adapter.AdapterPresenter
import com.avito.konveyor.blueprint.Item
import com.avito.konveyor.data_source.ListDataSource
import com.tomclaw.appsend_rb.dto.AppEntity
import com.tomclaw.appsend_rb.screen.apps.adapter.ItemClickListener
import com.tomclaw.appsend_rb.screen.apps.adapter.app.AppItem
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

        fun runApp(packageName: String): Boolean

        fun openGooglePlay(packageName: String)

    }

}

class AppsPresenterImpl(
        private val interactor: AppsInteractor,
        private val adapterPresenter: Lazy<AdapterPresenter>,
        private val appEntityConverter: AppEntityConverter,
        private val preferences: PreferencesProvider,
        private val schedulers: SchedulersFactory,
        state: Bundle?
) : AppsPresenter {

    private var view: AppsView? = null
    private var router: AppsPresenter.AppsRouter? = null

    private var entities: List<AppEntity>? = state?.getParcelableArrayList(KEY_ENTITIES)

    private val subscriptions = CompositeDisposable()

    override fun attachView(view: AppsView) {
        this.view = view
        subscriptions += view.refreshClicks().subscribe { }
        subscriptions += view.prefsClicks().subscribe { onPrefsClicked() }
        subscriptions += view.infoClicks().subscribe { onInfoClicked() }
        subscriptions += view.appMenuClicks().subscribe { onAppMenuClicked(it) }

        entities.takeIf { it != null }
                ?.run { bindAppEntities(this) }
                ?: loadAppItems()
    }

    private fun onPrefsClicked() {
        router?.showPrefsScreen()
    }

    private fun onInfoClicked() {
        router?.showInfoScreen()
    }

    private fun onAppMenuClicked(pair: Pair<Int, AppItem>) {
        val item = pair.second
        when (pair.first) {
            ACTION_RUN_APP -> router?.runApp(item.packageName)
                    ?.let { result ->
                        if (!result) view?.showAppLaunchError()
                    }
            ACTION_FIND_IN_GP -> router?.openGooglePlay(item.packageName)
        }
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
                systemApps = preferences.isShowSystemApps(),
                runnableOnly = preferences.isRunnableOnly(),
                sortOrder = preferences.getSortOrder()
        )
                .observeOn(schedulers.mainThread())
                .doOnSubscribe { view?.showProgress() }
                .doAfterTerminate { view?.showContent() }
                .subscribe({ entities ->
                    bindAppEntities(entities)
                }, {})
    }

    private fun bindAppEntities(entities: List<AppEntity>) {
        this.entities = entities
        var id: Long = 0
        val items = entities
                .sortedBy { it.lastUpdateTime }
                .reversed()
                .map { appEntityConverter.convert(id++, it) }
        val dataSource = ListDataSource(items)
        adapterPresenter.get().onDataSourceChanged(dataSource)
        view?.contentUpdated()
    }

    override fun saveState() = Bundle().apply {
        entities?.let { putParcelableArrayList(KEY_ENTITIES, ArrayList(it)) }
    }

    override fun onBackPressed() {
        router?.leaveScreen()
    }

    override fun onItemClick(item: Item) {
        when (item) {
            is AppItem -> view?.showAppMenu(item)
        }
    }

}

private const val KEY_ENTITIES = "entities"
