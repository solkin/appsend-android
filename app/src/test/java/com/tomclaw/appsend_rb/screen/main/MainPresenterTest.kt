package com.tomclaw.appsend_rb.screen.main

import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import com.tomclaw.appsend_rb.ImmediateSchedulers
import io.reactivex.rxjava3.core.Observable
import org.junit.Assert.assertEquals
import org.junit.Test

class MainPresenterTest {

    private val unseen = BehaviorRelay.createDefault(0)
    private val shares = BehaviorRelay.createDefault(false)
    private var marks = 0
    private val view = FakeView()
    private val router = FakeRouter()
    private val presenter = MainPresenterImpl(
        object : MainInteractor {
            override fun unseenExports(): Observable<Int> = unseen

            override fun markExportsSeen() {
                marks++
                unseen.accept(0)
            }

            override fun finishedShares(): Observable<Boolean> = shares
        },
        ImmediateSchedulers,
        null,
    ).also {
        it.attachView(view)
        it.attachRouter(router)
    }

    @Test
    fun `new exports show on the badge until the tab is opened`() {
        unseen.accept(2)
        assertEquals(2, view.badge)
        view.tabs.accept(MainTab.EXPORTS)
        assertEquals(0, view.badge)
        assertEquals(MainTab.EXPORTS, router.shown)
    }

    @Test
    fun `nothing counts while the tab is open`() {
        view.tabs.accept(MainTab.EXPORTS)
        unseen.accept(1)
        assertEquals(0, view.badge)
    }

    @Test
    fun `a requested tab goes through the navigation`() {
        presenter.onTabRequested(MainTab.EXPORTS)
        assertEquals(MainTab.EXPORTS, view.selected)
    }

    @Test
    fun `a share finished unattended is picked up`() {
        shares.accept(true)
        assertEquals(1, router.progressOpened)
    }

    @Test
    fun `a share finished while hidden waits for the screen to return`() {
        presenter.detachRouter()
        shares.accept(true)
        assertEquals(0, router.progressOpened)
        presenter.attachRouter(router)
        assertEquals(1, router.progressOpened)
    }

    private class FakeView : MainView {

        var badge = 0
        var selected: MainTab? = null
        val tabs = PublishRelay.create<MainTab>()

        override fun selectTab(tab: MainTab) {
            selected = tab
        }

        override fun setExportsBadge(count: Int) {
            badge = count
        }

        override fun tabSelections(): Observable<MainTab> = tabs

    }

    private class FakeRouter : MainRouter {

        var shown: MainTab? = null
        var progressOpened = 0

        override fun showTab(tab: MainTab) {
            shown = tab
        }

        override fun openExportProgress() {
            progressOpened++
        }

    }

}
