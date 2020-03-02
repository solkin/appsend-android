package com.tomclaw.appsend_rb.screen.apps

import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.avito.konveyor.adapter.SimpleRecyclerAdapter
import com.jakewharton.rxrelay2.PublishRelay
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.util.hideWithAlphaAnimation
import com.tomclaw.appsend_rb.util.showWithAlphaAnimation
import io.reactivex.Observable

interface AppsView {

    fun showProgress()

    fun showContent()

    fun contentUpdated()

    fun refreshClicks(): Observable<Unit>

    fun prefsClicks(): Observable<Unit>

    fun infoClicks(): Observable<Unit>

}

class AppsViewImpl(
        private val view: View,
        private val adapter: SimpleRecyclerAdapter
) : AppsView {

    private val context = view.context
    private val toolbar: Toolbar = view.findViewById(R.id.toolbar)
    private val recycler: RecyclerView = view.findViewById(R.id.recycler)
    private val overlayProgress: View = view.findViewById(R.id.overlay_progress)

    private val refreshRelay = PublishRelay.create<Unit>()
    private val prefsRelay = PublishRelay.create<Unit>()
    private val infoRelay = PublishRelay.create<Unit>()

    init {
        toolbar.setTitle(R.string.app_name)
        toolbar.inflateMenu(R.menu.main_menu)
        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.refresh -> refreshRelay.accept(Unit)
                R.id.settings -> prefsRelay.accept(Unit)
                R.id.info -> infoRelay.accept(Unit)
            }
            true
        }
        adapter.setHasStableIds(true)
        recycler.adapter = adapter
        recycler.layoutManager = LinearLayoutManager(
                context,
                RecyclerView.VERTICAL,
                false
        )
        recycler.itemAnimator = DefaultItemAnimator()
    }

    override fun showProgress() {
        overlayProgress.showWithAlphaAnimation(animateFully = true)
    }

    override fun showContent() {
        overlayProgress.hideWithAlphaAnimation(animateFully = false)
    }

    override fun contentUpdated() {
        adapter.notifyDataSetChanged()
    }

    override fun refreshClicks(): Observable<Unit> = refreshRelay

    override fun prefsClicks(): Observable<Unit> = prefsRelay

    override fun infoClicks(): Observable<Unit> = infoRelay

}
