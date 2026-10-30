package com.tomclaw.appsend_rb.screen.permissions

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.tomclaw.appsend_rb.databinding.ActivityPermissionsBinding
import com.tomclaw.appsend_rb.getComponent
import com.tomclaw.appsend_rb.screen.permissions.di.PermissionsModule
import javax.inject.Inject

class PermissionsActivity : AppCompatActivity(), PermissionsRouter {

    @Inject
    lateinit var presenter: PermissionsPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        val packageName = requireNotNull(intent.getStringExtra(EXTRA_PACKAGE))
        application.getComponent()
            .permissionsComponent(PermissionsModule(packageName))
            .inject(this)
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val binding = ActivityPermissionsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        presenter.attachView(PermissionsViewImpl(binding))
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

    override fun leave() {
        finish()
    }

}

fun createPermissionsIntent(context: Context, packageName: String): Intent =
    Intent(context, PermissionsActivity::class.java).putExtra(EXTRA_PACKAGE, packageName)

private const val EXTRA_PACKAGE = "package_name"
