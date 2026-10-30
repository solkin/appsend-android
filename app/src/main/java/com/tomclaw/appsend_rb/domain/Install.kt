package com.tomclaw.appsend_rb.domain

import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single

/** A package file the user wants to install, read but not yet handed to the system. */
data class PackageCandidate(
    val uri: String,
    val plainApk: Boolean,
    val packageName: String,
    val label: String,
    val versionName: String,
    val versionCode: Long,
    val minSdk: Int,
    val totalBytes: Long,
    val apkNames: List<String>,
    val signatures: List<String>,
) {
    val splitCount: Int get() = apkNames.size - 1
}

data class InstalledVersion(
    val versionName: String,
    val versionCode: Long,
    val signatures: List<String>,
)

/** Why a package cannot go on top of what the device has now. */
enum class InstallIssue { SIGNATURE_MISMATCH, DOWNGRADE, ANDROID_TOO_OLD, WRONG_ARCHITECTURE }

/** Opaque to everyone except the router that launches it. */
interface InstallConfirmation

enum class InstallFailure { ABORTED, BLOCKED, CONFLICT, INCOMPATIBLE, INVALID, STORAGE, UNKNOWN }

sealed interface InstallEvent {

    data class Writing(val writtenBytes: Long, val totalBytes: Long) : InstallEvent

    data class ConfirmationRequired(val confirmation: InstallConfirmation) : InstallEvent

    data object AwaitingConfirmation : InstallEvent

    data object Installed : InstallEvent

    data class Failed(val failure: InstallFailure) : InstallEvent

}

interface PackageInstallerGateway {

    /** Fails when the file is not a readable app package. */
    fun inspect(uri: String): Single<PackageCandidate>

    fun installedVersion(packageName: String): InstalledVersion?

    /** Whether the user has allowed this app to install packages. */
    fun canInstall(): Boolean

    /**
     * The candidate of the install of this file that no screen has finished with yet. It
     * may have ended while nobody was looking: joining it then replays the outcome.
     */
    fun ongoing(uri: String): PackageCandidate?

    /**
     * Starts installing, or joins the install already running for this file. Replays the
     * latest event, then follows. An install of another file is abandoned. An install
     * outlives its subscribers until [cancel].
     */
    fun install(candidate: PackageCandidate): Observable<InstallEvent>

    /** The confirmation was handed to the system, so it must not be shown again. */
    fun confirmationShown(uri: String)

    /** Abandons this file's install if unfinished and forgets it. Other files are left alone. */
    fun cancel(uri: String)

}

private val ABI_SPLIT = Regex("""(?:config\.|base-)(arm64_v8a|armeabi_v7a|armeabi|x86_64|x86|mips64|mips)""")

fun evaluateInstall(
    candidate: PackageCandidate,
    installed: InstalledVersion?,
    sdk: Int,
    supportedAbis: List<String>,
): InstallIssue? {
    if (candidate.minSdk > sdk) return InstallIssue.ANDROID_TOO_OLD
    // A set exported on another device holds only that device's native code.
    val splitAbis = candidate.apkNames.mapNotNull { ABI_SPLIT.find(it)?.groupValues?.get(1) }
    val supported = supportedAbis.map { it.replace('-', '_') }
    if (splitAbis.isNotEmpty() && splitAbis.none { it in supported }) return InstallIssue.WRONG_ARCHITECTURE
    if (installed == null) return null
    // Signatures come with their rotation history: a shared key means the same publisher.
    if (installed.signatures.isNotEmpty() && candidate.signatures.isNotEmpty() &&
        installed.signatures.intersect(candidate.signatures.toSet()).isEmpty()
    ) {
        return InstallIssue.SIGNATURE_MISMATCH
    }
    if (installed.versionCode > candidate.versionCode) return InstallIssue.DOWNGRADE
    return null
}
