package com.tomclaw.appsend_rb.util

import android.content.res.Resources
import android.os.Environment
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.dto.AppEntity
import java.io.File

fun getApkPrefix(item: AppEntity): String {
    return escapeFileSymbols(item.packageName + "_" + item.versionName)
}

fun getApkSuffix(): String {
    return ".apk"
}

fun getApkName(item: AppEntity): String {
    return getApkPrefix(item) + getApkSuffix()
}

fun escapeFileSymbols(name: String): String {
    var escaped = name
    for (symbol in RESERVED_CHARS) {
        escaped = escaped.replace(symbol[0], '_')
    }
    return escaped
}

fun formatBytesToString(resources: Resources, bytes: Long): String {
    return when {
        bytes < 1024 -> {
            resources.getString(R.string.bytes, bytes)
        }
        bytes < 1024 * 1024 -> {
            resources.getString(R.string.kibibytes, bytes / 1024.0f)
        }
        bytes < 1024 * 1024 * 1024 -> {
            resources.getString(R.string.mibibytes, bytes / 1024.0f / 1024.0f)
        }
        else -> {
            resources.getString(R.string.gigibytes, bytes / 1024.0f / 1024.0f / 1024.0f)
        }
    }
}

@Suppress("DEPRECATION")
fun getExternalDirectory(): File {
    val externalDirectory =
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val directory = File(externalDirectory, "Apps")
    directory.mkdirs()
    return directory
}

val RESERVED_CHARS = arrayOf("|", "\\", "?", "*", "<", "\"", ":", ">")
