package com.tomclaw.appsend_rb.screen.details

import android.view.View
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import com.google.android.material.snackbar.Snackbar
import com.jakewharton.rxrelay3.PublishRelay
import com.tomclaw.appsend_rb.R
import io.reactivex.rxjava3.core.Observable

interface AppDetailsView {

    fun navigationClicks(): Observable<Unit>

    fun copyPackageClicks(): Observable<Unit>

    fun permissionsClicks(): Observable<Unit>

    fun systemDetailsClicks(): Observable<Unit>

    fun setDetails(details: AppDetailsUi)

    fun showPackageNameCopied()

}

class AppDetailsViewImpl(
    private val view: View
) : AppDetailsView {

    private val toolbar: Toolbar = view.findViewById(R.id.toolbar)
    private val label: TextView = view.findViewById(R.id.app_label)
    private val packageName: TextView = view.findViewById(R.id.package_name)
    private val version: TextView = view.findViewById(R.id.version)
    private val installTime: TextView = view.findViewById(R.id.install_time)
    private val updateTime: TextView = view.findViewById(R.id.update_time)
    private val size: TextView = view.findViewById(R.id.size)
    private val path: TextView = view.findViewById(R.id.path)
    private val permissions: TextView = view.findViewById(R.id.permissions)
    private val status: TextView = view.findViewById(R.id.status)
    private val copyPackage: View = view.findViewById(R.id.copy_package)
    private val openPermissions: View = view.findViewById(R.id.open_permissions)
    private val openSystemDetails: View = view.findViewById(R.id.open_system_details)

    private val navigationRelay = PublishRelay.create<Unit>()
    private val copyPackageRelay = PublishRelay.create<Unit>()
    private val permissionsRelay = PublishRelay.create<Unit>()
    private val systemDetailsRelay = PublishRelay.create<Unit>()

    init {
        toolbar.setTitle(R.string.app_details)
        toolbar.setNavigationOnClickListener { navigationRelay.accept(Unit) }
        copyPackage.setOnClickListener { copyPackageRelay.accept(Unit) }
        openPermissions.setOnClickListener { permissionsRelay.accept(Unit) }
        openSystemDetails.setOnClickListener { systemDetailsRelay.accept(Unit) }
    }

    override fun navigationClicks(): Observable<Unit> = navigationRelay

    override fun copyPackageClicks(): Observable<Unit> = copyPackageRelay

    override fun permissionsClicks(): Observable<Unit> = permissionsRelay

    override fun systemDetailsClicks(): Observable<Unit> = systemDetailsRelay

    override fun setDetails(details: AppDetailsUi) {
        label.text = details.label
        packageName.text = details.packageName
        version.text = details.version
        installTime.text = details.installTime
        updateTime.text = details.updateTime
        size.text = details.size
        path.text = details.path
        permissions.text = details.permissions
        status.text = details.status
        openPermissions.isEnabled = details.permissionsEnabled
        openPermissions.alpha = if (details.permissionsEnabled) ENABLED_ALPHA else DISABLED_ALPHA
    }

    override fun showPackageNameCopied() {
        Snackbar.make(view, R.string.app_details_package_copied, Snackbar.LENGTH_LONG).show()
    }

}

private const val ENABLED_ALPHA = 1.0f
private const val DISABLED_ALPHA = 0.45f
