package com.tomclaw.appsend_rb.screen.apps.adapter.app

/**
 * A single row in the apps list. Rendered by Knopochki, so it carries only
 * display-ready strings; the icon is loaded on demand from [packageName].
 */
data class AppItem(
    val id: Long,
    val packageName: String,
    val name: String,
    val size: String,
    val firstInstallTime: String,
    val lastUpdateTime: String,
    val versionName: String,
    val versionCode: Long,
    val newApp: Boolean,
    val selectable: Boolean,
    val selected: Boolean
)
