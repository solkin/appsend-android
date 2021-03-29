package com.tomclaw.appsend_rb.screen.apps

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.ACTION_VIEW
import android.content.Intent.CATEGORY_DEFAULT
import android.content.Intent.EXTRA_STREAM
import android.content.Intent.EXTRA_TEXT
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION
import android.content.Intent.createChooser
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.androidisland.ezpermission.EzPermission
import com.avito.konveyor.ItemBinder
import com.avito.konveyor.adapter.AdapterPresenter
import com.avito.konveyor.adapter.SimpleRecyclerAdapter
import com.tomclaw.appsend_rb.AboutActivity
import com.tomclaw.appsend_rb.PermissionsActivity
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.SettingsActivity
import com.tomclaw.appsend_rb.getComponent
import com.tomclaw.appsend_rb.screen.apps.di.AppsModule
import com.tomclaw.appsend_rb.util.grantProviderUriPermission
import com.tomclaw.appsend_rb.util.isFileProviderUri
import com.tomclaw.appsend_rb.util.updateStatusBar
import com.tomclaw.appsend_rb.util.updateTheme
import java.io.File
import java.util.ArrayList
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
            val intent = intent.addFlags(FLAG_ACTIVITY_NO_ANIMATION)
            finish()
            startActivity(intent)
        } else {
            presenter.onResume()
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_UPDATE_SETTINGS) {
            if (resultCode == SettingsActivity.RESULT_UPDATE) {
                presenter.invalidateAppsList()
            }
        }
    }

    override fun showPrefsScreen() {
        val intent = Intent(
            this,
            SettingsActivity::class.java
        )
        startActivityForResult(intent, REQUEST_UPDATE_SETTINGS)
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
            startActivity(Intent(ACTION_VIEW, Uri.parse("market://details?id=$packageName")))
        } catch (ex: ActivityNotFoundException) {
            startActivity(
                Intent(
                    ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                )
            )
        }
    }

    override fun showAppDetails(packageName: String) {
        val intent = Intent()
            .setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .addCategory(CATEGORY_DEFAULT)
            .setData(Uri.parse("package:$packageName"))
            .addFlags(FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    override fun showRequestedPermissions(permissions: List<String>) {
        val intent = Intent(this, PermissionsActivity::class.java)
            .putStringArrayListExtra(
                PermissionsActivity.EXTRA_PERMISSIONS,
                ArrayList(permissions)
            )
        startActivity(intent)
    }

    override fun runAppUninstall(packageName: String) {
        val packageUri = Uri.parse("package:$packageName")
        val uninstallIntent = Intent(Intent.ACTION_DELETE, packageUri)
        startActivity(uninstallIntent)
    }

    override fun shareApk(file: File) {
        val uri: Uri = if (isFileProviderUri()) {
            FileProvider.getUriForFile(this, "$packageName.provider", file)
        } else {
            Uri.fromFile(file)
        }
        val intent = Intent().apply {
            action = ACTION_SEND
            putExtra(EXTRA_TEXT, file.name)
            putExtra(EXTRA_STREAM, uri)
            type = "application/zip"
        }
        grantProviderUriPermission(this, uri, intent)
        startActivity(createChooser(intent, resources.getText(R.string.send_to)))
    }

    override fun requestPermissions(onGranted: () -> Unit, onDenied: () -> Unit) {
        EzPermission.with(this)
            .permissions(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .request { granted, denied, permanentlyDenied ->
                if (granted.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    onGranted()
                } else {
                    onDenied()
                }
            }
    }

}

private const val KEY_PRESENTER_STATE = "presenter_state"
private const val REQUEST_UPDATE_SETTINGS = 6
