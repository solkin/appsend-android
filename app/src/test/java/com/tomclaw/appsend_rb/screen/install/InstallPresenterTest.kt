package com.tomclaw.appsend_rb.screen.install

import com.jakewharton.rxrelay3.PublishRelay
import com.tomclaw.appsend_rb.ImmediateSchedulers
import com.tomclaw.appsend_rb.NoAnalytics
import com.tomclaw.appsend_rb.domain.InstallConfirmation
import com.tomclaw.appsend_rb.domain.InstallEvent
import com.tomclaw.appsend_rb.domain.InstallFailure
import com.tomclaw.appsend_rb.domain.InstallIssue
import com.tomclaw.appsend_rb.domain.InstalledVersion
import com.tomclaw.appsend_rb.domain.PackageCandidate
import com.tomclaw.appsend_rb.domain.ValueFormatter
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstallPresenterTest {

    private val candidate = PackageCandidate(
        uri = "uri",
        plainApk = false,
        packageName = "pkg",
        label = "App",
        versionName = "2.0",
        versionCode = 2,
        minSdk = 24,
        totalBytes = 100,
        apkNames = listOf("base.apk", "split_config.en.apk"),
        signatures = listOf("AA"),
    )

    private val interactor = FakeInteractor()
    private val view = FakeView()
    private val router = FakeRouter()

    private val presenter by lazy {
        InstallPresenterImpl(interactor, Bytes, ImmediateSchedulers, NoAnalytics).also {
            it.attachView(view)
            it.attachRouter(router)
        }
    }

    @Test
    fun `an unreadable file says so`() {
        interactor.inspection = Single.error(IllegalStateException())
        presenter
        assertEquals(InstallViewModel.Unreadable, view.last)
    }

    @Test
    fun `a clean candidate is ready to install`() {
        presenter
        val model = view.last as InstallViewModel.Ready
        assertEquals("2.0", model.toVersion)
        assertEquals(1, model.splitCount)
        assertEquals(false, model.update)
    }

    @Test
    fun `an issue blocks the install and offers replacement only when it can help`() {
        interactor.issue = InstallIssue.SIGNATURE_MISMATCH
        presenter
        assertEquals(true, (view.last as InstallViewModel.Blocked).canReplace)
        interactor.issue = InstallIssue.WRONG_ARCHITECTURE
        presenter.onResume()
        assertEquals(false, (view.last as InstallViewModel.Blocked).canReplace)
    }

    @Test
    fun `without permission the button leads to settings instead of installing`() {
        interactor.allowed = false
        presenter
        view.installRelay.accept(Unit)
        assertEquals(1, router.permissionRequests)
        assertEquals(0, interactor.installs)
    }

    @Test
    fun `returning from settings picks the new permission up`() {
        interactor.allowed = false
        presenter
        assertEquals(true, (view.last as InstallViewModel.Ready).needsPermission)
        interactor.allowed = true
        presenter.onResume()
        assertEquals(false, (view.last as InstallViewModel.Ready).needsPermission)
    }

    @Test
    fun `reports progress, asks for confirmation and finishes`() {
        presenter
        view.installRelay.accept(Unit)
        interactor.events.accept(InstallEvent.Writing(50, 100))
        assertEquals(50, (view.last as InstallViewModel.Installing).percent)

        val confirmation = object : InstallConfirmation {}
        interactor.events.accept(InstallEvent.ConfirmationRequired(confirmation))
        assertEquals(listOf(confirmation), router.confirmations)
        assertEquals(true, (view.last as InstallViewModel.Installing).awaitingConfirmation)

        interactor.events.accept(InstallEvent.Installed)
        assertTrue(view.last is InstallViewModel.Installed)
    }

    @Test
    fun `confirmation waits for a visible screen`() {
        presenter
        view.installRelay.accept(Unit)
        presenter.detachRouter()
        interactor.events.accept(InstallEvent.ConfirmationRequired(object : InstallConfirmation {}))
        assertEquals(0, router.confirmations.size)
        presenter.attachRouter(router)
        assertEquals(1, router.confirmations.size)
    }

    @Test
    fun `resuming during an install does not reset the sheet`() {
        presenter
        view.installRelay.accept(Unit)
        interactor.events.accept(InstallEvent.Writing(10, 100))
        presenter.onResume()
        assertTrue(view.last is InstallViewModel.Installing)
    }

    @Test
    fun `cancelling in the system dialog returns to the ready state`() {
        presenter
        view.installRelay.accept(Unit)
        interactor.events.accept(InstallEvent.Failed(InstallFailure.ABORTED))
        assertTrue(view.last is InstallViewModel.Ready)
    }

    @Test
    fun `a failure stays on screen after resuming`() {
        presenter
        view.installRelay.accept(Unit)
        interactor.events.accept(InstallEvent.Failed(InstallFailure.STORAGE))
        presenter.onResume()
        assertTrue(view.last is InstallViewModel.Failed)
    }

    @Test
    fun `a re-created screen joins the running install instead of inspecting again`() {
        interactor.ongoingCandidate = candidate
        presenter
        assertEquals(0, interactor.inspections)
        assertTrue(view.last is InstallViewModel.Installing)
        interactor.events.accept(InstallEvent.Installed)
        assertTrue(view.last is InstallViewModel.Installed)
    }

    @Test
    fun `leaving the sheet cancels the install`() {
        presenter
        view.installRelay.accept(Unit)
        view.dismissRelay.accept(Unit)
        assertEquals(1, interactor.cancels)
    }

    @Test
    fun `a shown confirmation is reported so it is not launched twice`() {
        presenter
        view.installRelay.accept(Unit)
        interactor.events.accept(InstallEvent.ConfirmationRequired(object : InstallConfirmation {}))
        assertEquals(1, interactor.confirmationsShown)
    }

    @Test
    fun `an outcome saved before re-creation is shown again`() {
        val restored = InstallPresenterImpl(
            interactor, Bytes, ImmediateSchedulers, NoAnalytics, InstallState(false, InstallFailure.STORAGE),
        )
        restored.attachView(view)
        assertEquals(InstallFailure.STORAGE, (view.last as InstallViewModel.Failed).failure)
    }

    @Test
    fun `a confirmation of a finished session is not launched later`() {
        presenter
        view.installRelay.accept(Unit)
        presenter.detachRouter()
        interactor.events.accept(InstallEvent.ConfirmationRequired(object : InstallConfirmation {}))
        interactor.events.accept(InstallEvent.Failed(InstallFailure.ABORTED))
        presenter.attachRouter(router)
        assertEquals(0, router.confirmations.size)
    }

    @Test
    fun `repeated progress does not render again`() {
        presenter
        view.installRelay.accept(Unit)
        interactor.events.accept(InstallEvent.Writing(50, 100))
        val renders = view.renders
        interactor.events.accept(InstallEvent.Writing(50, 100))
        assertEquals(renders, view.renders)
    }

    @Test
    fun `a real failure is shown`() {
        presenter
        view.installRelay.accept(Unit)
        interactor.events.accept(InstallEvent.Failed(InstallFailure.STORAGE))
        assertEquals(InstallFailure.STORAGE, (view.last as InstallViewModel.Failed).failure)
    }

    private inner class FakeInteractor : InstallInteractor {

        var inspection: Single<PackageCandidate> = Single.just(candidate)
        var issue: InstallIssue? = null
        var allowed = true
        var installs = 0
        val events = PublishRelay.create<InstallEvent>()

        var ongoingCandidate: PackageCandidate? = null
        var inspections = 0
        var cancels = 0
        var confirmationsShown = 0

        override fun inspect(): Single<PackageCandidate> {
            inspections++
            return inspection
        }

        override fun ongoing() = ongoingCandidate

        override fun confirmationShown() {
            confirmationsShown++
        }

        override fun cancel() {
            cancels++
        }

        override fun installedVersion(packageName: String): InstalledVersion? = null

        override fun issue(candidate: PackageCandidate, installed: InstalledVersion?) = issue

        override fun canInstall() = allowed

        override fun install(candidate: PackageCandidate): Observable<InstallEvent> {
            installs++
            return events
        }

    }

    private class FakeView : InstallView {

        var last: InstallViewModel? = null
        var renders = 0
        val installRelay = PublishRelay.create<Unit>()
        val dismissRelay = PublishRelay.create<Unit>()

        override fun render(model: InstallViewModel) {
            last = model
            renders++
        }

        override fun installClicks(): Observable<Unit> = installRelay

        override fun replaceClicks(): Observable<Unit> = Observable.never()

        override fun openClicks(): Observable<Unit> = Observable.never()

        override fun dismissClicks(): Observable<Unit> = dismissRelay

    }

    private class FakeRouter : InstallRouter {

        var permissionRequests = 0
        val confirmations = ArrayList<InstallConfirmation>()

        override fun requestInstallPermission() {
            permissionRequests++
        }

        override fun confirmInstall(confirmation: InstallConfirmation) {
            confirmations += confirmation
        }

        override fun uninstall(packageName: String) = Unit

        override fun launch(packageName: String) = Unit

        override fun leave() = Unit

    }

    private object Bytes : ValueFormatter {

        override fun bytes(value: Long) = "$value B"

        override fun relativeTime(time: Long) = "$time"

        override fun date(time: Long) = "$time"

    }

}
