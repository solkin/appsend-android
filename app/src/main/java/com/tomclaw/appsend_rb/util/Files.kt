package com.tomclaw.appsend_rb.util

import com.tomclaw.appsend_rb.dto.AppEntity

fun getApkPrefix(item: AppEntity): String {
    return escapeFileSymbols(item.label + "-" + item.versionCode)
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

val RESERVED_CHARS = arrayOf("|", "\\", "?", "*", "<", "\"", ":", ">")
