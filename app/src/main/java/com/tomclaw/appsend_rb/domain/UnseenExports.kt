package com.tomclaw.appsend_rb.domain

import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import io.reactivex.rxjava3.core.Observable

/**
 * Files extracted since the exports were last looked at. Kept apart from any screen: a
 * session keeps its files until it is dismissed, and must not count them twice.
 */
class UnseenExports(manager: ExportManager) {

    private val counted = HashSet<String>()
    private val unseen = BehaviorRelay.createDefault(emptySet<String>())

    init {
        manager.session().subscribe { session ->
            if (session.action == ExportAction.EXTRACT) {
                val files = session.items
                    .filter { it.status == ExportStatus.DONE }
                    .mapNotNull { it.file?.uri }
                add(files)
            }
        }
    }

    @Synchronized
    private fun add(files: List<String>) {
        val fresh = files.filter { counted.add(it) }
        if (fresh.isNotEmpty()) unseen.accept(unseen.value!! + fresh)
    }

    fun count(): Observable<Int> = unseen.map { it.size }.distinctUntilChanged()

    @Synchronized
    fun markSeen() {
        unseen.accept(emptySet())
    }

}

/** Fires when files were removed, for whichever screen is showing them by then. */
class ExportChanges {

    private val relay = PublishRelay.create<Unit>()

    fun changes(): Observable<Unit> = relay

    fun notifyChanged() = relay.accept(Unit)

}
