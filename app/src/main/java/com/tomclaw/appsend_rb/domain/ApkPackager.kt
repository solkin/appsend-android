package com.tomclaw.appsend_rb.domain

import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExportCancelledException : RuntimeException()

/** Writes APK files out, either as they are or as one `.apks` archive. */
class ApkPackager {

    fun writeApk(
        path: String,
        output: OutputStream,
        isCancelled: () -> Boolean,
        onBytes: (Long) -> Unit,
    ) {
        File(path).inputStream().use { copy(it, output, isCancelled, onBytes) }
        output.flush()
    }

    /** [metadata] goes first, so reading it back never has to scan the APKs. */
    fun writeBundle(
        sources: ApkSources,
        metadata: String,
        output: OutputStream,
        isCancelled: () -> Boolean,
        onBytes: (Long) -> Unit,
    ) {
        val zip = ZipOutputStream(output)
        // APKs are already compressed, and the installer maps them uncompressed.
        zip.setLevel(Deflater.NO_COMPRESSION)
        zip.putNextEntry(ZipEntry(BUNDLE_METADATA_ENTRY))
        zip.write(metadata.toByteArray())
        zip.closeEntry()
        // A system app's base carries its own name on disk: in the archive it is always
        // `base.apk`, which is what tells the base from the splits when reading it back.
        val entries = listOf(BUNDLE_BASE_ENTRY to sources.basePath) +
            sources.splitPaths.map { File(it).name to it }
        for ((name, path) in entries) {
            zip.putNextEntry(ZipEntry(name))
            File(path).inputStream().use { copy(it, zip, isCancelled, onBytes) }
            zip.closeEntry()
        }
        zip.finish()
        zip.flush()
    }

    private fun copy(
        input: InputStream,
        output: OutputStream,
        isCancelled: () -> Boolean,
        onBytes: (Long) -> Unit,
    ) {
        val buffer = ByteArray(BUFFER_SIZE)
        while (true) {
            if (isCancelled()) throw ExportCancelledException()
            val read = input.read(buffer)
            if (read == -1) return
            output.write(buffer, 0, read)
            onBytes(read.toLong())
        }
    }

}

private const val BUFFER_SIZE = 512 * 1024
