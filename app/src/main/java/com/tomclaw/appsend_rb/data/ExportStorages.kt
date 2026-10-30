package com.tomclaw.appsend_rb.data

import android.Manifest
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.domain.APK_MIME_TYPE
import com.tomclaw.appsend_rb.domain.AppPreferences
import com.tomclaw.appsend_rb.domain.ExportStorage
import com.tomclaw.appsend_rb.domain.ExportTarget
import com.tomclaw.appsend_rb.domain.ExportedFile
import com.tomclaw.appsend_rb.domain.isPackageFile
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/** Picks the place to export to on every call, as the user can change it at any time. */
class SelectableExportStorage(
    private val context: Context,
    private val preferences: AppPreferences,
) : ExportStorage {

    private fun current(): ExportStorage {
        val folder = preferences.exportFolder
        return when {
            folder != null -> DocumentTreeExportStorage(context, folder.toUri())
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> MediaStoreExportStorage(context)
            else -> LegacyExportStorage(context)
        }
    }

    override fun create(fileName: String): ExportTarget = current().create(fileName)

    override fun list(): List<ExportedFile> = current().list()

    override fun delete(file: ExportedFile): Boolean = current().delete(file)

    override fun clear() = current().clear()

    override fun open(uri: String): InputStream =
        context.contentResolver.openInputStream(uri.toUri())
            ?: throw IOException("Unable to open $uri")

    override fun requiresPermission(): Boolean = current().requiresPermission()

    override fun location(): String = current().location()

}

@RequiresApi(Build.VERSION_CODES.Q)
private class MediaStoreExportStorage(private val context: Context) : ExportStorage {

    private val resolver: ContentResolver = context.contentResolver
    private val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    private val relativePath = Environment.DIRECTORY_DOWNLOADS + File.separator + EXPORT_DIR + File.separator

    override fun create(fileName: String): ExportTarget {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeTypeOf(fileName))
            put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(collection, values) ?: throw IOException("Unable to create $fileName")
        return object : ExportTarget {
            override val output: OutputStream = try {
                resolver.openOutputStream(uri) ?: throw IOException("Unable to open $uri")
            } catch (error: Exception) {
                resolver.delete(uri, null, null)
                throw error
            }

            override fun commit(): ExportedFile {
                val published = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                resolver.update(uri, published, null, null)
                return query(uri, null, null).firstOrNull()
                    ?: throw IOException("Lost $uri right after writing it")
            }

            override fun abort() {
                resolver.delete(uri, null, null)
            }
        }
    }

    override fun list(): List<ExportedFile> = query(
        collection,
        "${MediaStore.MediaColumns.RELATIVE_PATH} = ?",
        arrayOf(relativePath),
    ).filter { isPackageFile(it.name) }

    private fun query(uri: Uri, selection: String?, args: Array<String>?): List<ExportedFile> {
        val columns = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED,
        )
        val files = ArrayList<ExportedFile>()
        resolver.query(uri, columns, selection, args, null)?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getLong(0)
                files += ExportedFile(
                    uri = if (uri == collection) ContentUris.withAppendedId(collection, id).toString() else uri.toString(),
                    name = cursor.getString(1).orEmpty(),
                    size = cursor.getLong(2),
                    modified = cursor.getLong(3) * 1000,
                )
            }
        }
        return files
    }

    override fun delete(file: ExportedFile): Boolean =
        resolver.delete(file.uri.toUri(), null, null) > 0

    override fun clear() = deleteAll()

    override fun open(uri: String): InputStream =
        resolver.openInputStream(uri.toUri()) ?: throw IOException("Unable to open $uri")

    override fun requiresPermission(): Boolean = false

    override fun location(): String = context.getString(R.string.export_folder_default)

}

private class LegacyExportStorage(private val context: Context) : ExportStorage {

    @Suppress("DEPRECATION")
    private val directory = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
        EXPORT_DIR,
    )

    override fun create(fileName: String): ExportTarget {
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Unable to create $directory")
        }
        // A process killed mid-write leaves a partial file, and it must never pass for a
        // finished one. Only one export runs at a time, so any partial here is a leftover.
        directory.listFiles { file -> isOwnPartial(file.name) }?.forEach { it.delete() }
        val destination = uniqueFile(fileName)
        val partial = File(directory, destination.name + PARTIAL_SUFFIX)
        return object : ExportTarget {
            override val output: OutputStream = partial.outputStream()

            override fun commit(): ExportedFile {
                if (!partial.renameTo(destination)) throw IOException("Unable to finish $destination")
                return toExportedFile(destination)
            }

            override fun abort() {
                partial.delete()
            }
        }
    }

    private fun uniqueFile(fileName: String): File =
        File(directory, uniqueName(fileName) { File(directory, it).exists() })

    private fun toExportedFile(file: File) = ExportedFile(
        uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file).toString(),
        name = file.name,
        size = file.length(),
        modified = file.lastModified(),
    )

    override fun list(): List<ExportedFile> =
        directory.listFiles { file -> file.isFile && isPackageFile(file.name) }
            .orEmpty()
            .map(::toExportedFile)

    override fun delete(file: ExportedFile): Boolean = File(directory, file.name).delete()

    override fun clear() = deleteAll()

    override fun open(uri: String): InputStream =
        context.contentResolver.openInputStream(uri.toUri()) ?: throw IOException("Unable to open $uri")

    override fun requiresPermission(): Boolean = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
    ) != PackageManager.PERMISSION_GRANTED

    override fun location(): String = context.getString(R.string.export_folder_default)

}

