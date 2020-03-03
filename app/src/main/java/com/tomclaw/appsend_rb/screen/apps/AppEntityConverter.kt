package com.tomclaw.appsend_rb.screen.apps

import com.avito.konveyor.blueprint.Item
import com.tomclaw.appsend_rb.dto.AppEntity
import com.tomclaw.appsend_rb.screen.apps.adapter.app.AppItem
import com.tomclaw.appsend_rb.util.AppIconData

interface AppEntityConverter {

    fun convert(id: Long, entity: AppEntity): Item

}

class AppEntityConverterImpl : AppEntityConverter {

    override fun convert(id: Long, entity: AppEntity): Item = AppItem(
            id = id,
            icon = AppIconData(entity.packageName, entity.versionCode),
            name = entity.label,
            versionName = entity.versionName,
            versionCode = entity.versionCode
    )

}