package com.tomclaw.appsend_rb.screen.about

import com.tomclaw.appsend_rb.util.SchedulersFactory
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign

interface AboutPresenter {

    fun attachView(view: AboutView)

    fun detachView()

    fun attachRouter(router: AboutRouter)

    fun detachRouter()

    fun onBackPressed()

    interface AboutRouter {

        fun openRate()

        fun openProjects()

        fun leaveScreen()

    }

}

class AboutPresenterImpl(
    private val resourceProvider: AboutResourceProvider,
    private val schedulers: SchedulersFactory
) : AboutPresenter {

    private var view: AboutView? = null
    private var router: AboutPresenter.AboutRouter? = null

    private val subscriptions = CompositeDisposable()

    override fun attachView(view: AboutView) {
        this.view = view

        subscriptions += view.navigationClicks().subscribe { router?.leaveScreen() }
        subscriptions += view.rateClicks().subscribe { router?.openRate() }
        subscriptions += view.projectsClicks().subscribe { router?.openProjects() }

        bindVersion()
    }

    override fun detachView() {
        subscriptions.clear()
        this.view = null
    }

    override fun attachRouter(router: AboutPresenter.AboutRouter) {
        this.router = router
    }

    override fun detachRouter() {
        this.router = null
    }

    override fun onBackPressed() {
        router?.leaveScreen()
    }

    private fun bindVersion() {
        view?.setVersion(resourceProvider.provideVersion())
    }

}