package com.tomclaw.appsend_rb.screen.apps

import com.avito.konveyor.blueprint.Item

interface AppInfoConverter {

//    fun convert(record: AppInfo): Item

}

class AppInfoConverterImpl : AppInfoConverter {

//    override fun convert(record: AppInfo): Item = when (record.template.type) {
//        TYPE_GROUP -> GroupItem(
//                id = record.id,
//                title = record.getField("title"),
//                icon = record.template.icon
//        )
//        TYPE_PASSWORD -> PasswordItem(
//                id = record.id,
//                title = record.getField("title"),
//                subtitle = record.getField("username"),
//                icon = record.template.icon
//        )
//        TYPE_CARD -> CardItem(
//                id = record.id,
//                title = record.getField("title"),
//                number = record.getField("number"),
//                icon = record.template.icon
//        )
//        TYPE_NOTE -> NoteItem(
//                id = record.id,
//                title = record.getField("title"),
//                text = record.getField("text"),
//                icon = record.template.icon
//        )
//        else -> throw IllegalArgumentException("Unknown record type!")
//    }
//
//    private fun Record.getField(key: String): String {
//        return fields[key] ?: throw IllegalArgumentException("Mandatory field '$key' doesn't exist")
//    }

}