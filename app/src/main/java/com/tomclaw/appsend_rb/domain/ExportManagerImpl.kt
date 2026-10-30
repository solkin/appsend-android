package com.tomclaw.appsend_rb.domain

import com.jakewharton.rxrelay3.BehaviorRelay
import com.tomclaw.appsend_rb.domain.Analytics
import com.tomclaw.appsend_rb.domain.SchedulersFactory
import io.reactivex.rxjava3.core.Observable
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runs one export session at a time on a single background worker. All session state
 * goes through [update], so readers always see a consistent snapshot.
 */
class ExportManagerImpl(
    private val sources: ApkSourceProvider,
    private val storage: ExportStorage,
    private val shareStorage: ExportStorage,
    private val packager: ApkPackager,
    private val inspector: ArchiveInspector,
    private val metadata: (InstalledApp) -> String,
    private val preferences: AppPreferences,
    private val guard: ExportForegroundGuard,
    private val schedulers: SchedulersFactory,
    private val analytics: Analytics,
    private val clock: () -> Long = System::currentTimeMillis,
) : ExportManager {

    private val state = BehaviorRelay.createDefault(ExportSession())
    private val lock = Any()
    private val apps = HashMap<String, InstalledApp>()
    private val cancelled = AtomicBoolean(false)
    private var workerRunning = false
    private var shareSessionPending = false

    override fun session(): Observable<ExportSession> = state.hide()

    override val current: ExportSession get() = state.value!!

    @Volatile
    override var observed: Boolean = false

    override fun start(apps: List<InstalledApp>, action: ExportAction, format: ExportFormat) {
        if (apps.isEmpty()) return
        synchronized(lock) {
            if (state.value!!.active) return
            this.apps.clear()
            apps.forEach { this.apps[it.packageName] = it }
            shareSessionPending = action == ExportAction.SHARE
            state.accept(
                ExportSession(action, apps.map { app -> newItem(app, format) })
            )
            analytics.trackEvent(
                "export_started",
                mapOf("action" to action.name, "format" to format.name),
                mapOf("count" to apps.size.toDouble()),
            )
            launchWorker()
        }
    }

    private fun newItem(app: InstalledApp, format: ExportFormat) = ExportItem(
        packageName = app.packageName,
        label = app.label,
        format = format,
        totalBytes = if (format == ExportFormat.BUNDLE) app.totalSize else app.baseSize,
    )

    override fun cancel() {
        synchronized(lock) {
            cancelled.set(true)
            update { items ->
                items.map { if (it.status == ExportStatus.QUEUED) it.copy(status = ExportStatus.CANCELLED) else it }
            }
        }
    }

    override fun retry(packageName: String) {
        synchronized(lock) {
            update { items ->
                items.map {
                    if (it.packageName == packageName && it.status != ExportStatus.RUNNING) {
                        it.copy(status = ExportStatus.QUEUED, writtenBytes = 0, file = null)
                    } else {
                        it
                    }
                }
            }
            launchWorker()
        }
    }

    override fun dismiss() {
        synchronized(lock) {
            if (!state.value!!.active) {
                state.accept(ExportSession())
                guard.clearFinishedNotice()
            }
        }
    }

    /** Called under [lock] whenever work is added, even if the worker is still winding down. */
    private fun launchWorker() {
        guard.ensureRunning()
        if (workerRunning) return
        workerRunning = true
        schedulers.io().scheduleDirect { work() }
    }

    private fun work() {
        while (true) {
            val next = synchronized(lock) {
                // Everything queued now came after the last cancel, which it must not inherit.
                val item = state.value!!.items.firstOrNull { it.status == ExportStatus.QUEUED }
                if (item == null) workerRunning = false else cancelled.set(false)
                item
            } ?: return
            export(next)
        }
    }

    private fun export(item: ExportItem) {
        val app = synchronized(lock) { apps.getValue(item.packageName) }
        setItem(item.packageName) { it.copy(status = ExportStatus.RUNNING) }
        var target: ExportTarget? = null
        try {
            val fileName = exportFileName(preferences.fileNameTemplate, app, item.format)
            val sharing = current.action == ExportAction.SHARE
            if (sharing && shareSessionPending) {
                shareSessionPending = false
                shareStorage.clear()
            }
            target = (if (sharing) shareStorage else storage).create(fileName)
            val paths = sources.sources(app.packageName)
            val progress = ProgressReporter(item.packageName)
            target.output.use { output ->
                if (fileName.endsWith(BUNDLE_EXTENSION)) {
                    packager.writeBundle(paths, metadata(app), output, cancelled::get, progress::add)
                } else {
                    packager.writeApk(paths.basePath, output, cancelled::get, progress::add)
                }
            }
            val file = target.commit()
            if (!sharing) {
                inspector.remember(
                    file,
                    ArchiveInfo(app.packageName, app.label, app.versionName, app.versionCode),
                )
            }
            setItem(item.packageName) {
                it.copy(status = ExportStatus.DONE, writtenBytes = it.totalBytes, file = file)
            }
        } catch (ignored: ExportCancelledException) {
            target?.abortQuietly()
            setItem(item.packageName) { it.copy(status = ExportStatus.CANCELLED, writtenBytes = 0) }
        } catch (error: Exception) {
            target?.abortQuietly()
            analytics.trackException(error, mapOf("event" to "export_failed"))
            setItem(item.packageName) { it.copy(status = ExportStatus.FAILED, writtenBytes = 0) }
        }
    }

    /** A failed cleanup must not take the worker down with the rest of the queue. */
    private fun ExportTarget.abortQuietly() {
        try {
            abort()
        } catch (error: Exception) {
            analytics.trackException(error, mapOf("event" to "export_abort_failed"))
        }
    }

    private inner class ProgressReporter(private val packageName: String) {

        private var written = 0L
        private var reportedAt = 0L

        fun add(bytes: Long) {
            written += bytes
            val now = clock()
            if (now - reportedAt >= PROGRESS_INTERVAL_MS) {
                reportedAt = now
                setItem(packageName) { it.copy(writtenBytes = written) }
            }
        }

    }

    private fun setItem(packageName: String, change: (ExportItem) -> ExportItem) {
        update { items -> items.map { if (it.packageName == packageName) change(it) else it } }
    }

    private fun update(change: (List<ExportItem>) -> List<ExportItem>) {
        synchronized(lock) {
            val session = state.value!!
            state.accept(session.copy(items = change(session.items)))
        }
    }

}

private const val PROGRESS_INTERVAL_MS = 100L
