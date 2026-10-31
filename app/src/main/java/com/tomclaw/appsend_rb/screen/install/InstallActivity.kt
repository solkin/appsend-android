package com.tomclaw.appsend_rb.screen.install

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.os.BundleCompat
import android.provider.Settings
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.tomclaw.appsend_rb.data.IntentConfirmation
import com.tomclaw.appsend_rb.databinding.ActivityInstallBinding
import com.tomclaw.appsend_rb.domain.InstallConfirmation
import com.tomclaw.appsend_rb.getComponent
import com.tomclaw.appsend_rb.screen.install.di.InstallModule
import com.tomclaw.appsend_rb.util.launchPackage
import com.tomclaw.appsend_rb.util.requestUninstall
import javax.inject.Inject

/** Entry point both from the exports screen and from file managers opening a package. */
class InstallActivity : AppCompatActivity(), InstallRouter {

    @Inject
    lateinit var presenter: InstallPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        val uri = intent.data?.toString()
        if (uri == null) {
            super.onCreate(savedInstanceState)
            finish()
            return
        }
        application.getComponent()
            .installComponent(InstallModule(this, uri, savedInstanceState?.let {
                BundleCompat.getParcelable(it, KEY_STATE, InstallState::class.java)
            }))
            .inject(this)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val binding = ActivityInstallBinding.inflate(layoutInflater)
        setContentView(binding.root)
        presenter.attachView(InstallViewImpl(binding, this, onBackPressedDispatcher))
    }

    override fun onStart() {
        super.onStart()
        if (::presenter.isInitialized) presenter.attachRouter(this)
    }

    override fun onResume() {
        super.onResume()
        if (::presenter.isInitialized) presenter.onResume()
    }

    override fun onPause() {
        if (::presenter.isInitialized) presenter.onPause()
        super.onPause()
    }

    override fun onStop() {
        if (::presenter.isInitialized) presenter.detachRouter()
        super.onStop()
    }

    override fun onDestroy() {
        if (::presenter.isInitialized) {
            presenter.detachView()
            presenter.destroy()
        }
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::presenter.isInitialized) outState.putParcelable(KEY_STATE, presenter.saveState())
    }

    override fun requestInstallPermission() {
        startActivity(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, "package:$packageName".toUri())
            } else {
                Intent(Settings.ACTION_SECURITY_SETTINGS)
            }
        )
    }

    override fun confirmInstall(confirmation: InstallConfirmation) {
        startActivity((confirmation as IntentConfirmation).intent)
    }

    override fun uninstall(packageName: String) {
        requestUninstall(packageName)
    }

    override fun launch(packageName: String) {
        launchPackage(packageName)
    }

    override fun leave() {
        finish()
    }

}

private const val KEY_STATE = "install_state"

fun createInstallIntent(context: Context, uri: String): Intent =
    Intent(context, InstallActivity::class.java)
        .setData(uri.toUri())
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
