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

}

class OutputWrapperImpl(
    private val context: Context,
    private val resolver: ContentResolver
) : OutputWrapper {

    override fun getOutputUri(fileName: String, mimeType: String): Uri {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val collections = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val fileDetails = ContentValues()
            fileDetails.put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            fileDetails.put(MediaStore.Images.Media.MIME_TYPE, mimeType)
            return resolver.insert(collections, fileDetails)
                ?: throw IOException("unable to create URI")
        } else {
            val directory = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                APPS_DIR_NAME
            )
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

}