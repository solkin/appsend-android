package com.tomclaw.appsend_rb.screen.apps

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.avito.konveyor.ItemBinder
import com.avito.konveyor.adapter.AdapterPresenter
import com.avito.konveyor.adapter.SimpleRecyclerAdapter
import com.tomclaw.appsend_rb.AboutActivity
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.SettingsActivity
import com.tomclaw.appsend_rb.getComponent
import com.tomclaw.appsend_rb.screen.apps.di.AppsModule
import com.tomclaw.appsend_rb.util.updateStatusBar
import com.tomclaw.appsend_rb.util.updateTheme
import javax.inject.Inject

class AppsActivity : AppCompatActivity(), AppsPresenter.AppsRouter {

    @Inject
    lateinit var presenter: AppsPresenter

    @Inject
    lateinit var adapterPresenter: AdapterPresenter

    @Inject
    lateinit var preferences: PreferencesProvider

    @Inject
    lateinit var binder: ItemBinder

    private var isDarkTheme: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val presenterState = savedInstanceState?.getBundle(KEY_PRESENTER_STATE)
        application.getComponent()
                .appsComponent(AppsModule(this, presenterState))
                .inject(activity = this)

        isDarkTheme = updateTheme(preferences)
        updateStatusBar()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.apps_activity)

        val adapter = SimpleRecyclerAdapter(adapterPresenter, binder)
        val view = AppsViewImpl(window.decorView, adapter, preferences)

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

    override fun onResume() {
        super.onResume()
        if (isDarkTheme != preferences.isDarkTheme()) {
            val intent = intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            finish()
            startActivity(intent)
        }
    }

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBundle(KEY_PRESENTER_STATE, presenter.saveState())
    }

    override fun showPrefsScreen() {
        val intent = Intent(
                this,
                SettingsActivity::class.java
        )
        startActivity(intent)
    }

    override fun showInfoScreen() {
        val intent = Intent(
                this,
                AboutActivity::class.java
        )
        startActivity(intent)
    }

    override fun leaveScreen() {
        finish()
    }

    override fun runApp(packageName: String): Boolean {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        launchIntent?.run { startActivity(launchIntent) }
        return launchIntent != null
    }

    override fun openGooglePlay(packageName: String) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (ex: ActivityNotFoundException) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
        }
    }

    override fun showAppDetails(packageName: String) {
        val intent = Intent()
                .setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .addCategory(Intent.CATEGORY_DEFAULT)
                .setData(Uri.parse("package:$packageName"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun runAppUninstall(packageName: String) {
        val packageUri = Uri.parse("package:$packageName")
        val uninstallIntent = Intent(Intent.ACTION_DELETE, packageUri)
        startActivity(uninstallIntent)
    }

}

private const val KEY_PRESENTER_STATE = "presenter_state"
