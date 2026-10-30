package com.tomclaw.appsend_rb.screen.main

import com.tomclaw.appsend_rb.domain.ExportAction
import com.tomclaw.appsend_rb.domain.ExportManager
import com.tomclaw.appsend_rb.domain.SchedulersFactory
import com.tomclaw.appsend_rb.domain.UnseenExports
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign

enum class MainTab { APPS, EXPORTS, SETTINGS }

interface MainInteractor {

    fun unseenExports(): Observable<Int>

    fun markExportsSeen()

    /** A share that ended unattended: its files exist nowhere else the user could reach. */
    fun finishedShares(): Observable<Boolean>

}

class MainInteractorImpl(
    private val manager: ExportManager,
    private val unseen: UnseenExports,
) : MainInteractor {

    override fun unseenExports(): Observable<Int> = unseen.count()

    override fun markExportsSeen() = unseen.markSeen()

    override fun finishedShares(): Observable<Boolean> = manager.session()
        .map { it.action == ExportAction.SHARE && it.items.isNotEmpty() && !it.active }
        .distinctUntilChanged()

}

interface MainRouter {

    fun showTab(tab: MainTab)

    fun openExportProgress()

}

interface MainPresenter {

    fun attachView(view: MainView)

    fun detachView()

    fun attachRouter(router: MainRouter)

    fun detachRouter()

    fun onTabRequested(tab: MainTab)

    fun saveState(): MainState

}

class MainPresenterImpl(
    private val interactor: MainInteractor,
    private val schedulers: SchedulersFactory,
    state: MainState?,
) : MainPresenter {

    private var view: MainView? = null
    private var router: MainRouter? = null
    private val subscriptions = CompositeDisposable()
    private var tab = state?.tab ?: MainTab.APPS
    private var finishedShare = false

    override fun attachView(view: MainView) {
        this.view = view
        subscriptions += view.tabSelections().subscribe(::onTabSelected)
        subscriptions += interactor.unseenExports()
            .observeOn(schedulers.mainThread())
            .subscribe(::onUnseenExports)
        subscriptions += interactor.finishedShares()
            .observeOn(schedulers.mainThread())
            .subscribe {
                finishedShare = it
                pickUpFinishedShare()
            }
        view.selectTab(tab)
    }

    override fun detachView() {
        subscriptions.clear()
        view = null
    }

    override fun attachRouter(router: MainRouter) {
        this.router = router
        router.showTab(tab)
        pickUpFinishedShare()
    }

    override fun detachRouter() {
        router = null
    }

    override fun onTabRequested(tab: MainTab) {
        view?.selectTab(tab)
    }

    override fun saveState() = MainState(tab)

    private fun onTabSelected(selected: MainTab) {
        tab = selected
        router?.showTab(selected)
        if (selected == MainTab.EXPORTS) interactor.markExportsSeen()
    }

    private fun onUnseenExports(count: Int) {
        if (tab == MainTab.EXPORTS && count > 0) {
            interactor.markExportsSeen()
        } else {
            view?.setExportsBadge(count)
        }
    }

    private fun pickUpFinishedShare() {
        if (finishedShare) router?.openExportProgress()
    }

}
