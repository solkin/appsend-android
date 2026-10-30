package com.tomclaw.appsend_rb.domain

private val UNSAFE_CHARS = Regex("""[\\/:*?"<>|\p{Cntrl}]""")

/** Builds an export file name. Tokens: `{name}`, `{package}`, `{version}`, `{code}`. */
fun exportFileName(template: String, app: InstalledApp, format: ExportFormat): String {
    val stem = template
        .replace("{name}", app.label)
        .replace("{package}", app.packageName)
        .replace("{version}", app.versionName)
        .replace("{code}", app.versionCode.toString())
        .replace(UNSAFE_CHARS, "_")
        .trim()
        .trim('.')
        .ifEmpty { app.packageName }
    val extension = if (app.split && format == ExportFormat.BUNDLE) BUNDLE_EXTENSION else APK_EXTENSION
    return stem + extension
}
