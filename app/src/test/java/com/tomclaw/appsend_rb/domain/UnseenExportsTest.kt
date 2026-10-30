package com.tomclaw.appsend_rb.domain

import com.jakewharton.rxrelay3.BehaviorRelay
import io.reactivex.rxjava3.core.Observable
import org.junit.Assert.assertEquals
import org.junit.Test

class UnseenExportsTest {

    private val sessions = BehaviorRelay.createDefault(ExportSession())
    private val manager = object : ExportManager {
        override fun session(): Observable<ExportSession> = sessions
        override val current get() = sessions.value!!
        override var observed = false
        override fun start(apps: List<InstalledApp>, action: ExportAction, format: ExportFormat) = Unit
        override fun cancel() = Unit
        override fun retry(packageName: String) = Unit
        override fun dismiss() = Unit
    }
    private val unseen = UnseenExports(manager)

    private fun session(action: ExportAction, vararg uris: String) = ExportSession(
        action,
        uris.map {
            ExportItem(it, it, ExportFormat.BASE, 1, 1, ExportStatus.DONE, ExportedFile(it, it, 1, 0))
        },
    )

    private fun count() = unseen.count().blockingFirst()

    @Test
    fun `counts extracted files across sessions`() {
        sessions.accept(session(ExportAction.EXTRACT, "a"))
        sessions.accept(ExportSession())
        sessions.accept(session(ExportAction.EXTRACT, "b"))
        assertEquals(2, count())
    }

    @Test
    fun `a session replayed after being seen does not count again`() {
        sessions.accept(session(ExportAction.EXTRACT, "a"))
        unseen.markSeen()
        sessions.accept(session(ExportAction.EXTRACT, "a"))
        assertEquals(0, count())
    }

    @Test
    fun `shared files do not count`() {
        sessions.accept(session(ExportAction.SHARE, "a"))
        assertEquals(0, count())
    }

}
