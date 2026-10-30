package com.tomclaw.appsend_rb.domain

import com.tomclaw.appsend_rb.app
import org.junit.Assert.assertEquals
import org.junit.Test

class FileNamesTest {

    @Test
    fun `fills every token`() {
        val name = exportFileName(
            "{name}-{package}-{version}-{code}",
            app(label = "Tele", packageName = "org.tele", version = "2.1", code = 7),
            ExportFormat.BASE,
        )
        assertEquals("Tele-org.tele-2.1-7.apk", name)
    }

    @Test
    fun `split app in bundle format becomes apks`() {
        assertEquals("A_1.0_1.apks", exportFileName(DEFAULT_FILE_NAME_TEMPLATE, app(label = "A", splits = 2), ExportFormat.BUNDLE))
    }

    @Test
    fun `split app in base format stays apk`() {
        assertEquals("A_1.0_1.apk", exportFileName(DEFAULT_FILE_NAME_TEMPLATE, app(label = "A", splits = 2), ExportFormat.BASE))
    }

    @Test
    fun `single apk app stays apk in bundle format`() {
        assertEquals("A_1.0_1.apk", exportFileName(DEFAULT_FILE_NAME_TEMPLATE, app(label = "A"), ExportFormat.BUNDLE))
    }

    @Test
    fun `replaces characters no file system accepts`() {
        assertEquals("a_b_c_d.apk", exportFileName("{name}", app(label = "a/b:c?d"), ExportFormat.BASE))
    }

    @Test
    fun `template that leaves nothing falls back to the package name`() {
        assertEquals("org.tele.apk", exportFileName("...", app(packageName = "org.tele"), ExportFormat.BASE))
    }

}
