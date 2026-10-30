package com.tomclaw.appsend_rb.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.net.toUri
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.domain.APK_EXTENSION
import com.tomclaw.appsend_rb.domain.APK_MIME_TYPE
import com.tomclaw.appsend_rb.domain.ExportedFile

fun Context.openStorePage(packageName: String) {
    openUri("market://details?id=$packageName", "https://play.google.com/store/apps/details?id=$packageName")
}

fun Context.openDeveloperPage() {
    openUri(
        "market://search?q=pub:TomClaw+Software",
        "https://play.google.com/store/apps/developer?id=TomClaw+Software",
    )
}

private fun Context.openUri(uri: String, fallback: String) {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, uri.toUri()))
    } catch (ignored: ActivityNotFoundException) {
        startActivity(Intent(Intent.ACTION_VIEW, fallback.toUri()))
    }
}

/** Returns false when the package has nothing to launch. */
fun Context.launchPackage(packageName: String): Boolean {
    val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return false
    startActivity(intent)
    return true
}

fun Context.requestUninstall(packageName: String) {
    startActivity(Intent(Intent.ACTION_DELETE, "package:$packageName".toUri()))
}

fun Context.openAppSystemSettings(packageName: String) {
    startActivity(
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", packageName, null))
    )
}

fun Context.shareFiles(files: List<ExportedFile>) {
    if (files.isEmpty()) return
    val uris = files.map { it.uri.toUri() }
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).putExtra(Intent.EXTRA_STREAM, uris.first())
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
    }
    // A set is not an installable package, and must not present itself as one.
    intent.type = if (files.all { it.name.endsWith(APK_EXTENSION, ignoreCase = true) }) {
        APK_MIME_TYPE
    } else {
        "application/octet-stream"
    }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    startActivity(Intent.createChooser(intent, getText(R.string.send_to)))
}
