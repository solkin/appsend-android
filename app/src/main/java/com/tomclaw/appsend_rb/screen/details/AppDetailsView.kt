package com.tomclaw.appsend_rb.screen.details

import com.jakewharton.rxrelay3.PublishRelay
import com.tomclaw.appsend_rb.R
import com.tomclaw.knopochki.core.Box
import com.tomclaw.knopochki.core.Column
import com.tomclaw.knopochki.core.Insets
import com.tomclaw.knopochki.core.KnopochkiView
import com.tomclaw.knopochki.core.Node
import com.tomclaw.knopochki.core.Scroll
import com.tomclaw.knopochki.draw.Icons
import com.tomclaw.knopochki.draw.Shape
import com.tomclaw.knopochki.theme.Role
import com.tomclaw.knopochki.widget.Button
import com.tomclaw.knopochki.widget.ButtonStyle
import com.tomclaw.knopochki.widget.Divider
import com.tomclaw.knopochki.widget.Snackbar
import com.tomclaw.knopochki.widget.Surface
import com.tomclaw.knopochki.widget.Text
import com.tomclaw.knopochki.widget.TextStyle
import com.tomclaw.knopochki.widget.TopAppBar
import io.reactivex.rxjava3.core.Observable

interface AppDetailsView {

    fun navigationClicks(): Observable<Unit>

    fun copyPackageClicks(): Observable<Unit>

    fun permissionsClicks(): Observable<Unit>

    fun systemDetailsClicks(): Observable<Unit>

    fun setDetails(details: AppDetailsUi)

    fun showPackageNameCopied()

}

class AppDetailsViewImpl(knopochkiView: KnopochkiView) : AppDetailsView {

    private val resources = knopochkiView.context.resources

    private val navigationRelay = PublishRelay.create<Unit>()
    private val copyPackageRelay = PublishRelay.create<Unit>()
    private val permissionsRelay = PublishRelay.create<Unit>()
    private val systemDetailsRelay = PublishRelay.create<Unit>()

    private val label = Text("", style = TextStyle.TITLE_LARGE, role = Role.ON_SURFACE)
    private val packageName = Text("", style = TextStyle.BODY_SMALL, role = Role.ON_SURFACE_VARIANT)
    private val version = value()
    private val installTime = value()
    private val updateTime = value()
    private val size = value()
    private val status = value()
    private val path = value()
    private val permissions = value()

    private val openPermissions = Button(
        resources.getString(R.string.app_details_open_permissions),
        ButtonStyle.OUTLINED,
    ) { permissionsRelay.accept(Unit) }

    private val snackbar = Snackbar()

    init {
        val bar = TopAppBar(
            title = resources.getString(R.string.app_details),
            navigationIcon = Icons.ARROW_BACK,
            onNavigationClick = { navigationRelay.accept(Unit) },
        )
        val card = Surface(
            content = Column(paddingDp = 4f)
                .add(row(R.string.app_details_version, version))
                .add(Divider(insetStartDp = 16f))
                .add(row(R.string.app_details_installed, installTime))
                .add(Divider(insetStartDp = 16f))
                .add(row(R.string.app_details_updated, updateTime))
                .add(Divider(insetStartDp = 16f))
                .add(row(R.string.app_details_size, size))
                .add(Divider(insetStartDp = 16f))
                .add(row(R.string.app_details_status, status))
                .add(Divider(insetStartDp = 16f))
                .add(row(R.string.app_details_path, path))
                .add(Divider(insetStartDp = 16f))
                .add(row(R.string.app_details_permissions, permissions)),
            containerRole = Role.SURFACE_CONTAINER_LOW,
            cornerToken = Shape.LARGE,
            paddingDp = 4f,
        )
        val content = Column(spacingDp = 12f, paddingDp = 16f)
            .add(label)
            .add(packageName)
            .add(
                Button(
                    resources.getString(R.string.app_details_copy_package),
                    ButtonStyle.OUTLINED,
                ) { copyPackageRelay.accept(Unit) }
            )
            .add(card)
            .add(openPermissions)
            .add(
                Button(
                    resources.getString(R.string.app_details_open_system_details),
                    ButtonStyle.OUTLINED,
                ) { systemDetailsRelay.accept(Unit) }
            )
        val scroll = Scroll(Insets(content, applyBottom = true, applySides = true))
        scroll.onScroll = { bar.onContentScrolled(it) }
        knopochkiView.root = Box()
            .add(Column().add(bar).addFilling(scroll))
            .add(Insets(snackbar, applyBottom = true, applySides = true))
    }

    private fun value() = Text("", style = TextStyle.BODY_LARGE, role = Role.ON_SURFACE)

    private fun row(labelRes: Int, value: Text): Node =
        Column(spacingDp = 2f, paddingDp = 12f)
            .add(Text(resources.getString(labelRes), style = TextStyle.BODY_SMALL, role = Role.ON_SURFACE_VARIANT))
            .add(value)

    override fun navigationClicks(): Observable<Unit> = navigationRelay

    override fun copyPackageClicks(): Observable<Unit> = copyPackageRelay

    override fun permissionsClicks(): Observable<Unit> = permissionsRelay

    override fun systemDetailsClicks(): Observable<Unit> = systemDetailsRelay

    override fun setDetails(details: AppDetailsUi) {
        label.text = details.label
        packageName.text = details.packageName
        version.text = details.version
        installTime.text = details.installTime
        updateTime.text = details.updateTime
        size.text = details.size
        path.text = details.path
        permissions.text = details.permissions
        status.text = details.status
        openPermissions.enabled = details.permissionsEnabled
    }

    override fun showPackageNameCopied() {
        snackbar.show(resources.getString(R.string.app_details_package_copied))
    }

}
