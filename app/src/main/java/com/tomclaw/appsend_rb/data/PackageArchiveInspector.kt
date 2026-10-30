package com.tomclaw.appsend_rb.data

import android.content.Context
import androidx.core.content.edit
import androidx.core.content.pm.PackageInfoCompat
import com.tomclaw.appsend_rb.domain.ArchiveInfo
import com.tomclaw.appsend_rb.domain.ArchiveInspector
import com.tomclaw.appsend_rb.domain.BUNDLE_EXTENSION
import com.tomclaw.appsend_rb.domain.BUNDLE_METADATA_ENTRY
import com.tomclaw.appsend_rb.domain.ExportStorage
import com.tomclaw.appsend_rb.domain.ExportedFile
import com.tomclaw.appsend_rb.domain.InstalledApp
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Identifies exported files. Results are cached by file identity because the only way
 * to parse an APK that is not on a plain path is to copy it out first.
 */
class PackageArchiveInspector(
    private val context: Context,
    private val storage: ExportStorage,
) : ArchiveInspector {

    private val cache = context.getSharedPreferences("archive_info", Context.MODE_PRIVATE)

    override fun inspect(file: ExportedFile): ArchiveInfo? {
        // A file that failed once fails again, and reading it means copying all of it.
        cache.getString(file.cacheKey(), null)?.let { return parse(it) }
        val info = try {
            storage.open(file.uri).use { input ->
                if (file.name.endsWith(BUNDLE_EXTENSION, ignoreCase = true)) {
                    readBundle(input)
                } else {
                    readApk(input)
                }
            }
        } catch (ignored: Exception) {
            null
        }
        if (info != null) {
            remember(file, info)
        } else {
            cache.edit { putString(file.cacheKey(), UNREADABLE) }
        }
        return info
    }

    override fun remember(file: ExportedFile, info: ArchiveInfo) {
        cache.edit { putString(file.cacheKey(), serialize(info)) }
    }

    private fun ExportedFile.cacheKey() = "$uri|$size|$modified"

    private fun readBundle(input: InputStream): ArchiveInfo? {
        val zip = ZipInputStream(input)
        while (true) {
            val entry = zip.nextEntry ?: return null
            when {
                entry.name == BUNDLE_METADATA_ENTRY -> return parse(zip.readBytes().decodeToString())
                entry.name.startsWith("base") && entry.name.endsWith(".apk") -> return readApk(zip)
            }
        }
    }

    private fun readApk(input: InputStream): ArchiveInfo? {
        val temporary = File.createTempFile("inspect", ".apk", context.cacheDir)
        try {
            temporary.outputStream().use { input.copyTo(it) }
            val packageManager = context.packageManager
            val packageInfo = packageManager.getPackageArchiveInfo(temporary.path, 0) ?: return null
            val application = packageInfo.applicationInfo ?: return null
            application.sourceDir = temporary.path
            application.publicSourceDir = temporary.path
            return ArchiveInfo(
                packageName = packageInfo.packageName,
                label = packageManager.getApplicationLabel(application).toString(),
                versionName = packageInfo.versionName.orEmpty(),
                versionCode = PackageInfoCompat.getLongVersionCode(packageInfo),
            )
        } finally {
            temporary.delete()
        }
    }

}

fun bundleMetadata(app: InstalledApp): String = serialize(
    ArchiveInfo(app.packageName, app.label, app.versionName, app.versionCode)
)

private fun serialize(info: ArchiveInfo): String = JSONObject()
    .put(KEY_PACKAGE, info.packageName)
    .put(KEY_LABEL, info.label)
    .put(KEY_VERSION_NAME, info.versionName)
    .put(KEY_VERSION_CODE, info.versionCode)
    .toString()

private fun parse(json: String): ArchiveInfo? = try {
    val value = JSONObject(json)
    ArchiveInfo(
        packageName = value.getString(KEY_PACKAGE),
        label = value.getString(KEY_LABEL),
        versionName = value.getString(KEY_VERSION_NAME),
        versionCode = value.getLong(KEY_VERSION_CODE),
    )
} catch (ignored: Exception) {
    null
}

private const val UNREADABLE = "{}"
private const val KEY_PACKAGE = "package"
private const val KEY_LABEL = "label"
private const val KEY_VERSION_NAME = "versionName"
private const val KEY_VERSION_CODE = "versionCode"
