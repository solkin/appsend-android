package com.tomclaw.appsend_rb.data

import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.content.IntentCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import com.jakewharton.rxrelay3.BehaviorRelay
import com.tomclaw.appsend_rb.domain.BUNDLE_BASE_ENTRY
import com.tomclaw.appsend_rb.domain.InstallConfirmation
import com.tomclaw.appsend_rb.domain.InstallEvent
import com.tomclaw.appsend_rb.domain.InstallFailure
import com.tomclaw.appsend_rb.domain.InstalledVersion
import com.tomclaw.appsend_rb.domain.PackageCandidate
import com.tomclaw.appsend_rb.domain.PackageInstallerGateway
import com.tomclaw.appsend_rb.domain.SchedulersFactory
import com.tomclaw.appsend_rb.getComponent
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipInputStream

class IntentConfirmation(val intent: Intent) : InstallConfirmation

/**
 * Owns the one install in progress. It lives as long as the process, so a screen that is
 * re-created mid-install simply subscribes again and misses nothing.
 */
class AndroidPackageInstaller(
    private val context: Context,
    private val schedulers: SchedulersFactory,
) : PackageInstallerGateway {

    private val packageManager = context.packageManager
    private val mainThread = Handler(Looper.getMainLooper())
    private val lock = Any()
    private var active: ActiveInstall? = null

    private class ActiveInstall(val candidate: PackageCandidate) {
        val events: BehaviorRelay<InstallEvent> = BehaviorRelay.create()

        @Volatile
        var sessionId = NO_SESSION

        @Volatile
        var cancelled = false

        @Volatile
        var committed = false

        @Volatile
        var finished = false

        @Volatile
        var left = false
    }

    fun onStatus(intent: Intent) {
        val sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, NO_SESSION)
        val install = synchronized(lock) {
            active?.takeIf { it.sessionId == sessionId && !it.finished }
        } ?: return
        deliver(install, toEvent(intent))
    }

    private fun deliver(install: ActiveInstall, event: InstallEvent) {
        synchronized(lock) {
            if (event !is InstallEvent.ConfirmationRequired) {
                install.finished = true
                // Nobody is left to read this outcome, and it must not greet the next visit.
                if (install.left && active === install) active = null
            }
        }
        install.events.accept(event)
    }

    override fun inspect(uri: String): Single<PackageCandidate> =
        Single.fromCallable { readCandidate(uri) }.subscribeOn(schedulers.io())

    private fun open(uri: String): InputStream =
        context.contentResolver.openInputStream(uri.toUri()) ?: throw IOException("Unable to open $uri")

    private fun readCandidate(uri: String): PackageCandidate {
        val base = File.createTempFile("candidate", ".apk", context.cacheDir)
        try {
            val archive = readArchive(uri, base)
            val info = packageManager.getPackageArchiveInfo(base.path, SIGNATURE_FLAGS)
                ?: throw IOException("Not an app package: $uri")
            val application = info.applicationInfo ?: throw IOException("No application in $uri")
            application.sourceDir = base.path
            application.publicSourceDir = base.path
            return PackageCandidate(
                uri = uri,
                plainApk = archive.plainApk,
                packageName = info.packageName,
                label = packageManager.getApplicationLabel(application).toString(),
                versionName = info.versionName.orEmpty(),
                versionCode = PackageInfoCompat.getLongVersionCode(info),
                minSdk = application.minSdkVersion,
                totalBytes = archive.totalBytes,
                apkNames = archive.apkNames,
                signatures = signingCertificates(info),
            )
        } finally {
            base.delete()
        }
    }

    private class Archive(val plainApk: Boolean, val apkNames: List<String>, val totalBytes: Long)

    /**
     * Finds out what the file holds and copies the base APK to [baseOutput]. A plain APK
     * is itself a zip, so its own manifest entry is what tells the two formats apart.
     */
    private fun readArchive(uri: String, baseOutput: File): Archive {
        val names = ArrayList<String>()
        var bytes = 0L
        var plainApk = false
        open(uri).use { input ->
            val zip = ZipInputStream(input)
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.name == "AndroidManifest.xml") {
                    plainApk = true
                    break
                }
                val name = apkName(entry.name) ?: continue
                names += name
                bytes += if (isBaseApk(name)) {
                    baseOutput.outputStream().use { zip.copyTo(it) }
                } else {
                    zip.skipAll()
                }
            }
        }
        if (plainApk) {
            val size = open(uri).use { input -> baseOutput.outputStream().use { input.copyTo(it) } }
            return Archive(plainApk = true, listOf(BUNDLE_BASE_ENTRY), size)
        }
        if (names.none(::isBaseApk)) throw IOException("No base APK in $uri")
        return Archive(plainApk = false, names, bytes)
    }

    /** The name an archive entry installs under, or null when it is not one of the set. */
    private fun apkName(entryName: String): String? {
        // Sets from bundletool also carry whole-device standalone APKs, which replace
        // the splits rather than join them.
        if (entryName.startsWith("standalones/") || !entryName.endsWith(".apk")) return null
        return entryName.substringAfterLast('/')
    }

    private fun isBaseApk(name: String): Boolean =
        name == BUNDLE_BASE_ENTRY || name == "base-master.apk"

    private fun ZipInputStream.skipAll(): Long {
        val buffer = ByteArray(BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = read(buffer)
            if (read == -1) return total
            total += read
        }
    }

    override fun installedVersion(packageName: String): InstalledVersion? = try {
        val info = packageManager.getPackageInfo(packageName, SIGNATURE_FLAGS)
        InstalledVersion(
            versionName = info.versionName.orEmpty(),
            versionCode = PackageInfoCompat.getLongVersionCode(info),
            signatures = signingCertificates(info),
        )
    } catch (ignored: PackageManager.NameNotFoundException) {
        null
    }

    override fun canInstall(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        packageManager.canRequestPackageInstalls()
    } else {
        @Suppress("DEPRECATION")
        Settings.Secure.getInt(context.contentResolver, Settings.Secure.INSTALL_NON_MARKET_APPS, 0) == 1
    }

    override fun ongoing(uri: String): PackageCandidate? = synchronized(lock) {
        active?.takeIf { it.candidate.uri == uri && !it.left }?.candidate
    }

    override fun install(candidate: PackageCandidate): Observable<InstallEvent> {
        synchronized(lock) {
            active?.let { current ->
                if (current.candidate.uri == candidate.uri && !current.left) return current.events.hide()
                release(current)
                // Whoever watched the displaced install must not wait for it forever.
                current.events.accept(InstallEvent.Failed(InstallFailure.ABORTED))
            }
            val install = ActiveInstall(candidate)
            active = install
            schedulers.io().scheduleDirect { runSession(install) }
            return install.events.hide()
        }
    }

    private fun runSession(install: ActiveInstall) {
        val candidate = install.candidate
        val installer = packageManager.packageInstaller
        try {
            abandonLeftovers(installer, candidate.packageName)
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
                setAppPackageName(candidate.packageName)
                setSize(candidate.totalBytes)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    setInstallReason(PackageManager.INSTALL_REASON_USER)
                }
            }
            install.sessionId = installer.createSession(params)
            watchSession(installer, install)
            installer.openSession(install.sessionId).use { session ->
                write(install, session)
                synchronized(lock) {
                    if (install.cancelled) throw InstallCancelledException()
                    install.committed = true
                }
                session.commit(statusSender(install.sessionId))
            }
        } catch (ignored: InstallCancelledException) {
            abandon(installer, install)
        } catch (error: IOException) {
            abandon(installer, install)
            finish(install, InstallFailure.STORAGE)
        } catch (error: RuntimeException) {
            abandon(installer, install)
            finish(install, InstallFailure.UNKNOWN)
        }
    }

    /**
     * The status broadcast says how an install ended, and it is the only one that says why.
     * Should it never come, this keeps the screen from waiting for it forever.
     */
    private fun watchSession(installer: PackageInstaller, install: ActiveInstall) {
        installer.registerSessionCallback(object : PackageInstaller.SessionCallback() {
            override fun onCreated(sessionId: Int) = Unit

            override fun onBadgingChanged(sessionId: Int) = Unit

            override fun onActiveChanged(sessionId: Int, active: Boolean) = Unit

            override fun onProgressChanged(sessionId: Int, progress: Float) = Unit

            override fun onFinished(sessionId: Int, success: Boolean) {
                if (sessionId != install.sessionId) return
                installer.unregisterSessionCallback(this)
                mainThread.postDelayed({ onSessionFinished(install, success) }, STATUS_GRACE_MS)
            }
        }, mainThread)
    }

    private fun onSessionFinished(install: ActiveInstall, success: Boolean) {
        val waiting = synchronized(lock) { active === install && !install.finished }
        if (waiting) {
            deliver(install, if (success) InstallEvent.Installed else InstallEvent.Failed(InstallFailure.UNKNOWN))
        }
    }

    private fun finish(install: ActiveInstall, failure: InstallFailure) {
        // Whoever cancelled it has already moved on, and closing the session is what failed it.
        if (install.cancelled) return
        install.finished = true
        install.events.accept(InstallEvent.Failed(failure))
    }

    /** Sessions of a process that died mid-install stay around until someone drops them. */
    private fun abandonLeftovers(installer: PackageInstaller, packageName: String) {
        installer.mySessions
            .filter { it.appPackageName == packageName }
            .forEach { session ->
                try {
                    installer.abandonSession(session.sessionId)
                } catch (ignored: SecurityException) {
                    // It has just reached its end on its own.
                }
            }
    }

    private fun abandon(installer: PackageInstaller, install: ActiveInstall) {
        if (install.sessionId == NO_SESSION) return
        try {
            installer.abandonSession(install.sessionId)
        } catch (ignored: SecurityException) {
            // The session has already reached its end.
        }
    }

    private fun write(install: ActiveInstall, session: PackageInstaller.Session) {
        val candidate = install.candidate
        val total = candidate.totalBytes.coerceAtLeast(1)
        var written = 0L
        var percent = -1L
        val track: (Long) -> Unit = { bytes ->
            if (install.cancelled) throw InstallCancelledException()
            written += bytes
            // A zip stream hands out tiny chunks: reporting each one would flood the screen.
            val current = written * 100 / total
            if (current != percent) {
                percent = current
                install.events.accept(InstallEvent.Writing(written, candidate.totalBytes))
                session.setStagingProgress(written.toFloat() / total)
            }
        }
        open(candidate.uri).use { input ->
            if (candidate.plainApk) {
                copyToSession(input, session, BUNDLE_BASE_ENTRY, candidate.totalBytes, track)
                return
            }
            val zip = ZipInputStream(input)
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = apkName(entry.name) ?: continue
                copyToSession(zip, session, name, -1, track)
            }
        }
    }

    private fun copyToSession(
        input: InputStream,
        session: PackageInstaller.Session,
        name: String,
        size: Long,
        onBytes: (Long) -> Unit,
    ) {
        session.openWrite(name, 0, size).use { output ->
            val buffer = ByteArray(BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read == -1) break
                output.write(buffer, 0, read)
                onBytes(read.toLong())
            }
            session.fsync(output)
        }
    }

    override fun confirmationShown(uri: String) {
        unfinished(uri)?.events?.accept(InstallEvent.AwaitingConfirmation)
    }

    override fun confirmationAnswered(uri: String) {
        unfinished(uri)?.events?.accept(InstallEvent.Applying)
    }

    /** An outcome already delivered is the last word: it must stay the one that gets replayed. */
    private fun unfinished(uri: String): ActiveInstall? = synchronized(lock) {
        active?.takeIf { it.candidate.uri == uri && !it.finished }
    }

    override fun cancel(uri: String) {
        synchronized(lock) {
            active?.takeIf { it.candidate.uri == uri }?.let(::release)
        }
    }

    override fun leave(uri: String) {
        synchronized(lock) {
            active?.takeIf { it.candidate.uri == uri }?.let { install ->
                install.left = true
                if (install.finished) active = null
            }
        }
    }

    /** Called under [lock]. A session still being written notices the flag and abandons itself. */
    private fun release(install: ActiveInstall) {
        active = null
        install.cancelled = true
        if (install.committed && !install.finished && !install.left) {
            abandon(packageManager.packageInstaller, install)
        }
    }

    private fun statusSender(sessionId: Int) = PendingIntent.getBroadcast(
        context,
        sessionId,
        Intent(context, InstallStatusReceiver::class.java).setPackage(context.packageName),
        // The system fills in the session status, so this one has to stay mutable.
        PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_MUTABLE
        } else {
            0
        },
    ).intentSender

    private fun toEvent(intent: Intent): InstallEvent =
        when (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
                if (confirm != null) {
                    InstallEvent.ConfirmationRequired(IntentConfirmation(confirm))
                } else {
                    InstallEvent.Failed(InstallFailure.UNKNOWN)
                }
            }
            PackageInstaller.STATUS_SUCCESS -> InstallEvent.Installed
            PackageInstaller.STATUS_FAILURE_ABORTED -> InstallEvent.Failed(InstallFailure.ABORTED)
            PackageInstaller.STATUS_FAILURE_BLOCKED -> InstallEvent.Failed(InstallFailure.BLOCKED)
            PackageInstaller.STATUS_FAILURE_CONFLICT -> InstallEvent.Failed(InstallFailure.CONFLICT)
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE -> InstallEvent.Failed(InstallFailure.INCOMPATIBLE)
            PackageInstaller.STATUS_FAILURE_INVALID -> InstallEvent.Failed(InstallFailure.INVALID)
            PackageInstaller.STATUS_FAILURE_STORAGE -> InstallEvent.Failed(InstallFailure.STORAGE)
            else -> InstallEvent.Failed(InstallFailure.UNKNOWN)
        }

}

class InstallStatusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        (context.applicationContext as Application).getComponent()
            .packageInstaller()
            .onStatus(intent)
    }

}

private class InstallCancelledException : RuntimeException()

private const val NO_SESSION = -1
private const val BUFFER_SIZE = 512 * 1024
private const val STATUS_GRACE_MS = 2000L

private val SIGNATURE_FLAGS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    PackageManager.GET_SIGNING_CERTIFICATES
} else {
    @Suppress("DEPRECATION")
    PackageManager.GET_SIGNATURES
}
