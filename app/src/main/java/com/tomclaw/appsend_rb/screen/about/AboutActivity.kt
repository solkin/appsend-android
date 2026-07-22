package com.tomclaw.appsend_rb.screen.about

import android.content.Context
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.getComponent
import com.tomclaw.appsend_rb.screen.about.di.AboutModule
import com.tomclaw.appsend_rb.util.installKnopochki
import javax.inject.Inject

class AboutActivity : AppCompatActivity(), AboutPresenter.AboutRouter {

    @Inject
    lateinit var presenter: AboutPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        application.getComponent()
            .aboutComponent(AboutModule(this))
            .inject(activity = this)

        super.onCreate(savedInstanceState)
        val knopochkiView = installKnopochki()
        val view = AboutViewImpl(knopochkiView)
        presenter.attachView(view)

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = presenter.onBackPressed()
        })
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

    override fun openRate() {
        openUriSafe(
            uri = MARKET_URI_RATE + packageName,
            fallback = WEB_URI_RATE + packageName
        )
    }

    override fun openProjects() {
        openUriSafe(
            uri = MARKET_URI_PROJECTS,
            fallback = WEB_URI_PROJECTS
        )
    }

    override fun leaveScreen() {
        finish()
    }

    private fun openUriSafe(uri: String, fallback: String) {
        try {
            startActivity(Intent(ACTION_VIEW, Uri.parse(uri)))
        } catch (ignored: android.content.ActivityNotFoundException) {
            startActivity(Intent(ACTION_VIEW, Uri.parse(fallback)))
        }
    }

}

fun createAboutActivityIntent(
    context: Context
): Intent = Intent(context, AboutActivity::class.java)

private const val MARKET_URI_RATE = "market://details?id="
private const val MARKET_URI_PROJECTS = "market://search?q=pub:TomClaw+Software"
private const val WEB_URI_RATE = "http://play.google.com/store/apps/details?id="
private const val WEB_URI_PROJECTS = "http://play.google.com/store/apps/developer?id=TomClaw+Software"