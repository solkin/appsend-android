package com.tomclaw.appsend_rb.screen.details

import android.content.Context
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.dto.AppEntity
import com.tomclaw.appsend_rb.util.formatBytesToString
import java.text.DateFormat

interface AppDetailsResourceProvider {

    fun formatVersion(entity: AppEntity): String

    fun formatSize(bytes: Long): String

    fun formatTime(time: Long): String

    fun formatPermissionsCount(count: Int): String

    fun formatStatus(system: Boolean, split: Boolean): String

}

class AppDetailsResourceProviderImpl(
    private val context: Context,
    private val dateFormat: DateFormat
) : AppDetailsResourceProvider {

    override fun formatVersion(entity: AppEntity): String {
        return context.getString(
            R.string.app_details_version_value,
            entity.versionName.ifEmpty { context.getString(R.string.app_details_unknown_value) },
            entity.versionCode
        )
    }

    override fun formatSize(bytes: Long): String {
        return formatBytesToString(context.resources, bytes)
    }

    override fun formatTime(time: Long): String {
        return dateFormat.format(time)
    }

    override fun formatPermissionsCount(count: Int): String {
        return context.resources.getQuantityString(
            R.plurals.app_details_permissions_count,
            count,
            count
        )
    }

    override fun formatStatus(system: Boolean, split: Boolean): String {
        val appType = context.getString(
            if (system) R.string.app_details_status_system else R.string.app_details_status_user
        )
        val apkType = context.getString(
            if (split) R.string.app_details_status_split else R.string.app_details_status_single
        )
        return context.getString(R.string.app_details_status_value, appType, apkType)
    }

}
