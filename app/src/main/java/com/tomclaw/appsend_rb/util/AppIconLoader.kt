package com.tomclaw.appsend_rb.util

import android.content.pm.PackageManager
import com.tomclaw.imageloader.core.Loader
import java.io.File
import java.io.FileOutputStream
import java.net.URI

class AppIconLoader(private val packageManager: PackageManager) : Loader {

    override val schemes: List<String>
        get() = listOf("app")

    override fun load(uriString: String, file: File): Boolean {
        try {
            val packageName = parseUri(uriString)
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            val appInfo = packageInfo.applicationInfo
            if (appInfo != null) {
                val data = getPackageIconPng(appInfo, packageManager)
                FileOutputStream(file).use { output ->
                    output.write(data)
                    output.flush()
                }
                return true
            }
        } catch (ignored: Throwable) {
            // Игнорируем ошибки загрузки и возвращаем false
        }
        return false
    }

    private fun parseUri(s: String?): String {
        val uri = URI.create(s)
        return uri.authority
    }

}

fun createAppIconURI(packageName: String): String {
    return "app://$packageName"
}
