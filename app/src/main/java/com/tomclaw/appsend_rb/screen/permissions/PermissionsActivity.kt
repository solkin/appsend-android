package com.tomclaw.appsend_rb.screen.permissions

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.tomclaw.appsend_rb.getComponent
import com.tomclaw.appsend_rb.screen.permissions.di.PermissionsModule
import com.tomclaw.appsend_rb.util.installKnopochki
import javax.inject.Inject

class PermissionsActivity : AppCompatActivity(), PermissionsPresenter.PermissionsRouter {

    @Inject
    lateinit var presenter: PermissionsPresenter

    override fun onCreate(savedInstanceState: Bundle?) {
        val permissions = intent?.getStringArrayListExtra(EXTRA_PERMISSIONS)
            ?: throw IllegalArgumentException("Permissions list is required")
        val presenterState = savedInstanceState?.getBundle(KEY_PRESENTER_STATE)
        application.getComponent()
            .permissionsComponent(PermissionsModule(this, permissions, presenterState))
            .inject(activity = this)

        super.onCreate(savedInstanceState)
        val knopochkiView = installKnopochki()
        val view = PermissionsViewImpl(knopochkiView)
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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBundle(KEY_PRESENTER_STATE, presenter.saveState())
    }

    override fun leaveScreen() {
        finish()
    }

}

fun createPermissionsActivityIntent(
    context: Context,
    permissions: List<String>
): Intent = Intent(context, PermissionsActivity::class.java)
    .putStringArrayListExtra(EXTRA_PERMISSIONS, ArrayList(permissions))

private const val EXTRA_PERMISSIONS = "permissions"
private const val KEY_PRESENTER_STATE = "presenter_state"
