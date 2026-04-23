package com.tomclaw.appsend_rb.screen.apps

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.io.OutputStream

interface OutputWrapper {

    fun getOutputUri(fileName: String, mimeType: String): Uri

    fun openStream(uri: Uri): OutputStream

    fun clearExports()

}

class OutputWrapperImpl(
    private val context: Context,
    private val resolver: ContentResolver
) : OutputWrapper {

    override fun getOutputUri(fileName: String, mimeType: String): Uri {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collections = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val fileDetails = ContentValues()
            fileDetails.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            fileDetails.put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            fileDetails.put(MediaStore.MediaColumns.RELATIVE_PATH, getExportRelativePath())
            return resolver.insert(collections, fileDetails)
                ?: throw IOException("unable to create URI")
        } else {
            val directory = getLegacyExportDirectory()
            if (!(directory.exists() || directory.mkdirs())) {
                throw IOException("unable to create directory")
            }
            val destination = File(directory, fileName)
            if (destination.exists() && !destination.delete()) {
                throw IOException("unable to delete destination file")
            }
            return FileProvider.getUriForFile(context, "${context.packageName}.provider", destination)
        }
    }

    override fun openStream(uri: Uri): OutputStream {
        return resolver.openOutputStream(uri) ?: throw IOException("unable to open stream")
    }

    override fun clearExports() {
        clearLegacyExports()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            clearMediaStoreExports()
        }
    }

    private fun clearLegacyExports() {
        val directory = getLegacyExportDirectory()
        val files = directory.listFiles { pathname -> pathname.name.endsWith(".apk") }
        files?.forEach { file ->
            if (file.isFile && !file.delete()) {
                throw IOException("unable to delete ${file.name}")
            }
        }
    }

    private fun clearMediaStoreExports() {
        val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val selection = "${MediaStore.MediaColumns.RELATIVE_PATH} = ? " +
                "AND ${MediaStore.MediaColumns.MIME_TYPE} = ? " +
                "AND ${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf(getExportRelativePath(), APK_MIME_TYPE, "%.apk")
        resolver.delete(collection, selection, selectionArgs)
    }

    private fun getLegacyExportDirectory(): File {
        return File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            APPS_DIR_NAME
        )
    }

    private fun getExportRelativePath(): String {
        return Environment.DIRECTORY_DOWNLOADS + File.separator + APPS_DIR_NAME + File.separator
    }

}

const val APK_MIME_TYPE = "application/vnd.android.package-archive"