package com.tomclaw.appsend_rb.screen.about

import android.content.pm.PackageManager
import android.content.res.Resources
import com.tomclaw.appsend_rb.R

interface AboutResourceProvider {

    fun provideVersion(): String

}

class AboutResourceProviderImpl(
    private val packageName: String,
    private val packageManager: PackageManager,
    private val resources: Resources
) : AboutResourceProvider {

    override fun provideVersion(): String {
        try {
            val info = packageManager.getPackageInfo(packageName, 0)
            return resources.getString(R.string.app_version, info.versionName)
        } catch (ignored: PackageManager.NameNotFoundException) {
        }
        return ""
    }

}