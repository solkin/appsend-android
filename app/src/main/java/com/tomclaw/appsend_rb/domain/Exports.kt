package com.tomclaw.appsend_rb.domain

import io.reactivex.rxjava3.core.Observable
import java.io.InputStream
import java.io.OutputStream

enum class ExportFormat {
    /** Every split in one `.apks` archive. Single-APK apps still produce a plain `.apk`. */
    BUNDLE,

    /** The base APK only. */
    BASE,
}

enum class ExportAction { EXTRACT, SHARE }

enum class StorageAccess { GRANTED, DENIED, PERMANENTLY_DENIED }

enum class ExportStatus { QUEUED, RUNNING, DONE, FAILED, CANCELLED }

data class ExportedFile(
    val uri: String,
    val name: String,
    val size: Long,
    val modified: Long,
)

data class ExportItem(
    val packageName: String,
    val label: String,
    val format: ExportFormat,
    val totalBytes: Long,
    val writtenBytes: Long = 0,
    val status: ExportStatus = ExportStatus.QUEUED,
    val file: ExportedFile? = null,
)

/** An idle manager reports an empty [items] list. */
data class ExportSession(
    val action: ExportAction = ExportAction.EXTRACT,
    val items: List<ExportItem> = emptyList(),
) {
    val active: Boolean
        get() = items.any { it.status == ExportStatus.QUEUED || it.status == ExportStatus.RUNNING }
    val totalBytes: Long get() = items.sumOf { it.totalBytes }
    val writtenBytes: Long get() = items.sumOf { it.writtenBytes }
}

interface ExportManager {

    fun session(): Observable<ExportSession>

    val current: ExportSession

    /** Whether someone has the session on screen, so finishing needs no notification. */
    var observed: Boolean

    /** Ignored while a session is active: the caller shows the running one instead. */
    fun start(apps: List<InstalledApp>, action: ExportAction, format: ExportFormat)

    fun cancel()

    fun retry(packageName: String)

    /** Forgets a finished session. Does nothing while one is active. */
    fun dismiss()

}

interface ExportStorage {

    fun create(fileName: String): ExportTarget

    fun list(): List<ExportedFile>

    fun delete(file: ExportedFile): Boolean

    fun clear()

    fun open(uri: String): InputStream

    /** Whether writing needs the legacy storage permission on this device. */
    fun requiresPermission(): Boolean

    /** The current destination, as the user knows it. */
    fun location(): String

}

interface ExportTarget {

    val output: OutputStream

    /** Publishes the finished file. The target is unusable afterwards. */
    fun commit(): ExportedFile

    /** Removes whatever was written so far. Safe to call after a failed write. */
    fun abort()

}

/** What the manager has to read to export one app. */
data class ApkSources(
    val basePath: String,
    val splitPaths: List<String>,
)

interface ApkSourceProvider {

    fun sources(packageName: String): ApkSources

}

/** Keeps the export session alive while the app is in the background. */
interface ExportForegroundGuard {

    fun ensureRunning()

    /** The session was dealt with, so any notice of its end has lost its meaning. */
    fun clearFinishedNotice()

}

/** Identity of an exported archive, known without installing it. */
data class ArchiveInfo(
    val packageName: String,
    val label: String,
    val versionName: String,
    val versionCode: Long,
)

interface ArchiveInspector {

    /** Null when [file] cannot be read as an app package. */
    fun inspect(file: ExportedFile): ArchiveInfo?

    /** Records what is already known about a file, so it never has to be parsed. */
    fun remember(file: ExportedFile, info: ArchiveInfo)

}

const val APK_MIME_TYPE = "application/vnd.android.package-archive"
const val APK_EXTENSION = ".apk"
const val BUNDLE_EXTENSION = ".apks"
const val BUNDLE_METADATA_ENTRY = "appsend.json"
const val BUNDLE_BASE_ENTRY = "base.apk"
const val DEFAULT_FILE_NAME_TEMPLATE = "{name}_{version}_{code}"

fun isPackageFile(name: String): Boolean =
    name.endsWith(APK_EXTENSION, ignoreCase = true) ||
        name.endsWith(BUNDLE_EXTENSION, ignoreCase = true)
