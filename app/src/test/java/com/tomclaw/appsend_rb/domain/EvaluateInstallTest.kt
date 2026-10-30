package com.tomclaw.appsend_rb.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EvaluateInstallTest {

    private fun candidate(
        code: Long = 5,
        minSdk: Int = 24,
        apks: List<String> = listOf("base.apk"),
        signatures: List<String> = listOf("AA"),
    ) = PackageCandidate("uri", false, "pkg", "App", "1", code, minSdk, 100, apks, signatures)

    private fun installed(code: Long = 5, signatures: List<String> = listOf("AA")) =
        InstalledVersion("1", code, signatures)

    private fun evaluate(candidate: PackageCandidate, installed: InstalledVersion? = null) =
        evaluateInstall(candidate, installed, sdk = 34, supportedAbis = listOf("arm64-v8a", "armeabi-v7a"))

    @Test
    fun `fresh install passes`() = assertNull(evaluate(candidate()))

    @Test
    fun `same version over itself passes`() = assertNull(evaluate(candidate(), installed()))

    @Test
    fun `newer version passes`() = assertNull(evaluate(candidate(code = 6), installed()))

    @Test
    fun `older version is a downgrade`() =
        assertEquals(InstallIssue.DOWNGRADE, evaluate(candidate(code = 4), installed()))

    @Test
    fun `different signer is a mismatch even for a newer version`() =
        assertEquals(
            InstallIssue.SIGNATURE_MISMATCH,
            evaluate(candidate(code = 6, signatures = listOf("BB")), installed()),
        )

    @Test
    fun `a rotated key shares its lineage and passes`() =
        assertNull(evaluate(candidate(code = 6, signatures = listOf("AA", "BB")), installed()))

    @Test
    fun `unknown signatures are not treated as a mismatch`() =
        assertNull(evaluate(candidate(signatures = emptyList()), installed()))

    @Test
    fun `app for a newer android is rejected`() =
        assertEquals(InstallIssue.ANDROID_TOO_OLD, evaluate(candidate(minSdk = 35)))

    @Test
    fun `set for another architecture is rejected`() =
        assertEquals(
            InstallIssue.WRONG_ARCHITECTURE,
            evaluate(candidate(apks = listOf("base.apk", "split_config.x86_64.apk"))),
        )

    @Test
    fun `set for this architecture passes`() =
        assertNull(evaluate(candidate(apks = listOf("base.apk", "split_config.arm64_v8a.apk", "split_config.ru.apk"))))

    @Test
    fun `set without native splits passes`() =
        assertNull(evaluate(candidate(apks = listOf("base.apk", "split_config.xxhdpi.apk"))))

}
