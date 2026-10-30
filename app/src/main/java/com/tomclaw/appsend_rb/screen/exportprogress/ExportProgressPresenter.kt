package com.tomclaw.appsend_rb.screen.exportprogress

import com.tomclaw.appsend_rb.domain.ExportAction
import com.tomclaw.appsend_rb.domain.ExportManager
import com.tomclaw.appsend_rb.domain.ExportSession
import com.tomclaw.appsend_rb.domain.ExportStatus
import com.tomclaw.appsend_rb.domain.ExportedFile
import com.tomclaw.appsend_rb.domain.ValueFormatter
import com.tomclaw.appsend_rb.domain.SchedulersFactory
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.plusAssign
import java.util.concurrent.TimeUnit

data class ExportRow(
    val packageName: String,
    val label: String,
    val status: ExportStatus,
    val percent: Int,
    val note: String,
    val position: Int,
    val total: Int,
)

data class ExportProgressViewModel(
    val running: Boolean,
    val currentIndex: Int,
    val count: Int,
    val written: String,
    val total: String,
    val percent: Int,
    val hasFailures: Boolean,
    val canShare: Boolean,
    val canOpenExports: Boolean,
    val rows: List<ExportRow>,
)

interface ExportProgressRouter {

    fun shareFiles(files: List<ExportedFile>)

    fun openExports()

    fun requestNotifications()

    fun leave()

}

interface ExportProgressPresenter {

    fun attachView(view: ExportProgressView)

    fun detachView()

    fun attachRouter(router: ExportProgressRouter)

    fun detachRouter()

    fun destroy()

}

class ExportProgressPresenterImpl(
    private val manager: ExportManager,
    private val formatter: ValueFormatter,
    private val schedulers: SchedulersFactory,
) : ExportProgressPresenter {

    private var view: ExportProgressView? = null
    private var router: ExportProgressRouter? = null
    private val viewSubscriptions = CompositeDisposable()
    private val dataSubscriptions = CompositeDisposable()
    private var session = ExportSession()
    private var sharing = false
    private var leavePending = false

    init {
        dataSubscriptions += manager.session()
            .sample(UI_INTERVAL_MS, TimeUnit.MILLISECONDS, schedulers.io(), true)
            .observeOn(schedulers.mainThread())
            .subscribe(::onSession)
    }

    override fun attachView(view: ExportProgressView) {
        this.view = view
        viewSubscriptions += view.cancelClicks().subscribe { manager.cancel() }
        viewSubscriptions += view.minimizeClicks().subscribe { router?.leave() }
        // Leaving a finished session behind would only bring this screen back later.
        viewSubscriptions += view.backClicks().subscribe {
            if (manager.current.active) router?.leave() else finish()
        }
        viewSubscriptions += view.retryClicks().subscribe { manager.retry(it) }
        viewSubscriptions += view.doneClicks().subscribe { finish() }
        viewSubscriptions += view.shareClicks().subscribe { share() }
        viewSubscriptions += view.openExportsClicks().subscribe {
            router?.openExports()
            finish()
        }
        render()
    }

    override fun detachView() {
        viewSubscriptions.clear()
        view = null
    }

    override fun attachRouter(router: ExportProgressRouter) {
        this.router = router
        manager.observed = true
        if (session.active) router.requestNotifications()
        if (leavePending) router.leave() else leaveIfFinishedSharing()
    }

    override fun detachRouter() {
        manager.observed = false
        router = null
    }

    override fun destroy() {
        dataSubscriptions.clear()
    }

    private fun onSession(session: ExportSession) {
        if (!this.session.active && session.active) router?.requestNotifications()
        this.session = session
        if (session.items.isEmpty()) {
            leavePending = true
            router?.leave()
            return
        }
        render()
        leaveIfFinishedSharing()
    }

    /** A completed share hands straight over to the chooser: there is nothing to look at. */
    private fun leaveIfFinishedSharing() {
        val allDone = session.items.isNotEmpty() && session.items.all { it.status == ExportStatus.DONE }
        if (session.action == ExportAction.SHARE && allDone && !sharing) share()
    }

    private fun share() {
        val router = router ?: return
        sharing = true
        router.shareFiles(session.items.mapNotNull { it.file })
        finish()
    }

    private fun finish() {
        manager.dismiss()
        router?.leave()
    }

    private fun render() {
        val items = session.items
        val done = items.count { it.status == ExportStatus.DONE }
        val finished = !session.active
        view?.render(
            ExportProgressViewModel(
                running = !finished,
                currentIndex = (done + 1).coerceAtMost(items.size),
                count = items.size,
                written = formatter.bytes(session.writtenBytes),
                total = formatter.bytes(session.totalBytes),
                percent = percent(session.writtenBytes, session.totalBytes),
                hasFailures = items.any { it.status == ExportStatus.FAILED },
                canShare = finished && done > 0,
                canOpenExports = finished && done > 0 && session.action == ExportAction.EXTRACT,
                rows = items.mapIndexed { index, item ->
                    ExportRow(
                        packageName = item.packageName,
                        label = item.label,
                        status = item.status,
                        percent = percent(item.writtenBytes, item.totalBytes),
                        note = item.file?.name
                            ?: "${formatter.bytes(item.writtenBytes)} / ${formatter.bytes(item.totalBytes)}",
                        position = index,
                        total = items.size,
                    )
                },
            )
        )
    }

    private fun percent(part: Long, whole: Long): Int =
        if (whole > 0) (part * 100 / whole).toInt().coerceIn(0, 100) else 0

}

private const val UI_INTERVAL_MS = 150L
