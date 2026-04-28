package com.tomclaw.appsend_rb.screen.about

import com.tomclaw.appsend.util.Analytics
import com.tomclaw.appsend_rb.util.SchedulersFactory
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign

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
    private val schedulers: SchedulersFactory,
    private val analytics: Analytics
) : AboutPresenter {

    private var view: AboutView? = null
    private var router: AboutPresenter.AboutRouter? = null

    private val subscriptions = CompositeDisposable()

    override fun attachView(view: AboutView) {
        this.view = view

        analytics.trackEvent("screen_open", mapOf("screen" to "about"))
        subscriptions += view.navigationClicks().subscribe {
            analytics.trackEvent("navigation_back", mapOf("screen" to "about"))
            router?.leaveScreen()
        }
        subscriptions += view.rateClicks().subscribe {
            analytics.trackEvent("about_action_selected", mapOf("action" to "rate_app"))
            router?.openRate()
        }
        subscriptions += view.projectsClicks().subscribe {
            analytics.trackEvent("about_action_selected", mapOf("action" to "open_developer_projects"))
            router?.openProjects()
        }

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
        analytics.trackEvent("navigation_back", mapOf("screen" to "about", "source" to "system_back"))
        router?.leaveScreen()
    }

    private fun bindVersion() {
        view?.setVersion(resourceProvider.provideVersion())
    }

}