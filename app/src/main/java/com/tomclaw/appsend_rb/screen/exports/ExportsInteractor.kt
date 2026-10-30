package com.tomclaw.appsend_rb.screen.exports

import com.tomclaw.appsend_rb.domain.AppPreferences
import com.tomclaw.appsend_rb.domain.ArchiveInfo
import com.tomclaw.appsend_rb.domain.ArchiveInspector
import com.tomclaw.appsend_rb.domain.ExportChanges
import com.tomclaw.appsend_rb.domain.ExportManager
import com.tomclaw.appsend_rb.domain.ExportStorage
import com.tomclaw.appsend_rb.domain.ExportedFile
import com.tomclaw.appsend_rb.domain.InstalledApp
import com.tomclaw.appsend_rb.domain.InstalledAppsRepository
import com.tomclaw.appsend_rb.domain.SchedulersFactory
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.io.IOException

enum class InstallState { UNKNOWN, NOT_INSTALLED, CURRENT, NEWER_INSTALLED, OLDER_INSTALLED }

data class ExportEntry(
    val file: ExportedFile,
    val info: ArchiveInfo?,
    val state: InstallState,
)

interface ExportsInteractor {

    /** Emits the plain file list first, then again once each file is identified. */
    fun entries(): Observable<List<ExportEntry>>

    /** Fires when something that affects [entries] has changed. */
    fun invalidations(): Observable<Unit>

    /** Tries every file, so one that refuses does not spare the rest, and fails afterwards. */
    fun delete(files: List<ExportedFile>): Completable

    fun clear(): Completable

    fun folder(): String

    fun saveFolder(uri: String)

}

class ExportsInteractorImpl(
    private val storage: ExportStorage,
    private val inspector: ArchiveInspector,
    private val repository: InstalledAppsRepository,
    private val manager: ExportManager,
    private val changes: ExportChanges,
    private val preferences: AppPreferences,
    private val schedulers: SchedulersFactory,
) : ExportsInteractor {

    override fun entries(): Observable<List<ExportEntry>> =
        Single.fromCallable { storage.list().sortedByDescending { it.modified } }
            .subscribeOn(schedulers.io())
            .flatMapObservable { files ->
                Observable.just(files.map { ExportEntry(it, null, InstallState.UNKNOWN) })
                    .concatWith(identify(files).toObservable())
            }

    private fun identify(files: List<ExportedFile>): Single<List<ExportEntry>> =
        repository.apps().firstOrError()
            .observeOn(schedulers.io())
            .flatMap { apps ->
                val installed = apps.associateBy { it.packageName }
                // One file at a time, so a disposed load stops between files.
                Observable.fromIterable(files)
                    .map { file ->
                        val info = inspector.inspect(file)
                        ExportEntry(file, info, info?.let { stateOf(it, installed) } ?: InstallState.UNKNOWN)
                    }
                    .toList()
            }

    private fun stateOf(info: ArchiveInfo, installed: Map<String, InstalledApp>): InstallState {
        val app = installed[info.packageName] ?: return InstallState.NOT_INSTALLED
        return when {
            app.versionCode > info.versionCode -> InstallState.NEWER_INSTALLED
            app.versionCode < info.versionCode -> InstallState.OLDER_INSTALLED
            else -> InstallState.CURRENT
        }
    }

    override fun invalidations(): Observable<Unit> = Observable.merge(
        manager.session().map { it.active }.distinctUntilChanged().skip(1).filter { active -> !active }.map { },
        repository.apps().skip(1).map { },
        changes.changes(),
    )

    override fun delete(files: List<ExportedFile>): Completable = runToEnd {
        val failed = files.filterNot(storage::delete)
        if (failed.isNotEmpty()) throw IOException("Unable to delete ${failed.size} files")
    }

    override fun clear(): Completable = runToEnd { storage.clear() }

    /** The screen may be re-created meanwhile: the work stays subscribed here, not there. */
    private fun runToEnd(work: () -> Unit): Completable {
        val completable = Completable.fromAction(work)
            .subscribeOn(schedulers.io())
            .doOnTerminate { changes.notifyChanged() }
            .cache()
        completable.onErrorComplete().subscribe()
        return completable
    }

    override fun folder(): String = storage.location()

    override fun saveFolder(uri: String) {
        preferences.exportFolder = uri
    }

}
