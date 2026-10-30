package com.tomclaw.appsend_rb.screen.exportprogress

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.tomclaw.appsend_rb.R
import com.tomclaw.appsend_rb.domain.ExportForegroundGuard
import com.tomclaw.appsend_rb.domain.ExportManager
import com.tomclaw.appsend_rb.domain.ExportSession
import com.tomclaw.appsend_rb.domain.ExportStatus
import com.tomclaw.appsend_rb.getComponent
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import java.util.concurrent.TimeUnit

/** Holds the process in the foreground for as long as an export session is active. */
class ExportService : Service() {

    private lateinit var manager: ExportManager
    private var subscription: Disposable? = null
    private var lastStartId = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        manager = application.getComponent().exportManager()
        createChannel()
        subscription = manager.session()
            .sample(NOTIFICATION_INTERVAL_MS, TimeUnit.MILLISECONDS, true)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { update() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId
        // Every startForegroundService() call owes one of these, running already or not.
        try {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildProgressNotification(manager.current),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                } else {
                    0
                },
            )
        } catch (ignored: IllegalStateException) {
            // Refused by the system: out of the data-sync budget or started from the background.
            manager.cancel()
            stopSelf(startId)
        }
        // The session may have ended before this start was delivered. Its screen may not
        // be up yet either, so whether a notice is due stays with the regular update.
        if (!manager.current.active) stopSelf(startId)
        // Nothing to resume after the process is gone: the session lives in memory.
        return START_NOT_STICKY
    }

    private fun update() {
        val session = manager.current
        if (session.active) {
            getSystemService(NotificationManager::class.java)
                .notify(NOTIFICATION_ID, buildProgressNotification(session))
            return
        }
        if (session.items.isNotEmpty() && !manager.observed) {
            getSystemService(NotificationManager::class.java)
                .notify(FINISHED_NOTIFICATION_ID, buildFinishedNotification(session))
        }
        // Passing the id keeps the service alive if a new session has just started it again.
        stopSelf(lastStartId)
    }

    /** Android 15 stops data-sync services after a time budget: end the session cleanly. */
    override fun onTimeout(startId: Int, fgsType: Int) {
        manager.cancel()
        stopSelf(startId)
    }

    override fun onDestroy() {
        subscription?.dispose()
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.export_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun buildProgressNotification(session: ExportSession): Notification {
        val finished = session.items.count {
            it.status == ExportStatus.DONE || it.status == ExportStatus.FAILED
        }
        val running = session.items.firstOrNull { it.status == ExportStatus.RUNNING }
        val total = session.totalBytes
        val percent = if (total > 0) (session.writtenBytes * 100 / total).toInt() else 0
        return builder()
            .setContentTitle(
                getString(
                    R.string.export_progress_title,
                    (finished + 1).coerceAtMost(session.items.size),
                    session.items.size,
                )
            )
            .setContentText(running?.label)
            .setProgress(100, percent, total == 0L)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .build()
    }

    private fun buildFinishedNotification(session: ExportSession): Notification {
        val failed = session.items.any { it.status == ExportStatus.FAILED }
        return builder()
            .setContentTitle(
                getString(if (failed) R.string.export_finished_with_failures else R.string.export_finished)
            )
            .setAutoCancel(true)
            .build()
    }

    private fun builder() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_extract)
        .setContentIntent(
            PendingIntent.getActivity(
                this,
                0,
                createExportProgressIntent(this),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        )

}

class ExportServiceGuard(private val context: Context) : ExportForegroundGuard {

    override fun ensureRunning() {
        ContextCompat.startForegroundService(context, Intent(context, ExportService::class.java))
    }

    override fun clearFinishedNotice() {
        context.getSystemService(NotificationManager::class.java).cancel(FINISHED_NOTIFICATION_ID)
    }

}

private const val CHANNEL_ID = "export"
private const val NOTIFICATION_ID = 1
private const val FINISHED_NOTIFICATION_ID = 2
private const val NOTIFICATION_INTERVAL_MS = 500L
