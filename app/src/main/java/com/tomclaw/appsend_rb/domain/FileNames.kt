package com.tomclaw.appsend_rb.domain

const val TOKEN_NAME = "{name}"
const val TOKEN_PACKAGE = "{package}"
const val TOKEN_VERSION = "{version}"
const val TOKEN_CODE = "{code}"

private val UNSAFE_CHARS = Regex("""[\\/:*?"<>|\p{Cntrl}]""")

/** Builds an export file name from a template made of the `TOKEN_*` values and plain text. */
fun exportFileName(template: String, app: InstalledApp, format: ExportFormat): String {
    val stem = template
        .replace(TOKEN_NAME, app.label)
        .replace(TOKEN_PACKAGE, app.packageName)
        .replace(TOKEN_VERSION, app.versionName)
        .replace(TOKEN_CODE, app.versionCode.toString())
        .replace(UNSAFE_CHARS, "_")
        .trim()
        .trim('.')
        .ifEmpty { app.packageName }
    val extension = if (app.split && format == ExportFormat.BUNDLE) BUNDLE_EXTENSION else APK_EXTENSION
    return stem + extension
}
