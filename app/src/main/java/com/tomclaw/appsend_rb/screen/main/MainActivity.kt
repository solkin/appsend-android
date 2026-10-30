package com.tomclaw.appsend_rb.screen.main

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.BundleCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.commitNow
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.databinding.ActivityMainBinding
import com.tomclaw.appsend_rb.getComponent
import com.tomclaw.appsend_rb.screen.apps.AppsFragment
import com.tomclaw.appsend_rb.screen.exportprogress.createExportProgressIntent
import com.tomclaw.appsend_rb.screen.exports.ExportsFragment
import com.tomclaw.appsend_rb.screen.main.di.MainModule
import com.tomclaw.appsend_rb.screen.settings.SettingsFragment
import javax.inject.Inject

interface BottomBarHost {

    /** The bar shows only while no [owner] wants it hidden, so tabs cannot undo each other. */
    fun setBottomBarHidden(owner: String, hidden: Boolean)

}

class MainActivity : AppCompatActivity(), MainRouter, BottomBarHost {

    @Inject
    lateinit var presenter: MainPresenter

    private lateinit var view: MainViewImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        val state = savedInstanceState?.let { BundleCompat.getParcelable(it, KEY_STATE, MainState::class.java) }
        application.getComponent()
            .mainComponent(MainModule(state))
            .inject(this)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        view = MainViewImpl(binding)
        presenter.attachView(view)
        if (savedInstanceState == null) intent.requestedTab()?.let(presenter::onTabRequested)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.requestedTab()?.let(presenter::onTabRequested)
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
        outState.putParcelable(KEY_STATE, presenter.saveState())
    }

    private fun Intent.requestedTab(): MainTab? =
        if (getBooleanExtra(EXTRA_OPEN_EXPORTS, false)) MainTab.EXPORTS else null

    override fun showTab(tab: MainTab) {
        val tag = tab.name
        supportFragmentManager.commitNow {
            setReorderingAllowed(true)
            supportFragmentManager.fragments
                .filter { it.tag != tag && it.id == R.id.container }
                .forEach { hide(it) }
            val existing = supportFragmentManager.findFragmentByTag(tag)
            if (existing == null) {
                add(R.id.container, newTab(tab), tag)
            } else {
                show(existing)
            }
        }
    }

    override fun openExportProgress() {
        startActivity(createExportProgressIntent(this))
    }

    private fun newTab(tab: MainTab): Fragment = when (tab) {
        MainTab.APPS -> AppsFragment()
        MainTab.EXPORTS -> ExportsFragment()
        MainTab.SETTINGS -> SettingsFragment()
    }

    override fun setBottomBarHidden(owner: String, hidden: Boolean) {
        view.setBottomBarHidden(owner, hidden)
    }

}

fun createExportsTabIntent(context: Context): Intent =
    Intent(context, MainActivity::class.java)
        .putExtra(EXTRA_OPEN_EXPORTS, true)
        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

private const val EXTRA_OPEN_EXPORTS = "open_exports"
private const val KEY_STATE = "main_state"
