package com.tomclaw.appsend_rb.domain

import com.tomclaw.appsend_rb.FakePreferences
import com.tomclaw.appsend_rb.ImmediateSchedulers
import com.tomclaw.appsend_rb.NoAnalytics
import com.tomclaw.appsend_rb.app
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class ExportManagerTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val storage = RecordingStorage()
    private val shareStorage = RecordingStorage()
    private val paths = HashMap<String, ApkSources>()
    private val inspector = RecordingInspector()
    private var guardCalls = 0
    private var noticesCleared = 0

    private val manager = ExportManagerImpl(
        sources = object : ApkSourceProvider {
            override fun sources(packageName: String) =
                paths[packageName] ?: throw IOException("gone: $packageName")
        },
        storage = storage,
        shareStorage = shareStorage,
        packager = ApkPackager(),
        inspector = inspector,
        metadata = { "{}" },
        preferences = FakePreferences(),
        guard = object : ExportForegroundGuard {
            override fun ensureRunning() {
                guardCalls++
            }

            override fun clearFinishedNotice() {
                noticesCleared++
            }
        },
        schedulers = ImmediateSchedulers,
        analytics = NoAnalytics,
    )

    private fun source(packageName: String, content: String = "apk"): ApkSources {
        val dir = File(folder.root, packageName).apply { mkdirs() }
        val base = File(dir, "base.apk").apply { writeText(content) }
        return ApkSources(base.path, emptyList()).also { paths[packageName] = it }
    }

    private fun session() = manager.session().blockingFirst()

    @Test
    fun `exports every app and commits its file`() {
        source("com.example.a", "aaa")
        source("com.example.b", "b")
        manager.start(listOf(app("A"), app("B")), ExportAction.EXTRACT, ExportFormat.BUNDLE)

        val session = session()
        assertFalse(session.active)
        assertTrue(session.items.all { it.status == ExportStatus.DONE })
        assertEquals(listOf("A_1.0_1.apk", "B_1.0_1.apk"), storage.committed)
        assertEquals("aaa", storage.written.getValue("A_1.0_1.apk").toString())
        assertEquals(1, guardCalls)
    }

    @Test
    fun `remembers what it exported`() {
        source("com.example.a")
        manager.start(listOf(app("A", version = "2.0", code = 9)), ExportAction.EXTRACT, ExportFormat.BASE)
        assertEquals(ArchiveInfo("com.example.a", "A", "2.0", 9), inspector.remembered.single())
    }

    @Test
    fun `a failed app does not stop the others and leaves nothing behind`() {
        source("com.example.b")
        manager.start(listOf(app("A"), app("B")), ExportAction.EXTRACT, ExportFormat.BASE)

        val (first, second) = session().items
        assertEquals(ExportStatus.FAILED, first.status)
        assertEquals(ExportStatus.DONE, second.status)
        assertEquals(listOf("A_1.0_1.apk"), storage.aborted)
        assertEquals(listOf("B_1.0_1.apk"), storage.committed)
    }

    @Test
    fun `retry exports a failed app again`() {
        manager.start(listOf(app("A")), ExportAction.EXTRACT, ExportFormat.BASE)
        assertEquals(ExportStatus.FAILED, session().items.single().status)

        source("com.example.a")
        manager.retry("com.example.a")

        assertEquals(ExportStatus.DONE, session().items.single().status)
    }

    @Test
    fun `retry starts over after a cancelled run and keeps the foreground guard up`() {
        manager.start(listOf(app("A")), ExportAction.EXTRACT, ExportFormat.BASE)
        manager.cancel()
        source("com.example.a")
        manager.retry("com.example.a")
        assertEquals(ExportStatus.DONE, session().items.single().status)
        assertEquals(2, guardCalls)
    }

    @Test
    fun `a failed cleanup does not stop the queue`() {
        storage.abortFails = true
        source("com.example.b")
        manager.start(listOf(app("A"), app("B")), ExportAction.EXTRACT, ExportFormat.BASE)
        val (first, second) = session().items
        assertEquals(ExportStatus.FAILED, first.status)
        assertEquals(ExportStatus.DONE, second.status)
    }

    @Test
    fun `sharing stays out of the export folder and frees the last share first`() {
        source("com.example.a")
        manager.start(listOf(app("A")), ExportAction.SHARE, ExportFormat.BASE)
        assertTrue(storage.committed.isEmpty())
        assertEquals(listOf("A_1.0_1.apk"), shareStorage.committed)
        assertEquals(1, shareStorage.clears)
        assertTrue(inspector.remembered.isEmpty())
    }

    @Test
    fun `dismiss forgets a finished session`() {
        source("com.example.a")
        manager.start(listOf(app("A")), ExportAction.EXTRACT, ExportFormat.BASE)
        manager.dismiss()
        assertTrue(session().items.isEmpty())
        assertEquals(1, noticesCleared)
    }

    @Test
    fun `bundle format counts the splits and base format does not`() {
        source("com.example.a")
        source("com.example.b")
        val split = app("A", baseSize = 10, totalSize = 50, splits = 3)
        manager.start(listOf(split), ExportAction.EXTRACT, ExportFormat.BUNDLE)
        assertEquals(50, session().items.single().totalBytes)
        manager.dismiss()
        manager.start(listOf(split), ExportAction.EXTRACT, ExportFormat.BASE)
        assertEquals(10, session().items.single().totalBytes)
    }

    private class RecordingStorage : ExportStorage {

        val written = HashMap<String, ByteArrayOutputStream>()
        val committed = ArrayList<String>()
        val aborted = ArrayList<String>()
        var abortFails = false
        var clears = 0

        override fun create(fileName: String): ExportTarget {
            val stream = ByteArrayOutputStream().also { written[fileName] = it }
            return object : ExportTarget {
                override val output: OutputStream = stream

                override fun commit(): ExportedFile {
                    committed += fileName
                    return ExportedFile("uri:$fileName", fileName, stream.size().toLong(), 0)
                }

                override fun abort() {
                    if (abortFails) throw IOException("cannot clean up")
                    aborted += fileName
                }
            }
        }

        override fun list(): List<ExportedFile> = emptyList()

        override fun delete(file: ExportedFile): Boolean = true

        override fun clear() {
            clears++
        }

        override fun open(uri: String): InputStream = throw UnsupportedOperationException()

        override fun requiresPermission(): Boolean = false

        override fun location(): String = "test"

    }

    private class RecordingInspector : ArchiveInspector {

        val remembered = ArrayList<ArchiveInfo>()

        override fun inspect(file: ExportedFile): ArchiveInfo? = null

        override fun remember(file: ExportedFile, info: ArchiveInfo) {
            remembered += info
        }

    }

}
