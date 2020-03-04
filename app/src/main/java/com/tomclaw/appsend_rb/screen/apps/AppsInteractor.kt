package com.tomclaw.appsend_rb.screen.apps

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.tomclaw.appsend_rb.dto.AppEntity
import com.tomclaw.appsend_rb.util.SchedulersFactory
import io.reactivex.Observable
import io.reactivex.Single
import java.io.File
import java.util.ArrayList

interface AppsInteractor {
    fun loadApps(): Observable<List<AppEntity>>

}

class AppsInteractorImpl(
        private val packageManager: PackageManagerWrapper,
        private val schedulers: SchedulersFactory
) : AppsInteractor {

    override fun loadApps(): Observable<List<AppEntity>> {
        val appItemList = ArrayList<AppEntity>()
        val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        for (info in packages) {
            try {
                val packageInfo = packageManager.getPackageInfo(
                        info.packageName, PackageManager.GET_PERMISSIONS)
                val file = File(info.publicSourceDir)
                if (file.exists()) {
                    val label = packageManager.getApplicationLabel(info)
                    val versionName = packageInfo.versionName
                    val versionCode = packageInfo.versionCode
                    val firstInstallTime = packageInfo.firstInstallTime
                    val lastUpdateTime = packageInfo.lastUpdateTime
                    val launchIntent = packageManager.getLaunchIntentForPackage(info.packageName)
                    val appItem = AppEntity(label, info.packageName, versionName, versionCode, file.path,
                            file.length(), firstInstallTime, lastUpdateTime)
                    val isUserApp = info.flags and ApplicationInfo.FLAG_SYSTEM != ApplicationInfo.FLAG_SYSTEM &&
                            info.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP != ApplicationInfo.FLAG_UPDATED_SYSTEM_APP
                    if (isUserApp /*|| PreferenceHelper.isShowSystemApps(context)*/) {
                        if (launchIntent != null/* || !PreferenceHelper.isRunnableOnly(context)*/) {
                            appItemList.add(appItem)
                        }
                    }
                }
            } catch (ignored: Throwable) {
                // Bad package.
            }
        }
//        val sortOrder = PreferenceHelper.getSortOrder(context)
//        if (TextUtils.equals(sortOrder, context.getString(R.string.sort_order_ascending_value))) {
//            Collections.sort(appItemList) { lhs: AppItem, rhs: AppItem -> lhs.label.toUpperCase().compareTo(rhs.label.toUpperCase()) }
//        } else if (TextUtils.equals(sortOrder, context.getString(R.string.sort_order_descending_value))) {
//            Collections.sort(appItemList) { lhs: AppItem, rhs: AppItem -> rhs.label.toUpperCase().compareTo(lhs.label.toUpperCase()) }
//        } else if (TextUtils.equals(sortOrder, context.getString(R.string.sort_order_app_size_value))) {
//            Collections.sort(appItemList, java.util.Comparator { lhs: AppItem, rhs: AppItem -> compareLong(rhs.size, lhs.size) })
//        } else if (TextUtils.equals(sortOrder, context.getString(R.string.sort_order_install_time_value))) {
//            Collections.sort(appItemList, java.util.Comparator { lhs: AppItem, rhs: AppItem -> compareLong(rhs.firstInstallTime, lhs.firstInstallTime) })
//        } else if (TextUtils.equals(sortOrder, context.getString(R.string.sort_order_update_time_value))) {
//            Collections.sort(appItemList, java.util.Comparator { lhs: AppItem, rhs: AppItem -> compareLong(rhs.lastUpdateTime, lhs.lastUpdateTime) })
//        }
//        val baseItems: ArrayList<BaseItem> = ArrayList<BaseItem>(appItemList)
//        if (PreferenceHelper.IS_DONATE_ENABLED) {
//            val count = Math.min(baseItems.size, 7)
//            val random = Random(System.currentTimeMillis())
//            val position = random.nextInt(count)
//            val donateItem = DonateItem()
//            baseItems.add(position, donateItem)
//        }
        return Single.just(appItemList as List<AppEntity>).toObservable().subscribeOn(schedulers.io())
//        return Single.just(listOf(AppEntity(
//                label = "AppSend",
//                packageName = "com.tomclaw.appsend_rb",
//                versionName = "1.0",
//                versionCode = 15,
//                path = "",
//                size = 1024,
//                firstInstallTime = 1024,
//                lastUpdateTime = 1024
//        ))).toObservable().subscribeOn(schedulers.io())
    }

}