package com.tomclaw.appsend_rb.screen.apps

import android.content.pm.PackageInfo
import android.net.Uri
import android.os.Build
import com.tomclaw.appsend_rb.dto.AppEntity
import com.tomclaw.appsend_rb.util.SchedulersFactory
import com.tomclaw.appsend_rb.util.getApkName
import com.tomclaw.appsend_rb.util.safeClose
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.Locale

interface AppsInteractor {

    fun loadApps(
        systemApps: Boolean,
        runnableOnly: Boolean,
        sortOrder: Int
    ): Observable<List<AppEntity>>

    fun loadApp(packageName: String): Observable<AppEntity>

    fun exportApp(entity: AppEntity): Observable<Uri>

}

class AppsInteractorImpl(
    private val packageManager: PackageManagerWrapper,
    private val outputWrapper: OutputWrapper,
    private val locale: Locale,
    private val schedulers: SchedulersFactory
) : AppsInteractor {

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
                    val isUserApp = (info.flags and FLAG_SYSTEM) != FLAG_SYSTEM &&
                            (info.flags and FLAG_UPDATED_SYSTEM_APP) != FLAG_UPDATED_SYSTEM_APP
                    if (isUserApp || systemApps) {
                        val launchIntent =
                            packageManager.getLaunchIntentForPackage(info.packageName)
                        if (launchIntent != null || !runnableOnly) {
                            entities += entity
                        }
                    }
                }
            } catch (ignored: Throwable) {
                // Bad package, ignore
            }
        }
        when (sortOrder) {
            NAME_ASCENDING -> entities.sortWith { lhs, rhs ->
                lhs.label.uppercase(locale).compareTo(rhs.label.uppercase(locale))
            }

            NAME_DESCENDING -> entities.sortWith { lhs, rhs ->
                rhs.label.uppercase(locale).compareTo(lhs.label.uppercase(locale))
            }

            APP_SIZE -> entities.sortWith { lhs, rhs -> rhs.size.compareTo(lhs.size) }
            INSTALL_TIME -> entities.sortWith { lhs, rhs -> rhs.firstInstallTime.compareTo(lhs.firstInstallTime) }
            UPDATE_TIME -> entities.sortWith { lhs, rhs -> rhs.lastUpdateTime.compareTo(lhs.lastUpdateTime) }
        }

        return entities
    }

    private fun createAppEntity(packageInfo: PackageInfo): AppEntity? {
        val appInfo = packageInfo.applicationInfo  // локальная val для избежания smart cast ошибки
        val sourceDir = appInfo?.publicSourceDir
        if (sourceDir != null) {
            val file = File(sourceDir)
            if (file.exists()) {
                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }
                return AppEntity(
                    label = packageManager.getApplicationLabel(appInfo),
                    packageName = packageInfo.packageName,
                    versionName = packageInfo.versionName ?: "",
                    versionCode = versionCode,
                    requestedPermissions = packageInfo.requestedPermissions?.asList() ?: emptyList(),
                    path = file.path,
                    size = file.length(),
                    firstInstallTime = packageInfo.firstInstallTime,
                    lastUpdateTime = packageInfo.lastUpdateTime
                )
            }
        }
        return null
    }

    override fun exportApp(entity: AppEntity): Observable<Uri> {
        return Single
            .create<Uri> { emitter ->
                val buffer = ByteArray(524288)
                var inputStream: InputStream? = null
                var outputStream: OutputStream? = null
                val uri = outputWrapper.getOutputUri(
                    getApkName(entity),
                    "application/vnd.android.package-archive"
                )
                val file = File(entity.path)
                try {
                    inputStream = FileInputStream(file)
                    outputStream = outputWrapper.openStream(uri)
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
                emitter.onSuccess(uri)
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