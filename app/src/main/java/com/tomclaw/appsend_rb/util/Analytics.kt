package com.tomclaw.appsend.util

import android.app.Application
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import androidx.core.content.pm.PackageInfoCompat
import com.tomclaw.appsend_rb.BuildConfig
import com.tomclaw.bananalytics.Bananalytics
import com.tomclaw.bananalytics.BananalyticsConfig
import com.tomclaw.bananalytics.BananalyticsImpl
import com.tomclaw.bananalytics.EnvironmentProvider
import com.tomclaw.bananalytics.api.BreadcrumbCategory
import com.tomclaw.bananalytics.api.Environment
import java.util.Locale

interface Analytics {

    fun register()

    fun trackEvent(name: String)

    fun trackEvent(name: String, tags: Map<String, String>)

    fun trackEvent(name: String, tags: Map<String, String>, fields: Map<String, Double>)

    fun trackException(throwable: Throwable, context: Map<String, String>)

}

class AnalyticsImpl(
    private val app: Application,
) : Analytics {

    private val bananalytics: Bananalytics by lazy {
        BananalyticsImpl(
            filesDir = app.filesDir,
            config = BananalyticsConfig(
                baseUrl = getRequiredManifestString(app.applicationContext, BANANALYTICS_BASE_URL_KEY),
                apiKey = getRequiredManifestString(app.applicationContext, BANANALYTICS_API_KEY_KEY),
            ),
            environmentProvider = AppEnvironmentProvider(app.applicationContext),
            isDebug = BuildConfig.DEBUG,
        )
    }

    override fun register() {
        bananalytics.install()
    }

    override fun trackEvent(name: String) {
        trackEvent(name, emptyMap(), emptyMap())
    }

    override fun trackEvent(name: String, tags: Map<String, String>) {
        trackEvent(name, tags, emptyMap())
    }

    override fun trackEvent(name: String, tags: Map<String, String>, fields: Map<String, Double>) {
        bananalytics.trackEvent(name, tags, fields)
        bananalytics.leaveBreadcrumb(name, BreadcrumbCategory.USER_ACTION)
    }

    override fun trackException(throwable: Throwable, context: Map<String, String>) {
        bananalytics.trackException(throwable, context)
    }

    private fun getRequiredManifestString(context: Context, key: String): String {
        val value = getManifestString(context, key)
        if (TextUtils.isEmpty(value)) {
            throw RuntimeException("Bananalytics value for $key was not configured correctly in manifest or build configuration.")
        }
        return value.orEmpty()
    }

    private fun getManifestString(context: Context, key: String): String? {
        return getManifestBundle(context).getString(key)
    }

    private fun getManifestBundle(context: Context): Bundle {
        return try {
            context.packageManager.getApplicationInfo(
                context.packageName,
                PackageManager.GET_META_DATA
            ).metaData
        } catch (e: PackageManager.NameNotFoundException) {
            throw RuntimeException(e)
        }
    }

}

private class AppEnvironmentProvider(
    private val context: Context,
) : EnvironmentProvider {

    override fun environment(): Environment {
        val packageInfo = context.getPackageInfo()
        return Environment(
            packageName = context.packageName,
            appVersion = PackageInfoCompat.getLongVersionCode(packageInfo),
            appVersionName = packageInfo.versionName.orEmpty(),
            deviceId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ).orEmpty(),
            osVersion = Build.VERSION.SDK_INT,
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            country = Locale.getDefault().country,
            language = Locale.getDefault().language,
        )
    }

    private fun Context.getPackageInfo(): PackageInfo {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
        } catch (e: PackageManager.NameNotFoundException) {
            throw RuntimeException(e)
        }
    }

}

private const val BANANALYTICS_BASE_URL_KEY = "bananalytics.base_url"
private const val BANANALYTICS_API_KEY_KEY = "bananalytics.api_key"
