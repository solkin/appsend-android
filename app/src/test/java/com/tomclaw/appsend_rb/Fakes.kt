package com.tomclaw.appsend_rb

import com.tomclaw.appsend_rb.domain.AppFilter
import com.tomclaw.appsend_rb.domain.AppPreferences
import com.tomclaw.appsend_rb.domain.DEFAULT_FILE_NAME_TEMPLATE
import com.tomclaw.appsend_rb.domain.ExportFormat
import com.tomclaw.appsend_rb.domain.InstalledApp
import com.tomclaw.appsend_rb.domain.SortOrder
import com.tomclaw.appsend_rb.domain.ThemeMode
import com.tomclaw.appsend_rb.domain.Analytics
import com.tomclaw.appsend_rb.domain.SchedulersFactory
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers

object ImmediateSchedulers : SchedulersFactory {

    override fun io(): Scheduler = Schedulers.trampoline()

    override fun mainThread(): Scheduler = Schedulers.trampoline()

}

object NoAnalytics : Analytics {

    override fun register() = Unit

    override fun trackEvent(name: String) = Unit

    override fun trackEvent(name: String, tags: Map<String, String>) = Unit

    override fun trackEvent(name: String, tags: Map<String, String>, fields: Map<String, Double>) = Unit

    override fun trackException(throwable: Throwable, context: Map<String, String>) = Unit

}

class FakePreferences : AppPreferences {

    override var themeMode = ThemeMode.SYSTEM
    override var dynamicColors = false
    override var analyticsEnabled = true
    override var appFilter = AppFilter(SortOrder.NAME, showSystem = false, splitOnly = false, runnableOnly = false)
    override var exportFormat = ExportFormat.BUNDLE
    override var fileNameTemplate = DEFAULT_FILE_NAME_TEMPLATE
    override var exportFolder: String? = null

}

fun app(
    label: String = "App",
    packageName: String = "com.example.${label.lowercase()}",
    version: String = "1.0",
    code: Long = 1,
    baseSize: Long = 10,
    totalSize: Long = baseSize,
    splits: Int = 0,
    installed: Long = 0,
    updated: Long = 0,
    system: Boolean = false,
    launchable: Boolean = true,
) = InstalledApp(label, packageName, version, code, baseSize, totalSize, splits, installed, updated, system, launchable)
