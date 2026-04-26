package com.tomclaw.appsend_rb.screen.details

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.Intent.CATEGORY_DEFAULT
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.dto.AppEntity
import com.tomclaw.appsend_rb.getComponent
import com.tomclaw.appsend_rb.screen.details.di.AppDetailsModule
import com.tomclaw.appsend_rb.screen.permissions.createPermissionsActivityIntent
import com.tomclaw.appsend_rb.screen.apps.PreferencesProvider
import com.tomclaw.appsend_rb.util.updateStatusBar
import com.tomclaw.appsend_rb.util.updateTheme
import javax.inject.Inject

class AppDetailsActivity : AppCompatActivity(), AppDetailsPresenter.AppDetailsRouter {

    @Inject
    lateinit var presenter: AppDetailsPresenter

    @Inject
    lateinit var preferences: PreferencesProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        val entity = intent.getAppEntity()
            ?: throw IllegalArgumentException("App entity is required")
        application.getComponent()
            .appDetailsComponent(AppDetailsModule(this, entity))
            .inject(activity = this)

        updateTheme(preferences)
        updateStatusBar()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.app_details_activity)

        val view = AppDetailsViewImpl(window.decorView)
        presenter.attachView(view)
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

    @Deprecated("Deprecated in Java")
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        super.onBackPressed()
        presenter.onBackPressed()
    }

    override fun copyPackageName(packageName: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_details_package), packageName))
    }

    override fun showRequestedPermissions(permissions: List<String>) {
        startActivity(createPermissionsActivityIntent(this, permissions))
    }

    override fun showSystemDetails(packageName: String) {
        val intent = Intent()
            .setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .addCategory(CATEGORY_DEFAULT)
            .setData(android.net.Uri.parse("package:$packageName"))
            .addFlags(FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun leaveScreen() {
        finish()
    }

}

fun createAppDetailsActivityIntent(
    context: Context,
    entity: AppEntity
): Intent = Intent(context, AppDetailsActivity::class.java)
    .putExtra(EXTRA_APP_ENTITY, entity)

private fun Intent.getAppEntity(): AppEntity? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(EXTRA_APP_ENTITY, AppEntity::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(EXTRA_APP_ENTITY)
    }
}

private const val EXTRA_APP_ENTITY = "app_entity"
