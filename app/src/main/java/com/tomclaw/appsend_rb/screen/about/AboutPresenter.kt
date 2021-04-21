package com.tomclaw.appsend_rb.screen.about

import com.tomclaw.appsend_rb.util.SchedulersFactory

interface AboutPresenter {

    fun attachView(view: AboutView)

    fun detachView()

    fun attachRouter(router: AboutRouter)

    fun detachRouter()

    fun onBackPressed()

    interface AboutRouter {

        fun leaveScreen()

    }

}

class AboutPresenterImpl(schedulers: SchedulersFactory) : AboutPresenter {

    override fun detachView() {
    }

    override fun attachView(view: AboutView) {
    }

    override fun attachRouter(router: AboutPresenter.AboutRouter) {
    }

    override fun detachRouter() {
    }

    override fun onBackPressed() {
    }

}