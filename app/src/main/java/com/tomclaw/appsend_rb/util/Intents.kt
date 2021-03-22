package com.tomclaw.appsend_rb.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build

fun grantProviderUriPermission(context: Context, uri: Uri, intent: Intent) {
    if (isFileProviderUri()) {
        val resInfoList =
            context.packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        for (resolveInfo in resInfoList) {
            val packageName = resolveInfo.activityInfo.packageName
            context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}

fun isFileProviderUri(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
}