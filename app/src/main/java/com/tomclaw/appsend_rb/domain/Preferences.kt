package com.tomclaw.appsend_rb.domain

enum class ThemeMode { SYSTEM, LIGHT, DARK }

interface AppPreferences {

    var themeMode: ThemeMode

    var dynamicColors: Boolean

    var analyticsEnabled: Boolean

    var appFilter: AppFilter

    var exportFormat: ExportFormat

    var fileNameTemplate: String

    /** A document tree chosen by the user, or null for the default Downloads/Apps. */
    var exportFolder: String?

}
