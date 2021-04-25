package com.tomclaw.appsend_rb.screen.about

import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.jakewharton.rxrelay2.PublishRelay
import com.tomclaw.appsend_rb.R
import io.reactivex.Observable

interface AboutView {

    fun navigationClicks(): Observable<Unit>

    fun rateClicks(): Observable<Unit>

    fun projectsClicks(): Observable<Unit>

    fun setVersion(version: String)

}

class AboutViewImpl(
    view: View
) : AboutView {

    private val toolbar: Toolbar = view.findViewById(R.id.toolbar)
    private val rateButton: View = view.findViewById(R.id.rate_button)
    private val projectsButton: View = view.findViewById(R.id.projects_button)
    private val versionText: TextView = view.findViewById(R.id.app_version)

    private val navigationRelay = PublishRelay.create<Unit>()
    private val rateRelay = PublishRelay.create<Unit>()
    private val projectsRelay = PublishRelay.create<Unit>()

    init {
        toolbar.setTitle(R.string.info)
        toolbar.setNavigationOnClickListener { navigationRelay.accept(Unit) }
        rateButton.setOnClickListener { rateRelay.accept(Unit) }
        projectsButton.setOnClickListener { projectsRelay.accept(Unit) }
    }

    override fun navigationClicks(): Observable<Unit> = navigationRelay

    override fun rateClicks(): Observable<Unit> = rateRelay

    override fun projectsClicks(): Observable<Unit> = projectsRelay

    override fun setVersion(version: String) {
        versionText.text = version
    }

}