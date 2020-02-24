package com.tomclaw.appsend_rb.screen.apps

import android.os.Bundle
import com.tomclaw.appsend_rb.util.SchedulersFactory
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign

interface AppsPresenter {

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

}
