package com.tomclaw.appsend_rb.screen.apps

import com.avito.konveyor.blueprint.Item
import com.tomclaw.appsend_rb.dto.AppEntity
import com.tomclaw.appsend_rb.screen.apps.adapter.app.AppItem
import com.tomclaw.appsend_rb.util.createAppIconURI
import java.util.concurrent.TimeUnit

interface AppEntityConverter {

    fun convert(id: Long, entity: AppEntity): Item

}

class AppEntityConverterImpl(private val resourceProvider: ResourceProvider) : AppEntityConverter {

    override fun convert(id: Long, entity: AppEntity): Item = AppItem(
        id = id,
        icon = createAppIconURI(entity.packageName),
        packageName = entity.packageName,
        name = entity.label,
        size = resourceProvider.formatBytes(entity.size),
        firstInstallTime = resourceProvider.formatTime(entity.firstInstallTime),
        lastUpdateTime = resourceProvider.formatTime(entity.lastUpdateTime),
        versionName = entity.versionName,
        versionCode = entity.versionCode,
        newApp = entity.lastUpdateTime > System.currentTimeMillis() - TimeUnit.DAYS.toMillis(1)
    )

}
