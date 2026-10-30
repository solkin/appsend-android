package com.tomclaw.appsend_rb.data

import android.content.Context
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.tomclaw.appsend_rb.domain.ExportStorage
import com.tomclaw.appsend_rb.domain.ExportTarget
import com.tomclaw.appsend_rb.domain.ExportedFile
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * Where files wait to be picked up by the app they are shared with. Private to this app,
 * so sharing asks for no permission and leaves nothing in the user's folders.
 *
 * Every session gets a directory of its own, so names stay clean and an earlier session
 * is not disturbed: a receiver may read its files long after the chooser has closed.
 */
class ShareCacheStorage(private val context: Context) : ExportStorage {

    private val root = File(context.cacheDir, SHARE_DIR)
    private var session: File? = null

    override fun create(fileName: String): ExportTarget {
        val directory = session ?: newSession()
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("Unable to create $directory")
        }
        val destination = File(directory, uniqueName(fileName) { File(directory, it).exists() })
        return object : ExportTarget {
            override val output: OutputStream = destination.outputStream()

            override fun commit(): ExportedFile = ExportedFile(
                uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", destination).toString(),
                name = destination.name,
                size = destination.length(),
                modified = destination.lastModified(),
            )

            override fun abort() {
                destination.delete()
            }
        }
    }

    private fun newSession() = File(root, System.currentTimeMillis().toString()).also { session = it }

    override fun list(): List<ExportedFile> = emptyList()

    override fun delete(file: ExportedFile): Boolean = false

    /** Starts a new session and frees the ones old enough for nobody to be reading them. */
    override fun clear() {
        val oldest = System.currentTimeMillis() - RETENTION_MS
        root.listFiles { file -> file.lastModified() < oldest }?.forEach { it.deleteRecursively() }
        newSession()
    }

    override fun open(uri: String): InputStream =
        context.contentResolver.openInputStream(uri.toUri()) ?: throw IOException("Unable to open $uri")

    override fun requiresPermission(): Boolean = false

    override fun location(): String = root.name

}

private const val SHARE_DIR = "share"
private const val RETENTION_MS = 24L * 60 * 60 * 1000
