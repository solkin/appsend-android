package com.tomclaw.appsend_rb.screen.exportprogress

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.tomclaw.appsend_rb.databinding.ActivityExportProgressBinding
import com.tomclaw.appsend_rb.domain.ExportedFile
import com.tomclaw.appsend_rb.getComponent
import com.tomclaw.appsend_rb.screen.exportprogress.di.ExportProgressModule
import com.tomclaw.appsend_rb.screen.main.createExportsTabIntent
import com.tomclaw.appsend_rb.util.shareFiles
import javax.inject.Inject

class ExportProgressActivity : AppCompatActivity(), ExportProgressRouter {

    @Inject
    lateinit var presenter: ExportProgressPresenter

    // The export runs either way: the notification only reports on it.
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        application.getComponent()
            .exportProgressComponent(ExportProgressModule(this))
            .inject(this)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val binding = ActivityExportProgressBinding.inflate(layoutInflater)
        setContentView(binding.root)
        presenter.attachView(ExportProgressViewImpl(binding, this, onBackPressedDispatcher))
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

    override fun shareFiles(files: List<ExportedFile>) {
        (this as Context).shareFiles(files)
    }

    override fun openExports() {
        startActivity(createExportsTabIntent(this))
    }

    override fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun leave() {
        finish()
    }

}

fun createExportProgressIntent(context: Context): Intent =
    Intent(context, ExportProgressActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
