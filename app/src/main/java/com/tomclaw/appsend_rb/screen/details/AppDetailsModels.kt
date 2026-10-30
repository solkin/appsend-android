package com.tomclaw.appsend_rb.screen.details

import android.content.Context
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.domain.InstalledApp
import com.tomclaw.appsend_rb.domain.ValueFormatter

data class InfoRow(val label: String, val value: String)

data class AppDetailsViewModel(
    val packageName: String,
    val label: String,
    val canOpen: Boolean,
    val rows: List<InfoRow>,
    val permissions: String,
    val hasPermissions: Boolean,
)

interface AppDetailsResources {

    fun versionLabel(): String

    fun sizeLabel(): String

    fun statusLabel(): String

    fun installerLabel(): String

    fun installedLabel(): String

    fun updatedLabel(): String

    fun sdkLabel(): String

    fun signatureLabel(): String

    fun pathLabel(): String

    fun version(app: InstalledApp): String

    fun size(app: InstalledApp): String

    fun status(app: InstalledApp): String

    fun date(time: Long): String

    fun sdk(min: Int, target: Int): String

    fun permissionsSummary(total: Int, dangerous: Int): String

}

class AppDetailsResourcesImpl(
    private val context: Context,
    private val formatter: ValueFormatter,
) : AppDetailsResources {

    override fun versionLabel() = context.getString(R.string.app_details_version)

    override fun sizeLabel() = context.getString(R.string.app_details_size)

    override fun statusLabel() = context.getString(R.string.app_details_status)

    override fun installerLabel() = context.getString(R.string.app_details_installer)

    override fun installedLabel() = context.getString(R.string.app_details_installed)

    override fun updatedLabel() = context.getString(R.string.app_details_updated)

    override fun sdkLabel() = context.getString(R.string.app_details_sdk)

    override fun signatureLabel() = context.getString(R.string.app_details_signature)

    override fun pathLabel() = context.getString(R.string.app_details_path)

    override fun version(app: InstalledApp) = context.getString(
        R.string.app_details_version_value,
        app.versionName.ifEmpty { context.getString(R.string.app_details_unknown_value) },
        app.versionCode,
    )

    override fun size(app: InstalledApp): String {
        val total = formatter.bytes(app.totalSize)
        if (!app.split) return total
        return context.resources.getQuantityString(
            R.plurals.app_details_size_split,
            app.splitCount,
            total,
            formatter.bytes(app.baseSize),
            app.splitCount,
            formatter.bytes(app.totalSize - app.baseSize),
        )
    }

    override fun status(app: InstalledApp) = context.getString(
        R.string.app_details_status_value,
        context.getString(
            if (app.system) R.string.app_details_status_system else R.string.app_details_status_user
        ),
        context.getString(
            if (app.split) R.string.app_details_status_split else R.string.app_details_status_single
        ),
    )

    override fun date(time: Long) = formatter.date(time)

    override fun sdk(min: Int, target: Int) = context.getString(R.string.app_details_sdk_value, min, target)

    override fun permissionsSummary(total: Int, dangerous: Int) =
        context.getString(R.string.app_details_permissions_summary, total, dangerous)

}
