package com.tomclaw.appsend_rb.screen.exportprogress

import com.jakewharton.rxrelay3.BehaviorRelay
import com.tomclaw.appsend_rb.domain.ExportAction
import com.tomclaw.appsend_rb.domain.ExportFormat
import com.tomclaw.appsend_rb.domain.ExportItem
import com.tomclaw.appsend_rb.domain.ExportManager
import com.tomclaw.appsend_rb.domain.ExportSession
import com.tomclaw.appsend_rb.domain.ExportStatus
import com.tomclaw.appsend_rb.domain.ExportedFile
import com.tomclaw.appsend_rb.domain.InstalledApp
import com.tomclaw.appsend_rb.domain.SchedulersFactory
import com.tomclaw.appsend_rb.domain.ValueFormatter
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.schedulers.Schedulers
import io.reactivex.rxjava3.schedulers.TestScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class ExportProgressPresenterTest {

    private val sampler = TestScheduler()
    private val manager = FakeManager()
    private val formatter = CountingFormatter()
    private val view = FakeView()

    private fun presenter() = ExportProgressPresenterImpl(
        manager,
        formatter,
        object : SchedulersFactory {
            override fun io(): Scheduler = sampler

            override fun mainThread(): Scheduler = Schedulers.trampoline()
        },
    )

    @Test
    fun `a running session shows as running before its first update arrives`() {
        manager.state.accept(ExportSession(items = listOf(item("a", ExportStatus.RUNNING))))
        presenter().attachView(view)
        assertTrue(view.model!!.running)
    }

    @Test
    fun `sizes are formatted only for the rows that show them`() {
        val queued = (1..500).map { item("queued$it", ExportStatus.QUEUED) }
        manager.state.accept(
            ExportSession(items = listOf(item("running", ExportStatus.RUNNING)) + queued)
        )
        presenter().attachView(view)
        formatter.calls = 0
        sampler.advanceTimeBy(1, TimeUnit.SECONDS)
        // Header and the running row only, however long the queue is.
        assertTrue("formatted ${formatter.calls} sizes", formatter.calls <= 4)
        assertEquals("", view.model!!.rows.last().note)
    }

    @Test
    fun `a finished row shows its file name`() {
        val file = ExportedFile("content://a", "a.apk", 10, 0)
        manager.state.accept(
            ExportSession(items = listOf(item("a", ExportStatus.DONE).copy(file = file)))
        )
        presenter().attachView(view)
        assertEquals("a.apk", view.model!!.rows.single().note)
    }

    private fun item(name: String, status: ExportStatus) = ExportItem(
        packageName = name,
        label = name,
        format = ExportFormat.BASE,
        totalBytes = 10,
        status = status,
    )

    private class FakeManager : ExportManager {

        val state = BehaviorRelay.createDefault(ExportSession())

        override fun session(): Observable<ExportSession> = state

        override val current: ExportSession get() = state.value!!

        override var observed = false

        override fun start(apps: List<InstalledApp>, action: ExportAction, format: ExportFormat) = Unit

        override fun cancel() = Unit

        override fun retry(packageName: String) = Unit

        override fun dismiss() = Unit

    }

    private class CountingFormatter : ValueFormatter {

        var calls = 0

        override fun bytes(value: Long): String {
            calls++
            return "$value B"
        }

        override fun relativeTime(time: Long) = ""

        override fun date(time: Long) = ""

    }

    private class FakeView : ExportProgressView {

        var model: ExportProgressViewModel? = null

        override fun render(model: ExportProgressViewModel) {
            this.model = model
        }

        override fun cancelClicks(): Observable<Unit> = Observable.never()

        override fun minimizeClicks(): Observable<Unit> = Observable.never()

        override fun backClicks(): Observable<Unit> = Observable.never()

        override fun retryClicks(): Observable<String> = Observable.never()

        override fun doneClicks(): Observable<Unit> = Observable.never()

        override fun shareClicks(): Observable<Unit> = Observable.never()

        override fun openExportsClicks(): Observable<Unit> = Observable.never()

    }

}
