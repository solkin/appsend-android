package com.tomclaw.appsend_rb.screen.apps

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.avito.konveyor.ItemBinder
import com.avito.konveyor.adapter.AdapterPresenter
import com.avito.konveyor.adapter.SimpleRecyclerAdapter
import com.tomclaw.appsend_rb.AboutActivity
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.SettingsActivity
import com.tomclaw.appsend_rb.getComponent
import com.tomclaw.appsend_rb.screen.apps.di.AppsModule
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

    override fun onCreate(savedInstanceState: Bundle?) {
        val presenterState = savedInstanceState?.getBundle(KEY_PRESENTER_STATE)
        application.getComponent()
                .appsComponent(AppsModule(this, presenterState))
                .inject(activity = this)

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

}

private const val KEY_PRESENTER_STATE = "presenter_state"
