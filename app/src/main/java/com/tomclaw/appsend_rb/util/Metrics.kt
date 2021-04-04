package com.tomclaw.appsend_rb.util

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.TextUtils
import com.microsoft.appcenter.AppCenter
import com.microsoft.appcenter.analytics.Analytics
import com.microsoft.appcenter.crashes.Crashes

fun registerAppCenter(application: Application) {
    getAppIdentifier(application)?.let { appIdentifier ->
        AppCenter.start(application, appIdentifier, Analytics::class.java, Crashes::class.java)
    }
}

fun getAppIdentifier(context: Context): String? {
    val appIdentifier = getManifestString(context, APP_IDENTIFIER_KEY)
    require(!TextUtils.isEmpty(appIdentifier)) { "AppCenter app identifier was not configured correctly in manifest or build configuration." }
    return appIdentifier
}

fun getManifestString(context: Context, key: String?): String? {
    return getBundle(context).getString(key)
}

private fun getBundle(context: Context): Bundle {
    return try {
        context.packageManager.getApplicationInfo(
            context.packageName,
            PackageManager.GET_META_DATA
        ).metaData
    } catch (e: PackageManager.NameNotFoundException) {
        throw RuntimeException(e)
    }
}

private const val APP_IDENTIFIER_KEY = "com.microsoft.appcenter.android.appIdentifier"
