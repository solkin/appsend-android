package com.tomclaw.appsend_rb.screen.details

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.tomclaw.appsend_rb.databinding.ActivityDetailsBinding
import com.tomclaw.appsend_rb.getComponent
import com.tomclaw.appsend_rb.screen.details.di.AppDetailsModule
import com.tomclaw.appsend_rb.screen.exportoptions.ExportOptionsFragment
import com.tomclaw.appsend_rb.screen.exportprogress.createExportProgressIntent
import com.tomclaw.appsend_rb.screen.permissions.createPermissionsIntent
import com.tomclaw.appsend_rb.util.launchPackage
import com.tomclaw.appsend_rb.util.openAppSystemSettings
import com.tomclaw.appsend_rb.util.openStorePage
import com.tomclaw.appsend_rb.util.requestUninstall
import javax.inject.Inject

class AppDetailsActivity : AppCompatActivity(), AppDetailsRouter {

    @Inject
    lateinit var presenter: AppDetailsPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        val packageName = requireNotNull(intent.getStringExtra(EXTRA_PACKAGE))
        application.getComponent()
            .appDetailsComponent(AppDetailsModule(this, packageName))
            .inject(this)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val binding = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        presenter.attachView(AppDetailsViewImpl(binding))
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
        presenter.destroy()
        super.onDestroy()
    }

    override fun launch(packageName: String): Boolean = launchPackage(packageName)

    override fun openExportOptions(packageName: String) {
        ExportOptionsFragment.newInstance(listOf(packageName))
            .show(supportFragmentManager, ExportOptionsFragment.TAG)
    }

    override fun openExportProgress() {
        startActivity(createExportProgressIntent(this))
    }

    override fun openPermissions(packageName: String) {
        startActivity(createPermissionsIntent(this, packageName))
    }

    override fun openStorePage(packageName: String) {
        (this as Context).openStorePage(packageName)
    }

    override fun openSystemDetails(packageName: String) {
        openAppSystemSettings(packageName)
    }

    override fun uninstall(packageName: String) {
        requestUninstall(packageName)
    }

    override fun leave() {
        finish()
    }

}

fun createAppDetailsIntent(context: Context, packageName: String): Intent =
    Intent(context, AppDetailsActivity::class.java).putExtra(EXTRA_PACKAGE, packageName)

private const val EXTRA_PACKAGE = "package_name"
