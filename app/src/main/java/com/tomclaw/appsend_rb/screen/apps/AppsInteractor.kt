package com.tomclaw.appsend_rb.screen.apps

import android.content.pm.PackageInfo
import android.os.Build
import android.os.Environment
import android.os.Environment.DIRECTORY_DOWNLOADS
import com.tomclaw.appsend_rb.dto.AppEntity
import com.tomclaw.appsend_rb.util.SchedulersFactory
import com.tomclaw.appsend_rb.util.getApkName
import com.tomclaw.appsend_rb.util.safeClose
import io.reactivex.Observable
import io.reactivex.Single
import java.io.*
import java.util.*

interface AppsInteractor {

    fun loadApps(
        systemApps: Boolean,
        runnableOnly: Boolean,
        sortOrder: Int
    ): Observable<List<AppEntity>>

    fun loadApp(packageName: String): Observable<AppEntity>

    fun exportApp(entity: AppEntity): Observable<File>

}

class AppsInteractorImpl(
    private val packageManager: PackageManagerWrapper,
    private val schedulers: SchedulersFactory
) : AppsInteractor {

    private val locale = Locale.getDefault()

    override fun loadApps(
        systemApps: Boolean,
        runnableOnly: Boolean,
        sortOrder: Int
    ): Observable<List<AppEntity>> {
        return Single
            .create<List<AppEntity>> { emitter ->
                val entities = loadEntities(systemApps, runnableOnly, sortOrder)
                emitter.onSuccess(entities)
            }
            .toObservable()
            .subscribeOn(schedulers.io())
    }

    override fun loadApp(packageName: String): Observable<AppEntity> {
        return Single
            .create<AppEntity> { emitter ->
                try {
                    val packageInfo = packageManager.getPackageInfo(packageName, GET_PERMISSIONS)
                    createAppEntity(packageInfo)?.let { emitter.onSuccess(it) }
                        ?: emitter.onError(IOException("unable to create app entity"))
                } catch (ex: Throwable) {
                    emitter.onError(ex)
                }
            }
            .toObservable()
            .subscribeOn(schedulers.io())
    }

    private fun loadEntities(
        systemApps: Boolean,
        runnableOnly: Boolean,
        sortOrder: Int
    ): List<AppEntity> {
        val entities = ArrayList<AppEntity>()
        val packages = packageManager.getInstalledApplications(GET_META_DATA)
        for (info in packages) {
            try {
                val packageInfo = packageManager.getPackageInfo(info.packageName, GET_PERMISSIONS)
                createAppEntity(packageInfo)?.let { entity ->
                    val isUserApp = info.flags and FLAG_SYSTEM != FLAG_SYSTEM &&
                            info.flags and FLAG_UPDATED_SYSTEM_APP != FLAG_UPDATED_SYSTEM_APP
                    if (isUserApp || systemApps) {
                        val launchIntent =
                            packageManager.getLaunchIntentForPackage(info.packageName)
                        if (launchIntent != null || !runnableOnly) {
                            entities += entity
                        }
                    }
                }
            } catch (ignored: Throwable) {
                // Bad package.
            }
        }
        when (sortOrder) {
            NAME_ASCENDING -> entities.sortWith { lhs: AppEntity, rhs: AppEntity ->
                lhs.label.toUpperCase(
                    locale
                ).compareTo(rhs.label.toUpperCase(locale))
            }
            NAME_DESCENDING -> entities.sortWith { lhs: AppEntity, rhs: AppEntity ->
                rhs.label.toUpperCase(
                    locale
                ).compareTo(lhs.label.toUpperCase(locale))
            }
            APP_SIZE -> entities.sortWith { lhs: AppEntity, rhs: AppEntity -> rhs.size.compareTo(lhs.size) }
            INSTALL_TIME -> entities.sortWith { lhs: AppEntity, rhs: AppEntity ->
                rhs.firstInstallTime.compareTo(
                    lhs.firstInstallTime
                )
            }
            UPDATE_TIME -> entities.sortWith { lhs: AppEntity, rhs: AppEntity ->
                rhs.lastUpdateTime.compareTo(
                    lhs.lastUpdateTime
                )
            }
        }

        return entities
    }

    private fun createAppEntity(packageInfo: PackageInfo): AppEntity? {
        val file = File(packageInfo.applicationInfo.publicSourceDir)
        if (file.exists()) {
            val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            return AppEntity(
                label = packageManager.getApplicationLabel(packageInfo.applicationInfo),
                packageName = packageInfo.packageName,
                versionName = packageInfo.versionName,
                versionCode = versionCode,
                requestedPermissions = packageInfo.requestedPermissions.asList(),
                path = file.path,
                size = file.length(),
                firstInstallTime = packageInfo.firstInstallTime,
                lastUpdateTime = packageInfo.lastUpdateTime
            )
        }
        return null
    }

    override fun exportApp(entity: AppEntity): Observable<File> {
        return Single
            .create<File> { emitter ->
                val file = File(entity.path)
                val directory = File(
                    Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS),
                    APPS_DIR_NAME
                )
                if (!(directory.exists() || directory.mkdirs())) {
                    emitter.onError(IOException("unable to create directory " + directory.name))
                    return@create
                }
                val destination = File(directory, getApkName(entity))
                if (destination.exists() && !destination.delete()) {
                    emitter.onError(IOException("unable to delete destination file " + destination.name))
                    return@create
                }
                val buffer = ByteArray(524288)
                var inputStream: InputStream? = null
                var outputStream: OutputStream? = null
                try {
                    inputStream = FileInputStream(file)
                    outputStream = FileOutputStream(destination)
                    var read: Int
                    while (inputStream.read(buffer).also { read = it } != -1) {
                        outputStream.write(buffer, 0, read)
                    }
                    outputStream.flush()
                } catch (ex: Throwable) {
                    emitter.onError(ex)
                    return@create
                } finally {
                    outputStream.safeClose()
                    inputStream.safeClose()
                }
                emitter.onSuccess(destination)
            }
            .toObservable()
            .subscribeOn(schedulers.io())
    }

}

const val NAME_ASCENDING = 1
const val NAME_DESCENDING = 2
const val APP_SIZE = 3
const val INSTALL_TIME = 4
const val UPDATE_TIME = 5

const val APPS_DIR_NAME = "Apps"