private class DocumentTreeExportStorage(
    private val context: Context,
    private val treeUri: Uri,
) : ExportStorage {

    private val tree = DocumentFile.fromTreeUri(context, treeUri)
        ?: throw IOException("Unable to open $treeUri")

    override fun create(fileName: String): ExportTarget {
        tree.listFiles()
            .filter { isOwnPartial(it.name.orEmpty()) }
            .forEach { it.delete() }
        val finalName = uniqueName(fileName) { tree.findFile(it) != null }
        // A provider that cannot rename gets the final name straight away: a process killed
        // mid-write is then the one thing that can leave a cut-off file behind.
        var document = createDocument(finalName + PARTIAL_SUFFIX)
        val renamable = supportsRename(document)
        if (!renamable) {
            document.delete()
            document = createDocument(finalName)
        }
        return object : ExportTarget {
            override val output: OutputStream = try {
                context.contentResolver.openOutputStream(document.uri)
                    ?: throw IOException("Unable to open ${document.uri}")
            } catch (error: Exception) {
                document.delete()
                throw error
            }

            override fun commit(): ExportedFile {
                if (renamable && !document.renameTo(finalName)) {
                    throw IOException("Unable to finish $finalName")
                }
                return toExportedFile(document)
            }

            override fun abort() {
                document.delete()
            }
        }
    }

    // The MIME type would make some providers append a second extension to `.apks`.
    private fun createDocument(name: String): DocumentFile =
        tree.createFile("application/octet-stream", name) ?: throw IOException("Unable to create $name")

    private fun supportsRename(document: DocumentFile): Boolean =
        context.contentResolver.query(
            document.uri,
            arrayOf(DocumentsContract.Document.COLUMN_FLAGS),
            null,
            null,
            null,
        )?.use { cursor ->
            cursor.moveToFirst() &&
                cursor.getInt(0) and DocumentsContract.Document.FLAG_SUPPORTS_RENAME != 0
        } ?: false

    private fun toExportedFile(document: DocumentFile) = ExportedFile(
        uri = document.uri.toString(),
        name = document.name.orEmpty(),
        size = document.length(),
        modified = document.lastModified(),
    )

    override fun list(): List<ExportedFile> = tree.listFiles()
        .filter { it.isFile && isPackageFile(it.name.orEmpty()) }
        .map(::toExportedFile)

    override fun delete(file: ExportedFile): Boolean =
        DocumentFile.fromSingleUri(context, file.uri.toUri())?.delete() ?: false

    override fun clear() = deleteAll()

    override fun open(uri: String): InputStream =
        context.contentResolver.openInputStream(uri.toUri()) ?: throw IOException("Unable to open $uri")

    override fun requiresPermission(): Boolean = false

    /** Read from the address alone: a provider may be a slow cloud one, and this runs on main. */
    override fun location(): String =
        DocumentsContract.getTreeDocumentId(treeUri).substringAfter(':').ifEmpty { "/" }

}

/** Goes through every file, so one that refuses does not spare the rest. */
private fun ExportStorage.deleteAll() {
    val failed = list().filterNot(::delete)
    if (failed.isNotEmpty()) throw IOException("Unable to delete ${failed.size} files")
}

/** The folder may be shared with other apps: only a partial of our own files is ours to remove. */
private fun isOwnPartial(name: String): Boolean =
    name.endsWith(PARTIAL_SUFFIX) && isPackageFile(name.removeSuffix(PARTIAL_SUFFIX))

internal fun uniqueName(fileName: String, exists: (String) -> Boolean): String {
    val stem = fileName.substringBeforeLast('.')
    val extension = fileName.substringAfterLast('.')
    var name = fileName
    var index = 1
    while (exists(name)) {
        name = "$stem ($index).$extension"
        index++
    }
    return name
}

private fun mimeTypeOf(fileName: String): String =
    if (fileName.endsWith(".apk", ignoreCase = true)) APK_MIME_TYPE else "application/octet-stream"

private const val EXPORT_DIR = "Apps"
private const val PARTIAL_SUFFIX = ".partial"
