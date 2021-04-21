package com.tomclaw.appsend_rb.screen.about

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.getComponent
import com.tomclaw.appsend_rb.screen.about.di.AboutModule
import com.tomclaw.appsend_rb.screen.apps.PreferencesProvider
import com.tomclaw.appsend_rb.util.updateStatusBar
import com.tomclaw.appsend_rb.util.updateTheme
import javax.inject.Inject

class AboutActivity : AppCompatActivity(), AboutPresenter.AboutRouter {

    @Inject
    lateinit var presenter: AboutPresenter

    @Inject
    lateinit var preferences: PreferencesProvider

    private var isDarkTheme: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        application.getComponent()
            .aboutComponent(AboutModule(this))
            .inject(activity = this)

        isDarkTheme = updateTheme(preferences)
        updateStatusBar()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.about_activity)

        val view = AboutViewImpl(window.decorView)

        presenter.attachView(view)
    }

    override fun onBackPressed() {
        presenter.onBackPressed()
    }

    override fun onStart() {
        super.onStart()
        presenter.attachRouter(this)
    }

    override fun onStop() {
        presenter.detachRouter()
        super.onStop()
    }

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }

    override fun leaveScreen() {
        finish()
    }

}