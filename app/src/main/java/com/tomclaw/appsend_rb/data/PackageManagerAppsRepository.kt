package com.tomclaw.appsend_rb.data

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.PermissionInfo
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.pm.PackageInfoCompat
import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import com.tomclaw.appsend_rb.domain.AppDetails
import com.tomclaw.appsend_rb.domain.ApkSourceProvider
import com.tomclaw.appsend_rb.domain.ApkSources
import com.tomclaw.appsend_rb.domain.InstalledApp
import com.tomclaw.appsend_rb.domain.InstalledAppsRepository
import com.tomclaw.appsend_rb.domain.RequestedPermission
import com.tomclaw.appsend_rb.domain.SchedulersFactory
import com.tomclaw.appsend_rb.util.AppIcons
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * The single source of the installed-app list. Package changes arrive as broadcasts,
 * so every screen stays current without tracking what it may have launched.
 */
class PackageManagerAppsRepository(
    context: Context,
    private val schedulers: SchedulersFactory,
) : InstalledAppsRepository, ApkSourceProvider {

    private val packageManager = context.packageManager
    private val snapshot = BehaviorRelay.create<List<InstalledApp>>()
    private val packageChanges = PublishRelay.create<Unit>()
    private val lock = Any()
    private var inFlight: Completable? = null

    init {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(
            context.applicationContext,
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    AppIcons.evictAll()
                    packageChanges.accept(Unit)
                }
            },
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )
        // An update arrives as a removal and an addition in a row: read once they settle.
        packageChanges
            .debounce(PACKAGE_CHANGE_DEBOUNCE_MS, TimeUnit.MILLISECONDS, schedulers.io())
            .flatMapCompletable { refreshAfterCurrentRead().onErrorComplete() }
            .subscribe()
    }

    /** Whoever asks first gets the list read, whichever screen the app was restored on. */
    override fun apps(): Observable<List<InstalledApp>> = snapshot.hide()
        .doOnSubscribe {
            if (!snapshot.hasValue()) refresh().onErrorComplete().subscribe()
        }

    override fun refresh(): Completable = synchronized(lock) {
        inFlight ?: Single.fromCallable { loadApps() }
            .subscribeOn(schedulers.io())
            .doOnSuccess { snapshot.accept(it) }
            .ignoreElement()
            .doFinally { synchronized(lock) { inFlight = null } }
            .cache()
            .also { inFlight = it }
    }

    /** A read already under way may have passed the package that has just changed. */
    private fun refreshAfterCurrentRead(): Completable {
        val current = synchronized(lock) { inFlight } ?: Completable.complete()
        return current.onErrorComplete().andThen(Completable.defer { refresh() })
    }

    private fun loadApps(): List<InstalledApp> =
        packageManager.getInstalledApplications(0).mapNotNull { info ->
            try {
                toApp(info, packageManager.getPackageInfo(info.packageName, 0))
            } catch (ignored: PackageManager.NameNotFoundException) {
                // Removed while we were reading the list.
                null
            }
        }

    private fun toApp(info: ApplicationInfo, packageInfo: PackageInfo): InstalledApp? {
        val base = info.publicSourceDir?.let(::File)?.takeIf { it.exists() } ?: return null
        val splits = info.splitPublicSourceDirs.orEmpty()
        val baseSize = base.length()
        return InstalledApp(
            label = packageManager.getApplicationLabel(info).toString(),
            packageName = info.packageName,
            versionName = packageInfo.versionName.orEmpty(),
            versionCode = PackageInfoCompat.getLongVersionCode(packageInfo),
            baseSize = baseSize,
            totalSize = baseSize + splits.sumOf { File(it).length() },
            splitCount = splits.size,
            firstInstallTime = packageInfo.firstInstallTime,
            lastUpdateTime = packageInfo.lastUpdateTime,
            system = info.flags and SYSTEM_FLAGS != 0,
            launchable = packageManager.getLaunchIntentForPackage(info.packageName) != null,
        )
    }

    override fun details(packageName: String): Single<AppDetails> =
        Single.fromCallable { loadDetails(packageName) }.subscribeOn(schedulers.io())

    private fun loadDetails(packageName: String): AppDetails {
        val packageInfo = packageManager.getPackageInfo(packageName, DETAILS_FLAGS)
        val info = packageInfo.applicationInfo
            ?: throw PackageManager.NameNotFoundException(packageName)
        val app = toApp(info, packageInfo)
            ?: throw PackageManager.NameNotFoundException(packageName)
        return AppDetails(
            app = app,
            basePath = info.publicSourceDir,
            installerLabel = installerLabel(packageName),
            minSdk = info.minSdkVersion,
            targetSdk = info.targetSdkVersion,
            signatureSha256 = signingCertificates(packageInfo).lastOrNull(),
            permissions = permissions(packageInfo),
        )
    }

    @Suppress("DEPRECATION")
    private fun installerLabel(packageName: String): String? {
        val installer = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                packageManager.getInstallSourceInfo(packageName).installingPackageName
            } else {
                packageManager.getInstallerPackageName(packageName)
            }
        } catch (ignored: IllegalArgumentException) {
            null
        } catch (ignored: PackageManager.NameNotFoundException) {
            null
        } ?: return null
        return try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(installer, 0))
                .toString()
        } catch (ignored: PackageManager.NameNotFoundException) {
            installer
        }
    }

    private fun permissions(packageInfo: PackageInfo): List<RequestedPermission> {
        val names = packageInfo.requestedPermissions.orEmpty()
        val flags = packageInfo.requestedPermissionsFlags
        return names.mapIndexed { index, name ->
            val info = try {
                packageManager.getPermissionInfo(name, 0)
            } catch (ignored: PackageManager.NameNotFoundException) {
                null
            }
            RequestedPermission(
                name = name,
                description = info?.loadLabel(packageManager)?.toString()
                    ?.takeIf { it != name }
                    ?.replaceFirstChar { it.uppercase() },
                dangerous = info?.isDangerous() ?: false,
                granted = flags != null &&
                    flags[index] and PackageInfo.REQUESTED_PERMISSION_GRANTED != 0,
            )
        }
    }

    private fun PermissionInfo.isDangerous(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            protection == PermissionInfo.PROTECTION_DANGEROUS
        } else {
            @Suppress("DEPRECATION")
            protectionLevel and PermissionInfo.PROTECTION_MASK_BASE == PermissionInfo.PROTECTION_DANGEROUS
        }

    override fun sources(packageName: String): ApkSources {
        val info = packageManager.getApplicationInfo(packageName, 0)
        return ApkSources(
            basePath = info.publicSourceDir,
            splitPaths = info.splitPublicSourceDirs.orEmpty().toList(),
        )
    }

}

@SuppressLint("InlinedApi")
private val DETAILS_FLAGS = PackageManager.GET_PERMISSIONS or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    PackageManager.GET_SIGNING_CERTIFICATES
} else {
    @Suppress("DEPRECATION")
    PackageManager.GET_SIGNATURES
}

private const val SYSTEM_FLAGS = ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
private const val PACKAGE_CHANGE_DEBOUNCE_MS = 500L
