package com.tomclaw.appsend_rb.domain

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

class ApkPackagerTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val packager = ApkPackager()

    private fun file(name: String, content: ByteArray) =
        folder.newFile(name).apply { writeBytes(content) }.path

    @Test
    fun `copies a plain apk and reports every byte`() {
        val content = ByteArray(2_000_000) { it.toByte() }
        val output = ByteArrayOutputStream()
        var reported = 0L
        packager.writeApk(file("base.apk", content), output, { false }, { reported += it })
        assertArrayEquals(content, output.toByteArray())
        assertEquals(content.size.toLong(), reported)
    }

    @Test
    fun `bundle holds metadata first and every apk under its own name`() {
        val sources = ApkSources(
            file("base.apk", byteArrayOf(1, 2, 3)),
            listOf(file("split_config.en.apk", byteArrayOf(4)), file("split_config.xhdpi.apk", byteArrayOf(5, 6))),
        )
        val output = ByteArrayOutputStream()
        var reported = 0L
        packager.writeBundle(sources, "{}", output, { false }, { reported += it })

        val entries = LinkedHashMap<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries[entry.name] = zip.readBytes()
            }
        }
        assertEquals(
            listOf(BUNDLE_METADATA_ENTRY, "base.apk", "split_config.en.apk", "split_config.xhdpi.apk"),
            entries.keys.toList(),
        )
        assertEquals("{}", entries.getValue(BUNDLE_METADATA_ENTRY).decodeToString())
        assertArrayEquals(byteArrayOf(5, 6), entries.getValue("split_config.xhdpi.apk"))
        assertEquals(6L, reported)
    }

    @Test
    fun `base entry is always named base apk`() {
        val sources = ApkSources(file("Chrome.apk", byteArrayOf(1)), emptyList())
        val output = ByteArrayOutputStream()
        packager.writeBundle(sources, "{}", output, { false }, {})
        val names = ArrayList<String>()
        ZipInputStream(ByteArrayInputStream(output.toByteArray())).use { zip ->
            while (true) names += (zip.nextEntry ?: break).name
        }
        assertEquals(listOf(BUNDLE_METADATA_ENTRY, BUNDLE_BASE_ENTRY), names)
    }

    @Test(expected = ExportCancelledException::class)
    fun `stops once cancelled`() {
        packager.writeApk(file("base.apk", ByteArray(10)), ByteArrayOutputStream(), { true }, {})
    }

}
