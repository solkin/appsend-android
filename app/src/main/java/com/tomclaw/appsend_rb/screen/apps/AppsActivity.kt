package com.tomclaw.appsend_rb.screen.apps

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.Intent.ACTION_SEND
import android.content.Intent.ACTION_SEND_MULTIPLE
import android.content.Intent.ACTION_VIEW
import android.content.Intent.EXTRA_STREAM
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.Intent.FLAG_ACTIVITY_NO_ANIMATION
import android.content.Intent.createChooser
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.androidisland.ezpermission.EzPermission
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.SettingsActivity
import com.tomclaw.appsend_rb.dto.AppEntity
import com.tomclaw.appsend_rb.getComponent
import com.tomclaw.appsend_rb.screen.about.createAboutActivityIntent
import com.tomclaw.appsend_rb.screen.apps.di.AppsModule
import com.tomclaw.appsend_rb.screen.details.createAppDetailsActivityIntent
import com.tomclaw.appsend_rb.screen.permissions.createPermissionsActivityIntent
import com.tomclaw.appsend_rb.util.ZipParcelable
import com.tomclaw.appsend_rb.util.grantProviderUriPermission
import com.tomclaw.appsend_rb.util.installKnopochki
import javax.inject.Inject


class AppsActivity : AppCompatActivity(), AppsPresenter.AppsRouter {

    @Inject
    lateinit var presenter: AppsPresenter

    @Inject
    lateinit var preferences: PreferencesProvider

    private var view: AppsViewImpl? = null

    private var isDarkTheme: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val compressedPresenterState: ZipParcelable? = savedInstanceState?.getParcelable(KEY_PRESENTER_STATE)
        val presenterState: Bundle? = compressedPresenterState?.restore()
        // Ensure nested app Parcelables (AppEntity) resolve against the app loader.
        presenterState?.classLoader = classLoader
        application.getComponent()
            .appsComponent(AppsModule(this, presenterState))
            .inject(activity = this)

        isDarkTheme = preferences.isDarkTheme()
        super.onCreate(savedInstanceState)
        val knopochkiView = installKnopochki()
        val view = AppsViewImpl(knopochkiView)
        this.view = view
        presenter.attachView(view)

        // With targetSdk 36 Back is delivered through the dispatcher and a plain
        // onBackPressed() override is never called — which would drop overlay,
        // search and selection-mode handling and just close the screen.
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (view.onBackPressed()) return
                presenter.onBackPressed()
            }
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
        outState.putParcelable(KEY_PRESENTER_STATE, ZipParcelable(presenter.saveState()))
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
        val intent = createAboutActivityIntent(this)
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

    override fun showAppDetails(entity: AppEntity) {
        val intent = createAppDetailsActivityIntent(this, entity)
        startActivity(intent)
    }

    override fun showRequestedPermissions(permissions: List<String>) {
        val intent = createPermissionsActivityIntent(context = this, permissions)
        startActivity(intent)
    }

    override fun runAppUninstall(packageName: String) {
        val packageUri = Uri.parse("package:$packageName")
        val uninstallIntent = Intent(Intent.ACTION_DELETE, packageUri)
        startActivity(uninstallIntent)
    }

    override fun shareApk(uri: Uri) {
        val intent = Intent().apply {
            action = ACTION_SEND
            putExtra(EXTRA_STREAM, uri)
            type = APK_MIME_TYPE
            addFlags(FLAG_GRANT_READ_URI_PERMISSION)
        }
        grantProviderUriPermission(this, uri, intent)
        startActivity(createChooser(intent, resources.getText(R.string.send_to)))
    }

    override fun shareApks(uris: List<Uri>) {
        val intent = Intent().apply {
            action = ACTION_SEND_MULTIPLE
            putParcelableArrayListExtra(EXTRA_STREAM, ArrayList(uris))
            type = APK_MIME_TYPE
            addFlags(FLAG_GRANT_READ_URI_PERMISSION)
        }
        uris.forEach { uri -> grantProviderUriPermission(this, uri, intent) }
        startActivity(createChooser(intent, resources.getText(R.string.send_to)))
    }

    override fun requestPermissions(onGranted: () -> Unit, onDenied: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            onGranted()
        } else {
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

}

private const val KEY_PRESENTER_STATE = "presenter_state"
private const val REQUEST_UPDATE_SETTINGS = 6
